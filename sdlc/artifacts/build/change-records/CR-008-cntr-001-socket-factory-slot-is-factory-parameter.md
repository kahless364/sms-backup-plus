---
status: approved
severity: medium
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-019
  - REQ-MODERNIZATION-002
  - REQ-MODERNIZATION-012
design_docs:
  - CNTR-MODERNIZATION-001
  - DES-MODERNIZATION-002
  - DES-MODERNIZATION-009
  - DES-MODERNIZATION-012
related_stories:
  - U-062
tags:
  - change-record
  - cntr-modernization-001
  - tls
  - thunderbird-migration
  - p-4
id: CR-008
title: 'CNTR-MODERNIZATION-001: the socket-factory slot is a parameter of ImapStore.create(...), not an argument of a super-constructor (P-4)'
type: DCR
domain: change-records
summary: 'P-4: CNTR-MODERNIZATION-001 describes the socket-factory slot as an argument of the ImapStore super-constructor; upstream''s ImapStore is created through a factory function and the slot is a parameter of that factory. Found by the accepted feasibility analysis (U-062).'
updated_at: '2026-11-17T18:01:48.643Z'
---

# CR-008: CNTR-MODERNIZATION-001 — the socket-factory slot becomes a parameter of `ImapStore.create(...)` (P-4)

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-008 |
| **Type** | DCR (Design Correction) |
| **Severity** | Medium |
| **Status** | Open |
| **Reported By** | Requirements Analyst agent, for the product owner (forced change record approved as a batch, Record 4 of the U-062 analysis) |
| **Date Reported** | 2026-11-17 |
| **Source** | Sprint work: U-062 feasibility analysis, item P-4 (analysis Q7.b, Q12.d PO-6, Q12.e condition 9) |

Sibling records against the same contract: CR-007 (P-3, the `createSocket` exception class) and CR-009 (P-5, producer 1). CR-007, CR-008 and CR-009 are applied together as one contract revision (see Timing).

---

## Finding

### Description

CNTR-MODERNIZATION-001 describes the place where the resolved socket factory enters the IMAP library as a **super-constructor argument**: the app's store class extends the library's `ImapStore` and passes the factory to `super(BackupStoreConfig, TrustedSocketFactory, ConnectivityManager)`. The contract uses that form in its diagram, its "App-owned wrapper note", its handoff signatures, its Invariant 3, its scenario C and its breaking-change rule.

At the pinned upstream commit, `ImapStore` is an **interface**. A store is created through a factory function in its companion, `ImapStore.create(serverSettings, config, trustedSocketFactory, oauthTokenProvider)`, and the socket factory is the third parameter [C-245]. The app cannot subclass an interface, so there is no super-constructor slot after the migration. The carried type is unchanged, so the contract's own breaking-change trigger (`:284-285`, "changing the carried type ... while the slot still expects it") is not met; what changes is the form of the slot.

This matters beyond wording for three reasons found while reading the code:

1. **The app has two subclasses of `ImapStore`, not one.** The analysis names the adapter's inner class (`K9MailTransport.BackupImapStoreDelegate`, `K9MailTransport.java:414`). `BackupImapStore` (`BackupImapStore.java:54`) also extends `ImapStore`. In production only the first is constructed (`K9MailTransport.java:121`); `BackupImapStore` is constructed only by `BackupImapStoreTest` (search below), and its static validators are still used by `MailModule.kt:80` and `AdvancedSettings.java:533`. The contract names `BackupImapStore` as the "current consumer", which is no longer where the production handoff happens.
2. **A test seam disappears.** The contract (`:161-163`) keeps `BackupImapStore.getTrustedSocketFactory()` as the assertion hook. It returns the inherited protected field `mTrustedSocketFactory` of the vendored `RemoteStore` (`RemoteStore.java:24`), which is the very slot the library uses. An interface has no inherited field. The surviving seam, `K9MailTransport.getTrustedSocketFactory()` (`K9MailTransport.java:347-349`), returns a field the adapter owns, so it shows what the adapter passed, not what the store holds. Today the production path is asserted only at adapter level (`K9MailTransportTest.java:68-76`, `:88`); only the legacy `BackupImapStore` is asserted at slot level (`BackupImapStoreTest.java:91`, `:104`, `:111`). The analysis already records that no test would show a new library routing both connection paths through the app's factory (Q7.i, last bullet).
3. **A constraint in an approved requirement may not survive.** REQ-MODERNIZATION-012 (`:75`) and DES-MODERNIZATION-012 (`:300`) say the package-private constructor `K9MailTransport(BackupImapStoreDelegate, TrustedSocketFactory)` (`K9MailTransport.java:93`) must not change. It holds only if a type named `BackupImapStoreDelegate` survives as a composition wrapper around the interface. That is a Part B design choice (Scope Concern below), not decided here.

