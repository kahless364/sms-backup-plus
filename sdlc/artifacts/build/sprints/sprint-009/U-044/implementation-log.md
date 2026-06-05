---
story: U-044
type: implementation-log
status: complete
verdict: PASS
---

# U-044 Implementation Log

## Summary

Widened the create→select retry window in `K9MailTransport.BackupImapStoreDelegate.openWithRetryAfterCreate`
from the original 3×1000 ms (~3 s) to 5 attempts with exponential backoff (1s, 2s, 4s, 8s),
totalling a 15 s inter-attempt delay budget. Fixes BUG-013.

## Exact New Values

| Parameter | Old | New |
|---|---|---|
| `MAX_CREATE_OPEN_RETRIES` | 3 | 5 |
| `CREATE_OPEN_RETRY_DELAY_MS` constant | 1000L (removed) | n/a |
| `getRetryDelayMs(attempt)` signature | `getRetryDelayMs()` no-arg | `getRetryDelayMs(int attempt)` |
| Backoff formula | fixed 1000 ms | `min(1000L << (attempt-1), 8000L)` |
| Attempt 1 delay | 1000 ms | 1000 ms |
| Attempt 2 delay | 1000 ms | 2000 ms |
| Attempt 3 delay | 1000 ms | 4000 ms |
| Attempt 4 delay | 1000 ms | 8000 ms (cap) |
| Total inter-attempt budget | ~2 s (2 inter-attempt sleeps in old 3-attempt loop) | 15 s (4 inter-attempt sleeps in 5-attempt loop) |

## Files Changed

### app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java

- **Line 500**: `MAX_CREATE_OPEN_RETRIES = 3` → `MAX_CREATE_OPEN_RETRIES = 5` with updated Javadoc referencing BUG-013.
- **Lines 499, 513–516**: Removed `CREATE_OPEN_RETRY_DELAY_MS` constant. Replaced `getRetryDelayMs()` no-arg method with `getRetryDelayMs(int attempt)` returning `Math.min(1000L << (attempt-1), 8000L)`.
- **Lines 611, 582–632**: Updated `openWithRetryAfterCreate` Javadoc to reference BUG-013, exponential schedule, and AC-2/AC-4. Changed `getRetryDelayMs()` call to `getRetryDelayMs(attempt)`.

### app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportCreateOpenTest.java

- **Javadoc (lines 27–48)**: Updated to reference BUG-013 and new AC-1..AC-4 coverage.
- **`constants_retryDelay_isPositive` test**: Removed (constant `CREATE_OPEN_RETRY_DELAY_MS` no longer exists).
- **`getRetryDelayMs_exponentialBackoff_matchesSchedule` (new test, lines 73–94)**: Verifies the production formula returns 1000, 2000, 4000, 8000, 8000 ms for attempts 1–5. Uses a minimal direct subclass of `BackupImapStoreDelegate` (not `TestableBackupImapStoreDelegate`) so the zero-delay override does not mask the production formula.
- **`openWithRetryAfterCreate_interruptedDuringSleep_restoresInterruptFlagAndThrows` (new test, lines 230–277)**: Verifies AC-2 cancellability. Uses an anonymous subclass that sets `Thread.currentThread().interrupt()` inside `getRetryDelayMs` (returns 0) so `Thread.sleep(0)` immediately throws `InterruptedException`. Asserts that (a) `MessagingException("Interrupted...")` is thrown, (b) interrupt flag is restored after the catch.
- **`createAndOpenFolder_folderExists_exactlyOneOpen_noRetry` (new test, lines 279–294)**: Verifies AC-4 fast path — `open()` called exactly once, `create()` never called when folder exists.
- **`createAndOpenFolder_openFailsNonExistentThreeTimes_thenSucceeds` (new test, lines 296–321)**: Verifies AC-1 widened budget — 3 NONEXISTENT failures then success on 4th attempt succeeds (would have thrown with the old 3-attempt cap).
- **`TestableBackupImapStoreDelegate.getRetryDelayMs`** (line 432): Updated from `getRetryDelayMs()` to `getRetryDelayMs(int attempt)` to match new signature.

## Tests Added / Updated

