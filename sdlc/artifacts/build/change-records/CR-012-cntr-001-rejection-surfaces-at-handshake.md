---
status: approved
severity: low
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-002
  - REQ-MODERNIZATION-019
design_docs:
  - CNTR-MODERNIZATION-001
related_stories:
  - U-062
  - U-063
tags:
  - change-record
  - cntr-modernization-001
  - tls
  - thunderbird-migration
  - sc-1
id: CR-012
title: 'CNTR-MODERNIZATION-001: a certificate rejection surfaces at the TLS handshake of the socket createSocket returns, not from createSocket itself (U-063 SC-1)'
type: DCR
domain: change-records
summary: CNTR-MODERNIZATION-001 says a certificate rejection is surfaced from createSocket(...); in the code createSocket returns an unconnected socket and the rejection surfaces at the TLS handshake (story U-063 scope concern SC-1, confirmed by contract verification). CR-007 keeps the wrong wording, so no approved change record carries the correction.
updated_at: '2026-11-21T00:41:54.011Z'
---

# CR-012: CNTR-MODERNIZATION-001 — a certificate rejection surfaces at the TLS handshake, not from `createSocket` (SC-1)

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-012 |
| **Type** | DCR (Design Correction) |
| **Severity** | Low |
| **Status** | Open |
| **Reported By** | Requirements Analyst agent, for the orchestrator (routing of U-063 contract-verification warning W1) |
| **Date Reported** | 2026-11-20 |
| **Source** | Sprint work: story U-063 (sprint-020) scope concern SC-1, confirmed by its contract-verification gate, warning W1 |

Sibling records against the same contract: CR-007 (the `createSocket` exception class), CR-008 (the socket-factory slot) and CR-009 (producer 1). CR-007 and CR-009 are approved and not yet applied; CR-012 amends CR-007's proposal for one table row (see Proposed Change, "How CR-012 relates to CR-007, CR-008 and CR-009"). Read the product owner's Records 5 and 6 (appendix "Product owner decision" of the U-062 analysis) together with this record.

---

## Finding

### Description

CNTR-MODERNIZATION-001 describes how a certificate rejection crosses the TLS boundary. Its Error Handling lead-in says errors are "exceptions thrown from `createSocket(...)` at TLS-handshake time", and its first table row says the trust manager's `CertificateException` is "surfaced from `createSocket(...)`".

That is not where it happens. `createSocket(...)` builds a TLS context and returns a socket. It does not connect that socket, does not read from it, and does not start a handshake. For an implicit-TLS connection it returns an unconnected socket; for STARTTLS it returns a socket layered over a connection that is already open. The trust manager runs later, inside the TLS handshake of the returned socket. The IMAP connection code does not start that handshake itself. The handshake starts on the socket's first read (implicit TLS) or on the first command after the STARTTLS upgrade (STARTTLS). The resulting `SSLException` is caught in `ImapConnection.open()`, and when its direct cause is a `CertificateException` it is wrapped as `CertificateValidationException`, a `MessagingException`. The connection is then closed and the exception reaches the caller.

So the outcome the contract promises is correct and the place it names is wrong. The connection fails closed, by an exception, and no trust decision is made silently. Only where the exception comes from is misstated. The contract's own lead-in half-concedes this ("at TLS-handshake time"), which makes the sentence contradict itself.

Nothing is blocked and the code is not defective. This is a contract-text correction. It was found by story U-063, which recorded it as scope concern SC-1, and the U-063 contract-verification gate confirmed it as warning W1. That gate also found the routing gap this record closes: CR-007's proposed text for the same row keeps the words "surfaced from `createSocket(...)`" and says the wrapping claim is "carried over unchanged"; CR-008 and CR-009 do not touch the phrase (CR-009 changes only the possessive in the same cell). If the three records are applied as approved, the wrong wording survives into the next contract revision, and the only record of SC-1 would be the U-063 story notes, which none of the three records tells the revision's author to read.

This record changes contract text only. It does not change what any factory validates, which hosts it accepts, or how a connection fails.

### Source of Truth

- **The code** (read this session; lines in Evidence): `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` (`createSocket`), `.../ssl/TrustManagerFactory.java` (`checkServerTrusted`), `.../store/imap/ImapConnection.java` (the two call sites, `open()`, `handleSslException`), and `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` (`createSocket`, `checkServerTrusted`).
- **The U-063 tests that pin it**: `DefaultTrustedSocketFactoryHandshakeTest` (the rejection is raised by `startHandshake()`; `createSocket` returns normally) and `PinnedCertificateSocketFactoryCreateSocketTest`, both under `app/src/test/java/com/zegoggles/smssync/mail/`.
- **The contract-verification ruling**: `sdlc/artifacts/build/sprints/sprint-020/U-063/contract-verification.md`, warning W1 (lines 70-107; suggested wording at lines 98-103) and the SC-1 ruling (lines 246-249): a design correction, not a code defect, no bug.
- **Product owner decisions**: Records 5 and 6 of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` (lines 2185-2212). Record 6 decides that the SNI behaviour is to be stated in the contract's next revision and that a blocklisted chain is "a certificate error that fails closed, like an untrusted chain". Neither decision changes where a rejection surfaces.
- **The contract being corrected**: `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md`, status `approved`, version v1.

### Evidence

**Contract text, current** (quoted from `CNTR-MODERNIZATION-001-tls-socket-factory.md`):

```
:191  Errors are communicated across this boundary as exceptions thrown from `createSocket(...)` at
:192  TLS-handshake time — never as a silent trust decision:
```

```
:196  | Untrusted / expired / hostname-mismatched chain under `SYSTEM_VALIDATED` | `DefaultTrustedSocketFactory`'s TrustManager throws `CertificateException`, surfaced from `createSocket(...)` (wrapped per k-9 as `MessagingException`) | Connection fails closed. The transport propagates `MessagingException` (DES-009 translates it to an app-owned `MailException`/`TemporaryImapException` at the ACL boundary). |
```

Lines that say nothing wrong but sit next to these, read to decide whether they need a change (they do not; reasons in Proposed Change):

```
:89-91   The object crossing the boundary is a `TrustedSocketFactory` whose `createSocket(...)` returns a
         `javax.net.ssl.SSLSocket` wrapping an `SSLContext` initialized with a **non-permissive**
         `javax.net.ssl.X509TrustManager`.
