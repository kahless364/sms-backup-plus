---
status: approved
artifact_type: interface-contract
consumers: []
related_requirements: []
related_design_docs: []
related_stories: []
change_records: []
id: CNTR-MODERNIZATION-001
title: ''
domain: modernization
contract_type: ''
producer: ''
---

# CNTR-MODERNIZATION-001: Validated/Pinned TLS Socket-Factory Handoff (TLS Trust-Policy → IMAP Transport)

**Producer:** the transport-security policy layer — `ServiceBase.getBackupImapStore()` TLS-policy resolver + `PinnedCertStore` (DES-MODERNIZATION-002 / MU-003).

**Consumers:**
- `com.zegoggles.smssync.mail.BackupImapStore` constructor (current consumer; DES-002).
- `com.zegoggles.smssync.mail.transport.K9MailTransport` adapter (future consumer; DES-MODERNIZATION-009 / MU-008), which reshapes `BackupImapStore` behind the `MailTransport` port and explicitly "receives the validated `TrustedSocketFactory` (or user-pinned-cert factory) selected by DES-002" (DES-009 §Interfaces).

> **Canonical-naming reconciliation (binding).** DES-002 (the producer/owner of this boundary)
> names the trust-policy enum **`TlsTrustPolicy`**. DES-009 §"Socket-factory injection" refers to
> the app-owned policy as `TlsPolicy` (`TlsPolicy.VALIDATED` / `TlsPolicy.PINNED(cert)`). These are
> the same concept under two names. **This contract reconciles them to the single canonical type
> `com.zegoggles.smssync.mail.TlsTrustPolicy`, owned by the producer (DES-002).** The canonical
> enum constants are **`SYSTEM_VALIDATED`** and **`PINNED_CERTIFICATE`** (DES-002 Decision 2) — *not*
> `VALIDATED` / `PINNED`. Both designs and all implementing stories MUST use `TlsTrustPolicy`,
> `SYSTEM_VALIDATED`, and `PINNED_CERTIFICATE` verbatim. Where DES-009 writes `TlsPolicy.VALIDATED`,
> read `TlsTrustPolicy.SYSTEM_VALIDATED`; where it writes `TlsPolicy.PINNED(cert)`, read the
> `PINNED_CERTIFICATE` policy carrying the enrolled certificate (see "Policy + certificate" below).
> The DES-009 carried-certificate idiom is preserved; only the type/constant *names* are unified.

## Overview

This contract governs the single security-critical object handed from the TLS trust-policy layer
to the IMAP transport: a **fully-resolved socket factory** that performs either platform chain
validation or per-host pinned-certificate validation. It exists because DES-002 deletes the
trust-all path (`mail/AllTrustedSocketFactory.java`, `INSTANCE` at `:22`, empty
`InsecureX509TrustManager.checkServerTrusted()` at `:49-52`, `getAcceptedIssuers()` returning
`null` at `:54-56`) and replaces the boolean `trustAllCertificates` selection at
`BackupImapStore.java:58-63` with a resolved factory. DES-009 later moves the consumer from the
`BackupImapStore` constructor to the `K9MailTransport` adapter, but the produced object and its
invariants are unchanged. This contract pins those invariants so the two designs cannot drift.

The defining property of this boundary is **one-directional trust authority**: the producer decides
trust; the consumer applies it. The consumer (transport) never re-decides, relaxes, or augments the
trust posture of the factory it receives.

## Contract Boundary

```
  PRODUCER (transport-security policy layer — DES-002)
  ServiceBase.getBackupImapStore()  +  PinnedCertStore
        │  1. resolve TlsTrustPolicy for the configured host:port
        │       - pinned cert exists for host:port  → PINNED_CERTIFICATE (+ enrolled X509Certificate)
        │       - otherwise                          → SYSTEM_VALIDATED
        │  2. build the concrete TrustedSocketFactory for that policy
        ▼
  ═══════════ CONTRACT SURFACE: a resolved TrustedSocketFactory ═══════════
        │   exactly one of:
        │     SYSTEM_VALIDATED   → new DefaultTrustedSocketFactory(context)
        │     PINNED_CERTIFICATE → new PinnedCertificateSocketFactory(context, host, enrolledCert)
        ▼
  CONSUMER (IMAP transport)
   now:    BackupImapStore(context, uri, TrustedSocketFactory)  → super(BackupStoreConfig, factory, ConnectivityManager)
   later:  K9MailTransport(MailTransportConfig{uri, TlsTrustPolicy})  → maps policy to the same factory internally
        ▼
   k-9 ImapStore opens the TLS connection using the factory's TrustManager — and only that one.
```

