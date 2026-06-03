---
id: EPIC-MODERNIZATION-003
artifact_type: epic
title: 'Engine & Integration Modernization: Event Bus, DI, and Mail ACL'
status: approved
domain: modernization
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-007
  - REQ-MODERNIZATION-008
  - REQ-MODERNIZATION-009
related_stories: []
change_records: []
spec_ref: sdlc/analysis/20260529-modernization/reports/modernization-plan.md
notes: |
  Roadmap band: M3 (Substrate Swap, structural completion). The post-spine structural
  modernization that retires the remaining abandoned dependency (Otto) and removes the
  last leaky abstractions. Migration units: MU-006 (REQ-007), MU-007 (REQ-008),
  MU-008 (REQ-009).
  Prerequisite: EPIC-MODERNIZATION-002 — the WorkManager spine (REQ-005) should land
  first to avoid double-churning the engine, and the ports it introduces are what Hilt
  injects. Source assessment: 20260529-modernization.
---

# Engine & Integration Modernization

## Description

With the scheduler spine replaced (EPIC-002), this epic completes the structural
modernization: it retires Square Otto — the last abandoned runtime dependency — in
favor of an app-owned reactive state repository, formalizes the hand-wired dependency
graph with Hilt, and isolates the k-9 mail library behind an anti-corruption layer so
no vendor type leaks into app code. Together these remove the remaining invisible
coupling and make the codebase idiomatic for modern Android.

## Scope

**In scope**
- REQ-MODERNIZATION-007 (MU-006): replace Otto (12 files) with a `SyncStateRepository`
  exposing `StateFlow<SyncState>` + `SharedFlow<SyncEvent>`; decompose `MainActivity`
  with a `ViewModel`.
- REQ-MODERNIZATION-008 (MU-007): introduce Hilt; replace the `BackupTask`
  dual-constructor Service-Locator with constructor injection; delete test-only constructors.
- REQ-MODERNIZATION-009 (MU-008): introduce a `MailTransport` anti-corruption layer so
  no `com.fsck.k9.*` type crosses into app code; unpin k-9 from the JitPack SHA to a
  reproducible coordinate (or vendor it).

**Out of scope**
- Scheduler/worker, credential-encryption, and dead-code work (EPIC-MODERNIZATION-002).
- Strategic/elective integrations — Billing, People API (EPIC-MODERNIZATION-004).

## Success Criteria

- Otto removed from all 12 files; `SyncStateRepository` live; `MainActivity` uses a
  `ViewModel` with `repeatOnLifecycle`; sticky `@Produce` semantics preserved via `StateFlow.value`.
- Hilt component graph live; the Service-Locator pattern removed; test-only
  constructors deleted; tests run on the Hilt test harness.
- No `com.fsck.k9.*` import outside the `MailTransport` adapter; the `State.java:32`
  magic-string match removed; k-9 on a reproducible coordinate.

## Related Requirements

- REQ-MODERNIZATION-007 — Replace Otto with a StateFlow repository (MU-006; TECH-003, STRUCT-002, STRUCT-004).
- REQ-MODERNIZATION-008 — Introduce Hilt dependency injection (MU-007; STRUCT-001/ARCH-001 binding).
- REQ-MODERNIZATION-009 — Mail anti-corruption layer + k-9 unpin (MU-008; DEP-005, ARCH-001).

## Notes

Sequencing: REQ-007 (Otto→Flow) is best done after the WorkManager spine (REQ-005) to
avoid concurrent engine churn. REQ-008 (Hilt) depends on REQ-007 (a repository to
inject) and REQ-005 (CoroutineWorker → `@HiltWorker`). REQ-009 (mail ACL) follows the
Phase-0 trust-all fix (REQ-002) since both touch `BackupImapStore`.
Traceability: MU-006, MU-007, MU-008; source assessment 20260529-modernization.
