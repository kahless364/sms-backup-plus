---
artifact_type: implementation-log
story_id: U-047
verdict: PASS
agent: Developer
timestamp: "2026-06-05"
files_changed: 2
files_created: 2
tests_added: 6
tests_passing: 672
---

# Implementation Log: U-047

## Summary

Fixed BUG-015: `DefaultTrustedSocketFactory.setSniViaSSLParameters` now guards against
IP-literal (IPv4/IPv6) and empty/blank hostnames before constructing `SNIHostName`. Per
RFC 6066 §3, SNI must not be sent for IP literals; previously the unguarded `new
SNIHostName(hostname)` call would throw `IllegalArgumentException` and crash the TLS
connection setup for any user whose IMAP server is addressed by IP.

The fix uses a two-layer approach: (1) an explicit syntactic `isIpOrBlankHostname()`
helper that detects IPv4 dotted-decimal and IPv6 colon-notation without DNS resolution,
returning early before the `SNIHostName` constructor is reached; (2) a `try/catch
IllegalArgumentException` safety net wrapping the `SNIHostName` construction for any
malformed hostnames that slip through. All new log calls are at DEBUG level, preserving
the BUG-012 fix.

## Files Modified

### `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`

**Lines 207-246 (after edit):** Added `isIpOrBlankHostname(String)` package-private
static helper method that returns `true` for null, blank, IPv4 dotted-decimal, or IPv6
(colon-containing) hostnames. Modified `setSniViaSSLParameters` to call this helper and
return early (skipping SNI) for non-DNS hosts, with a `try/catch` safety net for any
remaining `IllegalArgumentException` from `SNIHostName`.

Capabilities inventory (replacement/rewrite: NOT applicable — this is a targeted guard
addition to a single method, not a replacement):

- RETAINED: DNS hostname SNI set via `SSLParameters.setServerNames` — line 238
- RETAINED: `socket.setSSLParameters(params)` called for DNS hosts — line 239
- RETAINED: DEBUG-only logging (BUG-012) — line 244
- NEW: `isIpOrBlankHostname` guard — line 207
- NEW: try/catch safety net — lines 233-245

### `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java`

Added 6 new `@Test` methods covering U-047 ACs:

1. `setSniViaSSLParameters_dnsHostname_setsSNI` — AC-2/AC-3(a): DNS host sets SNIHostName
2. `setSniViaSSLParameters_ipv4Literal_doesNotThrowAndSkipsSNI` — AC-1/AC-3(b): IPv4 skips SNI
3. `setSniViaSSLParameters_ipv6Literal_doesNotThrowAndSkipsSNI` — AC-1/AC-3(c): "::1" skips SNI
4. `setSniViaSSLParameters_ipv6FullLiteral_doesNotThrowAndSkipsSNI` — AC-1/AC-3(c): "fe80::1" skips SNI
5. `setSniViaSSLParameters_emptyHostname_doesNotThrowAndSkipsSNI` — AC-1/AC-3(d): "" skips SNI
6. `setSniViaSSLParameters_blankHostname_doesNotThrowAndSkipsSNI` — AC-1/AC-3(d): "   " skips SNI

## Files Created

- `sdlc/artifacts/build/sprints/sprint-011/U-047/plan.md`
- `sdlc/artifacts/build/sprints/sprint-011/U-047/implementation-log.md` (this file)

## Test Results

```
BUILD SUCCESSFUL
assembleDebug: PASS
testDebugUnitTest: PASS (672 tests, 0 failed, 2 skipped)
jacocoTestCoverageVerification: PASS (per-package LINE >= 70%)
```

Authoritative @Test count (git grep after commit):
`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` = **672**

Pre-implementation baseline (HEAD before this story): 666 @Test annotations.
Tests added: 6.

## Regression Results

All 666 previously-passing tests continue to pass. No regressions introduced.

## Acceptance Criteria

- [x] AC-1: `setSniViaSSLParameters` no longer throws for IPv4/IPv6 literal or empty/blank hostname; SNI is skipped and TLS proceeds.
  - `isIpOrBlankHostname` guard at line 207; try/catch at lines 233-245
- [x] AC-2: DNS hostname (`imap.gmail.com`) still sets SNI via `SSLParameters.setServerNames` — behavior unchanged.
  - Verified by `setSniViaSSLParameters_dnsHostname_setsSNI` and existing `setSniViaSSLParameters_setsSSLParametersWithCorrectSNIHostName`
- [x] AC-3: Unit tests: (a) DNS host → setSSLParameters with SNIHostName; (b) IPv4 literal → no throw, no SNI; (c) IPv6 literal → no throw; (d) empty/blank → no throw, SNI skipped.
  - Six new @Test methods in `DefaultTrustedSocketFactorySniTest.java`
- [x] AC-4: Change confined to vendored module + test; BUG-012 fix preserved (DEBUG only); build green.
  - Only `DefaultTrustedSocketFactory.java` and `DefaultTrustedSocketFactorySniTest.java` modified; all log calls at DEBUG; BUILD SUCCESSFUL

## Integration Path

New code is called from the same entry point as before:
`DefaultTrustedSocketFactory.createSocket` → `setSniHost` → `setSniViaSSLParameters` (API 24+ path).
The `isIpOrBlankHostname` helper is called within `setSniViaSSLParameters` only.

## Contract Adherence

No CNTR-* contracts listed in story frontmatter (`integration_contracts: []`). N/A.

## Notes

The JVM's `SNIHostName` constructor accepts IPv4 dotted-decimal without throwing
(unlike Android's). This means the pure try/catch approach (as originally suggested)
would not catch IPv4 in unit tests running on JVM. The explicit `isIpOrBlankHostname`
check handles IPv4 reliably in both JVM test and Android runtime environments. The
try/catch is retained as a safety net for malformed names not covered by the explicit
check.
