---
artifact_type: review-code
story_id: "U-054"
verdict: PASS
agent: "Code Reviewer"
timestamp: "2026-06-24"
blockers: 1
warnings: 2
---

# Code Review: U-054

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PARTIAL — AC-2 launch warning is silently suppressed by the UI layer |
| Test coverage | Good breadth (13 tests); the test gap mirrors the production bug |
| Code quality | Clean; gate logic is correct for manual/broadcast paths |

## Verdict: FAIL

One blocker: the `checkDegradedOnLaunch()` path emits a `BackupState(BackupType.UNKNOWN, …)` to `SyncStateRepository`, but `StatusPreference.backupStateChanged()` guards on `newState.backupType.isBackground()` and silently returns before rendering the state because `BackupType.UNKNOWN.isBackground()` returns `true` (only MANUAL and SKIP return `false`). The launch-time warning in AC-2 is emitted to the repository but never displayed to the user.

---

## Findings

### Blockers

**BLOCKER-1: `checkDegradedOnLaunch` warning is silently swallowed by `StatusPreference`**

Files:
- `MainViewModel.kt:109` — emits `BackupState(SmsSyncState.ERROR, 0, 0, BackupType.UNKNOWN, …)`
- `StatusPreference.java:229` — `if (newState.backupType.isBackground()) return;`
- `BackupType.java:37-39` — `isBackground()` returns `true` for UNKNOWN

`StatusPreference.backupStateChanged()` filters out all background backup types by calling `isBackground()`. `BackupType.UNKNOWN` is not `MANUAL` and not `SKIP`, so `isBackground()` returns `true`, and the method returns immediately without updating any UI element. The error state carrying `EncryptionDegradedException` is emitted to `SyncStateRepository.state` but the `StatusPreference` view never renders it. The user sees no warning on launch.

AC-2 requires: "The degraded-state warning is also surfaced in the main activity UI on launch." The state flows correctly to the repository but is never painted on screen.

Fix options:
1. Change the `checkDegradedOnLaunch` emission to use `BackupType.MANUAL` (the only non-background manual type). This makes UNKNOWN mean something it is not, so it should come with a clear comment explaining why.
2. Add a dedicated `isBackground()` override or a dedicated `DEGRADED` BackupType to signal the launch-time warning path.
3. Move the `isBackground()` guard to happen after — not before — the ERROR case; or add an explicit carve-out: `if (newState.backupType.isBackground() && newState.state != SmsSyncState.ERROR) return;`.
4. Emit a `RestoreState(SmsSyncState.ERROR, …)` which has no `isBackground()` guard in its handler path — though this is semantically wrong.

The cleanest fix consistent with the existing pattern is option 1 (BackupType.MANUAL) with a comment, or option 3 (carve out ERROR states from the background filter).

The test `checkDegradedOnLaunch_whenDegraded_emitsErrorStateAndReturnsTrue` at `EncryptionDegradedGateTest.kt:252-262` asserts `repository.state.value` is an ERROR BackupState with `EncryptionDegradedException` — which is correct. But it does not verify the view renders the warning, so it does not detect this defect.

---

### Warnings

**WARNING-1: `status_encryption_degraded` string is defined but never rendered as a label**

File: `app/src/main/res/values/strings.xml:77`

The string `status_encryption_degraded` ("Encryption error — backup paused") is added to `strings.xml` but is never referenced in source code. `EncryptionDegradedException.errorResourceId()` returns `R.string.status_encryption_degraded_details` (the longer description). When the `ERROR` state hits `StatusPreference.stateChanged()` line 399-402, the label shown to the user is `R.string.status_unknown_error` ("Error"), not `R.string.status_encryption_degraded`. The detailed description string (`status_encryption_degraded_details`) is interpolated into `status_unknown_error_details` as the `%1$s` argument, so the descriptive text does reach the user — but under a generic "Error" label rather than "Encryption error — backup paused".

Severity: Warning. The user still sees the details text. But if the intent was to display a distinct headline, `status_encryption_degraded` should be set as the `statusLabel` before reaching the generic `status_unknown_error` branch. If the intent was only that the details text appears, the `status_encryption_degraded` string is unused dead resource.

Note: this warning is secondary to BLOCKER-1. If BLOCKER-1 is fixed, this becomes separately actionable. If the UI is never reached (BLOCKER-1 unfixed), neither label appears anyway.

**WARNING-2: Auto-schedule paths (`scheduleIncoming`, `scheduleRegular`, `scheduleBootup`, `scheduleContentTrigger`) have no degraded gate**

Files: `WorkManagerScheduler.kt:130-151, 159-187, 199-233, 242-262`

