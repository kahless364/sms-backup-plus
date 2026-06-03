---
id: REQ-MODERNIZATION-002
artifact_type: requirement
title: "Eliminate Transport-Security MITM Exposure"
status: approved
domain: modernization
type: nfr
priority: critical
epic: EPIC-MODERNIZATION-001
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-001
  - REQ-MODERNIZATION-003
related_stories: []
change_records: []
notes: |
  Migration unit: MU-003 (Transport Security Hardening). Phase 1 - runs in parallel to
  Phase 0 from day 1; independent of the SDK gate (REQ-MODERNIZATION-001).
  Decision gate / milestone: Gate G1 / Milestone M1.
  Finding refs: SEC-001, ARCH-003, ARCH-008.
  Security classification: CWE-295, CWE-312; OWASP MASVS-NETWORK-1.
  requirement_source: authored. created: 2026-05-30.
  Source assessment: 20260529-modernization.
---

# REQ-MODERNIZATION-002 — Eliminate Transport-Security MITM Exposure

## Description

The SMS Backup+ application must eliminate every code path that accepts an unvalidated TLS certificate during IMAP communication, replace the silent security-downgrade migration in `AuthPreferences.migrate()` with a non-silent, consent-based behavior, and provide a consensual user-initiated pinned-certificate path for self-hosted IMAP users with private CAs. No upgrade, migration, or default configuration must reduce any user's transport-security posture without their explicit, informed consent. These changes are independent of the SDK uplift and must begin in parallel from day one of the modernization.

## Context

### Active Vulnerability: AllTrustedSocketFactory (CWE-295)

`mail/AllTrustedSocketFactory.java:42-57` declares an inner `InsecureX509TrustManager` whose `checkServerTrusted()` body is completely empty and whose `getAcceptedIssuers()` returns `null`. When this factory is active, the TLS handshake accepts any certificate presented by any peer — forged, expired, self-signed, or from an unknown CA — with no validation whatsoever. The class is annotated `@SuppressLint("TrustAllX509TrustManager")` at line 20, an explicit acknowledgment that the Android platform linter identifies this construct as dangerous.

The factory is selected at `mail/BackupImapStore.java:60-62` via a ternary:

```java
// BackupImapStore.java:59-62
public BackupImapStore(final Context context, final String uri,
                       boolean trustAllCertificates) throws MessagingException {
    super(new BackupStoreConfig(uri),
        trustAllCertificates ? AllTrustedSocketFactory.INSTANCE : new DefaultTrustedSocketFactory(context),
```

When `trustAllCertificates` is `true`, `AllTrustedSocketFactory.INSTANCE` is used. There is no code path that partially validates; the flag is binary, and the trust-all branch performs no validation at all.

### Active Vulnerability: Silent Security-Downgrade Migration (ARCH-008)

`preferences/AuthPreferences.migrate()` (lines 276–288) runs on every app launch via `App.onCreate()`. For any user whose stored `SERVER_PROTOCOL` preference value is `+ssl` or `+tls`, it unconditionally writes `SERVER_TRUST_ALL_CERTIFICATES = true` without displaying any notice, requesting any consent, or emitting any log entry visible to the user:

```java
// AuthPreferences.java:276-288
void migrate() {
    if (useXOAuth()) {
        return;
    }
    if ("+ssl".equals(getServerProtocol()) ||
        "+tls".equals(getServerProtocol())) {
        preferences.edit()
            .putBoolean(SERVER_TRUST_ALL_CERTIFICATES, true)   // silent security downgrade
            .putString(SERVER_PROTOCOL, getServerProtocol()+"+")
            .commit();
    }
}
```

A user who originally configured the app with `+ssl` — meaning validated TLS, the secure choice — is silently transitioned to `+ssl+` with certificate validation disabled on the next launch after an upgrade. The user made no deliberate choice to relax validation. The migration converted a secure configuration into a maximally insecure one without their knowledge. This is a security-downgrade event masquerading as a migration.

### Exploit Path (End-to-End)

