---
artifact_type: plan
story_id: "U-037"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-04"
contracts_verified: []
risks_identified: 0
---

# Implementation Plan: U-037

## Story Overview
The U-031 WorkInfo observer is registered via `observeForever` but the onDestroy teardown
path only nulls the observer reference without calling `removeObserver()`. Leaked observers
cause duplicate `*StateChanged` callbacks on service restart.

## Acceptance Criteria Mapping

| AC | Implementation Step | Notes |
|----|-------------------|-------|
| AC-1 | Cache LiveData alongside observer; teardown always calls removeObserver() | `workInfoLiveData` field added |
| AC-2 | Reflection test asserts fields null after teardown | Added to both service test files |
| AC-3 | U-031 foreground bridge preserved | Only teardown changed; registration unchanged |
| AC-4 | Build green | Verified |
| IC-1 | LiveData handle cached alongside observer | `workInfoLiveData` field in both services |

## Implementation Steps

1. Add `workInfoLiveData` field (`androidx.lifecycle.LiveData<List<WorkInfo>>`) to `SmsBackupService.java` and `SmsRestoreService.java`.
2. In `registerBackupWorkInfoObserver()` and `registerRestoreWorkInfoObserver()`: cache the LiveData reference (`workInfoLiveData = WorkManager.getInstance(...).getWorkInfosForUniqueWorkLiveData(uniqueWorkName)`) before calling `observeForever()`.
3. Change `tearDownObserverAndCollector(String)` to `tearDownObserverAndCollector()` (no parameter): call `workInfoLiveData.removeObserver(workInfoObserver)` when both fields are non-null, then null both fields.
4. Update all call sites (3 in backup, 2 in restore) to use the no-arg signature.
5. Add observer teardown tests to `SmsBackupServiceTest.java` and `SmsRestoreServiceTest.java`.

## Files Modified/Created

| File | Action |
|------|--------|
| `app/src/main/java/.../service/SmsBackupService.java` | Add `workInfoLiveData` field; refactor teardown; update all call sites |
| `app/src/main/java/.../service/SmsRestoreService.java` | Same |
| `app/src/test/.../service/SmsBackupServiceTest.java` | 3 new tests |
| `app/src/test/.../service/SmsRestoreServiceTest.java` | 3 new tests |

## Phase Completion Report
---
story_id: "U-037"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-006/U-037/plan.md"
story_status: "done"
current_build_phase: "done"
errors: []
---
