---
type: bug
status: done
artifact_type: bug
severity: low
priority: low
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-045
platforms: []
tags: []
id: BUG-012
title: conscrypt Java8EngineSocket.setHostname NoSuchMethodException in vendored k-9 DefaultTrustedSocketFactory (SNI) on API 35+
domain: build
origin: qa
resolved_by: U-045
sprint: '000010'
---

# BUG-012: conscrypt Java8EngineSocket.setHostname NoSuchMethodException (SNI) in vendored k-9

## Description
During every IMAP connect, the vendored k-9 `DefaultTrustedSocketFactory` uses reflection to set the TLS SNI hostname on the SSL socket via `setHostname(String)`. On Android API 35+ (observed on emulator API 37), the underlying conscrypt socket class is `com.android.org.conscrypt.Java8EngineSocket`, which does not expose a `setHostname(String)` method, so the reflective lookup throws `java.lang.NoSuchMethodException`. The exception is caught (the connection proceeds without SNI and backups still succeed against Gmail), but it is logged at ERROR level on every connection attempt, producing noisy, alarming stack traces in the sync log/logcat.

## Steps to Reproduce
1. Run any backup on an emulator/device with API 35+ (conscrypt Java8EngineSocket).
2. Watch logcat for tag `k9` during the IMAP connect.
3. Observe: `java.lang.NoSuchMethodException: com.android.org.conscrypt.Java8EngineSocket.setHostname [class java.lang.String]` logged at ERROR, repeated per connection attempt.

## Expected Behavior
SNI is set when the platform supports it; when it is not available, the factory degrades gracefully and logs at most a single low-level (debug) note — no repeated ERROR-level stack traces. Backups continue to work either way.

## Actual Behavior
A full `NoSuchMethodException` stack trace is logged at ERROR on every connect (multiple times per backup), even though the condition is benign and the connection succeeds.

## Evidence
- Live logcat (emulator-5554, API 37, live Gmail backup, 2026-06-05):
  ```
  E k9: java.lang.NoSuchMethodException: com.android.org.conscrypt.Java8EngineSocket.setHostname [class java.lang.String]
  E k9:   at com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory.createSocket(DefaultTrustedSocketFactory.java:175)
  E k9:   at com.zegoggles.smssync.mail.transport.K9MailTransport$BackupImapStoreDelegate.createAndOpenFolder(K9MailTransport.java:538)
  E k9:   at com.fsck.k9.mail.store.imap.ImapConnection.connectToAddress(ImapConnection.java:209)
  ```
- The backup ultimately succeeds (`backup complete, backedUp=1`), confirming the error is non-fatal.

## Acceptance Criteria
- [ ] AC-1: On API 35+, an IMAP connect no longer logs a `NoSuchMethodException` ERROR for `setHostname`; SNI is either set via a supported API or skipped quietly (single debug-level note at most).
- [ ] AC-2: TLS still negotiates correctly and backups/restores continue to work on API 21..35+ (no SNI regression where it previously worked; certificate validation and cert-pinning unaffected).

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `k9mail-vendored/.../mail/ssl/DefaultTrustedSocketFactory.java:~175` (`createSocket`) | reflective `setHostname(String)` lookup throws + logs ERROR on conscrypt Java8EngineSocket (API 35+) | detect the unsupported method and skip/route to a supported SNI API; log at debug, not error |

## Existing Behavior to Preserve
- TLS negotiation, certificate validation, and the pinned-certificate path (REQ-MODERNIZATION-002 / CNTR-MODERNIZATION-001) must be unaffected.
- SNI must still be set on platforms where the supported method exists.

## Verification Steps
1. Backup on API 37 emulator; confirm no `NoSuchMethodException` ERROR in logcat and backup succeeds.
2. (If feasible) confirm on a lower API where `setHostname` exists that SNI is still applied.

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context
- Pre-existing in the vendored k-9 (`:k9mail-vendored`, U-027), surfaced during sprint-008 live testing. The reflective SNI shim predates the conscrypt class rename. Non-fatal; cosmetic/log-noise severity. Any change is confined to the vendored module's `DefaultTrustedSocketFactory`.

## Supporting Documentation
- sprint-008 live testing 2026-06-05; related stories U-007/U-008 (TLS), U-027 (vendored k-9).

## Notes
Low severity (log noise only; backups work). Logged during sprint-008 guided exploratory testing. Fix deferred (per user: log only).
