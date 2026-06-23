---
artifact_type: review-security
story_id: "U-049"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-06-23"
blockers: 0
warnings: 0
---

# Security Review: U-049 — Retire Legacy Backup/Restore Service Dual-Dispatch Layer

## Review Summary

U-049 deletes `SmsBackupService`, `SmsRestoreService`, and `ServiceBase` (~1,200 source lines),
removes the corresponding `<service>` entries from `AndroidManifest.xml`, moves foreground
notification into `BackupWorker`/`RestoreWorker` via `setForeground(ForegroundInfo(...))`, and
re-homes WorkInfo observation and cancel routing into `MainViewModel`. All five security-focused
verification items from the delegation brief were checked against the actual code on disk and
pass without reservation. This story is security-neutral relative to baseline.

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

None.

### Low (informational)

None that require action.

---

## Detailed Security Verification

### 1. CNTR-MODERNIZATION-005: BackupBroadcastReceiver — unchanged, contract preserved

Read `BackupBroadcastReceiver.java` in full and cross-referenced against
`CNTR-MODERNIZATION-005-backup-broadcast.md` (status: approved).

**Verified:**
- The receiver class, action string (`"com.zegoggles.smssync.BACKUP"`), and
  `<intent-filter>` entry are **identical** to the pre-sprint tree; U-049 made zero
  changes to this file (confirmed via `git diff 40a93bd7 HEAD -- .../BackupBroadcastReceiver.java`
  producing no output).
- Manifest entry (lines 162–169) retains `android:exported="true"` with **no**
  `android:permission` attribute — exactly as the contract requires. No permission was added.
- `onReceive` still calls `backupRequested()` which calls `getScheduler(context).scheduleImmediate()`
  — the user gate (`isAllow3rdPartyIntegration()`) is intact. Post-condition preserved.
- The implementation log claim "BackupBroadcastReceiver was NOT touched" is verified correct.
  CNTR-MODERNIZATION-005 §Validation Rule 3 (no sender permission) and Rule 4 (user gate) both
  hold.

### 2. Foreground Service: ForegroundInfo in Workers

Read `BackupWorker.kt` lines 474–493 and `RestoreWorker.kt` lines 689–707 in full.

**Verified:**
- Both workers call `setForeground(createBackupForegroundInfo())` /
  `setForeground(createRestoreForegroundInfo())` as **the first statement** in `doWork()`,
  before any user-data access. This is the correct, ANR-safe pattern for WorkManager foreground
  workers.
- `ForegroundInfo` is constructed with notification IDs `BACKUP_NOTIFICATION_ID = 1` and
  `RESTORE_NOTIFICATION_ID = 2` (matching the legacy service constants) using channel
  `App.CHANNEL_ID`. No new notification channel is created.
- `foregroundServiceType` on the `ForegroundInfo` object is **not** set explicitly in the
  constructor call (the single-arg `ForegroundInfo(id, notification)` is used). Under
  WorkManager 2.6+, when workers use `setForeground()`, WorkManager's own
  `SystemForegroundService` handles FGS promotion. `SystemForegroundService` is declared in
  WorkManager's library manifest with `android:foregroundServiceType="dataSync"` (confirmed by
  the library's own manifest; the comment at `AndroidManifest.xml:130–135` documents this).

  The app manifest declares `FOREGROUND_SERVICE` (line 79) and
  `FOREGROUND_SERVICE_DATA_SYNC` (line 81) permissions — the correct pair for targetSdk 34+
  with dataSync foreground service type. No permission regression.

  **Note on ForegroundInfo constructor:** The two-argument `ForegroundInfo(int notificationId,
  Notification notification)` constructor was available in WorkManager 2.3+ and does not accept
  a `foregroundServiceType` parameter. The three-argument variant
  `ForegroundInfo(int, Notification, int foregroundServiceType)` could be used for API-level
  exactness, but WorkManager routes `setForeground()` through `SystemForegroundService` which
  carries its own `foregroundServiceType="dataSync"` declaration. The practical result is
  correct: the OS sees dataSync. This is a documentation observation, not a security finding.

- `PendingIntent` flags: both workers set `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`. `FLAG_IMMUTABLE`
  is required on API 31+ to prevent third parties from modifying the pending intent's extras —
  correct and present. No `FLAG_MUTABLE` used.

### 3. AndroidManifest — exported components, permissions, allowBackup, debuggable

Read `AndroidManifest.xml` in full and ran targeted checks.

**Verified:**
- **Removed entries:** The diff shows exactly two `<service>` entries removed:
  `SmsBackupService` (exported=false, foregroundServiceType=dataSync) and `SmsRestoreService`
  (exported=false, foregroundServiceType=dataSync). No other entries were modified.
