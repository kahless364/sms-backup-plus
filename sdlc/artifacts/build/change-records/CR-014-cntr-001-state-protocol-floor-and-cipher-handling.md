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
  - U-063
  - U-064
  - U-065
  - BUG-019
  - BUG-020
tags:
  - change-record
  - cntr-modernization-001
  - tls
  - protocol-versions
  - cipher-suites
  - thunderbird-migration
id: CR-014
title: 'CNTR-MODERNIZATION-001: state the TLS protocol-version floor and the cipher-suite handling of both socket-factory producers (rule only, no suite list)'
type: DCR
domain: change-records
summary: 'CNTR-MODERNIZATION-001 does not state which TLS protocol versions and cipher suites its two producers enable. Since story U-065 (commit 12b584ab) both TLS socket factories enable only TLS 1.2 and TLS 1.3 where the platform supports them and apply one cipher-suite order and blocked list; no contract line protects that. Product-owner decision 2026-11-21 (analysis Record 11, "1 raise now"): raise the change record now; it is applied in the combined contract revision at the migration and carries the open part of BUG-020 AC-5.'
updated_at: '2026-11-21T06:27:51.847Z'
---

# CR-014: CNTR-MODERNIZATION-001 — state the protocol-version floor and the cipher-suite handling of both producers

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-014 |
| **Type** | DCR (Design Correction) |
| **Severity** | Low |
| **Status** | Open |
| **Reported By** | Requirements Analyst agent, for the orchestrator (product owner decision, Record 11, "1 raise now") |
| **Date Reported** | 2026-11-21 |
| **Source** | Sprint work: the gap named by U-065 (Scope Concern 1; contract-verification, item 5 of its section 4), left without a carrier by Record 10, and decided by the product owner in Record 11 |

Sibling records against the same contract: CR-007 (the `createSocket` exception class), CR-008 (the socket-factory slot), CR-009 (producer 1 becomes app-owned), CR-012 (a rejection surfaces at the TLS handshake) and CR-013 (the server-name behaviour). All five are approved and none is applied. This record is the sixth. All six are applied together in one combined contract revision (v1 to v2) at the migration (product-owner decisions, Records 7, 8 and 11). This record is applied in that revision after CR-013 (see Timing). Nothing is applied now.

CR-013 says protocol versions and cipher suites are "stated separately" (CR-013 `:332`), and CR-012 says a protocol statement "is a further record then" (CR-012 `:300`). This is that record.

