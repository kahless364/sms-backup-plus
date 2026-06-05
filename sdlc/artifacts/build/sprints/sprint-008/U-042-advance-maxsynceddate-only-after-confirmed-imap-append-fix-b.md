---
type: bug
status: planned
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
id: U-042
title: Advance max_synced_date only after confirmed IMAP append (fix BUG-010)
pipeline: ''
domain: modernization
requirement_source: bug:BUG-010
sprint: '000008'
---

# U-042: Advance max_synced_date only after a confirmed IMAP append

## Story
As an SMS Backup+ user,
I want the backup watermark (`max_synced_date`) to advance only for messages that were actually uploaded to IMAP,
So that a failed backup never marks un-uploaded messages as "backed up" and silently skips them forever.

## Source
Fixes **BUG-010** (critical — silent data loss) — `sdlc/artifacts/stories/BUG-010-backup-advances-synced-date-on-failed-upload.md`.

## Acceptance Criteria
- [ ] AC-1: A backup that fails before/while appending (folder error, login error, append error, cancellation) does NOT advance `max_synced_date` or any per-type watermark.
- [ ] AC-2: The watermark advances only for messages whose IMAP append is confirmed successful. Advance per successful batch (so a mid-run failure still commits the messages that DID upload and retries the rest), and to the max date among confirmed-appended messages — never ahead of unconfirmed ones.
- [ ] AC-3: Regression tests: (a) simulate an append/folder failure → assert the watermark is unchanged; (b) simulate a successful batch → assert it advances to the max appended message date; (c) simulate partial success (batch 1 ok, batch 2 fails) → assert watermark = max date of batch 1 only.
- [ ] AC-4: After the fix, re-running a backup that previously failed re-attempts the same messages (no silent skip). Build green (assembleDebug, testDebugUnitTest, jacoco per-package LINE ≥70%). On-device round-trip confirmation deferred to post-merge validation.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` (backup loop / `setMaxSyncedDate`) | Advances the watermark even when no append succeeded (e.g., folder NONEXISTENT, 0 items) | Advance the watermark only after a confirmed successful append, per batch; never on failure/cancellation |
| (investigate) backup task / append path it calls (`BackupTask` / `BackupItemsFetcher` / repo writing `MAX_SYNCED_DATE`) | watermark write may be decoupled from append confirmation | ensure the watermark write is gated on append success and threaded from the actual appended-message dates |

## Existing Behavior to Preserve
- Per-batch progress reporting and the INV-3 backoff/retry behavior.
- Durable restore/backup checkpoint behavior (U-016).
- Successful backups still advance the watermark correctly so already-uploaded messages are not re-uploaded on the next run (no duplicate uploads).
- Per-type watermarks (SMS / MMS / calllog) remain independent.

## Verification Steps
1. AC-1/AC-3 (unit, in-worktree): inject a transport/folder that throws on append (or returns 0 successful appends); run the backup loop; assert the persisted `max_synced_date` is unchanged. Then inject a success and assert it advances to the max appended message date. Add the partial-success case.
2. AC-4 (on-device, post-merge, with U-043 folder-create in place): induce a real backup, confirm the watermark advances only to uploaded messages; induce a failure (e.g., bad folder) and confirm the watermark does NOT advance and the next backup re-attempts.

## Technical Context
- CRITICAL / data-loss class. Likely a regression in the U-015/U-016 CoroutineWorker rewrite — the watermark-write ordering relative to append confirmation. Interacts with BUG-011/U-043 (the folder failure is what exposed it) but is independent: it would also mis-fire on login/append errors. The correct invariant is "watermark follows confirmed durable state," matching the checkpoint philosophy of U-016.
- Prefer advancing the watermark from the actual appended messages' dates (not wall-clock "now"), so an interrupted run is exactly recoverable.

## Supporting Documentation
- `sdlc/artifacts/stories/BUG-010-backup-advances-synced-date-on-failed-upload.md`
- Related stories: U-015 (CoroutineWorker rewrite), U-016 (durable checkpoint).

## Notes
Highest-severity item in the sprint. Disjoint file set from U-041 (MainActivity.java) and U-043 (K9MailTransport.java) — parallel-eligible. Full validation of the success path depends on U-043 landing (a backup must be able to succeed), so on-device confirmation is post-merge.
