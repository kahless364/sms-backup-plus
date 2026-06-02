---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-002
related_stories: []
related_design_docs: []
integration_contracts:
  - CNTR-MODERNIZATION-001
  - CNTR-MODERNIZATION-002
change_records: []
type: ''
id: DES-MODERNIZATION-002
title: ''
domain: modernization
---

# DES-MODERNIZATION-002: Transport-Security Hardening — Eliminate the Trust-All MITM Path

## Overview

This design specifies the elimination of every code path in SMS Backup+ that accepts an
unvalidated TLS server certificate during IMAP communication, and the replacement of the
silent security-downgrade migration with a non-silent, consent-based behavior. It realizes
migration unit **MU-003** (`migration-units.md`) and target-state **ADR-007**
(`target-state.md:525-530`), tracing **REQ-MODERNIZATION-002** (AC-1..AC-10), and closes
ilities findings **ARCH-007** (Critical, trust-all TLS, CWE-295) and **ARCH-008** (High,
silent downgrade migration). It is a **Phase 1** unit that runs in parallel with the SDK
gate (MU-001) from day one because the trust-all path is an *active* MITM exposure that the
SDK-uplift release would otherwise worsen.

Three concrete production changes anchor the design, each verified against the live source
this session:

1. **Delete `mail/AllTrustedSocketFactory.java`** (59 LOC; `INSTANCE` field at line 22;
   `InsecureX509TrustManager` with empty `checkServerTrusted()` at lines 42-57). Its only
   reachable reference is the ternary at `BackupImapStore.java:60-62`.
2. **Rewrite the socket-factory selection** in `BackupImapStore` so validated TLS is the
   unconditional default and the only relaxed path is an explicitly user-enrolled, per-host
   **pinned certificate** factory — never blanket trust.
3. **Rewrite `AuthPreferences.migrate()`** (lines 276-288) so it can never write
   `SERVER_TRUST_ALL_CERTIFICATES = true`, and surface a **one-time notice** to legacy
   `+ssl`/`+tls` users whose posture the old migration silently degraded.

The design is deliberately scoped so it lands *before* DES-MODERNIZATION-009 (the
`MailTransport` ACL): MU-003 validates the transport here; MU-008 later wraps the ACL around
the already-validated transport (`migration-units.md` Constraint 3, Shared-File Allocation
table). This document specifies the socket-factory/transport boundary as a **contract
candidate** that DES-009's `MailTransport` will consume.

## Context

### Why this is needed — the active vulnerability (verified this session)

`mail/AllTrustedSocketFactory.java` is a `TrustedSocketFactory` whose private
`InsecureX509TrustManager` (lines 42-57) has an **empty** `checkServerTrusted(X509Certificate[], String)`
body and a `getAcceptedIssuers()` that returns `null`. The class carries
`@SuppressLint("TrustAllX509TrustManager")` (line 20) — an explicit acknowledgment that the
Android linter flags it as dangerous. When this factory is active, the IMAP TLS handshake
accepts **any** certificate: forged, expired, self-signed, or from an unknown CA. This is
CWE-295 (Improper Certificate Validation) and violates OWASP MASVS-NETWORK-1.

The factory is selected in the `BackupImapStore` constructor (verified `BackupImapStore.java:58-63`):

```java
public BackupImapStore(final Context context, final String uri,
                       boolean trustAllCertificates) throws MessagingException {
    super(new BackupStoreConfig(uri),
        trustAllCertificates ? AllTrustedSocketFactory.INSTANCE : new DefaultTrustedSocketFactory(context),
        (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE));
}
```

The `trustAllCertificates` argument is supplied by exactly one production call site (scope
verified via grep this session): `ServiceBase.getBackupImapStore()`
(`ServiceBase.java:99-105`) passes `getAuthPreferences().isTrustAllCertificates()`. The flag
reader is `AuthPreferences.isTrustAllCertificates()` (`AuthPreferences.java:196-198`),
defaulting to `false`.

### Why this is needed — the silent security downgrade (ARCH-008, verified this session)

`AuthPreferences.migrate()` (`AuthPreferences.java:276-288`) runs on every launch via
`App.onCreate()` → `preferences.migrate()` (`App.java:72`) → `Preferences.migrate()`
(`Preferences.java:294-296`, which constructs `new AuthPreferences(context).migrate()`):

```java
void migrate() {
    if (useXOAuth()) {
        return;
    }
    if ("+ssl".equals(getServerProtocol()) ||
        "+tls".equals(getServerProtocol())) {
        preferences.edit()
            .putBoolean(SERVER_TRUST_ALL_CERTIFICATES, true)   // silent downgrade
            .putString(SERVER_PROTOCOL, getServerProtocol()+"+")
            .commit();
    }
}
```

A user who deliberately chose `+ssl` (validated TLS — the *secure* option) is, on the first
launch after an upgrade, silently transitioned to `+ssl+` **with certificate validation
disabled**. No prompt, no notice, no log entry the user can see. Combined with the trust-all
factory above, this is a consent-free elevation of risk on the user's entire SMS/MMS/call-log
corpus plus a Gmail full-mailbox credential.

### Exploit path (from REQ-MODERNIZATION-002 §Exploit Path)

