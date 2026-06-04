---
artifact_type: security-review
story_id: "U-031"
verdict: "PASS"
reviewer: "Developer (self-review)"
timestamp: "2026-06-04"
---

# Security Review: U-031 — Remove Legacy AsyncTask Execution Path

## Summary

This story deletes legacy code and migrates execution to WorkManager. The security surface is not expanded. No new permissions, network calls, data paths, or credentials handling is introduced.

## Review Findings

### PASS — No New Permissions

No new Android permissions are declared or requested. The WorkManager workers already had the required permissions for IMAP backup/restore.

### PASS — No Credentials Handling

The `WorkManagerCancelCollector` only interacts with `SyncStateRepository.events` (cancel events) and `WorkManager`. No credentials, tokens, or sensitive data are handled.

### PASS — Cancel Signal Safety

`SyncEvent.Cancel` carries an `Origin` enum (USER or SYSTEM) and a boolean `mayInterruptIfRunning`. The `WorkManagerCancelCollector` collects ANY `SyncEvent.Cancel` event (regardless of origin) and routes to WorkManager's cancellation. This matches the behavior of the old `BackupCancelCollector` which also did not discriminate by origin.

### PASS — WorkManager Process Boundary

WorkManager workers run in the app process. The `WorkInfo` observer receives data via the WorkManager's internal LiveData, not via any cross-process IPC. No security boundary crossing.

### PASS — FULL_WAKE_LOCK Scope

`acquireLocks()` acquires the wake lock with a 10-minute timeout (defined in `ServiceBase`). `releaseLocks()` is called in the terminal state handler — preventing indefinite wake lock hold if the worker completes. The 10-minute cap also prevents battery drain if the observer is never triggered.

### PASS — No Hardcoded Secrets

No hardcoded credentials, API keys, or secrets introduced.

## No Security Issues Found

Implementation is a deletion/migration with no new security surface.
