---
artifact_type: review-code
story_id: U-020
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# Code Review: U-020

## Summary

Otto bus removal is architecturally clean. The branch-by-abstraction seam (U-019 SyncStateRepository
interface) held: all engine→UI coupling was routed through the repository before this story, so the
swap to live Flow backing was purely mechanical at the seam points.

## Findings

### PASS Items

1. **FlowSyncStateRepository** correctly implements the contract:
   - `MutableStateFlow<SyncState>` seeded with `BackupState()` (non-null, non-running INITIAL)
   - `MutableSharedFlow(replay=0, extraBufferCapacity=1, DROP_OLDEST)` — no-deadlock buffer, no stale replay
   - `emitState` is non-suspending as required (engine threads call without a coroutine scope)
   - `tryEmitEvent` return value is logged when false (AC-17 silent-swallow elimination)

2. **App.java** — bus field, register/unregister/post helpers deleted cleanly. autoBackupSettingsChanged
   migrated to Application-scoped coroutine via FlowCollectHelper. `syncStateRepository()` accessor
   unchanged from U-019.

3. **repeatOnLifecycle(STARTED)** used in MainActivityFlowHelper — correct lifecycle state for UI
   collection that should pause when activity is backgrounded.

4. **Cancel event subscription** (BackupCancelCollector) uses `flow.first()` — terminates after one
   Cancel event, mirroring the single-fire semantics of the old @Subscribe handler.

5. **tryEmitEvent return value** checked at every call site in production code (73 sites).

6. **Zero Otto references** in production source confirmed by grep.

### Minor Observations

1. `PerformAction.java` (POJO) retained because `SyncEvent.PerformActionRequested` imports its
   `Actions` enum. This is acceptable for this story; full POJO cleanup is a follow-up once
   SyncEvent's `PerformActionRequested.action` type is changed to an inline enum (future story).

2. `SyncEvent.AccountConnectionChanged` and `SyncEvent.FallbackAuth` are payload-less objects
   per U-019 design. The two call sites that relied on their payloads (AdvancedSettings toggle direction,
   FallbackAuthEvent.showDialog) were adapted with minimal semantic change.

3. `DefaultSyncStateRepositoryTest.kt` renamed to reference `FlowSyncStateRepository` but kept
   original filename for git history traceability. Acceptable; the test content was updated.

## Verdict: PASS
