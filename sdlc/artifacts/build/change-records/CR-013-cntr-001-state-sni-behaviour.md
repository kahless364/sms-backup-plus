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
  - U-047
  - U-062
  - U-063
  - U-064
  - BUG-015
  - BUG-019
  - BUG-020
tags:
  - change-record
  - cntr-modernization-001
  - tls
  - sni
  - thunderbird-migration
id: CR-013
title: 'CNTR-MODERNIZATION-001: state the server-name (SNI) behaviour of the default TLS socket factory; the pinned-certificate factory clause waits for the BUG-020 decision'
type: DCR
domain: change-records
summary: 'CNTR-MODERNIZATION-001 does not state the SNI (server name) behaviour of the TLS socket factories, so no contract line protects it. Product-owner decision 2026-11-17 (analysis Record 6): the contract states it in its next revision. Product-owner instruction 2026-11-20 (Record 8): raise the change record now; the clause for the pinned-certificate factory waits for the BUG-020 server-name decision.'
updated_at: '2026-11-21T01:51:35.337Z'
---

# CR-013: CNTR-MODERNIZATION-001 — state the server-name (SNI) behaviour of the default factory; pinned clause pending

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-013 |
| **Type** | DCR (Design Correction) |
| **Severity** | Low |
| **Status** | Open |
| **Reported By** | Requirements Analyst agent, for the orchestrator (product owner instruction, Record 8) |
| **Date Reported** | 2026-11-20 |
| **Source** | Sprint work: the "SNI in its own record" item left open by CR-009 (Scope Concern) and CR-012 (Scope), decided in Record 6, instructed in Record 8 |

Sibling records against the same contract: CR-007 (the `createSocket` exception class), CR-008 (the socket-factory slot), CR-009 (producer 1 becomes app-owned) and CR-012 (a rejection surfaces at the TLS handshake). All four are applied in one combined contract revision (v1 to v2). CR-012 is HELD for that revision (Record 7, "Hold for the migration"). This record is applied in the same revision, last (see Timing).

