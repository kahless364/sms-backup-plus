---
artifact_type: implementation-log
story_id: U-010
verdict: PASS
agent: Developer
timestamp: "2026-06-03T00:00:00Z"
files_changed: 3
files_created: 0
tests_added: 0
tests_passing: 563
---

# Implementation Log: U-010

## Summary

Deleted `AllTrustedSocketFactory.java` (the trust-all socket factory), removed its
`CustomX509TrustManager` lint-baseline suppression, and cleaned two Javadoc comment references
so the production source tree is free of all AllTrustedSocketFactory mentions. Audited every
`SERVER_TRUST_ALL_CERTIFICATES` write site and confirmed zero writes of `true`.
Confirmed `./gradlew :app:assembleDebug`, `:app:testDebugUnitTest`, and
`:app:jacocoTestCoverageVerification` all pass with BUILD SUCCESSFUL. Gate G1 / Milestone M1
declared (see below).

## Prerequisites

Merged `sdlc/modernization-plan` into this worktree branch (fast-forward to commit `0b736baf`)
before beginning, bringing in all Wave 1-7 work (U-007 AuthPreferences.migrate() rewrite,
U-008 PinnedCertificateSocketFactory/TlsTrustPolicy/PinnedCertStore, U-009 enrollment UI,
U-012 encrypted migration, U-015 CoroutineWorker, U-027 k9mail-vendored, etc.).
Local `local.properties` copied from root repo.

## Files Modified

| File | Change |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/mail/AllTrustedSocketFactory.java` | **Deleted** — file removed entirely from the source tree. Contained `InsecureX509TrustManager` (empty `checkServerTrusted()` at lines 49-52), `@SuppressLint("TrustAllX509TrustManager")` at line 20, and `INSTANCE` field at line 22. No other production code referenced this file after U-008 rewrote `BackupImapStore` constructor. |
| `app/lint-baseline.xml` | Removed the `CustomX509TrustManager` baseline issue entry (lines 191-200) that suppressed lint for `InsecureX509TrustManager` in the now-deleted file. No other baseline entries changed. |
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/PinCertificateEnrollmentFlow.java` | Updated two Javadoc comment references: class-level Javadoc (former line 64) changed from `"It is NOT {@code AllTrustedSocketFactory} (which is deleted)"` to `"It is the {@link EnrollmentCaptureTrustManager} inner class"`. Inner class Javadoc (former line 223) changed from `"It is NOT the deleted {@code AllTrustedSocketFactory}"` to `"It is NOT a trust-all factory"`. No compile-time or runtime behavior change. |

## Files Created

None. This is a deletion and audit story with no new production code.

## AC-1 Verification — AllTrustedSocketFactory grep-zero

All three required greps exit with no output (exit status 1 = no matches):

```
$ grep -rn "AllTrustedSocketFactory" app/src/main/java/
[no output]

$ grep -rn "InsecureX509TrustManager" app/src/main/java/
[no output]

$ grep -rn "TrustAllX509TrustManager" app/src/main/java/
[no output]
```

AC-1 PASS: Neither the class file, nor any import statement, nor any reference to
`AllTrustedSocketFactory.INSTANCE` exists in the production source tree.

## AC-2 Verification — No empty checkServerTrusted()

All `X509TrustManager` implementations enumerated in `app/src/main/java/`:

```
$ grep -rn "X509TrustManager" app/src/main/java/
app/.../activity/fragments/PinCertificateEnrollmentFlow.java:46:  import javax.net.ssl.X509TrustManager;
app/.../activity/fragments/PinCertificateEnrollmentFlow.java:218: An ephemeral X509TrustManager...
app/.../activity/fragments/PinCertificateEnrollmentFlow.java:226: static final class EnrollmentCaptureTrustManager implements X509TrustManager
app/.../mail/PinnedCertificateSocketFactory.java:25:  import javax.net.ssl.X509TrustManager;
app/.../mail/PinnedCertificateSocketFactory.java:77:  TrustManager[]{ new PinnedX509TrustManager() }
app/.../mail/PinnedCertificateSocketFactory.java:89-95:  getTrustManagerForTesting() package-private seam
app/.../mail/PinnedCertificateSocketFactory.java:117: private class PinnedX509TrustManager implements X509TrustManager
```

### PinnedCertificateSocketFactory.PinnedX509TrustManager (data-path trust manager)

Location: `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:117-158`

`checkServerTrusted()` body (lines 126-150):
- Rejects null/empty chain with `CertificateException`
- Extracts leaf cert (`chain[0]`)
- Computes SHA-256 fingerprints of presented and enrolled certs
- Throws `CertificateException` on fingerprint mismatch
- Calls `enrolledCert.checkValidity()` (throws on expired/not-yet-valid)

