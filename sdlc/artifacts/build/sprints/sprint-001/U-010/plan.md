---
artifact_type: plan
story_id: U-010
verdict: PASS
agent: Developer
timestamp: "2026-06-03T00:00:00Z"
---

# Plan: U-010 Delete AllTrustedSocketFactory, Audit Trust-Write Sites, Declare Gate G1

## Approach

This is a closing verification and deletion story. No new production code is introduced; the
work is deletion, audit, and gate declaration.

### Step 1 — Merge prerequisites

Merge `sdlc/modernization-plan` branch into the worktree to bring in U-007 through U-027
(including the trust-policy work, k9mail-vendored, etc.).

### Step 2 — Delete AllTrustedSocketFactory

Delete `app/src/main/java/com/zegoggles/smssync/mail/AllTrustedSocketFactory.java` from the
source tree entirely.

### Step 3 — Update lint-baseline.xml

Remove the `CustomX509TrustManager` baseline entry that suppressed lint for
`InsecureX509TrustManager` in the now-deleted file.

### Step 4 — Clean up comment references

Update the two Javadoc comment references to `AllTrustedSocketFactory` in
`PinCertificateEnrollmentFlow.java` (lines 64, 223) so that `grep -rn "AllTrustedSocketFactory"
app/src/main/java/` produces zero output.

### Step 5 — AC-1 verification

Run three greps:
- `grep -rn "AllTrustedSocketFactory" app/src/main/java/` → zero lines
- `grep -rn "InsecureX509TrustManager" app/src/main/java/` → zero lines
- `grep -rn "TrustAllX509TrustManager" app/src/main/java/` → zero lines

### Step 6 — AC-2 verification (X509TrustManager audit)

Enumerate all `X509TrustManager` implementations in `app/src/main/java/` and confirm no empty
`checkServerTrusted()` body exists. Expected implementations post-deletion:
- `PinnedCertificateSocketFactory.PinnedX509TrustManager` — fingerprint comparison + validity check
- `PinCertificateEnrollmentFlow.EnrollmentCaptureTrustManager` — enrollment-only capture (not data path)

### Step 7 — AC-3 verification (SERVER_TRUST_ALL_CERTIFICATES write audit)

Run `grep -rn "SERVER_TRUST_ALL_CERTIFICATES" app/src/main/java/` and trace every write site.
Confirm only `AuthPreferences.migrate()` writes to this key, and that write is `false` (clearing
stale value), never `true`.

### Step 8 — AC-4 CI verification

Run `./gradlew :app:assembleDebug`, `:app:testDebugUnitTest`, and
`:app:jacocoTestCoverageVerification`. All must be BUILD SUCCESSFUL.

### Step 9 — Gate G1 declaration

Record Gate G1 / Milestone M1 declaration in the story file and implementation-log.