Decisions this record depends on are in the appendix "Product owner decision" of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`: Record 6 (line 2210: the contract states the SNI behaviour of the default factory, in its next revision, with CR-007 to CR-009), Record 7 (line 2220: CR-012 held) and Record 8 (line 2234: "Raise the cr for 5"; the clause for the pinned factory waits for the BUG-020 server-name decision).

---

## Finding

### Description

CNTR-MODERNIZATION-001 governs the one security-critical object that crosses from the TLS trust-policy layer to the IMAP transport: a resolved socket factory. It states what the factory validates (the producer table, the Invariant, the Validation Rules). It says nothing about the server name the factory puts on its sockets (SNI, the host name a client names in its TLS hello so a server holding several certificates can choose one).

The behaviour exists, and it is deliberate. The default factory (the vendored `DefaultTrustedSocketFactory`) does this for the `host` it is given:

- a DNS host name: it sets the server name to that host name;
- a host written as four dot-separated groups of one to three digits (the form of an IPv4 address), a host containing a colon (any written form of an IPv6 address), or a null, empty or blank host: it sets no server name and does not throw;
- a host name that passes those checks but that the platform's server-name class rejects as malformed: it sets no server name and does not throw; the socket is returned and the connection goes on.

That behaviour came from this repository's own fixes: BUG-012 (story U-045) made the fallback quiet, and BUG-015 (story U-047) added the guard and the safety net after an IP-literal or blank host made `createSocket` throw. Story U-063 pinned it with tests, and the product owner's requirement REQ-MODERNIZATION-019 (AC-B8, line 109) already asks that these cases keep passing.

The gap is in the contract. CR-009 moves this class out of the vendored library into app-owned code, and the migration moves it again. A contract that is silent on server names lets that ownership change drop or alter the behaviour without any contract line, and any contract-verification gate, noticing. Nothing is wrong today: the code does what it should, and tests now pin it. The risk is at the move.

This record adds the statement. It describes behaviour that already exists, so it changes no code and no test. It does add a commitment: once the statement is in an approved contract, a later change to the behaviour is a contract change.

The pinned-certificate factory is deliberately left out of the proposed text. It sets no server name itself today. Whether it should, or should leave the name to the platform, is the open server-name decision in BUG-020 (AC-1). The record carries two prepared alternatives for that clause and does not choose between them.

### Source of Truth

- **The code** (read this session; exact lines in Evidence): `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` (`createSocket` `:152-180`, `setSniHost` `:191-205`, `isIpOrBlankHostname` `:216-226`, `setSniViaSSLParameters` `:238-254`) and `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` (`createSocket` `:75-86`, which sets no server name).
- **The tests that pin it** (U-063, commits `5a5321d4` and `cfbeada5`): `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java` (12 tests) and `DefaultTrustedSocketFactoryJdkProviderTest.java` (AC-3 `:173`, AC-4 `:192`, AC-19 `:312`).
- **The product owner's decisions**: Records 6, 7 and 8 (above).
- **What is known about the wire, and its limits**: `sdlc/artifacts/build/sprints/sprint-020/U-063-tls-socket-factory-characterisation-tests.md` (Implementation Notes, line 315) and `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/I0c-diag.txt`. Under the desktop test provider, the provider itself puts a server name in the client hello from the host or the connect address, for both factories, and puts none for an IPv6 literal on the layered shape. Under the JDK's own provider, the factory's call is what sets the name. **Nothing was measured on a device about the client hello.** The proposed text therefore states what the factory does, not what is sent.
- **The contract being amended**: `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md`, status `approved`, version v1.

### Evidence

**The contract's silence.** Search run on the contract file, case-insensitive, pattern `\bsni\b|server.?name|hostname|host name`:

```
:95   ... `checkServerTrusted()` throws `CertificateException` on any untrusted/expired/hostname-mismatched chain.
:196  | Untrusted / expired / hostname-mismatched chain under `SYSTEM_VALIDATED` | ...
:289  validation (e.g., adding hostname-match on top of fingerprint); changing the internal storage
```

Three matches, all about host-name verification of the certificate. No match for `SNI` or `server name`. The same search over `sdlc/artifacts/design` (all documents) and over `README.md` and `sdlc/artifacts/engagement` returns 0 matches for `SNI` and `server name`. CR-007 (`:206`), CR-008 (`:243`) and CR-009 (guarantee check) each record the same 0.

**The code, with lines** (all opened this session):

```
152  public Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias)
155  TrustManager[] trustManagers = new TrustManager[] { TrustManagerFactory.get(host, port) };
167  SSLSocketFactory socketFactory = sslContext.getSocketFactory();
169  if (socket == null) {
170      trustedSocket = socketFactory.createSocket();
171  } else {
172      trustedSocket = socketFactory.createSocket(socket, host, port, true);
173  }
175  SSLSocket sslSocket = (SSLSocket) trustedSocket;
176  hardenSocket(sslSocket);
177  setSniHost(socketFactory, sslSocket, host);
```

Both socket shapes (a new unconnected socket, and a socket layered over a connected one) reach `:177` with the same `host`.

```
191  public static void setSniHost(SSLSocketFactory factory, SSLSocket socket, String hostname) {
192      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
197          setSniViaSSLParameters(socket, hostname);
198      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
199              factory instanceof SSLCertificateSocketFactory) {
201          sslCertificateSocketFactory.setHostname(socket, hostname);
202      } else {
203          setHostnameViaReflection(socket, hostname);
```

Three code paths, selected by API level. `Build.VERSION_CODES.N` is API 24. The middle path cannot be taken: `factory` comes from `sslContext.getSocketFactory()` (`:167`), which is the provider's factory, not an `SSLCertificateSocketFactory`. So below API 24 the factory takes the reflective path `:256-266`.

```
216  static boolean isIpOrBlankHostname(String hostname) {
217      if (hostname == null || hostname.trim().isEmpty()) {
218          return true;
219      }
221      if (hostname.indexOf(':') >= 0) {
222          return true;
223      }
225      return hostname.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
```

null, empty and blank: `:217-219`. IPv6, any written form that contains a colon, including the eight-group form: `:221-223`. IPv4 by form only (four groups of one to three digits, no range check): `:225`.

```
238  public static void setSniViaSSLParameters(SSLSocket socket, String hostname) {
239      if (isIpOrBlankHostname(hostname)) {
242          return;
243      }
244      SSLParameters params = socket.getSSLParameters();
245      try {
246          params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));
247          socket.setSSLParameters(params);
248      } catch (IllegalArgumentException e) {
251          Log.d(LOG_TAG, "Skipping SNI — SNIHostName rejected hostname: " + ...
253      }
254  }
```

DNS name: `:244-247` (one entry, built from the same `hostname`; no other name is ever put in the list). A name the platform class rejects: the `catch` at `:248-253` (only `IllegalArgumentException`). The method is public static and the guard is package-private.

**The pinned factory sets no server name** (`app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`):

```
75   public Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias)
77   TrustManager[] trustManagers = new TrustManager[]{ new PinnedX509TrustManager() };
81   if (socket == null) {
82       return socketFactory.createSocket();
83   } else {
84       return socketFactory.createSocket(socket, host, port, true);
```

The socket is returned as the provider made it. Search for `SNI`, `server name`, `setServerNames`, `SNIHostName` over `app/src/main`, `k9mail-vendored/src/main`, `README.md` and `sdlc/artifacts/engagement`: 13 matches, all in the vendored `DefaultTrustedSocketFactory.java`; none in the pinned factory or anywhere else in `app/src/main`.

**What the platform floor does to this.** `app/build.gradle:24` and `k9mail-vendored/build.gradle:54` still say `minSdkVersion 21`. CR-011 (approved, not applied) moves it to 24. At 21 to 23 the reflective path `:203` runs, and it has no IP-literal or blank-host guard at all (`:256-258` calls `setHostname` with whatever `host` is). The unit tests run under Robolectric at a simulated API level 29 (BUG-020, Test environment limits), so they exercise only the API 24+ path. The statements proposed below are therefore stated for "API level 24 and later", which is exactly the code the tests pin.

**Wire facts and their limits** (`I0c-diag.txt`, desktop JVM, test provider): the first handshake message of the default factory and of the pinned factory carry the same server names (`:262` and `:271` implicit, `:264` and `:273` layered DNS host), and on the layered shape an IPv6 literal gives no name for either factory (`:266`, `:275`). On the implicit shape, an IPv6 literal host still produced a name on the wire for both factories (`:280`, `:285`: the name came from the connect address, a loopback name). So **the factory not setting a name for an IP literal is not a promise that no name is sent**. The contract text must not say it is.

**Tests that pin the behaviour today** (lines opened this session): see the Traceability table in Proposed Change. In short: 10 of the 12 tests in `DefaultTrustedSocketFactorySniTest` pin it (the other 2 test the reflective fallback only, `:98`, `:133`); the JDK-provider class pins the DNS case and the eight-group IPv6 case at the level of `createSocket` itself (`:173`, `:192`) and pins that the pinned factory sets no name on its unconnected socket (`:312`). Two things are not pinned: the catch for a name the platform rejects (U-064 AC-2, AC-3, status ready, not built), and a null host (no test passes null; the code handles it at `:217`).

---

## Triage

### Classification

- **Type:** DCR (Design Correction). The design contract is incomplete against the source of truth: the code and its tests define a deliberate server-name behaviour that the contract does not mention.
  - Not a DEF: the code is not defective. REQ-MODERNIZATION-019 AC-B8 (`:109`) already requires these SNI cases to keep passing.
  - Not an RCR: no requirement, scope or behaviour changes. The requirement already exists; the contract lacks it. The product owner's decision adds a contract commitment, not product behaviour.
- **Root:** Design and contracts layer. The contract was written to pin trust behaviour (what is validated, and that the consumer never re-decides it). Server-name handling is not a trust decision, so it fell outside what the contract was written to say, though it is a property of the same object.
- **Layers Affected:** 2. Design and contracts (the root: one file, four places); stories, sprints and change records (documentary: CR-009's and CR-012's pointers to "a separate record", and BUG-020 AC-5). Requirements, application code, vendored library, tests and documentation need no change.
- **Severity Justification:** Low. The criterion is "documentation-only or future impact". Nothing is blocked and no story is in progress against the contract. The tests (U-063) and REQ-019 AC-B8 already protect the behaviour, so the contract gap matters only at the CR-009 move, and only if the combined revision is written without this record. This record is itself the control for that. A defensible alternative reading is Medium, because CR-009 rated the matching risk Medium (the guarantee "most at risk in this migration") and this record is the only thing that puts it into the contract. The create-cr skill gives Low and Medium the same next-step wording, so the choice changes no action. The product owner may raise it.

---

## Impact Analysis

> `change_records.layers` is not configured in `sdlc/config.yaml`. The layers below are the ones given for this batch: requirements, design and contracts, stories, sprints and change records, application code, vendored library, tests, documentation.

Checklist run: `knowledge/checklists/impact-analysis-checklist.md` (per-layer search, ground-truth identification, impact summary, cascade analysis). No `[TBC]` placeholders are involved.

Ground truth (layer 1, read-only): `DefaultTrustedSocketFactory.java:152-180`, `:191-254`; `PinnedCertificateSocketFactory.java:75-86`. Correct value: the behaviour listed in the Description.

### Searches run

| # | Search (ripgrep through the Grep tool) | Path | Result |
|---|----------------------------------------|------|--------|
| 1 | `\bsni\b\|server.?name\|hostname\|host name`, case-insensitive | the contract | 3 lines (`:95`, `:196`, `:289`), all about certificate host-name verification; 0 for SNI or server name |
| 2 | `\bsni\b\|server name`, case-insensitive | `sdlc/artifacts/design` | 0 matches. No other design document states it |
| 3 | same | `sdlc/artifacts/requirements` | 1 file, `REQ-MODERNIZATION-019`: `:56`, `:73`, `:84`, `:95`, `:109` (5 lines). `REQ-MODERNIZATION-002` has none |
| 4 | same | `sdlc/artifacts/stories` | 3 files: `BUG-019` 6 lines, `BUG-020` 31, `U-064` 15 |
| 5 | same | `sdlc/artifacts/build/change-records` | `CR-007:206`, `CR-008:243`, `CR-009` (19 lines), `CR-011:464`, `CR-012` (20 lines, its Scope section holds the candidate text), `CR-013` (this record). 6 files, 43 lines |
| 6 | same, excluding `evidence` folders | `sdlc/artifacts/build/sprints` | 37 files, 393 lines, all closed sprints 9 to 20. The ones that matter: the introducing fixes (BUG-012, U-045, BUG-015, U-047 and their reviews), U-062, and U-063 (story, plan, review, QA, contract-verification) |
| 7 | `\bsni\b\|server name\|server-name\|SNIHostName`, case-insensitive | `app/src/test` | 3 files, 42 lines: `DefaultTrustedSocketFactorySniTest` 28, `DefaultTrustedSocketFactoryJdkProviderTest` 12, `PinnedCertificateSocketFactoryCreateSocketTest` 2 |
| 8 | `\bsni\b\|server name\|setServerNames\|SNIHostName` | `app/src/main`, `k9mail-vendored/src/main`, `README.md`, `sdlc/artifacts/engagement` | 1 file, 13 lines: the vendored `DefaultTrustedSocketFactory.java`. 0 in `app/src/main`, `README.md`, engagement |
| 9 | `setSniViaSSLParameters(\|isIpOrBlankHostname\|setSniHost(` | repository, `*.java *.kt` | Production: `DefaultTrustedSocketFactory.java:177,191,197,216,238,239` and one other caller, `k9mail-vendored/.../store/webdav/WebDavSocketFactory.java:67` (the WebDAV store is outside the source set, CR-009 search 4). Tests: `DefaultTrustedSocketFactorySniTest` only |

### Layer Impact Matrix

| # | Layer | Affected? | Files and lines | Occurrences | Action Needed |
|---|-------|-----------|-----------------|-------------|---------------|
| 1 | Ground truth: the code | — | `DefaultTrustedSocketFactory.java:152-180,191-254`; `PinnedCertificateSocketFactory.java:75-86` | — | Ground truth (read-only). Correct as it is |
| 2 | Requirements (`sdlc/artifacts/requirements`) | No text change | `REQ-MODERNIZATION-019:73,84,95,109`: AC-B8 already asks for the SNI cases (DNS name set; IPv4, IPv6 and blank host not set). The proposed statements S1 to S4 are the same cases, so they agree | 0 changes; 5 lines read | None. Not edited |
| 3 | Design and contracts (`sdlc/artifacts/design`) | **Yes (root)** | `CNTR-MODERNIZATION-001`: a pointer sentence below the producer table (after `:96`; rows `:95` and `:96` are not edited by this record), Validation Rules `:204-219` (a new rule 6), a new section between `:219` and `:221` (`## Example`), Versioning (changelog line, `:279-291`). Other design documents: none (search 2) | 1 file, 4 places | Apply in the combined revision, last (Timing) |
| 4 | Stories, sprints and change records (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | Documentary | CR-009 Scope Concern (`:237`) and CR-012 Scope (`SNI behaviour: a separate record`) point at "a record": this is it. BUG-020 AC-5 (`:129`) asks the contract revision to cover the pinned producer or say why not: the pending clause answers it. U-064 (S5's test). BUG-019 (`:233` says SNI behaviour is unchanged by its fix) | 4 records read; none edited | None by this record. The orchestrator may add a pointer "carried by CR-013" in CR-009 and CR-012; not done here |
| 5 | Application code (`app/src/main`) | No | `PinnedCertificateSocketFactory.java:75-86` behaves as the pending clause's alternative (a) says; alternative (b) would change it (BUG-020, not this record) | 0 | None |
| 6 | Vendored library (`k9mail-vendored`) | No | `DefaultTrustedSocketFactory.java` is the ground truth. At the CR-009 move, the class and its tests move together; at minSdk 24 (CR-011) `:198-204` and `:256-266` become unreachable | 0 | None under this record |
| 7 | Tests (`app/src/test`) | No change; they pin the behaviour | See Traceability. Gaps: S5 (U-064 ready, not built), the null case of S4, and SNI at the level of `createSocket` for IPv4, blank, null and the layered shape (Scope Concerns C and D) | 3 files, 42 lines | None required by this record |
| 8 | Documentation (`README.md`, `sdlc/artifacts/engagement`) | No | Search 8: 0 matches | 0 | None |
| | **Total** | | | | 1 contract (4 places, one of them shared with CR-009) |

Layers affected: 2 (design and contracts; stories, sprints and change records). Cascade analysis follows because two layers are affected.

### Cascade Analysis

**Root Layer:** Design and contracts (CNTR-MODERNIZATION-001 v1)

**Root Defect:** The contract pins the trust behaviour of the resolved factory but not its server-name behaviour. A deliberate, tested property of the default factory (fixed in BUG-012 and BUG-015) is carried only by code and tests, and the CR-009 move changes who owns the code.

**Cascade Direction:** Mid-stream down, documentary only. The code and the tests are correct; nothing was built from the missing text.

#### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Design and contracts | Requirements | No | Search 3: REQ-019 AC-B8 states the cases already |
| 2 | Design and contracts | Other design documents | No | Search 2: 0 matches |
| 3 | Design and contracts | Change records and stories | Yes, as pointers | CR-007 `:206`, CR-008 `:243`, CR-009 Scope Concern, CR-012 Scope, BUG-020 AC-5 each note the gap and wait for a record |
| 4 | Design and contracts | Application code and vendored library | No | They are the ground truth |
| 5 | Design and contracts | Tests | No | U-063 followed the code, not the contract |
| 6 | Design and contracts | Documentation | No | Search 8: 0 matches |

#### Fix Order

1. [ ] Design and contracts: apply CR-007, CR-008, CR-012 and CR-009 in the combined revision (by quoted text), then apply CR-013 last — ROOT FIX
2. [ ] Design and contracts: when BUG-020 AC-1 (server-name part) is decided, replace the pending pinned subsection with alternative (a) or (b) — ROOT FIX, second pass (see Pinned-certificate factory clause)
3. [ ] Stories and records: the orchestrator notes in CR-009, CR-012 and BUG-020 that this record carries the SNI statement — CASCADE FIX (optional; the records are approved, ready or closed, orchestrator's call)
4. [ ] Verification: after the revision, `grep -n -i "server-name\|\bSNI\b" <contract>` shows the pointer below the table, the section, S1 to S6, rule 6 and the changelog line; each of S1 to S6 has a named test (Traceability)

#### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| U-064 | none yet (status ready) | not started | Continue. It supplies the only test for S5. No change to its scope |
| BUG-019 | none yet (status ready) | not started | Continue. Its fix touches `hardenSocket` and the protocol lists (`:83-89`, `:107`, `:116-117`), not `:191-254`; it states SNI behaviour is unchanged (`:233`) |
| BUG-020 | none yet (status ready) | not started | Continue. Its server-name decision (AC-1) selects the pending clause below. Its AC-5 is answered by this record's pending subsection |

---

## Proposed Change

Exact contract text to add to `CNTR-MODERNIZATION-001`, and where it goes. **Nothing is applied by this record.** The class name of the app-owned default factory is a Part B design decision (CR-009); the placeholder `<app-owned default factory>` is the same one CR-009 uses.

### Where the statement goes, and why

| Candidate place | Decision | Reason |
|-----------------|----------|--------|
| The producer table, third cell (`:95`) | **No (revised after the Solution Architect's point 6)** | The column is headed "Embedded `X509TrustManager` behavior" (`:93`); server-name handling is not trust-manager behaviour, so a pointer there suggests that it is. CR-009 also rewrites that row, and six testable statements do not fit a cell. Re-reading the table confirmed both |
| One sentence directly below the producer table (after `:96`, before the "App-owned wrapper note" at `:98`) | **Yes, the pointer** | It names the section for both producers, leaves CR-009's row exactly as CR-009 wrote it, and removes the dependency on the row's final form (Part B may change it again). It adds two lines (the sentence and a blank line) |
| The Invariant (`:165-187`) | **No** | The Invariant is "the transport receives a fully-resolved, never-trust-all factory and never re-decides trust". Server-name handling is not a trust property. Putting it there would dilute the one sentence every other record cites (Invariant 2 is cited by CR-007, CR-008, CR-009) |
| A new Validation Rule (`:204-219`) | **Yes, as rule 6, a one-line obligation** | The Validation Rules are the "both sides MUST honor" list. A rule makes the statements binding and states that moving the producer must not change them. It carries no detail |
| A section of its own | **Yes, the detail lives here** | Six statements, a scope line and a "not stated" list need room, and a section can be located by heading rather than by line number. The pinned-certificate subsection can later be filled in without touching the default-factory text |

**Line numbers.** The pointer sentence goes below the producer table (two lines, after `:96`). The section and rule 6 go in a second place, between the end of Validation Rule 5 (`:219`) and the heading `## Example` (`:221`). So lines from `:97` on shift, including the ones CR-007, CR-008 and CR-009 cite (`:98-101`, `:146-148`, `:153`, `:161-163`, `:173-174`, `:177-180`, `:196`, `:243`, `:275-276`, `:284-285`). That is why this record is applied **last** in the combined revision, and the applier locates both insertions by text (the table's last row, and the heading `## Example`), not by number (Timing).

### Edit 1 — pointer sentence below the producer table

The producer table rows are **not** changed by this record. Row `:95` is exactly CR-009's combined text (cell 2 rewritten by CR-009, cell 3 word for word); row `:96` is unchanged (its clause is pending). The combined target text for the default producer's row is therefore CR-009's proposal as it stands:
```
| `SYSTEM_VALIDATED` (default) | `<app-owned default factory>` (`com.zegoggles.smssync...`; owned by this repository, moved from the vendored `com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory`; not supplied by the library) | Full Android `TrustManager` chain validation against the system CA store. `checkServerTrusted()` throws `CertificateException` on any untrusted/expired/hostname-mismatched chain. |
```
Insert, as a paragraph directly after the table's last row (the `PINNED_CERTIFICATE` row, `:96` in v1) and before the "App-owned wrapper note":
```
Server-name (SNI) handling of the producers is stated in "Server-name (SNI) behaviour of the producers".
```

### Edit 2 — new Validation Rule 6 (after rule 5, `:217-219`)

```
6. The `SYSTEM_VALIDATED` producer honours statements S1 to S6 of "Server-name (SNI) behaviour of the
   producers". Moving, rewriting or re-owning that producer changes none of them. A change to any
   statement is a contract revision and changes its test in the same step.
```

### Edit 3 — new section, inserted immediately before `## Example: resolution + handoff (concrete)`

```
## Server-name (SNI) behaviour of the producers

The server name (SNI) is the host name a client names in its TLS hello so that a server holding more
than one certificate can choose one. This section states what the producer's **factory itself sets** as the
server name on the socket it returns. It never states what is sent (see "Not stated").

### `SYSTEM_VALIDATED` producer (the default factory)

On API level 24 and later, and for both sockets that `createSocket(...)` can return (a new unconnected
socket when `socket` is null, and a socket layered over the given connected socket), the default factory
behaves as follows for its `host` argument:

| # | Statement |
|---|-----------|
| S1 | If `host` is a DNS-form name (not null, not blank, containing no colon, and not four dot-separated groups of one to three digits) and the platform's server-name class (`javax.net.ssl.SNIHostName`) accepts it, the factory itself sets exactly one server name on the socket, `host`. |
| S2 | If `host` is four dot-separated groups of one to three digits (the form of an IPv4 address; the check is by form only, so `999.999.999.999` is treated the same), the factory itself sets no server name, and `createSocket(...)` does not throw because of the form of `host`. |
| S3 | If `host` contains a colon (an IPv6 address in any written form: compressed, eight-group, or other), the factory itself sets no server name, and `createSocket(...)` does not throw because of the form of `host`. |
| S4 | If `host` is null, empty or only white space, the factory itself sets no server name, and `createSocket(...)` does not throw because of the form of `host`. |
| S5 | If `host` passes the checks of S2 to S4 but the platform's server-name class rejects it as malformed, the factory itself sets no server name, `createSocket(...)` does not throw because of it, and returns the socket so that the connection proceeds. |
| S6 | Whenever the factory itself sets a server name on the socket, the name is `host`. The factory never sets a different name: not a resolved address, not a configured value, not the name of an earlier connection. |

Each statement has a test among the default factory's SNI tests. A statement and its test change together.

**Not stated** (by design, because it is not known or is not the factory's doing). Every statement above
(S1 to S6) is about what the factory itself sets, never about what is sent:
- What the platform's TLS provider puts in the client hello, for any host. A provider may supply a server
  name on its own, from `host` or from the address the socket connects to, so the name sent can differ
  from the name the factory set: S1 and S6 do not say the name set is the name sent, and S2 to S5 do not say
  no name is sent. In the one capture taken (a desktop JVM), on the implicit shape the hello carried the
  name of the connect address, even where the factory had set `host` and that differed. Nothing about the
  client hello has been measured on a device.
- Whether `createSocket(...)` throws for any reason other than the form of `host` (for example, a
  certificate problem or a failure of the socket's parameter calls other than the rejection in S5).
- Protocol versions and cipher suites. They are decided and stated separately.
- How a `host` written with non-ASCII letters is encoded in the name.
- Which names the platform's server-name class rejects. S5 covers whatever it rejects.
- Host names written as an IPv4 address in a form other than four dot-separated groups (fewer groups,
  hexadecimal). The check does not recognise them, so S1 applies to them if the platform's class accepts them.
- API levels below 24 (21 to 23 while the app's minimum stays at 21).

### `PINNED_CERTIFICATE` producer

This version does not state the server-name handling of the pinned-certificate factory. It is to be
stated once the decision recorded under BUG-020 is made.
```

The last subsection is the neutral form to use if the BUG-020 decision is still open at the time of the combined revision. It states that the contract is silent. It makes no claim about what the pinned factory does, so a later decision cannot contradict it.

### Edit 4 — changelog line (Versioning)

Add to the combined revision's changelog: "v2: states the server-name (SNI) behaviour of the default producer, statements S1 to S6 (CR-013); the pinned producer's server-name handling is not yet stated." **If the app's `minSdkVersion` is still 21 when the revision is made**, add to the same line: "API levels 21 to 23 are not stated: on them the default factory passes any host, IP literals included, to the platform with no guard." No change to the breaking and backward-compatible lists (Scope Concern E).

### Clause-by-clause check against the code

The starting point was CR-012's candidate text (CR-012, Scope, "Candidate text") and CR-009's candidate sentence. Every clause was checked against the code lines in Evidence.

| Candidate clause (CR-012 / CR-009) | Checked against | Result in the proposed text |
|------------------------------------|-----------------|-----------------------------|
| "sets the TLS server name to the host name ... when that host is a DNS name" | `:244-247` | Kept as S1, with the condition the code has: the platform class must accept the name (otherwise S5) |
| "sets no server name, and does not throw, when the host is null, empty or blank" | `:217-219` | Kept as S4. The null part has no test (Scope Concern C) |
| "contains a colon (an IPv6 literal)" | `:221-223` | Kept as S3, worded "contains a colon" because that is what the code tests. A string with a colon that is not an IPv6 address is treated the same |
| "has the form of an IPv4 literal (four dot-separated groups of one to three digits)" | `:225` | Kept as S2, and made exact: the check is by form, so out-of-range numbers (`999.999.999.999`, U-063 SC-4) are covered, and other written forms of IPv4 are not |
| "If the platform rejects the name as malformed, the factory skips SNI and the connection continues" | `:248-253` | Kept as S5. Only `IllegalArgumentException` is absorbed, which is why "any other failure" is in the not-stated list |
| (CR-009) "does not set SNI for IP-address literals or empty or blank host names" | `:217-225` | Narrowed to what the code does: only the four-group IPv4 form and any host containing a colon |
| "The pinned factory sets no server name itself and applies none of this handling" | `PinnedCertificateSocketFactory.java:75-86` | **Not carried over.** It is true today, but stating it before the BUG-020 decision could later contradict the contract (CR-012, Suggested Approach; BUG-020 line 188). See the pending clause |
| (not in either candidate) | `:192`, `:198-204`, `:256-266` | **Added: the API 24 scope.** Below API 24 the factory has no guard (`:203`), so S2 to S4 would be false there |
| (not in either candidate) | `:177`, `:246` | **Added: S6**, the product owner's "never substitutes a different name" |
| (not in either candidate) | `:169-177` | **Added: both socket shapes.** Code only for the layered shape (Scope Concern D) |

### Traceability: each statement to a code line and to the test that pins it

Test names are as in the files today; they will be retargeted when the class moves (Interactions). "Mock" means a Mockito `SSLSocket` calling the public static `setSniViaSSLParameters`. "Real socket" means `createSocket(null, host, 993, null)` under the JDK test provider.

| # | Code | Pinned by | Not pinned |
|---|------|-----------|------------|
| S1 | `:177` (call), `:192`, `:197` (API 24+ path), `:244-247` | Mock: `setSniViaSSLParameters_setsSSLParametersWithCorrectSNIHostName` (`SniTest:48`), `..._setsCorrectHostname_forDifferentHost` (`:70`), `..._dnsHostname_setsSNI` (`:151`). Real socket: `createSocket_dnsHost_setsServerNameOnRealSocket` (`JdkProviderTest:192`), which asserts one `SNIHostName` entry equal to the host | The layered shape (Scope Concern D) |
| S2 | `:225`, `:239-243` | Mock: `..._ipv4Literal_doesNotThrowAndSkipsSNI` (`SniTest:171`, host `192.168.1.10`) | `999.999.999.999` (U-063 SC-4); the real-socket level (Scope Concern D) |
| S3 | `:221-223`, `:239-243` | Mock: `..._ipv6Literal_...` (`SniTest:187`, `::1`), `..._ipv6FullLiteral_...` (`:202`, `fe80::1`), `..._ipv6EightGroupLiteral_...` (`:277`). Guard: `isIpOrBlankHostname_ipv6EightGroupLiteral_isTrue` (`:295`, including `1:2:3:4:5:6:7:8`). Real socket: `createSocket_ipv6EightGroupLiteralHost_leavesServerNamesUnset` (`JdkProviderTest:173`) | Bracketed and zone-identifier forms (they contain a colon, so the code covers them, but no test passes one) |
| S4 | `:217-219` | Mock: `..._emptyHostname_...` (`SniTest:216`), `..._blankHostname_...` (`:231`) | **null: no test passes null.** Real-socket level |
| S5 | `:245-253` | **None today.** U-064 AC-2 (mock, three names) and AC-3 (real socket, `createSocket`) are the planned tests (status ready, not built). The U-063 mutation M-A1c (catch removed) left all 16 tests green (U-064, line 48) | Everything, until U-064 is built |
| S6 | `:177` passes `host`; `:246` builds the name from the same `hostname` | Mock: `getAsciiName()` equals the host (`SniTest:63`, `:79`, `:163`). Real socket: `JdkProviderTest:199` | The negative ("never a different name") is shown only by the code having a single place that builds the list; no test tries to catch a substitution |

Mutation evidence for the two real-socket tests exists in U-063: with the factory's call at `:177` removed (M-A4), AC-4 fails and the other three tests pass (`U-063` story, Implementation Notes, line 323).

### The pinned-certificate factory clause — PENDING, two prepared alternatives, NOT YET CHOSEN

**Decision recorded after this record was approved, 2026-11-20 (added by the orchestrator):** the product owner decided BUG-020's server-name question: the pinned factory follows the same rule as the default factory ("Same as default"; Record 9 in the appendix "Product owner decision" of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`). The alternative to apply is therefore alternative (b) below, and only once the BUG-020 fix and its tests exist. The heading and the text below are kept as approved.

The product owner instructed (Record 8) that the clause for the pinned-certificate factory waits for the BUG-020 server-name decision (BUG-020 AC-1: does the pinned factory set the server name itself, or leave it to the platform?). **This record must not be applied with either alternative below before that decision is recorded.** Whoever applies the revision picks one, once.

**Alternative (a) — the pinned factory sets no server name itself (today's behaviour).** Text to replace the pending subsection:

```
### `PINNED_CERTIFICATE` producer

The pinned-certificate factory makes no server-name call of its own. Whether a server name is sent,
and which, is left to the platform's
TLS provider. This statement says nothing about what is sent.
```
- *What it means:* the factory does not call any server-name method on its sockets; the contract does not promise the pinned path sends a name, nor that it sends none. It matches `PinnedCertificateSocketFactory.java:75-86` and is pinned for the unconnected socket by `pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider` (`JdkProviderTest:312`, AC-19).
- *What it does not mean:* that no name is sent. Under the desktop test provider the pinned factory sends the same names as the default factory (`I0c-diag.txt:271`, `:273`). Nothing was measured on a device. The layered socket is not asserted by any test (`JdkProviderTest:309`). Choosing (a) makes a later move to set a name a contract change.
- *Applies without a code change.* It is true of today's code. It still waits for the combined revision (Timing).

**Alternative (b) — the pinned factory applies the same rule as the default factory.** Text:

```
### `PINNED_CERTIFICATE` producer

The pinned-certificate factory applies statements S1 to S6 to its `host` argument in the same way as
the default factory, for both sockets that `createSocket(...)` can return, on API level 24 and later.
```
- *What it means:* the same six statements bind both factories, and the "Not stated" list above applies to both.
- *What it needs first:* a code change to `PinnedCertificateSocketFactory.createSocket` (BUG-020), after which AC-19's test must be updated (BUG-020 AC-3) and the server-name assertions added under the JDK provider (BUG-020 AC-2). The pinned `createSocket` already receives `host` on both connection paths (BUG-020, line 178), so no constructor change. **Do not apply (b) until that code and its tests are in**, for the same reason the sibling records wait for their code: the contract must be true of the code it describes.

**If the decision is still open when the combined revision is made — recommendation: apply the default-factory clause and the neutral pending subsection, and leave this record open for the pinned clause.** Reasons: (1) the default clause is what Record 6 wants in place at the CR-009 move, and that move is the reason for the record; waiting for the pinned decision would let the move happen with the contract still silent. (2) The neutral subsection claims nothing about the pinned factory, so it cannot be contradicted by either decision. (3) Filling the subsection in later changes only the tail of the section, not the lines other records cite. (4) BUG-020 AC-5 asks that the contract covers the pinned producer "or the record says why not": the neutral subsection plus this paragraph is that reason. Cost: the contract carries one stated gap for a time, and the revision's changelog says so.

---

## Timing

**Applied in the combined revision with CR-007, CR-008, CR-009 and CR-012, as the last of the five.** This follows the product owner's standing decisions: the contract text waits for the migration (CR-007, CR-008, CR-009 Timing) and CR-012 is held for that revision (Record 7). This record is not applied earlier.

Why last:
- It is the only record of the five that **adds lines** (about 40, in the one place between rule 5 and `## Example`). CR-012 in its combined form is a line-for-line rewrite; CR-007 adds three comment lines near `:85`; CR-008 replaces blocks. If this record goes last, the other four are applied by their quoted text and any line shifts among them are handled once; this record is then placed by heading, after everything else has settled.
- It edits cell 3 of the row that CR-009 rewrites (`:95`). Applied after CR-009 it works on the post-CR-009 row, and the combined row above is the single target text.
- The new section's text cites no contract line numbers and no vendored file paths, so it does not need to be refreshed when the other edits move lines.

Sequence the applier follows (matching the convention in CR-012's table):

1. CR-007, CR-008, CR-012 (combined row for `:196`), CR-009 — any order among them, by quoted text (CR-012's combined row supersedes CR-007's wording for `:196` cell 2).
2. CR-013: Edit 1 (the pointer paragraph below the table, placed by the table's last row; the rows themselves are CR-009's and are not touched), Edit 2 (rule 6), Edit 3 (the section, before `## Example`), Edit 4 (changelog).
3. Check: `grep -n -i "server-name\|\bSNI\b"` on the contract returns the pointer paragraph below the table, the section heading and body, rule 6 and the changelog line; the version is v2.

Checks before applying (the applier confirms each; the record's text assumes them):
- The app's own `minSdkVersion` in `app/build.gradle` (not the vendored module's, which CR-011 may leave at 21). The section says "API level 24 and later" and makes no claim about what the app supports, so it is true whether or not the app is at 24. If the app is still at 21, nothing in the section changes; the changelog line (Edit 4) records that API 21 to 23 are not stated, because on them the reflective path (`:203`) passes any host with no guard.
- The class that now holds this behaviour still does what S1 to S6 say. If the move changed any of it, the move is the defect, not the statement (a failed behavioural assertion in the retargeted tests is a regression, per the U-063 test comments).
- U-064 is built (S5 has its tests). If not, apply S5 anyway and list its missing test in the changelog line.

**Must not happen before approval and the combined revision:** any edit to `CNTR-MODERNIZATION-001`, including a "pending revision" note; any code or test change in the name of this record; applying alternative (a) or (b) before BUG-020 AC-1 (server-name part) is decided.

---

## Interactions with other work

| Work | Interaction with the proposed text |
|------|------------------------------------|
| **U-064** (ready; three tests, tests only) | Its AC-2 and AC-3 are the only tests of S5. It does not cover null (S4) and, by the product owner's scope answer, adds no "not yet valid" fixture. It should land before the CR-009 move (its own plan). This record adds no scope to it. Its SC-1 (a stale comment at `SniTest:168` claiming `SNIHostName` throws for IP literals; true for IPv6, not for IPv4) shows why S2 is worded as "by form, the guard alone" and is not a statement about what the platform class would do |
| **minSdk 24 (CR-011, REQ-015)** | Makes `:198-204` and `:256-266` unreachable, so the section's scope ("API level 24 and later") then covers every level the app runs on; the section does not depend on that. Two of the 12 SNI tests (the reflective fallback, `SniTest:98`, `:133`) pin only the dead path and pin no statement; CR-009 already leaves their deletion to Part B. The other 10 pin S1 to S4 and S6. Applying the section before the app's build moves to 24 is safe, because the statements are scoped to API 24 and later; API 21 to 23 are then recorded as not stated |
| **CR-009 tests T1 and T2** | T1 ("the 10 `DefaultTrustedSocketFactorySniTest` cases") is now **12** tests, 10 of which pin statements (above). T2 ("a true eight-group IPv6 literal", marked Missing in CR-009) now **exists**, added by U-063 at `SniTest:277`, `:295` and `JdkProviderTest:173`; CR-009's table row is stale on both counts and is not edited here. S1 to S6 are the contract-level wording of T1 and T2. They make the "retarget, never edit to pass" rule of the U-063 test comments a contract matter |
| **BUG-019** (default factory enables TLS 1.0 and 1.1; ready) | Edits the protocol lists and `hardenSocket`, not the server-name code, and says SNI behaviour is unchanged (`:233`). The "Not stated" list keeps protocols out of this section, as Record 8 decided |
| **BUG-020** (pinned factory; ready) | Its AC-1 selects alternative (a) or (b). Its AC-5 ("the contract revision planned for the SNI statement covers the pinned producer, or the record says why not") is answered by the pending subsection and the recommendation above. If (b) is chosen, BUG-020's fix must land before the pinned text is applied |

---

## Scope Concerns (raised, not decided; for the Solution Architect and product owner)

- **A. No reason found to apply this record earlier.** Applying it before the combined revision would add about 40 lines and shift the numbers CR-008 and CR-009 cite, and it would put a statement about code that is about to move into a contract that is true of the live vendored class today. The one consideration on the other side: the move's design (Part B) should read S1 to S6 as the behaviour to preserve. That needs this record **approved**, not applied (CR-007, CR-008 and CR-009 rely on the same rule: the design cites the approved record as the target form). I recommend approval of CR-013 be treated as a precondition of the Part B move design, as CR-009 is.
- **B. API scope.** The behaviour below API 24 is different (no guard). The statements are written for "API level 24 and later" with no claim about what the app supports (the app's minSdk is 21 today; CR-011 changes requirement text, not the build). API 21 to 23 are not stated, and the changelog line records that while the app is at 21 (Timing, Edit 4).
- **C. S4 (null) and S5 (rejected name) have no test today.** S5 waits for U-064. The null case is not in U-064 and no other story. A null host cannot reach the factory from `ImapConnection` with a valid IMAP address, so the risk is small; a one-line mock test for null would pin it. Not created here.
- **D. Most statements are pinned at the helper level, not at `createSocket`.** Only S1 (DNS host) and S3 (eight-group IPv6) are shown through `createSocket`, and only for the unconnected socket (`hardenedSocket` calls `createSocket(null, ...)`, `JdkProviderTest:347`). S2, S4, S5 and the layered shape rely on reading `:169-177` and `:197`. If a refactor at the move inlined or bypassed `setSniHost`, the helper-level tests would stay green. A small set of `createSocket`-level tests (IPv4, blank, a rejected name, and the layered shape) is a test decision for a later story; U-064 AC-3 adds the rejected-name one.
- **E. Versioning classification.** The Versioning section lists breaking and backward-compatible changes. This record does not add a line to either list: rule 6 says a change to a statement is a contract revision, which is enough for its purpose. Whether, for example, "sending no name where the contract says a name" should be a listed breaking change is a Solution Architect question.
- **F. Pre-existing drift in the contract** (producer named as `ServiceBase.getBackupImapStore()`, stale citations into `BackupImapStore.java`) is already reported by CR-007, CR-008 and CR-009 and is not repeated or fixed here.

---

## Assumption Ledger

| Status | Item | Basis |
|--------|------|-------|
| verified | The contract is silent on SNI and server names | Search 1: 3 matches, all certificate host-name verification (`:95`, `:196`, `:289`) |
| verified | Server-name behaviour of the default factory (S1 to S6) | `DefaultTrustedSocketFactory.java:169-177`, `:191-205`, `:216-226`, `:238-254` |
| verified | The pinned factory sets no server name and calls none of this | `PinnedCertificateSocketFactory.java:75-86`; search 8 (0 matches in `app/src/main`) |
| verified | minSdk is 21 today; below API 24 the reflective path runs and has no guard | `app/build.gradle:24`; `DefaultTrustedSocketFactory.java:192-204`, `:256-258` |
| verified | Which tests pin which statement | `DefaultTrustedSocketFactorySniTest` read in full; `DefaultTrustedSocketFactoryJdkProviderTest:173`, `:192`, `:312`, `:345-347` |
| verified | Wire facts and their limit (provider supplies names; implicit-shape IPv6 still showed a name) | `I0c-diag.txt:262-285`; U-063 story line 315. Desktop JVM only |
| assumed | The middle code path (`factory instanceof SSLCertificateSocketFactory`) cannot be taken, because the factory comes from `sslContext.getSocketFactory()` | Read from `:167` and `:198-199`; no run. Does not change any statement (API 24+ is stated) |
| assumed | The three names `bad_host.example`, `host.example.`, `-x.example` are rejected by `SNIHostName` and pass the guard | From U-064 (line 49) quoting the U-063 code review's JDK run. I did not run code. These names are not in the contract text |
| needs_check | What a device's TLS provider puts in the client hello, for either factory | Not measured. Deliberately not stated in the contract |
| needs_check | Whether `SNIHostName` accepts other written forms of IPv4 (for example `127.1`) | Not run. Deliberately not stated; the "Not stated" list says the check does not recognise them |
| needs_check | Whether a null host can reach `createSocket` in production | Not traced. S4 says the factory sets no name and `createSocket(...)` does not throw because of the form of the host; Scope Concern C |

---

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory only — refined during story planning.

**Classification.** DCR and Low: agreed. The description of the code matches `DefaultTrustedSocketFactory.java:169-177` and `:191-266` and `PinnedCertificateSocketFactory.java:75-86` as read. One fact in this record is out of date: BUG-020 is `ready`, not `draft` (`BUG-020:3`). This record calls it draft under Affected In-Progress Work and under Interactions.

**Shape: a section of its own, a one-line rule and a pointer. This is right, with two adjustments.**
- Keeping the statements out of the Invariant is correct. The Invariant (`:165-187`) is about who decides trust, and every sibling record cites Invariant 2. Keeping six statements out of the producer table is also correct: in v2 the table is the most-edited place in the contract (CR-009 rewrites row `:95`; CR-007 `:240` notes that row `:96`'s class name changes too).
- The pointer goes in the column headed "Embedded `X509TrustManager` behavior" (`:93`). Server-name handling is not trust-manager behaviour, so a pointer there suggests that it is. I would prefer one sentence directly below the table, before the "App-owned wrapper note", that names the section for both producers. Because this record is applied last and placed by text, the extra line costs nothing, and CR-009's row stays exactly as CR-009 wrote it. Keeping the pointer in the cell is acceptable; this is a question of how it reads, not of whether it is correct.
- Rule 6 should bind only the `SYSTEM_VALIDATED` producer while the pinned clause is pending, and it does. It goes next to Validation Rule 3, which CR-007 (`:240`) and CR-008 (`:285`) also want reworded in v2. Reword rule 3 before inserting rule 6.

**API level: state the condition rather than relying on minSdk.** The section says "every Android API level the app supports (24 and later)". That is false today (`app/build.gradle:24`, `minSdkVersion 21`). It becomes true only when the REQ-015 story changes the build file. CR-011 being approved, or applied, changes requirement text, not the build. I suggest writing the statements as "on API level 24 and later", with no condition, and dropping the claim that this is the whole supported range. That wording is true whichever order the work lands in. It is true of the vendored class today, and of an app-owned copy whether or not the copy keeps the old branches. The existing "API levels below 24" line under "Not stated" then covers 21 to 23 honestly. At the time of the revision, the applier checks the app's own `minSdkVersion` (and its merged manifest), not the vendored module's; CR-011 (`:305`) says the vendored module may stay at 21. If the app is still at 21, the changelog line should name API 21 to 23 as a known unstated range. On those levels the reflective path (`:256-258`) passes any host, IP literals included, with no guard. The tests cannot show the difference either way, because they run at a simulated API 29 (`app/src/test/resources/robolectric.properties:8`).

**What the statements are about: "the factory" or "the socket `createSocket` returns".** This matters most to whoever writes the app-owned factory under CR-009.
- "The server-name step" names a piece of today's structure: the call at `:177` and the public static helper at `:238`. A re-implementation may inline that step. CR-009 and CR-011 also expect the middle and reflective branches (`:198-204`, `:256-266`) to be dropped, so the step may not survive as a unit. I would word the statements as behaviour: "the factory itself sets ..." and "`createSocket(...)` does not throw because of the form of `host`". The second sentence is the property BUG-015 actually fixed (`createSocket` used to throw), and it sits on the contract surface (`:85`).
- What the returned socket carries is not the factory's doing alone. On the layered shape, the provider's own `createSocket(socket, host, port, true)` receives the host (`:172`, and `:84` for the pinned factory) and may set a name from it. So a socket-level claim such as "the returned socket carries no name" would be false, or would depend on the provider. Keep "sets" (what the factory does) for S1 to S6. Raise only the no-throw to the `createSocket` level, and say there that the no-throw is pinned through `createSocket` only for the cases that have such a test (Scope Concern D).
- Ten of the twelve tests in `DefaultTrustedSocketFactorySniTest` call the helper `setSniViaSSLParameters` directly (`:47`-`:302`). If the re-implementation drops or renames the helper, those tests must be retargeted. Retargeted to a new helper, they still pin nothing at the `createSocket` level. So the section's sentence "Each statement has a test" stays meaningful after the move only if the move retargets them through `createSocket`. The contract should not name the helper. It is also called by `WebDavSocketFactory.java:67` (search 9), which is outside the source set and leaves with the vendored module.

**Ordering inside the combined revision.** I agree with applying this record last and placing it by heading. First apply CR-007, CR-008, CR-012 and CR-009 by their quoted text, including the Validation Rule 3 rewording that CR-007 and CR-008 add, then this record. The new section quotes no contract line numbers, so it needs no refresh. One dependency remains. Edit 1 is written against the row as CR-009 leaves it. If Part B design changes that row again (the package, or the blocklist sentence CR-009 `:225` leaves room for), append the pointer to the row as it then stands. A pointer sentence below the table avoids this dependency altogether. The contract has no changelog list yet (Versioning, `:279-291`). CR-007 (`:198`), CR-012 and this record each add a "v2:" line, so the applier creates the list once.

**The pending pinned clause.** I agree with the record's recommendation: apply the default clause with the neutral pinned subsection, and keep this record open as the authority for a second pass. Three additions:
- BUG-020 AC-5 asks for the pinned behaviour to be stated "in the contract revision ... or the AC-1 record says why not". If v2 is applied before AC-1 is decided, AC-5 can be met only by a later edit. Point AC-5 at this record's prepared alternatives, so that the later edit is this record's second pass, not new text nobody reviewed. That keeps one owner for the wording.
- The Versioning section has only "v1" and no notion of a minor revision. Decide at v2 whether filling in the pinned subsection makes a v3 or a recorded amendment to v2. Do not leave that choice to whoever does the second pass.
- Alternative (a) says the pinned socket "carries the platform provider's defaults". The protocol part of BUG-020 AC-1 is already decided (no protocol older than TLS 1.2 on either factory, `BUG-020:120`). After BUG-020's fix, the pinned socket will no longer carry the provider's defaults for protocols, and (a) would read as false. Narrow (a) to server names only, for example: "makes no server-name call of its own; whether a server name is sent, and which, is left to the platform's TLS provider".

**CR-009's test list is stale.** T1 says "the 10 `DefaultTrustedSocketFactorySniTest` cases"; the class has 12 `@Test` methods today. T2 (an eight-group IPv6 literal) is marked Missing, but it exists: `DefaultTrustedSocketFactorySniTest.java:277` and `:295`, and `DefaultTrustedSocketFactoryJdkProviderTest.java:173`. CR-009's guarantee row (`:233`) also counts "the 10 tests". CR-009 is approved, and the Part B design treats approved records as the target form (CR-009 `:243`). So fix the list before the Part B design cites it, not at the revision. The orchestrator can do this through the Artifact Librarian as a dated note on CR-009. The same note can say that CR-013 now carries the SNI statement CR-009 raised as its Scope Concern (`:237`).

**One story or several.** Applying this record is part of the combined contract revision, not a story of its own. It implies separate work, not planned here, that no story owns today: server-name tests through `createSocket` for IPv4, blank and null hosts and for the layered shape, plus a null-host case at any level. U-064's scope is fixed and should not take these on. Their natural home is the characterisation-first work that CR-009's Suggested Approach recommends before the move, because tests at the `createSocket` level are the ones that survive a re-implementation. The second pass on the pinned clause belongs with BUG-020.

**Constraints and risks for planning.**
- "Sets no name" is not "sends no name", and "sets this name" is not "sends this name". In U-063's capture (`I0c-diag.txt:262`), on the implicit shape the default factory set the DNS host, yet the hello carried a loopback name taken from the connect address. The IPv6 cases (`:280`, `:285`) did the same, and the pinned factory, which sets nothing, sent the same names as the default factory (`:271`, `:273`). The "Not stated" list should cover S1 and S6 as well as S2 to S5: the name on the wire may differ from the name the factory set.
- Promising wire behaviour nobody measured. No client hello has been captured on a device. A later wording such as "no server name is sent for an IP literal" is tempting, because the TLS server-name standard says one must not be sent. But it would be an unmeasured promise, and it should require a device capture first.
- Choosing alternative (a) by default. It is true today and needs no code, so it is the easy choice at revision time. Choosing it would pre-empt the product owner's server-name decision under BUG-020 AC-1, and it would turn option (b) into a contract change. The record's rule "not before the decision" is the control: the applier looks for a dated decision record and does not infer one from the code.
- Copying from the wrong source. CR-009 (`:247`) already warns that upstream's replacement factory lacks the blank-host guard. A re-implementation built from upstream code, rather than moved from the vendored class, would break S4 while the moved tests stay green, unless those tests are retargeted at the new class.

---

## Plan

- **Execution Window:** Backlog. A text amendment applied inside the combined contract revision (v1 to v2), last in its sequence. Not a stand-alone task.
- **Assigned To:** Solution Architect or Artifact Librarian (contract text)
- **Estimated Effort:** About 1 hour for the four edits; none for code or tests
- **Sprint:** None

### Fix Checklist

- [ ] Design and contracts: apply CR-007, CR-008, CR-012 and CR-009 first, by quoted text
- [ ] Design and contracts: Edit 1 (pointer paragraph below the producer table), Edit 2 (rule 6), Edit 3 (section, before `## Example`), Edit 4 (changelog line) from Proposed Change
- [ ] Design and contracts: confirm the three checks under Timing (minSdk 24, behaviour intact after the move, U-064 built)
- [ ] Design and contracts: when BUG-020 AC-1 (server-name part) is decided, replace the pending pinned subsection with alternative (a) or (b); for (b), only after its code and tests are in
- [ ] Verification: the contract names `S1` to `S6`, each with a named test; the `grep` under Timing step 3 returns the expected places

## Next Steps

CR filed and queued for sprint planning. Run `/amp:create-stories --from-cr CR-013` when ready to schedule.

Read this with the timing above. This is a text amendment applied inside the combined contract revision with CR-007, CR-008, CR-009 and CR-012; it is not a story to schedule on its own. There is no code, test or behaviour to build, and a stand-alone repair story is not recommended. Three things follow: (1) the product owner approves this record so that the Part B move design can cite S1 to S6; (2) the pinned-certificate clause is chosen only when BUG-020 AC-1 (server-name part) is decided; (3) U-064 should land before the move, because it supplies the only test for S5.

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

- **Root Cause Category:** A boundary contract written to pin trust behaviour omitted a related security property (server-name handling) that lives only in code and tests; the property was at risk when the code changed owner.
- **Prevention Recommendation:** For each behaviour a boundary contract's object is relied on for (validation, host-name verification, server name, pinning, hardening), state it in the contract and name the test that proves it. State what a statement does not cover (here, the wire) in the same place, so a reader cannot take a factory's call for a guarantee about the network. Raise the change record on the day a decision to state a property is made, so that no sibling record has to leave it as a "separate record" pointer.
- **Closed By:** (pending)
- **Date Closed:** (pending)