A user who originally configured `+ssl` installs an app update. On next launch, `App.onCreate()` calls `preferences.migrate()`. The method matches `+ssl`, writes `SERVER_TRUST_ALL_CERTIFICATES = true`, and commits. No notice appears.

The user opens their laptop at a coffee shop. An on-path attacker on the same Wi-Fi intercepts the IMAP TCP connection to port 993. The attacker's machine presents a self-signed certificate asserting `imap.gmail.com` as the Common Name. `BackupImapStore` reads `isTrustAllCertificates()` — returns `true` — selects `AllTrustedSocketFactory.INSTANCE`. The TLS handshake calls `InsecureX509TrustManager.checkServerTrusted()`, which returns immediately. The handshake completes. The attacker's TLS terminator sits between the app and Gmail.

The attacker now reads the complete backup stream: every SMS and MMS thread with every contact, complete call log, and the Gmail app password or OAuth2 token used in the IMAP `AUTH` command. The attacker has a long-lived Gmail credential that grants full mailbox access. The user has no indication that anything occurred. The attack requires no malware, no physical access, no server compromise — only adjacency on the same network segment.

### Data Sensitivity

This application's backup stream contains the device's complete SMS/MMS corpus and call-log history — the totality of the user's private communications — plus a Gmail full-mailbox credential. There is no data class on a mobile device with higher blast-radius on confidentiality and integrity breach. An MITM attack here is not bounded to one transaction or one session; it yields a permanent historical record and a durable credential.

### Independence from SDK Uplift

Neither the `AllTrustedSocketFactory` deletion nor the `migrate()` rewrite has any dependency on raising `targetSdkVersion`. Both files use only APIs available since API 1 (`javax.net.ssl`, `SharedPreferences`). They can be fixed, tested, and shipped independently as a Phase 1 unit that runs in parallel with the SDK gate. Waiting for the SDK gate to clear before beginning this work leaves an active MITM exposure in production for the entire Phase 0 duration — and the SDK uplift release itself would silently degrade additional `+ssl`/`+tls` users on the day it ships via the unchanged `migrate()` logic.

### Relationship to REQ-MODERNIZATION-003 (Test-Harness and Coverage Gate)

The `AuthPreferences.migrate()` characterization test required by AC-5 is backfilled as part of REQ-MODERNIZATION-003. That test pins the current behavior of `migrate()` before this requirement's rewrite modifies it. It is a Feathers-style characterization test (Working Effectively with Legacy Code, Ch. 2): it does not assert desired behavior; it asserts current behavior, locking it in place so that the subsequent rewrite cannot silently change other call paths. The characterization test must be committed and green on the main branch before any production change to `migrate()` is merged. This is a hard sequencing constraint — not advisory.

## Acceptance Criteria

**AC-1 — AllTrustedSocketFactory is deleted from the production source tree**

Given the production source directory `app/src/main/java/`,
when a developer searches for `AllTrustedSocketFactory` by file name or by import statement,
then the file `mail/AllTrustedSocketFactory.java` does not exist, no production Java file imports `AllTrustedSocketFactory`, and `AllTrustedSocketFactory.INSTANCE` is not referenced from any reachable production call site.

**AC-2 — No production X509TrustManager has an empty checkServerTrusted() body**

Given all `X509TrustManager` implementations present in the production source tree (`app/src/main/java/`),
when each implementation's `checkServerTrusted(X509Certificate[] chain, String authType)` method is read,
then no implementation has an empty body, a body containing only a comment, or a body that unconditionally returns without examining the chain; every implementation either delegates to a platform-validated trust chain or validates the chain against a specific user-pinned certificate.

**AC-3 — Every IMAP TLS connection uses validated TLS by default**

Given any code path through which `BackupImapStore` (or any replacement class) initiates an IMAP TLS connection, and given that the user has not explicitly completed the pinned-certificate enrollment flow described in AC-4,
when the TLS handshake is initiated,
then the `TrustedSocketFactory` active for that connection is the platform-validated factory (`DefaultTrustedSocketFactory` or an equivalent that delegates chain validation to the Android `TrustManager` framework); no factory that bypasses or weakens certificate validation is active unless the user has explicitly enrolled a pinned certificate as described in AC-4.

