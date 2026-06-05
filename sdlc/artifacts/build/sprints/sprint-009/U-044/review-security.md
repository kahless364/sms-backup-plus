---
artifact_type: review-security
story_id: "U-044"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-06-05"
blockers: 0
warnings: 0
---

# Security Review: U-044

## Review Summary

U-044 is a pure retry-timing change: `MAX_CREATE_OPEN_RETRIES` raised from 3 to 5 and `getRetryDelayMs()` replaced with `getRetryDelayMs(int attempt)` returning an exponential backoff formula (`min(1000L << (attempt-1), 8000L)`). No structural, protocol, authentication, or data-handling code is touched. All four security-relevant areas (loop boundedness, cancellation, TLS/trust-policy integrity, log content) pass verification against the actual source on disk.

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

None.

### Informational Notes

All four delegated security questions were verified against the actual file at
`app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java`.

**1. Loop bounded — no DoS/hang risk**

The retry loop at lines 597-631 is a counted `for` loop with the upper bound fixed at
`MAX_CREATE_OPEN_RETRIES = 5` (line 500). It can iterate at most 5 times. The total
inter-attempt sleep budget is 1+2+4+8 = 15 s (4 sleeps; the 5th attempt receives no sleep
because `attempt < MAX_CREATE_OPEN_RETRIES` is false on the last iteration — line 610).
The loop cannot run indefinitely regardless of server behaviour because: (a) it is counted,
not `while(true)`; (b) any exception that is not NONEXISTENT propagates immediately (line
607); (c) on exhaustion the loop falls through to a deterministic throw at lines 627-631.
Arithmetic overflow in the shift expression is not possible: `1000L << 4 = 16000` fits
comfortably in a `long`; the result is capped by `Math.min` before use.

**2. Cancellation handled correctly**

`Thread.sleep(delayMs)` at line 616 is wrapped in a `try/catch(InterruptedException)`.
On interrupt: line 619 restores the interrupt flag via `Thread.currentThread().interrupt()`,
and line 620-622 throws a `MessagingException` immediately. This satisfies AC-2:
the worker cannot be blocked indefinitely by an external cancellation signal. The
cancellation test `openWithRetryAfterCreate_interruptedDuringSleep_restoresInterruptFlagAndThrows`
(test file line 243) verifies both that the exception is thrown and that the interrupt flag
is restored (line 277 asserts `Thread.currentThread().isInterrupted()` is true).

**3. No TLS / connection-security regression**

The change is confined entirely to the timing of `folder.open()` retry attempts. No
modifications were made to: `buildSocketFactory` (lines 135-149); `TlsTrustPolicy` dispatch
logic; `PinnedCertificateSocketFactory`; `DefaultTrustedSocketFactory`; or the `ImapStore`
constructor call chain. The `resolvedSocketFactory` field is set once at construction and is
not touched by the retry path. CNTR-MODERNIZATION-001 (SYSTEM_VALIDATED / PINNED_CERTIFICATE
dispatch) is unaffected.

**4. No credential or message-content exposure in new log lines**

Two log statements exist in the modified code region:

- Line 553: `Log.i(TAG, "Label '" + label + "' does not exist yet. Creating.")` — logs
  only the IMAP folder label (a user-configured folder name, not a credential). This line
  predates U-044 and was not changed.
- Lines 612-614 (the one log statement added by U-044 in `openWithRetryAfterCreate`):
  `Log.w(TAG, "Folder '" + folder.getName() + "' not selectable yet after CREATE (attempt "
  + attempt + "/" + MAX_CREATE_OPEN_RETRIES + "); retrying in " + delayMs + " ms")`
  — logs folder name (user-configured label), attempt counter, total retries, and delay
  in ms. No credentials, OAuth tokens, message content, UIDs, or server responses are
  interpolated. The `MessagingException` at line 627-631 uses only `folder.getName()`,
  attempt count, and `lastException.getMessage()` (the IMAP server's NONEXISTENT response
  string, which contains no user data — only the folder name already logged).

The credential-masking path (`getStoreUriForLogging` lines 461-476) is not involved in
the retry code path and remains unchanged.

**5. Integer arithmetic safety in `getRetryDelayMs`**

`long delay = base << (attempt - 1)` where `base = 1000L`. For `attempt = 5` (max):
`1000L << 4 = 16_000L`. This is within `long` range. `Math.min(16000L, 8000L) = 8000L`.
No overflow. The `attempt` parameter is always a loop variable in range `[1, MAX_CREATE_OPEN_RETRIES]`
(verified at the call site: line 611 is inside the `for` block, so `attempt` is 1-based and
bounded by the loop invariant). Test `getRetryDelayMs_exponentialBackoff_matchesSchedule`
exercises attempts 1-5 and asserts exact values.

### Security Checklist

- [x] No hardcoded credentials or secrets — no credentials appear anywhere in the changed lines; `getStoreUriForLogging` credential-masking is untouched
- [x] Input validation on all user inputs — not applicable to this change; folder name is user-configured but only used as a log label, not in any security decision
- [x] Output encoding prevents injection attacks — not applicable; the change path has no SQL, HTML, shell, or external command construction
- [x] Authentication tokens handled securely — OAuth/auth code paths are untouched; retry loop is post-authentication
- [x] Authorization checks on all protected resources — not applicable; IMAP authorization is handled by k-9 layer, unchanged
- [x] Sensitive data encrypted at rest and in transit — TLS/socket factory selection is untouched; no new storage writes
- [x] Error messages don't leak internal details — the final `MessagingException` at line 627-631 exposes only: folder name (user-chosen label), attempt count, and the IMAP server's NONEXISTENT error string (no internal state, credentials, or stack trace exposed to callers)
- [x] Dependencies have no known critical vulnerabilities — no new dependencies introduced; this is a pure logic change within an existing file

## Phase Completion Report
---
story_id: "U-044"
phase: "security-review"
verdict: "PASS"
artifact_path: "C:/Code/Android/sms-backup-plus/sdlc/artifacts/build/sprints/sprint-009/U-044/review-security.md"
story_status: "review"
current_build_phase: "validation"
blockers: 0
warnings: 0
errors: []
notes: "Pure retry-timing change. Loop is counted/bounded (5 attempts, 15s max). Interrupt flag correctly restored on cancellation. No TLS/auth changes. New log line contains folder label + counters only — no credentials or message content."
---
