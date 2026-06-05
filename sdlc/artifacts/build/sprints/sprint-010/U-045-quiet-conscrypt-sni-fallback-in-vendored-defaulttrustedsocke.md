---
type: bug
status: planned
artifact_type: user-story
priority: high
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-045
title: Quiet conscrypt SNI fallback in vendored DefaultTrustedSocketFactory (fix BUG-012)
pipeline: ''
domain: modernization
requirement_source: bug:BUG-012
sprint: '000010'
---

# U-045: Quiet conscrypt SNI fallback in vendored DefaultTrustedSocketFactory

## Story
As an SMS Backup+ user on Android 15 / API 35+,
I want the IMAP TLS connection to set SNI without spamming the log with errors,
So that the sync log isn't full of alarming `NoSuchMethodException` stack traces on every connect.

## Source
Fixes **BUG-012** (low) — `sdlc/artifacts/stories/BUG-012-conscrypt-java8enginesocketsethostname-nosuchmethodexception.md`.

## Acceptance Criteria
- [ ] AC-1: On API 35+, an IMAP connect no longer logs a `NoSuchMethodException` ERROR for the reflective `setHostname` SNI call. SNI is set via the supported public API where available (`SSLParameters.setServerNames(new SNIHostName(host))`, API 24+), and where it cannot be set the code degrades quietly (at most a single debug-level note — no ERROR, no stack trace).
- [ ] AC-2: TLS still negotiates correctly and backups/restores continue to work on the supported range (API 21..35+). Certificate validation and the pinned-certificate path (`PinnedCertificateSocketFactory`) are unaffected — no trust/SNI regression where SNI previously worked.
- [ ] AC-3: The change is confined to the vendored `:k9mail-vendored` module (`DefaultTrustedSocketFactory`); no `service.*` / transport API changes; the `:k9mail-vendored` module's own build/tests stay green.
- [ ] AC-4: Build green (assembleDebug, testDebugUnitTest, jacoco per-package LINE ≥70%). A unit test covers the fallback path where the legacy reflective method is absent (no exception thrown/logged at error). On-device: a backup on API 37 produces no `NoSuchMethodException` ERROR in logcat and still succeeds.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` (~line 175, `createSocket`) | reflectively calls `setHostname(String)` on the SSL socket; on conscrypt `Java8EngineSocket` (API 35+) the method is absent → `NoSuchMethodException` logged at ERROR every connect | set SNI via public `SSLParameters.setServerNames(...)` when supported; keep/guard the reflective path for older platforms; on absence, log at debug (no ERROR stack trace) |

## Existing Behavior to Preserve
- TLS handshake, hostname/cert verification, and the pinned-certificate flow (REQ-MODERNIZATION-002 / CNTR-MODERNIZATION-001) — unchanged.
- SNI is still actually set on platforms where it was working (don't silently drop SNI everywhere — prefer the public API).
- No change to the transport (`K9MailTransport`) or `service.*`; ACL boundary intact.

## Verification Steps
1. AC-1/AC-4 (on-device): run a backup on emulator-5554 (API 37); `adb logcat | grep -i "NoSuchMethodException\|setHostname"` → none; backup succeeds.
2. AC-2: confirm backup/restore still work (TLS OK); confirm pinned-cert path unaffected.
3. Unit test the SNI-set / fallback branch.

## Technical Context
- Pre-existing reflective SNI shim in vendored k-9 predates the conscrypt `Java8EngineSocket` rename. The public SNI API (`SSLSocket.getSSLParameters().setServerNames(List.of(new SNIHostName(host)))` + `setSSLParameters`) is available API 24+ (guard the call; minSdk is 21). Keep the change minimal and within the vendored module.

## Supporting Documentation
- `sdlc/artifacts/stories/BUG-012-conscrypt-java8enginesocketsethostname-nosuchmethodexception.md`
- Related: U-007/U-008 (TLS), U-027 (vendored k-9).

## Notes
Low severity (log noise). Disjoint file set from U-046 (MainActivity/layout) — parallel-eligible.
