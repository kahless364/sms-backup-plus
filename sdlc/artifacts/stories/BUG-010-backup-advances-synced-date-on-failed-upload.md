---
type: bug
status: ready
artifact_type: bug
severity: critical
priority: high
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-015
  - U-016
platforms: []
tags: []
id: BUG-010
title: Backup advances max_synced_date even when the IMAP upload failed (silent skip / data loss)
domain: build
origin: qa
---

# BUG-010: Backup advances max_synced_date despite a failed upload

## Description
A manual backup that **fails before any message is successfully appended to IMAP** still advances the persisted `max_synced_date` watermark to ~now. Because subsequent backups only consider messages with `date > max_synced_date`, every previously-read message is then treated as "already backed up" and **silently skipped forever** — even though it was never uploaded. The user sees "nothing to backup" and believes their messages are safe when they are not. This is a silent data-loss class bug.

## Steps to Reproduce
1. Fresh install + connected IMAP account where the backup will fail at the folder step (see BUG-011), with SMS present on the device.
2. Tap Backup. Backup reads the messages, fails to open/create the IMAP "SMS" folder (no append succeeds), state ends `FINISHED_BACKUP` with `currentSyncedItems=0`.
3. Tap Backup again → `BackupWorker: nothing to backup`.
4. Inspect prefs: `max_synced_date` has advanced to ~now despite zero successful appends.

## Expected Behavior
`max_synced_date` (and per-type watermarks) advance **only after** the corresponding messages are confirmed appended to IMAP. A backup that fails (folder/login/append error) must NOT advance the watermark, so the messages are retried on the next backup.

## Actual Behavior
After failed backups (folder NONEXISTENT, no append), `max_synced_date` = 1780636365293 (~now) while `max_synced_date_mms` = -1. Messages are now skipped.

## Evidence
- App prefs (`run-as` `shared_prefs/com.zegoggles.smssync_preferences.xml`): `<long name="max_synced_date" value="1780636365293" />` after backups that only ever logged folder failures + `currentSyncedItems=0`.
- Logcat: backups failed at `K9MailTransport.openFolder` (NONEXISTENT "SMS"); no `appendMessages` success; yet later runs log `BackupWorker: nothing to backup`.
- Engine: `service/BackupWorker.kt` (backup loop, `setMaxSyncedDate` per type) — verify where/when the watermark is written relative to a confirmed append.

## Acceptance Criteria
- [ ] AC-1: A backup that fails before/while appending (folder error, login error, append error, cancellation) does NOT advance `max_synced_date` / per-type watermarks.
- [ ] AC-2: The watermark advances only for messages whose IMAP append is confirmed successful (ideally per successful batch, so a mid-run failure still commits the messages that DID upload and retries the rest).
- [ ] AC-3: Regression test: simulate an append/folder failure and assert the watermark is unchanged; simulate a success and assert it advances to the max appended message date.
- [ ] AC-4: After the fix, re-running a backup that previously failed re-attempts the same messages (no silent skip). Build green; jacoco ≥70%.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `service/BackupWorker.kt` (backup loop / setMaxSyncedDate) | watermark advances even when append did not succeed | advance watermark only after confirmed append (per batch); never on failure/cancel |

## Existing Behavior to Preserve
- Per-batch progress + the INV-3 backoff/retry; checkpoint behavior (U-016); successful backups still advance the watermark correctly so messages aren't re-uploaded.

## Verification Steps
1. Induce a backup failure with SMS present; confirm prefs `max_synced_date` unchanged; re-run after fixing the failure → the messages upload.
2. Unit test the failure and success paths around the watermark write.

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context
- CRITICAL / data-loss class. Likely a regression in the U-015/U-016 CoroutineWorker rewrite (the watermark write ordering relative to append confirmation). Interacts with BUG-011 (the folder failure is what exposed it), but the watermark-on-failure bug is independent and would also mis-fire on login/append errors.

## Supporting Documentation
- Live diagnosis 2026-06-05.

## Notes
Highest-severity live-testing finding. Logged; fix deferred per user. Until fixed, a failed backup silently marks messages as backed-up.
