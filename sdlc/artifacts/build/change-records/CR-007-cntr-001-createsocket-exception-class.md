---
status: approved
severity: low
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-019
  - REQ-MODERNIZATION-002
design_docs:
  - CNTR-MODERNIZATION-001
related_stories:
  - U-062
tags:
  - change-record
  - cntr-modernization-001
  - tls
  - thunderbird-migration
  - p-3
id: CR-007
title: 'CNTR-MODERNIZATION-001: the createSocket signature names the vendored MessagingException; the Thunderbird migration replaces it with a different class (P-3)'
type: DCR
domain: change-records
summary: 'P-3: CNTR-MODERNIZATION-001 names MessagingException in the createSocket signature; after the migration that exception is a different class (it has left the package com.fsck.k9.mail upstream). Found by the accepted feasibility analysis (U-062).'
updated_at: '2026-11-17T18:01:48.635Z'
---

# CR-007: CNTR-MODERNIZATION-001 — `createSocket` signature names the vendored `MessagingException` (P-3)

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-007 |
| **Type** | DCR (Design Correction) |
| **Severity** | Low |
| **Status** | Open |
| **Reported By** | Requirements Analyst agent, for the product owner (forced change record approved as a batch, Record 4 of the U-062 analysis) |
| **Date Reported** | 2026-11-17 |
| **Source** | Sprint work: U-062 feasibility analysis, item P-3 (analysis Q7.b, Q12.d PO-5, Q12.e condition 8) |

Sibling records raised in the same batch against the same contract: CR-008 (P-4, the socket-factory slot) and CR-009 (P-5, producer 1). Related, different contract: CR-005 (P-1, package strings in CNTR-MODERNIZATION-007), whose wording for "the new package of the moved types" this record must match.

---

## Finding

### Description

CNTR-MODERNIZATION-001 defines the one object that carries the TLS trust decision into the IMAP transport: a `TrustedSocketFactory` whose `createSocket(...)` method is declared as throwing `NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException`. In the contract, and in all the code it was written against, `MessagingException` means the vendored class `com.fsck.k9.mail.MessagingException`.

The product owner has decided on the full migration to Thunderbird for Android's mail libraries (Record 3), consumed from a fork built as Android variants (Record 4). At the pinned upstream commit the interface `com.fsck.k9.mail.ssl.TrustedSocketFactory` keeps its name, its package and its parameter and return types. The `MessagingException` in its `throws` list is a different class: `net.thunderbird.core.common.exception.MessagingException`. Upstream moved it out of `com.fsck.k9.mail` in a commit dated 2025-06-05. After the migration, the words "throws ... `MessagingException`" in the contract would name a class the build no longer contains, or be read as the old class.

Nothing is wrong today. The vendored interface and the app's only implementer in `app/src/main` declare the same class the contract names (see Evidence), so the contract is correct for the live code. This is a forward-looking design correction: the contract will become incorrect when the migration lands, and the accepted analysis (condition 8) requires this record to be approved before Part B design starts.

This record changes contract text only. It does not change what the factory validates, which hosts it accepts, or how it fails. The security guarantees of the contract are unchanged (Proposed Change, "Guarantee check").

### Source of Truth

- **Accepted feasibility analysis**, `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`: Section 7 Q7.b (the measurement table, lines 626-635, and the P-3 row, line 641), Section 12 Q12.d row PO-5 (line 1334), Section 12 Q12.e condition 8 (line 1356). Product owner decisions: Record 3 and Record 4 (lines 2151-2183). Note on location: the P-item table in Section 5 Q5.p (about line 546) lists only P-1 and P-2; P-3 to P-5 are recorded in Section 7 Q7.b and listed in Q12.d.
- **Upstream at the pin** (thunderbird-android commit `ac1f0e85f63c1b600ac443d1fb2d939bffff7de5`), as the analysis cites it: `TrustedSocketFactory.kt` lines 9-17 [C-067]; `MessagingException` in `net.thunderbird.core.common.exception` [C-304]; the commit that moved it, 2025-06-05 [C-475]. I did not re-read upstream; these facts are taken from the accepted analysis.
- **The contract being corrected**: `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md`, status `approved`, version v1.

### Evidence

Contract text, quoted from `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md`:

```
:84  Interface: com.fsck.k9.mail.ssl.TrustedSocketFactory   (k-9 library interface; verified import BackupImapStore.java:30)
:85  Method:    Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias)
:86               throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException
```