:197     | Presented leaf does not match the enrolled cert under `PINNED_CERTIFICATE` | `PinnedCertificateSocketFactory`'s TrustManager throws `CertificateException` (SHA-256 mismatch or outside validity window) | Connection fails closed; same propagation path. Fail-closed is the intended behavior (DES-002 §Migration UX). |
:259-260 // PinnedCertificateSocketFactory.createSocket(...) builds an SSLContext whose X509TrustManager
         // .checkServerTrusted() accepts ONLY `enrolled` (SHA-256 match + validity window), else CertificateException.
:262     // If an on-path attacker presents a different cert → CertificateException → connection fails closed.
```

**CR-007's proposed text for the same row** (`CR-007-cntr-001-createsocket-exception-class.md:184-194`), which keeps the wording:

```
:188  ... surfaced from `createSocket(...)` (wrapped per k-9 as `MessagingException`) | Connection fails closed. The transport propagates `MessagingException` (DES-009 translates it ...      <- current
:192  ... surfaced from `createSocket(...)` (wrapped per the library as its `MessagingException`) | Connection fails closed. The transport propagates the library's `MessagingException` (DES-009 translates it ...      <- proposed
:194  "Only the class name is qualified. The claim about the wrapping mechanism is carried over unchanged ..."
```

**Code: where the rejection is raised, where it is caught and wrapped, and what the caller sees.** All lines opened this session.

| Step | File and line | What the code does |
|------|---------------|--------------------|
| `createSocket` builds a context, returns a socket | `k9mail-vendored/.../ssl/DefaultTrustedSocketFactory.java:152-180` | `:155` installs `TrustManagerFactory.get(host, port)`; `:165-167` builds the context; `:170` `socketFactory.createSocket()` (unconnected) or `:172` `socketFactory.createSocket(socket, host, port, true)` (layered over the given socket); `:176` hardening; `:177` SNI; `:179` `return trustedSocket;`. No connect, no read, no `startHandshake()` |
| Pinned `createSocket` is the same shape | `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:75-86` | `:77` installs `PinnedX509TrustManager`; `:82` unconnected socket or `:84` layered socket; returned without a handshake |
| The trust decision, run by the platform during the handshake | `k9mail-vendored/.../ssl/TrustManagerFactory.java:68-94` | `:76` system chain validation; `:77` `verifyHostname`; `:91` local key store consulted only after a failure; `:92` `throw new CertificateChainException(...)` (a `CertificateException`). `verifyHostname` throws `SSLException` at `:147`, which `:83-87` folds into the same path |
| Pinned trust decision | `PinnedCertificateSocketFactory.java:126-150` | `:141-144` `throw new CertificateException("Pinned certificate mismatch ...")`; `:149` `enrolledCert.checkValidity()` |
| Implicit TLS: socket created | `k9mail-vendored/.../store/imap/ImapConnection.java:209` | `socket = socketFactory.createSocket(null, host, port, clientCertificateAlias);` |
| Implicit TLS: plain connect, still no handshake | `ImapConnection.java:214`, `:115-117` | `address.connect(socket, ...)`; `socket = connect(); configureSocket(); setUpStreamsAndParserFromSocket();` (no I/O) |
| Implicit TLS: first read starts the handshake | `ImapConnection.java:119`, `:242-243` | `readInitialResponse()` calls `responseParser.readResponse()` |
| STARTTLS: plain command, then socket layered | `ImapConnection.java:309`, `:315` | `executeSimpleCommand(Commands.STARTTLS);` then `socket = socketFactory.createSocket(socket, host, port, clientCertificateAlias);` |
| STARTTLS: first I/O after the upgrade starts the handshake | `ImapConnection.java:324`, `:281`, `:668`, `:689-690` | `requestCapabilities()` -> `executeSimpleCommand(CAPABILITY)` -> `sendCommand` writes and flushes (`:689-690`), then the status read (`:671`) |
| Caught and wrapped | `ImapConnection.java:132-133`, `:146-152` | `catch (SSLException e) { handleSslException(e); }`; `if (e.getCause() instanceof CertificateException) { throw new CertificateValidationException(e.getMessage(), e); } else { throw e; }` |
| Fails closed | `ImapConnection.java:138-143` | `finally { if (!authSuccess) { ... close(); } }`. No retry with another factory and no plain-text fallback exists on this path |
| What the caller sees | `ImapConnection.java:100`; `k9mail-vendored/.../CertificateValidationException.java:10` | `open()` throws `CertificateValidationException`, which `extends MessagingException`, for a certificate cause; for an `SSLException` with any other cause it rethrows the `SSLException` itself (`:150`), which is an `IOException`, not a `MessagingException` |

**Tests that now pin the real behaviour** (U-063, committed in `5a5321d4`):

| Test | Where | What it pins |
|------|-------|--------------|
| `DefaultTrustedSocketFactoryHandshakeTest` (8 tests: 2 controls, 6 rejections) | `.../mail/ssl/DefaultTrustedSocketFactoryHandshakeTest.java:297-308` (`assertRejected`), `:314-326` (`connectClient`), `:332-350` (shape of the cause) | `connectClient` calls `createSocket` at `:318` and `:323` outside any `try`, so it returns for every bad-certificate server. The rejection is then asserted from `ssl.startHandshake()` (`:303`, `fail` at `:304`, `catch (SSLException e)` at `:305`), with a direct cause that is a `CertificateException` and a `CertificateChainException` (`:335-338`). Javadoc `:87-91` states the position |
| `PinnedCertificateSocketFactoryCreateSocketTest.handshake_certificateOtherThanEnrolled_isRejected` | `.../mail/PinnedCertificateSocketFactoryCreateSocketTest.java:180-196` | `createSocket` at `:184`; rejection from `client.startHandshake()` (`:188`); direct cause a `CertificateException` carrying "Pinned certificate mismatch" (`:191-194`). Javadoc `:75-76` |

What is not shown, so the new wording does not claim it as tested:
- No test drives the IMAP connection class. `DefaultTrustedSocketFactoryHandshakeTest.java:87-88` says so. A search of the repository found no test (and no code outside `ImapConnection.java`, `KeyChainKeyManager.java`, `Pop3Store.java`, `WebDavStore.java`, `SmtpTransport.java`) that names `CertificateValidationException` or `handleSslException`. The wrapping step and the "first read starts the handshake" step are therefore established by reading the code, not by a test.
- No test asserts, as a statement of its own, that `createSocket` does not throw for a bad certificate. It follows from `connectClient` calling `createSocket` outside a `try` in every rejection test.
- The tests run under a desktop test provider, at TLS 1.3, with a test CA in place of the platform trust store (Javadoc `:78-95`). Nothing is shown about a device's provider.

---

## Triage

### Classification

- **Type:** DCR (Design Correction). The design contract does not match the source of truth (the code and its characterisation tests) about where a rejection surfaces.
  - Not a DEF: the implementation is not defective. A bad certificate fails the connection closed, by an exception, on both call shapes and for both factories.
  - Not an RCR: no behaviour, scope or client request changes, and no guarantee changes.
- **Root:** Design and contracts layer. The contract was written from the shape of the `TrustedSocketFactory` interface (an exception is declared on `createSocket`) and carried the interface's exception list over to the trust failure, without tracing where JSSE runs the trust manager. The lead-in (`:191-192`) shows the author knew the handshake was involved.
- **Layers Affected:** 2. Design and contracts (the root, one file, two places); stories, sprints and change records (documentary references only: CR-007's proposed text for the row, and the U-063 notes that point at an unwritten record). Application code, vendored library, tests, requirements and documentation are not wrong and need no change.
- **Severity Justification:** Low. The criterion for low is "documentation-only or future impact"; the first holds. Nothing is blocked (U-063 is done and sprint-020 is closed), no code or test changes, and no guarantee is touched. It is not medium (isolated impact, non-blocking) because nothing is reworked and the correction is two places in one contract. A defensible alternative reading is medium, because unlike CR-007 to CR-009 the wording is false today, in an approved contract, on a security boundary, and an approved record already propagates it. The create-cr skill gives medium and low the same next-step text, so the choice changes no action; the product owner may raise it.

---

## Impact Analysis

> `change_records.layers` is not configured in `sdlc/config.yaml`. The layers below are the ones given for this batch: requirements, design and contracts, stories, sprints and change records, application code, vendored library, tests, documentation.

Checklist run: `knowledge/checklists/impact-analysis-checklist.md` (per-layer search, ground-truth identification, impact summary, cascade analysis). No `[TBC]` placeholders are involved.

Ground truth (layer 1, read-only): the code listed in Evidence, chiefly `ImapConnection.java:100-152` and `DefaultTrustedSocketFactory.java:152-180`. Correct value: a certificate rejection is raised during the TLS handshake of the socket `createSocket` returns, which the IMAP code starts on the first read or write; it fails closed.

### Searches run

| # | Search (ripgrep through the Grep tool) | Path | Result |
|---|----------------------------------------|------|--------|
| 1 | `createSocket` | `sdlc/artifacts/design` | 1 file, `CNTR-MODERNIZATION-001`: `:85`, `:89`, `:191`, `:196`, `:259`, `:313`. No other design document mentions it |
| 2 | `createSocket` | `sdlc/artifacts/requirements` | 0 files. `REQ-MODERNIZATION-002:109` ("when the TLS handshake is initiated") says nothing about where a rejection surfaces |
| 3 | ``surfaced from\|thrown from `createSocket\|from `createSocket`` | `sdlc/artifacts` | 8 files. Normative: the contract (`:191`, `:196`). Records that repeat or quote it: `CR-007` (3: `:73`, `:188`, `:192`), `CR-012` (this file), and in U-063 its story (`:253`), `plan.md` (`:1712`), `contract-verification.md` (3) and `review-security.md` (1). The eighth, `sprint-010/U-045/review-security.md:52`, is a false positive (it is about the host name passed to SNI) |
| 4 | same pattern | whole repository, `*.java *.kt *.md *.xml *.gradle *.yaml` | No hit outside `sdlc/artifacts`. No production code, test or README repeats the phrase |
| 5 | `SC-1` | `sdlc/artifacts/build/sprints/sprint-020` | U-063 story `:89`, `:253`, `:307`, `:337`; `plan.md` `:1712`, `:2126`; `contract-verification.md` `:34`, `:70-104`, `:241`, `:246`; `review-code.md:142`; `review-security.md:192`; `retro.md:144`; `implementation-log.md` (4). All closed records |
| 6 | `SC-1` | `app/src` | 2 test Javadoc comments: `DefaultTrustedSocketFactoryHandshakeTest.java:90`, `PinnedCertificateSocketFactoryCreateSocketTest.java:76` |
| 7 | `CertificateValidationException\|handleSslException` | repository, `*.java *.kt` | Production code only inside the vendored module (`ImapConnection.java:36,133,146,148,302,354,490`, `KeyChainKeyManager.java`, `Pop3Store.java`, `WebDavStore.java`, `SmtpTransport.java`). 0 hits in `app/src` (main or test), 0 tests anywhere. The vendored module has no `src/test` |
| 8 | `surfaced\|handshake\|fails closed` (case-insensitive) | `sdlc/artifacts/design` | Other documents say "fails closed" or "validation failures are surfaced" with no place named (`CNTR-MODERNIZATION-002:253-254`, `DES-MODERNIZATION-002:468`); both stay correct |

### Layer Impact Matrix

| # | Layer | Affected? | Files and lines | Occurrences | Action Needed |
|---|-------|-----------|-----------------|-------------|---------------|
| 1 | Ground truth: the code | — | `ImapConnection.java:100-152,209,214,315,324`; `DefaultTrustedSocketFactory.java:152-180`; `TrustManagerFactory.java:68-94`; `PinnedCertificateSocketFactory.java:75-86,126-150` | — | Ground truth (read-only). Correct as it is |
| 2 | Requirements (`sdlc/artifacts/requirements`) | No | Search 2: 0 files restate where a rejection surfaces. `REQ-MODERNIZATION-002:109` and `REQ-MODERNIZATION-019` are unaffected | 0 | None |
| 3 | Design and contracts (`sdlc/artifacts/design`) | **Yes (root)** | `CNTR-MODERNIZATION-001`: `:191-192` (lead-in says the exception is "thrown from `createSocket(...)`"); `:196` cell 2 ("surfaced from `createSocket(...)`"). Implies the place but is true as written, so unchanged: `:89-91`, `:197`, `:259-260`, `:262`. Other design documents: none (search 1, 8) | 2 places to reword; 4 lines reviewed and kept | Reword at the timing under Timing (Proposed Change) |
| 4 | Stories, sprints and change records (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | Documentary only | `CR-007` proposed text for `:196` (`:188`, `:192`, `:194`) repeats the wording; U-063 story `:253`, `:307`, `:337`, `plan.md:1712`, `:2126`, `contract-verification.md` W1 (`:70-107`) say the correction is pending a record. All are closed, finished work | 1 approved, unapplied record; 7 closed notes | CR-007: superseded for this row by this record's combined text (no edit to CR-007 by this record). Closed notes: leave; once CR-012 is approved the orchestrator may add "carried by CR-012" to the U-063 story as W1 item 2 asks. Not done here |
| 5 | Application code (`app/src/main`) | No | `PinnedCertificateSocketFactory.java:75-86` behaves as the corrected text says. No `createSocket` caller in `app/src/main` (search 3 of CR-007) | 0 | None |
| 6 | Vendored library (`k9mail-vendored`) | No | `ImapConnection.java`, `DefaultTrustedSocketFactory.java`, `TrustManagerFactory.java`: ground truth, correct | 0 | None |
| 7 | Tests (`app/src/test`) | No change needed; they pin the behaviour | `DefaultTrustedSocketFactoryHandshakeTest.java:297-350`; `PinnedCertificateSocketFactoryCreateSocketTest.java:180-196`. Comment only: the two Javadoc mentions of "scope concern SC-1" (`:90`, `:76`) point at a story-local label | 9 tests pin the behaviour (8 + 1); 2 comments | None required. Optional comment refresh to cite this record, in a later test story (Scope Concern C) |
| 8 | Documentation (`README.md`, `sdlc/artifacts/engagement`) | No | `README.md` and the engagement folder: no match for the pattern (search 4) | 0 | None |
| | **Total** | | | | 1 contract (2 places); 1 approved record's proposal for one row superseded |

Layers affected: 2 (design and contracts; stories, sprints and change records). Cascade analysis follows because two layers are affected.

---

## Proposed Change

Wording changes to apply to `CNTR-MODERNIZATION-001` at the timing recommended under Timing. Nothing is applied by this record.

**The guarantee is unchanged.** The connection still fails closed, by an exception, and never as a silent trust decision. The third cell of the row ("Connection fails closed. The transport propagates `MessagingException` ...") is carried over word for word in the erratum, and the lead-in keeps "never as a silent trust decision". Only the place the exception comes from is corrected.

### Lines `:191-192` (Error Handling lead-in)

Current text:
```
Errors are communicated across this boundary as exceptions thrown from `createSocket(...)` at
TLS-handshake time — never as a silent trust decision:
```
Proposed text:
```
Errors are communicated across this boundary as exceptions, never as a silent trust decision. A certificate
rejection is raised during the TLS handshake of the socket that `createSocket(...)` returns, not by `createSocket(...)` itself:
```
Two lines for two lines. The lead-in also covers the third row of the table ("Producer cannot resolve a factory"), which is thrown before any socket exists; the new first sentence is true for all three rows, and the second sentence is scoped to certificate rejections (rows 1 and 2).

### Line `:196` (error table, row 1, cell 2 only)

Starting point: contract-verification W1's suggested wording ("surfaced at the TLS handshake of the socket that `createSocket(...)` returns (on its first read or write, or an explicit `startHandshake()`), as an `SSLException` whose direct cause is the `CertificateException`; k-9 wraps it as `CertificateValidationException` (a `MessagingException`) in `ImapConnection.handleSslException`"). Checked against the code, three adjustments:
- "wraps it" is true only when the `SSLException`'s direct cause is a `CertificateException` (`ImapConnection.java:147-151`). The text says so. It holds for both certificate rows: the default factory's rejections have a `CertificateChainException` as direct cause (`DefaultTrustedSocketFactoryHandshakeTest.java:335-338`), the pinned factory's a plain `CertificateException` (`PinnedCertificateSocketFactoryCreateSocketTest.java:191-194`).
- "on its first read or write, or an explicit `startHandshake()`" is JSSE's behaviour, and `ImapConnection` never calls `startHandshake()`; the IMAP code reaches it on a first read (`:119`) or a first write-then-read (`:324`). The text says "first read or write unless the caller starts it", which is true of both the vendored code and the tests (which call `startHandshake()`).
- `handleSslException` is private to `ImapConnection.open()`, and the class names are the vendored library's. The text names them for the live contract (v1) and the combined text below replaces them with the library-neutral form CR-007 introduces.

Current text (cell 2 of the row; cells 1 and 3 are unchanged):
```
`DefaultTrustedSocketFactory`'s TrustManager throws `CertificateException`, surfaced from `createSocket(...)` (wrapped per k-9 as `MessagingException`)
```
Proposed text, **erratum form for v1** (live vendored names):
```
`DefaultTrustedSocketFactory`'s TrustManager throws `CertificateException` during the TLS handshake of the socket that `createSocket(...)` returns (`createSocket(...)` itself opens no connection and starts no handshake). The handshake runs on that socket's first read or write unless the caller starts it, and raises an `SSLException` whose direct cause is the `CertificateException`; k-9's `ImapConnection` wraps an `SSLException` with that cause as `CertificateValidationException`, a `MessagingException`
```
The whole row after the erratum:
```
| Untrusted / expired / hostname-mismatched chain under `SYSTEM_VALIDATED` | `DefaultTrustedSocketFactory`'s TrustManager throws `CertificateException` during the TLS handshake of the socket that `createSocket(...)` returns (`createSocket(...)` itself opens no connection and starts no handshake). The handshake runs on that socket's first read or write unless the caller starts it, and raises an `SSLException` whose direct cause is the `CertificateException`; k-9's `ImapConnection` wraps an `SSLException` with that cause as `CertificateValidationException`, a `MessagingException` | Connection fails closed. The transport propagates `MessagingException` (DES-009 translates it to an app-owned `MailException`/`TemporaryImapException` at the ACL boundary). |
```
It is still one table row. A blocklisted chain (Record 6) needs no new row for this purpose: it is rejected by the same trust manager during the same handshake, so the corrected wording already says where it surfaces. Whether it is its own row is the open question CR-007 and CR-009 already carry.

### How CR-012 relates to CR-007, CR-008 and CR-009

CR-012 amends CR-007's proposal for the row at `:196`, cell 2, and nothing else in CR-007. It does not alter CR-007's other lines (`:84`, `:85-86`) or its error-table cell 3. CR-008 does not touch this row or the lead-in. CR-009 touches only the possessive in cell 2.

| Place | Contract v1 today | CR-007 proposes | CR-009 proposes | CR-012 proposes |
|-------|-------------------|-----------------|-----------------|-----------------|
| `:191-192` lead-in | "thrown from `createSocket(...)` at TLS-handshake time" | nothing | nothing | reworded (above) |
| `:196` cell 2, class of the trust-manager owner | `DefaultTrustedSocketFactory`'s TrustManager | nothing | "the default factory's TrustManager" | nothing (keeps v1 wording; CR-009 applies on top) |
| `:196` cell 2, where it surfaces | "surfaced from `createSocket(...)`" | keeps it | nothing | replaced (above) |
| `:196` cell 2, wrapping | "(wrapped per k-9 as `MessagingException`)" | "(wrapped per the library as its `MessagingException`)" | nothing | replaced by the wrapping clause (above); CR-007's intent (qualify the class as the library's) is kept in the combined text |
| `:196` cell 3 | "propagates `MessagingException`" | "propagates the library's `MessagingException`" | nothing | nothing (unchanged) |

**Combined target text for the row, with CR-007, CR-009 and CR-012 all applied** (whoever applies the combined revision uses this one text):
```
| Untrusted / expired / hostname-mismatched chain under `SYSTEM_VALIDATED` | The default factory's TrustManager throws `CertificateException` during the TLS handshake of the socket that `createSocket(...)` returns (`createSocket(...)` itself opens no connection and starts no handshake). The handshake runs on that socket's first read or write unless the caller starts it, and raises an `SSLException` whose direct cause is the `CertificateException`; the library's IMAP connection code wraps an `SSLException` with that cause in its certificate-validation subtype of `MessagingException` | Connection fails closed. The transport propagates the library's `MessagingException` (DES-009 translates it to an app-owned `MailException`/`TemporaryImapException` at the ACL boundary). |
```
The lead-in `:191-192` is the same in the combined revision as in the erratum. If the erratum has already been applied, the combined revision changes only the three phrases CR-007 and CR-009 own ("`DefaultTrustedSocketFactory`'s" to "The default factory's"; "k-9's `ImapConnection` wraps ... as `CertificateValidationException`, a `MessagingException`" to "the library's IMAP connection code wraps ... in its certificate-validation subtype of `MessagingException`"; "`MessagingException`" to "the library's `MessagingException`" in cell 3). The vendored class name `CertificateValidationException` is deliberately not in the combined text: after the migration the class that reaches the transport on a failed check has not been measured at the pin. The loopback test of analysis condition 12 (and CR-009 test T3) is where it is observed, and the text should be tightened to the measured class then.

### Lines reviewed and not changed

| Contract lines | Why no change |
|----------------|---------------|
| `:89-91` ("`createSocket(...)` returns a `javax.net.ssl.SSLSocket` wrapping an `SSLContext` initialized with a non-permissive `X509TrustManager`") | True as written, and it is the line that already says the socket is returned with the trust manager inside its context |
| `:197` (pinned row, "same propagation path") | The pinned rejection is raised in the same place and has a `CertificateException` direct cause (test `:191-194`), so "same propagation path" is true once `:196` is corrected. The corrected lead-in also covers it |
| `:259-260`, `:262` (scenario B comments) | `createSocket` does build the context whose trust manager does the check; the check runs later, in the handshake. Imprecise to a reader, but not false, and no place is named |
| `:313-314` (notes: "source verified") | Cites the signature only |

### Guarantee check: what each reworded place carries, and that it is unchanged

| Guarantee | Where the contract states it | Carried by the places reworded here? | Result |
|-----------|------------------------------|--------------------------------------|--------|
| Fails closed, by an exception | `:196` cell 3, `:197`, `:200-202`, `:262` | `:196` cell 3 is not edited; the lead-in keeps "never as a silent trust decision" | Unchanged. This record corrects the place the exception comes from. It does not weaken, narrow or condition the guarantee |
| Certificate validation | `:95`, `:96`, Invariant 2 (`:173-176`) | No | Unchanged |
| Hostname verification | `:95` ("hostname-mismatched chain"), `:196` (row title) | Only as the row title, which is not edited. The host-name check is part of the same trust manager and the same handshake (`TrustManagerFactory.java:77`) | Unchanged |
| No silent fallback | `:200-202` | No. The code has no catch-and-downgrade on this path (`ImapConnection.java:132-143`) | Unchanged |
| Pinning | `:96`, `:181-184`, `:215-216` | No | Unchanged |
| SNI | Not stated in the contract | No | Not touched; see Scope |
| Protocol versions | Not stated in the contract | No | Not touched; see Scope |

The proposed text is slightly more specific than the old text about the success of the wrapping, and it states a condition ("an `SSLException` with that cause") that the old text left implicit. That is a narrowing of a claim the code never made unconditionally, not of a guarantee: an `SSLException` with another cause escapes as an `IOException`, still closes the connection, and is the subject of Scope Concern B.

### Contract version note

No version change is needed for the erratum: no obligation, type, name or behaviour the contract fixes is altered. Recording it is the Artifact Librarian's job (the contract's `change_records` field is empty today and is Librarian-owned). When the combined revision is applied, add to its changelog: "v2: a certificate rejection surfaces at the TLS handshake of the socket `createSocket` returns (CR-012)".

---

## Scope

This record is about where a rejection surfaces. Two neighbouring gaps came out of the same work.

### SNI behaviour: a separate record, not this one

**Decision:** stating the SNI behaviour does not belong in CR-012. It needs its own record, which must be raised before the combined revision. The product owner has already decided that the contract states it (Record 6), so the new record is a drafting task with a known outcome, not an open question.

Why a separate record:
1. **Different kind of change.** CR-012 corrects a statement the contract got wrong, and its guarantee check shows it adds no obligation. An SNI statement is a new guarantee: once it is in an approved contract, the Part B move of the default factory must preserve it, and a later change to it is a contract change. A reviewer approving a wording erratum should not approve a new commitment in the same step.
2. **Different timing.** The surfacing correction is recommended now (see Timing). The SNI text sits in the producer table whose rows CR-009 rewrites (class, owner, location), so its natural carrier is the combined revision, or a separate erratum decided on its own.
3. **The pinned half is not settled.** The statement must say what the pinned factory does about SNI. Today it sets none (`PinnedCertificateSocketFactory.java:75-86`, no SNI call). BUG-020 (the pinned factory has no TLS hardening, draft) does not mention SNI as filed, but a fix that changes how the pinned socket is set up could reopen the wording. That is better decided with BUG-020's direction known.
4. **Bundling would put a partly open item on a closed one.** If the SNI wording needed a second round, the surfacing erratum would wait with it.

The cost of separating is the one W1 found: a decision that no approved record carries. The next step names it so it is not lost, and candidate text is given here so the new record is quick to write. This text is **not** part of CR-012's proposed change and is not applied by it.

Candidate text, from what the code does today (`DefaultTrustedSocketFactory.java:191-254`, tests in `DefaultTrustedSocketFactorySniTest`, `DefaultTrustedSocketFactoryJdkProviderTest`):
```
SNI. The default factory sets the TLS server name to the host name it is given when that host is a DNS name.
It sets no server name, and does not throw, when the host is null, empty or blank, contains a colon (an IPv6
literal), or has the form of an IPv4 literal (four dot-separated groups of one to three digits). If the platform
rejects the name as malformed, the factory skips SNI and the connection continues. The pinned factory sets no
server name itself and applies none of this handling.
Not stated: what a device's TLS provider puts on the wire for either factory.
```
What is not stated, because it is not known: U-063's capture found that under the test provider the provider supplies a name by itself (on the layered shape the host passed to `createSocket`; on the implicit shape the name from the connect address), and that the pinned factory, which sets none, sends the same names as the default factory. In that environment the default factory's own SNI call has no observable effect. What a device does is not shown by any test. Two code facts the statement should not over-promise: the IPv4 pattern is loose (`999.999.999.999` also skips SNI, U-063 SC-4), and under minSdk 24 only one of the three code paths remains (Record 6), which is an implementation detail and not contract text.

### Protocol versions: not part of this record

The contract says nothing about protocol versions (U-063 contract-verification, row 6 of its section 7). BUG-019 (the default factory enables TLS 1.0 and 1.1 wherever the provider supports them, status ready) and BUG-020 (the pinned factory applies no hardening, draft) are open, and the keep-or-drop decision on TLS 1.0 and 1.1 is the product owner's, with the Security Reviewer's judgement. A protocol statement in the contract would pre-empt it. It waits for the BUG-019 decision and is a further record then.

### Scope Concerns (raised, not decided; for the Solution Architect and product owner)

- **A. The timing rule in CR-007 is a prohibition.** CR-007's Timing lists "any edit to `CNTR-MODERNIZATION-001` text" as something that must not happen before the combined revision. Applying the erratum now (my recommendation) is an explicit, recorded exception for two places that CR-007 does not own. Approval of CR-012 with "apply now" is that exception; if the product owner wants CR-007's rule kept whole, CR-012 is held for the combined revision (Timing, option 2).
- **B. Handshake failures that are not certificate rejections.** `handleSslException` wraps only when the direct cause is a `CertificateException` (`ImapConnection.java:147-151`); any other `SSLException` (for example a protocol or cipher mismatch, or a server alert unrelated to the certificate) leaves `open()` as an `IOException`, not a `MessagingException`. The connection still fails closed (`:138-143`). The contract's Error Handling has no row for this and the corrected cell 2 now states the condition. Whether a row is wanted, and what the transport sees, is a design question for the Solution Architect; this record adds none.
- **C. The wrapping and first-read claims rest on reading the code.** No test drives `ImapConnection` through a rejection (Evidence, "What is not shown"). The planned proof is analysis condition 12 (a loopback TLS test through the real store path) and CR-009 test T6. Whether to write a characterisation test of the vendored path now is a separate test decision and is not created here. The two test Javadocs that cite "scope concern SC-1" could cite this record instead; comment-only.
- **D. Pre-existing drift in the contract** (producer named as `ServiceBase.getBackupImapStore()`, `resolveSocketFactory`, stale line citations into `BackupImapStore.java`). Already reported by CR-007, CR-008 and CR-009; not repeated or fixed here.

---

## Timing

**Recommendation: apply the surfacing correction now, on its own, as a v1 erratum, limited to `:191-192` and `:196` cell 2, and keep CR-007 to CR-009 for the combined revision.** This is the product owner's call (Scope Concern A).

Why this record differs from CR-007 to CR-009:
- CR-007, CR-008 and CR-009 describe code that will exist after the migration. The contract is true of the live vendored code today, so they wait for the story that makes them true; writing them now would make the contract false. CR-012 is the opposite: the contract is **false today** for the live code, and the correction is **true today**. The reason to hold the other three does not apply.
- The erratum can be made line-count-neutral (two lines for two lines, one row for one row), so every line citation in CR-007, CR-008 and CR-009 (`:64`, `:84-86`, `:95`, `:98-101`, `:146-148`, `:153`, `:161-163`, `:173-174`, `:177-180`, `:196`, `:243`, `:275-276`, `:284-285`) still points at the same text.
- It overlaps the three records in one cell only (`:196` cell 2). The erratum keeps the phrase CR-009 edits (`DefaultTrustedSocketFactory`'s TrustManager) and cell 3 verbatim, so CR-007's cell-3 change and CR-009's possessive change still apply cleanly on top. The combined target text above removes any doubt about the end state.

| | Apply now as an erratum | Hold for the combined revision |
|---|---|---|
| **Risk** | Two edits to one row at different times: the "current text" quoted in CR-007 (`:188`) and CR-009 (`:195`) no longer matches cell 2 when the combined revision is applied, so the author must work from the post-erratum text or this record's combined text. It also breaks CR-007's "no edit before" rule for these places (Scope Concern A). The erratum names vendored classes that are replaced again a few sprints later, so the row is edited twice | A known-wrong line stays in an approved contract for an unknown time (the migration is gated by the toolchain requirements, analysis condition 1). Every story that cites the contract in its plan gate meets the mismatch again, as U-063 did (U-064, BUG-019 and BUG-020 would meet the same row if they cite the contract). The combined revision's author may copy "surfaced from `createSocket(...)`" from CR-007, which is the trap W1 found |
| **Mitigation** | This record gives the post-erratum row, the combined row and a table of who changes what; the Fix Order lists the order | Part B design cites approved records as the target form (CR-007 Timing), so design is not misled if it cites CR-012. The contract itself is still wrong for any reader who does not follow the links; the Librarian can set `change_records` on the contract so they are listed |

If the product owner prefers a single revision, nothing in this record needs to change: CR-012 is then listed with CR-007 to CR-009 as an input to the combined revision, and the combined row above is the target.

Must not happen before approval: any edit to `CNTR-MODERNIZATION-001` text, including a "pending revision" note, and any change to code or tests in the name of this record. The erratum, when approved, is a text change by the Solution Architect or Artifact Librarian and touches nothing else.

---

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory only — refined during story planning.

**Classification.** DCR and Low: agreed. The analyst's trace matches the code. `createSocket` returns an unconnected or layered socket and starts no handshake (`DefaultTrustedSocketFactory.java:152-180`, `PinnedCertificateSocketFactory.java:75-86`). The handshake runs on the first read (`ImapConnection.java:119`) or on the first command after STARTTLS (`:324`). `handleSslException` wraps the exception only when its direct cause is a `CertificateException` (`:146-152`), and `open()` closes the connection on failure (`:138-143`).

**Advice: apply it now as a v1 erratum.** The contract's Versioning section (`CNTR-MODERNIZATION-001-tls-socket-factory.md:279-291`) defines two kinds of change: breaking changes, which need a new version, and backward-compatible ones. This correction is neither. No type, name, invariant or consumer duty changes, and failing closed is kept. Nothing in the section forbids a v1 erratum, and nothing in it requires a version bump. CR-007 to CR-009 hold their text back because applying it now would make the contract false for the live code (CR-007 `:217`). CR-012's text makes the contract true, so the reason behind their timing rule argues for applying it. Holding it back would leave a known-false line in an approved security contract. The plan gates of BUG-019, BUG-020 and U-064 would read that line, and the combined revision has no date because it waits for the toolchain work. The cost of applying now is one row edited twice; the analyst's combined row removes that cost.

**What must be recorded if the erratum is applied.**
- It is an exception to three timing rules, not one. CR-007 `:219-220`, CR-008 `:257-258` and CR-009 `:244-245` each forbid "any edit" to the contract text before the combined revision, but Scope Concern A names only CR-007. The product owner's approval should name all three and limit the exception to `:191-192` and `:196` cell 2.
- Who applies it. The contract is approved and owned through the Artifact Librarian. The Librarian copies the two places word for word from Proposed Change, sets `change_records` (empty today, `:8`) to list CR-012, and leaves the status and v1 alone. An erratum line may be added at the end of Notes, after `:318`, so that no cited line moves.
- A pointer for the author of the combined revision. After the erratum, the "current text" that CR-007 and CR-009 quote for `:196` no longer matches the contract. CR-012 does not edit CR-007, so the contract's `change_records` would be the only signal. I would add the same pointer to CR-007 and CR-009 through the Librarian.

**The combined row composes without conflict.** The three records change different phrases of the row:
- CR-009 changes the possessive to "the default factory's TrustManager" (CR-009 `:195`).
- CR-007 adds the library qualification: the wrapping phrase in cell 2 and the class in cell 3 (CR-007 `:192`).
- CR-012 changes where the rejection surfaces and rewrites the wrapping clause.

They overlap in one place only: CR-007's cell-2 wrapping phrase. The combined text replaces that phrase but keeps its intent, and cell 3 matches CR-007 word for word. Two notes for whoever applies the row:
- CR-009 labels its `:196` edit "first cell", but the possessive is in cell 2. Go by the quoted phrase.
- The phrase "its certificate-validation subtype of `MessagingException`" describes the library after the migration, which nobody has measured, as this record says itself. The condition-12 loopback test must confirm or narrow it before v2 is approved.

**SNI in its own record: agreed, and raise it now.** An SNI statement adds a guarantee, whereas this record adds none, and its pinned-factory half depends on the BUG-020 decision. If no record carries Record 6 (`thunderbird-mail-libraries-feasibility-analysis.md:2210`), the author of the combined revision, working from CR-007 to CR-009, will leave it out. CR-009 raised it only as a scope concern. That is how SC-1 nearly reached v2 uncorrected. Raise the record before the combined revision: the default factory's clause now, the pinned clause after the BUG-019/BUG-020 decision. Do not write "the pinned factory sets no server name" into the contract before that decision. The statement belongs in v2, not in an erratum, because it adds lines and would shift the line numbers CR-007 to CR-009 cite.

**Where the scope concerns belong.**
- B (a handshake failure with any other cause leaves `open()` as a plain I/O error): this is true at `ImapConnection.java:150`. The folder paths I read then turn it into a plain `MessagingException` (`ImapFolder.java:100-102`, `:171-172`, `:1377-1386`). So the transport sees "certificate rejection" or "other error", and both fail closed. It belongs in the contract as one sentence or row in v2, not in the erratum. The BUG-019 decision makes it more likely in practice: after a protocol floor is raised, a server limited to TLS 1.1 would fail this way.
- C (the wrapping is shown by reading the code only): this belongs to the Part B loopback test (condition 12, CR-009 T6). It does not belong in the contract, nor in U-064, whose scope excludes the IMAP connection class. The erratum already avoids claiming that it is tested.
- The two test Javadoc comments that cite "SC-1": no action for now. Refresh them when those tests are next edited.
- D (pre-existing drift): CR-007 to CR-009 already carry it into v2.

**Shape.** This is a text amendment applied on its own: no code, no test, no story, and nothing else to schedule. If the product owner holds it back, it becomes the fourth input to the combined revision, with its combined row as the target text.

**Order with the related items.** Apply the erratum first; it depends on nothing. Then come U-064, the joint BUG-019/BUG-020 decision, the SNI record and the code work, with the combined revision last. Two orders conflict:
- Combined revision applied from CR-007's literal text, before or without this record: if the erratum was held, CR-007's wording brings back "surfaced from `createSocket(...)`"; if it was applied, CR-007's quoted text no longer matches the contract.
- An SNI erratum applied ahead of v2: it would shift the line numbers CR-007 to CR-009 cite.

---

## Cascade Analysis

**Root Layer:** Design and contracts (CNTR-MODERNIZATION-001 v1)

**Root Defect:** The contract places the surfacing of a certificate rejection at `createSocket(...)`, which returns an unconnected or layered socket. The rejection is raised later, by the platform, during the handshake of that socket, which the IMAP code starts on a first read or write.

**Cascade Direction:** Mid-stream down, documentary only. The code and the tests are correct and follow the code; the error propagated into one approved record (CR-007's proposed text) and into the U-063 notes that wait for a record.

### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Design and contracts | Requirements | No | Search 2: 0 files; `REQ-MODERNIZATION-002:109` names no place |
| 2 | Design and contracts | Other design documents | No | Searches 1 and 8: only the contract names `createSocket`; `CNTR-MODERNIZATION-002:253-254` and `DES-MODERNIZATION-002:468` say "fails closed" with no place |
| 3 | Design and contracts | Change records | Yes | CR-007 `:188`, `:192`, `:194` carry the wording and say it is "carried over unchanged"; CR-008 and CR-009 do not touch it |
| 4 | Design and contracts | Stories and sprints | Yes, as notes | U-063 story `:253`, `:307`, `:337`, `plan.md:1712`, `:2126`: recorded as a concern awaiting a record. Closed work |
| 5 | Design and contracts | Application code and vendored library | No | They were never written from the contract text in this respect; they are the ground truth |
| 6 | Design and contracts | Tests | No | U-063 followed the code, not the contract (Evidence) |
| 7 | Design and contracts | Documentation | No | Search 4: 0 matches |

### Fix Order

1. [ ] Design and contracts: erratum to `CNTR-MODERNIZATION-001` `:191-192` and `:196` cell 2 — ROOT FIX (now, if approved; otherwise inside the combined revision)
2. [ ] Design and contracts: the combined revision applies CR-007's cell-3 change and CR-009's possessive change to the post-erratum row (or applies the combined row above if the erratum was held) — CASCADE FIX
3. [ ] Change records: when the combined revision is applied, CR-007's proposed text for `:196` cell 2 is read as superseded by this record's combined row — CASCADE FIX (no edit to CR-007 by this record)
4. [ ] Stories and sprints: once CR-012 is approved, the orchestrator records in the U-063 story that SC-1 is "carried by CR-012" (contract-verification W1, item 2) — CASCADE FIX, closed record, orchestrator's call
5. [ ] Verification: ``grep -n "surfaced from\|thrown from `createSocket" sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md`` returns 0 lines

### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| None | — | — | U-063 is done and sprint-020 is closed. Stories U-064 (draft), BUG-019 (ready) and BUG-020 (draft) have no plan gate yet; if they cite the contract, their contract verification will read the corrected row if the erratum is applied first. Hold nothing |

---

## Plan

- **Execution Window:** Inline (a text amendment applied outside any story) if the product owner approves the erratum; otherwise Backlog, inside the combined revision.
- **Assigned To:** Solution Architect or Artifact Librarian (contract text); orchestrator (the U-063 pointer)
- **Estimated Effort:** Under 30 minutes for the two places; none for code or tests
- **Sprint:** None

### Fix Checklist

- [ ] Design and contracts: reword `CNTR-MODERNIZATION-001` `:191-192` and `:196` cell 2 as in Proposed Change (line-count-neutral)
- [ ] Design and contracts: set the contract's `change_records` field (Librarian-owned); no version change for the erratum
- [ ] Design and contracts: apply CR-007 and CR-009 to the post-erratum row in the combined revision, or use the combined row
- [ ] Open a sibling record for the SNI statement (Record 6) before the combined revision
- [ ] Verification: 0 remaining "surfaced from `createSocket`" in the contract

## Next Steps

CR filed and queued for sprint planning. Run `/amp:create-stories --from-cr CR-012` when ready to schedule.

Read this with the timing above. This is a text amendment, not a story to schedule: there is no code, no test and no behaviour to build. The recommended route is a text-only erratum by the Solution Architect or Artifact Librarian after the product owner approves the record, or, if the product owner prefers one revision, inclusion in the combined revision with CR-007 to CR-009. A stand-alone repair story is not recommended. Two actions follow regardless: raise a sibling record for the SNI statement that Record 6 asks for (it is not covered here), and leave the protocol-version wording until the BUG-019 decision.

---

## Execution

### Changes Made

| File | Change | Layer |
|------|--------|-------|
| *(none yet)* | Pending approval and the timing above | — |

### Verification Evidence

*(Pending)*

### Commit Traceability

| Commit | Date | Branch | Scope | Author |
|--------|------|--------|-------|--------|
| *(none yet)* | | | | |

---

## Closure

- **Root Cause Category:** Contract text written from an interface's exception list instead of from the runtime path that raises the exception (the place a security check runs was inferred, not traced).
- **Prevention Recommendation:** When a contract states where a check runs, cite the line that runs it, and have the contract-verification gate re-find that line. Route a gate's wording finding to a change record the same day it is made, so that records proposing text for the same lines (here CR-007) carry the correction instead of repeating the error.
- **Closed By:** (pending)
- **Date Closed:** (pending)
