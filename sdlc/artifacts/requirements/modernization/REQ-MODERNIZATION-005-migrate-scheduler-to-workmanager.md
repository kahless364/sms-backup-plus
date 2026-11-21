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
change_records:
  - CR-015
  - CR-017
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

Amended 2026-11-21 by CR-015: AC-3(d), "content-URI trigger (with broadcast fallback below API 24)", is unchanged in wording. Since minSdk 24 (REQ-MODERNIZATION-015, CR-011) no supported level is below API 24, so the clause has no case and no test of it is required; AC-3(d) is met by the content-URI trigger tests; their lowest-level assertion is made at API 24 once story S1 of DES-MODERNIZATION-014 retargets `inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast` (CNTR-MODERNIZATION-004 v3, INV-4 note). CR-015 did not remove the `SMS_RECEIVED` path; CR-017 removes it from the contract on the product owner's decision of 2026-11-21 (decision appendix, Record 20, ruling 56), and its own story removes it from the code. Ruled not a behavioural break of CNTR-MODERNIZATION-004 (v3).

Amended 2026-11-21 by CR-017: no criterion changes. AC-3(d) is met by the content-URI trigger alone; the SMS-received receiver, which it never relied on, has not been delivered the broadcast since U-001 and is to be removed by its own story (ruling 56). "Preserving all current scheduling behavior" (Description) is read with two corrections that CNTR-MODERNIZATION-004 v4 makes, each restoring the legacy behaviour: a regular schedule of "Never" schedules nothing (BUG-026), and a restart keeps the regular backup (BUG-027). The extra backup about a minute after every restart, which the migrated code ran while it deleted the regular schedule, is not kept unless the product owner decides otherwise (BUG-027, PO-1 and PO-2).