The producer side owns *trust resolution*. The consumer side owns *connection mechanics* (folder
open, append, fetch, search). The factory is the only object that crosses, and it is opaque to the
consumer with respect to trust: the consumer cannot inspect, downgrade, or replace the TrustManager
embedded in it.

## Contract Definition

### Carried type — the resolved factory

```
Interface: com.fsck.k9.mail.ssl.TrustedSocketFactory   (k-9 library interface; verified import BackupImapStore.java:30)
Method:    Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias)
             throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException
```

The object crossing the boundary is a `TrustedSocketFactory` whose `createSocket(...)` returns a
`javax.net.ssl.SSLSocket` wrapping an `SSLContext` initialized with a **non-permissive**
`javax.net.ssl.X509TrustManager`. Two and only two concrete producers are permitted:

| Policy (`TlsTrustPolicy`) | Concrete factory handed across | Embedded `X509TrustManager` behavior |
|---------------------------|--------------------------------|--------------------------------------|
| `SYSTEM_VALIDATED` (default) | `com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory` (k-9; imported `BackupImapStore.java:29`) | Full Android `TrustManager` chain validation against the system CA store. `checkServerTrusted()` throws `CertificateException` on any untrusted/expired/hostname-mismatched chain. |
| `PINNED_CERTIFICATE` (enrolled) | `com.zegoggles.smssync.mail.PinnedCertificateSocketFactory` (new, app-owned; DES-002 Decision 2/3) | `checkServerTrusted()` validates the presented leaf against the user-enrolled certificate for this `host:port` (SHA-256 fingerprint equality + validity-window check) and throws `CertificateException` on mismatch. Non-empty body; `getAcceptedIssuers()` returns the enrolled cert, never `null`. |

> **App-owned wrapper note.** The carried type is the k-9 `TrustedSocketFactory` interface, not a
> raw `javax.net.ssl.SSLSocketFactory`, because that is the slot the k-9 `ImapStore`
> super-constructor accepts (`super(BackupStoreConfig, TrustedSocketFactory, ConnectivityManager)`,
> verified `BackupImapStore.java:60-62`). `PinnedCertificateSocketFactory` is the *app-owned*
> implementation of that interface; internally it constructs a `javax.net.ssl.SSLSocketFactory`
> from an `SSLContext`, exactly as the deleted `AllTrustedSocketFactory` did (verified
> `AllTrustedSocketFactory.java:27-39`) — but with a validating TrustManager rather than the empty
> `InsecureX509TrustManager`.

### Trust-policy enum (canonical — owned by producer/DES-002)

```
Enum: com.zegoggles.smssync.mail.TlsTrustPolicy
Constants:
  SYSTEM_VALIDATED      // platform CA chain validation; the default for every host with no enrolled cert
  PINNED_CERTIFICATE    // validates ONLY the user-enrolled cert for a specific host:port
```

**There is explicitly NO trust-all / accept-any state.** The enum has exactly two constants. No
`TRUST_ALL`, `INSECURE`, `ACCEPT_ANY`, or equivalent value may be added. Re-introducing such a
constant — or a factory whose TrustManager has an empty `checkServerTrusted()` body or a
`getAcceptedIssuers()` returning `null` — is a breaking violation of this contract and re-opens
ARCH-007 / CWE-295.

### Policy + certificate (the `PINNED_CERTIFICATE` payload)

`PINNED_CERTIFICATE` is meaningless without the enrolled certificate it pins. The producer resolves
both together:
- For `K9MailTransport` (DES-009), `MailTransportConfig` carries the **app-owned `TlsTrustPolicy`**;
  for `PINNED_CERTIFICATE` it also carries the enrolled `java.security.cert.X509Certificate` (or the
  `host:port` key the adapter uses to read it from `PinnedCertStore`). The adapter maps this to the
  concrete factory **inside the `mail` package** — the k-9 `TrustedSocketFactory` type must not
  cross the `MailTransport` port (DES-009 AC-2). This is the DES-009 `TlsPolicy.PINNED(cert)` idiom,
  expressed with the canonical name.
- For the current `BackupImapStore` constructor (DES-002), the producer resolves the concrete
  `TrustedSocketFactory` *before* construction and passes the factory directly (the constructor
  signature changes from `boolean trustAllCertificates` to a resolved `TrustedSocketFactory`).