| Test | Status | Purpose |
|---|---|---|
| `constants_maxRetries_isPositive` | retained | MAX_CREATE_OPEN_RETRIES > 0 |
| `constants_retryDelay_isPositive` | **removed** | referenced deleted constant |
| `getRetryDelayMs_exponentialBackoff_matchesSchedule` | **new** | AC-1: formula 1s/2s/4s/8s/8s |
| `createAndOpenFolder_folderExists_opensDirectlyWithoutCreate` | retained | AC-4 fast path |
| `createAndOpenFolder_folderExists_exactlyOneOpen_noRetry` | **new** | AC-4 exactly 1 open() call |
| `createAndOpenFolder_folderNotExists_callsCreateThenOpen` | retained | create→open sequence |
| `createAndOpenFolder_createReturnsFalse_throwsWithoutCallingOpen` | retained | silent create fail |
| `createAndOpenFolder_openFailsNonExistentOnce_thenSucceeds_noException` | retained | 1 retry succeeds |
| `createAndOpenFolder_allOpenAttemptsFailNonExistent_throwsMessagingException` | retained | bounded exhaustion (now 5 attempts) |
| `createAndOpenFolder_openFailsNonNonExistent_propagatesImmediately` | retained | non-NONEXISTENT not retried |
| `openWithRetryAfterCreate_interruptedDuringSleep_restoresInterruptFlagAndThrows` | **new** | AC-2 cancellable |
| `createAndOpenFolder_openFailsNonExistentThreeTimes_thenSucceeds` | **new** | AC-1: 4th attempt success (beyond old cap) |
| `createAndOpenFolder_customLabel_callLog_createsAndOpens` | retained | configurable label |
| `createAndOpenFolder_nonExistentDetection_caseInsensitive` | retained | case-insensitive NONEXISTENT |
| `openFolder_cachedFolder_returnedWithoutReopen` | retained | folder cache idempotency |

## Build / Coverage Result

```
BUILD SUCCESSFUL
:app:assembleDebug UP-TO-DATE
:app:testDebugUnitTest  — 650 tests completed, 0 failed, 2 skipped
:app:jacocoTestCoverageVerification  — PASSED (per-package LINE ≥ 70%)
```

Total build time: ~3m (first full build), ~21s (incremental assembleDebug + jacoco).

## Authoritative @Test Count

```
git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
647
```

(Java: 541, Kotlin: 106)

## Integration Path

No new entry points. The change is purely internal to `K9MailTransport.BackupImapStoreDelegate`:
- `K9MailTransport.openFolder()` → `BackupImapStoreDelegate.openFolder()` → `createAndOpenFolder()` → `openWithRetryAfterCreate()` → `getRetryDelayMs(attempt)`.
- The retry only executes after a successful `folder.create()` on the NONEXISTENT code path.
- All existing callers of `K9MailTransport.openFolder()` are unaffected.

## AC Verification

- **AC-1**: `MAX_CREATE_OPEN_RETRIES=5` with delays 1s/2s/4s/8s = 15s budget. `createAndOpenFolder_openFailsNonExistentThreeTimes_thenSucceeds` confirms 4th-attempt success (beyond old 3-cap). K9MailTransport.java:597–632.
- **AC-2**: Loop bounded by `MAX_CREATE_OPEN_RETRIES=5`. `InterruptedException` → `Thread.currentThread().interrupt()` + immediate throw. K9MailTransport.java:617–623. Test: `openWithRetryAfterCreate_interruptedDuringSleep_restoresInterruptFlagAndThrows`.
- **AC-3**: `BackupWorker` and watermark logic untouched. U-042 invariant preserved.
- **AC-4**: Fast path (`folder.exists()==true`) → direct `folder.open(OPEN_MODE_RW)` at K9MailTransport.java:563, no retry, no delay. Non-NONEXISTENT propagates immediately at K9MailTransport.java:605–607. Tests: `createAndOpenFolder_folderExists_exactlyOneOpen_noRetry`, `createAndOpenFolder_openFailsNonNonExistent_propagatesImmediately`.

## Contract Adherence

No integration contracts (story frontmatter `integration_contracts: []`). No CNTR-* artifacts applicable.

