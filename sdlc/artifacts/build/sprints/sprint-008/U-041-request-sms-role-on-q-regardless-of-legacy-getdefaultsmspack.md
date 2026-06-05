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
id: U-041
title: Request SMS role on Q+ regardless of legacy getDefaultSmsPackage (fix BUG-009)
pipeline: ''
domain: modernization
resolution: done
updated_at: '2026-06-05T14:40:00.000Z'
requirement_source: bug:BUG-009
sprint: '000008'
---

# U-041: Request SMS role on Q+ regardless of legacy getDefaultSmsPackage

## Story
As an SMS Backup+ user on Android Q+,
I want tapping Restore to request the SMS role even when the legacy `getDefaultSmsPackage()` returns null,
So that restore can become the temporary default SMS app and actually write my restored messages.

## Source
Fixes **BUG-009** (high) — `sdlc/artifacts/stories/BUG-009-restore-bails-on-q-plus-null-default-sms-package.md`.

## Acceptance Criteria
- [ ] AC-1: On Q+ (API 29+), tapping Restore requests the SMS role via `RoleManager.createRequestRoleIntent(ROLE_SMS)` even when `Telephony.Sms.getDefaultSmsPackage()` returns null/empty; the role-request dialog appears.
- [ ] AC-2: After restore completes (success or failure), the prior default is restored / role released exactly as today (SmsReceiver disabled on Q+; ACTION_CHANGE_DEFAULT switch-back on pre-Q). The U-009 restore-default-provider flow and the role round-trip are preserved.
- [ ] AC-3: The "no default package — running on tablet?" toast (`error_no_sms_default_package`) only fires in genuinely unsupported cases (e.g., a device without telephony / no app can hold ROLE_SMS), NOT merely because the legacy API returned null on a phone with a RoleManager-managed default.
- [ ] AC-4: Build green (assembleDebug, testDebugUnitTest, jacoco per-package LINE ≥70%); a Robolectric test covers the Q+ null-default path (role requested, no early bail). On-device confirmation is deferred to post-merge validation (cannot run in a worktree).

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java:411-435` (`startRestore`) | Captures legacy `getDefaultSmsPackage()`; gates the entire restore flow on it being non-empty; null → "tablet?" toast and bails, never requesting the role | On Q+, proceed to the RoleManager request regardless of the legacy value; only use the legacy package capture on pre-Q (for ACTION_CHANGE_DEFAULT switch-back) |
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java:469-481` (`requestDefaultSmsPackageChange`) | Already uses `RoleManager.createRequestRoleIntent(ROLE_SMS)` on Q+, but only reachable when defaultSmsPackage is non-empty | Ensure reachable on Q+ irrespective of legacy capture |

## Existing Behavior to Preserve
- Pre-Q: capture current default + ACTION_CHANGE_DEFAULT switch-back (`restoreDefaultSmsProvider`).
- Q+: role released after restore (SmsReceiver.disable) — no permanent default-SMS hijack.
- Genuine no-telephony devices still get a graceful message (no crash, no infinite role loop).
- All other MainActivity restore wiring (confirm dialog, RestoreWorker enqueue, progress observation) unchanged.

## Verification Steps
1. AC-1/AC-3 (Robolectric, in-worktree): with `Build.VERSION.SDK_INT >= Q` and a stubbed `getDefaultSmsPackage()` returning null, invoke the restore entry path and assert the RoleManager request intent is produced (and the error toast is NOT shown).
2. AC-1/AC-2 (on-device, post-merge): on emulator-5554 (API 37) where `getDefaultSmsPackage()` is null, tap Restore → role dialog appears → grant → restore runs and writes SMS → afterward the role is released / prior default restored.

## Technical Context
- The legacy `Telephony.Sms.getDefaultSmsPackage()` is unreliable on RoleManager-managed devices (Q+). On Q+ the RoleManager is authoritative. The fix decouples the Q+ role request from the legacy capture. The defect predates modernization but persisted through the U-021 MainActivity decomposition.
- Coordinate with the existing Q+ branch already present at lines 469-481 — reuse it; do not duplicate the RoleManager call.

## Supporting Documentation
- `sdlc/artifacts/stories/BUG-009-restore-bails-on-q-plus-null-default-sms-package.md`
- Related stories: U-009 (TLS/restore-default-provider flow context), U-021 (MainActivity decomposition).

## Notes
Disjoint file set from U-042 (BackupWorker.kt) and U-043 (K9MailTransport.java) — parallel-eligible.
