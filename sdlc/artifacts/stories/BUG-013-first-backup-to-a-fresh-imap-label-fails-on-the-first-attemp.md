---
type: bug
status: done
artifact_type: bug
severity: medium
priority: high
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items: []
platforms: []
tags: []
id: BUG-013
title: 'First backup to a fresh IMAP label fails on the first attempt: create-then-select retry window (3x1s) too short for Gmail label propagation'
domain: build
origin: qa
---

# BUG-013: First backup to a fresh IMAP label fails on the first attempt (create→select retry window too short)

## Description
The U-043 fix (BUG-011) makes `K9MailTransport.createAndOpenFolder` CREATE a missing label and then retry `SELECT` on a NONEXISTENT response, to bridge Gmail's label-propagation gap. In live testing against Gmail, the retry window — **3 attempts × 1000 ms = ~3 s** — is too short: after a successful `CREATE`, Gmail does not make the new label SELECTable within 3 s, so the **first backup attempt still fails** (`MailException: Did not find message count during open`) and the WorkManager job returns RETRY. The backup only succeeds on the next WorkManager retry (~38 s later). Because of the U-042 fix, no data is lost (the watermark does not advance on the failed attempt and the message is retried), but the **first manual backup to a brand-new label visibly fails**, which is a poor first-run experience and partially re-exposes the original BUG-011 symptom ("first backup to a fresh account fails").

## Steps to Reproduce
1. On an emulator/device with the app configured for a Gmail/IMAP account, set the Gmail label / IMAP folder to a brand-new name that does not exist in the mailbox (e.g. `SMS_FRESH_TEST`).
2. Ensure at least one SMS qualifies for backup (date > max_synced_date).
3. Tap BACKUP and watch logcat.
4. Observe: `Label '<name>' does not exist yet. Creating.` → `SELECT "<name>"` → `NO [NONEXISTENT]` → `not selectable yet after CREATE (attempt 1/3); retrying in 1000 ms` → after 3 attempts → `MailException: Did not find message count during open` → `Worker result RETRY`. The first attempt fails; success only comes on a later WorkManager retry.

## Expected Behavior
The first backup to a fresh label creates the label and completes the append within that same backup run (or with enough in-run retry/backoff that first-run success is the common case), so the user does not see the first backup fail.

## Actual Behavior
First attempt exhausts the 3×1 s retry and fails (`Did not find message count during open`); the watermark correctly does not advance (U-042); success occurs only on the subsequent WorkManager retry tens of seconds later.

## Evidence
- Live logcat (emulator-5554, API 37, live Gmail, folder `SMS_U043_LIVE`, 2026-06-05):
  ```
  I SMSBackup+: Label 'SMS_U043_LIVE' does not exist yet. Creating.
  E k9: NegativeImapResponseException: Command: SELECT "SMS_U043_LIVE"; response: [NO, [NONEXISTENT], Unknown Mailbox: SMS_U043_LIVE (Failure)]
        at K9MailTransport$BackupImapStoreDelegate.openWithRetryAfterCreate(K9MailTransport.java:575)
  W SMSBackup+: Folder 'SMS_U043_LIVE' not selectable yet after CREATE (attempt 1/3); retrying in 1000 ms
  W SMSBackup+: MailException: ... Did not find message count during open
  I WM-WorkerWrapper: Worker result RETRY ...
  ... ~38 s later (WorkManager retry) ...
  I SMSBackup+: BackupWorker: backup complete, backedUp=1
  ```

## Acceptance Criteria
- [ ] AC-1: First backup to a fresh label succeeds within a single backup run in the common case — increase the create→select retry budget (more attempts and/or exponential backoff to a sensible cap, e.g. ~15–30 s total) so Gmail label propagation completes before the run gives up.
- [ ] AC-2: The retry remains bounded (no unbounded loop / no indefinite worker block); the wait is interruptible/cancellable and respects coroutine cancellation.
- [ ] AC-3: No data loss and no duplicate appends across the slower path (U-042 invariant preserved): a still-failing run does not advance the watermark; a success advances it exactly once to the confirmed message date.
- [ ] AC-4: Idempotent on existing labels (no added latency when the folder already exists — the longer retry only applies after a CREATE on NONEXISTENT). Unit tests updated for the new attempt count/backoff; build green (jacoco ≥70%). On-device: first backup to a fresh label succeeds in one run.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java` (`openWithRetryAfterCreate`, `MAX_CREATE_OPEN_RETRIES`, `getRetryDelayMs`) | 3 attempts × 1000 ms fixed (~3 s) after CREATE; insufficient for Gmail | increase attempts + exponential backoff to a bounded cap (~15–30 s); keep cancellable; only on post-CREATE NONEXISTENT |

## Existing Behavior to Preserve
- U-043 create-then-open behavior and the NONEXISTENT-only retry guard (non-NONEXISTENT errors still propagate immediately).
- U-042 watermark-only-on-confirmed-append invariant; no duplicate appends.
- Idempotent fast path when the folder already exists (no extra latency).
- MailTransport ACL boundary; configurable folder name.

## Verification Steps
1. Set the IMAP folder to a brand-new label; ensure ≥1 SMS qualifies; tap BACKUP; confirm the backup completes in ONE run (`backedUp≥1`, watermark advances) without a RETRY cycle.
2. Confirm a folder that already exists still opens immediately (no added delay).
3. Unit-test the new attempt-count/backoff (bounded, cancellable, NONEXISTENT-only).

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context
- Follow-on to U-043/BUG-011. The fix direction (create + retry-on-NONEXISTENT) is correct; only the retry budget needs tuning for real Gmail propagation latency. WorkManager retry already guarantees eventual success + no data loss (thanks to U-042), so this is a first-run UX/robustness fix, not a data-integrity fix.

## Supporting Documentation
- sprint-008 live testing 2026-06-05; U-043 (BUG-011 fix), U-042 (watermark), U-016 (durable checkpoint).

## Notes
Medium severity. Logged during sprint-008 guided exploratory testing; user requested a fix now (sprint-009).
