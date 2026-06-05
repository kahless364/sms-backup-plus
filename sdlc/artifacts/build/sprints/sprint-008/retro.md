---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-008
started: 2026-06-05
completed: 2026-06-05
---

# Retro: sprint-008 — Live-testing bug fixes (BUG-009/010/011)

## Business Summary

**Objective.** Resolve and validate the three bugs found during live device testing: BUG-009 (restore bails on Q+ when the legacy default-SMS API returns null), BUG-010 (critical — backup advances the synced-date watermark even when the upload failed → silent data loss), and BUG-011 (first backup to a fresh IMAP account fails because the "SMS" folder is not created/selectable).

**Outcome.** All three fixed, reviewed (code + security), QA'd, and integrated. 647 tests green, per-package LINE ≥70%, assembleDebug green. Executed with **git worktree isolation** (the recurring worktree-base defect was worked around — see Process Observations).

- **U-041 / BUG-009:** `MainActivity.startRestore()` now requests the SMS role via `RoleManager.createRequestRoleIntent(ROLE_SMS)` on Q+ regardless of the legacy `Telephony.Sms.getDefaultSmsPackage()` value; the "no default package — tablet?" toast is scoped to genuine pre-Q no-telephony cases. A code+security review caught a real **blocker** the developer (and the first QA pass) missed: the `onActivityResult` re-entry guard still gated on the legacy `getSmsDefaultPackage() != null`, which the Q+ branch never sets — so the role dialog appeared but restore never restarted after the grant. Remediated by gating re-entry on `isSmsBackupDefaultSmsApp(this)` on Q+, with a new Robolectric round-trip test. Re-reviewed PASS. **Confirmed on-device:** on emulator-5554 (API 37, `sms_default_application=null`), tapping Restore now shows the "Set SMS Backup+ as your default SMS app?" dialog and granting it results in `cmd role get-role-holders android.app.role.SMS` = `com.zegoggles.smssync`. +5 tests.
- **U-042 / BUG-010 (critical):** `MailTransport.appendMessages` return type changed `void → long` (the max confirmed-appended message date); `BackupWorker` captures that return value and only then calls `setMaxSyncedDate`. The "watermark advances only on confirmed append" invariant is now **compiler-enforced** — any throw in `openFolder`/`appendMessages` makes `setMaxSyncedDate` unreachable. The date is message-derived (DATE headers), never wall-clock, so an interrupted run is exactly recoverable; per-batch commits preserve partial progress. +8 tests (failure → watermark unchanged; success → confirmed date; partial success → batch-1 date only; +2 adapter tests).
- **U-043 / BUG-011:** `K9MailTransport.createAndOpenFolder` now checks the `create()` return value (fail-fast on silent CREATE failure) and retries `open()` up to 3× (1 s back-off) only on a NONEXISTENT response — bridging the Gmail label-propagation gap where a freshly CREATE-d label is not immediately SELECTable. Idempotent on existing folders; configurable folder name honored; ACL boundary and vendored module untouched. +11 tests.

**Key Deliverables.** `MainActivity.java`, `MailTransport.java`, `K9MailTransport.java`, `BackupWorker.kt`; new tests `MainActivityRestoreTest` (Q+ round-trip), `BackupWorkerWatermarkTest`, `K9MailTransportCreateOpenTest`, plus additions to `K9MailTransportTest`/`SmsBackupServiceTest`; 15 workspace artifacts (3 stories × 5, all PASS) + 1 remediation.

**Decisions Made.**
- Watermark advancement is keyed off the transport's confirmed-append return value rather than a separate "success" flag, so the invariant cannot be bypassed by future refactors (compiler-enforced).
- U-043's create→select gap handled at the `K9MailTransport` call site (bounded retry) rather than in the vendored k-9 `ImapFolder`, keeping the vendored module untouched.

## Process Observations

