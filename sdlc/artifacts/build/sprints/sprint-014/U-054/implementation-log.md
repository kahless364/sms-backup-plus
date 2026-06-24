---
story: U-054
title: "Enforce ENCRYPTION_DEGRADED flag gate and warn on degraded launch"
sprint: sprint-014
status: COMPLETE
verdict: PASS
implemented_by: Developer
date: 2026-06-24
---

# Implementation Log — U-054

## Summary

U-054 adds the ENCRYPTION_DEGRADED enforcement gate: blocks backup/restore when credentials
are in plaintext (post-Keystore failure), emits a visible warning on launch, and shows a
distinct error in the UI.

This log covers the original implementation plus the post-review remediation that fixed two
BLOCKER/MEDIUM findings.

---

## Remediation (post-review)

### Finding 1 — BLOCKER: Launch degraded warning never rendered (AC-2 fails at runtime)

**Root cause**: `StatusPreference.backupStateChanged()` line 229 had:
```java
if (newState.backupType.isBackground()) return;
```
`BackupType.UNKNOWN.isBackground()` returns `true` (UNKNOWN is not MANUAL or SKIP).
`MainViewModel.checkDegradedOnLaunch()` emits `BackupState(ERROR, BackupType.UNKNOWN, ...)`.
The early-return guard silently dropped the ERROR state before any view update occurred.
The user saw no warning on launch.

**Fix** (`StatusPreference.java` line 234):
```java
if (newState.backupType.isBackground() && newState.state != SmsSyncState.ERROR) return;
```
ERROR states are never suppressed, regardless of backup type. Background progress/completion
updates are still suppressed (intended: they would be confusing noise for auto-backup).

**Headline string wiring** (`StatusPreference.java` `stateChanged()` ERROR branch):
Added a new `else if (state.exception instanceof EncryptionDegradedException)` branch that sets:
- `statusLabel` to `R.string.status_encryption_degraded` ("Encryption error — backup paused")
- `syncDetailsLabel` to `state.getErrorMessage(resources)` which returns
  `R.string.status_encryption_degraded_details` via the `LocalizableException` branch in
  `State.getErrorMessage()`.

Previously, the degraded error fell through to the generic `status_unknown_error` branch,
showing a confusing "Error" headline and an unhelpful detail string. Now it shows the
specific, user-actionable degraded headline and detail.

**Files changed**:
- `app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java`
  - Added import: `EncryptionDegradedException`
  - Line 234: guard condition changed (1-line)
  - `stateChanged()` ERROR branch: added `EncryptionDegradedException` case (7 lines)

**Test added** (`StatusPreferenceTest.java`):
- `backupStateChanged_backgroundType_nonError_returnsEarlyWithoutException()`: verifies
  all background types + all non-ERROR states return before touching unbound views (no NPE).
- `backupStateChanged_backgroundType_error_isNotSuppressed_proceedsPastGuard()`: verifies
  UNKNOWN+ERROR does NOT early-return — the call proceeds past the guard into `stateChanged()`
  and attempts to touch the unbound `progressBar`, which NPEs in the unit-test context.
  Catching NPE proves the guard did not suppress the ERROR call.

---

### Finding 2 — MEDIUM: Automatic schedule paths not gated when encryption is degraded

**Root cause**: `WorkManagerScheduler` gated `scheduleManual` and `scheduleImmediate` when
degraded but NOT the four automatic paths. A degraded device could enqueue doomed background
work (REGULAR/INCOMING/CONTENT_TRIGGER/BOOTUP) that would silently fail when BackupWorker
tried to decrypt credentials.

**Fix** (`WorkManagerScheduler.kt`): Added `if (secretStore.isEncryptionDegraded())` early-
return guard (returning null, logging a warning) to:
- `scheduleIncoming()` — incoming SMS trigger
- `scheduleRegular()` — periodic automatic backup
- `scheduleContentTrigger()` — content-URI observer (API 24+)
- `scheduleBootup()` — boot-triggered backup
- `scheduleRestore()` — restore (defense-in-depth; ViewModel also gates, but scheduler
  should be self-consistent since RestoreWorker requires IMAP credentials)

Guard style matches the existing pattern in `scheduleImmediate()` and `scheduleManual()`.

**Files changed**:
- `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt`
  - `scheduleIncoming()`: +5 lines (guard + log)
  - `scheduleRegular()`: +5 lines (guard + log)
  - `scheduleContentTrigger()`: +5 lines (guard + log)
  - `scheduleBootup()`: +5 lines (guard + log)
  - `scheduleRestore()`: +5 lines (guard + log)

**Tests added** (`WorkManagerSchedulerTest.kt`) — 5 new tests:
- `se002_scheduleIncoming_encryptionDegraded_returnsNullNoWork()`
- `se002_scheduleRegular_encryptionDegraded_returnsNullNoWork()`
- `se002_scheduleContentTrigger_encryptionDegraded_returnsNullNoWork()`
- `se002_scheduleBootup_encryptionDegraded_returnsNullNoWork()`
- `se002_scheduleRestore_encryptionDegraded_returnsNullNoWork()`

Each test stubs `secretStore.isEncryptionDegraded()` to return `true`, calls the schedule
method, asserts it returns `null`, and queries WorkManager to assert no work was enqueued.

---

### Finding 3 — LOW: EncryptedPrefsSchemeRegressionTest scheme capture fidelity

Deferred. The `SchemeCapturingStore` in `EncryptedPrefsSchemeRegressionTest` mirrors scheme
constants rather than capturing them from production code. A production refactor could change
the scheme constants without the test catching it. This is a test-fidelity concern, not a
functional gap in the current implementation. Tracking deferred to a follow-up story.

---

## Build Result

```
./gradlew :app:assembleDebug                  → BUILD SUCCESSFUL
./gradlew :app:testDebugUnitTest              → BUILD SUCCESSFUL (all tests pass)
./gradlew :app:jacocoTestCoverageVerification → BUILD SUCCESSFUL (coverage gates pass)
```

**Authoritative @Test count** (post-remediation, working tree):
```
grep -rh "@Test" app/src/test/ --include="*.java" --include="*.kt" | grep -c "@Test"
→ 697
```
Pre-remediation HEAD count: 690. Delta: +7 new @Test annotations.

---

## Integration Path

- `MainViewModel.checkDegradedOnLaunch()` is called from `MainActivity.onCreate()`.
- It emits `BackupState(ERROR, UNKNOWN, EncryptionDegradedException)` to `SyncStateRepository`.
- `StatusPreferenceFlowHelper` collects state from the repository and calls
  `StatusPreference.backupStateChanged()`.
- After the guard fix, the ERROR+UNKNOWN state is no longer dropped — `stateChanged()` is
  called, which routes to the new `EncryptionDegradedException` branch.
- The UI renders `status_encryption_degraded` as the headline and
  `status_encryption_degraded_details` as the detail.

- All automatic schedule paths (`scheduleIncoming`, `scheduleRegular`, `scheduleContentTrigger`,
  `scheduleBootup`, `scheduleRestore`) now gate on `secretStore.isEncryptionDegraded()` before
  enqueuing any work — consistent with `scheduleManual` and `scheduleImmediate` (existing gates).