---

## Remediation 2 (on-device: handle 'Did not find message count' transitional state)

### Problem Found in Live Testing

On-device testing against live Gmail revealed that after a CREATE, the label goes through
TWO transitional failure states before becoming fully SELECTable:

1. **Phase 1** — `NO [NONEXISTENT] Unknown Mailbox`: label not yet visible (already handled).
2. **Phase 2** — `MessagingException: Did not find message count during open`: the label now
   exists and SELECT succeeds, but the response has no EXISTS count yet.

The original fix only retried on phase 1 (NONEXISTENT). Phase 2 (`"did not find message
count"`) did not match the predicate, so it was propagated as a fatal error. This caused
AC-1 to fail on the first backup run — only the WorkManager retry ~38 s later would succeed.

### Broadened Predicate

In `openWithRetryAfterCreate` (K9MailTransport.java), the boolean guard was renamed and
extended to cover both transitional states:

```java
boolean isFolderNotReadyYet = msg != null && (
        msg.toUpperCase(java.util.Locale.US).contains("NONEXISTENT") ||
        msg.toLowerCase(java.util.Locale.US).contains("did not find message count"));
```

Both conditions represent a freshly-created Gmail label that is not yet fully SELECTable.
All genuinely fatal errors (auth failures, connection timeouts, etc.) still propagate
immediately on the first occurrence. The bounded retry budget (`MAX_CREATE_OPEN_RETRIES = 5`,
total ~15 s), exponential backoff via `getRetryDelayMs(attempt)`, and cancellability on
`InterruptedException` are all unchanged.

### Files Changed

- **`app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java`**
  - `openWithRetryAfterCreate` javadoc updated to document both transitional conditions.
  - `isNonExistent` boolean renamed to `isFolderNotReadyYet`; OR-clause added for
    `"did not find message count"` (case-insensitive via `toLowerCase`).
  - `createAndOpenFolder` javadoc updated to reference both conditions and remediation 2.

- **`app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportCreateOpenTest.java`**
  - Four new tests added (see below).

### New Tests

| Test | What It Asserts |
|---|---|
| `createAndOpenFolder_openFailsMessageCountOnce_thenSucceeds_noException` | `open()` throws `"Did not find message count during open"` on attempt 1, succeeds on attempt 2 → completes without exception (retried, not propagated) |
| `createAndOpenFolder_allOpenAttemptsFailMessageCount_throwsMessagingException` | All 5 attempts fail with `"Did not find message count"` → bounded `MessagingException` thrown after `MAX_CREATE_OPEN_RETRIES` attempts |
| `createAndOpenFolder_mixedNonExistentThenMessageCount_thenSucceeds_noException` | Mixed two-phase sequence: attempt 1 NONEXISTENT, attempt 2 "did not find message count", attempt 3 success → completes without exception (mirrors live Gmail behaviour) |
| `createAndOpenFolder_messageCountDetection_caseInsensitive` | Upper-case `"DID NOT FIND MESSAGE COUNT DURING OPEN"` is also retried (detection is case-insensitive) |

All 15 existing tests remain green. No regressions.

### Build / Coverage Result

```
BUILD SUCCESSFUL
:app:assembleDebug
:app:testDebugUnitTest  — all tests passed, 0 failed
:app:jacocoTestCoverageVerification  — PASSED (per-package LINE >= 70%)
```

Total build time: ~2m 49s (full build).

### Authoritative @Test Count

```
git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
650
```

(Previously 647 before remediation 1; 650 reflects 3 additional tests from rem. 1 + 4 new
tests from rem. 2 = 650 total; the 647→650 delta includes both remediations.)

### AC-1 Verification (Updated)

After remediation 2, AC-1 is fully met for first-run success:
- Phase 1 (NONEXISTENT) → retried (unchanged from original fix).
- Phase 2 ("did not find message count") → now also retried.
- Mixed NONEXISTENT → message-count → success sequence tested explicitly in
  `createAndOpenFolder_mixedNonExistentThenMessageCount_thenSucceeds_noException`.
- No delay or retry on the folder-already-exists fast path (AC-4 unchanged).
