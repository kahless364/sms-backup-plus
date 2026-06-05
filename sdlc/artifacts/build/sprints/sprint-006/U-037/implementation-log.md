---
artifact_type: implementation-log
story_id: "U-037"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-04"
files_changed: 4
files_created: 0
tests_added: 6
tests_passing: 615
---

# Implementation Log: U-037

## Summary
Fixed BUG-005: the `tearDownObserverAndCollector()` method in both `SmsBackupService` and
`SmsRestoreService` previously nulled the `workInfoObserver` reference without calling
`removeObserver()` on the onDestroy path (when `uniqueWorkName` was null). This left the
`observeForever` registration permanently active, causing duplicate `*StateChanged` callbacks
on service restart.

Fix: added a `workInfoLiveData` field to each service, populated when `observeForever` is
called. The teardown method (now no-arg) calls `workInfoLiveData.removeObserver(workInfoObserver)`
when both fields are non-null, then nulls both. This works on ALL teardown paths (terminal state
from WorkInfo observer callback, and onDestroy) without needing the `uniqueWorkName` string.

## Files Modified

| File | Change |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` | Added `workInfoLiveData` field; updated `registerBackupWorkInfoObserver()` to cache LiveData; replaced `tearDownObserverAndCollector(String)` with no-arg version that calls `removeObserver()`; updated 3 call sites |
| `app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` | Same pattern |
| `app/src/test/java/com/zegoggles/smssync/service/SmsBackupServiceTest.java` | Added 3 observer teardown tests |
| `app/src/test/java/com/zegoggles/smssync/service/SmsRestoreServiceTest.java` | Added 3 observer teardown tests |

## Test Results

6 new tests added:

**SmsBackupServiceTest:**
- `onDestroy_withNoObserverRegistered_doesNotThrow` — null-safety on empty teardown
- `onDestroy_nullsWorkInfoObserverField` — fields null after onDestroy (reflection)
- `backupStateChanged_terminalState_nullsObserverFields` — terminal-state teardown path (reflection)

**SmsRestoreServiceTest:**
- `onDestroy_withNoObserverRegistered_doesNotThrow` — null-safety on empty teardown
- `onDestroy_nullsWorkInfoObserverAndLiveDataFields` — fields null after onDestroy (reflection)
- `onDestroy_afterObserverRegistered_nullsObserverAndLiveDataFields` — fields non-null after register, null after destroy (reflection + WorkManager)

Total: 615 tests, 0 failures.

## Regression Results

All pre-existing service tests pass. U-031 WorkInfo observer foreground bridge (registration path, `registerBackupWorkInfoObserver`, `registerRestoreWorkInfoObserver`) is unchanged — only teardown changed.

## Integration Verification

`tearDownObserverAndCollector()` is called from:
- `SmsBackupService.onDestroy()` (line ~130)
- `SmsBackupService.backupStateChanged()` terminal branch (line ~394)
- WorkInfo observer callback inside `SmsBackupService` when `workInfo.getState().isFinished()` (line ~233)
- `SmsRestoreService.onDestroy()` (line ~106)
- WorkInfo observer callback inside `SmsRestoreService` when `workInfo.getState().isFinished()` (line ~198)

All paths now call `removeObserver()` via the cached `workInfoLiveData`.

## Phase Completion Report
---
story_id: "U-037"
phase: "implementation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-006/U-037/implementation-log.md"
story_status: "done"
current_build_phase: "done"
files_changed: [
  "app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java",
  "app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java",
  "app/src/test/java/com/zegoggles/smssync/service/SmsBackupServiceTest.java",
  "app/src/test/java/com/zegoggles/smssync/service/SmsRestoreServiceTest.java"
]
tests_run: 615
tests_passed: 615
errors: []
---
