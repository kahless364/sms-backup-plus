---
type: bug
status: ready
artifact_type: bug
severity: medium
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
platforms: []
tags: []
id: BUG-019
title: Default TLS socket factory enables TLS 1.0 and TLS 1.1 wherever the provider supports them
domain: build
origin: qa
updated_at: '2026-11-17T22:19:07.053Z'
---

# BUG-019: Default TLS socket factory enables TLS 1.0 and TLS 1.1 wherever the provider supports them

## Description

The vendored default socket factory (`k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`) is the factory the app uses for every connection whose trust policy is `SYSTEM_VALIDATED` (the default). It decides which protocol versions a socket may use like this:

1. Once, when the class loads, it asks the TLS provider for the protocols the provider **supports** (not the ones the provider enables by default) (`:107`).
2. It removes SSLv3 and nothing else; the blocked list holds one entry (`:87-89`).
3. It puts `TLSv1.2`, `TLSv1.1`, `TLSv1` first, in that order (`:83-85`), and appends every other supported protocol after them (`:116-117`, `reorder` at `:124-148`).
4. `hardenSocket` applies that list to every socket the factory returns, on both the direct path and the path that layers TLS over an existing connection (`:176`, `:186-188`).

The result is that TLS 1.0 and TLS 1.1 are enabled on every socket wherever the provider supports them. RFC 8996 ("Deprecating TLS 1.0 and TLS 1.1") deprecates both versions and says clients must not negotiate them.

What was observed, and its limits:

- **Under the default test provider (Conscrypt)**, the provider itself supports and enables TLS 1.0, 1.1, 1.2 and 1.3 (`sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/I0-probe.txt:26-27`). The factory's list is `[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3]` (`I0-probe.txt:28`). Because the provider already enables the two old versions, **the factory's own part cannot be separated from the provider's under Conscrypt**: a list built from the enabled protocols would give the same result (`I0-probe.txt:49`).
- **Under the JDK's own provider (SunJSSE)**, which is selected for one test class, the provider supports `[TLSv1.3, TLSv1.2, TLSv1.1, TLSv1, SSLv3, SSLv2Hello]` but enables only `[TLSv1.3, TLSv1.2]` (`I0c-diag.txt:7-8`). The factory's socket enables `[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3, SSLv2Hello]` (`I0c-diag.txt:15`; `:17-18` record `has_TLSv1=true` and `has_TLSv1.1=true`). Here the provider leaves the two old versions off and **the factory turns them on**. This is the observation that shows the factory's own part.
- The same JDK run shows `SSLv2Hello` enabled by the factory. That is a legacy hello format, not a protocol version, and is recorded here as an observed fact; it is not part of this bug's acceptance criteria.

What has **not** been shown:

- No handshake below TLS 1.3 was run. Every test handshake negotiated TLS 1.3 (`I0-probe.txt:12`).
- "Enabled" is not "negotiable". The JDK's disabled-algorithms property lists `TLSv1` and `TLSv1.1` (`I0-probe.txt:65`) and may still refuse them during a handshake on a desktop JDK. Conscrypt does not follow that property.
- What a device's platform provider supports or enables, per Android API level, was not measured. Test runs use Robolectric at a simulated API level (`I0-probe.txt:56` shows 29) on a desktop JDK, not a device.
- Whether any server this app talks to would negotiate below TLS 1.2 is unknown. The owner's actual use is Gmail's IMAP endpoint with an app password; **its minimum TLS version was not checked in this work, and none is asserted here.** Nothing in U-063 connects to a real server.
- Whether an on-path attacker could use the enabled versions to force a downgrade was not examined.

The code itself explains why the supported list is used (`:102-106`): the comment says TLS 1.1 and 1.2 are supported on API levels 16 and up but enabled by default only on API 20 and up. Whether that rationale still applies at minSdk 24 (change record CR-011) was not assessed.

## Steps to Reproduce

Both reproductions are test runs. Both tests belong to U-063 (sprint-020) and were committed with it on 2026-11-20 (commit `5a5321d4`). Run Gradle in a shell without `MSYS_NO_PATHCONV` set.

