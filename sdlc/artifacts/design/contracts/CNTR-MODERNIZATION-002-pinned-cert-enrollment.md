---
status: approved
artifact_type: interface-contract
consumers: []
related_requirements: []
related_design_docs: []
related_stories: []
change_records: []
id: CNTR-MODERNIZATION-002
title: ''
domain: modernization
contract_type: ''
producer: ''
---

# CNTR-MODERNIZATION-002: Pinned-Certificate Enrollment Boundary

**Title:** Pinned-Certificate Enrollment — UI/Service Boundary to the `PinnedCertStore`
**Producer:** The pinned-certificate **enrollment UI/service** — the user-initiated opt-in flow launched from `AdvancedSettings$Server` (`app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java:347-368`), which fetches and displays a server's leaf certificate, obtains affirmative consent, and writes the enrolled certificate. Realizes DES-MODERNIZATION-002 Decision 3 and the design's `CNTR-MODERNIZATION-PINENROLL-001` boundary.
**Consumers:** The **TLS policy resolver** — `ServiceBase.getBackupImapStore()` (`app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java:99-105`) — reading the `PinnedCertStore` at connect time to decide `SYSTEM_VALIDATED` vs. `PINNED_CERTIFICATE`; and the **one-time-notice presentation layer** consuming the downgraded-user notice flag. The resolved policy feeds the sibling factory-handoff contract **CNTR-MODERNIZATION-001** (TLS trust-policy → IMAP transport).

## Overview

This contract defines the boundary at which a user-enrolled, host-pinned server certificate
enters the application. It is the **single legitimate write path** by which SMS Backup+ may
relax TLS validation below full platform chain validation. DES-MODERNIZATION-002 deletes the
blanket-trust `AllTrustedSocketFactory` and retires the silently-written
`server_trust_all_certificates` boolean; this contract specifies what *replaces* them: a
deliberate, informed, per-host enrollment that pins exactly one certificate the user has
verified out-of-band.

The contract has three parts:

1. **`PinnedCertStore`** — the persistence interface (store / retrieve / clear, keyed by
   `host:port`) that the enrollment producer writes and the policy-resolver consumer reads.
2. **The enrolled-certificate data shape** — `EnrolledCertificate` (subject, issuer, SHA-256
   fingerprint, validity window, DER bytes) — what crosses the boundary and what the
   enrollment UI must display before consent.
3. **The enrollment-event semantics** — explicit user action mandatory; never auto-activated;
   plus the one-time-notice contract for users whose posture the legacy `migrate()` silently
   degraded.

The load-bearing invariant: **the relaxed-trust state (`PINNED_CERTIFICATE`, formerly
`SERVER_TRUST_ALL_CERTIFICATES = true`) is reachable only through `PinnedCertStore.store(...)`,
and `store(...)` is reachable only from an affirmative user enrollment confirmation.** No
launch, upgrade, migration, settings-load, or background path may reach it.

## Contract Boundary

```
  PRODUCER                                          CONSUMER(S)
  ────────                                          ───────────
  AdvancedSettings$Server enrollment action         ServiceBase.getBackupImapStore()
    1. fetch leaf cert (no data trust)                resolveTlsPolicy(host, port):
    2. build EnrolledCertificate                        store.retrieve(host,port) present?
    3. display subj/issuer/SHA-256/expiry                 → PINNED_CERTIFICATE + cert  ──┐
    4. affirmative consent ("Trust this certificate")     → else SYSTEM_VALIDATED        │
    5. store.store(host, port, EnrolledCertificate)                                      │
       │                                                                                 ▼
       ▼                                              feeds CNTR-MODERNIZATION-001:
  ┌──────────────────────────┐                       TrustedSocketFactory selection
  │  PinnedCertStore          │◀── retrieve ──        (PinnedCertificateSocketFactory)
  │  host:port → cert         │
  │  SharedPreferences        │── retrieve ──▶        One-time-notice presentation layer
  │  "pinned_certs" MODE_PRIV │                         consumeTransportSecurityNotice()
  │  backup-excluded          │
  └──────────────────────────┘
       ▲
       └── clear(host,port) ── "Remove pinned certificate" action
       └── markTransportSecurityNoticePending() ── AuthPreferences.migrate() (downgraded cohort)
```

The producer **decides** trust (with the user). The consumer **reads** that decision; it never
re-decides trust and never writes the store. The migration path (`AuthPreferences.migrate()`)
may set the *notice* flag but is forbidden from writing the *certificate* store — it is not an
enrollment.