Nothing is wrong today: the contract describes the live vendored code correctly, apart from the stale producer and consumer names listed under Pre-existing drift. This is a forward-looking design correction.

### Source of Truth

- **Accepted feasibility analysis**, `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`: Section 7 Q7.b "Slot in the store" (line 632) and the P-4 row (line 642); Q7.i (lines 726-734); Q12.d row PO-6 (line 1335); Q12.e conditions 9, 12 and 17 (lines 1357, 1360, 1365). Product owner decisions: Record 3 and Record 4 (lines 2151-2183).
- **Upstream at the pin** (commit `ac1f0e85f63c1b600ac443d1fb2d939bffff7de5`), as the analysis cites it: `ImapStore.kt` lines 26-34 [C-245]; the commit that made `ImapStore` an interface with a `create` factory, 2021-06-03 [C-470]. I did not re-read upstream.
- **The contract being corrected**: `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md`, status `approved`, version v1.

### Evidence

Contract text (all quoted from `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md`):

```
:68   now:    BackupImapStore(context, uri, TrustedSocketFactory)  → super(BackupStoreConfig, factory, ConnectivityManager)
:71   k-9 ImapStore opens the TLS connection using the factory's TrustManager — and only that one.
:98   > **App-owned wrapper note.** The carried type is the k-9 `TrustedSocketFactory` interface, not a
:99   > raw `javax.net.ssl.SSLSocketFactory`, because that is the slot the k-9 `ImapStore`
:100  > super-constructor accepts (`super(BackupStoreConfig, TrustedSocketFactory, ConnectivityManager)`,
:101  > verified `BackupImapStore.java:60-62`). `PinnedCertificateSocketFactory` is the *app-owned*
:146  public BackupImapStore(Context context, String uri, TrustedSocketFactory socketFactory)
:147          throws MessagingException;
:148  // super(new BackupStoreConfig(uri), socketFactory, (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE));
:161  The package-private test seam `BackupImapStore.getTrustedSocketFactory()` (verified
:162  `BackupImapStore.java:116-118`, returns `mTrustedSocketFactory`) is preserved as the assertion hook
:163  for both designs; DES-009 re-homes it onto `K9MailTransport`.
:177  3. **Consumer never re-decides.** The transport does not inspect, unwrap, replace, relax, or
:178     supplement the TrustManager inside the factory. It passes the factory verbatim to the k-9
:179     `ImapStore` super-constructor (now) or stores it for connection (adapter, later). No conditional
:180     in the consumer chooses between trust postures.
:276  // super(new BackupStoreConfig(cfg.uri), factory, connectivityManager);  // same k-9 slot, same invariant
:284  - Changing the carried type away from `com.fsck.k9.mail.ssl.TrustedSocketFactory` while the k-9
:285    `ImapStore` super-constructor slot still expects it.
```

Live code, read this session:

| What | File and line | Text |
|------|---------------|------|
| Legacy store subclass | `app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java:54`, `:71-76` | `public class BackupImapStore extends ImapStore {` ... `super(new BackupStoreConfig(uri), socketFactory, (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE));` |
| Test seam on the store's own slot | `BackupImapStore.java:129-131` | `/* package, for testing */ TrustedSocketFactory getTrustedSocketFactory() { return mTrustedSocketFactory; }` |
| Production store subclass | `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java:414`, `:418-422` | `static class BackupImapStoreDelegate extends ImapStore {` ... `super(new BackupStoreConfig(uri), socketFactory, (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE));` |
| Production handoff | `K9MailTransport.java:119-125` | `this.resolvedSocketFactory = buildSocketFactory(context, config);` then `this.store = new BackupImapStoreDelegate(context, config.storeUri, this.resolvedSocketFactory);` |
| Adapter-owned seam | `K9MailTransport.java:347-349` | `TrustedSocketFactory getTrustedSocketFactory() { return resolvedSocketFactory; }` |
| Where the slot lives in the library | `k9mail-vendored/.../store/RemoteStore.java:24`, `:32-35`; `store/imap/ImapStore.java:75-77`, `:372` | `protected TrustedSocketFactory mTrustedSocketFactory;` ... `public ImapStore(StoreConfig storeConfig, TrustedSocketFactory trustedSocketFactory, ConnectivityManager connectivityManager)` ... `return new ImapConnection(new StoreImapSettings(), mTrustedSocketFactory, connectivityManager);` |
| The two places the factory is used | `k9mail-vendored/.../store/imap/ImapConnection.java:209` and `:315` | `socket = socketFactory.createSocket(null, host, port, clientCertificateAlias);` (implicit TLS) and `socket = socketFactory.createSocket(socket, host, port, clientCertificateAlias);` (after STARTTLS) |