**AC-4 — A consensual, user-initiated pinned-certificate path exists for self-hosted IMAP**

Given a user who operates a self-hosted IMAP server whose certificate was issued by a private CA not trusted by the Android system certificate store,
when the user navigates to the IMAP server settings and explicitly initiates the certificate enrollment flow,
then the app:
- Displays the certificate subject, issuer, SHA-256 fingerprint, and expiry date before requesting consent.
- Requires an affirmative, deliberate user action (a confirmed button press or equivalent) to enroll the certificate.
- Stores the pinned certificate in persistent storage keyed to the specific server host and port.
- Selects a certificate-pinning factory only for connections to that specific host using only the enrolled certificate.
- Never activates this path automatically during app launch, upgrade, migration, or any non-user-initiated event.
- Displays a persistent label in the settings UI indicating that relaxed validation is active for that server, so long as the pinned certificate is enrolled.

**AC-5 — migrate() never writes SERVER_TRUST_ALL_CERTIFICATES = true**

Given the rewritten `AuthPreferences.migrate()` method and a device with any combination of stored `SERVER_PROTOCOL` values (`+ssl`, `+tls`, `ssl`, `tls`, or any custom value),
when `migrate()` executes,
then `SERVER_TRUST_ALL_CERTIFICATES` is not written as `true` by `migrate()` under any branch, condition, or code path; the method may update `SERVER_PROTOCOL` to a standardized form but must not alter the trust-all flag from its default `false` state unless the user has separately and explicitly enrolled a pinned certificate via the flow in AC-4.

**AC-6 — migrate() surfaces a one-time notice for users previously affected by the silent downgrade**

Given a user whose stored `SERVER_PROTOCOL` at the time of this app update was `+ssl` or `+tls` (values that would previously have triggered the silent trust-all write),
when `migrate()` executes for the first time after this requirement is deployed,
then a one-time user-visible notice is displayed on screen explaining that: (a) their connection will now use certificate validation, (b) if they connect to a server with a self-signed or private-CA certificate, they can enroll it via the pinned-certificate settings flow, and (c) no action is required for Gmail or other servers with publicly trusted certificates; this notice is stored as shown in persistent preferences and is not displayed again on subsequent launches.

**AC-7 — Characterization test for migrate() is green before any production change to migrate() is merged (Gate G1 / Milestone M1)**

Given the characterization test backfilled under REQ-MODERNIZATION-003 in `preferences/AuthPreferencesTest.java`,
when the test suite is executed against the unmodified production code,
then the test cases for `migrate()` pass, confirming that the pre-change behavior is pinned; this test must be green on the main branch and included in the CI run before the first commit modifying `migrate()` is merged; a pull request that modifies `migrate()` without the characterization test already present and green is not acceptable.

**AC-8 — BackupImapStoreTest asserts the factory type after the change**

Given the test file `mail/BackupImapStoreTest.java`,
when a test case constructs a `BackupImapStore` (or replacement) with a configuration that previously would have selected `AllTrustedSocketFactory.INSTANCE`,
then the test asserts that the active `TrustedSocketFactory` is not an instance of `AllTrustedSocketFactory` and does not contain an `InsecureX509TrustManager` in its trust-manager chain; this assertion must be present and passing before `AllTrustedSocketFactory.java` is deleted.

**AC-9 — Existing validated-TLS users experience no behavior change**

Given a user whose stored configuration uses a protocol that does not match `+ssl` or `+tls` (i.e., users already on validated TLS, users using OAuth2, or users with no prior migration trigger),
when `migrate()` executes,
then the user's `SERVER_TRUST_ALL_CERTIFICATES` preference is not altered, their server address and port are not altered, and their next IMAP backup or restore runs without any change in behavior; the existing `BackupImapStoreTest` suite passes without regressions for these cases.

**AC-10 — SERVER_TRUST_ALL_CERTIFICATES can only be set to true via explicit user enrollment**