### Handoff signatures

```
// CURRENT consumer (DES-002 — replaces BackupImapStore.java:58-63 boolean ternary)
//   BEFORE (verified, to be removed):
//     public BackupImapStore(Context context, String uri, boolean trustAllCertificates)
//       super(new BackupStoreConfig(uri),
//             trustAllCertificates ? AllTrustedSocketFactory.INSTANCE : new DefaultTrustedSocketFactory(context),
//             (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE));
//   AFTER (factory resolved by producer, passed in):
public BackupImapStore(Context context, String uri, TrustedSocketFactory socketFactory)
        throws MessagingException;
// super(new BackupStoreConfig(uri), socketFactory, (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE));

// PRODUCER resolution entry point (DES-002 — replaces isTrustAllCertificates() read at ServiceBase.java:104)
TlsTrustPolicy        getTlsTrustPolicy(String host, int port);     // resolver; SYSTEM_VALIDATED unless a pinned cert exists
TrustedSocketFactory  resolveSocketFactory(Context context, String host, int port);
//   returns DefaultTrustedSocketFactory for SYSTEM_VALIDATED,
//   PinnedCertificateSocketFactory(context, host, enrolledCert) for PINNED_CERTIFICATE

// FUTURE consumer (DES-009 — adapter receives policy, builds factory internally)
class MailTransportConfig { String uri; TlsTrustPolicy tlsTrustPolicy; X509Certificate pinnedCert; /* null unless PINNED_CERTIFICATE */ }
// K9MailTransport maps MailTransportConfig.tlsTrustPolicy → the same TrustedSocketFactory, inside mail.transport
```

The package-private test seam `BackupImapStore.getTrustedSocketFactory()` (verified
`BackupImapStore.java:116-118`, returns `mTrustedSocketFactory`) is preserved as the assertion hook
for both designs; DES-009 re-homes it onto `K9MailTransport`.

## Invariant

**The transport receives a fully-resolved, never-trust-all `TrustedSocketFactory`, and never
re-decides trust.** Formally, all of the following hold at and after the handoff:

1. **Fully resolved.** The factory crossing the boundary is one concrete instance, ready to use. The
   consumer performs no policy resolution, no `host:port` lookup, and no certificate fetch. (DES-002
   hoists resolution into the producer to create a single auditable choke point — AC-10.)
2. **Never trust-all.** The factory is exactly one of `DefaultTrustedSocketFactory` (system
   validation) or `PinnedCertificateSocketFactory` (single-cert validation). It is never
   `AllTrustedSocketFactory` (deleted, AC-1), never any factory whose `X509TrustManager` has an empty
   `checkServerTrusted()` body, and never one whose `getAcceptedIssuers()` returns `null` (AC-2).
3. **Consumer never re-decides.** The transport does not inspect, unwrap, replace, relax, or
   supplement the TrustManager inside the factory. It passes the factory verbatim to the k-9
   `ImapStore` super-constructor (now) or stores it for connection (adapter, later). No conditional
   in the consumer chooses between trust postures.
4. **Per-host scoping is the producer's responsibility.** A `PINNED_CERTIFICATE` factory validates
   only the enrolled cert for its specific `host:port`; pinning a self-hosted server never weakens a
   Gmail connection. The consumer relies on the producer having scoped correctly and does not
   broaden scope.
5. **Default is secure.** Absent an explicit, user-initiated pinned-cert enrollment, the resolved
   policy is `SYSTEM_VALIDATED` (AC-3). No launch, upgrade, migration, or non-user event can yield a
   relaxed factory (AC-4, AC-10).

## Error Handling

Errors are communicated across this boundary as exceptions thrown from `createSocket(...)` at
TLS-handshake time — never as a silent trust decision:

| Condition | Mechanism | Crossing behavior |
|-----------|-----------|-------------------|
| Untrusted / expired / hostname-mismatched chain under `SYSTEM_VALIDATED` | `DefaultTrustedSocketFactory`'s TrustManager throws `CertificateException`, surfaced from `createSocket(...)` (wrapped per k-9 as `MessagingException`) | Connection fails closed. The transport propagates `MessagingException` (DES-009 translates it to an app-owned `MailException`/`TemporaryImapException` at the ACL boundary). |
| Presented leaf does not match the enrolled cert under `PINNED_CERTIFICATE` | `PinnedCertificateSocketFactory`'s TrustManager throws `CertificateException` (SHA-256 mismatch or outside validity window) | Connection fails closed; same propagation path. Fail-closed is the intended behavior (DES-002 §Migration UX). |
| Producer cannot resolve a factory (e.g., enrolled cert bytes corrupt/undecodable) | Producer throws before handing anything across — never a trust-all fallback | The handoff does not occur with a degraded factory. `getBackupImapStore()` already declares `throws MessagingException` (verified `ServiceBase.java:99`); resolution failure surfaces here. |