Upstream side, quoted from the accepted analysis (line 632 and line 642; not re-measured here):

> `ImapStore` is an interface; the factory is the third parameter of the companion function `create(serverSettings, config, trustedSocketFactory, oauthTokenProvider)` [C-245]. The app's `BackupImapStoreDelegate extends ImapStore` (`K9MailTransport.java:414`) cannot subclass an interface.

> P-4 ... The store accepts the factory as a parameter of `ImapStore.create(...)`, not through a super-constructor; the app cannot subclass `ImapStore`.

---

## Triage

### Classification

- **Type:** DCR (Design Correction). The contract does not match the source of truth for the code the product owner decided to consume.
  - Not a DEF: the implementation matches the contract today.
  - Not an RCR: the requirement (a resolved, never-trust-all factory reaches the transport and is not re-decided there) does not change. Only the mechanical form of the handoff does.
- **Root:** Design and contracts layer. The contract specified the handoff as an inheritance mechanism (a super-constructor argument) of a vendored class instead of stating the invariant (the store is built with exactly the resolved factory) and naming the mechanism as a library binding.
- **Layers Affected:** 4. Design and contracts (the text change); application code, tests and the vendored library when the migration lands. The text change by this record touches 1 layer.
- **Severity Justification:** Medium (isolated impact, non-blocking). Nothing is blocked: no migration story exists, U-062 is done and sprint-019 is closed. It is not low because the change is structural and not just a rename: two app classes lose their base class, one test seam stops existing, 5 test files build or subclass the store, and a requirement constraint (REQ-012:75) needs a design answer. It is not high because no open story is affected and the contract is the only artifact this record changes. The "5+ files" criterion for critical is read as files this record changes (one); the migration footprint (listed below) is Part B's work and nothing is blocked.

---

## Impact Analysis

> `change_records.layers` is not configured in `sdlc/config.yaml`. The layers below are the ones given for this batch.

Checklist run: `knowledge/checklists/impact-analysis-checklist.md`. No `[TBC]` placeholders are involved.

Ground truth (layer 1, read-only): `ImapStore.kt` at the pin, lines 26-34 [C-245]: `create(serverSettings, config, trustedSocketFactory, oauthTokenProvider)`. Correct value: the socket factory is the third parameter of the store factory; `ImapStore` is an interface.

### Searches run