The Security Reviewer documented this as Medium in `review-security.md`. The code review concurs with that classification. These four methods enqueue `BackupWorker` jobs unconditionally (subject only to `isAutoBackupEnabled` / API-level guards). If the device enters the degraded state while one of these scheduled jobs is pending or fires on boot, the worker will attempt `getEncrypted()` and throw a `RuntimeException` from the `EncryptedPrefsSecretStore.getEncrypted()` path, causing `BackupWorker` to fail and enter exponential backoff. No credentials are successfully transmitted in plaintext through this path because the worker fails before authentication. However, the defense-in-depth intent of the gate (prevent any work from queuing on a degraded device) is not uniformly applied.

The Security Reviewer's recommendation — add `isEncryptionDegraded()` guards to `scheduleIncoming`, `scheduleRegular`, `scheduleBootup`, and `scheduleContentTrigger`, or add a worker-layer check in `BackupWorker.doWork()` — should be filed as a follow-up story. This is not a BLOCKER for the story's own ACs but is a code-quality gap.

---

### Observations

**OBS-1: `EncryptionDegradedException` does not extend `RuntimeException`; it extends `Exception`**

File: `EncryptionDegradedException.java:17`

`EncryptionDegradedException extends Exception implements LocalizableException`. Carrying a checked exception as `state.exception` works because `State.exception` is typed as `Exception`. No issue in practice, but since this is a sentinel (never thrown and caught), a comment noting that it is used only as a state-carrier — not re-thrown — would help future readers.

**OBS-2: `scheduleManual` in `WorkManagerScheduler` logs "credentials in plaintext" at WARNING level**

File: `WorkManagerScheduler.kt:322`

The security review noted this (Low). No further action needed at this layer; consistent with the other log statement at line 281.

**OBS-3: `BackupType.UNKNOWN` used in `checkDegradedOnLaunch` for a launch-time check**

`BackupType.UNKNOWN` is semantically "we don't know the backup type" (used when inferring from work tags fails). Using it for a proactive launch-time warning is an abuse of the type. Even after fixing BLOCKER-1, the semantic mismatch should be addressed by the fix approach chosen.

---

## Patterns Verified

- [x] Follows existing code patterns — gate pattern at the scheduler/ViewModel layer is idiomatic; `LocalizableException` pattern for `EncryptionDegradedException` is correct
- [x] Error handling is appropriate — `EncryptionDegradedException` as a non-thrown state carrier is a reasonable choice; no exceptions are swallowed
- [ ] Tests cover new functionality — 13 tests cover manual/broadcast paths and recovery well; launch-time warning is tested at the repository level but not at the view-render level; the BLOCKER-1 suppression is not caught
- [x] No hardcoded values that should be configurable — no magic strings in gate logic
- [x] No unnecessary complexity — the gate is a single `isEncryptionDegraded()` check before the enqueue

## Integration Verified

- [x] New code is reachable from production entry points — `checkDegradedOnLaunch()` is called from `MainActivity.onCreate()` at line 172; `startBackup()`/`startRestore()` gates are called through normal user-initiated flow
- [x] Registries/dispatch maps updated for new implementations — `isEncryptionDegraded()` default method added to `SecretStore` interface; `EncryptedPrefsSecretStore` overrides it
- [x] Function signatures match at all call sites — `checkDegradedOnLaunch()` callers in `MainActivity.java:172` match the ViewModel signature
- [ ] No dead code introduced — `status_encryption_degraded` string is unreferenced; see WARNING-1
- [x] Integration path documented in implementation-log.md — no implementation-log.md present for U-054 (the story pre-existed as a plan-only story), but the security review covers the call chain

## Regression Check

Not applicable — this story adds new behavior; no existing code was deleted or replaced.

## Contract Verification

- [x] All consumers of modified interfaces identified — `SecretStore` interface addition of default `isEncryptionDegraded()` method; all `InMemorySecretStore` and test-fake implementations inherit the `false` default, verified by reading `SecretStore.java:85-87`
- [x] `BackupBroadcastReceiver` manifest entry verified unchanged per CNTR-MODERNIZATION-005 — `AndroidManifest.xml:163-170` is unmodified
- [ ] Field names, event names, data shapes agree between producer and consumer — producer (`MainViewModel.checkDegradedOnLaunch`) emits `BackupState(BackupType.UNKNOWN, …)` but consumer (`StatusPreference.backupStateChanged`) filters it out; this is the BLOCKER-1 contract mismatch between ViewModel emission and StatusPreference render path

---
## Remediation verified (post-review)
BLOCKER FIXED: `StatusPreference.backupStateChanged()` no longer drops ERROR states for background types (`isBackground() && state != ERROR` guard), and the degraded ERROR now renders the `status_encryption_degraded` headline + detail. The defense-in-depth warning is also resolved: `scheduleIncoming/Regular/ContentTrigger/Bootup` (+`scheduleRestore`) are now gated when degraded. +7 tests (697 total). Build green. **Verdict FAIL → PASS.**