**Prohibited error behavior:** the producer MUST NOT, on any resolution failure, fall back to a
trust-all or unvalidated factory. A failure to resolve a secure factory is a hard failure, not a
silent downgrade. (This is the inversion of the deleted `migrate()` downgrade behavior — ARCH-008.)

## Validation Rules

Both sides MUST honor:

1. `TlsTrustPolicy` has exactly two constants: `SYSTEM_VALIDATED`, `PINNED_CERTIFICATE`. Adding a
   third (any trust-all/insecure variant) breaks this contract.
2. No `X509TrustManager` reachable through the produced factory may have an empty
   `checkServerTrusted()` body or a `getAcceptedIssuers()` returning `null` (AC-2). Enforced by the
   AC-2 grep/read gate: `grep -rn AllTrustedSocketFactory app/src/main/java` returns zero post-MU-003.
3. The consumer's only interaction with the factory is to pass it to the k-9 `ImapStore` connection
   path. No trust branch may exist in the consumer.
4. `PINNED_CERTIFICATE` is set only by the user-initiated enrollment flow
   (CNTR-MODERNIZATION-PINENROLL-001); never by migration, launch, or upgrade (AC-4, AC-10).
5. The canonical names `TlsTrustPolicy` / `SYSTEM_VALIDATED` / `PINNED_CERTIFICATE` are used verbatim
   in all implementations; DES-009's `TlsPolicy.VALIDATED` / `.PINNED` are aliases that resolve to
   these and must be renamed to the canonical form when MU-008 lands.

## Example: resolution + handoff (concrete)

**Scenario A — Gmail (no pinned cert): `SYSTEM_VALIDATED`.**

```java
// PRODUCER: ServiceBase.getBackupImapStore() (DES-002 rewrite of the verified :99-105 method)
protected BackupImapStore getBackupImapStore() throws MessagingException {
    final String uri = getAuthPreferences().getStoreUri();          // e.g. imap+ssl+://user:pw@imap.gmail.com:993
    if (!BackupImapStore.isValidUri(uri)) {
        throw new MessagingException("No valid IMAP URI: " + uri);
    }
    final Uri parsed = Uri.parse(uri);
    final String host = parsed.getHost();                            // "imap.gmail.com"
    final int    port = parsed.getPort();                            // 993

    // 1. resolve policy — no pinned cert for imap.gmail.com:993 → SYSTEM_VALIDATED
    final TlsTrustPolicy policy = pinnedCertStore.getTlsTrustPolicy(host, port); // SYSTEM_VALIDATED

    // 2. build the concrete factory for that policy (never trust-all)
    final TrustedSocketFactory factory =
        (policy == TlsTrustPolicy.PINNED_CERTIFICATE)
            ? new PinnedCertificateSocketFactory(getApplicationContext(), host, pinnedCertStore.get(host, port))
            : new DefaultTrustedSocketFactory(getApplicationContext());          // ← chosen here

    // 3. HAND ACROSS the resolved factory — the transport never sees the policy or the boolean
    return new BackupImapStore(getApplicationContext(), uri, factory);
}
// CONSUMER: BackupImapStore passes `factory` straight to super(...); makes no trust decision.
```

**Scenario B — self-hosted server with an enrolled cert: `PINNED_CERTIFICATE`.**

```java
// Same resolver; user previously enrolled the leaf cert for mail.example.org:993 via the pin flow.
final TlsTrustPolicy policy = pinnedCertStore.getTlsTrustPolicy("mail.example.org", 993); // PINNED_CERTIFICATE
final X509Certificate enrolled = pinnedCertStore.get("mail.example.org", 993);            // DER-decoded leaf
final TrustedSocketFactory factory =
    new PinnedCertificateSocketFactory(getApplicationContext(), "mail.example.org", enrolled);
// PinnedCertificateSocketFactory.createSocket(...) builds an SSLContext whose X509TrustManager
// .checkServerTrusted() accepts ONLY `enrolled` (SHA-256 match + validity window), else CertificateException.
return new BackupImapStore(getApplicationContext(), uri, factory);
// If an on-path attacker presents a different cert → CertificateException → connection fails closed.
```

