---
artifact_type: code-review
story_id: "U-015"
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# Code Review: U-015

## Summary

BackupWorker.kt and RestoreWorker.kt are clean CoroutineWorker implementations.
The logic faithfully ports BackupTask and RestoreTask with no AsyncTask or Otto
dependencies. The INV-3 backoff cap, cooperative cancellation, and dedup guard
verbatim transplant are all correct.

## Findings

### BackupWorker.kt

- [PASS] Extends CoroutineWorker (not Worker or ListenableWorker)
- [PASS] No `import android.os.AsyncTask` or `extends AsyncTask`
- [PASS] No `import com.squareup.otto`, no App.register/post/unregister, no @Subscribe
- [PASS] INV-3 backoff cap at runAttemptCount: 30 * 2^N > 300s → Result.failure
- [PASS] Cooperative cancellation via coroutineContext.ensureActive() in loop (not isCancelled() polling)
- [PASS] setProgress() emits progress data (replaces publishProgress + App.post)
- [PASS] SKIP path calls fetcher.getMostRecentTimestamp() and setMaxSyncedDate() (no IMAP)
- [PASS] XOAuth2 retry: at most once (config.currentTry < 1 check)
- [PASS] CalendarSyncer called per CALLLOG batch (cursor.type == CALLLOG && calendarSyncer != null)
- [PASS] setMaxSyncedDate per type after append
- [PASS] BackupImapStore.closeFolders() in finally block
- [PASS] BackupCursors.close() in finally block
- [NOTE] wakelock not reproduced: WorkManager manages wakelock for CoroutineWorker; documented as intentional

### RestoreWorker.kt

- [PASS] Extends CoroutineWorker
- [PASS] No AsyncTask or Otto dependencies
- [PASS] INV-3 backoff cap (same formula as BackupWorker)
- [PASS] Cooperative cancellation via ensureActive() in restore loop
- [PASS] setProgress() emits progress data
- [PASS] Early-exit when !restoreSms && !restoreCallLog
- [PASS] smsExists() SQL: "date = ? AND address = ? AND type = ?" — exactly 3 fields, verbatim from RestoreTask.java:309-316
- [PASS] callLogExists() SQL: "date = ? AND number = ? AND duration = ? AND type = ?" — exactly 4 fields, verbatim from RestoreTask.java:291
- [PASS] SMS type filter: MESSAGE_TYPE_INBOX or MESSAGE_TYPE_SENT (avoids re-sending)
- [PASS] XOAuth2 retry passes currentRestoredItem to retryWithStore (resume offset preserved)
- [PASS] Thread update called when smsIds.isNotEmpty()
- [PASS] GC hint: msgs[currentRestoredItem] = null after import
- [PASS] imapStore.closeFolders() in finally block

### WorkManagerScheduler.kt changes

- [PASS] Import updated from `worker.BackupWorker` to `service.BackupWorker`
- [PASS] scheduleRestore() replaced with real RestoreWorker enqueue (OneTimeWorkRequest)
- [PASS] Constraints.NONE for restore (user-initiated, no network constraint needed)
- [PASS] REPLACE semantics preserved

### BackupTriggerWorker.kt

- [PASS] Import updated from `worker.BackupWorker` to `service.BackupWorker`

## Issues

None blocking. Noted deferrals (SmsJobService, BackupTask, RestoreTask deletion) are
correctly documented as U-017 scope per branch-by-abstraction design.