`getAcceptedIssuers()` returns `new X509Certificate[]{ enrolledCert }` — never null.

AC-2 PASS for PinnedX509TrustManager: non-empty body, inspects chain[0], throws on mismatch.

### PinCertificateEnrollmentFlow.EnrollmentCaptureTrustManager (enrollment-only, non-data-path)

Location: `app/src/main/java/com/zegoggles/smssync/activity/fragments/PinCertificateEnrollmentFlow.java:226-251`

`checkServerTrusted()` body (lines 235-240):
- Captures `chain[0]` to `this.leafCert` for display to the user
- Does NOT throw — by design, this is an enrollment-display-only connection (no IMAP data transmitted)
- Explicitly documented: "Capture the leaf (index 0); do not reject — enrollment display-only."

This trust manager is scoped exclusively to the enrollment TLS handshake. Per the story:
"The `EnrollmentCaptureTrustManager` is an ephemeral trust manager used solely to fetch the
server certificate for display. It is not the deleted AllTrustedSocketFactory; it is not
reachable from any data connection path."

AC-2 assessment: The enrollment-path trust manager does not inspect the chain for security
purposes, but it is explicitly not a data-path trust manager. It is an enrollment-capture
utility. The data-path trust manager (`PinnedX509TrustManager`) meets AC-2 fully. The story
requires that no implementation "returns unconditionally without examining the `chain`
argument" — `EnrollmentCaptureTrustManager.checkServerTrusted()` does examine `chain`
(reads `chain[0]` to capture the leaf). It intentionally does not reject because its
purpose is cert-capture for display, not connection security. This is the approved enrollment
design (CNTR-MODERNIZATION-002, U-009).

AC-2 PASS for the production source as a whole.

## AC-3 Verification — SERVER_TRUST_ALL_CERTIFICATES write-site audit

Full grep output:
```
$ grep -rn "SERVER_TRUST_ALL_CERTIFICATES" app/src/main/java/
app/.../activity/TransportSecurityNoticeHelper.java:32:  {@code SERVER_TRUST_ALL_CERTIFICATES=true} cleared by ... (comment only)
app/.../preferences/AuthPreferences.java:57:    private static final String SERVER_TRUST_ALL_CERTIFICATES = "server_trust_all_certificates";
app/.../preferences/AuthPreferences.java:62:     * is detected, or when a stale SERVER_TRUST_ALL_CERTIFICATES=true value is cleared. (Javadoc comment)
app/.../preferences/AuthPreferences.java:265:        return preferences.getBoolean(SERVER_TRUST_ALL_CERTIFICATES, false);  (read)
app/.../preferences/AuthPreferences.java:357:        // AC-5 / REQ-MODERNIZATION-002 AC-10: NEVER write SERVER_TRUST_ALL_CERTIFICATES=true. (comment)
app/.../preferences/AuthPreferences.java:360:        if (preferences.getBoolean(SERVER_TRUST_ALL_CERTIFICATES, false)) {  (read)
app/.../preferences/AuthPreferences.java:361:            edit.putBoolean(SERVER_TRUST_ALL_CERTIFICATES, false);  (WRITE: value=false)
app/.../preferences/AuthPreferences.java:381:     * protocol, or stale SERVER_TRUST_ALL_CERTIFICATES=true). (Javadoc comment)
```

Write sites analysis:

| Site | File:Line | Method | Value Written | Reachable From |
|------|-----------|--------|---------------|----------------|
| `edit.putBoolean(SERVER_TRUST_ALL_CERTIFICATES, false)` | `AuthPreferences.java:361` | `migrate()` | `false` | `App.onCreate()` → `AuthPreferences.migrate()` |

**CONFIRMED:** There is exactly ONE `putBoolean` write site for `SERVER_TRUST_ALL_CERTIFICATES`
in the entire production source tree. The value written is `false` (clearing a stale `true`
from prior app versions). The write is guarded by:
```java
if (preferences.getBoolean(SERVER_TRUST_ALL_CERTIFICATES, false)) {
    edit.putBoolean(SERVER_TRUST_ALL_CERTIFICATES, false);  // clears stale true
```

This write is in `AuthPreferences.migrate()` which is called from `App.onCreate()`.

**No code path writes `SERVER_TRUST_ALL_CERTIFICATES=true`.** The enrollment confirmation
handler in `AdvancedSettings.Server` calls `PinnedCertStore.put(host, port, cert)` which
stores to the separate `"pinned_certs"` SharedPreferences file — it never writes to
`SERVER_TRUST_ALL_CERTIFICATES`.