Legacy `+ssl` user upgrades → `migrate()` writes `SERVER_TRUST_ALL_CERTIFICATES=true` → user
on hostile Wi-Fi → on-path attacker presents a self-signed cert claiming `imap.gmail.com` →
`isTrustAllCertificates()` returns `true` → `AllTrustedSocketFactory.INSTANCE` selected →
`InsecureX509TrustManager.checkServerTrusted()` returns immediately → handshake completes →
attacker reads the full backup stream and the IMAP `AUTH` credential. No malware, no server
compromise — only network adjacency.

### What must be preserved

- **The self-hosted IMAP use case** (REQ Constraint 1). Users running a private-CA or
  self-signed IMAP server must retain a viable path. The replacement is a consensual,
  user-initiated **pinned-certificate** opt-in (AC-4) — not blanket trust.
- **The Preserved Core** (MU-000): the `service/state/` machine, `DataType`, the exception
  hierarchy, and the conversion components must not change as a side effect (REQ Constraint 6).
- **The migrate() sequencing constraint** (AC-7, REQ Constraint 2): the `migrate()`
  characterization test backfilled by REQ-MODERNIZATION-003 / MU-002 must be green on main
  **before** any production change to `migrate()` is merged.

## Design

### Decision 1 — Delete AllTrustedSocketFactory; validated TLS is the unconditional default

`AllTrustedSocketFactory.java` is deleted outright (AC-1). The `BackupImapStore` constructor
no longer selects a factory by a boolean trust-all flag. The default and only-without-enrollment
factory becomes the platform-validated `DefaultTrustedSocketFactory` (already imported at
`BackupImapStore.java:29`), which delegates chain validation to the Android `TrustManager`
framework. No production `X509TrustManager` with an empty `checkServerTrusted()` body remains
(AC-2) — the deletion removes the only one in the tree.

This satisfies AC-3 (every IMAP TLS connection uses validated TLS by default) directly: with
the trust-all arm removed, the construction path has no branch that can yield an
unvalidated-cert factory unless the user has enrolled a pinned certificate (Decision 3).

### Decision 2 — Rewrite the constructor's factory selection to a three-state policy

The current boolean `trustAllCertificates` parameter conflates two ideas: "validate normally"
vs. "validate nothing." It is replaced by an explicit **TLS trust policy** with three states,
modeled as an app-owned enum so the call site is self-documenting and the dangerous state is
unreachable except by user enrollment:

```java
// new: mail/TlsTrustPolicy.java (app-owned; sole arbiter of socket-factory selection)
public enum TlsTrustPolicy {
    SYSTEM_VALIDATED,     // DefaultTrustedSocketFactory — platform chain validation (default)
    PINNED_CERTIFICATE    // PinnedCertificateSocketFactory — validates ONLY the enrolled cert for this host
}
```

