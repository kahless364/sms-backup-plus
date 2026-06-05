---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-008
sprint: '000008'
---

# Sprint Plan: 008 — Bug-Fix Sprint (BUG-009, BUG-010, BUG-011)

## Goals

- Fix BUG-009 (high): request the SMS role on Android Q+ even when the legacy `getDefaultSmsPackage()` returns null, unblocking restore on RoleManager-managed devices
- Fix BUG-010 (critical — silent data loss): advance `max_synced_date` only after a confirmed IMAP append, eliminating the silent-skip regression introduced in the U-015/U-016 CoroutineWorker rewrite
- Fix BUG-011 (high): create the "SMS" IMAP folder on first backup to a fresh account, resolving the `NO [NONEXISTENT] Unknown Mailbox` failure that blocks new-user onboarding

## Story Summary

| ID    | Title | Type | Points | Complexity |
|-------|-------|------|--------|------------|
| U-041 | Request SMS role on Q+ regardless of legacy getDefaultSmsPackage (fix BUG-009) | bug | 3 | medium |
| U-042 | Advance max_synced_date only after confirmed IMAP append (fix BUG-010) | bug | 3 | medium |
| U-043 | Create IMAP SMS folder on first backup to a fresh account (fix BUG-011) | bug | 3 | medium |

## Execution Waves

### Wave 1

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-041 | Request SMS role on Q+ regardless of legacy getDefaultSmsPackage (fix BUG-009) | Android | developer | sonnet | lead | — | planned | high |
| U-042 | Advance max_synced_date only after confirmed IMAP append (fix BUG-010) | Android | developer | sonnet | lead | — | done | high |
| U-043 | Create IMAP SMS folder on first backup to a fresh account (fix BUG-011) | Android | developer | sonnet | lead | — | done | high |

## Wave Summary

| Wave | Stories | Can Parallel? | Gate |
|------|---------|---------------|------|
| wave-1 | U-041, U-042, U-043 | Yes (disjoint files) | wave-1-gate |

## Capacity and Sequencing Notes

### Team

- **Team size:** Single maintainer (lead). All three stories assigned to lead.
- **Agent/model:** `developer` / `sonnet` — consistent with sprint-001 through sprint-007 precedent and `config.yaml` `defaults.model_routing.developer: sonnet`.

### Sprint Framing

This is a three-story maintenance sprint targeting three high/critical bugs logged against the modernized backup pipeline. Total estimated effort: 9 story points (3 + 3 + 3). The three stories touch fully disjoint file sets:

- **U-041**: `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` (lines 411–481, `startRestore` and `requestDefaultSmsPackageChange`)
- **U-042**: `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` (backup loop / `setMaxSyncedDate`), plus investigation of the backup-task/append path for watermark write ordering
- **U-043**: `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java` (`createAndOpenFolder`), with possible extension into `k9mail-vendored/.../ImapFolder.java` only if the root cause is in the vendored layer

No file appears in more than one story. There are no intra-sprint `depends_on` edges. All three stories are `parallel_eligible: true`. The dependency graph has three nodes and zero edges — a single wave of three parallel-eligible stories.

### Validation Coupling Note

Although the three stories have no code-level dependencies and execute in parallel in the same wave, U-043 (folder-create) and U-042 (watermark) are logically coupled for **on-device validation**:

- U-042's success path (watermark advances to confirmed-appended message dates) can only be fully verified on-device when a backup can succeed end-to-end. A `NO [NONEXISTENT]` folder error (BUG-011) causes the backup to fail before any messages are appended, making it impossible to confirm U-042's watermark-advance behavior against real IMAP state.
- Therefore: in-worktree unit tests (injected failures and successes) are the primary AC gate for both U-042 and U-043. Full on-device round-trip confirmation — where a live backup creates the folder (U-043) and the watermark advances only to uploaded messages (U-042) — is deferred to **post-merge validation** once both fixes are in the same build.

This is not a sprint-blocking sequencing constraint; it is a validation sequencing note. Both stories remain parallel-eligible and are in wave-1.

### On-Device Tests Are Post-Merge

Per the Acceptance Criteria for all three stories (AC-4 for U-041, AC-4 for U-042, AC-4 for U-043), on-device / emulator confirmation is explicitly deferred to post-merge. Worktree execution runs in-worktree unit tests only. This is consistent with prior sprints (e.g., sprint-006, sprint-007).

### Dependencies