```
:196 | Untrusted / expired / hostname-mismatched chain under `SYSTEM_VALIDATED` | `DefaultTrustedSocketFactory`'s TrustManager throws `CertificateException`, surfaced from `createSocket(...)` (wrapped per k-9 as `MessagingException`) | Connection fails closed. The transport propagates `MessagingException` (DES-009 translates it to an app-owned `MailException`/`TemporaryImapException` at the ACL boundary). |
```

Live code that matches the contract today (read this session):

| What | File and line | Text |
|------|---------------|------|
| Vendored interface | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustedSocketFactory.java:3` and `:10-13` | `import com.fsck.k9.mail.MessagingException;` ... `Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias) throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException;` |
| Vendored default implementer | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java:152-153` | `public Socket createSocket(...) throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException {` |
| App-owned implementer | `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:19` and `:75-76` | `import com.fsck.k9.mail.MessagingException;` ... `public Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias) throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException {` |

Upstream side, quoted from the accepted analysis (line 631 and line 641; not re-measured here):

> `fun createSocket(socket: Socket?, host: String, port: Int, clientCertificateAlias: String?): Socket` with `@Throws(NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException)`, where `MessagingException` is `net.thunderbird.core.common.exception.MessagingException` [C-067, C-304]. Erased Java parameter and return types are identical.

> P-3 ... `MessagingException` in the `throws` clause is `net.thunderbird.core.common.exception.MessagingException`, not `com.fsck.k9.mail.MessagingException`; the interface name and the erased parameter types are unchanged.

Searches run for this record are listed in Impact Analysis.

---

## Triage

### Classification