1. Conscrypt provider (Robolectric default):
   `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryHardeningTest.createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem"`
   The test builds a socket through the factory and asserts that `TLSv1` and `TLSv1.1` are enabled if and only if the provider lists them as supported (`DefaultTrustedSocketFactoryHardeningTest.java:389-410`). It passes because the provider supports and enables both. Per its own Javadoc (`:378-382`) it cannot tell the factory's behaviour from the provider's here.
2. JDK provider (SunJSSE, one separate test class with its own sandbox):
   `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryJdkProviderTest.createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff"`
   The test first asserts that on this provider each of `TLSv1` and `TLSv1.1` is supported and not enabled by default (precondition A1-P2, `DefaultTrustedSocketFactoryJdkProviderTest.java:265-278`), then asserts that the socket from the factory has both enabled (`:280-289`). It passes. This run shows the factory itself enabling the two versions.
3. Observe: **both tests pass today.** They assert present behaviour (a characterisation), not a desired state. Passing is therefore not evidence the behaviour is acceptable. Run 2 is the one that shows the defect.
4. To run both together: `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryHardeningTest" --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryJdkProviderTest"`.

## Expected Behavior

A socket returned by the factory does not have `TLSv1` or `TLSv1.1` enabled, whatever the provider supports or enables by default (the guidance of RFC 8996). TLS 1.2 and TLS 1.3 stay enabled wherever the provider supports them. This is the expectation under current guidance; whether the product owner adopts it, and at what compatibility cost, is the decision recorded under AC-5.

## Actual Behavior

The factory enables `TLSv1` and `TLSv1.1` on every socket whenever the provider lists them as supported, including on a provider (SunJSSE) that leaves them off by default. Only SSLv3 is removed.

## Evidence

Evidence files (workspace-relative), all under `sdlc/artifacts/build/sprints/sprint-020/U-063/`:

| Evidence | What it shows |
|----------|---------------|
| `evidence/I0-probe.txt:26` | Conscrypt: supported protocols `[TLSv1, TLSv1.1, TLSv1.2, TLSv1.3]` |
| `evidence/I0-probe.txt:27` | Conscrypt: enabled by default `[TLSv1, TLSv1.1, TLSv1.2, TLSv1.3]` |
| `evidence/I0-probe.txt:28` | Conscrypt: factory's enabled list `[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3]` |
| `evidence/I0-probe.txt:39-44` | `tls10_supported`, `tls10_default`, `tls10_hardened`, and the same three for TLS 1.1, all `true` |
| `evidence/I0-probe.txt:45-46, 49` | SSLv3 and SSLv2Hello not supported under Conscrypt; supported minus SSLv3 equals the default set |
| `evidence/I0-probe.txt:12` | every handshake negotiated TLS 1.3 |
| `evidence/I0-probe.txt:56, 65` | simulated API level 29; the JDK's disabled-algorithms list names `TLSv1` and `TLSv1.1` |
| `evidence/I0c-diag.txt:7` | SunJSSE: supported `[TLSv1.3, TLSv1.2, TLSv1.1, TLSv1, SSLv3, SSLv2Hello]` |
| `evidence/I0c-diag.txt:8` | SunJSSE: enabled by default `[TLSv1.3, TLSv1.2]` |
| `evidence/I0c-diag.txt:10-13` | supported has `TLSv1` and `TLSv1.1`; default-enabled has neither |
| `evidence/I0c-diag.txt:15, 17-18, 19` | the factory's socket and static list: `[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3, SSLv2Hello]`; both legacy versions present |

Tests that record the behaviour (both pass today):

- `createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem`, `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java:389` (Javadoc `:374-387`; the comment at `:372-373` already names BUG-019 (it read "Bug id: pending" until this bug was filed) and must stay when the tests are updated).
- `createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff`, `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java:263` (Javadoc `:254-261`).

Code lines (all in `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`, each opened and confirmed): `:83-85` ordered known protocols; `:87-89` blocked protocols (`SSLv3` only); `:102-107` supported protocols read, with the comment; `:116-117` list built; `:124-148` `reorder`; `:176` `hardenSocket` call in `createSocket`; `:182-189` `hardenSocket`, protocols at `:186-188`.

Developer's draft: `sdlc/artifacts/build/sprints/sprint-020/U-063/implementation-log.md`, section I3.7 (`:524-547`).

Earlier mention: the sprint-019 security review noted the same behaviour as a pre-existing observation, not a finding against that story (`sdlc/artifacts/build/sprints/sprint-019/U-062/review-security.md:116` and `:268`). It was not filed as a bug then.