## Contract Definition

### Interface: PinnedCertStore

App-owned, package `com.zegoggles.smssync.mail`. Persists enrolled certificates keyed by
`host:port`. Backed by a dedicated `SharedPreferences` file `"pinned_certs"` opened
`MODE_PRIVATE`, mirroring the credential-segregation discipline of
`AuthPreferences.getCredentials()` (`AuthPreferences.java:221-226`, separate `"credentials"`
file). The file MUST be listed in the app's backup-exclusion descriptor (`fullBackupContent` /
auto-backup rules) so a pinned cert never travels silently to a new device.

```java
package com.zegoggles.smssync.mail;

public interface PinnedCertStore {

    /**
     * Enroll (or replace) the pinned certificate for a host:port.
     * PRECONDITION (contract invariant): callable ONLY from an affirmative user
     * enrollment confirmation. No launch/migration/background path may call this.
     * Overwrites any existing entry for the same key (re-pin after expiry).
     *
     * @param host non-null, non-empty, normalized lower-case hostname (no scheme, no path)
     * @param port 1..65535 (IMAP default 993)
     * @param certificate the user-verified enrolled certificate (non-null)
     */
    void store(String host, int port, EnrolledCertificate certificate);

    /**
     * @return the enrolled certificate for host:port, or null if none is pinned
     *         (caller then resolves to SYSTEM_VALIDATED). Read-only; side-effect free.
     */
    EnrolledCertificate retrieve(String host, int port);

    /** @return true iff a certificate is pinned for host:port. */
    boolean isPinned(String host, int port);

    /**
     * Remove the pinned certificate for host:port, reverting that host to
     * SYSTEM_VALIDATED. No-op if none pinned. Triggered ONLY by the user's
     * "Remove pinned certificate" action.
     */
    void clear(String host, int port);
}
```

**Key format (normative).** The storage key is the string `host + ":" + port`, with `host`
normalized to lower case (RFC-style host comparison; ASCII hostnames in this codebase). Port
is always explicit — the resolver MUST resolve the effective IMAP port (default 993) before
keying, so that a pin made against `imap.example.org:993` is found whether the saved
`SERVER_ADDRESS` carried an explicit port or not. Scoping is strict: a pin for one `host:port`
MUST NOT apply to any other host:port (a pinned self-hosted server never weakens the Gmail
993 connection).

### Data Shape: EnrolledCertificate

The value object that crosses the boundary. All five display fields are derived from the
single source of truth — the DER-encoded `X509Certificate` — and are what the enrollment UI
MUST present to the user before consent (DES-002 Decision 3 step 2).

```java
package com.zegoggles.smssync.mail;

public final class EnrolledCertificate {
    private final String  subject;          // X500 subject DN (CN + SANs surfaced for display)
    private final String  issuer;           // X500 issuer DN
    private final String  sha256Fingerprint;// SHA-256 over DER bytes, uppercase hex, colon-separated
    private final long    notBeforeEpochMs; // validity window start (cert NotBefore)
    private final long    notAfterEpochMs;  // validity window end / expiry (cert NotAfter)
    private final byte[]  derBytes;          // DER-encoded X509Certificate — the persisted truth
    // accessors only; immutable; equals/hashCode by sha256Fingerprint
}
```

| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `subject` | `String` | `X509Certificate.getSubjectX500Principal()` | Human-readable; CN + Subject Alternative Names surfaced for the user |
| `issuer` | `String` | `X509Certificate.getIssuerX500Principal()` | For a self-signed cert, equals `subject` |
| `sha256Fingerprint` | `String` | `SHA-256(derBytes)` | **Primary out-of-band verification identity.** Uppercase hex, colon-separated octets, e.g. `AB:CD:…`. Exactly 32 octets / 95 chars |
| `notBeforeEpochMs` | `long` | `X509Certificate.getNotBefore()` | Epoch millis UTC |
| `notAfterEpochMs` | `long` | `X509Certificate.getNotAfter()` | Epoch millis UTC; the **expiry** shown at enrollment |
| `derBytes` | `byte[]` | `X509Certificate.getEncoded()` | Persisted as Base64 in `"pinned_certs"`. The pinning check (CNTR-MODERNIZATION-001's `PinnedCertificateSocketFactory`) compares the presented leaf's SHA-256 against `sha256Fingerprint` and rejects on mismatch |

**Persistence encoding.** In the `"pinned_certs"` SharedPreferences file the value stored for
key `host:port` is `Base64(derBytes)` (no-wrap). The remaining display fields are
re-derivable from `derBytes` on `retrieve(...)`; an implementation MAY cache them but the DER
bytes are authoritative. Storing only the fingerprint is **non-conforming** — the full DER is
required so the socket factory can reconstruct and compare the certificate.

### Event: PinnedCertificateEnrolled

```
Event:       PinnedCertificateEnrolled  (logical; in-process, no bus)
Trigger:     user taps the affirmative "Trust this certificate" control in the
             enrollment confirmation dialog — and ONLY that control
Precondition: subject, issuer, sha256Fingerprint, and expiry have been displayed to the user
Effect:      PinnedCertStore.store(host, port, EnrolledCertificate);
             host's resolved TLS policy becomes PINNED_CERTIFICATE;
             settings UI shows persistent "Relaxed validation active — pinned certificate
             for {host}" indicator
Cancel:      any non-affirmative dismissal (Cancel / back / safe default) → store NOT written;
             policy remains SYSTEM_VALIDATED; no indicator
```

**Affirmative-consent requirements (normative).**
- The confirming control MUST be a deliberate, explicitly-labeled action ("Trust this
  certificate"), NOT a default-focused / default-positive button. The **safe choice
  (Cancel / do-not-trust) is the default**.
- Enrollment MUST NOT proceed without the four display fields having been shown.
- `store(...)` MUST be invoked exactly once per confirmed enrollment, with the same
  certificate that was displayed (no fetch-display / store-different race).

### Event: TransportSecurityNoticePending  (downgraded-user one-time notice)

```
Event:       TransportSecurityNoticePending  (logical; persisted boolean flag)
Flag key:    "transport_security_notice_shown" (default SharedPreferences), boolean,
             default false — analogous to the existing one-time
             "sms_default_package_change_seen" pattern (Preferences.java:247-253)
Set by:      AuthPreferences.migrate() (DES-002 Decision 6) for the affected cohort:
             users on legacy "+ssl"/"+tls" protocol, OR users carrying a stale
             SERVER_TRUST_ALL_CERTIFICATES=true that migrate() is clearing to false
Consumed by: the one-time-notice presentation layer at the next UI opportunity
Semantics:   shown at most once; on presentation the layer persists "shown"=true and
             never re-displays. Content explains: (a) the connection now uses certificate
             validation; (b) self-hosted/private-CA users may enroll a pinned cert via
             settings (pointing at the enrollment producer); (c) no action needed for
             Gmail / publicly-trusted servers
```

This event is the **only** thing `migrate()` is permitted to write across this boundary. It is
explicitly NOT an enrollment: `migrate()` MUST NOT call `PinnedCertStore.store(...)` nor set
`PINNED_CERTIFICATE` (DES-002 Decision 6; the negative test at DES-002 §"Pinned-cert requires
explicit action").

## Versioning

- **Current version:** v1
- **Breaking changes** (require a new contract version and consumer coordination): changing the
  `host:port` key format; changing the persisted value encoding away from `Base64(DER)`;
  removing any of the four mandatory display fields; weakening the affirmative-consent
  requirement; making any non-user path able to call `store(...)`.
- **Backward-compatible changes** (consumers unaffected): adding new read-only accessors to
  `EnrolledCertificate`; adding optional display metadata derivable from the DER; tightening
  validation (e.g., rejecting wildcard-only subjects) provided already-enrolled certs keep
  resolving.

## Validation Rules

Both sides MUST honor:

1. **Single write path.** `PinnedCertStore.store(...)` is reachable only from the affirmative
   enrollment confirmation. (Enforced by the DES-002 negative test enumerating
   `App.onCreate()`, `Preferences.migrate()`, `AuthPreferences.migrate()`, settings load.)
2. **Never automatic.** No launch / upgrade / migration / background / settings-load path may
   set `PINNED_CERTIFICATE` or write the store. Migration may set only the notice flag.
3. **Display-before-consent.** Subject, issuer, SHA-256 fingerprint, and expiry MUST be shown
   before the affirmative control is actionable.
4. **Strict host scoping.** A pin for `hostA:portA` MUST NOT influence trust for any other
   `host:port`. Resolution keys on the effective (explicit-or-default) port.
5. **DER is authoritative.** The persisted unit is the full DER certificate; the fingerprint
   is derived, not the stored primary.
6. **Read-only consumer.** The TLS policy resolver and the notice presenter MUST NOT mutate
   the certificate store. The notice presenter may only flip `transport_security_notice_shown`.
7. **Invariant — relaxed trust gateway.** The only way to reach a non-`SYSTEM_VALIDATED`
   factory in CNTR-MODERNIZATION-001 is a non-null `retrieve(host,port)`; the only way to make
   `retrieve` non-null is `store(...)`; the only caller of `store(...)` is enrollment consent.

## Error Handling

| Condition | Producer behavior | Consumer behavior |
|-----------|-------------------|-------------------|
| TLS fetch during enrollment fails (unreachable host, handshake error) | Surface a clear error; enroll nothing; policy stays `SYSTEM_VALIDATED` | n/a |
| User cancels / dismisses confirmation | `store(...)` not called | resolver returns `SYSTEM_VALIDATED` |
| `retrieve(host,port)` returns null | n/a | resolve `SYSTEM_VALIDATED` (validated TLS default) — never trust-all |
| Pinned cert expired at connect time | n/a (display expiry at enrollment) | `PinnedCertificateSocketFactory` (CNTR-001) throws `CertificateException`; backup fails closed; user re-enrolls via `store(...)` overwrite |
| Presented leaf SHA-256 ≠ pinned `sha256Fingerprint` | n/a | reject — `CertificateException`; fail closed (no silent fallback to system trust) |
| Corrupt / un-decodable persisted Base64 | n/a | treat as not-pinned → `SYSTEM_VALIDATED` (fail closed to the *stricter* posture, not to trust-all) |

Fail-closed is the rule throughout: any ambiguity resolves to the stricter
`SYSTEM_VALIDATED`, never to relaxed trust.

## Example Payloads

**Enrolled-certificate value object (logical view at the boundary):**

```json
{
  "host": "imap.selfhosted.example.org",
  "port": 993,
  "certificate": {
    "subject": "CN=imap.selfhosted.example.org, O=Example Self-Hosted, C=DE",
    "issuer":  "CN=imap.selfhosted.example.org, O=Example Self-Hosted, C=DE",
    "sha256Fingerprint": "3A:7B:E1:09:4F:2C:8D:55:AA:01:F4:6E:9C:12:B0:7D:88:23:5E:91:0A:CF:44:7B:E2:10:9D:33:6F:1C:A4:55",
    "notBeforeEpochMs": 1748736000000,
    "notAfterEpochMs":  1780272000000,
    "derBytesBase64":   "MIIDXTCCAkWgAwIBAgIJAL…(truncated DER)…q3hd0RXyBfFw=="
  }
}
```

**Persisted form in `pinned_certs` SharedPreferences (MODE_PRIVATE, backup-excluded):**

```
key:   "imap.selfhosted.example.org:993"
value: "MIIDXTCCAkWgAwIBAgIJAL…q3hd0RXyBfFw=="   // Base64(DER), no-wrap
```

**Downgraded-user one-time notice flag (default SharedPreferences):**

```
key:   "transport_security_notice_shown"
value: false   // set false→(pending) by migrate() for affected cohort; presenter flips to true after showing, never re-displays
```

## Dependencies

- **CNTR-MODERNIZATION-001** (TLS trust-policy → IMAP transport): consumes the resolution this
  contract produces. The resolver reads `PinnedCertStore.retrieve(host,port)` here, then
  CNTR-001 selects `PinnedCertificateSocketFactory` vs. `DefaultTrustedSocketFactory`. The
  `EnrolledCertificate.sha256Fingerprint` / `derBytes` defined here are the inputs to CNTR-001's
  `PinnedCertificateSocketFactory.checkServerTrusted()` comparison.
- **DES-MODERNIZATION-002** (Decisions 3, 5, 6): authoritative design; this contract formalizes
  its `CNTR-MODERNIZATION-PINENROLL-001` boundary.
- **REQ-MODERNIZATION-002** AC-4 (consensual pinned-cert path), AC-6 (one-time notice), AC-10
  (single legitimate writer).
- Precedent (not a code dependency): credential-segregation pattern
  `AuthPreferences.getCredentials()` (`AuthPreferences.java:221-226`); one-time-seen pattern
  `Preferences.java:247-253`.

## Notes

Draft body for CNTR-MODERNIZATION-002 (`CNTR-MODERNIZATION-PINENROLL-001` boundary); NOT finalized — frontmatter (status/title/producer/consumers/contract_type/related_*) is Artifact-Librarian-owned and untouched, and final approval gates story sprint-planning.
