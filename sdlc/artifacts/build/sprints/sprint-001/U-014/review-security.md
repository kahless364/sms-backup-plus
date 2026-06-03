---
artifact_type: review-security
story_id: "U-014"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-03"
blockers: 0
warnings: 1
---

# Security Review: U-014 — WorkManagerScheduler Adapter

## Overall Verdict: PASS

No security blockers found. The implementation introduces a WorkManager scheduling adapter with
no new attack surface, no credential handling, and no new permissions.

## Security Checklist

### Secrets and Credentials

- [x] No hardcoded credentials, tokens, or API keys introduced
- [x] No new SharedPreferences writes from WorkManagerScheduler or worker stubs
- [x] BackupWorker and BackupTriggerWorker stubs access no sensitive data

### Android Permissions

- [x] No new `<uses-permission>` declarations added to AndroidManifest.xml
- [x] No new exported components added (WorkManager workers are not exported by default)
- [x] `BackupBroadcastReceiver` retains `android:exported="true"` — this is the correct,
  pre-existing value required for the external BACKUP broadcast contract (CNTR-005). No change.

### Intent Safety

- [x] `WorkManagerScheduler.scheduleImmediate()` uses `Constraints.NONE` — does not add
  a network constraint, consistent with the BROADCAST_INTENT legacy behavior. No implicit
  trust or permission elevation.
- [x] `BackupTriggerWorker` constructs a `OneTimeWorkRequest` targeting `BackupWorker` by class —
  internal to the application, not exported, no intent extras that could carry untrusted data.
- [x] No PendingIntents with mutable flags are introduced.

### Input Validation

- [x] `scheduleIncoming()` returns null when `incomingTimeoutSecs <= 0` — no negative delay passed to WorkManager.
- [x] `scheduleRegular()` uses `maxOf(regularTimeoutSecs, PERIODIC_MIN_INTERVAL_SECS)` — prevents
  zero or negative interval from reaching WorkManager.
- [x] No user-controlled data is passed to WorkManager worker input data (Data.EMPTY used implicitly).

### ContentProvider URI Triggers

- [x] Content-URI triggers observe `content://sms` and `content://call_log/calls` — read-only
  observation, no write capability granted by adding these triggers.
- [x] `triggerForDescendants = true` is consistent with the existing Firebase JobDispatcher
  `FLAG_NOTIFY_FOR_DESCENDANTS` behavior.

### WorkManager Configuration

- [x] `WorkManagerTestInitHelper.initializeTestWorkManager` is test-only, not in production code.
- [x] Default WorkManager initializer is in use (AndroidX work manifest meta-data). The custom
  `HiltWorkerFactory` setup (U-024) is explicitly deferred. No custom executor in production.
- [x] Logging level in test config (`Log.DEBUG`) is test-only.

## Warning (Non-blocking)

### Warning 1: CompositeScheduler divergence log may expose scheduling metadata in debug builds

`CompositeScheduler.logDivergence` logs the unique-work tags (e.g. "REGULAR", "BROADCAST_INTENT")
at `Log.d` (DEBUG level). These tags are not sensitive data — they are app-internal scheduling
identifiers with no user PII. However, in a debug build connected to ADB, these log lines are
visible. This is acceptable for a debug-only class and is consistent with the project's existing
debug logging patterns.
