---
artifact_type: implementation-plan
story_id: "U-008"
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# Implementation Plan: U-008 — TlsTrustPolicy + TrustedSocketFactory + PinnedCertStore

## Objective

Introduce the app-owned TLS trust policy layer: a two-constant enum (`TlsTrustPolicy`), a
validating `PinnedCertificateSocketFactory`, a per-host certificate store (`PinnedCertStore`),
and wire `BackupImapStore` / `ServiceBase` to the new constructor signature. Per
CNTR-MODERNIZATION-001, the store is a passive recipient of an already-resolved factory;
all policy resolution happens in `ServiceBase.getBackupImapStore()`.

## Files to Create

| File | Purpose |
|------|---------|
| `app/src/main/java/com/zegoggles/smssync/mail/TlsTrustPolicy.java` | Two-constant enum: SYSTEM_VALIDATED, PINNED_CERTIFICATE |
| `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` | TrustedSocketFactory with SHA-256 + validity-window TrustManager |
| `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertStore.java` | SharedPreferences-backed certificate store keyed by host:port |
| `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryTest.java` | Unit tests: match / mismatch / getAcceptedIssuers invariants |
| `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertStoreTest.java` | Unit tests: null-for-unenrolled, round-trip, remove, independence |

## Files to Modify

| File | Change |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java` | Constructor: `(Context, String, boolean)` → `(Context, String, TrustedSocketFactory)` |
| `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` | Hoist factory resolution; replace `isTrustAllCertificates()` boolean with TlsTrustPolicy dispatch |
| `app/src/main/res/xml/backup_descriptor.xml` | Add `<exclude domain="sharedpref" path="pinned_certs.xml"/>` |
| `app/src/test/java/com/zegoggles/smssync/mail/BackupImapStoreTest.java` | Adapt all call sites to new `(Context, String, TrustedSocketFactory)` constructor; replace trust-all test with PinnedCertificateSocketFactory assertion |

## Approach

1. `TlsTrustPolicy`: minimal two-constant enum, no third constant.
2. `PinnedCertificateSocketFactory`: inner `PinnedX509TrustManager` performs SHA-256 fingerprint equality + `enrolledCert.checkValidity()`; `getAcceptedIssuers()` returns `new X509Certificate[]{enrolledCert}`. Package-private `getTrustManagerForTesting()` seam.
3. `PinnedCertStore`: uses `context.getSharedPreferences("pinned_certs", Context.MODE_PRIVATE)`; Base64-DER round-trip; exposes `getTlsTrustPolicy(host, port)` → SYSTEM_VALIDATED unless key present.
4. `BackupImapStore` constructor: remove boolean ternary; accept pre-resolved `TrustedSocketFactory`.
5. `ServiceBase.getBackupImapStore()`: parse URI for host:port; instantiate `PinnedCertStore`; dispatch via `TlsTrustPolicy`; pass factory to `new BackupImapStore(...)`.
6. Tests: use hardcoded DER literals (keytool-generated, valid until 2036) — no BouncyCastle dependency needed.
