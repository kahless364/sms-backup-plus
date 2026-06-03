---
id: REQ-MODERNIZATION-005
title: Migrate Scheduler and Workers to WorkManager + CoroutineWorker
artifact_type: requirement
domain: modernization
status: approved
type: functional
priority: high
epic: EPIC-MODERNIZATION-002
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-001
  - REQ-MODERNIZATION-003
related_stories: []
related_design_docs: []
traces_to: []
change_records: []
notes: |
  Traceability: TECH-002/DEP-002 (gaps), ARCH-003 (patterns), STRUCT-003 (gaps); migration unit MU-005 (THE SPINE); source assessment 20260529-modernization. Highest-leverage single migration — one binding flip retires four dead mechanisms.
---

# Migrate Scheduler and Workers to WorkManager

## Description

Replace the four obsolete background-execution mechanisms — Firebase JobDispatcher,
`AsyncTask` workers, the `AlarmManagerDriver` fallback, and the `SmsJobService`
lifecycle bypass — with a single WorkManager + `CoroutineWorker` implementation,
behind an app-owned `BackupScheduler` port, preserving all current scheduling behavior
and the public broadcast contract.

## Context

This is the central migration of the engagement. `firebase-jobdispatcher:0.8.6` is
deprecated and served only from a non-Google scijava mirror (a supply-chain risk);
`AsyncTask` is removed in API 33; `SmsJobService` manually instantiates
`SmsBackupService` to dodge the API-26 background-start restriction (so `onCreate()`
never runs); and `AlarmManagerDriver` is a third parallel scheduling path. WorkManager
natively subsumes `JobScheduler` (API 23+) and `AlarmManager` (below 23), owns worker
lifecycle and foreground promotion, and provides durable work state that enables the
restore checkpoint. The existing `Driver` Strategy in `service/BackupJobs.java:66-72`
is already half of the `BackupScheduler` port this requires. One binding flip retires
four dead mechanisms at once.

## Acceptance Criteria

1. A `BackupScheduler` port abstracts scheduling; the WorkManager implementation is the
   only active adapter after cutover.
2. Backup and restore run as `CoroutineWorker`s under WorkManager; `BackupTask`,
   `RestoreTask`, and `SmsJobService` no longer extend/instantiate Android services via
   the lifecycle bypass.
3. All four scheduling invariants are preserved and test-verified:
   (a) REPLACE enqueue semantics (`setReplaceCurrent(true)` equivalent);
   (b) 30s/300s exponential backoff;
   (c) UNMETERED / ANY_NETWORK constraints per backup type;
   (d) content-URI trigger (with broadcast fallback below API 24).
4. The public `com.zegoggles.smssync.BACKUP` broadcast contract is preserved — third-party
   triggers continue to work (verified by an ADB broadcast test).
5. `firebase-jobdispatcher`, `AsyncTask` usage, `AlarmManagerDriver`, and the scijava
   Maven mirror are all removed from the build and source.
6. A durable restore checkpoint is active post-cutover (resumable after process kill;
   no duplicate messages) — see REQ-MODERNIZATION (durable restore is enabled by, and
   verified alongside, this migration).

## Rationale

Retiring JobDispatcher restores supply-chain integrity; retiring `AsyncTask` removes a
class removed in API 33; removing the lifecycle bypass fixes a structural hazard; and
WorkManager's durable work-state is the prerequisite for safe, resumable restores.

## Constraints

- Hard dependency on REQ-MODERNIZATION-001 (SDK/AGP for WorkManager 2.9.x+).
- Hard dependency on REQ-MODERNIZATION-003 (Gate G2): characterization tests pinning
  the retry/constraint behavior MUST be green before cutover begins.
- Co-developed with the `BackupScheduler` port; use branch-by-abstraction with a
  parallel-run of the new scheduler in debug builds before the production flip.
- Large/High risk — the public broadcast contract must not break.

## Notes

Boundary: `service/BackupJobs.java`, `service/SmsJobService.java`, `service/BackupTask.java`,
`service/RestoreTask.java`, `service/AlarmManagerDriver.java`.
Traceability: TECH-002/DEP-002, ARCH-003, STRUCT-003; MU-005; source assessment 20260529-modernization.
