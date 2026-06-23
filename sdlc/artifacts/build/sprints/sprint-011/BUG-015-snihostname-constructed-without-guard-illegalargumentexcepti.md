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
  - U-047
platforms: []
tags: []
id: BUG-015
title: SNIHostName constructed without guard — IllegalArgumentException on IP-literal/empty IMAP host (API 24+)
domain: build
origin: code-review
resolved_by: U-047
sprint: '000011'
---

# BUG-015: SNIHostName constructed without guard — IllegalArgumentException on IP-literal/empty host

## Description
The U-045 SNI fix added `setSniViaSSLParameters` in the vendored `DefaultTrustedSocketFactory`, which constructs `new SNIHostName(hostname)` unconditionally on API 24+. `javax.net.ssl.SNIHostName`'s constructor throws `IllegalArgumentException` when the hostname is empty or is not a valid DNS name — notably an **IP literal** (e.g. `192.168.1.10` or `[::1]`). Per RFC 6066, SNI should not be sent for IP-literal server addresses at all. For the common Gmail case (`imap.gmail.com`, a DNS name) this is fine, but a user who configures a custom IMAP server by IP address would hit an unhandled `IllegalArgumentException` during socket creation, breaking the connection. Flagged as code-review warning W-1 during U-045.

## Steps to Reproduce
1. Configure the IMAP server with an IP-literal host (e.g. a self-hosted IMAP server at `192.168.1.10`) on an API 24+ device.
2. Attempt a backup/restore (TLS connect).
3. Observe: `setSniViaSSLParameters` → `new SNIHostName("192.168.1.10")` throws `java.lang.IllegalArgumentException` (not a valid DNS name); the connect fails.

## Expected Behavior
SNI is set only for valid DNS hostnames. For IP-literal or empty/blank hosts, SNI is simply not set (skipped quietly) and the TLS connection proceeds normally — consistent with RFC 6066 (no SNI for IP literals).

## Actual Behavior
`new SNIHostName(hostname)` is called for any host; an IP-literal/empty host throws `IllegalArgumentException`, which is not caught in `setSniViaSSLParameters` and propagates out of socket creation.

## Evidence
- `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java:212-216` (`setSniViaSSLParameters`): `params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));` — no null/empty/IP-literal guard.
- `javax.net.ssl.SNIHostName(String)` Javadoc: throws `IllegalArgumentException` if the argument is not a valid hostname (includes IP literals and empty strings).
- Raised as W-1 in `sdlc/artifacts/build/sprints/sprint-010/U-045/review-code.md`.

## Acceptance Criteria
- [ ] AC-1: `setSniViaSSLParameters` (and the `setSniHost` path) no longer throws for an IP-literal or empty/blank hostname — SNI is skipped for such hosts and the TLS connection still succeeds.
- [ ] AC-2: For a normal DNS hostname (e.g. `imap.gmail.com`), SNI is still set exactly as before (no behavior change for the Gmail path); cert validation / pinned-cert / TLS hardening unchanged.
- [ ] AC-3: Unit tests cover: (a) DNS host → `setSSLParameters` called with the `SNIHostName`; (b) IP-literal host (IPv4 and IPv6) → no throw, SNI not set; (c) empty/blank host → no throw, SNI not set.
- [ ] AC-4: Change confined to the vendored module (`DefaultTrustedSocketFactory`); build green (assembleDebug, testDebugUnitTest, jacoco ≥70%).

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `k9mail-vendored/.../mail/ssl/DefaultTrustedSocketFactory.java:212-216` (`setSniViaSSLParameters`) | `new SNIHostName(hostname)` unguarded → IllegalArgumentException on IP-literal/empty host | guard: skip SNI when hostname is null/blank or an IP literal (e.g. detect via `InetAddresses`/regex, or wrap the `SNIHostName` construction in a try/catch IllegalArgumentException and skip on failure); log at debug |

## Existing Behavior to Preserve
- DNS-host SNI behavior (Gmail) unchanged.
- TLS handshake, cert validation, hostname verification, pinned-cert path (REQ-MODERNIZATION-002) unchanged.
- No new ERROR-level logging (BUG-012 fix preserved).

## Verification Steps
1. Unit test the three host cases (DNS / IPv4 / IPv6 / empty) on `setSniViaSSLParameters` with a mock SSLSocket.
2. Confirm a normal Gmail backup still works (SNI still set for DNS) — covered by the existing U-045 on-device check; no new device test required for the IP case (no IP IMAP server available).

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context
- Follow-on hardening to U-045/BUG-012. Low severity: only affects users with an IP-literal IMAP host on API 24+; the default Gmail path is unaffected. Prefer skipping SNI for IP literals (RFC 6066) rather than catching-and-ignoring, but a guarded try/catch is acceptable. Keep confined to the vendored module.

## Supporting Documentation
- `sdlc/artifacts/build/sprints/sprint-010/U-045/review-code.md` (W-1); U-045 (BUG-012 fix); RFC 6066 §3 (no SNI for literal addresses).

## Notes
Low severity, logged from the U-045 code review. Fix scheduled in sprint-011.