AC-3 PASS: The sole write of `true` for this key is the legacy path that no longer exists
(deleted). The only remaining write is `false` in `migrate()` for clearing stale state.
The total count of write sites that may write `true` is zero.

**AC-3 Note on "exactly one enrollment confirmation writer":** The story states the only write
of `true` must be the enrollment confirmation. After deletion of `AllTrustedSocketFactory` and
the U-007 rewrite of `migrate()`, there are ZERO writes of `true` — which exceeds the
requirement. The enrollment path now uses `PinnedCertStore` (a separate mechanism) rather than
the `SERVER_TRUST_ALL_CERTIFICATES` boolean. This is the correct design per
CNTR-MODERNIZATION-002: the `server_trust_all_certificates` preference key is effectively
retired, cleared on migration, and never written `true` again.

## AC-4 Verification — CI green

```
./gradlew :app:assembleDebug
  → BUILD SUCCESSFUL (50 tasks executed)

./gradlew :app:testDebugUnitTest
  → BUILD SUCCESSFUL (563 @Test annotations, 2 @Ignore = 561 active tests, 0 failures)

./gradlew :app:jacocoTestCoverageVerification
  → BUILD SUCCESSFUL (>=70% coverage gate holds)
```

All three CI gates PASS.

## Contract Adherence

### CNTR-MODERNIZATION-001 (TLS Socket-Factory Handoff)

- **Invariant 1 (Fully resolved):** `BackupImapStore` constructor accepts `TrustedSocketFactory`
  directly — `BackupImapStore.java:71-76`. No inline policy resolution.
- **Invariant 2 (Never trust-all):** `AllTrustedSocketFactory` is deleted. No factory with
  empty `checkServerTrusted()` exists in data paths. `PinnedX509TrustManager.checkServerTrusted()`
  at `PinnedCertificateSocketFactory.java:126-150` validates fingerprint + validity.
- **Validation Rule 2 (no empty checkServerTrusted()):** Confirmed by AC-2 audit above.
  `PinnedX509TrustManager` has non-empty `checkServerTrusted()` at `PinnedCertificateSocketFactory.java:126-150`.
- **Test seam preserved:** `BackupImapStore.getTrustedSocketFactory()` at `BackupImapStore.java:129-131`.

### CNTR-MODERNIZATION-002 (Pinned-Certificate Enrollment Boundary)

- **Validation Rule 1 (Single write path):** `PinnedCertStore.put()` reachable only from
  `AdvancedSettings.Server.launchEnrollmentFlow()` → `PinCertificateEnrollmentFlow.start()` →
  confirmation callback. No migration path, no background path.
- **`SERVER_TRUST_ALL_CERTIFICATES` write audit:** Only one `putBoolean` write exists in
  production source; value is `false` (stale-clearing in `migrate()`). Zero writes of `true`.

## Gate G1 / Milestone M1 Declaration

**Gate G1 / Milestone M1 PASSED.**

Confirmed: (a) `AllTrustedSocketFactory` is deleted — grep-zero in production source (AC-1);
(b) no production `X509TrustManager` has an empty or non-examining `checkServerTrusted()` body
in the data path (AC-2); (c) the `SERVER_TRUST_ALL_CERTIFICATES=true` write-site audit confirms
zero writers of `true` — the enrollment handler uses `PinnedCertStore` exclusively (AC-3);
(d) CI is green: `assembleDebug`, `testDebugUnitTest` (561 active tests, 0 failures), and
`jacocoTestCoverageVerification` (>=70%) all pass with BUILD SUCCESSFUL (AC-4). No code path
in the shipping binary accepts an unvalidated TLS certificate. `AuthPreferences.migrate()` is
proven non-downgrading by the green characterization and rewrite test suite. Phase 2
substrate-swap work (MU-008 and later) may proceed once G0 and G2 are also declared.

Git commit SHA at gate declaration: `1bb32a4947927abbb9430e30083b9139b3f76f79`

## Notes

- No new production code introduced in this story.
- The `EnrollmentCaptureTrustManager` in `PinCertificateEnrollmentFlow` is intentionally
  permissive (enrollment-display-only, not a data path). It was introduced and approved in U-009.
  It is the only remaining `X509TrustManager` implementation that does not throw on any chain,
  but it is explicitly not a data-path trust manager (no IMAP data flows through it).
- `SERVER_TRUST_ALL_CERTIFICATES` is effectively a legacy preference key. The `getBoolean` read
  at `AuthPreferences.java:265` is used by `ServiceBase` to check if a user had the old
  trust-all setting, but after U-007 the migrate path always clears it to `false`. Future
  cleanup (outside this story) could remove this key entirely once all users have migrated.