Given all production Java source files that write to the `SERVER_TRUST_ALL_CERTIFICATES` preference key,
when the implementation is complete and a reviewer audits every write site,
then the only code path that may write `true` for this key is the user-initiated pinned-certificate enrollment flow (AC-4); `migrate()`, `App.onCreate()`, any initialization path, and any default-value assignment must not write `true` for this key under any condition.

## Rationale

SMS Backup+ holds the device's entire SMS, MMS, and call-log corpus alongside a Gmail full-mailbox credential. A single successful MITM attack on this app yields a complete, timestamped record of every private communication the user has had on that device plus the ability to access, read, modify, or permanently delete the user's Gmail mailbox. There is no data class on a mobile device with higher blast-radius on confidentiality and integrity breach.

The trust-all TLS path is not a theoretical or low-likelihood risk. It is an active, exploitable vulnerability requiring only that the attacker be on the same network as the target — a precondition satisfied by any shared Wi-Fi environment. The silent `migrate()` logic means users who have never knowingly opted into relaxed certificate validation may have been exposed on every backup since their last upgrade without their knowledge. This is not a hardening measure; it is closure of an open attack surface that the app itself created through the migration.

The Android platform's own lint tooling identifies `InsecureX509TrustManager` as a critical security defect (hence the `@SuppressLint("TrustAllX509TrustManager")` suppression at `AllTrustedSocketFactory.java:20`). The OWASP MASVS-NETWORK-1 control explicitly prohibits disabling certificate validation in production mobile applications. CWE-295 is present in the NIST National Vulnerability Database's most-cited vulnerability list for mobile applications. The gap analysis rates this as the highest-priority gap on the security axis (SEC-001, Critical, independent of the SDK gate).

The remediation preserves the legitimate self-hosted IMAP use case via a consensual, user-controlled pinned-cert path. The risk does not come from supporting private certificates; it comes from accepting any certificate without user knowledge or consent.

## Constraints

1. **Self-hosted IMAP users must not lose functionality.** Users who operate their own IMAP server with a private CA or self-signed certificate must retain a viable configuration path after this change. The consensual pinned-cert opt-in (AC-4) is the required mechanism. The silent trust-all flag is not an acceptable substitute for any user.

2. **The migrate() characterization test must precede the migrate() rewrite.** The characterization test for `AuthPreferences.migrate()` (AC-7) must be merged and green on the main branch before any implementation changes to `migrate()` are committed. This is a hard process constraint. The test is the safety net that makes the rewrite verifiable without risk of silent behavioral regression on adjacent code paths.

3. **Coordinate on BackupImapStore.java with MU-008.** `mail/BackupImapStore.java` is touched by both this unit (MU-003, transport security hardening) and MU-008 (Mail ACL and k-9 unpin). Per the migration unit shared-file allocation, MU-003 must land first: the transport is validated here, and MU-008 wraps the Anti-Corruption Layer around the already-validated transport. Concurrent editing of this file across both units, or landing MU-008 before MU-003, is a constraint violation.

4. **Coordinate on AuthPreferences.java with MU-004 and MU-007.** `preferences/AuthPreferences.java` is touched by MU-003 (migrate() rewrite, lines 276–288), MU-004 (getCredentials() seam, lines 221–226), and MU-007 (Hilt DI injection). Edits must be sequenced MU-003 first, then MU-004, then MU-007.

5. **No dependency on REQ-MODERNIZATION-001.** This requirement must begin immediately and in parallel with the SDK uplift. Treating it as work that waits for Phase 0 completion is a process defect. The critical path for this requirement runs through REQ-MODERNIZATION-003's migrate() backfill only, not through any build-system or SDK change.

6. **Preserved Core must not be altered.** The domain state machine (`service/state/`), the `DataType` type-object, the exception hierarchy, and the message-conversion components defined under MU-000 must not be modified as a side effect of this requirement.

## Notes

### Traceability

