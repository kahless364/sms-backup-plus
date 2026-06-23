---
type: story
status: planned
artifact_type: user-story
priority: medium
complexity: medium
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
id: U-054
title: 'Enforce the ENCRYPTION_DEGRADED flag: gate and warn on degraded credential storage'
pipeline: ''
domain: modernization
requirement_source: assessment:20260623-post-migration-assessment#SE-002
sprint: '000014'
---

# U-054: Enforce the ENCRYPTION_DEGRADED flag: gate and warn on degraded credential storage

## Story
As a user of SMS Backup+, I want the app to display a persistent warning and block automatic sync when the Keystore has degraded to plaintext credential storage, so that I am never silently syncing with credentials that are unprotected on disk.

## Source
Derived from assessment 20260623-post-migration-assessment, finding SE-002 (completes BUG-008). See `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/security.md`.

## Acceptance Criteria
- [ ] AC-1: When `EncryptedPrefsSecretStore.isEncryptionDegraded()` returns `true` at the point a backup or restore is initiated (whether from the UI, a schedule trigger, or the `BackupBroadcastReceiver`), the sync is not started; instead a user-visible error or warning message is surfaced (e.g. a notification or in-app dialog) describing that credentials are unprotected and the user must re-authenticate.
- [ ] AC-2: The degraded-state warning is also surfaced in the main activity UI on launch (e.g. a persistent status chip or banner, consistent with the app's existing warning patterns) so the user is not surprised when a scheduled backup silently does nothing.
- [ ] AC-3: A unit test added to `EncryptedPrefsSecretStoreDegradedTest` (or a new sibling class) verifies that calling `scheduleManual()` or the backup-entry path while `isEncryptionDegraded()` returns `true` results in no `WorkManager` job being enqueued and the appropriate error state being emitted to `SyncStateRepository`.
- [ ] AC-4: Manual sync that was previously skipped due to degraded state is retried automatically (without user intervention) once the user re-authenticates and `isEncryptionDegraded()` returns `false` — the retry behavior established by BUG-008's migration (`EncryptedPrefsSecretStore.java:250-262`) is preserved.
- [ ] AC-5: Build green: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` passes; the new guard logic is covered by AC-3's test.

## Affected Code
| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/auth/EncryptedPrefsSecretStore.java:315` | `isEncryptionDegraded()` exists but has no callers that gate or surface anything | Add callers in the backup-initiation path that check this flag before enqueuing work |
| `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` (or backup-gate equivalent) | Enqueues backup/restore work unconditionally | Check `isEncryptionDegraded()` before enqueuing; emit an error state via `SyncStateRepository` if degraded |
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` (or `MainViewModel.kt`) | No degraded-state warning shown on launch | Add a check for `isEncryptionDegraded()` on launch; surface a persistent warning (banner/notification) |
| `app/src/test/java/.../auth/EncryptedPrefsSecretStoreDegradedTest.java` (or `.kt`) | Covers the keystore-degraded migration path but not the backup-gate behavior | Add test asserting no WorkManager job is enqueued when degraded; error state emitted |

## Existing Behavior to Preserve
- U-040 rollback-safe credential migration: the three-step pattern (buffer plaintext → commit ciphertext → clear plaintext only after commit, at `EncryptedPrefsSecretStore.java:273-285`) must not be touched.
- The `ENCRYPTION_DEGRADED` flag setting and next-launch retry behavior (`EncryptedPrefsSecretStore.java:250-262`) must remain: if Keystore fails on migration, the flag is set and the next launch re-attempts migration. The gate added by this story must not prevent the re-attempt.
- The `BackupBroadcastReceiver` contract (`CNTR-MODERNIZATION-005`) remains unchanged: the receiver is still exported with no permission; the degraded check happens inside the scheduling path, not at the receiver level.
- Existing `EncryptedPrefsSecretStoreDegradedTest` tests must continue to pass without modification.

## Verification Steps
1. In a unit test, mock `isEncryptionDegraded()` to return `true`; call the backup-initiation path; assert that `WorkManager.enqueueUniqueWork` is never called and an error `SyncState` is emitted.
2. Mock `isEncryptionDegraded()` to return `false`; call the same path; assert that work IS enqueued (confirming the gate is conditional, not always-blocking).
3. Run `./gradlew :app:testDebugUnitTest` — all existing `EncryptedPrefsSecretStore*` tests pass alongside the new test.
4. Run `./gradlew :app:jacocoTestCoverageVerification` — gate passes; new code is covered.

## Technical Context
- SE-002 root cause: BUG-008's fix correctly sets `ENCRYPTION_DEGRADED` on Keystore failure and retries on next launch, but never wired `isEncryptionDegraded()` into the backup path. The result is that a device in degraded state syncs with plaintext-on-disk credentials until the user re-authenticates — a state the user cannot observe.
- The gate check belongs in `WorkManagerScheduler` (or whatever scheduleManual entry point exists post-U-049) rather than deep inside the worker, so that the WorkManager queue is never populated with jobs that will run in a degraded state.
- The user-visible warning should use the same presentation mechanism as other app warnings (check existing warning/banner patterns in `MainActivity` or `StatusPreference`) to avoid inconsistent UX.

## Notes
- Sprint C (Security Hardening). Can run in parallel with Sprint A and B since it touches auth and scheduler, not the service-retirement or coverage infrastructure.
- Completes BUG-008: BUG-008 ensured the degraded flag is set correctly; this story ensures the flag is acted upon.