- **Remaining exported components:** `MainActivity` (exported=true, LAUNCHER), `RedirectReceiverActivity`
  (exported=true, oauth2redirect), `ComposeSmsActivity` (exported=true, SMS compose intent),
  `BackupBroadcastReceiver` (exported=true, BACKUP action), `SmsReceiver` (exported=true,
  BROADCAST_SMS permission gated), `MmsReceiver` (exported=true, BROADCAST_WAP_PUSH permission
  gated), `HeadlessSmsSendService` (exported=true, SEND_RESPOND_VIA_MESSAGE permission gated).
  All pre-existing. No new exported component was added.
- **`android:permission` audit:** No permission attribute was removed from any existing
  component. `SmsBroadcastReceiver` retains `android:permission="android.permission.BROADCAST_SMS"`.
  `BackupBroadcastReceiver` correctly has no permission (contract requirement). `HeadlessSmsSendService`
  retains `android:permission="android.permission.SEND_RESPOND_VIA_MESSAGE"`.
- **`allowBackup`:** `android:allowBackup="true"` (line 95) — unchanged from pre-sprint. The
  `@xml/backup_descriptor` reference is present. This is the existing project policy; no change.
- **`debuggable`:** No `android:debuggable` attribute present in manifest. Build toolchain
  controls this at signing time; release builds default to false. Unchanged.
- **`FOREGROUND_SERVICE_DATA_SYNC` permission:** Present at line 81 — correctly declared for
  API 34+ dataSync FGS workers. This permission was pre-existing (confirmed by `git diff` showing
  only the two `<service>` entries as changed).

### 4. Credential/Token Handling — WorkerEngineFactory injection, no credential exposure

Read `MainViewModel.kt` in full, `WorkManagerCancelCollector.kt` in full, and reviewed all new
log statements introduced in the diff.

**Verified:**
- `MainViewModel` receives `Context`, `SyncStateRepository`, and `BackupScheduler` via Hilt
  injection. None of these carry raw credentials or tokens.
- New `Log.d` statements in `MainViewModel.startBackup()` and `startRestore()` log only
  `backupType` (an enum) and `job.tag` (a unique-work-name string such as "MANUAL" or "RESTORE").
  No token, password, email address, or OAuth secret is logged.
- `WorkManagerCancelCollector.collect()` logs only `"Cancel received, cancelling $uniqueWorkName"` —
  no credential content.
- `K9MailLib.setDebugStatus` in `App.java` has `debugSensitive()` returning `false` — confirmed
  unchanged. K9 will not log passwords/tokens even if its debug flag is enabled.
- The observation path `WorkManager.getWorkInfosForUniqueWorkFlow()` yields `WorkInfo` objects
  containing `WorkData` (typed key-value pairs of primitives: counts, state strings, data-type
  names). None of these contain credentials or PII beyond message counts.

### 5. TLS / Trust — no change confirmed

Read `BackupWorker.kt` and `RestoreWorker.kt` in full.

**Verified:**
- Neither worker contains TLS-related code. `mailTransportFactory.create()` is the sole
  transport construction call in both workers — unchanged in this story. `MailTransportFactory`
  and `K9MailTransport` (which own TLS socket factory and pinned-cert logic) are not modified
  in this sprint.
- The `AuthPreferences.getStoreUri()` path (which encodes the IMAP URI including auth type)
  is invoked inside `MailTransportFactory.create()`, not in the workers directly — pre-existing
  architecture, unchanged.
- No `TrustAll` or `SERVER_TRUST_ALL_CERTIFICATES` references appear in any file changed by
  U-049.

---

## Security Checklist

- [x] No hardcoded credentials or secrets — none in `MainViewModel.kt`, `BackupWorker.kt`
      foreground info section, or `RestoreWorker.kt` foreground info section.
- [x] Input validation on all user inputs — no new user input surfaces. `WorkInfo` progress
      data is produced internally by workers; no external input is parsed.
- [x] Output encoding prevents injection attacks — N/A; no output encoding surface changed.
- [x] Authentication tokens handled securely — no change to token read/write paths;
      `EncryptedPrefsSecretStore` is not touched.
- [x] Authorization checks on all protected resources — `BackupBroadcastReceiver` export and
      gate intact; all removed services were `exported=false` (no attack surface reduction lost).
      WorkManager's `SystemForegroundService` is not app-declared and carries no risk.
- [x] Sensitive data encrypted at rest and in transit — no change to EncryptedSharedPreferences
      or TLS configuration.
- [x] Error messages don't leak internal details — `WorkInfo` `KEY_FAILURE_REASON` values are
      opaque strings ("auth_failed", "backoff_cap_exceeded", etc.); no stack traces or
      credential fragments are placed in WorkData.
- [x] Dependencies have no known critical vulnerabilities — no dependency additions in U-049.
