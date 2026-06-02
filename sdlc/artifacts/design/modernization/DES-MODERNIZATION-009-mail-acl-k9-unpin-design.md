---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-009
related_stories: []
related_design_docs: []
integration_contracts:
  - CNTR-MODERNIZATION-007
  - CNTR-MODERNIZATION-001
change_records: []
type: ''
id: DES-MODERNIZATION-009
title: ''
domain: modernization
---

# DES-MODERNIZATION-009: Mail Anti-Corruption Layer and k-9 Dependency Unpin

## Overview

This design specifies the `MailTransport` Anti-Corruption Layer (ACL) at the
`mail`/k-9 boundary and the unpinning of the k-9 mail library from its JitPack git-SHA
(`com.github.jberkel.k-9:k9mail-library:eaf689025e`, `app/build.gradle:58`) to a
reproducible coordinate (preferred) or a vendored local module (fallback). It realizes
migration unit **MU-008** and target-state **ADR-004**, tracing **REQ-MODERNIZATION-009**
(AC-1..5). It is the last seam in the substrate-swap program: it contains the most
operationally risky dependency behind an app-owned port so the domain no longer depends
on a vendor library's internal types or error text.

The design is deliberately scoped against three traps surfaced by the verified source
read and the approved approach analysis (`approach.md` Component B): (1) it is **not** an
IMAP/MIME rewrite — k-9's protocol logic is retained verbatim behind the adapter;
(2) it draws the port boundary at the **transport surface actually consumed by the
engine** (`BackupTask`/`RestoreTask`/`ServiceBase`), not at every k-9 import in the
codebase, because the Preserved-Core converters are built on k-9 MIME *value* types and
moving them is out of scope (and out of MU-008's mandate); (3) it touches the Preserved
Core in exactly one place — the k-9 magic-string match in `State.java` — which is the
single permitted Preserved-Core edit in the entire engagement (`migration-units.md`
MU-000 invariants).

> **Sequencing — hard constraint.** This design is **applied after DES-MODERNIZATION-002**
> (transport-security hardening, REQ-MODERNIZATION-002). Both touch `BackupImapStore.java`;
> the security fix removes the trust-all path first, then this ACL wraps the
> already-validated transport. See `## Integration Design`.

## Context

### The leak this design closes (verified this session)

A direct grep this session found `com.fsck.k9.*` imported across **19 production files
(62 import occurrences)** — not the single-file coupling the requirement summary implies.
**Four kinds** of coupling exist, and the design treats them differently. The bucket model
below is **complete against the verified 19-file / 62-occurrence reality**: every k-9-importing
production file falls into exactly one bucket (transport, domain error-text, MIME value-type
converter residual, or the named service/bootstrap residual), so the AC-2 fitness function
(scoped per ADR-009-B) does not fail on its own enumeration.

1. **Transport coupling (in scope for the port).** `mail/BackupImapStore.java:55`
   `extends ImapStore`; the engine reaches the store and its folders directly —
   `ServiceBase.getBackupImapStore()` returns a `BackupImapStore`
   (`ServiceBase.java:99` method / `:104` construction, declared `throws MessagingException`);
   `BackupTask`
   (`BackupTask.java:67,70,135`) and `RestoreTask` (`RestoreTask.java:73,85`) call
   `getFolder(...)`, iterate `BackupFolder`, append/fetch `Message`s, and `catch`
   `MessagingException` / `AuthenticationFailedException` /
   `XOAuth2AuthenticationFailedException`. This is the surface the `MailTransport` port
   isolates.

2. **Domain error-text coupling (the one Preserved-Core edit).** `service/state/State.java`
   string-matches a k-9 internal exception message:

   ```java
   // State.java:32-34 (verified this session; the equals() is on line 33)
   if (exception instanceof MessagingException &&
           "Unable to get IMAP prefix".equals(exception.getMessage())) {
       return resources.getString(R.string.status_gmail_temp_error);
   ```

   This lets a vendor library's error wording shape a domain branch — the leaky
   abstraction REQ-MODERNIZATION-009 AC-3 requires removed.

3. **MIME value-type coupling (explicitly OUT of scope — documented to bound the design).**
   The Preserved-Core converters (`MessageConverter`, `MessageGenerator`, `MmsSupport`,
   `Attachment`, `HeaderGenerator`, `Headers`, `ConversionResult`, `PersonRecord`) traffic
   in `com.fsck.k9.mail.Message`/`Address`/`Body`/`internet.Mime*`. These are
   reference-quality, characterization-tested, and **preserved verbatim** (MU-000).
   Re-typing them onto app-owned MIME models is a separate, large, behavior-risky effort
   with no driver in this requirement. This design **scopes the AC-2 grep assertion to the
   transport surface** and records the converter coupling as a known, bounded residual
   (see `## Design Validation`). Conflating the two would turn a contained ACL into the
   IMAP/MIME rewrite `approach.md` rejects at 12/100.

4. **Service-layer and bootstrap residual (named and explicitly carved OUT of the AC-2
   transport-grep — not silently ignored).** Four files import `com.fsck.k9.*` while falling
   **outside** the design's enumerated port surface (`BackupTask`/`RestoreTask`/`ServiceBase`)
   **and** outside the converter bucket. They are itemized here so the AC-2 grep scope
   (ADR-009-B) is honest and the fitness function is well-defined:

   | File / line | k-9 import | Nature | Disposition |
   |-------------|------------|--------|-------------|
   | `service/SmsBackupService.java:28` | `com.fsck.k9.mail.MessagingException` | transport exception type used in a `service.*` engine entry point not in the enumerated port surface | **brought into the ACL port migration** — replace with app-owned `MailException`; this is `service.*` and MUST reach 0 under the AC-2 engine grep |
   | `service/SmsRestoreService.java:9` | `com.fsck.k9.mail.MessagingException` | transport exception type in a `service.*` engine entry point | **brought into the ACL port migration** — replace with app-owned `MailException`; MUST reach 0 under the AC-2 engine grep |
   | `service/SmsRestoreService.java:10` | `com.fsck.k9.mail.internet.BinaryTempFileBody` | **MIME body value type**, not a transport exception — it leaks the engine↔MIME converter residual *into* `service.*` | **port-boundary fix required**: the `BinaryTempFileBody` usage is moved behind the adapter (the restore body-staging belongs on the adapter side of the `MailMessageHandle` resolution, not in `service.*`); MUST reach 0 under the AC-2 engine grep |
   | `App.java:38` | `com.fsck.k9.mail.K9MailLib` | one-time library bootstrap/config call at application start (`mail.*` library init), **not** an engine↔transport interaction | **carved OUT of the AC-2 transport grep** — `App` is the application bootstrap, not `service.*`; the `K9MailLib` init call is the legitimate place k-9 is configured and is unaffected by the port (it stays until/unless the vendor module changes the init API). Documented residual; outside the engine grep scope. |
   | `preferences/AuthPreferences.java:11` | `com.fsck.k9.mail.AuthType` | a k-9 **enum used as a settings value** in the preferences layer, not a transport call | **carved OUT of the AC-2 transport grep** — `AuthType` is a configuration enum, not a transport type crossing the port. Re-typing it onto an app-owned auth enum is a bounded future seam (notable for the same MU-012 that owns the converter residual); not driven by REQ-009. Documented residual; outside the engine grep scope. |

   **Net effect on AC-2:** the three `service.*` occurrences (`SmsBackupService.java:28`,
   `SmsRestoreService.java:9`, `SmsRestoreService.java:10`) are **in scope** and MUST reach 0
   — the `BinaryTempFileBody` one is the most important catch, because it is a MIME residual
   that had leaked *out of* `mail.*` and into the engine; the ACL pulls it back behind the
   adapter. `App.java` (bootstrap) and `AuthPreferences.java` (preferences enum) are
   **explicitly carved out** with rationale, so the engine grep target is precisely
   `service.*` + `State.java` and nothing in those two packages causes a false failure.

### The reproducibility risk

The dependency is a raw JitPack commit hash (`eaf689025e`). If JitPack evicts the build
artifact the project cannot build, and no upstream k-9/Thunderbird security fixes are
reachable. AC-4 requires this pin removed in favor of a reproducible coordinate or a
vendored module.

## Design

> **Note on the magic-string line number.** REQ-MODERNIZATION-009 and `migration-units.md`
> cite `State.java:32`. Direct read this session places the `instanceof MessagingException`
> test on line 32 and the `"Unable to get IMAP prefix".equals(...)` match on line **33**
> (the whole `if` block spans 32–34). `approach.md` already corrected this to 33. The
> edit target is the two-line condition; the off-by-one does not change scope. This design
> uses "State.java:32–34" for the block.

### MailTransport port — app-owned types only

Introduce `com.zegoggles.smssync.mail.transport.MailTransport`, an app-owned interface
expressing exactly the IMAP operations the engine performs. No `com.fsck.k9.*` type
appears in the port's signatures. The port surface is derived from the verified call
sites in `BackupTask`/`RestoreTask`/`ServiceBase`, not invented:

| Port operation | Replaces (verified call site) | App-owned types in signature |
|----------------|-------------------------------|------------------------------|
| `connect()` / construction | `new BackupImapStore(ctx, uri, trustAll)` via `ServiceBase.getBackupImapStore()` (`ServiceBase.java:99` method / `:104` construction) | `MailTransportConfig` (uri + TLS policy handle from DES-002), throws `MailException` |
| `openFolder(DataType, DataTypePreferences)` | `getFolder(dataType, prefs)` (`BackupTask.java:70,135`; `RestoreTask.java:85`) | returns `BackupFolderHandle` (app-owned), throws `MailException` |
| `appendMessages(BackupFolderHandle, List<ConversionResult>)` | `folder.appendMessages(...)` (backup write path) | app-owned `ConversionResult` (already Preserved-Core), throws `MailException` |
| `getMessages(BackupFolderHandle, max, flagged, since)` | `BackupFolder.getMessages(...)` (`BackupImapStore.java:148`) | returns an app-owned message-handle list (restore read path), throws `MailException` |
| `fetch(BackupFolderHandle, handles, profile)` | `fetch(msgs, fp, null)` (`RestoreTask`, `BackupImapStore.java:169`) | app-owned `FetchSpec` enum, throws `MailException` |
| `closeFolders()` / `close()` | `store.closeFolders()` (`BackupTask.java:99`) | none |

**Boundary decision — message handles, not message bodies, cross the port.** The restore
read path fetches messages whose *bodies* are then handed to the Preserved-Core converters
(which legitimately operate on `com.fsck.k9.mail.Message`). To avoid an IMAP/MIME rewrite,
the port returns an **opaque app-owned `MailMessageHandle`** that the **adapter** can
resolve back to a k-9 `Message` *for the converter call only*, inside the `mail` package.
The engine (`service.*`) never names a k-9 type. This keeps the strict "no `com.fsck.k9.*`
in `service.*`" guarantee (AC-2 for the engine) while honestly acknowledging the converter
residual inside `mail.*` (documented in Validation). The converter call is itself a
candidate for a future `mail.transport` adapter-internal seam, noted as out of scope.

### k-9 adapter — the only place k-9 transport types live

`com.zegoggles.smssync.mail.transport.K9MailTransport implements MailTransport`. This is
`BackupImapStore` reshaped: it owns the `ImapStore` subclass, the `BackupFolder`
(`ImapFolder` subclass), `FetchProfile`, `ImapSearcher`, and the socket-factory selection.
It is the sole class permitted to import `com.fsck.k9.mail.store.imap.*` and to handle k-9
`MessagingException`. Everything the engine used to reach on `BackupImapStore` is reached
through the port; the adapter does the k-9 work behind it.

### Exception translation — the Anti-Corruption mapping (AC-2, AC-3)

The adapter catches every k-9 throwable at the boundary and translates it to an app-owned
type **before it reaches `service.*` or the `State` machine**. The mapping is enumerated
from the exceptions the verified call sites currently catch and the one `State` string-matches:

| k-9 throwable (current) | Caught/observed at | App-owned target | Domain consumer |
|-------------------------|--------------------|------------------|-----------------|
| `MessagingException` + message `"Unable to get IMAP prefix"` (`State.java:33`) | `State.getErrorMessage` | new `LocalizableException` subtype `TemporaryImapException` (maps to `R.string.status_gmail_temp_error`) | `State` selects on **type**, not text |
| `AuthenticationFailedException` (`BackupTask.java:8`, `RestoreTask.java:13`, `ServiceBase.java`) | engine catch | `RequiresLoginException` (existing Preserved-Core type) — already in `State.isAuthException()` | `State.isAuthException()` |
| `XOAuth2AuthenticationFailedException` (`State.java:8`, `BackupTask.java:11`) | engine catch + `State.isAuthException()` | app-owned `XOAuth2FailedException` (LocalizableException) | `State.isAuthException()` |
| generic `MessagingException` (connect/folder/append/fetch failures) | engine catches → `ERROR` | `MailException` (new LocalizableException base) | `State.getErrorMessage` falls through to `LocalizableException` branch |

After translation, **`State.java:32–34` deletes its `instanceof MessagingException` +
string-equals branch** and relies on the existing `LocalizableException` branch
(`State.java:35–36`) — which already resolves a localized resource id. The
`com.fsck.k9.mail.*` imports in `State.java` (lines 6,7,8) and the `service.*` engine
files are removed; `State.isAuthException()` switches from k-9
`XOAuth2AuthenticationFailedException`/`AuthenticationFailedException` to the app-owned
subtypes. `StateTest.shouldGetErrorMessagePrefix()` (`StateTest.java:62-67`) is updated to
throw the new `TemporaryImapException` instead of `new MessagingException("Unable to get
IMAP prefix")`, asserting the same localized output — preserving behavior (AC-5).

### k-9 unpin — ADR: Replace-coordinate-preferred, Vendor-fallback (AC-4)

**ADR-009-A: Unpin k-9 to a reproducible coordinate; vendor behind the ACL as fallback.**

- **Status:** Accepted (realizes target-state ADR-004 for the k-9 dependency line).
- **Context:** `app/build.gradle:58` pins `com.github.jberkel.k-9:k9mail-library:eaf689025e`
  — a raw JitPack git SHA, not a semantic version; unreproducible if evicted; no upstream
  security-patch stream (verified this session).
- **Decision — preference order:**
  1. **Replace-coordinate (preferred).** Resolve k-9 (or the maintained
     `app.k9mail`/Thunderbird `k9mail-library` lineage) to a **published, semantically
     versioned, reproducible coordinate** from `mavenCentral()` or a pinned JitPack
     *tag* (not a bare SHA), recorded in `gradle/verification-metadata.xml`
     (the supply-chain control MU-002/MU-001 introduce). This is the lowest-risk path:
     no protocol code changes, restores the security-patch stream.
  2. **Vendor (fallback).** If no API-compatible reproducible coordinate exists
     (`approach.md` flags this as EVALUATE-not-assume; the requirement Constraints say
     "vendoring may be required"), vendor the k-9 IMAP/MIME subset as an **in-repo Gradle
     module** (`:k9mail-vendored`) under our build, license-compatible (k-9 is Apache 2.0).
     This is *Asset Capture*, **not a rewrite** — existing working code adopted under our
     build for reproducibility.
- **Explicit non-goal:** **Do NOT rewrite the IMAP or MIME implementation.** A from-scratch
  IMAP/MIME client is the single highest-risk action available to this project
  (`approach.md` Component B scores it 12/100; safety-critical, field-only edge cases). The
  ACL exists precisely so the coordinate-vs-vendor choice is swappable without touching
  protocol logic or the engine.
- **Escalation gate:** If, at implementation, **neither** a reproducible coordinate **nor**
  a viable vendored module can be sourced, the implementer must **STOP and escalate** — a
  from-scratch IMAP rewrite is out of scope and breaks the program risk profile
  (`approach.md` Conditions-for-Success #2). It must not be silently substituted.
- **Consequences:** (+) reproducible builds; security-patch stream restored; the riskiest
  dependency becomes swappable. (−) vendoring (if taken) adds an in-repo maintenance
  surface — accepted as the price of supply-chain control.

### Decision: scope the AC-2 grep to the engine + the port boundary

**ADR-009-B: The "no `com.fsck.k9.*` outside the adapter" guarantee is enforced for
`service.*` (the domain/engine) and the `mail.transport` port; the Preserved-Core MIME
converters in `mail.*` are a documented, bounded residual.**

- **Context:** k-9 MIME value types are structural to the Preserved-Core converters
  (verified: 8 converter files import `com.fsck.k9.mail.*`). Re-typing them is a separate
  effort with no driver here and would violate MU-000 ("no behavior change").
- **Decision:** AC-2 is satisfied for the **domain and engine** (zero `com.fsck.k9.*` in
  `service.*` and in `State`); the converter residual stays inside `mail.*` behind the
  adapter, where k-9 already belongs, and is recorded as a future seam (potential MU-012).
- **Consequence:** The leaky abstraction REQ-MODERNIZATION-009 targets (vendor types/error
  text shaping the *domain*) is fully closed; the contained converter coupling is honestly
  disclosed, not hidden. This is the correct reading of "no `com.fsck.k9.*` crosses **the
  port**" — the port is the engine↔mail boundary, not every package boundary.

## Architecture

```
  service.*  (ENGINE — zero com.fsck.k9.* after this design)
  ┌───────────────────────────────────────────────┐
  │ BackupTask / RestoreTask / ServiceBase          │
  │   getFolder / append / getMessages / fetch      │
  │   catch MailException / RequiresLoginException   │   State machine
  └───────────────┬─────────────────────────────────┘   ┌───────────────┐
                  │ app-owned MailTransport port          │ State.java     │
                  │ (MailTransportConfig, BackupFolder-    │  (no k-9 text  │
                  │  Handle, MailMessageHandle, FetchSpec, │   match; type- │
                  │  MailException)                        │   based branch)│
  ════════════════▼═══════════════ PORT (app types only) ═└───────────────┘
  mail.transport  (ADAPTER — sole owner of k-9 transport types)
  ┌───────────────────────────────────────────────┐
  │ K9MailTransport implements MailTransport         │
  │   was BackupImapStore extends ImapStore          │
  │   • socket-factory injection ◀── from DES-002     │
  │     (validated TLS / pinned cert; trust-all gone) │
  │   • BackupFolder extends ImapFolder               │
  │   • translate MessagingException &c → MailException│
  │     / RequiresLoginException / XOAuth2Failed /     │
  │       TemporaryImapException  (ACL mapping)        │
  │   • resolve MailMessageHandle → k-9 Message for    │
  │     Preserved-Core converters (inside mail.* only) │
  └───────────────┬─────────────────────────────────┘
                  │ k-9 ImapStore API (UNCHANGED — not rewritten)
                  ▼
            IMAP server (Gmail 993 / custom)

  build: app/build.gradle:58  k-9 SHA eaf689025e ─▶ reproducible coordinate
                                                    (or :k9mail-vendored module)
  Preserved-Core converters (mail.*) keep k-9 MIME value types — bounded residual.
```

## Components

| Component | Responsibility | k-9 visibility |
|-----------|----------------|----------------|
| `MailTransport` (port, new) | App-owned IMAP operation contract; the engine's only door to mail | none (app types only) |
| `K9MailTransport` (adapter; `BackupImapStore` reshaped) | k-9 `ImapStore` work; folder open/append/fetch/search; socket-factory selection; exception translation | sole owner of `com.fsck.k9.mail.store.imap.*`, `MessagingException` |
| `MailException` + `TemporaryImapException` + `XOAuth2FailedException` (new `LocalizableException` subtypes) | App-owned error vocabulary the ACL maps k-9 throwables onto | none (extend Preserved-Core `LocalizableException`) |
| `BackupFolderHandle`, `MailMessageHandle`, `MailTransportConfig`, `FetchSpec` (new app-owned value types) | Cross the port in place of k-9 `BackupFolder`/`Message`/`FetchProfile` | none |
| `State.java` (Preserved Core — ONE permitted edit) | Remove k-9 string-match + k-9 imports; branch on app-owned types | k-9 imports removed |
| `ServiceBase`/`BackupTask`/`RestoreTask`/`SmsBackupService`/`SmsRestoreService` (engine, refactor) | Depend on `MailTransport`, not `BackupImapStore`; catch app-owned exceptions; `SmsRestoreService` `BinaryTempFileBody` body-staging moves behind the adapter | k-9 imports removed (all of `service.*` → 0) |
| Preserved-Core converters (`mail.*`, unchanged) | MIME conversion on k-9 value types — verbatim | bounded residual inside `mail.*` |

## Interfaces

The `MailTransport` port is the cross-component boundary this design introduces. Its
operations, app-owned types, and the exception-translation mapping are specified above and
formalized in `## Integration Contracts`. The socket-factory injection point is **owned by
DES-MODERNIZATION-002** and consumed here: `K9MailTransport` receives the validated
`TrustedSocketFactory` (or user-pinned-cert factory) selected by DES-002, replacing the
`trustAllCertificates ? AllTrustedSocketFactory.INSTANCE : new DefaultTrustedSocketFactory(...)`
selection at `BackupImapStore.java:60-62` (which DES-002 removes the trust-all arm of first).

## Integration Design

### Sequencing — applied AFTER DES-MODERNIZATION-002 (hard ordering on a shared file)

`mail/BackupImapStore.java` is touched by **both** DES-002 (transport security, MU-003)
and this design (ACL, MU-008). `migration-units.md` §Shared-File Allocation mandates
**MU-003 first, then MU-008**, and REQ-MODERNIZATION-009 Constraints restate it
("sequence the security fix first, then introduce the ACL"). The rationale is correctness,
not preference: DES-002 deletes `AllTrustedSocketFactory` and rewrites the socket-factory
selection at `BackupImapStore.java:60-62` to validated-TLS-only; this design then wraps the
ACL around an **already-validated** transport. If the order were reversed, the ACL would be
built around a transport still capable of trust-all, and DES-002 would then have to reach
back inside the adapter — re-coupling what the ACL just isolated.

```
DES-002 (REQ-002, MU-003)                 DES-009 (REQ-009, MU-008)  ← THIS DESIGN
─────────────────────────                 ─────────────────────────
1. delete AllTrustedSocketFactory          4. introduce MailTransport port
2. BackupImapStore socket-factory          5. reshape BackupImapStore → K9MailTransport
   selection → validated TLS only             (adapter behind the port)
3. migrate() never enables trust-all       6. ACL exception translation (k-9 → app types)
                                            7. remove State.java:32-34 k-9 string-match
   ── ships independently ──▶               8. unpin k-9 (coordinate / vendor)
                                               ── ships independently ──▶
```

### Socket-factory injection — the DES-002 → DES-009 handoff

DES-002 produces the validated `TrustedSocketFactory` (and the opt-in user-pinned-cert
factory for self-hosted IMAP). The current code selects it inline in the
`BackupImapStore` constructor (`BackupImapStore.java:60-62`). Under this design, that
selection moves **into `K9MailTransport`** (the adapter — the correct home, since the
k-9 `TrustedSocketFactory` type must not cross the port). The port's `MailTransportConfig`
carries an **app-owned TLS policy** (e.g. `TlsPolicy.VALIDATED` / `TlsPolicy.PINNED(cert)`),
which the adapter maps to the DES-002 factory. The engine therefore expresses *intent*
(validated vs pinned) in app types; the adapter does the k-9-typed wiring. This preserves
both DES-002's security guarantee and DES-009's no-k-9-type-crosses-the-port guarantee.

### Hilt provides the MailTransport binding (DES-MODERNIZATION-008)

`MailTransport` is provided via **Hilt** (DES-008 / MU-007). A Hilt module binds the
`MailTransport` interface to `K9MailTransport` (or, in tests, to a fake). This:

- removes the `new BackupImapStore(...)` construction currently buried in
  `ServiceBase.getBackupImapStore()` (`ServiceBase.java:99` method / `:104` construction) — the engine receives an
  injected `MailTransport` instead of constructing a store;
- makes the coordinate-vs-vendor choice and the adapter swap a **binding flip**
  (branch-by-abstraction reversibility — the program's core mechanic);
- lets `MailTransportConfig` (uri + TLS policy) be assembled from injected
  `AuthPreferences`/`Preferences`, not read ad hoc.

**Ordering caveat:** MU-008 depends on MU-001/MU-002/MU-003 (`migration-units.md`
Dependency Matrix), and Hilt (MU-007) is sequenced *after* MU-008 in the phase plan. So
the **first** MailTransport binding may be hand-wired (a factory/provider method on the
existing construction seam, no behavior change), then folded into the Hilt graph when
MU-007 lands. The port and adapter do not depend on Hilt existing; Hilt only formalizes
the binding. This is called out so the implementer does not block MU-008 on MU-007.

### k-9 escalation gate (integration risk)

If the AC-4 evaluation finds **no reproducible published coordinate AND no viable vendored
module**, the integration **halts and escalates** (per ADR-009-A). The adapter cannot be
completed against an unresolvable dependency, and the explicit non-goal forbids substituting
a from-scratch IMAP client. This gate is the integration-time expression of
`approach.md` Conditions-for-Success #2.

### Test integration — behavior preserved through the adapter (AC-5)

- `BackupImapStoreTest` (`BackupImapStoreTest.java`) — the `isValidUri`/`isValidImapFolder`
  static helpers and the `getStoreUriForLogging` masking move with the adapter and assert
  unchanged. The trust-factory assertions
  (`testShouldCreateCorrectTrustFactoryFor*`, lines 46–62) are **owned by DES-002's change**
  (they already lose `AllTrustedSocketFactory` when MU-003 lands); this design re-homes them
  onto `K9MailTransport`'s `MailTransportConfig`→factory mapping, asserting the same
  validated/pinned factory selection — never the deleted trust-all factory.
- `StateTest.shouldGetErrorMessagePrefix` (`StateTest.java:62-67`) — updated to construct
  the new `TemporaryImapException` instead of `new MessagingException("Unable to get IMAP
  prefix")`; asserts the same `"Temporary IMAP error, try again later."` output. This is the
  characterization test that **gates the State.java:32-34 edit** (MU-000 invariant).
- Converter tests (`MessageConverterTest`, `MessageGeneratorTest`) — **unchanged**; the
  converters and their k-9 MIME types are not touched (bounded residual, ADR-009-B).

## Integration Contracts

> The single cross-component boundary this design introduces is the `MailTransport` port
> (engine `service.*` ⇄ mail-transport adapter `mail.transport.*`). It is a **service**
> contract: an app-owned interface with a defined exception-translation mapping. No
> approved CNTR-* artifact exists yet; one MUST be created before any story referencing
> this design is sprint-planned.

| Boundary | Producer | Consumer(s) | Contract Type | CNTR Artifact | Status |
|----------|----------|-------------|---------------|---------------|--------|
| `MailTransport` port: connect, openFolder, append, getMessages, fetch, close + exception-translation mapping | `K9MailTransport` (adapter, `mail.transport`) | `BackupTask`, `RestoreTask`, `ServiceBase` (engine); `State` (error branch) | service | CNTR-MODERNIZATION-009-mailtransport | needed |
| Socket-factory injection (validated/pinned TLS) into the adapter | DES-MODERNIZATION-002 transport-security | `K9MailTransport` | service | (covered by DES-002's contract; consumed here) | needed (cross-ref DES-002) |

### Contracts Needed (pre-sprint gate)

- [ ] **CNTR needed:** `MailTransport` service port — enumerate every operation signature
      in app-owned types, and the **complete** k-9-throwable → app-owned-exception mapping
      (the table in `## Design` is the seed; the contract must be exhaustive: every
      `MessagingException` subtype `BackupImapStore`/`BackupFolder`/`State` can observe must
      map to a defined `LocalizableException` subtype, per MU-008 contract_gate). Run
      `/amp:create-contracts` before sprint-planning any MU-008 story.
- [ ] **CNTR cross-ref:** the TLS-policy → socket-factory handoff from DES-002 — confirm the
      app-owned `TlsPolicy` shape with DES-002 so the two designs agree on the type that
      crosses into the adapter.

## Design Validation

The design satisfies REQ-MODERNIZATION-009 AC-1..5 as follows. Each row is checkable by the
implementer and the reviewer.

| AC | Requirement | How this design satisfies it | Verification method |
|----|-------------|------------------------------|---------------------|
| AC-1 | `MailTransport` port (app-owned types) + k-9 adapter; all IMAP ops go through the port | `MailTransport` interface + `K9MailTransport` adapter; engine call sites (`BackupTask`/`RestoreTask`/`ServiceBase`) rewired to the port | Code review: engine references `MailTransport`, never `BackupImapStore`/`ImapStore` |
| AC-2 | No `com.fsck.k9.*` import outside the adapter (grep) | k-9 transport imports confined to `mail.transport.K9MailTransport`; removed from `service.*` (including `SmsBackupService.java:28`, `SmsRestoreService.java:9`, and the `BinaryTempFileBody` leak at `SmsRestoreService.java:10` — pulled back behind the adapter) and from `State.java`. **Scoped per ADR-009-B**: enforced for engine + domain; converter residual inside `mail.*`, plus `App.java:38` (`K9MailLib` bootstrap) and `AuthPreferences.java:11` (`AuthType` settings enum) are documented, explicitly carved-out residuals (Context bucket 4) | `grep -r "com.fsck.k9" app/src/main/java/com/zegoggles/smssync/service/` returns **0** (covers `SmsBackupService`/`SmsRestoreService` too); `State.java` returns **0**; `mail.*` returns only the adapter + documented converters; `App.java` and `AuthPreferences.java` are out-of-scope by ADR-009-B and not asserted by the engine grep |
| AC-3 | `State.java:32` magic-string match removed; domain no longer depends on k-9 error text | Remove the `instanceof MessagingException && "Unable to get IMAP prefix".equals(...)` block (lines 32–34); rely on the type-based `LocalizableException` branch fed by the ACL's `TemporaryImapException` | `grep "Unable to get IMAP prefix" app/src/main` returns **0**; `StateTest` passes |
| AC-4 | k-9 on a reproducible/versioned coordinate (or vendored); JitPack SHA pin removed | ADR-009-A: replace `app/build.gradle:58` `...:eaf689025e` with a versioned coordinate (preferred) or `:k9mail-vendored` module (fallback); recorded in `verification-metadata.xml` | `grep "eaf689025e" app/build.gradle` returns **0**; clean build resolves reproducibly |
| AC-5 | All mail conversion/IMAP tests pass through the adapter unchanged in behavior | Converters untouched; `BackupImapStoreTest`/`StateTest` re-homed asserting identical outputs; no behavior change (MU-000) | `./gradlew test` green; `StateTest.shouldGetErrorMessagePrefix` and converter tests pass |

### Validation checks (the design's own fitness functions)

1. **No `com.fsck.k9.*` in the engine or domain.** Grep `service.*` (all of it — including
   `SmsBackupService`, `SmsRestoreService`, and the `BinaryTempFileBody` MIME leak the ACL
   pulls back into the adapter) and `State.java` → 0 matches. (The authoritative AC-2 check,
   scoped per ADR-009-B.) `App.java:38` (`K9MailLib` bootstrap) and `AuthPreferences.java:11`
   (`AuthType` enum) are explicitly carved out (Context bucket 4) and are **not** part of this
   grep target.
2. **State.java magic-string gone.** Grep `"Unable to get IMAP prefix"` across `app/src/main`
   → 0 matches; `State.isAuthException()` references app-owned, not k-9, exception types.
3. **k-9 reproducible.** `app/build.gradle` contains no bare-SHA k-9 coordinate; the
   resolved coordinate is in `verification-metadata.xml`.
4. **Tests pass through the adapter unchanged.** Full `./gradlew test`; specifically the
   characterization gate `StateTest` and the converter suites.
5. **Exception-mapping completeness.** Every k-9 throwable the boundary can emit has a
   defined app-owned target (the CNTR must be exhaustive — this is the contract gate).

### Known, bounded residual (disclosed, not hidden)

The Preserved-Core MIME converters (`MessageConverter`, `MessageGenerator`, `MmsSupport`,
`Attachment`, `HeaderGenerator`, `Headers`, `ConversionResult`, `PersonRecord`) retain
`com.fsck.k9.mail.*` value-type imports. This is **out of scope** for MU-008/REQ-009 by
design (MU-000 preserves them verbatim; re-typing is behavior-risky with no driver here).
The leak REQ-009 targets — vendor types/error text shaping the **domain** — is fully closed.
A future MU-012 could introduce app-owned MIME models behind the adapter; this design names
that seam without taking it.

Two further **named** residuals outside both the engine and the converters are carved out of
AC-2 (Context bucket 4): `App.java:38` (`com.fsck.k9.mail.K9MailLib` — one-time library
bootstrap at application start, the legitimate k-9 init point, not a transport interaction)
and `preferences/AuthPreferences.java:11` (`com.fsck.k9.mail.AuthType` — a k-9 enum used as a
settings value, not a type crossing the port). Neither is in `service.*`; both are disclosed,
not hidden, and `AuthType` is a candidate for the same MU-012 app-owned-enum seam. By contrast,
the three `service.*` occurrences (`SmsBackupService.java:28`, `SmsRestoreService.java:9`, and
the `BinaryTempFileBody` MIME leak at `SmsRestoreService.java:10`) are **in scope** and reach 0
under the AC-2 engine grep — the `BinaryTempFileBody` case being the notable catch, since it is
a MIME value type that had leaked out of `mail.*` into the engine and is pulled back behind the
adapter. Together these account for the full verified breadth (**19 files / 62 occurrences**).

## Trade-offs

| Decision | Chosen | Alternative rejected | Why |
|----------|--------|----------------------|-----|
| Port boundary granularity | Transport surface (engine ⇄ mail) + the 3 in-scope `service.*` residuals (`SmsBackupService`, `SmsRestoreService` incl. `BinaryTempFileBody`) | Re-type all 19 k-9-importing files (incl. converters, `App.java` `K9MailLib`, `AuthPreferences` `AuthType`) | Re-typing the MIME converters / bootstrap / settings enum is an IMAP/MIME-adjacent rewrite with no driver; violates MU-000 (`approach.md` B). The 3 `service.*` residuals ARE in scope (Context bucket 4); `App.java`/`AuthPreferences` are carved out with rationale |
| Message crossing the port | Opaque `MailMessageHandle`, adapter resolves to k-9 `Message` for converters | Convert to app-owned MIME model at the port | App-owned MIME model = the rejected rewrite; the handle keeps the converter residual contained in `mail.*` |
| k-9 unpin path | Reproducible coordinate preferred, vendor fallback | Rewrite IMAP/MIME | Rewrite scores 12/100 (`approach.md`); safety-critical field-only edge cases |
| Exception handling | Type-based domain branch via ACL translation | Keep string-match but centralize it | String-match *is* the leak AC-3 forbids; centralizing it would not remove the dependency on k-9 error text |
| Binding mechanism | Hilt (when MU-007 lands) over an interim hand-wired provider | Block MU-008 on MU-007 | Port/adapter don't need Hilt to exist; Hilt only formalizes the binding — don't serialize unnecessarily |

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| No reproducible coordinate AND vendoring infeasible | Medium | High | Escalation gate (ADR-009-A); never substitute an IMAP rewrite; the ACL keeps the choice swappable |
| Exception mapping incomplete → an unmapped k-9 `MessagingException` reaches the engine | Medium | Medium | CNTR must be exhaustive (contract gate, MU-008 contract_gate); a catch-all `MailException` base type backstops unmapped cases without leaking the k-9 type |
| Sequencing inverted (ACL before DES-002) re-couples trust-all | Low | High | Hard ordering documented; Shared-File Allocation enforces MU-003 → MU-008 |
| Re-homed trust-factory tests collide with DES-002's edits to the same tests | Medium | Low | DES-002 owns the trust-factory test changes; this design only re-targets them onto `K9MailTransport` after MU-003 lands |
| AC-2 read as "zero k-9 anywhere" → scope explosion into the converters | Medium | High | ADR-009-B scopes AC-2 to engine+domain explicitly; residual disclosed in Validation |

## Notes

- **Source verified this session:** `app/build.gradle:58` (k-9 SHA `eaf689025e`),
  `State.java:32-34` (k-9 magic-string; `equals` on line 33), `BackupImapStore.java:55-62`
  (extends `ImapStore`; trust-factory selection), `BackupImapStoreTest`, `StateTest`,
  `BackupStoreConfig`, `AllTrustedSocketFactory`, and the `com.fsck.k9.*` grep
  (**19 files / 62 occurrences**). The 19-file breadth is the key correction over the
  requirement's single-file framing and drives ADR-009-B.
- **Traceability:** REQ-MODERNIZATION-009 (AC-1..5) · MU-008 · target-state ADR-004 ·
  `approach.md` Component B (Replace-coordinate/Vendor; never rewrite IMAP) · DEP-005 ·
  ARCH-001.
- **Cross-design dependencies:** DES-MODERNIZATION-002 (sequenced first; socket-factory
  source), DES-MODERNIZATION-008 (Hilt binding for `MailTransport`).

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| REQ-MODERNIZATION-009 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-009-mail-anti-corruption-layer-k9-unpin.md | The requirement under design (AC-1..5, constraints) |
| REQ-MODERNIZATION-002 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-002-eliminate-transport-security-mitm.md | Sequencing dependency (referenced) |
| target-state.md (ADR-004) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/target-state.md | MailTransport ACL ADR; port & adapter integration pattern |
| migration-units.md (MU-008, MU-000) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md | MU-008 scope, contract_gate, shared-file allocation, the one permitted State.java edit |
| approach.md (Component B) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/approach.md | Replace-coordinate/Vendor decision; explicit "never rewrite IMAP/MIME"; escalation condition |
| mail/BackupImapStore.java | app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java | Adapter source-of-record; extends ImapStore; trust-factory selection; folder/fetch/search |
| service/state/State.java | app/src/main/java/com/zegoggles/smssync/service/state/State.java | The k-9 magic-string match (lines 32-34); isAuthException k-9 types |
| service/BackupTask.java | app/src/main/java/com/zegoggles/smssync/service/BackupTask.java | Engine call sites: getFolder/append, k-9 exception catches |
| service/RestoreTask.java | app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java | Engine call sites: getFolder/getMessages/fetch |
| service/ServiceBase.java | app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java | getBackupImapStore() construction seam — method at :99, `new BackupImapStore(...)` at :104, throws MessagingException (line 57 is an unrelated appLog/wifiLock field) |
| service/SmsBackupService.java | app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java | In-scope `service.*` residual: `com.fsck.k9.mail.MessagingException` import (:28) |
| service/SmsRestoreService.java | app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java | In-scope `service.*` residual: `MessagingException` (:9) and `BinaryTempFileBody` MIME-leak (:10) |
| App.java | app/src/main/java/com/zegoggles/smssync/App.java | Carved-out residual: `com.fsck.k9.mail.K9MailLib` bootstrap import (:38) |
| preferences/AuthPreferences.java | app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java | Carved-out residual: `com.fsck.k9.mail.AuthType` settings enum import (:11) |
| mail/BackupStoreConfig.java | app/src/main/java/com/zegoggles/smssync/mail/BackupStoreConfig.java | k-9 StoreConfig surface the adapter retains |
| mail/AllTrustedSocketFactory.java | app/src/main/java/com/zegoggles/smssync/mail/AllTrustedSocketFactory.java | Trust-all factory (deleted by DES-002; selection moves into adapter) |
| app/build.gradle | app/build.gradle | k-9 SHA pin (:58) to unpin (AC-4) |
| BackupImapStoreTest.java | app/src/test/java/com/zegoggles/smssync/mail/BackupImapStoreTest.java | Tests to re-home through the adapter (AC-5) |
| StateTest.java | app/src/test/java/com/zegoggles/smssync/service/state/StateTest.java | Characterization gate for the State.java edit (shouldGetErrorMessagePrefix) |
| com.fsck.k9 grep | app/src/main/java | Boundary scope: 19 files / 62 occurrences (drives ADR-009-B) |