- **Type:** DCR (Design Correction). The design contract does not match the source of truth for the code the product owner has decided to consume (upstream's interface at the pin).
  - Not a DEF: the implementation matches the contract today. Verified: both implementers and the interface declare the class the contract names (Evidence table).
  - Not an RCR: no behaviour, scope or client request changes. REQ-MODERNIZATION-019 already approves the migration; the TLS guarantees stay as they are.
- **Root:** Design and contracts layer. The contract wrote a library-internal class name, with the package it had when the contract was written, into the signature of a security boundary. The contract was authored against the vendored copy and stated the library's type as if it were part of the app's own interface.
- **Layers Affected:** 3. Design and contracts (the text change, one file); application code (one import and one `throws` clause in `PinnedCertificateSocketFactory.java`) and the vendored library (the interface and the default implementer, which leave with the module) when the migration lands. The text change by this record touches 1 layer.
- **Severity Justification:** Low. The criterion for low is "documentation-only or future impact"; both hold. Nothing is blocked (no migration story exists; U-062 is done and sprint-019 is closed). The wording change is one clause of one contract and one table cell. The code consequence is one import in one class, and a wrong import cannot survive: after the migration the old class does not exist, so the compiler reports it. The criterion for critical ("affects 5+ files") is read as the files this record itself changes (one), not the future footprint of the migration, which belongs to Part B stories. If the product owner reads that criterion by the migration's footprint, this record would still not be critical, because nothing is blocked.

---

## Impact Analysis

> `change_records.layers` is not configured in `sdlc/config.yaml`. The layers below are the ones given for this batch: requirements, design and contracts, stories and sprints, application code, tests, vendored library, documentation.

Checklist run: `knowledge/checklists/impact-analysis-checklist.md` (per-layer search, ground-truth identification, impact summary, cascade analysis). The `[TBC]` placeholder question does not arise (no placeholders involved).

Ground truth (layer 1, read-only): upstream's `TrustedSocketFactory` at the pin, `mail/common/src/main/java/com/fsck/k9/mail/ssl/TrustedSocketFactory.kt` lines 9-17 [C-067], with `MessagingException` at `net.thunderbird.core.common.exception` [C-304]. Correct value: the `throws` list names the library's `MessagingException`, which at the pin is the `net.thunderbird.core.common.exception` class.

### Searches run

| # | Search (tool: ripgrep through the Grep tool) | Path | Result |
|---|----------------------------------------------|------|--------|
| 1 | `createSocket` | `sdlc/artifacts/requirements` | 0 files |
| 2 | `createSocket\|NoSuchAlgorithmException\|KeyManagementException` | `sdlc/artifacts`, feasibility folder excluded | 28 files: `CNTR-MODERNIZATION-001` (the only normative document), this record, and 26 sprint, story, review and evidence records of finished work |
| 3 | `createSocket\|TrustedSocketFactory` | `app/src/main` | 6 files; production `createSocket` declarations: `PinnedCertificateSocketFactory.java:75` only (the other `createSocket` hit, `PinCertificateEnrollmentFlow.java:349`, is `javax.net.ssl.SSLSocketFactory.createSocket(host, port)` on a capture-only trust manager, not this interface) |
| 4 | `implements TrustedSocketFactory\|new TrustedSocketFactory(\|mock(TrustedSocketFactory` | `app/src`, `k9mail-vendored/src` | 2 implementers: `PinnedCertificateSocketFactory.java:55`, `DefaultTrustedSocketFactory.java:33` (vendored). 0 anonymous implementations, 0 test doubles |
| 5 | `MessagingException` | `app/src/main` | 82 occurrences in 13 files. These belong to the general package move (CR-005 / P-1), not to this clause; only `PinnedCertificateSocketFactory.java` (2) is on the `createSocket` signature |
| 6 | `TrustedSocketFactory\|CNTR-MODERNIZATION-001\|CNTR-001` | `README.md` | 0 matches |
| 7 | `MessagingException` | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-002-transport-security-hardening-design.md` | 1 match, `:71`, in the quoted "before" code of the boolean constructor that DES-002 removed (history) |

### Layer Impact Matrix

| # | Layer | Affected? | Files and lines | Occurrences | Action Needed |
|---|-------|-----------|-----------------|-------------|---------------|
| 1 | Ground truth: upstream at the pin (external) | — | `TrustedSocketFactory.kt:9-17` [C-067]; `MessagingException.kt` [C-304] | — | Ground truth (read-only). Not re-measured by this record |
| 2 | Requirements (`sdlc/artifacts/requirements`) | No | REQ-MODERNIZATION-019:48 and :50 list `MessagingException` and `ssl.TrustedSocketFactory` as imports to replace; correct as written. REQ-MODERNIZATION-002 does not restate the signature | 0 restatements | None |
| 3 | Design and contracts (`sdlc/artifacts/design`) | **Yes (root)** | `CNTR-MODERNIZATION-001-tls-socket-factory.md:85-86` (primary, the analysis's cited lines), `:84` (stale citation only, see below), `:196` (names `MessagingException` as the class the transport propagates). Not changed by this record but also naming `MessagingException`: `:147` (sample constructor signature), `:198`, `:230` (sample producer; these describe a `ServiceBase` method that no longer exists, see Pre-existing drift) | 3 lines to reword (`:85-86`, `:196`), 1 citation to refresh (`:84`) | Reword at apply time (Proposed Change). No other design document restates the `createSocket` signature (search 2) |
| 4 | Stories and sprints (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | No in-progress work | 26 closed records of finished work mention `createSocket` or its exceptions (search 2); none is open. No story exists that consumes the contract after the migration | 0 open | None. Part B story authoring cites this record |
| 5 | Application code (`app/src/main`) | Yes, at migration | `mail/PinnedCertificateSocketFactory.java:19` (import), `:75-76` (`throws ... MessagingException`). `mail/BackupImapStore.java:28,72`, `mail/transport/K9MailTransport.java:123` and `di/MailModule.kt:95` handle the store-construction exception, not `createSocket`'s; they belong to CR-005 | 2 lines in 1 class for this clause | Part B migration story: switch the import to the new class (or drop the unused `MessagingException` from the override, a design choice). Not done by this record |
| 6 | Tests (`app/src/test`) | No change needed for this clause | No test declares or catches the exception of `createSocket`. `mail/PinnedCertificateSocketFactoryTest.java:193-210` mentions `createSocket` in comments only | 0 | None |
| 7 | Vendored library (`k9mail-vendored`) | Yes, disappears at migration | `ssl/TrustedSocketFactory.java:3,11-12`; `ssl/DefaultTrustedSocketFactory.java:152-153`; the two call sites `store/imap/ImapConnection.java:209` (implicit TLS) and `:315` (after STARTTLS; this method and its caller declare `MessagingException`, `:293` and `:308`) | 4 files | None here. The files leave with `:k9mail-vendored` or move under CR-009 |
| 8 | Documentation (`README.md`, `sdlc/artifacts/engagement`) | No | 0 matches (search 6; the engagement folder has no match in search 2) | 0 | None |
| | **Total** | | | | 1 contract (3 lines, 1 citation); 1 production class at migration |

### Pre-existing drift found while reading (reported, not part of this record)

These make the contract's "current text" harder to read against today's code. They are not P-3 and are not fixed here.
- The contract names `ServiceBase.getBackupImapStore()` as producer (`:18`, `:56`, `:150-152`, `:198`, `:226-227`, `:318`). No `ServiceBase` file exists under `app/src` (file search: none) and `getBackupImapStore` has no definition in `app/src/main`. The live producer is `di/MailModule.kt:78-98` together with `K9MailTransport.buildSocketFactory` (`K9MailTransport.java:135-149`).
- The contract's `resolveSocketFactory(Context, String, int)` (`:152`) has no match in `app/src/main`.
- Line citations `BackupImapStore.java:29/30`, `:58-63`, `:60-62`, `:116-118` are stale; the constructor is now `BackupImapStore.java:71-76` and the seam `:129-131`.
The Solution Architect should decide whether the contract revision that applies CR-007 to CR-009 also corrects these. This record does not.

---

## Proposed Change

Wording changes to apply to `CNTR-MODERNIZATION-001` when the timing condition below is met. Nothing is applied by this record.

### Line `:84` (citation refresh only)

Current text:
```
Interface: com.fsck.k9.mail.ssl.TrustedSocketFactory   (k-9 library interface; verified import BackupImapStore.java:30)
```
Proposed text:
```
Interface: com.fsck.k9.mail.ssl.TrustedSocketFactory   (library interface; same fully qualified name in the vendored copy and at the Thunderbird pin)
```
The interface name does not change. The "verified import" citation is stale today (`BackupImapStore.java:29`) and will point at removed code after the migration.

### Lines `:85-86` (the P-3 change)

Current text:
```
Method:    Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias)
             throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException
