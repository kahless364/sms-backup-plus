---
id: REQ-MODERNIZATION-008
title: Introduce Hilt Dependency Injection
artifact_type: requirement
domain: modernization
status: approved
type: constraint
priority: medium
epic: EPIC-MODERNIZATION-003
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-005
  - REQ-MODERNIZATION-007
related_stories: []
related_design_docs: []
traces_to: []
change_records: []
notes: |
  Traceability: STRUCT-001/ARCH-001 (DI binding portion); migration unit MU-007; source assessment 20260529-modernization.
---

# Introduce Hilt Dependency Injection

## Description

Introduce Hilt and replace the hand-wired `BackupTask`/`RestoreTask` dual-constructor
Service-Locator pattern with constructor injection, removing the test-only constructors
that currently exist solely to enable testing.

## Context

The construction graph is hand-wired: `BackupTask`'s primary constructor directly
`new`s `BackupItemsFetcher`, `BackupQueryBuilder`, `PersonLookup`, `ContactAccessor`,
`MessageConverter`, `CalendarSyncer`, `TokenRefresher`, and `OAuth2Client`, while a
second, fully-injecting constructor exists purely as a test seam (the Humble-Object
pattern). This is a manual Service-Locator: adding a collaborator means editing the
constructor body, and the test-only constructor duplicates the dependency list. Hilt
formalizes what the code already does by hand, removes the Service-Locator, and lets
tests use a proper DI test harness instead of a parallel constructor.

## Acceptance Criteria

1. Hilt is configured (`@HiltAndroidApp` on `App`, component graph, `@Module` provider classes).
2. `BackupTask` and `RestoreTask` receive collaborators via constructor injection; the
   manual `new`-wiring is removed.
3. The test-only secondary constructors are deleted; tests obtain doubles via the Hilt
   test harness (or a test component), not a parallel constructor.
4. WorkManager workers are `@HiltWorker`-injected (integrates with REQ-MODERNIZATION-005).
5. No `App`-level Service-Locator lookups remain; the dependency graph is compile-time verified.
6. The full test suite passes on the Hilt test harness.

## Rationale

Hilt removes the last manual Service-Locator and the duplicate test-only construction
path, giving compile-time-verified injection. It is mechanical once the ports
(REQ-MODERNIZATION-007) and CoroutineWorkers (REQ-MODERNIZATION-005) exist.

## Constraints

- Depends on REQ-MODERNIZATION-007 (the `SyncStateRepository` and ports are what gets injected).
- Depends on REQ-MODERNIZATION-005 (`CoroutineWorker` → `@HiltWorker`).
- Transition incrementally: Hilt and manual construction may coexist while collaborators
  are converted one at a time.

## Notes

Boundary: `service/BackupTask.java`, `service/RestoreTask.java`, `App.java`, new `@Module` classes.
Traceability: STRUCT-001/ARCH-001 (DI binding); MU-007; source assessment 20260529-modernization.
