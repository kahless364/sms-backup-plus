---
id: EPIC-MODERNIZATION-002
artifact_type: epic
title: 'Substrate Core: Encrypt Credentials, Replace the Scheduler, Clear Dead Weight'
status: approved
domain: modernization
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-004
  - REQ-MODERNIZATION-005
  - REQ-MODERNIZATION-006
related_stories: []
change_records: []
spec_ref: sdlc/analysis/20260529-modernization/reports/modernization-plan.md
notes: |
  Roadmap band: M2/M3 (Substrate Swap), the central engine replacement plus its
  security companion and foundational cleanup. Migration units: MU-004 (REQ-004),
  MU-005 (REQ-005), MU-011 (REQ-006).
  Prerequisite: EPIC-MODERNIZATION-001 (Phase-0) must be complete — specifically the
  Gate G2 CI + coverage safety net (REQ-MODERNIZATION-003) MUST be green before any
  substrate swap in this epic begins, and the SDK/AGP raise (REQ-MODERNIZATION-001)
  is a hard prerequisite for the Jetpack Security and WorkManager libraries.
  Source assessment: 20260529-modernization.
---

# Substrate Core

## Description

This epic covers the core of the M2/M3 "substrate swap" — replacing the rotted
runtime substrate underneath SMS Backup+'s healthy domain core, without rewriting the
business logic. It bundles the single highest-leverage migration in the engagement
(the scheduler/worker move to WorkManager) with its security companion
(credential-at-rest encryption) and the foundational dead-code/minSdk cleanup that
de-risks everything downstream. All work here is branch-by-abstraction: dead libraries
are swapped behind app-owned seams so the domain logic never notices the change.

## Scope

**In scope**
- REQ-MODERNIZATION-004 (MU-004): migrate plaintext credentials to AES-256
  `EncryptedSharedPreferences` (Keystore-backed) at the `AuthPreferences` seam.
- REQ-MODERNIZATION-005 (MU-005): replace Firebase JobDispatcher + AsyncTask workers
  + AlarmManagerDriver fallback + the `SmsJobService` lifecycle bypass with
  WorkManager + CoroutineWorker — the spine migration.
- REQ-MODERNIZATION-006 (MU-011): delete dead compatibility shims, implement the
  stubbed `StatusPreference` instance-state, and complete the minSdk-21 cleanup.

**Out of scope**
- Event-bus, DI, and mail-ACL modernization (EPIC-MODERNIZATION-003).
- Strategic/elective integrations — Billing, People API (EPIC-MODERNIZATION-004).
- The Phase-0 SDK raise, security TLS fix, and CI gate (EPIC-MODERNIZATION-001).

## Success Criteria

- Credentials are AES-256 encrypted at rest; a one-time plaintext→encrypted migration
  runs on first launch post-upgrade with rollback safety; no plaintext remains.
- WorkManager is the single scheduling mechanism; Firebase JobDispatcher, AsyncTask,
  the AlarmManager fallback, and the scijava mirror are all removed; the four
  scheduling invariants (REPLACE, 30/300 backoff, UNMETERED, content-URI trigger) and
  the public `com.zegoggles.smssync.BACKUP` broadcast contract are preserved.
- Dead shims removed, `StatusPreference` instance-state implemented, minSdk raised to 21.

## Related Requirements

- REQ-MODERNIZATION-004 — Encrypt credentials at rest (MU-004; SEC-002/CAP-003, ARCH-009).
- REQ-MODERNIZATION-005 — Migrate scheduler/workers to WorkManager (MU-005; TECH-002, ARCH-003, STRUCT-003).
- REQ-MODERNIZATION-006 — Dead-code & minSdk cleanup (MU-011; ARCH-005/STRUCT-005).

## Notes

Sequencing: REQ-004 follows the Phase-0 trust-all fix (REQ-002) since both touch
`AuthPreferences`. REQ-005 is the critical-path spine and depends on the Phase-0 CI
gate (REQ-003) and benefits from the ports introduced alongside it. REQ-006 is
low-risk and can run in parallel once minSdk 21 is settled by the SDK raise (REQ-001).
Traceability: MU-004, MU-005, MU-011; source assessment 20260529-modernization.
