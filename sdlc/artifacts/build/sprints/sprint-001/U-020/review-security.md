---
artifact_type: review-security
story_id: U-020
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# Security Review: U-020

## Summary

The Otto bus removal is a transport-layer swap. No new network calls, storage, or
permission-sensitive operations are introduced. Security posture is improved.

## Findings

### Improvements

1. **Silent swallow eliminated** — the old `App.register()` caught `IllegalArgumentException`
   silently (`catch (IllegalArgumentException ignored) { Log.w(TAG, ignored); }`). This hid
   double-registration bugs at runtime. The new Flow-based collection has no equivalent error
   path; incorrect lifecycle is surfaced as a structured Kotlin compilation or runtime error.

2. **No static mutable state for service tracking** — `SmsBackupService.service` and
   `SmsRestoreService.service` static fields were mutable, accessible from any thread, and
   could produce torn reads. Both deleted; state is now in an immutable `StateFlow.value`
   with thread-safe atomic assignment.

3. **No new permissions** — the lifecycle and coroutines libraries do not require any
   Android permissions.

4. **No new network calls** — all changes are in-process event routing.

5. **`MutableSharedFlow` with `DROP_OLDEST`** — the overflow strategy drops stale events
   rather than blocking. For the UI events in scope (permissions, OAuth2 callbacks, cancel),
   dropping a stale event is safe. No sensitive data is carried in the dropped events.

### No Regressions

- `BrowserAuthResult` (OAuth2 code/error) is still processed identically; routing
  changed from Otto bus to SharedFlow, but the data does not persist or leave the process.
- `OAuth2Token` is accessed only in `MainActivity.onSyncEvent()` after `SyncEvent.OAuth2Callback`
  is received — same point as before.

## Verdict: PASS