Decisions this record depends on are in the appendix "Product owner decision" of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`:

- Record 8 (`:2227-2237`, 2026-11-20): "Yes drop anything older than tls 1.2". Neither factory enables a version older than TLS 1.2, including SSLv3 and the `SSLv2Hello` hello format. TLS 1.2 and TLS 1.3 stay enabled where the platform supports them.
- Record 9 (`:2239-2253`, 2026-11-20): "Same as default". The pinned factory applies the default factory's cipher-suite order and blocked list, the same server-name rule and the same protocol rule.
- Record 10 (`:2255-2264`, 2026-11-20): both bugs closed; BUG-020 AC-5 carried forward; the protocol and cipher-suite statements "have no carrier" until a record is raised or a dated note says why not.
- Record 11 (`:2266-2276`, 2026-11-21): answer 1, "1 raise now": raise this record now, to be applied in the combined revision. Answer 2, "2 yes": a story will prune the weak cipher suites from the lists the two factories share, before the migration's code move. That story does not exist yet (no story file under `sdlc/artifacts` is named for cipher pruning or numbered above U-065; the only mentions are Record 11, the sprint-021 retro and U-065's review findings).

---

## Finding

### Description

CNTR-MODERNIZATION-001 governs the one security-critical object that crosses from the TLS trust-policy layer to the IMAP transport: a resolved socket factory. It states what the factory validates (the producer table, the Invariant, the Validation Rules). It says nothing about which TLS protocol versions or cipher suites the factory enables on its sockets.

Those behaviours exist, are deliberate, and are tested. Since U-065 (commit `12b584ab`) each producer does this on every socket `createSocket` returns, direct or layered over a connected socket:

- **Protocols.** It enables TLS 1.2 and TLS 1.3, each only where the platform supports it (the platform's supported list, not only its default list), and no other protocol name. SSLv2Hello, SSLv3, TLS 1.0 and TLS 1.1 are never enabled. If the platform supports neither version the list is empty and there is no fallback. If the lists could not be computed when the class was first used, the protocol floor is still applied, computed from the socket itself.
- **Cipher suites.** It starts from the suites the platform enables by default, removes the suites on a blocked list, and puts the suites on a preferred-order list first, in that order. Suites on neither list follow in the platform's order.
- **Both producers.** The pinned-certificate producer does exactly what the default producer does, because it calls the default factory's two steps (`hardenSocket`, then `setSniHost`) in the same order.

The gap is in the contract. No line protects any of this. CR-009 moves the default factory into app-owned code, and the migration moves it again; the pinned factory calls two members of the class that moves. A contract that is silent lets that ownership change drop or alter the floor with no contract line, and no contract-verification gate, noticing. Nothing is wrong today: the code does what the product owner decided, 19 tests were added for it and mutation runs showed they fail when the handling is removed. The risk is at the move.

BUG-020 AC-5 asks that the pinned producer's protocol, cipher-suite and server-name behaviour be stated in the contract revision "or the AC-1 record says why not". CR-013 (alternative (b)) carries the server-name part. This record carries the protocol and cipher-suite part, which Record 10 left without a carrier.

This record adds statements about behaviour that already exists. It changes no code and no test. It does add a commitment: once the statements are in an approved contract, a later change to the floor, to the pinned-versus-default parity, or to the cipher rule is a contract change.

Two decisions shape the text, and both are argued under Proposed Change:

1. **The cipher-suite clause states a rule, not a list of suites.** The product owner has decided that a story will prune the weak suites from the shared lists before the migration. A clause that enumerated today's suites would be stale within weeks. The rule (what is removed, what is preferred, that both producers use the same lists) stays true whatever the lists hold.
2. **The protocol clause names the two protocols.** The allow-list has two entries, it is the product owner's decision (Record 8), and a later version should need a deliberate change, so the contract names it.

### Source of Truth

- **The code at HEAD `4a888b16`** (the fix is `12b584ab`; `git status --porcelain -- app k9mail-vendored` is empty, so the files equal the committed ones). Exact lines in Evidence: `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` (`:34-35`, `:37-62`, `:64-81`, `:83-89`, `:97-99`, `:101-128`, `:134-158`, `:172-185`, `:189-217`, `:226-241`) and `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` (`createSocket` `:86-104`).
- **The tests that pin it** (U-065; 19 added, 7 renamed): in `app/src/test/java/com/zegoggles/smssync/mail/`, `ssl/DefaultTrustedSocketFactoryHardeningTest.java`, `ssl/DefaultTrustedSocketFactoryJdkProviderTest.java`, `ssl/DefaultTrustedSocketFactoryHandshakeTest.java` and `PinnedCertificateSocketFactoryCreateSocketTest.java`. The Traceability table gives each test with its HEAD line.
- **The story and its results**: `sdlc/artifacts/build/sprints/sprint-021/U-065-tls12-floor-both-factories-and-pinned-factory-parity.md` (AC-1 to AC-5, AC-8; Implementation Notes `:310-350`; Review Findings `:352-363`; Scope Concern 1 at `:285`), `sprint-021/U-065/contract-verification.md` (its section 4 recommends this record and says what it should hold), `sprint-021/U-065/review-security.md` (F-1 on the cipher lists, F-2 on the empty list) and `sprint-021/U-065/qa-results.md` (section "What Remains Open", items 1 and 5).
- **The bugs**: `sdlc/artifacts/stories/BUG-019-factory-enables-tls10-and-tls11.md` (AC-5 at `:142`; closed note at `:131`) and `sdlc/artifacts/stories/BUG-020-pinned-factory-no-tls-hardening.md` (AC-5 at `:135`; closed note at `:122`, which says the protocol-floor and cipher-suite statements "have no change record yet").
- **The product owner's decisions**: Records 8, 9, 10 and 11 (above).
- **What is known about a device, and its limits**: BUG-019, section "Measurements on two emulators (2026-11-20)": the Android 16 emulator supports and enables only TLS 1.2 and 1.3; the Android 11 emulator supports and enables TLS 1.0 to 1.3; one server (Gmail's IMAP endpoint) accepted TLS 1.2 and negotiated TLS 1.3. **Nothing was measured on the app's own sockets on a device after the fix** (U-065 security review F-5, residual risk statement). The proposed text therefore states what the factory enables, not what is negotiated.
- **The contract being amended**: `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md`, status `approved`, version v1.

### Evidence

**The contract's silence.** Search run on the contract file, case-insensitive, pattern `protocol|cipher|harden|tls ?1\.|sslv|version|suite|downgrade|floor`:

```
:76   consumer with respect to trust: the consumer cannot inspect, downgrade, or replace the TrustManager
:202  silent downgrade. (This is the inversion of the deleted `migrate()` downgrade behavior — ARCH-008.)
:279  ## Versioning
:281  - **Current version:** v1.
:282  - **Breaking changes** (require a new contract version and consumer migration):
```

Five lines. `:76` and `:202` use "downgrade" about the trust manager and the deleted `migrate()`; `:279-282` are the Versioning headings. Not one line mentions a TLS protocol version, a cipher suite, SSLv3, TLS 1.x or hardening of a socket. The Versioning section's lists (`:282-291`) are, in full: breaking changes are a third `TlsTrustPolicy` constant, changing the carried type, letting the consumer make a trust decision, and renaming the policy names; backward-compatible changes are tightening `PinnedCertificateSocketFactory` validation, changing the internal storage format of `PinnedCertStore`, and moving the consumer to `K9MailTransport`. Neither list says where a change to protocol or cipher handling falls.

**The code, with lines** (all opened this session, HEAD).

The one allow-list of protocol names (`DefaultTrustedSocketFactory.java`):

```
91   /**
92    * The only protocol versions this factory ever enables, where the platform supports them
93    * (BUG-019, BUG-020; product owner decision Record 8 of 2026-11-20: nothing older than
94    * TLS 1.2). A name not in this list is never enabled, including a protocol name the factory
95    * does not know, so a later TLS version needs an entry here before it is used.
96    */
97   protected static final String[] ALLOWED_PROTOCOLS = {
98           "TLSv1.2", "TLSv1.3"
99   };
```

The protocol step: it reads the platform's supported list, removes the blocked protocols, orders the known ones first, and keeps only names equal to an allowed name:

```
172  protected static String[] protocolsToEnable(SSLSocket socket) {
173      String[] ordered = reorder(socket.getSupportedProtocols(), ORDERED_KNOWN_PROTOCOLS,
174              BLACKLISTED_PROTOCOLS);
175      List<String> enabled = new ArrayList<String>();
176      for (String protocol : ordered) {
177          for (String allowed : ALLOWED_PROTOCOLS) {
178              if (allowed.equals(protocol)) {
179                  enabled.add(protocol);
180                  break;
181              }
182          }
183      }
184      return enabled.toArray(new String[enabled.size()]);
185  }
```

`:173` is the supported list (so TLS 1.2 and 1.3 are enabled where supported, also where the platform leaves one off by default: V2). `:177-181` is the exact-name match (V1 and V3: SSLv2Hello, SSLv3, TLSv1, TLSv1.1 and any unknown name never pass). If nothing passes, `:184` returns an empty array and there is no fallback branch (V4; the Javadoc at `:160-171` says so). The protocol step has no `Build.VERSION` use; none exists between `:134` and `:241` (the only uses are `:195` for client certificates and `:244`, `:250` for the server name), so V1 to V5 carry no API-level condition.

The two lists are computed once, when the class is first used:

```
101  static {
102      String[] enabledCiphers = null;
103      String[] enabledProtocols = null;
105      try {
106          SSLContext sslContext = SSLContext.getInstance("TLS");
107          sslContext.init(null, null, null);
108          SSLSocketFactory sf = sslContext.getSocketFactory();
109          SSLSocket sock = (SSLSocket) sf.createSocket();
110          enabledCiphers = sock.getEnabledCipherSuites();
118          enabledProtocols = protocolsToEnable(sock);
119      } catch (Exception e) {
120          Log.e(LOG_TAG, "Error getting information about available SSL/TLS ciphers and " + ...
122      }
124      ENABLED_CIPHERS = (enabledCiphers == null) ? null :
125              reorder(enabledCiphers, ORDERED_KNOWN_CIPHERS, BLACKLISTED_CIPHERS);
127      ENABLED_PROTOCOLS = enabledProtocols;
128  }
```

The cipher list starts from the platform's enabled suites (`:110`, C1) and goes through `reorder` with the preferred-order list `ORDERED_KNOWN_CIPHERS` (`:37-62`, 24 names at HEAD) and the blocked list `BLACKLISTED_CIPHERS` (`:64-81`, 16 names at HEAD). `reorder`:

```
134  protected static String[] reorder(String[] enabled, String[] known, String[] blacklisted) {
138      // Remove blacklisted items                      (:139-143)
145      // Order known items                             (:146-151)
153      // Add unknown items at the end. This way security won't get worse when unknown ciphers
154      // start showing up in the future.
155      result.addAll(unknown);
```

Blocked suites are removed (`:139-143`, C2). Known suites come first in the known list's order (`:146-151`, C3). **A suite named on neither list is appended and stays enabled** (`:153-155`). This matters for the pruning story (see Interactions).

The step that applies both lists, and the branch for a failed initialisation:

```
226  public static void hardenSocket(SSLSocket sock) {
227      hardenSocket(sock, ENABLED_CIPHERS, ENABLED_PROTOCOLS);
228  }
236  protected static void hardenSocket(SSLSocket sock, String[] ciphers, String[] protocols) {
237      if (ciphers != null) {
238          sock.setEnabledCipherSuites(ciphers);
239      }
240      sock.setEnabledProtocols(protocols != null ? protocols : protocolsToEnable(sock));
241  }
```

`:240`: a null protocol list (initialisation failed, `:119-122`) does not leave the platform's protocols; the list is computed from the socket (V5). `:237`: a null cipher list **does** leave the platform's suites, so in that one case the blocked list is not applied (Scope Concern A; this is why C1 to C3 are scoped by a "Not stated" line below).

Both socket shapes reach the same step in the default factory, and the server-name step runs after it:

```
206  if (socket == null) {
207      trustedSocket = socketFactory.createSocket();
208  } else {
209      trustedSocket = socketFactory.createSocket(socket, host, port, true);
210  }
212  SSLSocket sslSocket = (SSLSocket) trustedSocket;
213  hardenSocket(sslSocket);
214  setSniHost(socketFactory, sslSocket, host);
```

The pinned producer does the same with the same two calls (`PinnedCertificateSocketFactory.java`):

```
88   TrustManager[] trustManagers = new TrustManager[]{ new PinnedX509TrustManager() };
93   if (socket == null) {
94       sslSocket = (SSLSocket) socketFactory.createSocket();
95   } else {
96       sslSocket = (SSLSocket) socketFactory.createSocket(socket, host, port, true);
101  DefaultTrustedSocketFactory.hardenSocket(sslSocket);
102  DefaultTrustedSocketFactory.setSniHost(socketFactory, sslSocket, host);
103  return sslSocket;
```

The pinned factory has no list of its own; it imports the default factory's class (`:20`). Searching `app/src/main` and `k9mail-vendored/src/main` for `setEnabledProtocols|setEnabledCipherSuites|setSSLParameters|hardenSocket(|protocolsToEnable(|ALLOWED_PROTOCOLS` finds only these: the default factory (`:97`, `:118`, `:172`, `:177`, `:213`, `:226-227`, `:236-240`, and `setSSLParameters` at `:299`, which is the server-name step) and the one pinned call (`:101`). Nothing in the IMAP store code (`k9mail-vendored/.../store/imap`) calls `setEnabledProtocols` or `SSLParameters`, so nothing re-enables a protocol after `createSocket` returns (today; see Scope Concern E for the migration).

**What a refusal looks like.** `ImapConnection.open()` (`k9mail-vendored/.../store/imap/ImapConnection.java:100-144`) catches `SSLException` (`:132`); `handleSslException` (`:146-152`) wraps it only when its direct cause is a `CertificateException` and otherwise rethrows it unchanged (`:150`); a `finally` closes the connection (`:138-143`); there is no retry with another factory or an older protocol. The tests show an old-protocol server is refused with an `SSLException` whose direct cause is not a `CertificateException` (the probe saw `javax.net.ssl.SSLHandshakeException`, `U-065/evidence/I2-probe.txt:10`, `:12`). No test drives `ImapConnection`, so the step from that exception to what the transport sees rests on reading `:146-152` (CR-012 Scope Concern B says the same).

**Wire facts and their limits.** Under the desktop Conscrypt test provider a client with the provider's defaults completed handshakes at TLS 1.1 and TLS 1.0, and a client with the floor was refused (`I2-probe.txt:9-12`). The JDK provider refuses TLS 1.0 and 1.1 by its own property whatever the client enables, so a refusal test under it would prove nothing about the floor (U-065 AC-8); the refusal is shown under Conscrypt only. Both providers accept an empty protocol list at set time (`I2-probe.txt:6`, `:26`); no test starts a handshake with none enabled (security review F-2, code review W1). **None of this was measured on a device.** The proposed text must not say what is negotiated.

---

## Triage

### Classification

- **Type:** DCR (Design Correction). The design contract is incomplete against the source of truth: the code and its tests define a deliberate protocol and cipher-suite behaviour that the contract does not mention. Same type as CR-013, for the same reason.
  - Not a DEF: the code is not defective. Records 8 and 9 decided this behaviour and U-065 built it. The one weakness found in the code (the cipher branch for a failed initialisation, `:237`) is raised as Scope Concern A and is not what this record is about.
  - Not an RCR: no requirement, scope or product behaviour changes. The decisions exist; the contract lacks them. The record adds a contract commitment, not product behaviour.
- **Root:** Design and contracts layer. The contract was written to pin trust behaviour (what is validated, and that the consumer never re-decides it). Protocol and suite selection is not a trust decision, so it fell outside what the contract was written to say, though it is a property of the same object and of the same security boundary.
  - **Does Edit 6 (REQ-MODERNIZATION-019 AC-B8) change the classification? No.** The root stays the contract. One requirement criterion gains a sentence that adds a test obligation for behaviour that already exists and is decided; no requirement is wrong, and no scope or product behaviour changes, so it is still not an RCR. It adds a layer to the count (below) and one more fix in the cascade.
- **Layers Affected:** 3. Design and contracts (the root: one file, five places); requirements (one criterion, one added sentence, Edit 6); stories, sprints and change records (documentary: CR-013 `:332`, CR-012 `:300`, the stale CR-009 rows, BUG-020 AC-5, and the records that wait for a carrier). Application code, vendored library, tests and documentation need no change by this record.
- **Severity Justification:** Low, the same as CR-013. The criterion is "documentation-only or future impact". Nothing is blocked: sprint 021 is closed, no story is in progress against the contract, and the behaviour is protected today by 19 new tests with mutation evidence. The gap matters at the CR-009 move and for BUG-020 AC-5, and this record is the control for both. A defensible alternative reading is Medium: this is the security floor on the connection that carries the mail password, it was decided by the product owner, and two pieces of work (the cipher-pruning story and the CR-009 move) will touch the code before the contract revision, so a mistake could land with no contract line to catch it. The create-cr skill gives Low and Medium the same next-step wording, so the choice changes no action. The product owner may raise it. Compare CR-013: it rated the matching server-name risk Low with the same alternative.

---

## Impact Analysis

> `change_records.layers` is not configured in `sdlc/config.yaml`. The layers below are the ones given for this batch: requirements, design and contracts, stories, sprints and change records, application code, vendored library, tests, documentation.

Checklist run: `knowledge/checklists/impact-analysis-checklist.md` (per-layer search, ground-truth identification, impact summary, cascade analysis). No `[TBC]` placeholders are involved.

Ground truth (layer 1, read-only): `DefaultTrustedSocketFactory.java:97-99`, `:134-158`, `:172-185`, `:189-217`, `:226-241`; `PinnedCertificateSocketFactory.java:86-104`. Correct value: the behaviour listed in the Description.

### Searches run

| # | Search (ripgrep through the Grep tool) | Path | Result |
|---|----------------------------------------|------|--------|
| 1 | `protocol\|cipher\|harden\|tls ?1\.\|sslv\|version\|suite\|downgrade\|floor`, case-insensitive | the contract | 5 lines (`:76`, `:202`, `:279`, `:281`, `:282`); none about protocol versions, suites or socket hardening |
| 2 | `TLS ?1\.\|TLSv1\|cipher\|protocol version\|SSLv\|harden`, case-insensitive | `sdlc/artifacts/design` | No line states a protocol version or a cipher suite of a socket. Matches are "ciphertext" (CNTR-003, DES-004), the word "hardening" in titles and prose (DES-001 `:278`, DES-002 `:22`, DES-003, DES-004), and `SERVER_PROTOCOL` (the `+ssl`/`+tls` URI scheme, unrelated) in DES-002. DES-002 Decision 2 (`:146-186`) describes the pinned factory by validation only, as BUG-020 found |
| 3 | `TLS ?1\.[0-3]\|TLSv1\|cipher\|SSLv\|protocol version`, case-insensitive | `sdlc/artifacts/requirements` | 0 matches. `REQ-MODERNIZATION-019` says "TLS hardening ... must survive" in prose and lists the TLS protections its tests must preserve in AC-B8 (located by text; the file is being amended today, so no line number is cited); the list does not name a protocol floor, a suite rule or parity. `REQ-MODERNIZATION-002` has no match |
| 4 | `TLS ?1\.[0-3]\|TLSv1\|cipher suite\|SSLv\|protocol version\|hardenSocket\|ALLOWED_PROTOCOLS`, case-insensitive | `sdlc/artifacts/build/change-records` | `CR-009` 3 lines (`:85`, `:206`, `:214`), `CR-012` 5, `CR-013` 5 (including `:332`), this record's scaffold 1; `CR-007`, `CR-008`, `CR-011` 0. No approved record states the floor or the cipher handling |
| 5 | `setEnabledProtocols\|setEnabledCipherSuites\|setSSLParameters\|hardenSocket(\|protocolsToEnable(\|ALLOWED_PROTOCOLS` | `app/src/main`, `k9mail-vendored/src/main` | 2 files: the default factory (`:97`, `:118`, `:172`, `:177`, `:213`, `:226-227`, `:236-240`, `:299`) and the pinned factory (`:101`). No other caller; none in the IMAP store code |
| 6 | `ALLOWED_PROTOCOLS\|ORDERED_KNOWN_CIPHERS\|BLACKLISTED_CIPHERS\|ENABLED_CIPHERS\|ENABLED_PROTOCOLS\|hardenSocket\|protocolsToEnable` | `app/src/test` | 4 files, 44 lines: Hardening 30, Jdk 6, Handshake 4, pinned create-socket 4 |
| 7 | `TLS ?1\.[0-3]\|TLSv1\|cipher suite\|SSLv` | `README.md`, `sdlc/artifacts/engagement` | 0 matches |
| 8 | story, bug and sprint records | `sdlc/artifacts/stories`, `sdlc/artifacts/build/sprints/sprint-021` | The records that name the gap: BUG-019, BUG-020, U-065 (story, contract-verification, review-security, qa-results). No story exists for cipher pruning |
| 9 | `minSdkVersion` | `app/build.gradle`, `k9mail-vendored/build.gradle` | `app/build.gradle:24` and `k9mail-vendored/build.gradle:54`: both 21 |

### Layer Impact Matrix

| # | Layer | Affected? | Files and lines | Occurrences | Action Needed |
|---|-------|-----------|-----------------|-------------|---------------|
| 1 | Ground truth: the code | — | `DefaultTrustedSocketFactory.java:97-99,134-158,172-185,189-217,226-241`; `PinnedCertificateSocketFactory.java:86-104` | — | Ground truth (read-only). Correct as it is |
| 2 | Requirements (`sdlc/artifacts/requirements`) | **Yes, one criterion** | `REQ-MODERNIZATION-019` AC-B8 (located by its text): lists the TLS protections to test after the swap and omits the floor, the suite rule and the parity; one sentence is added (Edit 6). Its prose "TLS hardening ... must survive" is unchanged. `REQ-MODERNIZATION-002`: silent. Nothing in a requirement contradicts V1 to V5, C1 to C3, B1 to B3 | 1 criterion | Apply Edit 6 with the Part B design, in the same pass as CR-005 Change 4 (Timing) |
| 3 | Design and contracts (`sdlc/artifacts/design`) | **Yes (root)** | `CNTR-MODERNIZATION-001`: a sentence added to the pointer paragraph below the producer table (after `:96`; CR-013 adds the paragraph), a new Validation Rule 7 (after rule 5, `:217-219`, and CR-013's rule 6), a new section before `## Example` (`:221`) after CR-013's section, two lines in the Versioning lists (`:282-291`), a changelog line, and one bullet reworded in CR-013's "Not stated" list. The producer-table rows and the Invariant are not edited (reasons below). Other design documents: none (search 2) | 1 file, 5 places | Apply in the combined revision, after CR-013 (Timing) |
| 4 | Stories, sprints and change records (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | Documentary | CR-013 `:332` ("decided and stated separately": reworded when this record is applied), `:339-343` and `:389-411` (the pending pinned clause and alternatives (a) and (b); (b) is now applicable, (a) no longer describes the code); CR-012 `:300`; CR-009 `:85`, `:87`, `:206`, `:214` (T5), `:233`, `:234`, `:328-333` (stale or incomplete: see Interactions); BUG-020 AC-5 (`:135`) and its closed note (`:122`); BUG-019 `:131`; U-065 Scope Concern 1 (`:285`) | 6 records read; none edited | None by this record. The orchestrator may add "carried by CR-014" notes (closed records; its call) |
| 5 | Application code (`app/src/main`) | No | `PinnedCertificateSocketFactory.java:86-104` behaves as B1 says. At the CR-009 move its import (`:20`) and two calls (`:101`, `:102`) are retargeted | 0 | None under this record |
| 6 | Vendored library (`k9mail-vendored`) | No | `DefaultTrustedSocketFactory.java` is the ground truth. The cipher-pruning story edits its two lists (Interactions); at the move the class goes with its tests | 0 | None under this record |
| 7 | Tests (`app/src/test`) | No change; they pin the behaviour | See Traceability. Gaps: V4 at a handshake, V2 at socket level, C2 at socket level, V5's real initialiser failure | 4 files, 44 lines | None required by this record |
| 8 | Documentation (`README.md`, `sdlc/artifacts/engagement`) | No | Search 7: 0 matches | 0 | None |
| | **Total** | | | | 1 contract (5 places, three of them shared with CR-013); 1 requirement criterion |

Layers affected: 3 (design and contracts; requirements; stories, sprints and change records). Cascade analysis follows because two or more layers are affected.

### Cascade Analysis

**Root Layer:** Design and contracts (CNTR-MODERNIZATION-001 v1)

**Root Defect:** The contract pins the trust behaviour of the resolved factory but not its protocol and cipher-suite behaviour. A deliberate, tested property of both producers (decided in Records 8 and 9, built in U-065) is carried only by code and tests, and the CR-009 move changes who owns the code.

**Cascade Direction:** Mid-stream down, documentary only. The code and the tests are correct; nothing was built from the missing text.

#### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Design and contracts | Requirements | Yes, as a gap | Search 3: no requirement states a version or a suite; AC-B8 omits the floor, the suite rule and the parity, so the migration's acceptance gate would not require them (Edit 6) |
| 2 | Design and contracts | Other design documents | No | Search 2: no match states a protocol or suite of a socket |
| 3 | Design and contracts | Change records and stories | Yes, as pointers and stale rows | CR-013 `:332`, CR-012 `:300`, BUG-020 AC-5, U-065 Scope Concern 1; CR-009 rows made stale by U-065 |
| 4 | Design and contracts | Application code and vendored library | No | They are the ground truth |
| 5 | Design and contracts | Tests | No | U-065 followed the decisions and the code, not the contract |
| 6 | Design and contracts | Documentation | No | Search 7: 0 matches |

#### Fix Order

1. [ ] Design and contracts: apply CR-007, CR-008, CR-012 and CR-009 in the combined revision (by quoted text), then CR-013, then this record (Edits 1 to 5 below) — ROOT FIX. CR-013 is applied first; this record's combined pointer paragraph (Edit 1) governs and replaces CR-013's one-line paragraph; CR-013's alternative (b) is applied in the same pass (the code and tests exist since `12b584ab`)
1a. [ ] Requirements: amend REQ-MODERNIZATION-019 AC-B8 (Edit 6) with the Part B design, in the same pass as CR-005's Change 4 and the CR-010 and CR-011 edits, by one owner — CASCADE FIX
2. [ ] Stories and records: the orchestrator notes in CR-009 (the stale rows and the retarget line), CR-013 (the combined target, the "Not stated" bullet and that alternative (b) is now applicable) and BUG-020 (AC-5 carried by CR-014 and CR-013) — CASCADE FIX (optional; the records are approved or closed, the orchestrator's call)
3. [ ] Verification: after the revision, `grep -n -i "TLS 1\.2\|cipher" <contract>` shows the pointer sentence, the section heading and body, rule 7, the two Versioning lines and the changelog line; each of V1 to V5, C1 to C3 and B1 to B3 has a named test (Traceability)

#### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| (cipher pruning; no id yet) | none yet | not created | Create it with this record's C1 to C3 as acceptance (Interactions, "The cipher-pruning story"). No change to its decided scope |
| U-064, U-065, BUG-019, BUG-020 | sprint 021 | done / closed | Hold nothing. BUG-020 AC-5 waits for the applied revision |

---

## Proposed Change

Exact contract text to add to `CNTR-MODERNIZATION-001`, and where it goes. **Nothing is applied by this record.** The class names of the app-owned default factory are a Part B design decision (CR-009); the text below names no class and no constant, so it needs no change when that is decided.

### Where each statement goes, and why

| Candidate place | Decision | Reason |
|-----------------|----------|--------|
| The producer table, third cell (`:95`, `:96`) | **No** | The column is headed "Embedded `X509TrustManager` behavior" (`:93`). Protocol and suite handling is not trust-manager behaviour. CR-009 rewrites row `:95`, and CR-013 already decided not to touch the cells. Eleven testable statements do not fit a cell |
| The pointer paragraph below the table (CR-013's Edit 1) | **Yes, one added sentence** | CR-013 adds one sentence below the table that names its section. This record adds a second sentence to the same paragraph. The combined target is below, so there is one text, not two paragraphs |
| The Invariant (`:165-187`) | **No** | The Invariant is "the transport receives a fully-resolved, never-trust-all factory and never re-decides trust" and every sibling record cites Invariant 2. The protocol floor is not a trust property. Putting it there would dilute that sentence |
| A new Validation Rule | **Yes, rule 7, one obligation** | Rules are the "both sides MUST honor" list. Rule 7 makes the statements binding on both producers, says that moving a producer changes none of them, and says the two producers apply the same handling. CR-013's rule 6 binds the server-name statements; rule 7 sits beside it |
| A section of its own | **Yes** | Eleven statements, a rule about suites and a "Not stated" list need room, and a section can be found by heading, not by line number. It goes immediately after CR-013's section and before `## Example` |
| The Versioning lists | **Yes, one line in each list** | The section is silent on where a change to the floor or the suite rule falls. U-065's contract verification called "backward-compatible" true only by elimination and asked for a line. Scope Concern F asks the Solution Architect to confirm the classification |
| A changelog line | **Yes** | Combined text below |
| The error table (a new row for a refusal by an old-protocol server) | **No, not in this record** | See "The error-table row" below |
| The contract enumerating cipher suites | **No** | See "Why the contract does not list suites" below |
| REQ-MODERNIZATION-019 AC-B8 | **Yes, Edit 6, outside the contract** | The requirement's test list omits the floor, the suite rule and the parity; the Solution Architect ruled it belongs in this record (Scope Concern D) |

**Line numbers.** The sentence and rule 7 are placed by text, not by number: the sentence is appended to CR-013's pointer paragraph, rule 7 follows CR-013's rule 6, and the section goes directly before the heading `## Example: resolution + handoff (concrete)`, after CR-013's section. This record adds about 70 lines in that one place, so it is applied last in the combined revision and every insertion is located by heading or by quoted text (Timing).

### Why the contract does not list suites

The decision: **the contract states the rule and does not enumerate the suites on the lists.** Reasons:

1. **The lists are about to change, by a decision already taken.** Record 11 answer 2 sends a story to prune the weak suites from the lists the two factories share, before the migration. Today the preferred-order list has 24 names (`:37-62`) and the blocked list 16 (`:64-81`). A contract that copied them would be stale as soon as that story lands, and nothing would notice.
2. **The lists already have an authority and a guard.** The code constants are the authority, and `cipherConstants_matchRecordedBaseline` (Hardening `:271`) fails on any edit to either list, so a change must be a conscious, reviewed change. A copy in the contract is a third place with no check on it.
3. **A name list would not say what the factory does.** The enabled set is the platform's default set, filtered and ordered (C1 to C3). Listing the filter's inputs invites a reader to take them for the output.
4. **The contract should not name library constants.** CR-007 and CR-009 separate the invariants from the library binding, and the security review (F-4) recommends making the arrays private and immutable at the move. Statements in terms of roles ("the blocked list", "the preferred-order list") survive that; constant names do not.
5. **Category promises would be false today.** A line such as "no RC4, DES, 3DES, export, null or anonymous suite is enabled" is tempting. At HEAD it is not true of the code: ECDHE and ECDH 3DES suites are on the preferred-order list (`:51-54`), so they are not blocked, and no `TLS_*` null, anonymous or export name is on the blocked list (`:64-81`; the security review F-1). The statement could become true only if the pruning story delivers it. If it does, the product owner may want it stated, as a revision of C2 with its test then. It is not written now.

Cost: a reader of the contract cannot see which suites are removed. The mitigation is in the text: the section says the lists live in the producers' code and are recorded by a test, and that adding to the blocked list or reordering or removing names on the preferred-order list is not a contract revision as long as C1 to C3 and B1 still hold (removing a name from the blocked list is a breaking change, Edit 4).

### Edit 1 — the pointer paragraph below the producer table (combined target)

The producer-table rows are not edited by this record. CR-013's Edit 1 inserts, directly after the table's last row (the `PINNED_CERTIFICATE` row, `:96` in v1) and before the "App-owned wrapper note":

```
Server-name (SNI) handling of the producers is stated in "Server-name (SNI) behaviour of the producers".
```

After CR-013 is applied, replace that one-line paragraph with the following combined text (one paragraph, so there is one target):

```
Server-name (SNI) handling of the producers is stated in "Server-name (SNI) behaviour of the producers".
Protocol-version and cipher-suite handling of the producers is stated in "Protocol and cipher-suite
behaviour of the producers".
```

### Edit 2 — new Validation Rule 7 (after CR-013's rule 6)

```
7. Both producers honour statements V1 to V5, C1 to C3 and B1 to B3 of "Protocol and cipher-suite
   behaviour of the producers", and apply the same protocol and cipher-suite handling as each other.
   Moving, rewriting or re-owning either producer changes none of them. A change to any statement is a
   contract revision and changes its test in the same step.
```

### Edit 3 — new section, inserted after CR-013's section and immediately before `## Example: resolution + handoff (concrete)`

```
## Protocol and cipher-suite behaviour of the producers

This section states which TLS protocol versions and cipher suites each producer's factory itself enables
on the socket it returns. It never states which protocol or suite is negotiated with a server, because
the server and the platform decide that (see "Not stated").

Every statement holds for both sockets that `createSocket(...)` can return (a new unconnected socket
when `socket` is null, and a socket layered over the given connected socket), as the socket stands after
all of the producer's own steps have run. No statement has a condition on the Android API level.

### Protocol versions (both producers)

| # | Statement |
|---|-----------|
| V1 | A producer never enables SSLv2Hello, SSLv3, TLS 1.0 or TLS 1.1 (the platform's names `SSLv2Hello`, `SSLv3`, `TLSv1`, `TLSv1.1`) on a socket it returns, whether the platform supports them, enables them by default, or neither. |
| V2 | A producer enables TLS 1.2 and TLS 1.3 (`TLSv1.2`, `TLSv1.3`) each if and only if the platform supports it, including where the platform supports one and leaves it off by default. |
| V3 | A producer enables no protocol other than TLS 1.2 and TLS 1.3 (`TLSv1.2`, `TLSv1.3`). A protocol it does not name (a version newer than TLS 1.3, or a name only some platforms use) stays off until the producer's protocol list is deliberately changed; that change is a contract revision. |
| V4 | If the platform supports neither TLS 1.2 nor TLS 1.3, the producer enables no protocol at all. It never falls back to an older one. |
| V5 | No failure while the producer works out which protocols to enable leaves the platform's own protocols on a socket it returns: either V1 to V4 hold on the socket, or `createSocket(...)` returns no socket. |

V4 follows from the code having no fallback; no test starts a handshake from a socket with no protocol
enabled.

### Cipher suites (both producers)

A producer does not choose suites from a list of its own. It starts from the suites the platform enables
by default, removes the suites on a blocked list, and puts the suites on a preferred-order list first.
The two lists are kept in the producers' code and recorded by a test. They are not stated here. Adding
names to the blocked list, and reordering or removing names on the preferred-order list, are not contract
revisions as long as C1 to C3 and B1 still hold; removing a name from the blocked list is a breaking
change (see Versioning).

| # | Statement |
|---|-----------|
| C1 | The suites a producer enables are suites the platform enables by default. A producer never enables a suite the platform does not enable by default. |
| C2 | A suite on the blocked list is not enabled. |
| C3 | The enabled suites that are on the preferred-order list come first, in that list's order. No enabled suite that is not on that list comes before one that is, other than suites the platform places itself (see "Not stated"). |

### Both producers

| # | Statement |
|---|-----------|
| B1 | The `PINNED_CERTIFICATE` producer applies exactly the protocol-version and cipher-suite handling of the `SYSTEM_VALIDATED` producer: on the same platform, the same enabled protocols in the same order and the same enabled suites in the same order, on both sockets. They use the same blocked list and the same preferred-order list, so neither can change without the other. |
| B2 | Nothing a producer does to the socket after applying the protocol and cipher handling, setting the server name included, undoes it. The socket the producer returns still carries V1 to V5 and C1 to C3. |
| B3 | The protocol-version and cipher-suite handling reads and changes no trust manager and makes no host-name decision. It changes which certificates a producer accepts in no way. |

Each statement has a test among the factories' protocol, cipher-suite and handshake tests. A statement
and its test change together.

B1's single source for the two lists is deliberate: one source is the mechanism Record 9 chose, and two
identical copies would drift. V5 and B2 are written as behaviour, not as steps, so they hold for a rewrite
that works the lists out for each socket or sets the server name another way.

**Not stated** (by design, because it is not known or is not the producer's doing):
- Which protocol and suite are negotiated with a server. The server picks, the platform's TLS provider
  takes part, and a server limited to protocols older than TLS 1.2 is refused (V1), so the connection
  fails. Nothing about the negotiated result has been measured on a device.
- That every enabled suite is strong. C2 states what is removed and C3 what is preferred, not that what
  remains is secure.
- Whether a suite named on neither list is enabled. Today it is, and it follows the preferred ones.
- Where the platform places suites it fixes itself (for example TLS 1.3 suites and signalling suites),
  whatever order is asked for.
- Which protocols and suites a particular platform supports or enables by default.
- The suites of a socket when the producer could not read the platform's suite list when it was first
  used. Today the platform's own suites stand and the blocked list is not applied. This is not stated, so
  that closing it is not a contract revision.
- Which exception, if any, `createSocket(...)` raises when the platform refuses the protocol or suite list
  the producer sets (an empty protocol list included), and which exception the consumer sees when a
  server refuses a handshake because of its protocol or suites.
- What the mail library does to the socket after `createSocket(...)` returns.
- Trust and host-name verification (unchanged: the producer table and Invariant 2).
- The server name (see "Server-name (SNI) behaviour of the producers").
```

### Edit 4 — Versioning: two lines and a changelog line

Add to the **Breaking changes** list (Versioning, `:282-287` in v1):

```
  - Enabling, in either producer, SSLv2Hello, SSLv3, TLS 1.0 or TLS 1.1 (V1), or a cipher suite on the
    blocked list (C2); removing a name from the blocked list (it re-enables that suite, although C1 to C3
    still hold afterwards); or letting the two producers apply different protocol or cipher-suite
    handling (B1).
```

Add to the **Backward-compatible changes** list (`:288-291` in v1):

```
  - Adding a name to the producers' blocked list, or reordering or removing names on their preferred-order
    list, while C1 to C3 and B1 still hold (for example pruning weak suites); adding a later TLS version
    to the protocols the producers enable, or dropping one (each changes V2 and V3 and their tests in the
    same step, so it is a recorded revision of this contract; dropping a version cuts off servers and
    needs the product owner's decision, as the TLS 1.2 floor did).
```

Add to the combined revision's changelog: "v2: states the protocol-version and cipher-suite behaviour of both producers, statements V1 to V5, C1 to C3 and B1 to B3 (CR-014)." The combined list holds one "v2:" line from each of CR-007 (`:198`), CR-012, CR-013 and this record; the applier creates the list once. This record's line says nothing about CR-013's pinned server-name clause, so it does not conflict with it whichever form of that clause is applied.

### Edit 5 — one bullet in CR-013's "Not stated" list

CR-013's section ends its "Not stated" list with a bullet that points at a record that did not exist (CR-013 `:332`):

```
- Protocol versions and cipher suites. They are decided and stated separately.
```

When this record is applied, replace it with:

```
- Protocol versions and cipher suites. They are stated in "Protocol and cipher-suite behaviour of the
  producers"; this section says nothing about them.
```

### Prepared statement C4 — NOT part of Edit 3 unless the cipher-pruning story closes the gap (Scope Concern A)

Prepared in the way CR-013 prepared its alternatives (a) and (b). Today the cipher side of a failed
initialisation leaves the platform's suites in place (`DefaultTrustedSocketFactory.java:124-125`, `:237`),
so C1 to C3 are scoped by a "Not stated" bullet. If the pruning story (or another change) closes that gap
the way U-065 closed the protocol side (`:240`), the applier adds:

```
| C4 | C1 to C3 hold on every socket a producer returns, also when the producer could not work out its suite list in advance: either they hold on the socket, or `createSocket(...)` returns no socket. |
```

What the applier checks to decide:
1. Read the three-argument `hardenSocket` and the static initialiser (or their replacement). C4 is added only if a null or failed suite list no longer leaves the platform's suites on the socket: the list is worked out from the socket's own default-enabled suites, or the exception leaves `createSocket`.
2. A test exists for it, of the kind `hardenSocket_withNullLists_stillAppliesTheProtocolFloorFromTheSocket` (Hardening `:513`) but for the suites. That test today asserts the opposite for suites (`verify(socket, never()).setEnabledCipherSuites(any())`, `:522`), so the story must change it.
3. If both hold: add C4 to the table, change "C1 to C3" to "C1 to C4" in rule 7 and in the changelog line, and delete the "Not stated" bullet about the failed-initialisation suites. If either does not hold: keep the "Not stated" bullet and do not add C4.

Approval of this record does not wait for the decision.

### Edit 6 — REQ-MODERNIZATION-019 AC-B8 (a requirements change, not part of the contract revision)

Form as CR-005's Change 4. AC-B8 is located by its text, not by a line number: REQ-MODERNIZATION-019 is being amended today by CR-010 and CR-011 (its working tree already differs from HEAD, and AC-B8 has moved).

**Current text** (as in the working tree on 2026-11-21):

```
- **AC-B8**: Tests preserve the TLS protections: a test rejects a certificate whose hostname does not match, a test accepts a pinned certificate and rejects a different one, `DefaultTrustedSocketFactorySniTest`-equivalent cases pass (SNI set for a DNS name; not set for an IPv4 literal, an IPv6 literal, and a blank host), and `grep -rn "org.apache.http" app/src/main` returns zero matches.
```

**Proposed text** (the architect's sentence, inserted before the final grep clause, with its semicolons turned into commas to match the comma list AC-B8 already uses, and the blocked-suite clause worded so that it can be met):

```
- **AC-B8**: Tests preserve the TLS protections: a test rejects a certificate whose hostname does not match, a test accepts a pinned certificate and rejects a different one, `DefaultTrustedSocketFactorySniTest`-equivalent cases pass (SNI set for a DNS name; not set for an IPv4 literal, an IPv6 literal, and a blank host), a test shows that the socket each factory returns, on both the implicit-TLS and the STARTTLS path, has no protocol enabled other than TLS 1.2 and TLS 1.3, and that a server limited to TLS 1.1 is refused at the handshake, a test shows that the default and the pinned factory enable the same protocols and the same cipher suites in the same order, a test shows that a cipher suite on the blocked list is not enabled (fed in at the step's input where the test provider offers none), and `grep -rn "org.apache.http" app/src/main` returns zero matches.
```

Checked against AC-B8 as it reads today: the existing clauses are kept word for word; the new clauses name only behaviour that U-065's tests show today, so applying the sentence early makes nothing false (Handshake `:306-324`, pinned create-socket `:263-281`, parity at pinned create-socket `:152`, `:176` and Jdk `:495`, the blocked-list step at Hardening `:205`). Two limits are in the wording on purpose: the refusal is shown under the desktop Conscrypt provider only (the JDK provider refuses old versions by itself), and no test provider offers a blocked suite, so the blocked-suite test works at the step's input (U-065 AC-3 (c)).

**When it applies:** with the Part B design, in the same pass as CR-005's Change 4 (AC-B3) and the CR-010 and CR-011 edits to REQ-MODERNIZATION-019, and edited once by one owner. Not now. It does not overlap Change 4: a different criterion, no shared text. Without it the migration's acceptance gate does not require the floor, the suite rule or the parity, so a library swap could pass AC-B8 with the floor gone.

### The error-table row (a refusal by an old-protocol server): not in this record

U-065's contract verification offered a candidate row: a server limited to an old protocol makes the handshake fail with a non-certificate exception. Checked against CR-012 Scope Concern B (`:305`): that concern is the same fact stated in general. `handleSslException` wraps only a certificate cause (`ImapConnection.java:146-152`); any other `SSLException`, a protocol or suite mismatch included, leaves `open()` as an `IOException` and still fails closed (`:138-143`); the contract's Error Handling has no row for it; CR-012 left "whether a row is wanted, and what the transport sees" to the Solution Architect. A refusal by an old-protocol server is one instance of that class, not a separate case.

**Decision: the row belongs in the application of CR-012 Scope Concern B, not in this record.** Reasons:
1. One row for the class, not one per cause. A second row here for one instance would leave the Solution Architect to reconcile two rows for the same mechanism.
2. What the row must say (the class the transport sees) is the unmeasured part. It rests on reading `ImapConnection.java:150`, no test drives that class, and after the migration the library's class "has not been measured at the pin" (CR-012 `:240`). This record can state what the factory enables; it cannot honestly state what a not-yet-chosen library raises.
3. What this record does supply is the cause. V1 says a server limited to an older protocol is refused, and "Not stated" says which exception the consumer sees is not stated here. That keeps the contract free of a gap a reader would trip over, and leaves the row's content to CR-012's application.

Hand-over to that application, if the Solution Architect decides to add a row (wording limited to what is shown; the class column is to come from the condition-12 loopback test, CR-009 T6):

```
| Handshake fails for a reason other than a certificate rejection (for example the server offers no protocol
  at or above TLS 1.2, or no suite in common) | The handshake of the socket `createSocket(...)` returned raises an
  `SSLException` whose direct cause is not a `CertificateException` | Connection fails closed. The connection is
  closed; there is no retry with an older protocol, a wider list or the other producer. Which exception class
  the transport sees is [to be set from the observed class]. |
```

The facts behind it: the refusal is shown under the desktop Conscrypt provider by 8 tests (Handshake `:306-324`, 4 tests; pinned create-socket `:263-281`, 4 tests), each asserting an `SSLException` with a non-certificate cause; the probe saw `SSLHandshakeException`. The absence of a retry is by reading `ImapConnection.java:132-143`.

### Traceability: each statement to a code line and to the test that pins it

Short names: **Hardening** = `.../mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java`; **Jdk** = `.../mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java`; **Handshake** = `.../mail/ssl/DefaultTrustedSocketFactoryHandshakeTest.java`; **Pinned** = `.../mail/PinnedCertificateSocketFactoryCreateSocketTest.java` (all under `app/src/test/java/com/zegoggles/smssync/`). Lines are the `public void` lines at HEAD; the `@Test` is the line above. Test names will be retargeted when the class moves (Interactions). "Conscrypt" and "JDK provider" are the two test providers; neither is a device's.

| # | Code | Pinned by | Not pinned |
|---|------|-----------|------------|
| V1 | `DefaultTrustedSocketFactory.java:97-99`, `:177-181`; pinned `:101` | Conscrypt: Hardening `createSocket_legacyProtocols_notEnabled_althoughProviderEnablesThemByDefault` (`:405`; precondition that the provider's default socket has TLSv1 and TLSv1.1 on; direct and layered), Pinned `createSocket_implicit_appliesDefaultFactoryProtocolAndCipherHandling` (`:152`) and `createSocket_layered_...` (`:176`). JDK provider: Jdk `createSocket_legacyProtocols_notEnabledByFactory_whereJdkProviderSupportsThem` (`:287`), `pinnedFactory_createSocket_enablesExactlyTls12AndTls13_bothShapes_jdkProvider` (`:372`). Handshake: Handshake `implicitTls_serverLimitedToTls11_handshakeRefused` (`:306`), `startTls_...Tls11` (`:312`), `implicitTls_...Tls10` (`:318`), `startTls_...Tls10` (`:324`); Pinned `handshake_implicitTls_serverLimitedToTls11_isRefused` (`:263`), `:269`, `:275`, `:281`. Mutations M-P1/M-P8, M-P2, M-P3 | The refusal under the JDK provider (it refuses old versions itself); any device |
| V2 | `:173` (supported list), `:172-185` | Hardening `protocolsToEnable_givenSupportedList_keepsOnlySupportedTls12AndNewer` (`:480`, cases (ii) and (iii): only TLS 1.2 supported; TLS 1.2 supported but off by default); Hardening `createSocket_enabledProtocols_areProviderSupportedTls12AndNewer_inRecordedOrder` (`:363`); Jdk `..._areProviderSupportedTls12AndNewer_inRecordedOrder_jdkProvider` (`:242`). TLS 1.2 completes: Handshake `implicitTls_serverLimitedToTls12_handshakeCompletesAtTls12` (`:290`), `startTls_...` (`:296`); Pinned `:251`, `:257` | The difference between "supported" and "enabled by default" at socket level (both test providers list the same): only `:480` fails under mutation M-D11r (security review F-3) |
| V3 | `:97-99`, `:177-181` | `:480` (the allow-list against its literal copy, `:481-482`; case (iv) with an unknown name gives an empty list); Jdk `:372` (exactly TLSv1.2 and TLSv1.3) | A real platform listing a future version |
| V4 | `:184` (empty array), no fallback branch | `:480` case (iv) (`:497-501`: nothing at or above 1.2 gives an empty list); `I2-probe.txt:6`, `:26` (both providers accept an empty list at set time) | **A handshake from a socket with no protocol enabled** (review W1, F-2). Shown by reading |
| V5 | `:236-241`, `:240`; `:119-122` | Hardening `hardenSocket_withNullLists_stillAppliesTheProtocolFloorFromTheSocket` (`:513`; mutation M-F1) | **The real initialiser failure** (it runs once per class loader; only the branch's code is called directly) |
| C1 | `:104-110`, `:124-125` | Hardening `createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst` (`:297`, check (i): the set equals the provider's default set minus the blocked names) and its pinned twin `pinnedFactory_createSocket_enabledCipherSuites_...` (`:532`) | A device's provider |
| C2 | `:139-143`, `:237-238` | Hardening `reorder_removesEveryBlockedItem` (`:205`); `:297` (i); `cipherConstants_matchRecordedBaseline` (`:271`, the snapshot of both lists). Mutation M-P4 (pinned cipher handling removed alone) | **A blocked suite on a real socket.** Neither test provider offers one (`sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/I0-probe.txt:33`), so the removal is shown at the step's input, not at a socket (U-065 AC-3 (c)) |
| C3 | `:146-151`, `:153-155` | Hardening `:297` (ii) and (iii) (the provider-fixed suites are set aside by design); `reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder` (`:235`); `:532` | The provider-fixed suites (TLS 1.3 and signalling): the test measures and excludes them |
| B1 | pinned `:101`, `:102`; default `:213` | Parity under Conscrypt: Pinned `:152` (direct) and `:176` (layered), protocols as a set and in order and suites in order; under the JDK provider: Jdk `pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider` (`:495`, direct for six hosts, layered for the DNS host). Mutations M-P6 (i) cipher order swapped in one factory, (ii) floor removed from one, (iii) server-name step removed from one; M-P7 (pinned handling on the direct shape only) | Parity of the layered shape for every host under the JDK provider (the provider sets the name itself there) |
| B2 | `:213` then `:214`; pinned `:101` then `:102` | Jdk `pinnedFactory_createSocket_implicit_setsDnsHostAsOnlyServerName_jdkProvider` (`:330`, asserts exactly TLSv1.2 and TLSv1.3 and the cipher order on the same socket after the server-name step); Jdk `:372` (both shapes after the whole `createSocket`); Jdk `:495`; `I2-probe.txt:4-5` (the Conscrypt round trip kept both) | A refactor that ran the server-name step on a fresh parameter object: caught by `:330` and `:372` only under the JDK provider |
| B3 | `:226-241` touches only the two enabled lists; pinned `:88-91` installs the trust manager before | Pinned `handshake_certificateOtherThanEnrolled_isRejected` (`:232`), `handshake_enrolledCertificateForDifferentHostName_isAccepted` (`:212`); Handshake host-name, untrusted and expired tests (`:228-280`); mutations M-E4 and M-E5 re-run in U-065 and detected | — |

---

## Timing

**Applied in the combined revision with CR-007, CR-008, CR-009, CR-012 and CR-013, after CR-013.** This follows the product owner's standing decisions: the contract text waits for the migration (CR-007, CR-008, CR-009 Timing) and CR-012 is held for that revision (Record 7); Record 11 answer 1 says this record is applied in the same revision. It is not applied earlier.

Why after CR-013, and why last of the six:
- It adds the most lines (about 70, in one place before `## Example`), as CR-013 does. Placing both by heading, with this record's insertions following CR-013's, means the other four records are applied by their quoted text and any line shifts are handled once.
- It edits text CR-013 introduces: the pointer paragraph (Edit 1), CR-013's "Not stated" bullet (Edit 5), and it sits beside CR-013's rule 6 (Edit 2). Those exist only after CR-013 is applied.
- The new section quotes no contract line numbers, so it needs no refresh when other edits move lines.

Sequence the applier follows (the same convention as CR-013's table):

1. CR-007, CR-008, CR-012 (combined row for `:196`), CR-009: any order among them, by quoted text.
2. CR-013: its Edit 1 (pointer paragraph), Edit 2 (rule 6), Edit 3 (section), Edit 4 (changelog). Since U-065 the code and tests CR-013 alternative (b) waits for exist, so (b) can be applied in this one pass (Interactions); that is the orchestrator's note to CR-013, not an edit here.
3. CR-014 (this record): Edit 1 (replace the one-line pointer paragraph with the combined paragraph), Edit 2 (rule 7, after rule 6), Edit 3 (the section, after CR-013's section and before `## Example`), Edit 4 (the two Versioning lines and the changelog line), Edit 5 (CR-013's "Not stated" bullet).
4. Check: `grep -n -i "TLS 1\.2\|cipher"` on the contract returns the pointer paragraph, the section, rule 7, the Versioning lines and the changelog line; the version is v2.

Edit 6 (REQ-MODERNIZATION-019 AC-B8) is not part of this sequence. It is applied with the Part B design, in the same pass as CR-005's Change 4 and the CR-010 and CR-011 edits to the same file, by one owner, and not now (REQ-MODERNIZATION-019 is being amended today). It is true of today's code, so applying it early would make nothing false, but the in-flight edits make a second editor a conflict.

Checks before applying (the applier confirms each; the record's text assumes them):
- **The cipher-pruning story has landed or is known.** The text states a rule, so it is true whatever the lists hold. The applier still checks that C1 to C3 hold for the pruned lists (Interactions, "The cipher-pruning story") and that the story did not change the shape of the lists.
- **The class that now holds this behaviour still does what V1 to V5, C1 to C3 and B1 to B3 say.** If the move changed any of it, the move is the defect, not the statement. A failed assertion in the retargeted tests is a regression, per the U-065 test comments.
- **The library does not change the socket after `createSocket` returns** (Scope Concern E): search the library's IMAP and SSL code at the pin for `setEnabledProtocols`, `setEnabledCipherSuites` and `setSSLParameters`. Not measured yet.
- **The pinned factory's two calls were retargeted** to the app-owned default factory in the same change as the move (CR-009 checklist, Interactions).

**Must not happen before approval and the combined revision:** any edit to `CNTR-MODERNIZATION-001`, including a "pending revision" note; any code or test change in the name of this record; copying the suite lists into the contract.

---

## Interactions with other work

| Work | Interaction with the proposed text |
|------|------------------------------------|
| **CR-009** (approved, not applied; the default factory becomes app-owned) | **The move must keep V1 to V5, C1 to C3 and B1 to B3 true.** The default factory's two steps are public statics the pinned factory calls (`PinnedCertificateSocketFactory.java:101-102`, import `:20`). The move must retarget both calls and the import to the app-owned class in the same change. Once both classes share a package the shared step may become package-private, and the arrays private or unmodifiable (security review F-4). **Rows and tests in CR-009 now stale (CR-009 is not edited):** `:87` and `:234` ("producer 2 unchanged", "producer 2 is unchanged") no longer describe the code: the pinned factory now depends on the default factory's class; `:206` ("no test asserts cipher or protocol hardening") and T5 (`:214`, "Missing", describes `hardenSocket`/`reorder` only) are false: tests exist and an allow-list floor applies; `:85` cites `hardenSocket` at `:182-189`, now `:226-241`, and says nothing of `protocolsToEnable` (`:172-185`); T1 (the 10 SNI cases) and T2 (the eight-group IPv6 case, "Missing") were already stale, CR-013 says so; the checklist (`:328-333`) has no line for the retarget; there is no parity row (the tests at Pinned `:152`, `:176` and Jdk `:495`). The move's baseline is the tests as U-065 left them, not CR-009's table. **Copying risk:** a re-implementation built from upstream instead of moved from the vendored class could drop the floor with the moved tests green; CR-009 `:56` says upstream's default factory is not equivalent in the SNI respect, and whether it applies a floor was not read |
| **CR-013** (approved, open; the server-name statements) | Its "Not stated" bullet `:332` ("decided and stated separately") is reworded by Edit 5. Its pointer paragraph is extended by Edit 1 and its rule 6 sits beside rule 7. **Its second pass with alternative (b) is no longer a second pass.** Record 9 chose (b) "only once the BUG-020 fix and its tests exist"; both exist since `12b584ab`, so the combined revision applies (b) in the first pass and its pending-clause recommendation (neutral subsection, record left open) is moot. Alternative (a) (`:389-400`: "the pinned factory makes no server-name call of its own") no longer describes the code (the pinned factory calls `setSniHost`, `PinnedCertificateSocketFactory.java:102`), as U-065's contract verification noted. The layered-shape caveat (the provider sets the name itself, so no test can tell the step from the provider) belongs in CR-013's pinned subsection; it does not affect this record, because V1 to V5, C1 to C3 are observable on the layered shape (Pinned `:176`, Jdk `:372`) |
| **BUG-020 AC-5** (closed with AC-5 carried forward) | See the next section |
| **The cipher-pruning story** (Record 11, answer 2; does not exist yet) | See "The cipher-pruning story" below |
| **minSdk 24 (CR-011, REQ-015)** | V1 to V5, C1 to C3 and B1 to B3 have no API-level condition (no `Build.VERSION` use in `:134-241`), so they are unchanged by the change to 24. At 24 the platform supports and enables TLS 1.2 by default (the app's code comment cites support from API 16 and default from API 20, `:112-116` and `:160-171`). Whether TLS 1.3 is available at API 24 to 28 was not measured here (assumed not before Android 10); V2 covers it, because TLS 1.3 is enabled only where supported. At minSdk 24 V4 cannot occur on any platform the app runs on. The statements need no wait for CR-011 |
| **CR-012** (held for the combined revision) | The error-table row for non-certificate handshake failures is its Scope Concern B, not this record (above). This record's "Not stated" says which exception the consumer sees is not stated, so the two do not contradict |
| **U-064, U-065, BUG-019** | Done. Their tests are the traceability above. BUG-019 has no contract criterion, so nothing is left open on it |

### BUG-020 AC-5: which words each record covers

BUG-020 AC-5 (`:135`): "The pinned producer's protocol, cipher-suite and server-name behaviour, as decided, is stated in the contract revision that the feasibility analysis already plans for the SNI statement (`thunderbird-mail-libraries-feasibility-analysis.md:2210`), or the AC-1 record says why it is not. Search the contract for the pinned producer row (`CNTR-MODERNIZATION-001-tls-socket-factory.md:96`) and confirm."

| Words of AC-5 | Carried by | Status |
|---------------|------------|--------|
| "The pinned producer's protocol" | This record: V1 to V5 for both producers, B1 for parity, B2 | Carried; applied only when the revision is |
| "cipher-suite" | This record: C1 to C3, B1 | Carried; applied only when the revision is |
| "server-name behaviour" | CR-013 alternative (b), which is now applicable | Carried; applied only when the revision is |
| "as decided" | Records 8 and 9 (protocols, suites, server name) | Met (dated, before the fix, per U-065 AC-11) |
| "stated in the contract revision that the feasibility analysis already plans" | The combined revision (v1 to v2), in which this record and CR-013 are both applied | **Not yet true**: nothing is applied now |
| "or the AC-1 record says why it is not" | Not used. Record 9 gives no reason not to state them, and Record 11 chose to state them | Not needed |
| "Search the contract for the pinned producer row (`:96`) and confirm" | Not literally. The pinned row's third cell stays about trust, as in CR-013. The statements are in a section the pointer paragraph below the table names for both producers | **One wording gap**: whoever verifies AC-5 follows the pointer paragraph from the row, not the row itself |

Nothing in AC-5's words is uncovered once both records are applied. Until then AC-5 stays carried forward, as Record 10 decided. The orchestrator may add "carried by CR-014 and CR-013" to BUG-020 (a closed record; its call). This record does not edit it.

### The cipher-pruning story: what it must do to keep this record's text true, and what waits for it

The record is written so that **no text in it waits for the story**: it states a rule and lists no suites. The story can land before or after this record is approved or applied. What the story must do:

1. **Block what it prunes; do not only drop it from the preferred list.** `reorder` removes names on the blocked list and appends names on neither list (`:139-143`, `:153-155`). A weak suite removed from the preferred-order list and not added to the blocked list becomes an "unknown" suite: it stays enabled, last. C2 ("a suite on the blocked list is not enabled") would stay true while the intent of pruning failed. So pruning means adding to the blocked list (and may also remove from the preferred list), or changing the shape (point 3).
2. **Keep both producers on the same lists** (B1). The lists are shared today because the pinned factory calls the default factory's step. The story edits the shared constants; it must not add a pinned-only copy or a pinned-only edit. The parity tests (Pinned `:152`, `:176`; Jdk `:495`) must stay green with no change to the pinned side.
3. **If it changes the shape**, for example from "blocked list plus preferred order" to an allow-list of suites (security review F-1 suggests it), C2 and C3 and the sentence that opens the cipher subsection must be reworded in this record **before** it is applied, and the "Not stated" bullet about suites named on neither list falls away. The story's plan should say so and tell the orchestrator, who has the record changed through the Artifact Librarian.
4. **Change the recorded baselines in the same step**: `cipherConstants_matchRecordedBaseline` (Hardening `:271`) and the literal baselines `KNOWN_CIPHERS_BASELINE` (`:119`) and `BLOCKED_CIPHERS_BASELINE` (`:147`), which `assertCipherRule` and the pinned twin (`:532`) read. Rule 7 says a statement and its test change together; this is the same discipline for the lists.
5. **Add a test for C2 that does not depend on a snapshot**: each name the story prunes is on the blocked list (or the shape changed), shown by reflection or at `reorder`'s input. The snapshot test catches any edit but does not say why.
6. **Leave V1 to V5 and the null-list branch alone** unless it records the change. If the story closes the cipher branch for a failed initialisation (`:237`), the matching "Not stated" bullet becomes a statement, which is a revision of this record at that time (Scope Concern A).
7. **Not edit the contract** (held for the combined revision), and not copy suites into it.
8. **Decide the suites that are on neither list explicitly.** Suites the platform enables that appear on neither list today (for example the non-forward-secret GCM suites, security review F-1) are not touched by pruning the preferred list; they stay enabled, last (`:153-155`).
9. **An allow-list, if the story chooses one, filters the platform's default-enabled suites, not its supported ones.** Filtering the supported set would break C1, and C1 would then need rewording too.
10. **Keep the TLS 1.3 suite names enabled** (assumed, not measured): an allow-list that leaves them out may, on some providers, leave TLS 1.3 enabled with no TLS 1.3 suite, so V2 holds while the connection fails or falls back to TLS 1.2. The story checks this under both test providers.
11. **Read point 6 with Scope Concern A's prepared C4.** If the story closes the failed-initialisation branch, the applier adds C4 (above); if not, the "Not stated" bullet stays.

What waits for the story: nothing in the proposed text. At application time the applier refreshes the Traceability lines and names for C2 and C3 and confirms points 1 to 6 held. The counts in Evidence (24 known, 16 blocked) describe HEAD only. Scope Concern B is an input to the story, for the product owner's decision.

---

## Scope Concerns (raised, not decided; for the Solution Architect and product owner)

- **A. The cipher branch for a failed initialisation fails open.** If the platform query throws when the class is first used, `ENABLED_CIPHERS` is null and `hardenSocket` leaves the platform's suites (`DefaultTrustedSocketFactory.java:119-125`, `:237`); the blocked list is then not applied. U-065 closed the protocol side of that branch (`:240`) and left the cipher side as it was, because Record 8 concerns protocols (security review, section 1). Reaching the branch needs an initialisation failure that no supported platform is known to produce. The proposed text does not state this behaviour as a guarantee: it lists it under "Not stated" so that closing it later is not a contract revision. Whether to close it is a design decision; the cipher-pruning story is the natural place. Not changed here. *Architect's ruling: hand to the pruning story and keep the "Not stated" bullet for now; C4 is prepared (Proposed Change) and the applier turns the bullet into C4 only if the story closed the branch. Approval does not wait.*
- **B. The blocked list is a blocklist, and an incomplete one.** A suite named on neither list is enabled (`:153-155`). The blocked list names `SSL_*` forms and RC4 and has no `TLS_*` null, anonymous or export names; the preferred list contains CBC with SHA-1, non-forward-secret and static-ECDH suites, 3DES on ECDHE and ECDH, and DSS (security review F-1, Low). The reviewer's recommendation is an allow-list of ECDHE with AEAD suites plus the TLS 1.3 suites, which needs a product-owner decision because it can cut off an old self-hosted server. C2 and C3 are worded so that a blocklist or an allow-list both satisfy them. Input for the pruning story. *Architect's ruling (checked: the blocked list holds only `SSL_*` forms and RC4, `:64-81`): hand to the pruning story as a product-owner input; adding the `TLS_*` null, anonymous and export names is cheap if a blocklist stays and moot under an allow-list; nothing changes here.*
- **C. The error-table row.** Decided above: it belongs with CR-012 Scope Concern B. The transport-visible class rests on reading `ImapConnection.java:146-152` and is unmeasured at the pin. Not in the proposed text. *Architect's ruling: leave it to CR-012 Scope Concern B (`:305`); one row for the class; fill the class column from the observed class (contract verification O-new-1), not from `SSLException` by assumption.*
- **D. REQ-MODERNIZATION-019 AC-B8 omits the floor.** AC-B8 lists the TLS protections the tests must preserve after the library swap (host-name mismatch, pinned accept and reject, the SNI cases) and the requirement's prose says "TLS hardening ... must survive". It names no protocol floor, no suite rule and no pinned-versus-default parity. Without it the migration's test gate does not require what this record states. *Architect's ruling: act in this record. Done as Edit 6 (current text, proposed text, when it applies), applied with the Part B design as CR-005's Change 4 was, and merged into the requirements pass already touching REQ-019 so AC-B8 is edited once by one owner. The record does not edit the requirement.*
- **E. What the library does after `createSocket` returns.** The statements are about the producer. Today nothing in the IMAP store code calls `setEnabledProtocols` or `SSLParameters` (search 5), so the socket keeps what the factory set. At the pin, upstream's IMAP and SSL code was not read for this. A library that set protocols or parameters on the returned socket could undo V1. The applier checks before v2 (Timing). Not measured. *Architect's ruling: hand to the migration story's plan; the applier's check is a backstop only. The proof that survives the swap is a loopback handshake through the real store path against a server limited to TLS 1.1 (CR-009 T6, condition 12, one case more); until it exists the statements stay scoped to the producer ("Not stated").*
- **F. Versioning classification.** Edit 4 places "re-enabling an old protocol, enabling a blocked suite, or making the producers differ" in the breaking list, and "pruning the lists, adding or removing a TLS version" in the backward-compatible list with the condition that each is a recorded revision. The section's own test is "no type, name, invariant or consumer duty changes", which all of these pass; the breaking line is by analogy with re-introducing trust-all. The Solution Architect confirms the classification, and whether naming the two allowed versions in V3 (so that adding TLS 1.4 is a revision) is wanted; the alternative is to state only the floor. I kept the names because the allow-list was closed deliberately (U-065 Scope Concern 7). *Architect's ruling: confirmed, with one correction, which is made in Edit 4: the backward-compatible line is one-directional (adding to the blocked list, reordering or removing on the preferred list, adding a later TLS version, dropping one with the owner's decision); removing a name from the blocked list is in the breaking list. The named versions stay.*
- **G. Platform support claims rest on the code's own comment.** The comment at `:112-116` and the Javadoc at `:160-171` say TLS 1.2 is supported from API 16 and enabled by default from API 20, citing the platform's reference documentation. No test or probe covers it (the test providers are desktop JVMs at a simulated API 29). V2 and V4 are worded as "if the platform supports", so they do not rely on it. The contract must not state the API claim. *Architect's ruling: leave it; "where the platform supports" is the correct control.*
- **H. A consequence of U-065 on the contract's exception line.** In the branch for a failed initialisation, if the platform throws from `getSupportedProtocols()` an unchecked exception now leaves `createSocket` itself (U-065 Implementation Notes `:319`). Likewise a platform may refuse an empty protocol list. The contract's `createSocket` declares only checked exceptions (`:85-86`). "Not stated" covers which exception is raised; whether Error Handling should say a `createSocket` can raise an unchecked exception is a design question. *Architect's ruling: hand to the combined revision's applier with CR-007 (the `createSocket` exception line) and CR-012 (the handshake-failure row); V5 now states the fail-closed outcome (a socket that carries V1 to V4, or no socket).*
- **I. Pre-existing drift in the contract** (producer named as `ServiceBase.getBackupImapStore()`, stale citations) is already reported by CR-007, CR-008, CR-009 and CR-013 and is not repeated or fixed here. *Architect's ruling: leave it.*

---

## Assumption Ledger

| Status | Item | Basis |
|--------|------|-------|
| verified | The contract is silent on protocol versions, cipher suites and socket hardening | Search 1: 5 lines (`:76`, `:202`, `:279`, `:281`, `:282`), none about them |
| verified | Protocol behaviour of both producers (V1 to V5) | `DefaultTrustedSocketFactory.java:97-99`, `:101-128`, `:172-185`, `:189-217`, `:226-241`; `PinnedCertificateSocketFactory.java:86-104` |
| verified | Cipher rule (C1 to C3), including that a suite on neither list stays enabled and that a null cipher list leaves the platform's suites | `DefaultTrustedSocketFactory.java:104-110`, `:124-125`, `:134-158`, `:237-239` |
| verified | The pinned factory applies the default factory's handling by calling its two steps; nothing else in the production source calls the setters | `PinnedCertificateSocketFactory.java:20`, `:101-102`; search 5 |
| verified | No API-level branch in the protocol and cipher handling | `DefaultTrustedSocketFactory.java:134-241` read; `Build.VERSION` appears only at `:195`, `:244`, `:250` |
| verified | Which tests pin which statement, and their lines | The four test classes at HEAD (method lines from search); `qa-results.md` rows AC-1 to AC-8 |
| verified | A non-certificate `SSLException` leaves `ImapConnection.open()` unwrapped, and the connection is closed in `finally` with no retry | `ImapConnection.java:132-152`, `:138-143` |
| verified | Records 8, 9, 10 and 11 say what this record says they say | `thunderbird-mail-libraries-feasibility-analysis.md:2227-2276` |
| verified | BUG-020 AC-5 wording and its closed note | `BUG-020:135`, `:122` |
| verified | CR-007, CR-008, CR-009, CR-011, CR-012, CR-013 are approved and unapplied | Frontmatter `status: approved`; the contract is v1 and has no changelog; `git status` shows no contract change |
| verified | No story for cipher pruning exists | `find sdlc/artifacts` for names with cipher, prune, U-066 to U-079: none; text search finds only Record 11, the sprint-021 retro and U-065 mentioning the plan |
| verified | HEAD is `4a888b16`; the factories equal the committed files | `git rev-parse`; `git status --porcelain -- app k9mail-vendored` empty |
| verified | minSdk is 21 in both modules today | `app/build.gradle:24`, `k9mail-vendored/build.gradle:54` |
| assumed | The refusal class seen under the desktop Conscrypt provider (`SSLHandshakeException`) is what a device's provider raises | From `I2-probe.txt:10`, `:12`; no device run. The proposed text does not state a class |
| assumed | TLS 1.3 is not supported by the platform before Android 10 (API 29) | General knowledge, not in the repository, not measured. No statement depends on it (V2 says "supports") |
| assumed | An initialisation failure of the factory's static lists cannot happen on a supported platform | Reasoned from the code (the try block uses standard calls); not shown. Only the code of that branch is tested (`:513`) |
| needs_check | What the library at the Thunderbird pin does to a socket after the factory returns it | Not read. Scope Concern E; the applier checks before v2 |
| needs_check | Whether upstream's own default factory applies a protocol floor | Not read. CR-009 `:56` says it is not equivalent in the SNI respect. Relevant only if the move were built from upstream instead of moved |
| needs_check | What the app's own sockets negotiate on a device after the fix | Not measured (security review F-5). Deliberately not stated in the contract |

---

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory only. Refined during story planning.

**Classification.** DCR and Low: agreed. The Description matches the code as read at HEAD: `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java:97-99`, `:101-128`, `:134-158`, `:172-185`, `:206-214`, `:226-241`, and `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:86-104`. The contract is silent, as the record says (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md`, Versioning `:279-291`, which has no line on protocols or suites). Medium is defensible for the reason the record gives. Either rating leads to the same next steps.

**Shape: right.** A section of its own, one rule, one pointer sentence and Versioning lines is the right shape. Keeping the floor out of the Invariant and out of the producer table's trust-manager column is correct, for the reasons CR-013's Suggested Approach gave for the server name.

**Architectural considerations.**
- *A rule, not a list of suites: right for this contract.* The contract governs a boundary object, and the lists are an implementation input that a decided story is about to change. A copied list would be a third copy with no check. One thing should be said plainly: on today's shape, C1 does more of the protective work than C2. Null, anonymous and export suites under their `TLS_*` names are kept off only because the platform does not enable them by default (`:110`, `:124-125`). The blocked list does not name them (`:64-81`). The record's "Not stated" bullet ("that every enabled suite is strong") covers this. It is the honest statement, so keep it.
- *Naming TLS 1.2 and TLS 1.3: right; do not reduce it to a floor alone.* A floor-only statement ("nothing older than TLS 1.2") would let a re-implementation use a blocklist that admits any name it does not know. Those are the names the allow-list was written to stop (`:91-99`; U-065 security review, question 1). Advisory: give the platform's standard names once, in brackets (`TLSv1.2`, `TLSv1.3`, and in V1 `SSLv2Hello`, `SSLv3`, `TLSv1`, `TLSv1.1`). They are platform API names, not app constants, so naming them does not break the record's rule of naming no constant. They also remove the ambiguity between "TLS 1.0" and `TLSv1`.
- *Versioning (Scope Concern F): confirmed, with one correction.* Putting "re-enable an old protocol, enable a blocked suite, let the producers differ" in the breaking list follows how this contract already uses that list. Its first entry (a trust-all constant) is a security regression, not a consumer break, so "breaking" here means "not without a new version". Adding a newer TLS version, or dropping one, as a recorded backward-compatible revision is also right; dropping one matches "tightening validation" (`:288-289`). **The correction: the backward-compatible line as drafted allows "changing which suites are on the blocked list" with no direction.** Taking a name *off* the blocked list re-enables that suite, which is exactly what the breaking line forbids, and C1 to C3 still hold afterwards. Make the compatible line one-directional: adding names to the blocked list, and reordering or removing names on the preferred-order list. Put "removing a name from the blocked list" in the breaking list, or require the product owner's decision for it, as the line already does for dropping a TLS version.
- *Binding a re-implementation, not today's structure.* Most statements are about what can be observed on the returned socket, and that survives the CR-009 move and an app-owned rewrite. Three phrases tie the text to today's structure:
  - V5 ("could not read the platform's protocol list when it was first used") describes the static initialiser (`:101-128`). If a rewrite works the list out for each socket, V5 says nothing about it. Reword it as behaviour: no failure while working out the protocol list leaves the platform's own protocols on a returned socket; either V1 to V4 hold, or `createSocket(...)` returns no socket. This is what the code does: `:240`, and if `protocolsToEnable` throws there, the exception leaves `createSocket` (U-065 security review, question 1). It also takes in the fail-closed half of Scope Concern H.
  - B2 ("that producer's server-name step") names a step, as CR-013's text did. The opening scope sentence ("after all of the producer's own steps have run") already implies it. If B2 stays as an explicit guard, word it as behaviour: setting the server name does not change the enabled protocols or suites. The risk is real. Today's step reads the socket's current parameters and writes them back (`:296-299`). A rewrite that sets a parameter object built from the context's defaults would undo V1 to V5. Only the JDK-provider tests would see it (Traceability, B2).
  - B1's second sentence ("they use the same blocked list and the same preferred-order list") is the one deliberate structural binding. Keep it. A single source is the mechanism Record 9 chose, and two identical copies would drift. The record should say that this binding is intended.

**Scope Concerns: one line each.**
- **A. Hand to the cipher-pruning story, and leave "Not stated" in this record for now.** The branch fails open for suites (`:124-125`, `:237`), while the protocol side was closed (`:240`). Closing it the same way (work the list out from the socket's own defaults) is small and belongs with the story that edits these lists. Advisory: have the Requirements Analyst prepare a conditional statement C4 now ("C1 to C3 hold even when the list could not be worked out in advance"), in the way CR-013 prepared alternatives (a) and (b). The applier then turns the "Not stated" bullet into C4 only if the story closed the branch. Approval does not wait for this.
- **B. Hand to the cipher-pruning story, as a product-owner input.** Verified: the blocked list holds only `SSL_*` forms and RC4 names (`:64-81`). Adding the `TLS_*` null, anonymous and export names is cheap defence in depth if the story keeps a blocklist, and moot if it changes to an allow-list. Nothing changes in this record.
- **C. Leave it to the application of CR-012's Scope Concern B, as the record decides.** One row for the whole class of non-certificate handshake failures is right (CR-012 `:305`). Fill the class column from the observed class (U-065 contract verification, O-new-1), not from `SSLException` by assumption.
- **D. Act on it in this record: add a proposed amendment to REQ-MODERNIZATION-019 AC-B8, applied with the Part B design as CR-005's Change 4 was.** Without it, the migration's acceptance gate does not require the floor, so a library swap could pass AC-B8 with the floor gone. The amendment is true of today's code (the tests exist since U-065), so applying it early makes nothing false. The sentence to add, before AC-B8's final `org.apache.http` grep clause: "a test shows that the socket each factory returns, on both the implicit-TLS and the STARTTLS path, has no protocol enabled other than TLS 1.2 and TLS 1.3, and that a server limited to TLS 1.1 is refused at the handshake; a test shows that the default and the pinned factory enable the same protocols and the same cipher suites in the same order; a test shows that a cipher suite on the blocked list is removed from the suites the factory enables;". Coordinate with the requirements pass already touching REQ-MODERNIZATION-019 (Record 11, answer 3), so that AC-B8 is edited once, by one owner.
- **E. Hand to the migration (library-swap) story's plan, with the applier's check as a backstop only.** Reading the library's IMAP and SSL code at the pin is necessary, but it comes too late if it waits for the applier, and it proves less than a test does. The feasibility analysis records that the pinned library's connection calls `createSocket` on both paths (C-355, C-356). It does not record whether anything touches the socket's protocols afterwards. The proof that survives the swap is a loopback handshake through the real store path against a server limited to TLS 1.1, which fails if anything widens the protocols after `createSocket`. This is the condition-12 test of CR-009 T6, extended by one case. Until that test exists, the statements stay correctly scoped to the producer ("Not stated": what the mail library does afterwards).
- **F. Act on it in this record:** confirm the classification, keep the named versions, and fix the one-directional blocked-list wording (above).
- **G. Leave it.** V2 and V4 are worded "where the platform supports", and the section makes no API claim. That is the correct control.
- **H. Hand to the combined revision's applier, with CR-007 and CR-012.** CR-007 owns the `createSocket` exception line, and CR-012 owns the handshake-failure row. The V5 rewording above states the fail-closed outcome. Whether Error Handling should name an unchecked exception from `createSocket` belongs with those two records, not this one.
- **I. Leave it.** It is already carried by CR-007, CR-008, CR-009 and CR-013.

**Cascade implications.**
- *Ordering.* CR-013 is applied first and this record second, last of the six, as the record says. For the pointer paragraph, the combined target text in this record's Edit 1 is the governing text: it replaces CR-013's one-line paragraph and does not add a second one. Rule 7 follows rule 6, and both come after the rule 3 rewording that CR-007 and CR-008 bring (CR-013 Suggested Approach). The new section follows CR-013's section, before `## Example`.
- *CR-013 alternative (b) in the first pass: confirmed, with one condition.* Record 9 chose (b), and the code and tests it waited for exist since `12b584ab` (`PinnedCertificateSocketFactory.java:101-102`). But applying (b) in the first pass is more than swapping the subsection. Rule 6 binds only the `SYSTEM_VALIDATED` producer (CR-013 Edit 2), and CR-013's Edit 4 changelog line says the pinned handling "is not yet stated". Applied as written, both would contradict (b). U-065's contract verification (section 4) already drafted the dated note for CR-013: extend rule 6 to both producers, replace the pinned clause of the changelog line, name the pinned tests, and add the sentence on the layered shape. That note must be on CR-013 before the revision, so that the applier applies reviewed text and does not improvise.
- *CR-009's stale rows.* Verified stale: `:85` (cites `hardenSocket` at old lines and omits `protocolsToEnable`), `:87` and `:234` ("producer 2 unchanged"), `:206` ("no test asserts cipher or protocol hardening") and T5 at `:214` ("Missing"). There is also no checklist line for retargeting the pinned factory's two calls and import, and no parity test row. Who corrects them: the orchestrator, through the Artifact Librarian, as a dated note on CR-009. When: before the Part B design cites CR-009, not at the revision, because the design treats approved records as the target form (CR-009 `:243`). The wording is already drafted in U-065's contract verification (section 4).

**The cipher-pruning story: the record's points, checked.**
- Point 1, "block what it prunes": correct, and the most important. `reorder` removes blocked names and appends names that are on neither list (`:139-155`). A suite taken only off the preferred list stays enabled, ranked last, and C2 stays true while the pruning fails. One addition: suites the platform enables that are on neither list today, for example the non-forward-secret GCM suites the security review names (F-1), are not touched by pruning the preferred list. The story must decide them explicitly.
- Points 2, 4, 5 and 7: correct as written.
- Point 3 (a change of shape): correct. Add that an allow-list must still filter the platform's *default-enabled* set, not its supported set. Filtering the supported set would break C1, and C1 would then need rewording as well.
- Point 6: correct. Read it together with Scope Concern A's conditional C4.
- One risk to add (assumed, not measured): an allow-list that leaves out the TLS 1.3 suite names may, on some providers, leave TLS 1.3 enabled with no TLS 1.3 suite. V2 would still hold while the connection fails or falls back to TLS 1.2. The story should check this under both test providers.
- *Nothing in this record needs to wait for the story before approval.* The text states a rule. If the story changes the shape or closes Scope Concern A, the record is revised through the Artifact Librarian before it is applied, as point 3 already says.

**One story or several.** Applying this record is part of the combined contract revision. It is not a story of its own, and a stand-alone story is not recommended. No new story is implied beyond the cipher-pruning story already decided (Record 11, answer 2). The follow-on items fit work that already exists or needs no story:
- The C2 test that does not depend on a snapshot, and the conditional C4 fix, go into the pruning story.
- The protocol case of the E test goes into the migration's condition-12 loopback test (CR-009 T6).
- The AC-B8 sentence goes into the requirements pass or the Part B design.
- The dated notes on CR-009, CR-013 and BUG-020 are orchestrator edits, not stories.

**Constraints and risks for planning.**
- *Copying from upstream.* Upstream's factory has its own hardening step (feasibility analysis C-345). Whether its lists hold a TLS 1.2 floor was not read. A copy built from upstream instead of moved from the vendored class could bring back TLS 1.0 and 1.1. The control is U-065's tests, retargeted at the new class and not deleted.
- *Reflection coupling.* The tests reach `protocolsToEnable`, the three-argument `hardenSocket` and `ALLOWED_PROTOCOLS` by reflection (U-065 contract verification, section 1). The security review (F-4) recommends making these private or unmodifiable at the move. Doing that breaks those tests. They must be retargeted in the same change, not dropped as "no longer compiling".
- *Enabled is not negotiated.* No socket of the app has been measured on a device after the fix (security review F-5). Keep every statement about what is enabled. A later "negotiates TLS 1.2 or newer" needs a device measurement first.
- *Parallel requirement edits today.* REQ-MODERNIZATION-019 is being amended by another change. The AC-B8 sentence must be merged into that pass or follow it, not edited beside it.

**One point of disagreement with the Requirements Analyst's text.** The backward-compatible Versioning line (Edit 4) should not allow names to be removed from the blocked list. It is otherwise sound. I have no disagreement with the standing decisions.

---

## Plan

- **Execution Window:** Backlog. A text amendment applied inside the combined contract revision (v1 to v2), after CR-013, plus one added sentence in REQ-MODERNIZATION-019 AC-B8 applied with the Part B design (Edit 6). Not a stand-alone task.
- **Assigned To:** Solution Architect or Artifact Librarian (contract text)
- **Estimated Effort:** About 1 to 2 hours for the five edits; none for code or tests
- **Sprint:** None

### Fix Checklist

- [ ] Design and contracts: apply CR-007, CR-008, CR-012, CR-009 and CR-013 first, by quoted text
- [ ] Design and contracts: Edit 1 (combined pointer paragraph), Edit 2 (rule 7), Edit 3 (section, before `## Example`), Edit 4 (two Versioning lines and the changelog line), Edit 5 (CR-013's "Not stated" bullet)
- [ ] Requirements: amend REQ-MODERNIZATION-019 AC-B8 as in Edit 6, located by its text, with the Part B design, in the same pass as CR-005 Change 4 and the CR-010 and CR-011 edits
- [ ] Design and contracts: decide C4 (add it only if the pruning story closed the failed-initialisation cipher branch; the check is in Proposed Change)
- [ ] Design and contracts: confirm the four checks under Timing (pruning story, behaviour intact after the move, library does not change the socket, pinned calls retargeted)
- [ ] Verification: the contract names V1 to V5, C1 to C3 and B1 to B3, each with a named test; the `grep` under Timing step 4 returns the expected places

## Next Steps

CR filed and queued for sprint planning. Run `/amp:create-stories --from-cr CR-014` when ready to schedule.

Read this with the timing above. This is a text amendment applied inside the combined contract revision with CR-007, CR-008, CR-009, CR-012 and CR-013; it is not a story to schedule on its own. There is no code, test or behaviour to build, and a stand-alone repair story is not recommended. Four things follow: (1) the product owner approves this record so that the Part B move design can cite V1 to V5, C1 to C3 and B1 to B3 as the behaviour to preserve; (2) the cipher-pruning story is created with this record's rule as its acceptance (Interactions), and it does not need this record to be applied first; (3) the orchestrator adds the notes listed under Fix Order step 2 to CR-009, CR-013 and BUG-020; (4) the error-table row waits for CR-012's Scope Concern B.

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

- **Root Cause Category:** A boundary contract written to pin trust behaviour omitted a related security property (the protocol floor and suite handling) that lives only in code and tests; the property was at risk when the code changed owner.
- **Prevention Recommendation:** When a decision fixes a security property of a boundary object (a protocol floor, a suite rule, parity between two producers), raise the change record on the day the fix lands, not a sprint later, so no closed bug is left with a criterion that has no carrier. State the property as a rule, name the test that proves it, and list what is not stated. Where a list will change soon, state the rule the list serves and leave the list in code with a snapshot test, so the contract does not go stale.
- **Closed By:** (pending)
- **Date Closed:** (pending)
