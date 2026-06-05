---
artifact_type: review-code
story_id: "U-044"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-06-05"
blockers: 0
warnings: 1
---

# Code Review: U-044

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PASS — no CNTR-* artifacts applicable; MailTransport ACL boundary preserved |
| Test coverage | PASS — all 5 ACs covered; new tests use zero-delay seam correctly |
| Code quality | PASS — clean, well-documented, minimal change surface |

## Verdict: PASS

The implementation correctly widens the create→select retry budget from 3×1000 ms to 5 attempts
with exponential backoff (1s/2s/4s/8s, capped at 8s), totalling a 15 s inter-attempt delay
budget. All five acceptance criteria are met. One latent defect in the backoff formula is noted
as a Warning (not a Blocker) because `MAX_CREATE_OPEN_RETRIES=5` ensures the overflow-prone
shift range is never reached in production.

---

## Findings

### Blockers

None.

### Warnings

**W-1 — Latent long overflow in `getRetryDelayMs(int attempt)` for hypothetically large attempt values**
File: `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java:528`

The formula `base << (attempt - 1)` shifts a `long` by `attempt-1` bits. Java long-shift amounts
are masked to `& 63`, so:
- `attempt=61`: shift=60, raw=`Long.MIN_VALUE` (-9223372036854775808). `Math.min(Long.MIN_VALUE, 8000L)` returns `Long.MIN_VALUE`, which would then be passed to `Thread.sleep(negative)`, throwing `IllegalArgumentException` and bypassing the `MessagingException` wrapper entirely.
- `attempt=62..64`: shift 61–63, raw=0, making `Thread.sleep(0)` a no-op (no backoff at all).

**This is NOT a production risk today** because `MAX_CREATE_OPEN_RETRIES=5` and the method is
called only for attempts 1–4 (the sleep guard is `if (attempt < MAX_CREATE_OPEN_RETRIES)`). The
cap also correctly catches `attempt=5`+ for the formula's safe range. However, any future
increase of `MAX_CREATE_OPEN_RETRIES` beyond ~58 would silently break the delay. Recommend
replacing the shift with a multiplication or adding an explicit `attempt > 0` guard and capping
the shift amount:

```java
// Safe alternative:
long delay = attempt <= 13 ? (base << (attempt - 1)) : cap;
return Math.min(delay, cap);
// or simpler:
long delay = (long)(base * Math.pow(2, attempt - 1));
return Math.min(delay, cap);
```

---

### Observations

**O-1 — Retry count vs. sleep count arithmetic is correctly documented**

The implementation log correctly notes 4 inter-attempt sleeps across 5 attempts (1+2+4+8 = 15 s).
The code confirms this: the sleep is inside `if (attempt < MAX_CREATE_OPEN_RETRIES)`, so the last
attempt (attempt=5) never sleeps. The total budget is 15 s, not 23 s.

**O-2 — `getRetryDelayMs(attempt)` test uses a correct bypass of the zero-delay override**

`getRetryDelayMs_exponentialBackoff_matchesSchedule` correctly creates a minimal subclass that
overrides only `createBackupFolder` (not `getRetryDelayMs`), ensuring the production formula is
exercised directly. This is the right seam design — it does not accidentally test a stub.

**O-3 — Cancellation test mechanism is sound**

`Thread.sleep(0)` does throw `InterruptedException` when the interrupt flag is already set
(verified: JVM specification requires this). The test pattern — override `getRetryDelayMs` to
set the interrupt flag and return 0, then assert `MessagingException("Interrupted...")` is thrown
and the interrupt flag is restored — correctly validates AC-2 without actual sleeping.

**O-4 — Non-NONEXISTENT propagation path is preserved unmodified**

The diff shows no change to the `if (!isNonExistent) { throw e; }` branch at line 605–607.
The test `createAndOpenFolder_openFailsNonNonExistent_propagatesImmediately` asserts
`e.isSameInstanceAs(connectEx)` — verifying the exact exception object is re-thrown, not wrapped.

**O-5 — U-042 watermark invariant is untouched**

`BackupWorker.kt` was not touched. The `openFolder()` call path is:
`K9MailTransport.openFolder()` → `store.openFolder()` → `createAndOpenFolder()` → `openWithRetryAfterCreate()`.
The watermark (`setMaxSyncedDate`) is only written after `appendMessages` returns a confirmed
max-date. If `openWithRetryAfterCreate` exhausts all retries and throws `MessagingException`,
that exception propagates through `transport.openFolder()` → `MailException` translation →
`BackupWorker.backupCursors()`, which exits without calling `setMaxSyncedDate`. AC-3 is satisfied.

**O-6 — Fast path (already-exists) has zero added latency**

`createAndOpenFolder` at line 562–563 calls `folder.open(OPEN_MODE_RW)` directly when
`folder.exists()` is true — `openWithRetryAfterCreate` is never entered on this path.
Verified by both code reading and `createAndOpenFolder_folderExists_exactlyOneOpen_noRetry`.

---

## Patterns Verified

- [x] Follows existing code patterns (package-private seam, inner-class structure, verbatim preservation comments)
- [x] Error handling is appropriate (MessagingException wrapping, interrupt flag restoration)
- [x] Tests cover new functionality (5 new tests covering AC-1..AC-4; TestableBackupImapStoreDelegate updated for new signature)
- [x] No hardcoded values that should be configurable (constants are `/* package */` static finals with Javadoc)
- [x] No unnecessary complexity (change is minimal — 3 constants/method changes, loop body unchanged structurally)

## Integration Verified

- [x] New code is reachable from production entry points — traced: `BackupWorker.backupCursors()` → `transport.openFolder()` → `store.openFolder()` → `createAndOpenFolder()` → `openWithRetryAfterCreate()` → `getRetryDelayMs(attempt)`
- [x] No registries/dispatch maps affected (internal-only change)
- [x] Function signature change (`getRetryDelayMs()` → `getRetryDelayMs(int)`) — only two callers: production code (line 611) and `TestableBackupImapStoreDelegate` (line 435). Both updated and verified.
- [x] No dead code introduced

## Regression Check

Not a replacement/rewrite story. No Capabilities Inventory required. The diff is a targeted
change to two methods and one constant; all surrounding behavior is preserved verbatim.

## Contract Verification

Story frontmatter declares `integration_contracts: []`. No CNTR-* artifacts applicable.
`MailTransport` interface is unchanged; `K9MailTransport.openFolder()` signature is unchanged;
no consumers are affected.