| # | Search | Path | Result |
|---|--------|------|--------|
| 1 | `extends ImapStore` | `app/src` | 2 classes: `BackupImapStore.java:54`, `K9MailTransport.java:414` |
| 2 | `new BackupImapStore(` | `app/src/main` | 0 (production never constructs it); `app/src/test`: 9 sites in `BackupImapStoreTest.java` |
| 3 | `BackupImapStore\b\|resolveSocketFactory\|getTlsTrustPolicy\|new K9MailTransport` | `app/src/main` | static validators used by `MailModule.kt:80` and `AdvancedSettings.java:533`; policy read at `MailModule.kt:87`, `AdvancedSettings.java:503`; no `resolveSocketFactory` |
| 4 | `getTrustedSocketFactory` | `app/src/test` | 5 call sites: `BackupImapStoreTest.java:91,104,111`, `K9MailTransportTest.java:75,88` |
| 5 | `BackupImapStoreDelegate(` and `TestableBackupImapStoreDelegate` | `app/src/test` | `BackupImapStoreDelegateIntegrationTest.java:93,146`; `K9MailTransportTest.java:345`; `K9MailTransportPortMethodsTest.java:47`; `K9MailTransportCreateOpenTest.java:86,260,631` and a subclass at `:655-671` |
| 6 | `TrustedSocketFactory` (count) | `app/src/test` | 63 occurrences in 6 files: `BackupImapStoreTest` 14, `DefaultTrustedSocketFactorySniTest` 13, `K9MailTransportPortMethodsTest` 16, `K9MailTransportTest` 10, `K9MailTransportCreateOpenTest` 5, `BackupImapStoreDelegateIntegrationTest` 5 |
| 7 | `RemoteStore\|Transport.getInstance\|SmtpTransport\|Pop3Store` | `app/src` | 0 (the vendored library's own factories are never called by the app) |
| 8 | `TrustedSocketFactory\|DefaultTrustedSocketFactory\|CNTR-MODERNIZATION-001\|CNTR-001` | `sdlc/artifacts/design`, `sdlc/artifacts/requirements` | Design files that restate the slot or the factory: CNTR-001, DES-002, DES-009, DES-012, CNTR-007, CNTR-002 (lines in the matrix below). DES-003, DES-006 and DES-013 also match, but only on the deleted trust-all class or on a generic "all contracts approved" statement (DES-013 `:366,375,508`); none restates the slot. Requirements: REQ-002, REQ-003, REQ-012, REQ-019, EPIC-001 (only REQ-012 and REQ-019 bear on the slot) |
| 9 | `TrustedSocketFactory\|ssl` | `app/build.gradle`; also `*.pro`, `*.gradle`, `*.kts`, `*.xml` repository-wide | 0 build or shrinker rules name the class; all other hits are evidence files under `sdlc` |

### Layer Impact Matrix

| # | Layer | Affected? | Files and lines | Occurrences | Action Needed |
|---|-------|-----------|-----------------|-------------|---------------|
| 1 | Ground truth: upstream at the pin (external) | — | `ImapStore.kt:26-34` [C-245] | — | Ground truth (read-only) |
| 2 | Requirements | Cross-reference | `REQ-MODERNIZATION-012-seal-mailtransport-acl-exception-boundary.md:75` (package-private constructor "must not be changed"; names `MailTransportTestFactories` as its user, which does not reference it: `MailTransportTestFactories.java` has no `K9MailTransport` or `Delegate` call, so that citation is already stale). `REQ-MODERNIZATION-019:48` (inventory row for `BackupImapStore.java`) correct as written. REQ-002 `:110` ("`DefaultTrustedSocketFactory` or an equivalent") is unaffected | 1 constraint to resolve in Part B | Scope Concern (below); no text change by this record |
| 3 | Design and contracts | **Yes (root)** | `CNTR-MODERNIZATION-001`: `:68`, `:71`, `:98-101`, `:146-148`, `:161-163`, `:177-180`, `:276`, `:284-285` (analysis cited `:98-101`, `:276`, `:284-285`; the others restate the same slot and were found by this record's search). Other design documents that restate the slot: `DES-MODERNIZATION-002:174-176` ("drops into the same `ImapStore` super-constructor slot"), `:345-350` (flow diagram), `:395` ("`BackupImapStore` super-constructor"), `:540`; `DES-MODERNIZATION-009:331-336` (the adapter builds the factory; unaffected in substance); `DES-MODERNIZATION-012:300` (test constructor). `CNTR-MODERNIZATION-007:73,145,165-166,269,434` state that no `TrustedSocketFactory` crosses the port; unaffected | 8 contract line groups; 4 lines in DES-002 | Contract: reword at apply time. DES-002 and DES-009 are approved records of the as-built design: leave unedited and let the contract changelog state that v2 supersedes their slot wording (Solution Architect to confirm; editing them is a separate approval cycle) |
| 4 | Stories and sprints | No in-progress work | Closed records only (U-008, U-010, U-025, U-026, U-030, U-052, U-062 and others); none open | 0 open | None |
| 5 | Application code | Yes, at migration | `mail/BackupImapStore.java:29,54,71-76,129-131` (class, constructor, seam); `mail/transport/K9MailTransport.java:93` (test constructor), `:119-125`, `:414-422`, `:478-481` (delegate seam), `:70-82` (javadoc: "an `ImapStore` subclass"). The `forceFreshConnection` override seam (`K9MailTransport.java:556-558`, `:669`) also depends on subclassing; the analysis re-makes it by composition (Q5.p, `openFolder` row) | 2 classes lose a base class; 1 constructor to keep or change | Part B design: composition around `ImapStore.create(...)`. Not done by this record |
| 6 | Tests | Yes, at migration | Searches 2, 4, 5 and 6: 5 files construct or subclass the store (`BackupImapStoreTest`, `BackupImapStoreDelegateIntegrationTest`, `K9MailTransportTest`, `K9MailTransportPortMethodsTest`, `K9MailTransportCreateOpenTest`): 9 `new BackupImapStore(` sites, 6 delegate construction sites, one subclass (`:655-671`), 5 seam call sites. A 6th file, `DefaultTrustedSocketFactorySniTest`, references the default factory only and belongs to CR-009. 63 `TrustedSocketFactory` occurrences over all 6 | 5 files for the slot | Retarget in Part B stories. The slot-level assertion must be replaced by a recording-factory test (Proposed Change, `:161-163`) before the old seam tests are retired |
| 7 | Vendored library | Yes, disappears | `store/RemoteStore.java:24,32-35` (slot field and constructor); `store/imap/ImapStore.java:75-77,372`; `store/imap/ImapConnection.java:67,82,91,209,315`. Not used by the app but also carrying the slot: `store/pop3/Pop3Store.java:224`, `transport/SmtpTransport.java:188`, `store/RemoteStore.java:51-56` and `Transport.java:26` (they build their own `DefaultTrustedSocketFactory`; search 7 shows the app never calls them) | 8 files | None here. The files leave with the module |
| 8 | Documentation | No | `README.md`: 0 matches; `sdlc/artifacts/engagement`: no match in search 8 | 0 | None |
| | **Total** | | | | 1 contract (8 line groups) at apply time; 2 production classes and 5 test files at migration |

### Pre-existing drift relevant to reading this record (reported, not fixed here)

- The producer named in the contract, `ServiceBase.getBackupImapStore()`, no longer exists (no `ServiceBase` file under `app/src`). Resolution is in `di/MailModule.kt:78-98` and `K9MailTransport.buildSocketFactory` (`K9MailTransport.java:135-149`), so the production consumer is the adapter, and `BackupImapStore` is a test-only constructor target. The contract's "now: `BackupImapStore(...)` / later: `K9MailTransport(...)`" labels are therefore reversed in practice.
- Line citations into `BackupImapStore.java` in the contract (`:58-63`, `:60-62`, `:116-118`) are stale.

---

## Proposed Change

Wording changes to apply to `CNTR-MODERNIZATION-001` when the timing condition is met. Nothing is applied by this record. The exact parameter list of `ImapStore.create(...)` is as at the pin; the fork may add parameters (for example the patches the product owner chose in Record 4), but the factory stays a parameter, which is all the contract needs to say.

### Lines `:98-101` (wrapper note; analysis-cited)

Current text:
```
> **App-owned wrapper note.** The carried type is the k-9 `TrustedSocketFactory` interface, not a
> raw `javax.net.ssl.SSLSocketFactory`, because that is the slot the k-9 `ImapStore`
> super-constructor accepts (`super(BackupStoreConfig, TrustedSocketFactory, ConnectivityManager)`,
> verified `BackupImapStore.java:60-62`). `PinnedCertificateSocketFactory` is the *app-owned*
```
Proposed text:
```
> **App-owned wrapper note.** The carried type is the library's `TrustedSocketFactory` interface, not a
> raw `javax.net.ssl.SSLSocketFactory`, because that is the slot the library's store factory accepts:
> `ImapStore.create(serverSettings, config, trustedSocketFactory, oauthTokenProvider)` takes it as its
> third parameter. `ImapStore` is an interface, so the app does not subclass it and there is no
> super-constructor slot. `PinnedCertificateSocketFactory` is the *app-owned*
```

### Line `:276` (scenario C; analysis-cited)

Current text:
```
// super(new BackupStoreConfig(cfg.uri), factory, connectivityManager);  // same k-9 slot, same invariant
```
Proposed text:
```
// store = ImapStore.create(serverSettings, config, factory, oauthTokenProvider);  // same slot (third parameter), same invariant
```
(`serverSettings` and `config` stand for the values the adapter builds from `cfg.uri`; their construction, including the connection-security mapping of analysis Q7.h and condition 17, is Part B design.)

### Lines `:284-285` (breaking-change rule; analysis-cited)

Current text:
```
  - Changing the carried type away from `com.fsck.k9.mail.ssl.TrustedSocketFactory` while the k-9
    `ImapStore` super-constructor slot still expects it.
```
Proposed text:
```
  - Changing the carried type away from `com.fsck.k9.mail.ssl.TrustedSocketFactory` while the library's
    store factory (`ImapStore.create`, third parameter) still expects it.
```

### Lines found by this record's search (not in the analysis's list)

`:68` and `:71` (diagram). Current text: `now: BackupImapStore(context, uri, TrustedSocketFactory)  → super(BackupStoreConfig, factory, ConnectivityManager)` and `k-9 ImapStore opens the TLS connection using the factory's TrustManager — and only that one.` Proposed text: `adapter: K9MailTransport(MailTransportConfig{uri, TlsTrustPolicy})  → ImapStore.create(serverSettings, config, factory, oauthTokenProvider)` and `the library's ImapStore opens the TLS connection using the factory's TrustManager — and only that one.` The `now:`/`later:` pair collapses to the adapter line because the adapter is already the production consumer.

`:146-148` (handoff signature, "AFTER" block). This block specifies the `BackupImapStore` constructor with the factory as an argument. Proposed: replace the block with the adapter handoff and the sentence "the store class does not receive the factory in a constructor; the adapter passes it to `ImapStore.create`". Whether the class `BackupImapStore` survives (only its static validators are used in production) is a Part B design question, not decided here.

`:177-180` (Invariant 3). Current: `It passes the factory verbatim to the k-9 ImapStore super-constructor (now) or stores it for connection (adapter, later).` Proposed: `It passes the factory verbatim as the factory parameter of the library's store factory (ImapStore.create).` The remaining sentences are unchanged.

`:161-163` (test seam). Current: `The package-private test seam BackupImapStore.getTrustedSocketFactory() (verified BackupImapStore.java:116-118, returns mTrustedSocketFactory) is preserved as the assertion hook for both designs; DES-009 re-homes it onto K9MailTransport.` Proposed:
```
The package-private seam K9MailTransport.getTrustedSocketFactory() returns the factory the adapter
resolved and passed to ImapStore.create. A seam on the store itself no longer exists: ImapStore is an
interface with no inherited field. Because the seam now shows what the adapter passed rather than what
the store holds, the store's use of that factory on both connection paths (implicit TLS, and after
STARTTLS) is shown by a test with a recording factory against a loopback server (analysis conditions 12 and 17).
```
This keeps the property the old seam gave on the legacy class (the store's own slot holds the resolved factory) by naming the test that now provides it; it adds no new requirement on the producer.

### Guarantee check: what each reworded line carries, and that it is unchanged

| Guarantee | Where stated | Carried by the lines reworded here? | Result |
|-----------|--------------|-------------------------------------|--------|
| Certificate validation | `:90-96`, Invariant 2 `:173-176` | No. The slot lines say how the factory reaches the store; validation is inside the factory | Unchanged |
| Hostname verification | `:95` | No | Unchanged |
| SNI, including no SNI for IP literals | Not stated anywhere in the contract (0 matches for `SNI`, `server name`, `IP literal`) | No | Unchanged. SNI is decided inside the factory at the two call sites (`ImapConnection.java:209`, `:315`); the slot rewording does not move that decision |
| Pinning and user-trusted certificates | `:96`, `:181-184`, `:215-216` | No | Unchanged. Both producers still fit the slot: the parameter type is the interface (analysis Q7.b, last row) |
| No silent fallback / consumer never re-decides | Invariant 3 `:177-180`, `:200-202` | **Yes: Invariant 3 and the seam paragraph** | Preserved. Invariant 3 keeps "passes the factory verbatim" and "no conditional in the consumer chooses between trust postures". The seam paragraph replaces a slot-level assertion with a named test, so the proof does not weaken silently |

One difference of kind is stated openly. A super-constructor argument is the only way a subclass can set the slot, so the inheritance form made "the factory is the one the store uses" true by construction. A factory parameter makes it true by the adapter calling `create` with the right argument. The equivalence therefore depends on two things the rewording does not by itself provide: that the adapter is the only caller of `ImapStore.create` in `app/src/main`, and that a test observes the factory being called on both paths.

**Scope Concern (raised, not decided; for the Solution Architect and product owner):** (a) whether the revision should add a verifiable statement that `app/src/main` contains exactly one `ImapStore.create(` call, which is a strengthening, not part of the equivalence; (b) whether `BackupImapStoreDelegate` survives as a composition wrapper so that REQ-MODERNIZATION-012:75 and DES-MODERNIZATION-012:300 ("must not change") still hold, or whether a change record against REQ-012 is needed; (c) where the certificate-blocklist decision (Record 4) touches the slot: it does not. The blocklist lives inside the trust manager that the factory installs, not in the slot's form. It is raised under CR-009 (trust-manager construction) and CR-007 (`:196`).

---

## Timing

- **When to apply:** with the story or stories that make it true, in **one** revision of CNTR-MODERNIZATION-001 (v1 to v2) together with CR-007 and CR-009. The trigger is the Part B migration story that replaces `extends ImapStore` with `ImapStore.create(...)` in `K9MailTransport`. Part B design is not blocked by waiting: it cites the approved CR-008 as the target form (analysis condition 9 asks for "CR-nnn approved", not "applied").
- **Why not now:** the contract is `approved` and describes live code that is still a subclass of the vendored `ImapStore`; the sprint contract verifications compare it with code. Writing the factory-parameter form now makes the contract false for every TLS fix made between now and the migration. The CR-002 precedent (contract amended before the code) does not apply: there the narrowed signatures could be written by queued stories under an approved requirement, whereas this form cannot exist until the toolchain requirements (analysis condition 1) and the consumption proof are done.
- **Must not happen before then:**
  - Any edit to `CNTR-MODERNIZATION-001` text.
  - Removing or rewriting the `getTrustedSocketFactory()` seams or their tests (`BackupImapStoreTest.java:91,104,111`; `K9MailTransportTest.java:75,88`) in the name of this record. They are the only slot-level and adapter-level assertions the repository has.
  - Any change to `BackupImapStore` or `BackupImapStoreDelegate` made in the name of this record.
  - Retiring the old seam tests before the recording-factory test exists and passes.
  - Adding a second place that builds a store with a library-supplied default factory.
- **Together or separately:** together. Line `:276` of the contract is in the same code block as `:275`, which CR-009 rewrites, and `:98-101` sits next to CR-007's `:85-86`. Applying the three records in separate passes leaves scenario C and the carried-type block half rewritten. If the Part B design splits the code changes across stories, apply the revision at the last of them and have each earlier story name the deviation from the contract text.

---

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory only — refined during story planning.

**What the slot protects, and what the rewording loses.** Invariant 3, "consumer never re-decides" (contract lines 177-180), held by construction under inheritance, because the super-constructor was the only way to set the slot. With `ImapStore.create(...)` it holds only by convention: the adapter must pass the resolved factory, and nothing else may call `create`. The words can stay the same while the mechanism gets weaker, and the record says so openly. Two controls bring it back:
- Adopt Scope Concern (a): a checkable statement that `app/src/main` contains exactly one `ImapStore.create(` call. It costs a grep and turns the convention into a fitness function. I recommend it.
- The recording-factory loopback test on both connection paths (implicit TLS and STARTTLS; analysis conditions 12 and 17) exists and passes before the old seam tests are retired. The record's ordering rule here is right.

DCR and Medium: agreed.

**Pattern for Part B.** Replace subclassing with composition: an app-owned wrapper (GoF Adapter) holds the `ImapStore` that `create` returns and rebuilds the `forceFreshConnection` seam (`K9MailTransport.java:556-558`). If the wrapper keeps the name `BackupImapStoreDelegate`, REQ-MODERNIZATION-012 line 75 still holds literally. That line is already stale, though: it names `MailTransportTestFactories`, which does not use the constructor (record, matrix row 2). Do not let a class name drive the design. Record the lapse in one REQ-012 supersession note shared with CR-005 and CR-006.

**`BackupImapStore`.** Production never constructs it; only its static validators are used (`MailModule.kt:80`, `AdvancedSettings.java:533`). Under CR-005's Rule 1 it cannot stay in `mail/` with any library import. That answers the record's open question: move the validators to an app-owned class, then delete the subclass and its tests.

**Cross-record.**
- The fourth parameter of `create` is CR-006's token provider (analysis line 632), so the adapter is the single construction site for both. Keep `oauthTokenProvider` in CNTR-MODERNIZATION-001's scenario C as a placeholder only, because that parameter belongs to CNTR-MODERNIZATION-007.
- Validation Rule 3 (contract lines 213-214, "pass it to the k-9 `ImapStore` connection path") restates the slot and is not in this record's line list. Add it to the v2 revision.
- Lines 275 and 276 share a code block with CR-009, so the three records go into one revision.

**Story shape and order.** No story of its own. The code is the adapter-rewrite story of Part B, which follows the toolchain requirements (condition 1) and the consumption proof. The text goes into the combined v2, under the timing policy proposed in CR-007's section.

**Risk.** The fork may add parameters to `create` (for example for the patched read timeout). The contract needs to say only that the factory is a parameter, which the proposed wording already does.

---

## Cascade Analysis

**Root Layer:** Design and contracts (CNTR-MODERNIZATION-001 v1)

**Root Defect:** The contract expresses the handoff as an inheritance mechanism of a vendored class (a super-constructor argument) instead of as an invariant plus a library binding. When the library turns the class into an interface, the mechanism the contract names, and the test seam built on the inherited field, no longer exist.

**Cascade Direction:** Mid-stream down (contract, then restating design documents that need no edit, then two classes and 5 test files at migration time)

### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Design and contracts (CNTR-001) | Design (DES-002, DES-009, DES-012) | Yes, as restated history | DES-002 `:174-176`, `:345-350`, `:395`; DES-009 `:331-336`; DES-012 `:300`. Approved as-built records; not edited |
| 2 | Design and contracts | Requirements | Cross-reference only | REQ-012 `:75` constraint on the test constructor (Scope Concern) |
| 3 | Design and contracts | Application code | Yes, at migration | `BackupImapStore.java:54,71-76`; `K9MailTransport.java:414-422` |
| 4 | Application code | Tests | Yes, at migration | Searches 2, 4, 5, 6: 5 files construct or subclass the store, 5 seam call sites |
| 5 | Application code | Vendored library | Yes, disappears | `RemoteStore.java:24`, `ImapStore.java:75-77`, `ImapConnection.java:67,209,315` |
| 6 | Design and contracts | Documentation | No | `README.md` and engagement folder: no match |

### Fix Order

1. [ ] Design and contracts: apply the CR-008 wording in the combined CNTR-MODERNIZATION-001 revision, ROOT FIX
2. [ ] Application code: composition around `ImapStore.create(...)` in the Part B migration story, CASCADE FIX
3. [ ] Tests: add the recording-factory test (both connection paths), then retarget the 5 test files, CASCADE FIX; the seam tests are retired only after the new test passes
4. [ ] Verification: the contract contains no `super-constructor`; `grep -rn "extends ImapStore" app/src` returns 0; exactly one `ImapStore.create(` call in `app/src/main` (if the Solution Architect adopts that check)

### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| None | — | — | No open story. Hold nothing |

---

## Plan

- **Execution Window:** Backlog. Approval now (a precondition of Part B design); text applied with the Part B migration story, in the combined revision.
- **Assigned To:** TBD
- **Estimated Effort:** About 1 hour for the contract text; the code and test work is the Part B migration stories' own
- **Sprint:** None scheduled

### Fix Checklist

- [ ] Design and contracts: reword `CNTR-MODERNIZATION-001` `:68`, `:71`, `:98-101`, `:146-148`, `:161-163`, `:177-180`, `:276`, `:284-285` as in Proposed Change, in the combined revision
- [ ] Design and contracts: changelog line stating that v2 supersedes the slot wording in DES-002 and DES-009; set `change_records` frontmatter (Artifact Librarian)
- [ ] Application code: composition around `ImapStore.create(...)` (Part B story)
- [ ] Tests: recording-factory test on both paths, then retarget the 5 test files (Part B stories)
- [ ] Verification: 0 remaining mentions of the super-constructor slot in the contract

## Next Steps

CR filed and queued for sprint planning. Run `/amp:create-stories --from-cr CR-008` when ready to schedule.

Read this with the batch decision: CR-008 is a precondition of Part B design (analysis condition 9, row 6 of Section 11), not a story to schedule on its own. The code and test work it describes is part of the Part B migration stories; its contract text is applied in the combined revision with CR-007 and CR-009. Approval of the record is the condition; do not create a stand-alone repair story for it.

---

## Execution

### Changes Made

| File | Change | Layer |
|------|--------|-------|
| *(none yet)* | Pending the timing above | — |

### Verification Evidence

*(Pending)*

### Commit Traceability

| Commit | Date | Branch | Scope | Author |
|--------|------|--------|-------|--------|
| *(none yet)* | | | | |

---

## Closure

- **Root Cause Category:** Contract coupled to a library mechanism (an inheritance-based injection point of a vendored class stated as part of the normative handoff).
- **Prevention Recommendation:** State the invariant ("the store is built with exactly the factory the producer resolved, and no other") separately from the mechanism, and name the mechanism in a library-binding block with the version it was verified against. Where a test seam reads a library-owned field, say what independent proof replaces it if the library changes the field away.
- **Closed By:** (pending)
- **Date Closed:** (pending)