All three stories have `dependencies: []` in frontmatter. No intra-sprint edges. Dependency graph: three isolated nodes. Wave assignment: all three in wave-1.

### Risk Notes

- **U-041**: The Q+ `RoleManager.createRequestRoleIntent(ROLE_SMS)` branch already exists at lines 469–481; the fix decouples it from the legacy `getDefaultSmsPackage()` gate. Risk is low: confirm the error toast is still surfaced for genuine no-telephony devices and that the pre-Q ACTION_CHANGE_DEFAULT switch-back path is unaffected.
- **U-042**: CRITICAL severity / data-loss class. The watermark-write ordering relative to append confirmation is the key invariant. Implementation must ensure per-batch watermark advancement from actual appended-message dates, not wall-clock, so an interrupted run is exactly recoverable. Per-type watermark independence (SMS / MMS / calllog) must be preserved. The partial-success regression test (batch 1 ok, batch 2 fails → watermark = batch 1 max date) is mandatory per AC-3.
- **U-043**: Root-cause diagnosis required before fix (vendored k-9 vs. `K9MailTransport.java` vs. Gmail label-propagation nuance). The create→select gap (Gmail may not immediately SELECT a freshly CREATE-d label) is a likely contributor; the fix may need a retry or re-SELECT. Changes to the vendored module should be minimal and scoped to the create/open path only.
- Build gate (assembleDebug + testDebugUnitTest + jacocoTestCoverageVerification LINE ≥70%) applies to all three stories.

## Resolution Log

### Planner Override

**Trigger:** `node lib/index.mjs plan --process story-build --artifacts U-041,U-042,U-043 --config sdlc/config.yaml` — process YAML not found for pipeline `story-build` at project or core paths (consistent with sprint-001 through sprint-007 behavior).

**Resolution:** Agent/model resolved from `config.yaml` `defaults.model_routing.developer: sonnet` and sprint-001 through sprint-007 precedent (all prior stories used `developer` / `sonnet` / assigned to `lead`). No override of wave structure, gate type, or expected artifacts. Execution plan produced directly per CNTR-PIPELINE-007.

### Contract Gate Check

- `integration_contracts: []` for all three stories (U-041, U-042, U-043).
- All three fixes are fully internal to the Android application module. No cross-domain service boundary, no external API, schema, or protocol reference.
  - U-041: `MainActivity.java` SMS role / `RoleManager` wiring — intra-module Activity logic; no boundary crossing.
  - U-042: `BackupWorker.kt` watermark write ordering — intra-module service logic; the IMAP append confirmation is internal to the backup pipeline.
  - U-043: `K9MailTransport.java` `createAndOpenFolder` and possibly `k9mail-vendored` — the vendored module is within the `:k9mail-vendored` Gradle sub-project, which is an internal module boundary (not a cross-domain service boundary). No external contract artifact required.
- CONTRACT GATE: PASS for all three stories. No block.

### SA Sprint Completeness Delegation

- `design_docs: []` for all three stories (U-041, U-042, U-043). Per Step 5a instruction (`design_docs=[] for all candidates → SKIP`), SA delegation is skipped.
- All three stories are scoped exclusively from BUG-* artifacts. Each BUG artifact documents root cause, evidence, affected code locations, and fix approach in full. No separate DES-* design document is required for bounded bug-fix repair stories of this scope.
- The three stories touch disjoint files and have no wave-boundary artifact dependencies — no story in wave-1 depends on an artifact created by another wave-1 story. The logical validation coupling between U-042 and U-043 is a post-merge on-device concern, not a wave-boundary partial-state risk.
- SA delegation: SKIPPED.

### Dependency Analysis and Wave Assignment

- U-041: `dependencies: []` — no intra-sprint edges. Wave 1.
- U-042: `dependencies: []` — no intra-sprint edges. Wave 1.
- U-043: `dependencies: []` — no intra-sprint edges. Wave 1.
- Intra-sprint dependency graph: three isolated nodes, zero edges.
- Circular dependency check: N/A (no edges, no cycles possible).
- Wave assignment: wave-1 = {U-041, U-042, U-043} (all three in parallel).

### Freshness Check

- BUG-009: `status: ready` (non-draft). PASS.
- BUG-010: `status: ready` (non-draft). PASS.
- BUG-011: `status: ready` (non-draft). PASS.
- U-041: `status: ready` at time of scheduling. PASS.
- U-042: `status: ready` at time of scheduling. PASS.
- U-043: `status: ready` at time of scheduling. PASS.
