---
id: REQ-MODERNIZATION-007
title: Replace Otto Event Bus with a StateFlow/SharedFlow Repository
artifact_type: requirement
domain: modernization
status: approved
type: functional
priority: high
epic: EPIC-MODERNIZATION-003
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-005
related_stories: []
related_design_docs: []
traces_to: []
change_records: []
notes: |
  Traceability: TECH-003/DEP-003 (Otto), STRUCT-002 (global singleton), STRUCT-004 (MainActivity); migration unit MU-006; source assessment 20260529-modernization.
---

# Replace Otto with a StateFlow/SharedFlow Repository

## Description

Replace the abandoned Square Otto event bus with an app-owned `SyncStateRepository`
exposing `StateFlow<SyncState>` for sticky state and `SharedFlow<SyncEvent>` for
one-shot events, and decompose `MainActivity` with a `ViewModel` that collects from it.

## Context

`com.squareup:otto:1.3.8` (archived 2014) is imported across 12 production files in
every layer. `App.bus` is a process-global static singleton with `private static`
back-references to the services, creating invisible bidirectional coupling and a
Context-leak hazard; `App.register()` silently swallows `IllegalArgumentException`.
Otto's `@Produce` provides sticky last-state semantics that must be preserved when
migrating to Flow (`StateFlow.value` reproduces it). `MainActivity` (499 LOC, the
highest-complexity file) subscribes to the bus directly and mixes permission flows,
dialogs, and backup/restore triggering with no `ViewModel` — the standard God-Activity
pattern (STRUCT-004), which is naturally addressed alongside the bus migration.

## Acceptance Criteria

1. An app-owned `SyncStateRepository` exposes `StateFlow<SyncState>` (sticky) and
   `SharedFlow<SyncEvent>` (one-shot); the sticky semantics match Otto's `@Produce` behavior.
2. Otto is removed from all 12 importing files and from the build; `App.bus` and the
   `private static` service back-references are deleted.
3. `MainActivity` uses a `MainViewModel` and collects state via `repeatOnLifecycle`;
   dialog logic moves to the existing `Dialogs.java`.
4. The silent `IllegalArgumentException` swallow in `App.register()` is eliminated (no
   equivalent silent failure in the Flow implementation).
5. Backup/restore status updates reach the UI with no regression in observed behavior
   (verified by tests around the repository and a UI smoke check).

## Rationale

Otto is unpatched abandonware and the global bus is the largest source of hidden
coupling and test friction. A lifecycle-aware `StateFlow`/`SharedFlow` repository
removes the dependency, makes state observable and testable, and the `ViewModel`
decomposition resolves the God-Activity hotspot in the same change.

## Constraints

- Depends on REQ-MODERNIZATION-001 (Kotlin/coroutines via the AGP/SDK uplift).
- Best sequenced after REQ-MODERNIZATION-005 (WorkManager) to avoid double-churning the
  engine concurrently.
- Migration strategy: wrap `App.bus` behind the `SyncStateRepository` interface first
  (zero behavior change), then swap the implementation to Flow (Feathers, "Introduce
  Instance Delegator").

## Notes

Boundary: `App.java` (bus singleton), the 12 Otto-importing files, `activity/MainActivity.java`.
Traceability: TECH-003/DEP-003, STRUCT-002, STRUCT-004; MU-006; source assessment 20260529-modernization.