`BackupImapStore` is reshaped so its constructor receives a resolved `TrustedSocketFactory`
(or the policy + the host's enrolled certificate, from which it builds one) rather than a raw
boolean. The selection becomes:

| Policy | Factory selected | Validation behavior |
|--------|------------------|---------------------|
| `SYSTEM_VALIDATED` (default) | `DefaultTrustedSocketFactory` | Full Android `TrustManager` chain validation against the system CA store |
| `PINNED_CERTIFICATE` (enrolled) | `PinnedCertificateSocketFactory` | Builds an `X509TrustManager` whose `checkServerTrusted()` accepts **only** the user-enrolled certificate for the specific host; rejects all others — never empty, never `null` issuers |

There is no third "trust everything" state. The deleted `AllTrustedSocketFactory` had no
replacement that performs partial validation, because none is needed: the legitimate
self-hosted case is served by pinning the specific certificate, not by disabling validation.

`PinnedCertificateSocketFactory` implements `com.fsck.k9.mail.ssl.TrustedSocketFactory`
(same interface the deleted class implemented and `DefaultTrustedSocketFactory` implements),
so it drops into the same `ImapStore` super-constructor slot. Its `X509TrustManager`
satisfies AC-2: `checkServerTrusted()` validates the presented chain's leaf against the
enrolled certificate (e.g., by SHA-256 fingerprint equality plus validity-window check) and
throws `CertificateException` on mismatch — a non-empty body that examines the chain.

> **Note on scope of the `BackupImapStore` reshape.** This design changes only the
> constructor's *factory selection*. The `getFolder`/`getMessages`/`append`/`fetch` surface,
> the `getStoreUriForLogging()` credential masking (`BackupImapStore.java:98-114`), the
> `isValidUri`/`isValidImapFolder` validators, and the `BackupFolder`/`MessageComparator`
> inner classes are untouched. DES-009 later moves this whole surface behind `MailTransport`;
> MU-003 must not pre-empt that and must keep the change minimal (REQ Constraint 3).

### Decision 3 — Pinned-certificate enrollment: an explicit, user-initiated opt-in (AC-4)

The pinned-certificate path is the *only* mechanism by which relaxed (non-system) trust can
ever be activated, and it can only be activated by a deliberate user action. The flow:

1. **Entry point — IMAP server settings.** A new preference action is added under the
   existing `AdvancedSettings$Server` screen (`preferences.xml:15-59`,
   `AdvancedSettings.Server` at `AdvancedSettings.java:347-368`). The action replaces the
   removed `server_trust_all_certificates` checkbox (Decision 4). It is a button-style
   `Preference` ("Pin a self-signed / private-CA certificate") that launches enrollment for
   the currently configured server address (`SERVER_ADDRESS`, `AuthPreferences.java:42`).

2. **Fetch-and-display.** The app opens a TLS connection to the configured host:port,
   captures the presented leaf certificate **without trusting it for any data operation**,
   and displays for user inspection (AC-4):
   - Subject (CN / SANs)
   - Issuer
   - **SHA-256 fingerprint** (the primary identity the user verifies out-of-band)
   - Validity window (not-before / not-after / expiry)

3. **Affirmative consent.** Enrollment requires a deliberate confirmed action — a dialog with
   an explicit "Trust this certificate" button (not a default-focused OK; the safe choice is
   the default). Cancelling enrolls nothing and leaves the policy at `SYSTEM_VALIDATED`.

4. **Persist, keyed per host:port.** On confirmation, the certificate (DER bytes) is stored
   in a dedicated per-host pinned-cert store keyed by `host:port` (see Decision 5). The
   server's policy for that host becomes `PINNED_CERTIFICATE`.

5. **Scoped selection.** `PinnedCertificateSocketFactory` is selected **only** for connections
   to that specific host:port, trusting **only** the enrolled certificate. Any other host
   continues to use `SYSTEM_VALIDATED`. Pinning the self-hosted server does not weaken Gmail
   connections.

6. **Persistent UI indicator.** While a pinned certificate is enrolled, the settings UI shows
   a persistent label (e.g., a summary on the pin preference: "Relaxed validation active —
   pinned certificate for {host}") so the relaxed state is never invisible (AC-4 final bullet).
   A "Remove pinned certificate" action reverts the host to `SYSTEM_VALIDATED`.

7. **Never automatic.** No launch, upgrade, migration, or non-user-initiated event may write
   the pinned store or set `PINNED_CERTIFICATE` (AC-4, AC-10). `migrate()` in particular never
   touches it (Decision 6).

### Decision 4 — Remove the trust-all preference key and its UI (AC-10)

`SERVER_TRUST_ALL_CERTIFICATES` ("server_trust_all_certificates") is retired as a
user-settable, silently-writable boolean:

- **UI removed.** The `<CheckBoxPreference android:key="server_trust_all_certificates">`
  (`preferences.xml:54-58`) and its strings `ui_protocol_trust_all_certificates_label` /
  `_desc` (`strings.xml:90-91`, plus the 12 verified locale copies in `values-da/de/fr/gl/ko/nl/pl/pt-rPT/sv/tr/zh-rCN/zh-rTW`)
  are removed and replaced by the pin-certificate action's strings. Scope verified via grep
  this session.
- **The key's only legitimate writer becomes the pinned-cert enrollment flow** (AC-10). For
  backward compatibility the reader `isTrustAllCertificates()` is retired in favor of a
  policy resolver (`getTlsTrustPolicy(host)`); the migration (Decision 6) actively clears any
  stale `true` value left by the old `migrate()` on already-affected devices.

> **Migration-of-already-downgraded users — design point.** A device that already ran the old
> `migrate()` has `SERVER_TRUST_ALL_CERTIFICATES=true` persisted. The new code must **not**
> honor that stale flag as "trust all" — doing so would leave the exposure open. The new
> `migrate()` (Decision 6) clears it to `false`, restoring validated TLS, and the one-time
> notice (AC-6) tells the user how to re-enroll a pinned cert if their server genuinely needs
> it. This is the "migration UX for already-downgraded users" required by the integration
> brief.

### Decision 5 — Per-host pinned-certificate store

A small app-owned store persists enrolled certificates keyed by `host:port`:

- **Storage.** A dedicated `SharedPreferences` file (e.g., `pinned_certs`) mapping
  `host:port` → Base64(DER certificate). It deliberately mirrors the existing
  credential-segregation discipline (`AuthPreferences.getCredentials()` uses a separate
  `credentials` file, `AuthPreferences.java:221-226`). Like `credentials.xml`, the pinned-cert
  file should be added to the backup-exclusion descriptor so a pinned cert does not silently
  travel to a new device via cloud backup (consistency with ARCH-009's defensive design).
- **No Keystore encryption required.** A server's public certificate is not a secret; integrity
  (it cannot be silently swapped) matters more than confidentiality. It lives in app-private
  storage (`MODE_PRIVATE`). (This store is independent of MU-004's `SecretStore`, which covers
  *credentials*; the two must not be conflated.)
- **Lookup at connect time.** `ServiceBase.getBackupImapStore()` resolves the policy for the
  configured host: if a pinned cert exists, policy is `PINNED_CERTIFICATE` with that cert;
  otherwise `SYSTEM_VALIDATED`.

### Decision 6 — Rewrite migrate() to never enable trust-all; one-time notice (AC-5, AC-6, AC-9)

The rewritten `AuthPreferences.migrate()`:

```java
void migrate() {
    if (useXOAuth()) {
        return;
    }
    final String protocol = getServerProtocol();
    final boolean wasLegacyDowngradeProtocol =
        "+ssl".equals(protocol) || "+tls".equals(protocol);

    SharedPreferences.Editor edit = preferences.edit();

    // AC-5 / AC-10: NEVER write SERVER_TRUST_ALL_CERTIFICATES=true.
    // AC-6 migration-of-already-downgraded-users: actively clear any stale true
    // left by the previous app version's silent downgrade, restoring validated TLS.
    if (preferences.getBoolean(SERVER_TRUST_ALL_CERTIFICATES, false)) {
        edit.putBoolean(SERVER_TRUST_ALL_CERTIFICATES, false);
        markTransportSecurityNoticePending(edit);   // AC-6 one-time notice
    }

    // Protocol normalization is preserved (AC-5 "may update SERVER_PROTOCOL"),
    // but WITHOUT the coupled trust-all write.
    if (wasLegacyDowngradeProtocol) {
        edit.putString(SERVER_PROTOCOL, protocol + "+");
        markTransportSecurityNoticePending(edit);   // AC-6: these users were the affected cohort
    }

    edit.apply();   // ARCH-017: apply(), not commit(), for a launch-path write
}
```

Key properties, each mapped to an AC:

- **AC-5 / AC-10:** No branch writes `true` for `SERVER_TRUST_ALL_CERTIFICATES`. The only
  writes are `false` (clearing a stale downgrade) — for any combination of stored
  `SERVER_PROTOCOL` values.
- **AC-6:** A one-time notice flag is set for the affected cohort (users on `+ssl`/`+tls`, or
  users carrying a stale `true`). On next UI presentation the app shows a notice explaining:
  (a) the connection now uses certificate validation; (b) self-hosted/private-CA users can
  enroll a pinned cert via settings; (c) no action is required for Gmail or other
  publicly-trusted servers. The notice flag is persisted-as-shown and never re-displayed
  (a `transport_security_notice_shown` boolean in default prefs, checked/cleared by the
  presentation layer — analogous to the existing `sms_default_package_change_seen` one-time
  pattern at `Preferences.java:247-253`).
- **AC-9:** Users not on `+ssl`/`+tls` (already-validated-TLS users, OAuth2 users via the
  early `useXOAuth()` return, users with no trigger) have no trust-all flag written or
  cleared, no server address/port change, and no notice — zero behavior change. The existing
  `BackupImapStoreTest` cases for these paths pass unchanged.

> **Sequencing gate (AC-7, hard constraint).** This `migrate()` rewrite must not be merged
> until the characterization test (`AuthPreferencesTest.migrate()` paths) backfilled by
> REQ-MODERNIZATION-003 / MU-002 is green on main. The characterization test pins the
> *current* (downgrading) behavior first; the rewrite then changes it under the safety net.
> See `## Design Validation`.

## Architecture

```
  App.onCreate()  →  Preferences.migrate()  →  AuthPreferences.migrate()   [REWRITTEN]
        │                                            │
        │                                            ├─ NEVER writes trust-all=true (AC-5)
        │                                            ├─ clears stale trust-all=true → false (AC-6 migration UX)
        │                                            └─ sets one-time-notice-pending for affected cohort (AC-6)
        ▼
  (next UI presentation) ── shows one-time transport-security notice, persists "shown"

  ServiceBase.getBackupImapStore()
        │  resolve TLS policy for configured host:port
        │     ├─ pinned cert exists  → PINNED_CERTIFICATE (+ enrolled cert)
        │     └─ otherwise           → SYSTEM_VALIDATED            (default; AC-3)
        ▼
  BackupImapStore(ctx, uri, TrustedSocketFactory)   [factory resolved, not a boolean]
        │
        ├─ SYSTEM_VALIDATED   → DefaultTrustedSocketFactory   (platform chain validation)
        └─ PINNED_CERTIFICATE → PinnedCertificateSocketFactory (validates ONLY enrolled cert; AC-2/AC-4)
        ▼
  super(BackupStoreConfig, TrustedSocketFactory, ConnectivityManager)   [k-9 ImapStore]
        ▼
  IMAP server (Gmail 993 / self-hosted)

  AdvancedSettings$Server  ──(user-initiated, AC-4)──▶  Pinned-cert enrollment
        │  fetch leaf cert → show subject/issuer/SHA-256/expiry → affirmative consent
        ▼
  PinnedCertStore  (per host:port; backup-excluded)  ◀── read at connect time

  DELETED: mail/AllTrustedSocketFactory.java  (AC-1)
  REMOVED: server_trust_all_certificates checkbox + strings (13 locales)  (AC-10)

  ── boundary handed to DES-009 ──
  The (uri, TrustedSocketFactory) construction surface is the contract candidate that
  DES-MODERNIZATION-009's MailTransport adapter (K9MailTransport) consumes: the validated/
  pinned factory selected HERE is injected INTO the ACL adapter THERE. Security first,
  then the ACL wraps it (migration-units.md Constraint 3).
```

## Components

| Component | Responsibility | Disposition |
|-----------|----------------|-------------|
| `mail/AllTrustedSocketFactory.java` | (removed) trust-all factory + empty `InsecureX509TrustManager` | **DELETED** (AC-1) |
| `mail/TlsTrustPolicy.java` (new) | App-owned enum: `SYSTEM_VALIDATED` / `PINNED_CERTIFICATE`; the only arbiter of relaxed trust | **NEW** |
| `mail/PinnedCertificateSocketFactory.java` (new) | `TrustedSocketFactory` whose `X509TrustManager.checkServerTrusted()` validates ONLY the enrolled cert for the host (non-empty, examines chain) | **NEW** (AC-2, AC-4) |
| `mail/PinnedCertStore.java` (new) | Per-`host:port` enrolled-certificate persistence; backup-excluded; read at connect time, written only by enrollment | **NEW** (AC-4, AC-10) |
| `mail/BackupImapStore.java` | Constructor takes a resolved `TrustedSocketFactory` (or policy+cert) instead of a boolean; factory-selection only — rest unchanged | **MODIFIED** (`:58-63`) |
| `preferences/AuthPreferences.java` | `migrate()` rewritten (never writes trust-all=true; clears stale; flags one-time notice); `isTrustAllCertificates()` retired for a policy resolver | **MODIFIED** (`:196-198`, `:276-288`) |
| `service/ServiceBase.java` | `getBackupImapStore()` resolves TLS policy for the configured host and passes a factory, not `isTrustAllCertificates()` | **MODIFIED** (`:99-105`) |
| Enrollment UI in `activity/fragments/AdvancedSettings$Server` | User-initiated pin action: fetch/display cert, affirmative consent, persistent indicator, removal | **MODIFIED** (`:347-368`) + `preferences.xml:54-58` |
| One-time-notice presentation | Shows AC-6 notice once, persists "shown" (pattern: `Preferences.java:247-253`) | **NEW** (small) |
| `res/xml/preferences.xml`, `res/values*/strings.xml` | Remove trust-all checkbox + strings (13 locales); add pin-cert action strings | **MODIFIED** |

## Interfaces

The cross-component boundary this design introduces is the **socket-factory / transport
construction boundary**: the point at which a resolved `TrustedSocketFactory` is handed to
the IMAP store. Today it is the `BackupImapStore` constructor's third argument; after
DES-009 it becomes an injection point on the `MailTransport`/`K9MailTransport` adapter.

Producer (this design): the TLS-policy resolution in `ServiceBase.getBackupImapStore()` plus
`PinnedCertStore`, which together yield exactly one of `DefaultTrustedSocketFactory` or
`PinnedCertificateSocketFactory`.

Consumer (this design, then DES-009): `BackupImapStore` super-constructor (k-9 `ImapStore`);
later `K9MailTransport` (DES-009 §Interfaces explicitly states it "receives the validated
`TrustedSocketFactory` (or user-pinned-cert factory) selected by DES-002").

The contract is: **the transport adapter never selects its own trust behavior; it receives a
fully-resolved, never-trust-all `TrustedSocketFactory` from the security policy layer.** This
is formalized in `## Integration Contracts` as a contract candidate (CNTR needed) feeding
MU-008's `MailTransport`.

## Integration Design

This section addresses the three integration concerns named in the design brief:
socket-factory selection in `BackupImapStore`, composition with the future `MailTransport`
ACL (DES-009), and the migration UX for already-downgraded users.

### Socket-factory selection in BackupImapStore

Before (verified `BackupImapStore.java:58-63`): the constructor took
`boolean trustAllCertificates` and selected `AllTrustedSocketFactory.INSTANCE` or
`new DefaultTrustedSocketFactory(context)` inline. The boolean originated at the single
production caller `ServiceBase.java:104` from `isTrustAllCertificates()`.

After: factory selection is hoisted *out* of the boolean ternary. The security-policy layer
(`ServiceBase.getBackupImapStore()` + `PinnedCertStore`) resolves a concrete
`TrustedSocketFactory` and passes it to `BackupImapStore`. The store no longer makes a
trust decision — it receives one. This is deliberate: it makes the trust decision a single,
auditable choke point (one write site for the dangerous state — AC-10) and makes the store
trivially composable with the DES-009 adapter, which expects to *receive* a factory.

Test impact (verified `BackupImapStoreTest.java`): four constructor call sites use the
`boolean` signature — `testAccountHasStoreUri` (`:42`, false), `testShouldCreateCorrectTrustFactoryForTrustedSSLUrl`
(`:48`, false), `testShouldCreateCorrectTrustFactoryForTrustAllSSLUrl` (`:54`, **true**, asserts
`AllTrustedSocketFactory.class` at `:55`), `testShouldCreateCorrectTrustFactoryForTrustedTLSUrl`
(`:60`, false), plus the `shouldHaveToString*` and `shouldThrowException*` cases (all false).
The trust-all assertion at `:52-56` must be rewritten to assert that the active factory is
**not** `AllTrustedSocketFactory` and contains no `InsecureX509TrustManager` (AC-8); since
the class is deleted, the test must instead exercise the pinned-cert path and assert
`PinnedCertificateSocketFactory`. The `false` cases continue to assert
`DefaultTrustedSocketFactory` and require only a signature adaptation (factory or
`SYSTEM_VALIDATED` policy instead of `false`). The package-private test seam
`getTrustedSocketFactory()` (`BackupImapStore.java:116-118`) is preserved as the assertion hook.

### Composition with the future MailTransport ACL (DES-009) — security first

`mail/BackupImapStore.java` is a shared file: MU-003 (this) and MU-008 (DES-009) both edit
it, and the allocation is **MU-003 first** (`migration-units.md` Shared-File Allocation;
REQ Constraint 3; DES-009 §Sequencing hard constraint). The composition contract:

- This design removes the trust-all arm and establishes the rule "the store receives a
  validated/pinned factory; it never selects trust-all." 
- DES-009 then reshapes `BackupImapStore` into `K9MailTransport implements MailTransport`,
  and — per DES-009 §Interfaces — "`K9MailTransport` receives the validated
  `TrustedSocketFactory` (or user-pinned-cert factory) selected by DES-002." DES-009
  explicitly defers the socket-factory injection point to this design.
- Therefore the factory-resolution layer built here (policy resolver + `PinnedCertStore`)
  survives the ACL refactor unchanged; only its *consumer* moves from the `BackupImapStore`
  constructor to the `MailTransport` adapter's construction (`MailTransportConfig` carries
  "uri + TLS policy handle from DES-002" per DES-009's port-operation table).

Landing MU-008 before MU-003, or co-editing the file, is a constraint violation: the ACL must
wrap an already-validated transport, not a trust-all one.

### Migration UX for already-downgraded users

The cohort that already executed the old `migrate()` carries `SERVER_TRUST_ALL_CERTIFICATES=true`.
The integration path for them (Decision 6):

1. New `migrate()` runs on next launch, detects the stale `true`, writes `false` (restoring
   validated TLS immediately — the exposure closes on first launch of the fixed build).
2. It flags the one-time notice (AC-6). The notice is presented at the next UI opportunity,
   explaining the change and pointing self-hosted users to the pin-certificate action.
3. If the user genuinely runs a self-hosted server with a private cert, their next backup
   will fail TLS validation; the notice has already told them how to enroll a pinned cert.
   This is the *intended* fail-closed behavior — validation failures are surfaced, not
   silently bypassed (REQ Rationale: "The risk does not come from supporting private
   certificates; it comes from accepting any certificate without user knowledge or consent.").

## Design Validation

This section maps each acceptance criterion to a verification mechanism and records the
verifications already performed this session.

### Verification matrix

| AC | What it requires | How this design verifies it |
|----|------------------|------------------------------|
| AC-1 | `AllTrustedSocketFactory` deleted; no import/`INSTANCE` reference | **grep gate** post-implementation: `grep -rn AllTrustedSocketFactory app/src/main/java` returns zero. Scope verified this session: only reachable references are `BackupImapStore.java:61` (removed) and test `:55` (rewritten). |
| AC-2 | No production `X509TrustManager` with empty `checkServerTrusted()` | **grep + read gate**: enumerate `X509TrustManager` impls; the only one (`InsecureX509TrustManager`) is deleted; the new `PinnedCertificateSocketFactory`'s trust manager has a non-empty body that examines the chain. |
| AC-3 | Validated TLS by default on every IMAP TLS path | **Test**: `BackupImapStoreTest` default-path cases assert `DefaultTrustedSocketFactory`; construction has no branch yielding an unvalidated factory absent enrollment. |
| AC-4 | Consensual user-initiated pinned-cert path (display/affirm/persist/scope/never-auto/indicator) | **Instrumented + unit test** of enrollment: cert details displayed; enrollment requires affirmative action; stored per host:port; factory scoped to that host; no auto-activation; persistent indicator shown. |
| AC-5 | `migrate()` never writes trust-all=true under any branch | **Characterization + new unit test**: parametrize `SERVER_PROTOCOL` over `+ssl/+tls/ssl/tls/custom`; assert `SERVER_TRUST_ALL_CERTIFICATES` never becomes `true`. |
| AC-6 | One-time notice for previously-affected `+ssl`/`+tls` users | **Unit test**: after `migrate()` on a `+ssl`/`+tls` or stale-`true` device, notice-pending flag set; after presentation, "shown" persisted; not re-shown on second `migrate()`/launch. |
| AC-7 | `migrate()` characterization test green before any `migrate()` change merges | **Process/CI gate** (see below) — hard sequencing constraint. |
| AC-8 | `BackupImapStoreTest` asserts non-`AllTrustedSocketFactory` factory before deletion | **Test ordering**: the rewritten assertion (asserts validated/pinned factory, no `InsecureX509TrustManager`) lands and passes before `AllTrustedSocketFactory.java` is deleted. |
| AC-9 | Already-validated / OAuth2 / no-trigger users unchanged | **Test**: `migrate()` leaves trust-all default, server address/port, protocol untouched for these; existing `BackupImapStoreTest` false-path cases pass without regression. |
| AC-10 | trust-all=true writable only by pinned-cert enrollment | **grep audit gate**: enumerate every write to `SERVER_TRUST_ALL_CERTIFICATES`; confirm `migrate()`, `App.onCreate()`, init paths, defaults never write `true`; only enrollment does (and Decision 4 retires the key in favor of the per-host pinned store, so the dangerous boolean is effectively eliminated). |

### No-unvalidated-cert-path proof obligation (grep + test)

The implementation's definition of done includes a repository-wide grep proving no residual
trust-all path: (1) `AllTrustedSocketFactory` — zero matches; (2) every `X509TrustManager`
`checkServerTrusted` body inspected for emptiness; (3) every `SERVER_TRUST_ALL_CERTIFICATES`
write-site inspected (AC-10). Scope for all three was verified this session and is small and
enumerable — the change is genuinely isolated to `mail/` + `preferences/AuthPreferences.java`
+ `service/ServiceBase.java` + the `AdvancedSettings$Server` UI + resource files. Scope
verified via grep.

### migrate() characterization-test dependency (AC-7, depends on DES-003 harness)

The `migrate()` rewrite is gated on the characterization test backfilled by
REQ-MODERNIZATION-003 / MU-002 (`AuthPreferencesTest.java`). That test must (a) pin the
*current* downgrading behavior of `migrate()` (Feathers Ch. 2 characterization — assert what
the code does, not what it should), (b) be green on main, and (c) be in the CI run **before**
the first commit changing `migrate()` merges. This is a hard process constraint
(REQ AC-7 / Constraint 2; `target-state.md` Technology Principle #4). The test harness upgrade
itself (Robolectric 4.3.1→4.12+, JaCoCo gate) is delivered by DES-MODERNIZATION-003 / MU-002;
this design consumes it and must not begin step 2 of the MU-003 sequence
(REQ §Implementation Sequence) until it is present and green.

### Pinned-cert requires explicit action (negative test)

A dedicated negative test asserts that no code path — `App.onCreate()`, `Preferences.migrate()`,
`AuthPreferences.migrate()`, settings load — ever writes the `PinnedCertStore` or sets
`PINNED_CERTIFICATE`. Only the enrollment confirmation handler may. This locks AC-4's
"never automatic" and AC-10's single-writer guarantee.

## Integration Contracts

> **Required for any design that introduces cross-component boundaries.**
> For each boundary, either reference an existing approved CNTR-* artifact or note that one must be created before stories referencing this design can be sprint-planned.

| Boundary | Producer | Consumer(s) | Contract Type | CNTR Artifact | Status |
|----------|----------|-------------|---------------|---------------|--------|
| TLS trust-policy → IMAP transport (resolved `TrustedSocketFactory` handoff) | `ServiceBase` TLS-policy resolver + `PinnedCertStore` (this design) | `BackupImapStore` (now); `MailTransport`/`K9MailTransport` (DES-009/MU-008) | service | CNTR-MODERNIZATION-TLSFACTORY-001 | needed |
| Pinned-certificate enrollment (UI → `PinnedCertStore`) | `AdvancedSettings$Server` enrollment action | `PinnedCertStore` | ui/service | CNTR-MODERNIZATION-PINENROLL-001 | needed |

### Contracts Needed (pre-sprint gate)
These boundaries do NOT yet have an approved CNTR-* artifact. They must be resolved by running
`/amp:create-contracts` before any story referencing this design can be approved for a sprint.

- [ ] **CNTR needed — socket-factory/transport boundary:** the contract that a fully-resolved,
  never-trust-all `TrustedSocketFactory` is produced by the security-policy layer and *consumed*
  (never re-decided) by the IMAP transport. This is the contract candidate the design brief
  calls out as "feeding MU-008's MailTransport": DES-009 already declares it consumes the
  factory selected here, so the two designs must agree on this interface before MU-008 is
  sprint-planned. Defines: factory type (`com.fsck.k9.mail.ssl.TrustedSocketFactory`),
  resolution inputs (`host:port`, pinned-cert presence), and the invariant "no `AllTrustedSocketFactory`,
  no empty-body trust manager."
- [ ] **CNTR needed — pinned-cert enrollment UI/service boundary:** what the enrollment flow
  displays (subject/issuer/SHA-256/expiry), the affirmative-consent requirement, the per-host
  key, and the persistent indicator — so the UI story and the store story agree.

## Trade-offs

| Decision | Trade-off | Why this way |
|----------|-----------|--------------|
| Delete trust-all; replace with per-host pinning, not partial validation | Self-hosted users must perform a one-time enrollment (friction) instead of a single checkbox | A checkbox that disables *all* validation is the vulnerability. Pinning trusts exactly the cert the user verified out-of-band — the legitimate self-hosted case — with no MITM surface. (REQ Rationale; ADR-007) |
| Hoist factory selection out of `BackupImapStore` into a policy resolver | One more indirection / a new enum + store | Single auditable trust choke point (AC-10); clean composition with the DES-009 ACL, which expects to *receive* a factory; minimal change to the store itself (REQ Constraint 3). |
| New `migrate()` actively clears stale `trust-all=true` to `false` | A self-hosted user who previously relied on the silent downgrade will see their next backup fail until they enroll a pinned cert | Fail-closed is correct for a security downgrade. The exposure must close on first launch; the one-time notice (AC-6) tells the user exactly how to restore their self-hosted setup safely. |
| Pinned-cert store unencrypted in app-private prefs, backup-excluded | Not Keystore-encrypted like credentials | A server's public certificate is not a secret; integrity (no silent swap) and backup-exclusion (no silent cross-device travel) are the relevant properties. Conflating it with MU-004's `SecretStore` would over-engineer it. |
| `migrate()` uses `apply()` not `commit()` | Slightly weaker write-durability guarantee on the launch path | ARCH-017: the launch path should not block on a synchronous fsync; the write is idempotent across launches, so `apply()` is safe and correct here. |

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| `migrate()` rewrite silently changes an adjacent prefs path | Low | High | AC-7 characterization test pins current behavior before the change (hard gate); rewrite is small and parametrized-tested over all `SERVER_PROTOCOL` values (AC-5/AC-9). |
| MU-008 (DES-009) edits `BackupImapStore` before MU-003 lands, re-introducing trust-all | Medium | Critical | Shared-File Allocation mandates MU-003 first; DES-009 §Sequencing encodes the same hard constraint; the contract gate (CNTR-TLSFACTORY) blocks MU-008 sprint-planning until the factory boundary is agreed. |
| Self-hosted users perceive the change as a regression (backup stops working) | Medium | Medium | One-time notice (AC-6) explains the change and the pin-cert remedy before they hit a failure; persistent settings indicator (AC-4) keeps the relaxed state visible. |
| Pinned-cert UI fetches and pins a MITM'd cert during enrollment | Low | Medium | Display the SHA-256 fingerprint prominently so the user verifies it out-of-band against their server; enrollment is a deliberate, informed act, not a blind "trust whatever is presented." (This is strictly better than the deleted blanket trust, which verified nothing.) |
| 13-locale string removal/addition introduces a missing-translation build issue | Low | Low | `warningsAsErrors`/lint will catch missing string refs; batch the locale edits; new pin-cert strings can fall back to the default locale until translated (minimize churn — `target-state.md` Constraints). |
| Cert expiry on a pinned self-hosted server breaks backups | Medium | Low | Display expiry at enrollment (AC-4); `PinnedCertificateSocketFactory` surfaces a clear validation error; the user re-enrolls. (Out of scope: automatic re-pin — deliberately, to preserve the consent model.) |

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| REQ-MODERNIZATION-002 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-002-eliminate-transport-security-mitm.md | The requirement traced; AC-1..AC-10, constraints, exploit path, implementation sequence |
| Target State (ADR-007) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/target-state.md | ADR-007 (remove trust-all, validated TLS, user-pinned cert), Security Architecture §Transport/Migration safety, QAS Transport security |
| Migration Units (MU-003, MU-008, MU-000) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md | MU-003 scope/sequencing; Shared-File Allocation (BackupImapStore, AuthPreferences); Preserved-Core boundary |
| Ilities Assessment (ARCH-007/008/017) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/architecture/ilities-assessment.md | ARCH-007 (Critical trust-all), ARCH-008 (silent downgrade), ARCH-017 (commit→apply), credential-segregation precedent |
| DES-MODERNIZATION-009 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-009-mail-acl-k9-unpin-design.md | MailTransport ACL composition; explicit "receives the validated factory selected by DES-002"; sequencing hard constraint |
| AllTrustedSocketFactory (source) | app/src/main/java/com/zegoggles/smssync/mail/AllTrustedSocketFactory.java | Verified: empty checkServerTrusted (`:42-57`), INSTANCE (`:22`), @SuppressLint (`:20`) — deleted by this design |
| BackupImapStore (source) | app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java | Verified: factory ternary (`:58-63`), test seam `getTrustedSocketFactory()` (`:116-118`), masking/validators unchanged |
| AuthPreferences (source) | app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java | Verified: SERVER_TRUST_ALL_CERTIFICATES (`:48`), isTrustAllCertificates (`:196-198`), migrate() (`:276-288`), getCredentials (`:221-226`) |
| ServiceBase (source) | app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java | Verified: sole production caller `getBackupImapStore()` passes isTrustAllCertificates (`:99-105`) |
| App / Preferences (source) | app/src/main/java/com/zegoggles/smssync/App.java; .../preferences/Preferences.java | Verified: migrate() wiring App.onCreate `:72` → Preferences.migrate `:294-296`; one-time-seen pattern `Preferences.java:247-253` |
| AdvancedSettings (source) | app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java | Verified: Server fragment `:347-368` — enrollment UI host |
| preferences.xml / strings.xml | app/src/main/res/xml/preferences.xml; app/src/main/res/values*/strings.xml | Verified: trust-all checkbox `preferences.xml:54-58`; strings `:90-91` + 12 locale copies |
| BackupImapStoreTest (source) | app/src/test/java/com/zegoggles/smssync/mail/BackupImapStoreTest.java | Verified: 4 constructor call sites; trust-all assertion `:52-56`; default-path assertions `:49,:61` |

## Notes

This design is **not finalized**. It is a content draft for DES-MODERNIZATION-002 covering
system-architecture, integration-design, and design-validation; frontmatter (id/status/
artifact_type/domain/related_requirements) is Artifact-Librarian-owned and untouched, and the
two integration boundaries require `/amp:create-contracts` (CNTR-MODERNIZATION-TLSFACTORY-001,
CNTR-MODERNIZATION-PINENROLL-001) before any referencing story is sprint-planned.
