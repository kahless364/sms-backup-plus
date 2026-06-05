---
type: bug
status: done
artifact_type: user-story
priority: low
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
id: U-047
title: Guard SNIHostName against IP-literal/empty IMAP host (fix BUG-015)
pipeline: ''
domain: modernization
resolution: done
requirement_source: bug:BUG-015
sprint: '000011'
---

# U-047: Guard SNIHostName against IP-literal/empty IMAP host

## Story
As a user backing up to a custom IMAP server addressed by IP,
I want the TLS connection to skip SNI for non-DNS hosts instead of crashing,
So that backups/restores work regardless of whether the server is a hostname or an IP.

## Source
Fixes **BUG-015** (low) — `sdlc/artifacts/stories/BUG-015-snihostname-constructed-without-guard-illegalargumentexcepti.md`. Follow-on hardening to U-045/BUG-012.

## Acceptance Criteria
- [ ] AC-1: `setSniViaSSLParameters` (and the `setSniHost` path) no longer throws for an IP-literal (IPv4/IPv6) or empty/blank hostname — SNI is skipped for such hosts (per RFC 6066) and the TLS connection still proceeds.
- [ ] AC-2: For a normal DNS hostname (e.g. `imap.gmail.com`), SNI is still set exactly as before via `SSLParameters.setServerNames(new SNIHostName(host))` — no behavior change for the Gmail path; cert validation / pinned-cert / TLS hardening untouched.
- [ ] AC-3: Unit tests: (a) DNS host → `setSSLParameters` called with the `SNIHostName`; (b) IPv4 literal → no throw, `setServerNames` not called with a SNIHostName; (c) IPv6 literal → no throw; (d) empty/blank host → no throw, SNI skipped.
- [ ] AC-4: Change confined to the vendored module (`DefaultTrustedSocketFactory`); no transport/`service.*` change; build green (assembleDebug, testDebugUnitTest, jacoco per-package LINE ≥70%). The BUG-012 fix (no ERROR log noise) is preserved.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java:212-216` (`setSniViaSSLParameters`) | `new SNIHostName(hostname)` unguarded → `IllegalArgumentException` on IP-literal/empty host | guard before constructing `SNIHostName`: if hostname is null/blank or an IP literal, skip setting SNI (return without setting server names); only set SNI for valid DNS names |
| `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java` | covers DNS-set + reflective fallback | add IPv4/IPv6/empty cases (no throw, SNI skipped) |

## Existing Behavior to Preserve
- DNS-host SNI behavior (Gmail) unchanged — SNI still set for `imap.gmail.com`.
- TLS handshake, cert validation, hostname verification, pinned-cert path (REQ-MODERNIZATION-002 / CNTR-MODERNIZATION-001) unchanged.
- BUG-012 fix: no ERROR-level log noise; reflective fallback still DEBUG-only.

## Verification Steps
1. Unit-test `setSniViaSSLParameters` (or the host-detection helper) with DNS / IPv4 / IPv6 / empty hosts using a mock SSLSocket: DNS → `setSSLParameters` with SNIHostName; IP/empty → no throw and no SNIHostName set.
2. Regression: confirm a normal Gmail backup still works (SNI set for DNS) — covered by the prior U-045 on-device check; no IP IMAP server available for a device test, so the IP path is unit-tested only.

## Technical Context
- Detect IP literals robustly: prefer a check that doesn't do DNS resolution — e.g. `android.net.InetAddresses.isNumericAddress(host)` (API 29+) with a small fallback, or a guarded `try { new SNIHostName(host) } catch (IllegalArgumentException) { skip }`. The try/catch approach is the simplest and covers all invalid-name cases (IP literals, empty, malformed) without platform-version branching — acceptable here. Whatever the approach, do not log at ERROR (preserve BUG-012). Keep the change minimal and confined to the vendored `DefaultTrustedSocketFactory`.

## Supporting Documentation
- `sdlc/artifacts/build/sprints/sprint-010/U-045/review-code.md` (W-1); `sdlc/artifacts/stories/BUG-015-...md`; U-045 (BUG-012 fix); RFC 6066 §3.

## Notes
Single-story sprint (sprint-011). Low severity hardening; default Gmail path unaffected. Touches only the vendored factory + its SNI test.
