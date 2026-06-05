---
story: U-044
type: plan
status: approved
verdict: PASS
---

# U-044 Plan: Widen create→select retry window with bounded exponential backoff

## Root Cause

`K9MailTransport.BackupImapStoreDelegate.openWithRetryAfterCreate` uses a fixed retry budget
that is too small for real Gmail label propagation:

- `MAX_CREATE_OPEN_RETRIES = 3` (line 493 in K9MailTransport.java)
- `CREATE_OPEN_RETRY_DELAY_MS = 1000L` (line 499)
- `getRetryDelayMs()` ignores the attempt number and always returns 1000 ms (line 514–516)

Total wait budget = 3 × 1000 ms ≈ 3 s, which is insufficient; Gmail label propagation
takes > 3 s in practice, causing the first backup run to fail with RETRY.

## Chosen Values

| Parameter | Old value | New value |
|---|---|---|
| `MAX_CREATE_OPEN_RETRIES` | 3 | 5 |
| Backoff schedule | fixed 1000 ms | exponential: 1s, 2s, 4s, 8s (after attempts 1, 2, 3, 4) |
| Per-attempt cap | n/a | 8000 ms |
| Total delay budget | ~2 s (3 delays × but only 2 inter-attempt delays since last attempt has no wait) | 1+2+4+8 = 15 s |

**Backoff formula:** `min(1000 × 2^(attempt-1), 8000)` ms

Attempt 1 → open → NONEXISTENT → wait 1 s  
Attempt 2 → open → NONEXISTENT → wait 2 s  
Attempt 3 → open → NONEXISTENT → wait 4 s  
Attempt 4 → open → NONEXISTENT → wait 8 s  
Attempt 5 → open → NONEXISTENT → throw (no wait)  

Total delay on complete failure: 15 s. Common success: resolves within attempts 2–4 (3–7 s).

## Signature Change

`getRetryDelayMs()` → `getRetryDelayMs(int attempt)` to allow per-attempt variation.
Test subclass overrides this to return 0 for all attempts (fast tests).

## Files to Change

1. `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java`
   - Update `MAX_CREATE_OPEN_RETRIES` constant (3 → 5)
   - Remove `CREATE_OPEN_RETRY_DELAY_MS` constant (replaced by formula in `getRetryDelayMs`)
   - Update `getRetryDelayMs()` → `getRetryDelayMs(int attempt)` with exponential formula
   - Update `openWithRetryAfterCreate` to pass `attempt` to `getRetryDelayMs(attempt)`

2. `app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportCreateOpenTest.java`
   - Update `TestableBackupImapStoreDelegate.getRetryDelayMs` to new signature
   - Remove `constants_retryDelay_isPositive` (constant `CREATE_OPEN_RETRY_DELAY_MS` removed)
   - Update `constants_maxRetries_isPositive` assertion (still valid)
   - Update `createAndOpenFolder_allOpenAttemptsFailNonExistent_throwsMessagingException`
     to use dynamic `maxRetries` (already does via constant reference — no change needed)
   - Add test: `getRetryDelayMs_exponentialBackoff_matchesSchedule`
   - Add test: `openWithRetryAfterCreate_interruptedDuringSleep_restoresInterruptFlag`
   - Add test: `createAndOpenFolder_folderExists_noDelayOnFastPath`

## AC Coverage

- AC-1: 5 attempts × exponential backoff → up to 15 s budget → covers Gmail propagation
- AC-2: Bounded (5 attempts max); InterruptedException restores interrupt flag and throws
- AC-3: U-042 watermark logic untouched (BackupWorker not modified)
- AC-4: Fast path (folder exists) → no retry, no delay; non-NONEXISTENT propagates immediately
