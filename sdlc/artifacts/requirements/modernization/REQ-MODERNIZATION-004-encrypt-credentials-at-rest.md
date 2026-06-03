---
id: REQ-MODERNIZATION-004
title: Encrypt Stored Credentials at Rest (EncryptedSharedPreferences)
artifact_type: requirement
domain: modernization
status: approved
type: nfr
priority: high
epic: EPIC-MODERNIZATION-002
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-002
related_stories: []
related_design_docs: []
traces_to: []
change_records: []
notes: |
  Traceability: SEC-002/CAP-003 (gaps), ARCH-009 (ilities); migration unit MU-004; source assessment 20260529-modernization. Co-requisite with REQ-MODERNIZATION-002 (both touch AuthPreferences — sequence the trust-all fix first, then encryption).
---

# Encrypt Stored Credentials at Rest

## Description

Migrate the application's stored secrets from plaintext `SharedPreferences` to
Jetpack Security `EncryptedSharedPreferences` (AES-256, Android Keystore-backed),
with a safe one-time migration for existing installs.

## Context

`AuthPreferences` persists the Gmail app password (`IMAP_PASSWORD`) and the OAuth2
tokens (`OAUTH2_TOKEN`, `OAUTH2_REFRESH_TOKEN`) in a plaintext `credentials.xml`
`SharedPreferences` file (`preferences/AuthPreferences.java:221-226`). The existing
defensive design — a separate `credentials.xml` excluded from Android auto-backup — is
good segregation but is **not encryption**: ADB backup on a debuggable/rooted device,
or forensic imaging, can read the secrets in cleartext (CWE-312). This is the
credential-at-rest counterpart to the in-transit MITM fix in REQ-MODERNIZATION-002;
the two together close the app's confidentiality exposure for the highest-sensitivity
data it holds (a full Gmail mailbox credential).

## Acceptance Criteria

1. `IMAP_PASSWORD`, `OAUTH2_TOKEN`, and `OAUTH2_REFRESH_TOKEN` are stored via
   `EncryptedSharedPreferences` (AES-256-GCM value encryption, Keystore-backed master key).
2. A one-time migration runs on first launch after upgrade: read plaintext → write
   encrypted → clear plaintext, executed atomically with rollback safety (a failure
   mid-migration must not leave the user unable to authenticate).
3. After migration, no plaintext credential value remains in any on-device preferences file.
4. The `credentials.xml` (or its encrypted successor) remains excluded from Android auto-backup.
5. The public `AuthPreferences` accessor signatures (`getCredentials()`/`setCredentials()`)
   are unchanged — callers are unaffected.
6. A test verifies the migration path (plaintext present → encrypted after first launch →
   plaintext absent) and that a fresh install writes only encrypted values.

## Rationale

The app holds a Gmail full-mailbox credential and the user's entire message corpus.
Segregation without encryption leaves that credential recoverable from device backups
and rooted devices. `EncryptedSharedPreferences` closes CWE-312 at the single
`getCredentials()`/`setCredentials()` seam with no API change to callers.

## Constraints

- Depends on REQ-MODERNIZATION-001 (AndroidX/SDK uplift) for the Jetpack Security library.
- Co-requisite with REQ-MODERNIZATION-002: both modify `AuthPreferences`; sequence the
  trust-all/`migrate()` security fix first, then layer encryption on top to avoid
  conflicting edits to the same migration path.
- The one-time migration must be idempotent and safe to re-run if interrupted.

## Notes

Boundary: `preferences/AuthPreferences.java` (`getCredentials()`/`setCredentials()` seam).
Traceability: SEC-002/CAP-003, ARCH-009; MU-004; source assessment 20260529-modernization.
