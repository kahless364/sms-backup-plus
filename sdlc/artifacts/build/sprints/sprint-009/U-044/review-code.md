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
| Test coverage | PASS — 23 tests total, all ACs and all new behaviours covered; pool-drain seam correctly instrumented |
| Code quality | PASS — minimal, idiomatic, well-documented; vendored change is surgical and lock-consistent |

## Verdict: PASS

The complete fix (all three remediations) is correct. The reconnect strategy — `forceFreshConnection` closes the folder then drains the pool so `open()` obtains a brand-new TCP/TLS login — is the right solution for Gmail's session-scoped label-visibility behaviour. Thread-safety of `closePooledConnections()` is correct (synchronized on the same `connections` monitor as `pollConnection()` and `releaseConnection()`). The retry predicate covers both known transitional states (NONEXISTENT and "did not find message count"). The loop is bounded to 6 attempts, interruptible, with correct exponential backoff (1/2/4/8/8s, capped at 8000ms). One latent-overflow warning from the prior unit-level review is carried forward as W-1; it is not a production risk at MAX=6. No new issues found in the integrated tree.

---

## Findings

### Blockers

None.

### Warnings

**W-1 — Latent long overflow in `getRetryDelayMs(int attempt)` for hypothetically large attempt values**
File: `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java:538`

The formula `base << (attempt - 1)` shifts a `long` by `attempt-1` bits. Java long-shift amounts are masked to `& 63`, so for `attempt >= 61` the shift wraps and `Math.min(wrapped_value, 8000L)` can return a negative number, which would then be passed to `Thread.sleep(negative)`, throwing `IllegalArgumentException` outside the `MessagingException` try-catch.

This is NOT a production risk at `MAX_CREATE_OPEN_RETRIES=6`: the sleep guard is `if (attempt < 6)`, so the method is only called for attempts 1–5, where the raw value is 1000–32000 and `Math.min` correctly caps at 8000. The risk exists only if `MAX_CREATE_OPEN_RETRIES` is ever raised above ~62. Recommend capping the shift argument:

```java
// Safe alternative — caps shift at 13 bits (8192000 ms > 8000ms cap):
int shiftBits = Math.min(attempt - 1, 13);
long delay = base << shiftBits;
return Math.min(delay, cap);
```

This is purely defensive and warrants a follow-up, not a blocker.

---

### Observations

**O-1 — `closePooledConnections()` thread-safety is correct and consistent**

`ImapStore` uses a `final LinkedList<ImapConnection> connections` as its pool. All three methods that touch this list — `pollConnection()` (line 337), `releaseConnection()` (line 344), and the new `closePooledConnections()` (line 363) — are synchronized on the `connections` object monitor. The while-loop inside `closePooledConnections` calls `connections.poll()` (not iterator-based traversal) so there is no `ConcurrentModificationException` risk. Exceptions from individual `c.close()` calls are swallowed, which is correct since a closed/broken connection should not abort the drain of the remaining pool. This implementation is idiomatic and minimal — it does not introduce any new fields, state, or monitor scopes.

**O-2 — `folder.close()` before pool drain is correct**

`ImapFolder.close()` (line 213) calls `store.releaseConnection(connection)` under `synchronized (this)`, which places the folder's stale connection back into the pool. `forceFreshConnection` then calls `closePooledConnections()`, which drains and closes that connection along with any other pooled connections. The sequencing — close folder first, then drain pool — is correct: if pool drain happened first, `folder.close()` would release the stale connection BACK into an empty pool, leaving it alive and potentially reused on the next `open()`.

**O-3 — Retry predicate is tight and correctly ordered**

The detection logic (lines 652–654) checks `NONEXISTENT` with `toUpperCase` and `"did not find message count"` with `toLowerCase`. The "did not find message count" string is sourced verbatim from `ImapFolder.open()` (line 126 of `ImapFolder.java`): `throw new MessagingException("Did not find message count during open")`. The production message is mixed-case; using `toLowerCase` for detection is correct and verified against the actual source string. The NONEXISTENT detection uses `toUpperCase` because IMAP RFC 5530 codes are case-insensitive and Gmail may vary their casing. Using `java.util.Locale.US` for both conversions is correct practice for IMAP string matching (avoids locale-specific case folding such as Turkish dotless-i).

**O-4 — No regression on the fast path (folder already exists)**

`createAndOpenFolder` at line 592–594 calls `folder.open(OPEN_MODE_RW)` directly when `folder.exists()` returns true — `openWithRetryAfterCreate` is never entered. `forceFreshConnection` is never called. This is confirmed by `openWithRetryAfterCreate_folderExists_forceFreshConnectionNeverCalled` which asserts `forceFreshConnectionCallCount == 0` and `open()` called exactly once.

**O-5 — Fatal errors still propagate immediately (no regression on non-retryable path)**

The `!isFolderNotReadyYet` branch at lines 655–658 throws `e` directly on the first occurrence for any exception not matching NONEXISTENT or message-count. `forceFreshConnection` is NOT called. This is confirmed by `openWithRetryAfterCreate_fatalError_forceFreshConnectionNeverCalled` which uses `"Connection refused"` — not a substring of either retryable string — and asserts `forceFreshConnectionCallCount == 0`, with the original exception propagated unwrapped.

**O-6 — Drain count invariant is correctly off-by-one from attempt count**

