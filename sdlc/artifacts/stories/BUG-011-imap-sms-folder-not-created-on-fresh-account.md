---
type: bug
status: done
artifact_type: bug
severity: high
priority: high
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-025
  - U-026
  - U-027
platforms: []
tags: []
id: BUG-011
title: 'First backup to a fresh IMAP account fails: "SMS" folder not created/selectable (NONEXISTENT)'
domain: build
origin: qa
---

# BUG-011: First backup to a fresh account fails — "SMS" IMAP folder NONEXISTENT

## Description
On the first backup to a fresh Gmail/IMAP account (no "SMS" label/folder yet), the backup reads the SMS, connects + authenticates to IMAP successfully, then fails opening the target folder: `SELECT "SMS"` returns `NO [NONEXISTENT] Unknown Mailbox: SMS (Failure)`. The create-if-missing path (`K9MailTransport$BackupImapStoreDelegate.createAndOpenFolder`) does not result in a usable, selectable "SMS" folder, so the backup never appends and ends with `currentSyncedItems=0` (and, per BUG-010, mis-advances the watermark).

## Steps to Reproduce
1. Connect a fresh Gmail account (IMAP) with NO "SMS" label.
2. Ensure SMS exist on the device; tap Backup.
3. Observe logcat: auth OK → `K9MailTransport.openFolder` → `createAndOpenFolder` → `SELECT "SMS"` → `NegativeImapResponseException: NO [NONEXISTENT] Unknown Mailbox: SMS`. Backup fails/retries (INV-3) and finishes with 0 items.

## Expected Behavior
On first backup, the "SMS" folder/label is created (IMAP CREATE) and then opened (SELECT) successfully, and messages are appended. (`imap_folder` pref = "SMS".)

## Actual Behavior
`SELECT "SMS"` → NONEXISTENT; backup fails to append. `folder.exists()` correctly returns false (vendored k-9 `ImapFolder.exists()` catches the NONEXISTENT STATUS and returns false), so `createAndOpenFolder` should call `folder.create(HOLDS_MESSAGES)` then `open` — but the open still reports NONEXISTENT, indicating the CREATE did not run, did not succeed, or the new label is not immediately selectable.

## Evidence
- Logcat (emulator-5554, fresh Gmail IMAP): `K9MailTransport$BackupImapStoreDelegate.openFolder(K9MailTransport.java:422)` → `createAndOpenFolder(K9MailTransport.java:487)` → `ImapFolder.open` → `internalOpen` → `SELECT "SMS"` → `NegativeImapResponseException: NO [NONEXISTENT] Unknown Mailbox: SMS`.
- `mail/transport/K9MailTransport.java:479-493` (`createAndOpenFolder`): `if (!folder.exists()) { create(HOLDS_MESSAGES); } open(OPEN_MODE_RW);` — marked verbatim from original `BackupImapStore.createAndOpenFolder`.
- Vendored `k9mail-vendored/.../ImapFolder.java:272-288` (`exists()`): STATUS → on NegativeImapResponseException returns false (correct).
- The expected `Log.i(TAG, "Label 'SMS' does not exist yet. Creating.")` (K9MailTransport:484) was not observed before the failure — needs confirmation whether `create()` ran (enable k-9 IMAP debug logging to capture the CREATE command + response).

## Acceptance Criteria
- [ ] AC-1: First backup to a fresh account creates the "SMS" folder/label and successfully appends messages (no NONEXISTENT failure).
- [ ] AC-2: Determine and fix the root cause: confirm whether `folder.create()` is invoked and what the IMAP CREATE response is on Gmail; ensure create-then-select yields a selectable folder (handle any create/propagation/namespace nuance).
- [ ] AC-3: Idempotent on subsequent backups (folder already exists → open succeeds, no duplicate create error).
- [ ] AC-4: Regression coverage where feasible (the IMAP path is integration-level; at minimum verify on-device against a fresh account). Build green.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `mail/transport/K9MailTransport.java:479-493` (`createAndOpenFolder`) | create-if-missing then open; open returns NONEXISTENT on fresh account | ensure CREATE runs + succeeds + folder is selectable before SELECT |
| `k9mail-vendored/.../ImapFolder.java` (create/open) | vendored k-9 create/open behavior | investigate Gmail CREATE handling if needed |

## Existing Behavior to Preserve
- Subsequent backups to an existing "SMS" folder must continue to work; the ACL boundary (no k-9 types in service.*) preserved; configurable folder name (`imap_folder`) honored.

## Verification Steps
1. Enable k-9 IMAP debug logging; tap Backup on a fresh account; capture the CREATE/SELECT exchange.
2. After fix: first backup creates the label and appends; verify messages appear under the "SMS" label in the account; second backup is idempotent.

## Root Cause Analysis
<!-- Filled during planning phase; needs IMAP-command-level trace -->

## Technical Context
- Proximate cause of the backup failure observed in live testing. createAndOpenFolder is marked verbatim from the original; the regression (if any) may be in the vendored k-9 (U-027) create/open behavior or the modernized openFolder wrapping (U-025/U-026), or a Gmail-specific create/propagation nuance. Recommend a focused diagnosis (IMAP debug logging) before the fix.

## Supporting Documentation
- Live diagnosis 2026-06-05.

## Notes
Logged; deeper diagnosis (IMAP debug logging) and fix deferred per user. Blocks first backup to a fresh account.
