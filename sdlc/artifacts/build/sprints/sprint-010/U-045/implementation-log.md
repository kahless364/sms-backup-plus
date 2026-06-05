---
artifact_type: implementation-log
story_id: U-045
verdict: PASS
agent: Developer
timestamp: "2026-06-05"
files_changed: 1
files_created: 1
tests_added: 4
tests_passing: 663
---

# Implementation Log: U-045

## Summary

Fixed BUG-012: on Android API 35+ (conscrypt `Java8EngineSocket`) the vendored
`DefaultTrustedSocketFactory` was logging a `NoSuchMethodException` ERROR on every IMAP
connect because its reflective `setHostname(String)` SNI shim no longer applies to the new
socket class. Fixed by:
- Adding an API-24+ primary SNI path via `SSLParameters.setServerNames` (public API, no reflection).
- Downgrading the reflective fallback's catch from `Log.e` to `Log.d` so absence of the
  method is benign on modern platforms.

## Files Modified

| File | Lines changed | Reason |
|------|---------------|--------|
| `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` | +34 / -3 | Add SSLParameters SNI path; quiet reflective fallback; extract setSniViaSSLParameters |

### Change detail — DefaultTrustedSocketFactory.java

- **Imports added** (lines 21–22): `javax.net.ssl.SNIHostName`, `javax.net.ssl.SSLParameters`
- **`setSniHost` method** (lines 191–205): new leading `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)` branch calling `setSniViaSSLParameters`; existing SSLCertificateSocketFactory branch kept for API 17–23; reflective branch kept for pre-API 17.
- **`setSniViaSSLParameters` static method** (lines 212–216): new public static helper; calls `getSSLParameters()`, `setServerNames(Collections.singletonList(new SNIHostName(hostname)))`, `setSSLParameters(params)`.
- **`setHostnameViaReflection` catch block** (line 225): changed from `Log.e(LOG_TAG, "...", e)` to `Log.d(LOG_TAG, "... " + e.getMessage())` — no stack trace, no ERROR.

## Files Created

| File | Reason |
|------|--------|
| `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java` | Unit tests for the new SNI path and reflective fallback no-throw contract |

### Tests added (4)

1. `setSniViaSSLParameters_setsSSLParametersWithCorrectSNIHostName` — verifies `setSSLParameters` called with an SSLParameters containing `SNIHostName("imap.gmail.com")`.
2. `setSniViaSSLParameters_setsCorrectHostname_forDifferentHost` — verifies a different hostname propagates correctly.
3. `reflectiveFallback_withAbsentSetHostnameMethod_doesNotThrow` — calls the private `setHostnameViaReflection` via reflection against a `FakeSSLSocketWithoutSetHostname` (no `setHostname` method); asserts no exception.
4. `reflectiveFallback_withAbsentSetHostnameMethod_doesNotCallSetSSLParameters` — verifies the catch block does not invoke `setSSLParameters` (no partial state change).

## Build / Coverage Results

| Task | Result |
|------|--------|
| `:app:assembleDebug` | BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` | BUILD SUCCESSFUL (663 tests) |
| `:app:jacocoTestCoverageVerification` | BUILD SUCCESSFUL (per-package LINE >= 70% gate passed) |
| Full chain (`assembleDebug testDebugUnitTest jacocoTestCoverageVerification`) | BUILD SUCCESSFUL |

## Authoritative @Test Count (post-commit)

```
git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' \
  'k9mail-vendored/src/test/**/*.java' 'k9mail-vendored/src/test/**/*.kt' | grep -c "@Test"
663
```

(Previous: 659. Added: 4.)

## Acceptance Criteria

| AC | Status | Evidence |
|----|--------|----------|
| AC-1: No `NoSuchMethodException` ERROR on API 35+; SNI set via public API | PASS | `setSniViaSSLParameters` called for API >= N; ERROR log replaced with DEBUG |
| AC-2: TLS negotiates correctly; cert validation / pinned-cert path unaffected | PASS | No changes to `TrustManagerFactory`, `PinnedCertificateSocketFactory`, or `hardenSocket`; cipher/protocol filter unchanged |
| AC-3: Change confined to `:k9mail-vendored` `DefaultTrustedSocketFactory`; no transport/service changes | PASS | Only `DefaultTrustedSocketFactory.java` in `:k9mail-vendored` modified; test in `:app` test sources only |
| AC-4: Build green; unit test covers fallback path with absent method | PASS | 4 @Test methods; full build chain green; authoritative count 663 |

## Integration Path

`DefaultTrustedSocketFactory.setSniHost` is called at `DefaultTrustedSocketFactory.java:175` by `createSocket`, which is called by `PinnedCertificateSocketFactory.createSocket` and ultimately by `K9MailTransport$BackupImapStoreDelegate.createAndOpenFolder`. The fix is transparent to all callers — same method signature, same behavior contract, quieter log.

## Contract Adherence

No `integration_contracts` listed in this story's frontmatter. No CNTR-* verification required.

## Capabilities Inventory (pre-existing DefaultTrustedSocketFactory)

| Capability | Status |
|------------|--------|
| TLS SNI via SSLCertificateSocketFactory on API 17–23 | RETAINED — branch preserved in `setSniHost` |
| TLS SNI via reflective `setHostname` on pre-API 17 | RETAINED — `setHostnameViaReflection` kept; only log level changed |
| Cipher suite filtering (`hardenSocket`) | RETAINED — unchanged |
| Protocol filtering (`hardenSocket`) | RETAINED — unchanged |
| Client certificate / KeyChain support | RETAINED — unchanged |
| TrustManager construction via `TrustManagerFactory.get` | RETAINED — unchanged |