### Measurements on two emulators (2026-11-20)

Added by the orchestrator after the product owner allowed a measurement on the installed emulators ("Any already installed android emulator is fine"). Method: a small read-only Java program, compiled to a dex file and run on the emulator with `app_process` from the adb shell. It prints the platform TLS provider's supported and default-enabled protocols, applies the list this factory would build (supported minus SSLv3; TLS 1.2, 1.1, 1.0 first), and makes bare TLS handshakes to `imap.gmail.com:993` (no login, no data, each connection closed after the handshake). The program and its raw output are kept outside the repository; nothing in the app, on either emulator's settings, or in the mailbox was changed.

| | Android 16 emulator (API 36; has the app, target level 35) | Android 11 emulator (API 30) |
|---|---|---|
| Platform TLS provider | Conscrypt (`AndroidOpenSSL`) | Conscrypt (`AndroidOpenSSL`) |
| Supported protocols | TLS 1.2, TLS 1.3 | TLS 1.0, 1.1, 1.2, 1.3 |
| Enabled by default | TLS 1.2, TLS 1.3 | TLS 1.0, 1.1, 1.2, 1.3 |
| Enabled after the factory's list is applied | TLS 1.2, TLS 1.3 | TLS 1.2, 1.1, 1.0, 1.3 |
| Handshake to Gmail IMAP, platform defaults | TLS 1.3 | TLS 1.3 |
| Handshake with the factory's list | TLS 1.3 | TLS 1.3 |
| Handshake limited to TLS 1.2 | accepted, TLS 1.2 | accepted, TLS 1.2 |
| Handshake limited to TLS 1.1 | refused by the device before sending: "SSLv3, TLSv1 and TLSv1.1 are no longer supported and were filtered from the list" | accepted by Gmail, TLS 1.1 |
| Handshake limited to TLS 1.0 | refused by the device, same message | accepted by Gmail, TLS 1.0 |

What this shows:
- On Android 16 the platform no longer offers TLS 1.0 or 1.1, so this factory cannot enable them there whatever its code asks for.
- On Android 11 the platform supports them and enables them by default. The factory's list contains the same four versions as the platform default, so on that version the factory does not add TLS 1.0 or 1.1; it also does not remove them.
- Gmail's IMAP endpoint accepted a handshake limited to TLS 1.0 and one limited to TLS 1.1 on 2026-11-20. It also accepts TLS 1.2 and negotiates TLS 1.3 when offered. Dropping TLS 1.0 and 1.1 in the app would therefore cost nothing for Gmail.
- With both ends offering TLS 1.3, every unrestricted handshake negotiated TLS 1.3.

Limits: the program runs as a standalone process in the adb shell, not inside the app's own process, so it cannot show whether an app's target level changes the lists (it gave the same lists at several target levels on both emulators); emulator system images, not the owner's physical devices; Android versions between 11 and 16 were not measured (an Android 13 image is installed and was not used); only Gmail's endpoint was tried; nothing here measures whether a downgrade can be forced by a third party.