**Scenario C — future DES-009 adapter handoff (policy crosses the port, factory built inside).**

```java
// ENGINE (service.*): expresses intent in app types only — no k-9 TrustedSocketFactory crosses the port.
MailTransportConfig cfg = new MailTransportConfig(uri, TlsTrustPolicy.PINNED_CERTIFICATE, enrolledCert);
MailTransport transport = mailTransportProvider.create(cfg);   // Hilt-bound to K9MailTransport (DES-008)

// ADAPTER (mail.transport.K9MailTransport): maps the canonical policy to the SAME factory, inside mail.*
TrustedSocketFactory factory = (cfg.tlsTrustPolicy == TlsTrustPolicy.PINNED_CERTIFICATE)
    ? new PinnedCertificateSocketFactory(context, host, cfg.pinnedCert)
    : new DefaultTrustedSocketFactory(context);
// super(new BackupStoreConfig(cfg.uri), factory, connectivityManager);  // same k-9 slot, same invariant
```

## Versioning

- **Current version:** v1.
- **Breaking changes** (require a new contract version and consumer migration):
  - Adding any third `TlsTrustPolicy` constant, especially any trust-all/insecure variant.
  - Changing the carried type away from `com.fsck.k9.mail.ssl.TrustedSocketFactory` while the k-9
    `ImapStore` super-constructor slot still expects it.
  - Permitting the consumer to make any trust decision (any trust branch in the consumer).
  - Renaming `TlsTrustPolicy` / `SYSTEM_VALIDATED` / `PINNED_CERTIFICATE`.
- **Backward-compatible changes** (no consumer break): tightening `PinnedCertificateSocketFactory`
  validation (e.g., adding hostname-match on top of fingerprint); changing the internal storage
  format of `PinnedCertStore`; moving the consumer from the `BackupImapStore` constructor to
  `K9MailTransport` (DES-009 explicitly preserves the produced object and invariant).

## Dependencies

- **CNTR-MODERNIZATION-PINENROLL-001** (pinned-cert enrollment UI → `PinnedCertStore`): the *only*
  legitimate writer of the `PINNED_CERTIFICATE` state this contract reads. The enrolled certificate
  this contract's `PINNED_CERTIFICATE` factory pins is produced by that boundary.
- **CNTR-MODERNIZATION-009-mailtransport** (the `MailTransport` port, DES-009): the future consumer
  side. That contract must carry `TlsTrustPolicy` (canonical) in `MailTransportConfig` and map it to
  the factory inside the adapter; it must not let a k-9 `TrustedSocketFactory` cross the port.

## Notes

- **Not finalized.** Content draft for CNTR-MODERNIZATION-001 (create-contracts Step 6). Frontmatter
  (`id`, `status`, `artifact_type`) is Artifact-Librarian-owned and untouched.
- **Naming reconciliation is the load-bearing decision of this contract:** DES-002's `TlsTrustPolicy`
  / `SYSTEM_VALIDATED` / `PINNED_CERTIFICATE` is canonical; DES-009's `TlsPolicy.VALIDATED` /
  `.PINNED(cert)` are aliases to be renamed at MU-008. Both designs are otherwise consistent: DES-009
  §Interfaces already declares it consumes the factory selected by DES-002, and the carried-cert
  idiom is preserved.
- **Sequencing.** MU-003 (DES-002, producer) lands before MU-008 (DES-009, future consumer) on the
  shared file `mail/BackupImapStore.java`; the ACL must wrap an already-validated transport.
- **Source verified this session:** `TrustedSocketFactory` interface + `createSocket` signature and
  the `DefaultTrustedSocketFactory` import (`BackupImapStore.java:24-35`); the boolean trust-all
  ternary and super-constructor slot (`BackupImapStore.java:58-63`); the `getTrustedSocketFactory()`
  test seam (`:116-118`); the empty `InsecureX509TrustManager` and `getAcceptedIssuers()`→`null`
  (`AllTrustedSocketFactory.java:42-58`); the sole production caller `getBackupImapStore()` passing
  `isTrustAllCertificates()` and declaring `throws MessagingException` (`ServiceBase.java:99-105`).