**Process Compliance.** Live-testing bugs → create-stories (U-041/042/043) → create-sprint (sprint-008, Sprint Manager plan + CNTR-PIPELINE-007 execution plan) → execute-sprint (worktree isolation) → wave-1 gate → post-merge validation → retro. Full scaffold/review/finalize lifecycle preserved via the Artifact Librarian.

**Worktree isolation USED (defect worked around).** For the first time in this engagement, stories were executed in isolated git worktrees per the methodology. The standing worktree-base defect (worktrees branch from `origin/master` = `ef97bfa9`, pre-modernization, NOT session HEAD) was diagnosed precisely this time: the harness bases worktrees on the **`origin/master` remote-tracking ref**. Workaround: repoint the *local* `refs/remotes/origin/master` (and local `master`) to the current HEAD before spawning worktree agents (local ref surgery only — nothing pushed). A cheap probe agent confirmed `ANCESTOR_YES` before committing the sprint to worktrees. Every worktree branch was verified with `git merge-base --is-ancestor <HEAD> <branch>` before merge. All four worktree agents (3 stories + 1 remediation) branched correctly. **These refs must be restored to `ef97bfa9` at session end.**

**Cross-story file overlap at merge.** Despite the plan classifying the three stories as "disjoint files," U-042's fix changed `K9MailTransport.java` (appendMessages signature) AND `MailTransport.java`, overlapping U-043's `K9MailTransport.java` change (createAndOpenFolder). Git's `ort` strategy auto-merged cleanly (non-overlapping methods), and the **post-merge integrated build** (645→647 tests) confirmed semantic coexistence. Lesson: a watermark fix that needed a transport-interface change wasn't foreseeable from the bug text alone; the post-merge build is the real integration gate and caught nothing because the auto-merge was sound — but the disjoint-files assumption should be treated as provisional.

**Review caught what dev + QA missed.** The U-041 `onActivityResult` blocker is the headline process win: the developer's tests called `requestDefaultSmsPackageChange()` directly and the first QA pass PASSed on that basis, but the independent Code Reviewer AND Security Reviewer both flagged the broken round-trip. This is the multi-reviewer model working as designed — a single-agent pipeline would have shipped a fix that made the dialog appear but left restore still broken.

**Gate Results.** U-041: code FAIL → remediated → code PASS, security FAIL → remediated → security PASS, QA PASS. U-042: 3/3 PASS. U-043: 3/3 PASS. Wave-1 gate PASS (3 done, 15/15 PASS artifacts + 1 remediation).

**Issues Encountered.**
1. **Closing-bookend status mis-derivation (now 8×).** `completed-with-blockers` emitted with 3 done / 0 blocked; corrected to `completed`.
2. **Agent test-count drift.** Individual agents reported 626/631/634; authoritative merged count verified via `git grep @Test` = 645, then 647 after remediation — additive, zero loss.
3. **Sprint scaffold `sprint:` field format.** Artifact Librarian scaffold wrote `sprint: sprint-008`; finalize requires the bare 6-digit `'000008'`. Corrected before finalize (Sprint Manager flagged it).
4. **On-device prefs write blocked.** `run-as` allows reading the app's shared_prefs but redirected writes (`cat >`) are denied, so the live "fresh folder + reset watermark" backup test for U-043/U-042 could not be staged via adb. U-041/BUG-009 was fully confirmed on-device; U-042/U-043 on-device confirmation is recommended as a user-driven step (see below).

**Recommendations.**
- **Two standing framework fixes remain:** (a) worktree base should follow session HEAD, not `origin/master`; (b) bookend terminal-status heuristic mis-classifies all-done sprints as `completed-with-blockers`.
- **Remaining on-device confirmations (user-driven, need the live IMAP account):** U-043 first-backup-to-a-fresh-account folder creation (or change `imap_folder` to a new label via Advanced Settings and back up), and U-042 watermark behavior across an induced failure then a success. Both are thoroughly unit-tested and tri-reviewed; the device check is final confidence, not correctness-critical.
- Consider a follow-up to update CNTR-MODERNIZATION-007's `appendMessages` interface spec (`void → long`) to prevent documentation drift (noted by the U-042 code review).