## Acceptance Criteria
**Decision recorded (AC-5), 2026-11-20:** the product owner decided to drop every protocol version older than TLS 1.2 ("Yes drop anything older than tls 1.2"). The record is Record 8 in the appendix "Product owner decision" of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`. Added by the orchestrator; the acceptance criteria AC-1 to AC-4 now apply.


These apply if the product owner decides (AC-5) that the factory stops enabling TLS 1.0 and 1.1. If the decision is to keep them, AC-5 records it, the bug is closed with that record, and AC-1 to AC-4 do not apply.

- [ ] AC-1: The defect no longer reproduces following the steps above. After the fix, a socket from the factory has neither `TLSv1` nor `TLSv1.1` in `getEnabledProtocols()` under the Conscrypt test provider and under the JDK test provider, for both socket shapes (direct, and layered over an existing plain connection). Under SunJSSE the precondition that those two are supported but not enabled by default still holds, so the assertion is made against a provider that supports them.
- [ ] AC-2: The two characterisation tests are updated in the same change to assert the new behaviour. They are not deleted and not marked `@Ignore`. The comment at `DefaultTrustedSocketFactoryHardeningTest.java:372-373`, which already names BUG-019, is kept. Under Conscrypt the updated assertion is no longer satisfied by the provider's own defaults, so it must fail if the factory stops removing the two versions.
- [ ] AC-3: Existing functionality is not broken by the fix. `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exits 0 with 0 failures; the skipped count is no higher than before the change. `TLSv1.2` and `TLSv1.3` are enabled on factory sockets wherever the provider supports them, under both test providers. `SSLv3` stays not enabled. Cipher order, the blocked-cipher list and SNI behaviour are unchanged.
- [ ] AC-4: Every other test that pins the protocol lists (named in Impact Analysis) either passes unchanged or is changed in the same change with the reason stated in the change record. No pinned test is edited merely to make it pass without that explanation.
- [ ] AC-5: The decision on compatibility with old servers is recorded in a dated written record that names who decided (the product owner, with the Security Reviewer's judgement), whether TLS 1.0 and 1.1 are dropped or kept, and the compatibility consequence accepted. The record is found by searching the workspace for "BUG-019". The decision is made before the fix is merged.

## Impact Analysis

Searches run over the layers listed for this project's change records. Search terms: `TLS 1.0`, `TLS 1.1`, `TLSv1`, `SSLv3`, `ENABLED_PROTOCOLS`, `ORDERED_KNOWN_PROTOCOLS`, `BLACKLISTED_PROTOCOLS`, `getEnabledProtocols`, `RFC 8996`, and the class name.

| # | Layer | Affected? | Files and occurrences | Action needed |
|---|-------|-----------|-----------------------|---------------|
| 1 | Requirements (`sdlc/artifacts/requirements`) | No | 0 matches for any protocol-version term | None. No requirement states a protocol-version policy (see the note on the gap below) |
| 2 | Design and contracts (`sdlc/artifacts/design`) | No | `CNTR-MODERNIZATION-001-tls-socket-factory.md`: 0 matches for `protocol`, `TLS 1.x`, `TLSv1`, `SSLv3`; its only `version` hits are contract versioning (`:279-281`). Its `TLS` hits are the `TlsTrustPolicy` name. The contract **says nothing about protocol versions**. Design files: 0 matches | None required by the fix. A statement of the policy is a decision for the contract's next revision (CR-009 is already planned to add the SNI statement) |
| 3 | Stories, sprints, change records (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | Yes (references only) | See list below | Cross-reference BUG-019; check CR-009 T5 wording |
| 4 | Vendored library (`k9mail-vendored`) | Yes (root) | 1 file, `ssl/DefaultTrustedSocketFactory.java`, lines `:83-89`, `:102-107`, `:116-117`, `:124-148`, `:176`, `:182-189`. Only production construction: `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java:148` (per CR-009's search) | The change itself |
| 5 | Application code (`app/src/main`) | No | 0 matches for any protocol-version term or `ENABLED_PROTOCOLS` | None |
| 6 | Tests (`app/src/test`) | Yes | 3 test classes; see list below | Update with the fix |
| 7 | Documentation (`README.md`, `sdlc/artifacts/engagement`) | No | 0 matches in `README.md` and in `sdlc/artifacts/engagement` | None |

**Tests that pin the protocol lists** (`app/src/test/java/com/zegoggles/smssync/mail/ssl/`):

| Class and test | Lines | What it pins | Effect of a fix that removes TLS 1.0 and 1.1 |
|----------------|-------|--------------|-----------------------------------------------|
| `DefaultTrustedSocketFactoryHardeningTest.createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem` | `:389` | legacy versions enabled iff supported | Must change (AC-2) |
| `DefaultTrustedSocketFactoryJdkProviderTest.createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff` | `:263` | factory enables the two where the provider does not | Must change (AC-2) |
| `DefaultTrustedSocketFactoryHardeningTest.createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder` | `:332`, assertions `:349-369` | enabled set equals supported minus SSLv3; order 1.2, 1.1, 1.0 first | Set-equality fails if the two are removed; order check changes |
| `DefaultTrustedSocketFactoryJdkProviderTest.createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder_jdkProvider` | `:204`, assertions `:228-251` | same, under SunJSSE | Same |
| `DefaultTrustedSocketFactoryHardeningTest.protocolConstants_matchRecordedBaseline` | `:256-259` | `ORDERED_KNOWN_PROTOCOLS` equals `TLSv1.2, TLSv1.1, TLSv1`; blocked list equals `SSLv3` | Fails if either constant changes |
| `DefaultTrustedSocketFactoryHardeningTest.reorder_removesEveryBlockedItem` | `:179`, protocols `:196-199` | `reorder` with the constants removes `SSLv3`, keeps `TLSv1.2` | Depends on the blocked-list constant |
| `DefaultTrustedSocketFactoryHardeningTest.reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder` | `:209`, protocols `:220-223` | known protocols order using the constants | Depends on the known-list constant |
| `DefaultTrustedSocketFactoryHardeningTest.createSocket_layeredOverPlainSocket_appliesSameHardening` | `:418`, protocols `:434-436` | both socket shapes have identical protocol lists | Should still pass if both shapes are changed identically |
| `DefaultTrustedSocketFactoryHardeningTest.checkIsolationGuardA1G2` (a `@Before` guard) | `:154-173`, `SSLv2Hello` marker `:159` | uses `SSLv2Hello` in the factory's list as a sign that the list was computed under the JDK provider; also checks for DHE suites (`:164-172`) | If a fix also drops `SSLv2Hello`, this marker no longer fires; the DHE check remains |
| `DefaultTrustedSocketFactorySniTest` | whole class | Uses a fake socket whose `getEnabledProtocols` and `setEnabledProtocols` are no-ops (`:339-340`) | Not affected |

**Other artifacts that mention TLS versions or this factory's protocol handling:**

- `sdlc/artifacts/build/sprints/sprint-020/U-063-tls-socket-factory-characterisation-tests.md` (the story): SC-3 `:247` (the supported-set input), SC-5 `:249` (ordering is not a preference), DP-2 `:255`, AC-16 text `:109`, AC-23 `:124`, verification step `:182`, Implementation Notes `:301` (Option A stands) and `:309`. The story's text needs no edit unless the fix changes the counts it states.
- `sdlc/artifacts/build/sprints/sprint-020/sprint-plan.md:47` (DP-2); `sdlc/artifacts/build/sprints/sprint-020/U-063/plan.md`, `plan-review.md`, `expert-depth-check.md`, `contract-verification.md`, `implementation-log.md` (I3.7 `:524-547`). Build records; not edited by the fix.
- `sdlc/artifacts/build/change-records/CR-009-cntr-001-default-trusted-socket-factory-producer.md`, test list row **T5** (`:213`): "Cipher and protocol hardening ... apply the disallow and ordering lists to the enabled suites and protocols". The row does not say that for protocols the input is the supported set. U-063 SC-3 already notes this. If the policy changes, T5's wording would need to say what the lists apply to.
- `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`: Q7.f (`:692-702`) lists the factory's hardening code as part of what moves (`:34-89`, `:182-189`) and says nothing about protocol versions. Q12.f (`:1367-1380`) lists follow-ups on missing tests (items 5, 8, 9) and says nothing about protocol versions. The only `SSLv3` mention is the upstream blocked-protocol row `C-347` (`:1787`), which records upstream's blocked protocols as `SSLv3`. Record 6 (`:2212`) notes the pinned factory applies no hardening. Whether upstream's own factory builds its list from supported or enabled protocols was not examined.
- `sdlc/artifacts/build/sprints/sprint-011/U-047/review-security.md:167` ("Protocol blacklist (SSLv3 excluded): UNCHANGED"); `sdlc/artifacts/build/sprints/sprint-019/U-062/review-security.md:116, 268`. Historical reviews; not edited.

**Gap in the requirements corpus (stated, not filled here).** No requirement, contract or design file states which protocol versions the app accepts. The behaviour has therefore never been checked against a stated rule; this bug's fix needs the decision in AC-5 as its only authority.

**Cascade analysis** (three layers affected: vendored library, tests, build artifacts).

- Root layer: vendored library (implementation). Requirements and contracts are silent, so no upstream specification is wrong and none is violated; the checklist's "code-only fix, verify upstream first" case applies, with the added decision record.
- Propagation: vendored factory to the tests that pin its protocol lists (table above; yes) and to build-record wording (CR-009 T5; yes, wording only). To application code: none (no match). To requirements, design, documentation: none.
- In-progress work: U-063 (sprint-020) is in progress and its tests are uncommitted. U-063 will not fix this (its AC-23). Any fix lands after U-063's tests are committed, or the change set must be coordinated with them.
- Fix order: (1) decision record (AC-5); (2) the factory; (3) the pinned tests, in the same change; (4) reference wording (CR-009 T5, if kept as a live document).
- Change-record question: a change touching 3 layers would normally call for a change record. CR-009 is the existing record that moves this class; whether the fix needs its own record is not decided here.

**Interaction with the migration (stated, not chosen).** CR-009 moves this factory into app-owned code (feasibility analysis Record 4 and Record 6 decide the migration and the app-owned default factory; CR-011 moves minSdk to 24). So the fix can be made in either place.

| | Fix before the move, in the vendored file | Fix as part of the move |
|---|---|---|
| Cost | Edits vendored code. There is precedent: the BUG-015 SNI fix is already in this file (`:232-237`). The move must then carry the fix and the updated tests; if the move re-copies rather than moves, the fix is re-applied by hand and can be lost | One edit, in the new owner. But it couples a behaviour change to a move that CR-009 and U-063 treat as behaviour-preserving, so the characterisation tests could no longer prove "the move changed nothing" for protocols |
| Timing | Does not wait for the migration (Part B), whose date is not set | Waits until the move; the vendored class keeps the behaviour until then |
| Test effort | The two tests and the other pinned tests are updated once, then retargeted at the move (their Javadoc, `DefaultTrustedSocketFactoryJdkProviderTest.java:94-96`, already says a moved class needs retargeting, not editing to pass) | The pinned tests change and are retargeted in the same step, so a failing test could come from either change |
| Dependency | None on CR-009 | Depends on CR-009 and on CR-011 (REQ-015) |

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory only — refined during story planning.

**Architectural considerations.** The factory computes `ENABLED_PROTOCOLS` once, in a static initialiser, from `getSupportedProtocols()` (`DefaultTrustedSocketFactory.java:107`), removes only `SSLv3` (`:87-89`) and applies the result to both socket shapes (`:176`, `:186-188`). The comment at `:102-106` justifies "supported, not enabled" by API 16 to 19, where TLS 1.1 and 1.2 were supported but off by default; at minSdk 24 (CR-011) that reason no longer applies to TLS 1.2. CR-009 makes this class app-owned code at the Thunderbird move. A floor could live in one of three places, each with a different test footprint:
- *Blocked list:* add `TLSv1` and `TLSv1.1` to `BLACKLISTED_PROTOCOLS`. Smallest edit; `reorder` removes blocked items before ordering, so `ORDERED_KNOWN_PROTOCOLS` may stay. Breaks `protocolConstants_matchRecordedBaseline` and the two "supported minus SSLv3" set-equality tests; the two `reorder_*` tests depend on their inputs and need checking. `SSLv2Hello` stays enabled under SunJSSE.
- *Build from the enabled list:* reverts the old rationale. On its own it does not meet AC-1 under Conscrypt (the provider enables both versions itself, `I0-probe.txt:27`), and probably not on devices whose platform provider still enables them by default (not measured). It also drops `SSLv2Hello`.
- *Explicit minimum or allow-list (TLS 1.2 and above):* makes the floor independent of provider defaults and drops `SSLv2Hello` and any unknown protocol. That reverses the "append unknown items" stance at `:143-145` for protocols (it still applies to ciphers), so a future protocol would need a code change. If nothing passes the filter, the socket gets an empty list and fails closed. Planning should say that this is intended.

**Layer cascade.** As the Impact Analysis says: vendored root, the pinned tests, and the CR-009 T5 wording. The new point is the isolation guard `checkIsolationGuardA1G2` (`HardeningTest.java:154-173`). It treats `SSLv2Hello` in `ENABLED_PROTOCOLS` as the sign that the lists were computed under the JDK provider (`:159`). Both "enabled list" and "explicit minimum" remove `SSLv2Hello` under SunJSSE, so that marker would never fire again and only the DHE-cipher check (`:164-172`) would remain. If either option is chosen, give the guard a marker that does not depend on the protocol policy (for example, the provider name recorded when the lists were computed) before relying on it. Under the JDK provider, `SSLv2Hello` is enabled only because the factory enables it (`I0c-diag.txt:8`, `:15`). If the fix removes it, record that in the decision; if the fix keeps it, record that as a deliberate choice.

**One story or several; timing.** I advise one story for the vendored fix, made after U-063's tests are committed, and not folded into the move. CR-009 and U-063 treat the move as behaviour-preserving, and their characterisation tests can prove that only if no behaviour change rides along. The cost is that the fix and the updated tests must be carried across at the move. If the move copies the file rather than moving it with history, CR-009 should name this fix in its checklist so the fix is not lost. Folding the fix into the move saves one edit, but a failing pinned test would then have two possible causes. The evidence missing to choose: the Part B date (not set), and whether the move is a `git mv` or a re-copy.

**Constraints and risks for planning.**
- The keep-or-drop decision belongs to the product owner (AC-5). Whether the result is safe is the Security Reviewer's judgement. Neither can be inferred from the passing tests.
- Measure first, cheaply. (1) The minimum TLS version of the owner's one mail server (Gmail IMAP): a probe client limited to TLS 1.1, and one limited to TLS 1.2, run from a workstation, settles it in minutes. (2) What the platform provider enables by default at API 24 and on the owner's Android 11+ devices: log `getSupportedProtocols()` and `getEnabledProtocols()` from a debug build on an API 24 emulator and on one owner device. Neither has been measured. Nothing in this section asserts either result.
- "Enabled" does not mean "negotiable". SunJSSE's disabled-algorithms property may refuse these versions anyway, and Conscrypt ignores that property. Every test handshake negotiated TLS 1.3. The tests do not prove that any handshake below TLS 1.3 works. If a TLS 1.2 floor is chosen, planning should consider a loopback handshake limited to TLS 1.2 (the existing `LoopbackTlsTestServer` may support this) so that the floor is not over-tightened by mistake.
- The fix is a process-wide static. A wrong floor breaks every `SYSTEM_VALIDATED` connection at once, so the decision record should state the fallback for the owner.

**Pinned-certificate factory.** `PinnedCertificateSocketFactory` uses `SSLContext.getInstance("TLS")` (`:78`) with platform defaults and no hardening. That is a separate question, about the hardening policy for the trust path the user opts into, with its own consumers and tests, so it belongs in a separate bug, not in BUG-019. File it now so that the same AC-5 decision can be applied to both factories in one decision record, even though the fixes ship separately.

**On the analyst's text.** I agree with severity medium and with the acceptance criteria. One suggestion: AC-5 should also ask the decision to say whether `SSLv2Hello` is kept, because two of the three options change it as a side effect.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` (`:83-89`, `:107`, `:116-117`, `:124-148`, `:176`, `:186-188`) | Builds the enabled-protocol list from the provider's supported protocols, removes only SSLv3, orders TLS 1.2, 1.1, 1.0 first, applies the list to every socket | Socket no longer has TLS 1.0 or TLS 1.1 enabled (AC-1). How is for the Solution Architect |
| `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java` (`:154-173`, `:179`, `:209`, `:256`, `:332`, `:389`, `:418`) | Pins the current protocol behaviour (Conscrypt) | Updated in the same change (AC-2, AC-4) |
| `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java` (`:204`, `:263`) | Pins the current protocol behaviour (SunJSSE) | Updated in the same change (AC-2, AC-4) |

## Existing Behavior to Preserve

- SSLv3 stays blocked (`BLACKLISTED_PROTOCOLS`, `:87-89`); the tests at `HardeningTest.java:349` and `JdkProviderTest.java:228` assert it.
- Cipher ordering and the cipher blocklist (`ORDERED_KNOWN_CIPHERS`, `BLACKLISTED_CIPHERS`, `:37-81`) are unchanged.
- SNI behaviour is unchanged: DNS host name sent; none for IPv4 or IPv6 literals or blank hosts; skipped without failure if the platform rejects the name (`:191-254`; ten tests in `DefaultTrustedSocketFactorySniTest`, plus the U-063 server-name tests).
- Both socket shapes (direct, `createSocket` at `:170`, and layered over an existing connection, `:172`) are hardened identically (`createSocket_layeredOverPlainSocket_appliesSameHardening`, `HardeningTest.java:418`).
- TLS 1.2 and TLS 1.3 stay enabled wherever the provider supports them.
- Neighbouring fact, not part of this bug: `PinnedCertificateSocketFactory` (`app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:78`, `SSLContext.getInstance("TLS")`) does no cipher or protocol hardening and sets no SNI itself; it uses the platform's defaults (feasibility analysis `:707` and Record 6 `:2212`). Do not change it under this bug.

## Verification Steps

For QA, per acceptance criterion. Run Gradle in a shell without `MSYS_NO_PATHCONV`.

1. AC-1: run the two single-test commands from Steps to Reproduce against the changed code, using the updated tests. Confirm each updated test asserts that `TLSv1` and `TLSv1.1` are absent from `getEnabledProtocols()` for a direct socket and for a layered socket, that both tests pass, and that the JDK-provider precondition (the two supported, not enabled by default) is still asserted in the SunJSSE test. Then revert the factory change locally and confirm both tests fail, under Conscrypt as well as SunJSSE (the Conscrypt one must not pass on the provider's defaults alone). Restore the change.
2. AC-2: `git diff` shows both test methods still present (renamed if needed) and neither carries `@Ignore`; `grep -n "BUG-019"` finds the id at the comment above the legacy-protocol test.
3. AC-3: run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`; confirm exit 0, 0 failures, skipped count not higher than before. In the updated tests, confirm assertions that `TLSv1.2` and `TLSv1.3` are enabled where supported, `SSLv3` is not enabled, and that the cipher list and SNI tests are unchanged in the diff.
4. AC-4: for each row of the pinned-tests table, confirm the test passes unchanged, or that the change record states why it changed. Authoritative test count: `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`; compare with the count before and explain any difference.
5. AC-5: search the workspace for "BUG-019" and open the decision record. Confirm date, decider, the keep-or-drop outcome, and the stated compatibility consequence; confirm its date is before the fix's commit.

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context

- **Two providers, two test classes.** Under Robolectric's default provider (Conscrypt) the provider's supported protocols equal its enabled protocols, and it enables TLS 1.0 and 1.1 itself, so the factory's own part in enabling them cannot be observed. The JDK provider (SunJSSE) lists TLS 1.0, 1.1 and SSLv3 as supported and leaves the first two off by default, so the factory's part is observable there. The product owner decided on 2026-11-17 that the SunJSSE checks live in one separate class (`DefaultTrustedSocketFactoryJdkProviderTest.java:46-47`). `@LooperMode(LEGACY)` on that class only gives it its own Robolectric sandbox so the factory's static lists are computed under the right provider (`:49-55`).
- **Limits of the test environment.** Neither provider is the app's runtime provider or a device's. Nothing here shows what a device's TLS stack sends or enables.
- **Severity reasoning (developer's, medium).** Security hardening with no exploit path shown. The factory widens the set of protocol versions the client will accept beyond what a current provider enables, on a path that carries mail credentials and content; but acceptance needs a server or attacker that offers a legacy version, and U-063 shows none does (it connects to no real server). Severity is already set; the scaffold's value is kept.
- **Who judges.** Whether the behaviour is safe, and whether it should change given the compatibility cost for old servers, is the Security Reviewer's judgement and the product owner's decision (U-063 DP-2, `:255`). U-063 will not fix this (its AC-23, `:124`); until the decision, Option A (keep the passing characterisation, file this bug) stands (`:301`).
- **Deployment facts.** The app is private and unpublished, for its owner's own use. The owner's devices all run Android 11 or newer (feasibility analysis Record 5, item 8). Sign-in is by app password (Record 5, item 1).

## Supporting Documentation

- `sdlc/artifacts/build/sprints/sprint-020/U-063-tls-socket-factory-characterisation-tests.md` (DP-2, AC-16, AC-23, SC-3, SC-5, Implementation Notes)
- `sdlc/artifacts/build/sprints/sprint-020/U-063/implementation-log.md`, section I3.7
- `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/I0-probe.txt`, `evidence/I0c-diag.txt`
- `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md` (silent on protocol versions)
- `sdlc/artifacts/build/change-records/CR-009-cntr-001-default-trusted-socket-factory-producer.md` (T5, `:213`)
- `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` (Q7.f, Q12.f, Records 4 to 6)
- RFC 8996, "Deprecating TLS 1.0 and TLS 1.1" (external; cited by the developer's draft, not re-read in this work)

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes

- Filed from U-063 (sprint-020) under its AC-23. The behaviour is recorded by two passing characterisation tests; neither is evidence of acceptability.
