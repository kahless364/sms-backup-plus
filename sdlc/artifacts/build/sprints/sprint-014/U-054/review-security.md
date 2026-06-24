---
artifact_type: review-security
story_id: "U-054"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-06-24"
blockers: 0
warnings: 2
---

# Security Review: U-054

## Review Summary

U-054 closes the SE-002 gap from BUG-008: `isEncryptionDegraded()` is now wired into every user-initiated and broadcast-triggered backup and restore path. The gate is implemented at two independent layers (WorkManagerScheduler and MainViewModel), the broadcast path is genuinely blocked, the degraded flag is not permanently latched (it clears on successful re-migration), and the warning strings expose no credential material. One medium-severity defense-in-depth gap is documented below.

## Verdict: PASS

No blocking security issues. The primary attack surface (Tasker/ADB broadcast path via `BackupBroadcastReceiver → scheduleImmediate`, and all UI-initiated paths via `MainViewModel.startBackup/startRestore`) is completely gated. The medium finding is a defense-in-depth gap in automatic background scheduling paths that does not constitute a bypass of the stated acceptance criteria.

## Findings

### Blockers

None.

### Warnings

#### Medium — Automatic background scheduling paths not gated by `isEncryptionDegraded()`

**Files:** `WorkManagerScheduler.kt` (methods `scheduleIncoming`, `scheduleRegular`, `scheduleBootup`, `scheduleContentTrigger`)

**Description:** The four automatic-background scheduling methods do not call `secretStore.isEncryptionDegraded()` before enqueuing work. This means that if the degraded flag is set while a periodic backup job is already queued (e.g. via `scheduleRegular`), or on the next boot (via `scheduleBootup → BootReceiver`), a `BackupWorker` job will still be placed on the WorkManager queue. When the worker runs, it will attempt to read credentials from `EncryptedPrefsSecretStore`; in the degraded state those credentials are in plaintext SharedPreferences. The worker's `getEncrypted()` call would throw (Keystore unavailable), causing the job to fail and enter exponential backoff — so in practice no successful backup would occur with plaintext credentials. However, the defense-in-depth principle applied to `scheduleManual` and `scheduleImmediate` is not uniformly applied here.

**AC scope note:** U-054 AC-1 explicitly names "UI, a schedule trigger, or the `BackupBroadcastReceiver`" as the three trigger categories requiring gating. `scheduleBootup` is invoked by `BootReceiver` in response to `BOOT_COMPLETED`, which falls under "schedule trigger." This means `scheduleBootup` is within AC scope. `scheduleIncoming` (SMS broadcast), `scheduleRegular` (auto-backup periodic), and `scheduleContentTrigger` (content-URI observer) are the remaining ungated paths.

**Severity:** Medium. An enqueued worker in the degraded state would fail to authenticate (Keystore exception during `getEncrypted()`) and would not successfully transmit plaintext credentials to IMAP. This is not a credential-leakage bypass but is an incomplete enforcement of the gate's defensive intent.

**Recommendation:** Add `if (secretStore.isEncryptionDegraded()) return null` guards to `scheduleIncoming`, `scheduleRegular`, `scheduleBootup`, and `scheduleContentTrigger` in a follow-up story, or add a defense-in-depth check inside `BackupWorker.doWork()` that emits a degraded error state before touching credentials.

#### Low — Log message discloses degraded-state mechanism in logcat

**Files:** `WorkManagerScheduler.kt:281,322`, `MainViewModel.kt`

**Description:** `Log.w` statements emit "encryption degraded, credentials in plaintext" at WARNING level to logcat. On non-production/debug builds this is visible to any app with `READ_LOGS` permission. On production builds the log level is typically suppressed. This is an intentional diagnostic choice and not a credential leak (no credential values appear), but it does confirm the attack surface to a local attacker who can read logcat.

**Severity:** Low. No credential values are logged. The string is a sentinel, not a secret.

### Security Checklist

- [x] No hardcoded credentials or secrets — warning strings contain no credential material; `EncryptionDegradedException` message is a sentinel string only
- [x] Input validation on all user inputs — no new user input surfaces introduced
- [x] Output encoding prevents injection attacks — no output rendering of user data in changed paths
- [x] Authentication tokens handled securely — gate prevents scheduler from using degraded (plaintext-on-disk) credentials for IMAP authentication
- [x] Authorization checks on all protected resources — `BackupBroadcastReceiver` contract (CNTR-MODERNIZATION-005) preserved exactly; no permission changes
- [x] Sensitive data encrypted at rest and in transit — `isEncryptionDegraded()` read from separate `_meta` SharedPreferences file; no credential values exposed in flag persistence
- [x] Error messages do not leak internal details — `status_encryption_degraded_details` tells the user to re-authenticate; it does not name specific keys, file paths, or exception types
- [x] Dependencies have no known critical vulnerabilities — no new dependencies introduced by U-054

## Detailed Verification

### Broadcast path gate (AC-1, critical path)

`BackupBroadcastReceiver.backupRequested()` calls `getScheduler(context).scheduleImmediate()` (line 54). `App.getScheduler(context)` returns the Hilt-injected `WorkManagerScheduler` singleton. `WorkManagerScheduler.scheduleImmediate()` now checks `secretStore.isEncryptionDegraded()` at line 280 and returns `null` without calling `WorkManager.getInstance(context).enqueueUniqueWork(...)` when the flag is set. The broadcast path is genuinely blocked — verified by reading both `BackupBroadcastReceiver.java:54` and `WorkManagerScheduler.kt:279-283`, and confirmed by `EncryptionDegradedGateTest.degraded_scheduleImmediate_returnsNullAndNoJobEnqueued`.

### Flag not permanently latched (AC-4 / BUG-008 preservation)

`EncryptedPrefsSecretStore.migrateFromPlaintext()` at line 290 calls `setEncryptionDegraded(false)` after the full three-step commit succeeds (buffer → commit ciphertext → clear plaintext). The migration retry path (no `MIGRATION_COMPLETE_KEY` written on failure, so next launch re-enters) is intact at lines 230-293. The gate check at runtime is `isEncryptionDegraded()`, which reads the persisted flag; once migration succeeds the flag is false and scheduling proceeds normally. `EncryptionDegradedGateTest.recovery_afterDegradedClears_startBackupEnqueuesNormally` tests this conditional behavior.

### U-040 rollback-safe migration untouched

The three-step pattern (buffer → encrypt → clear) at `EncryptedPrefsSecretStore.java:273-285` is unchanged between the pre-U-054 commit and the U-054 commit. Confirmed by reading the diff: only `isEncryptionDegraded()` (default method) and one line adding `@Override` were added to the `EncryptedPrefsSecretStore`.

### No credential leakage in warning strings

`strings.xml` additions:
- `status_encryption_degraded`: "Encryption error — backup paused" — no credential data
- `status_encryption_degraded_details`: "Credential encryption failed on a prior launch. Backup and restore are blocked until credentials are re-entered. Open account settings to re-authenticate." — no credential data, no internal path or key name exposed

### Test coverage (AC-3)

`EncryptionDegradedGateTest` (13 tests) covers: `startBackup(MANUAL)` degraded → no enqueue + ERROR state; `startBackup(SKIP)` degraded → no enqueue + ERROR state; `startRestore()` degraded → ERROR state; happy-path `startBackup` non-degraded → job enqueued; `scheduleImmediate()` degraded → null + no enqueue; `scheduleManual(MANUAL)` degraded → null + no enqueue; recovery simulation; `checkDegradedOnLaunch` degraded → ERROR state; `checkDegradedOnLaunch` non-degraded → no-op.