| Reference | Source |
|-----------|--------|
| SEC-001 | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/gaps.md` — Critical, Technology/Security, CWE-295/CWE-312, OWASP MASVS-NETWORK-1 |
| ARCH-003 (gaps.md) / ARCH-007 (ilities) | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/architecture/ilities-assessment.md` — Critical, trust-all TLS certificate validation disabled |
| ARCH-008 | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/architecture/ilities-assessment.md` — High, silent security-downgrade migration |
| MU-003 | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md` — Transport Security Hardening, Phase 1 (parallel to Phase 0) |
| Phase 0 promotion | `sdlc/analysis/20260529-modernization/promoted/phase0-shippable-secure-context.md` — Finding 2 |
| CWE-295 | https://cwe.mitre.org/data/definitions/295.html |
| CWE-312 | https://cwe.mitre.org/data/definitions/312.html |
| OWASP MASVS-NETWORK-1 | https://mas.owasp.org/MASVS/controls/MASVS-NETWORK-1/ |

### Code Locations (paths relative to repo root)

| File | Lines | Relevance |
|------|-------|-----------|
| `app/src/main/java/com/zegoggles/smssync/mail/AllTrustedSocketFactory.java` | 42–57 | `InsecureX509TrustManager`: empty `checkServerTrusted()`, null `getAcceptedIssuers()` — deleted by this requirement |
| `app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java` | 59–62 | Socket-factory selection: `trustAllCertificates ? AllTrustedSocketFactory.INSTANCE : new DefaultTrustedSocketFactory(context)` — modified by this requirement |
| `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` | 276–288 | `migrate()` — unconditionally writes `SERVER_TRUST_ALL_CERTIFICATES = true` for `+ssl`/`+tls` users — rewritten by this requirement |
| `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` | 196 | `isTrustAllCertificates()` — gating predicate; reviewed for correct default behavior after rewrite |
| `app/src/test/java/com/zegoggles/smssync/preferences/AuthPreferencesTest.java` | — | `migrate()` characterization test — backfilled by REQ-MODERNIZATION-003, gates this requirement |
| `app/src/test/java/com/zegoggles/smssync/mail/BackupImapStoreTest.java` | 55 | Factory-type assertion — updated by this requirement |

### Related Requirements

- **REQ-MODERNIZATION-001** — Build and Platform Uplift (SDK gate). This requirement is independent and runs in parallel; it does not wait for REQ-MODERNIZATION-001.
- **REQ-MODERNIZATION-003** — Test-Harness and Coverage Gate. The `migrate()` characterization test is a co-requisite output of REQ-MODERNIZATION-003 and is a prerequisite for AC-7 of this requirement. The two may proceed in parallel; this requirement's production changes are not merged until the characterization test exists and is green.

### Decision Gate and Milestone

This requirement is the primary gating deliverable for **Gate G1 / Milestone M1** ("no code path accepts an unvalidated cert; `migrate()` proven non-downgrading by test"). Gate G1 is satisfied when all acceptance criteria above are met and CI is green. It is a co-gate alongside REQ-MODERNIZATION-001 (Gate G0) and REQ-MODERNIZATION-003 (Gate G2). All three gates must be passed before Phase 2 substrate-swap work may begin.

### Implementation Sequence Within This Requirement

The implementation sequence within MU-003 is:

1. REQ-MODERNIZATION-003 backfills the `AuthPreferences.migrate()` characterization test in `AuthPreferencesTest.java`. (Must be merged and green before step 2.)
2. Rewrite `migrate()` to never write `SERVER_TRUST_ALL_CERTIFICATES = true`; implement the one-time notice for legacy `+ssl`/`+tls` users.
3. Implement the pinned-certificate opt-in enrollment flow and the corresponding certificate-pinning `TrustedSocketFactory`.
4. Remove the `trustAllCertificates ? AllTrustedSocketFactory.INSTANCE : ...` branch from `BackupImapStore`; replace with unconditional use of the validated factory (with pinned-cert factory available only after user enrollment).
5. Update `BackupImapStoreTest` to assert the validated/pinned factory is selected and `AllTrustedSocketFactory` is never selected.
6. Delete `AllTrustedSocketFactory.java`.

Steps 3–6 may be sequenced in any order relative to each other but all must be complete before the unit is considered done. Step 2 must not be merged before step 1 is merged and green.
