---
type: bug
status: done
artifact_type: user-story
priority: high
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-044
title: Widen create-then-select retry window for fresh IMAP label propagation (fix BUG-013)
pipeline: ''
domain: modernization
requirement_source: bug:BUG-013
sprint: '000009'
updated_at: '2026-06-05T20:00:41.673Z'
resolution: done
---

# U-044: Widen the create→select retry window for fresh IMAP label propagation

## Story
As an SMS Backup+ user backing up to a brand-new Gmail label,
I want my first backup to create the label and finish in a single run,
So that I don't see the first backup fail and have to wait for an automatic retry.

## Source
Fixes **BUG-013** (medium) — `sdlc/artifacts/stories/BUG-013-first-backup-to-a-fresh-imap-label-fails-on-the-first-attemp.md`. Follow-on to U-043/BUG-011.

## Acceptance Criteria
- [ ] AC-1: First backup to a fresh label succeeds within a single backup run in the common case. Increase the post-CREATE `SELECT` retry budget from the current 3×1000 ms (~3 s) to more attempts with exponential backoff up to a bounded cap (~15–30 s total), so Gmail's label propagation completes before the run gives up.
- [ ] AC-2: The retry stays bounded (no unbounded loop, no indefinite worker block) and is cancellable — it must respect coroutine cancellation / thread interruption (restore the interrupt flag; abort promptly on cancel).
- [ ] AC-3: The U-042 invariant is preserved across the slower path — a run that still fails does NOT advance the watermark; a success advances it exactly once to the confirmed message date; no duplicate appends.
- [ ] AC-4: Idempotent fast path unchanged — when the folder already exists, `open()` is immediate with NO added latency (the extended retry applies ONLY after a CREATE on a NONEXISTENT response). Non-NONEXISTENT errors still propagate immediately (no masking). Unit tests updated for the new attempt-count/backoff (bounded, cancellable, NONEXISTENT-only, no delay on existing folder); build green (assembleDebug, testDebugUnitTest, jacoco per-package LINE ≥70%). On-device: first backup to a fresh label succeeds in one run.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java` (`openWithRetryAfterCreate`, `MAX_CREATE_OPEN_RETRIES`, `getRetryDelayMs`) | 3 attempts × fixed 1000 ms (~3 s) after CREATE; insufficient for Gmail label propagation → first run fails | increase attempt count + exponential backoff to a bounded cap (~15–30 s total); keep cancellable and NONEXISTENT-only; preserve the immediate fast path when the folder already exists |
| `app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportCreateOpenTest.java` | tests assert 3×1s retry behavior | update for the new attempt count / backoff schedule; add cancellation + no-delay-on-existing assertions |

## Existing Behavior to Preserve
- U-043 create-then-open behavior; NONEXISTENT-only retry guard (other errors propagate immediately).
- U-042 watermark-only-on-confirmed-append invariant; no duplicate appends.
- Idempotent fast path when the folder already exists (no added latency).
- MailTransport ACL boundary (no k-9 types in `service.*`); configurable folder name honored.
- Overall backup still completes in reasonable time; the longer wait only applies to the genuine fresh-label CREATE case.

## Verification Steps
1. AC-1/AC-4 (unit, in-worktree): test that after a CREATE the `open()` is retried with the new bounded exponential schedule and eventually succeeds; that an already-existing folder opens with no retry/delay; that non-NONEXISTENT errors are not retried; that cancellation/interruption aborts promptly.
2. AC-1 (on-device, post-merge): set the Gmail label / IMAP folder to a brand-new name; ensure ≥1 SMS qualifies; tap BACKUP; confirm it completes in ONE run (`backedUp≥1`, watermark advances) without a `Worker result RETRY` cycle.

## Technical Context
- Discovered in sprint-008 live testing: against real Gmail, a freshly CREATEd label is not SELECTable within 3 s, so the first run fails and only the WorkManager retry (~38 s later) succeeds. The fix direction (create + retry-on-NONEXISTENT) from U-043 is correct; only the retry budget needs widening. Use a `getRetryDelayMs(attempt)` exponential schedule (e.g. 1s, 2s, 4s, 8s … capped) and a higher `MAX_CREATE_OPEN_RETRIES`, keeping the total bounded (~15–30 s) and the wait interruptible. Because of U-042 there is no data-loss risk; this is a first-run UX/robustness improvement.

## Supporting Documentation
- `sdlc/artifacts/stories/BUG-013-first-backup-to-a-fresh-imap-label-fails-on-the-first-attemp.md`
- Related: U-043 (BUG-011 create-folder fix), U-042 (watermark), U-016 (durable checkpoint).

## Notes
Single-story sprint (sprint-009). Touches only `K9MailTransport.java` + its create/open test. Retry tuning, not a structural change.
