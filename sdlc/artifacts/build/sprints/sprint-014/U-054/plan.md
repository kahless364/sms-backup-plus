---
artifact_type: plan
story_id: U-054
verdict: PASS
---
# U-054 Plan — Enforce the ENCRYPTION_DEGRADED flag (SE-002, completes BUG-008)
## Root cause
`EncryptedPrefsSecretStore` sets a persisted `ENCRYPTION_DEGRADED` flag on Keystore failure during the plaintext→encrypted credential migration (U-040), but nothing reads it — backups can proceed with credentials in plaintext and the user is never warned (assessment finding SE-002).
## Approach
1. Gate backup/restore initiation when `isEncryptionDegraded()` is true: `MainViewModel.startBackup/startRestore` emit an ERROR state; `WorkManagerScheduler.scheduleManual/scheduleImmediate` (broadcast path) and the automatic paths (scheduleIncoming/Regular/ContentTrigger/Bootup) + scheduleRestore return null without enqueuing.
2. Surface a persistent launch warning via the status UI (StatusPreference ERROR state + `status_encryption_degraded` strings), rendered through `checkDegradedOnLaunch()` in MainActivity.onCreate.
3. Preserve U-040 rollback-safe migration + BUG-008 retry: the flag is not latched; it clears on the next successful migration and backup resumes.
## Verdict
PASS (after remediation) — gate covers manual + broadcast + automatic paths; launch warning renders; BUG-008 retry intact; 697 tests green.