`forceFreshConnection` is called inside `if (attempt < MAX_CREATE_OPEN_RETRIES)` — i.e., only for non-final failed attempts. With `MAX_CREATE_OPEN_RETRIES = 6`: if all 6 attempts fail, the drain happens 5 times (attempts 1–5), not 6. The test `openWithRetryAfterCreate_allAttemptsFail_forceFreshConnectionCalledPerRetry` asserts `forceFreshConnectionCallCount == maxRetries - 1`, which is the correct invariant: no point draining before a final attempt that will be the terminal exception.

**O-7 — Backoff schedule is arithmetically correct for the full 6-attempt range**

Verified by direct arithmetic: `1000L << (attempt-1)` for attempts 1–6 yields 1000, 2000, 4000, 8000, 16000, 32000. `Math.min(..., 8000L)` yields 1000, 2000, 4000, 8000, 8000, 8000. Sleep is suppressed on attempt 6 (the `attempt < MAX_CREATE_OPEN_RETRIES` guard). Total inter-attempt delay budget: attempts 1–5 sleep with delays 1000+2000+4000+8000+8000 = 23000ms. The test `getRetryDelayMs_exponentialBackoff_matchesSchedule` asserts the exact values for attempts 1–6 against the production method (not the zero-delay test stub), providing direct formula coverage.

**O-8 — U-042 watermark invariant is untouched**

`BackupWorker.kt` was not modified (confirmed via `git diff` showing no changes). The watermark (`setMaxSyncedDate`) is only written after a confirmed `appendMessages` return value. If `openWithRetryAfterCreate` exhausts all retries and throws `MessagingException`, that exception propagates through `transport.openFolder()` → `MailException` translation → `BackupWorker.backupCursors()`, which exits without calling `setMaxSyncedDate`. AC-3 is satisfied and carries no regression risk.

**O-9 — Cancellation (AC-2) is correctly implemented and tested**

The interrupt-restoration pattern (lines 673–677): `Thread.currentThread().interrupt()` followed immediately by `throw new MessagingException("Interrupted...", ie)` is the correct idiom. The `MessagingException` wraps the `InterruptedException` as its cause, preserving the causal chain for callers. The test uses `Thread.sleep(0)` with the interrupt flag pre-set — Java specifies that `Thread.sleep` throws `InterruptedException` if the interrupt flag is set at entry, even for a zero-duration sleep, making this a valid and fast test mechanism.

**O-10 — Vendored `closePooledConnections()` does not break other `ImapStore` callers**

`closePooledConnections()` is a new `public` method. It has no callers other than `K9MailTransport.BackupImapStoreDelegate.forceFreshConnection()` in the production tree. The method does not mutate any other state (no `folderCache`, no connection-related fields other than `connections`). `ImapFolder.internalOpen()` calls `store.getConnection()` which calls `createImapConnection()` when the pool is empty — this is the intended post-drain code path. Existing callers of `getConnection()`, `releaseConnection()`, `getFolder()`, `checkSettings()` etc. are not affected.

**O-11 — Implementation log is partially stale (old attempt count) but not misleading**

The implementation-log.md summary section (lines 14–28) describes the initial remediation 1 values (5 attempts, 15s), not the final state (6 attempts, 23s). The Remediation 3 section (lines 194–333) documents the correct final values. This is cosmetic — the summary was written incrementally and not retroactively updated — but anyone reading only the summary would see stale values. The actual code (the authoritative source) reflects the correct final state.

---

## Patterns Verified

- [x] Follows existing code patterns (package-private seam methods, inner-class structure, synchronized-on-field monitor style matches existing `pollConnection`/`releaseConnection`)
- [x] Error handling is appropriate (`MessagingException` wrapping, interrupt flag restoration, swallow of `close()` exceptions during drain)
- [x] Tests cover new functionality — 23 tests in the file; 14 directly test BUG-013 behaviour (pool drain counts, message-count retry, interrupt, fast path zero-drain, fatal-error zero-drain, mixed-phase sequence, case-insensitivity, exponential backoff formula)
- [x] No hardcoded values that should be configurable (`MAX_CREATE_OPEN_RETRIES` is a package-private static final with full Javadoc)
- [x] No unnecessary complexity (change surface is 3 files: 1 new vendored method ~10 lines, 1 new protected method ~2 lines, 1 loop body addition ~5 lines)

## Integration Verified

- [x] New code is reachable from production entry points — traced: `BackupWorker.backupCursors()` → `K9MailTransport.openFolder()` → `BackupImapStoreDelegate.openFolder()` → `createAndOpenFolder()` → `openWithRetryAfterCreate()` → `forceFreshConnection()` → `ImapStore.closePooledConnections()`
- [x] No registries or dispatch maps affected — internal change within `BackupImapStoreDelegate` + one new method on the vendored `ImapStore`
- [x] Function signature change (`getRetryDelayMs()` → `getRetryDelayMs(int)`) — only two callers: production loop (K9MailTransport.java:661) and `TestableBackupImapStoreDelegate` (test:680). Both updated and verified
- [x] No dead code introduced — `closePooledConnections()` is called from `forceFreshConnection()` which is called from `openWithRetryAfterCreate()`; all reachable from the production entry point above
- [x] Integration path documented in implementation-log.md (Remediation 3, lines 327–333)

## Regression Check

Not a replacement/rewrite story. No Capabilities Inventory required. The diff is targeted: 3 production source files changed, all surrounding behaviour (watermark, closeFolders, caching, folder-exists fast path, non-NONEXISTENT propagation) is preserved verbatim and verified.

## Contract Verification

Story frontmatter declares `integration_contracts: []`. No CNTR-* artifacts applicable. The `MailTransport` interface is unchanged. `K9MailTransport.openFolder()` signature is unchanged. No consumers are affected by this change.
