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
id: U-043
title: Create IMAP SMS folder on first backup to a fresh account (fix BUG-011)
pipeline: ''
domain: modernization
requirement_source: bug:BUG-011
sprint: '000008'
---

# U-043: Create the IMAP "SMS" folder on first backup to a fresh account

## Story
As a new SMS Backup+ user connecting a fresh Gmail/IMAP account,
I want my first backup to create the "SMS" label/folder and upload my messages,
So that backing up to a brand-new account works without manually pre-creating the folder.

## Source
Fixes **BUG-011** (high) — `sdlc/artifacts/stories/BUG-011-imap-sms-folder-not-created-on-fresh-account.md`.

## Acceptance Criteria
- [ ] AC-1: First backup to a fresh account (no "SMS" label) creates the folder/label (IMAP CREATE) and then opens it (SELECT) successfully and appends messages — no `NO [NONEXISTENT] Unknown Mailbox` failure.
- [ ] AC-2: Root cause determined and fixed: confirm whether `folder.create()` is invoked and what the IMAP CREATE response is; ensure create-then-select yields a selectable folder (handle Gmail label create/propagation/namespace nuances, including the case where SELECT immediately after CREATE must succeed or be retried).
- [ ] AC-3: Idempotent on subsequent backups — folder already exists → open succeeds with no duplicate-create error; the configurable folder name (`imap_folder`, default "SMS") is honored.
- [ ] AC-4: Regression coverage where feasible at unit level (e.g., `createAndOpenFolder` calls `create()` when `exists()` is false, then `open()`; idempotent when `exists()` is true). The full IMAP exchange is integration-level → on-device confirmation against a fresh account is deferred to post-merge validation. Build green (assembleDebug, testDebugUnitTest, jacoco per-package LINE ≥70%).

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java:479-493` (`createAndOpenFolder`) | `if (!folder.exists()) { create(HOLDS_MESSAGES); } open(OPEN_MODE_RW);` — open returns NONEXISTENT on a fresh account (CREATE apparently not run/succeeded, or new label not immediately selectable) | Ensure CREATE actually runs and succeeds before SELECT; handle the create→select gap (verify/retry select; correct folder type/namespace for Gmail labels) |
| `k9mail-vendored/.../ImapFolder.java` (create/open/exists) | vendored k-9 create/open behavior; `exists()` correctly returns false on NONEXISTENT | Investigate Gmail CREATE handling only if the root cause is in the vendored layer; keep changes minimal and within the vendored module boundary |

## Existing Behavior to Preserve
- Subsequent backups to an existing "SMS" folder continue to work (idempotent open).
- The MailTransport ACL boundary (no k-9 types leak into `service.*`) — U-025/U-030.
- Configurable folder name (`imap_folder`) honored; non-default folder names also created when missing.
- All other transport behavior (auth, append, IDLE/poll) unchanged.

## Verification Steps
1. AC-2 diagnosis (in-worktree where possible): add/inspect logging around `createAndOpenFolder`; unit-test that `create()` is invoked when `exists()==false` and that `open()` follows. If the vendored CREATE path is implicated, capture the IMAP command/response shape.
2. AC-1/AC-3 (on-device, post-merge): enable k-9 IMAP debug logging; tap Backup on a fresh Gmail account with no "SMS" label → label created, messages appended, visible under the "SMS" label; second backup is idempotent (no duplicate-create error).

## Technical Context
- Proximate cause of the live-testing backup failure. `createAndOpenFolder` is marked verbatim from the original `BackupImapStore.createAndOpenFolder`; the regression (if any) may be in the vendored k-9 (U-027) create/open behavior, the modernized `openFolder` wrapper (U-025/U-026), or a Gmail-specific create/propagation nuance (Gmail maps IMAP folders to labels; a freshly created label may need a re-SELECT or `\HasNoChildren` handling). Recommend a focused diagnosis before the fix.
- This story unblocks the success-path validation of U-042 (a backup must be able to succeed to confirm the watermark advances correctly).

## Supporting Documentation
- `sdlc/artifacts/stories/BUG-011-imap-sms-folder-not-created-on-fresh-account.md`
- Related stories: U-025 (MailTransport port/ACL), U-026 (engine rewire to MailTransport), U-027 (k-9 dependency unpin/reproducible).

## Notes
Disjoint file set from U-041 (MainActivity.java) and U-042 (BackupWorker.kt) — parallel-eligible. Touches the vendored `:k9mail-vendored` module only if the root cause is there.