```
Proposed text:
```
Method:    Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias)
             throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException
             // MessagingException is the library's checked mail exception. After the migration it is
             // net.thunderbird.core.common.exception.MessagingException, not com.fsck.k9.mail.MessagingException.
             // `socket` and `clientCertificateAlias` may be null (nullable in the library's Kotlin declaration).
```
The package string in the comment must use the same wording CR-005 settles for "the package of the moved types", so the two contracts do not disagree. The nullable remark records a fact from the analysis (line 631), already true of the vendored call sites: `ImapConnection.java:209` passes `null` as the socket.

### Line `:196` (error table, row 1)

Current text (the two cells that name the exception):
```
... surfaced from `createSocket(...)` (wrapped per k-9 as `MessagingException`) | Connection fails closed. The transport propagates `MessagingException` (DES-009 translates it ...
```
Proposed text:
```
... surfaced from `createSocket(...)` (wrapped per the library as its `MessagingException`) | Connection fails closed. The transport propagates the library's `MessagingException` (DES-009 translates it ...
```
Only the class name is qualified. The claim about the wrapping mechanism is carried over unchanged and is not re-measured by U-062 for the pin; analysis condition 12 (a loopback TLS test through the real store path) is where the class that reaches the transport on a failed certificate check is observed. Where the blocklist decision touches this row: a chain containing a listed public-key hash is rejected by upstream's trust manager before system validation with `CompromisedCertificateException` [C-344, C-338]. Whether that is a new row of this table, and what class the transport sees, depends on how the blocklist is wired with the app-owned trust code. That is a design question the product owner has not decided (Record 4) and this record does not write that text.

### Contract version note

Add to the changelog of the one combined revision (see Timing): "v2: `createSocket` names the library's `MessagingException` at its new package (CR-007)".

### Guarantee check: what each reworded line carries, and that it is unchanged

| Guarantee | Where the contract states it | Carried by the lines reworded here? | Result |
|-----------|------------------------------|-------------------------------------|--------|
| Certificate validation | `:95` (row 1), `:96` (row 2), `:90-91`, Invariant 2 `:173-176` | No. `:84-86` declare an exception type, they do not decide validation. `:196` states that a failed check "fails closed" | Unchanged. The words "Connection fails closed" are kept verbatim |
| Hostname verification | `:95` ("hostname-mismatched chain") and `:196` (row title) | Only as the title of the `:196` row, which is not edited | Unchanged |
| SNI, including no SNI for IP literals | Not stated anywhere in the contract (search for `SNI`, `IP literal`, `server name` in the contract: 0 matches). It lives in code (`DefaultTrustedSocketFactory.java:191-254`) and in 10 tests | No | Unchanged. Reported as a gap in the contract in CR-009, not here |
| Pinning and user-trusted certificates | `:96`, `:181-184`, `:215-216` | No | Unchanged |
| No silent fallback | `:200-202` (prohibited error behaviour), `:196` | `:196` only to the extent that the failure stays an exception from the factory | Unchanged. The wording keeps the failure as a thrown exception; no fallback is introduced or permitted |

The reword is a rename of the type named in a `throws` clause plus a comment. It can neither weaken nor strengthen any of the five guarantees.

---

## Timing

- **When to apply:** with the story or stories that make it true, as part of **one** revision of CNTR-MODERNIZATION-001 (v1 to v2) that also carries CR-008 and CR-009. The trigger is the first Part B migration story that removes `:k9mail-vendored`'s `MessagingException` from the factory interface (the change that makes `:85-86` false). Part B design is not blocked by waiting: the design cites the approved CR-007 as the target form, which is what analysis condition 8 ("CR-nnn approved") asks for.
- **Why not now:** the contract is `approved`, and it describes the live vendored code, with file and line citations that sprint contract verifications compare against code (for example `sdlc/artifacts/build/sprints/sprint-018/U-061/contract-verification.md`). The vendored factory is the only live TLS path until the migration lands, and condition 1 puts the toolchain requirements (REQ-MODERNIZATION-015, -016) ahead of any code that compiles upstream. TLS fixes were made in that interval before (sprints 10 and 11: U-045, U-047, BUG-015). A contract that is false for live code would mislead the next one.
- **Different from the CR-002 precedent, and why:** CR-002 amended CNTR-MODERNIZATION-007 before implementation because the narrowed signatures could be written by queued stories under an approved requirement (REQ-MODERNIZATION-012). Here the target form cannot exist in code until a toolchain-gated migration.
- **Must not happen before then:**
  - Any edit to `CNTR-MODERNIZATION-001` text (including a "pending revision" note; if one is wanted it is the Solution Architect's or Artifact Librarian's call).
  - Any change to `TrustedSocketFactory`, `DefaultTrustedSocketFactory` or `PinnedCertificateSocketFactory` made in the name of this record.
  - Any test or grep gate that asserts the new package string against live code.
  - Marking this record implemented on approval. Approval means the target wording is agreed, not applied.
- **Together or separately:** together. The lines of the three records sit in adjacent blocks of the same contract (this record `:84-86`; CR-008 `:98-101`; CR-009 `:95`, with CR-008 and CR-009 sharing lines `:275-276`). Rewriting them in separate passes leaves a contract that is true for one clause and false for the next. Each record is approved on its own; the text is applied once. If the Part B design splits the three code changes across stories, apply the revision at the last of them and have the earlier stories name the deviation from the contract text explicitly.

---

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory only — refined during story planning.

**What the contract protects.** CNTR-MODERNIZATION-001 protects one invariant. Exactly one validated, never trust-all factory reaches the store, and the consumer never re-decides trust (Invariants 1 to 3, contract lines 170-180). The `throws` clause carries none of that. The record's guarantee check is correct: rewording the clause cannot weaken validation, host-name checking, pinning or failing closed. DCR and Low: agreed; the compiler enforces the code side (one import, one `throws` clause in `PinnedCertificateSocketFactory.java`).

**Wording against CR-005.** No conflict. CR-005 names package roots (`com.fsck.k9`, `net.thunderbird`). This record names one class by its full name, which is the stricter form and is correct for a signature. To align vocabulary, line 196's "the library's `MessagingException`" should use whatever general term CR-005 settles (its proposal is "mail-library type").

**Recommendation for the combined v2 (CR-007, CR-008, CR-009).** All three records reach the same prevention recommendation: keep the invariants apart from a "library binding" block pinned to the fork commit. The v2 revision should adopt that structure: the invariants are the contract's published language, and the binding is an implementation detail. A future library change then edits the binding block only. The revision should also correct the pre-existing drift the three records report: the producer `ServiceBase.getBackupImapStore()` (lines 18, 56, 150-152, 198, 226-247) and `resolveSocketFactory` do not exist. Invariant 1 calls the producer "a single auditable choke point", and a v2 that names a choke point that does not exist would undercut the audit it promises. Cost: one larger review, but no second revision later.

**Lines the batch misses.** Line 96 names `com.zegoggles.smssync.mail.PinnedCertificateSocketFactory`. CR-005's rule forces that class under `mail/transport/`, so its full name changes. Validation Rule 3 (lines 213-214) still says "the k-9 `ImapStore` connection path". Both belong in the same v2.

**Story shape and order.** No story of its own. The import change sits in the Part B library-swap story; the text goes into the one CNTR-MODERNIZATION-001 v2 revision.

**Timing: cross-record inconsistency to resolve.** CR-007, CR-008 and CR-009 apply their text at the last code story. CR-005 and CR-006 apply CNTR-MODERNIZATION-007's text "with the Part B design", following that contract's own precedent of amending ahead of code (CNTR-007 line 319: "not yet implemented against the v2 surface"). Both contracts are checked against code by sprint contract verifications, so one policy should govern both. I recommend this one: agree all five target texts at Part B design, which is all that conditions 6 to 10 ask for ("CR-nnn approved"). Then apply both revisions to the contract files in the library-swap story, the one change set in which all five items become true together. The interim stories cite the approved records.

**Risk to settle at design.** Which class reaches the transport when a certificate check fails was not measured at the pin, and the blocklist adds `CompromisedCertificateException` (record, line-196 note). Condition 12's loopback test observes the first. The blocklist row is a design decision shared with CR-009.

---

## Cascade Analysis

**Root Layer:** Design and contracts (CNTR-MODERNIZATION-001 v1)

**Root Defect:** The contract fixes a library-internal exception class, with its package at authoring time, in the signature of the TLS boundary. A decided library change (the exception moved to another package upstream) makes that signature name a class that will not exist in the migrated build.

**Cascade Direction:** Mid-stream down (contract, then the one implementing class and the vendored interface at migration time; no requirement or test change)

### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Design and contracts | Requirements | No | Search 1: 0 files restate `createSocket`; REQ-019:48,50 are inventory lines and stay correct |
| 2 | Design and contracts | Other design documents | No | Search 2: only CNTR-001 states the signature; DES-002:71 is a historical snippet of removed code |
| 3 | Design and contracts | Application code | Yes, at migration | `PinnedCertificateSocketFactory.java:19,75-76` name the old class. The compiler enforces the change; there is no silent path |
| 4 | Design and contracts | Tests | No | Search 4 and the test search: no test names the exception of `createSocket` |
| 5 | Design and contracts | Vendored library | Yes, disappears | `TrustedSocketFactory.java:3,11-12` and `DefaultTrustedSocketFactory.java:152-153` leave with the module (or move under CR-009) |

### Fix Order

1. [ ] Design and contracts: apply the CR-007 wording in the combined CNTR-MODERNIZATION-001 revision, at the timing above, ROOT FIX
2. [ ] Application code: `PinnedCertificateSocketFactory.java` import and `throws` clause in the Part B migration story, CASCADE FIX
3. [ ] Verification: after the revision, the contract contains no `com.fsck.k9.mail.MessagingException` and `grep -rn "com.fsck.k9.mail.MessagingException" app/src/main` returns no `TrustedSocketFactory` implementer

### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| None | — | — | U-062 is done and sprint-019 is closed; no story consumes the contract after the migration. Hold nothing |

---

## Plan

- **Execution Window:** Backlog. Approval now (a precondition of Part B design); text applied with the Part B migration story, in the combined contract revision.
- **Assigned To:** TBD (the author of the combined revision)
- **Estimated Effort:** Under 1 hour for this record's share of the contract text; the code consequence is part of the Part B migration story
- **Sprint:** None scheduled

### Fix Checklist

- [ ] Design and contracts: reword `CNTR-MODERNIZATION-001` `:84`, `:85-86`, `:196` as in Proposed Change, in the combined revision
- [ ] Design and contracts: add the changelog line; set the contract's `change_records` frontmatter (owned by the Artifact Librarian)
- [ ] Application code: `PinnedCertificateSocketFactory` import and `throws` clause (Part B story)
- [ ] Verification: 0 remaining mentions of `com.fsck.k9.mail.MessagingException` in the contract

## Next Steps

CR filed and queued for sprint planning. Run `/amp:create-stories --from-cr CR-007` when ready to schedule.

Read this with the batch decision: CR-007 is a precondition of Part B design (analysis condition 8, row 5 of Section 11), not a story to schedule on its own. Its code consequence is one import inside a Part B story, and its contract text is applied in the combined revision with CR-008 and CR-009. Approval of the record is the condition; do not create a stand-alone repair story for it.

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

- **Root Cause Category:** Contract coupled to a library-internal type name (a normative interface contract embedded the third-party library's exception class and package).
- **Prevention Recommendation:** When a contract's carried type comes from a third-party or vendored library, state the role of each library type in the normative text and put the concrete class names and packages in a clearly separated "library binding" block that names the library version or commit it was verified against. A change of library then touches the binding block and never the invariants.
- **Closed By:** (pending)
- **Date Closed:** (pending)
