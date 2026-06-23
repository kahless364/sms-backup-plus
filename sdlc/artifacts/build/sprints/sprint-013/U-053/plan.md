---
artifact_type: plan
story_id: U-053
verdict: PASS
---
# U-053 Plan — Testable watermark seam in BackupWorker (TE-003)
## Root cause
`BackupWorkerWatermarkTest` validated a COPY (`processWatermarkLoop`) of the watermark logic, not the real worker body — a production regression wouldn't be caught (BUG-010 invariant).
## Approach
1. Extract the open→append→setMaxSyncedDate gating block from `BackupWorker.backupCursors` into a named `@VisibleForTesting internal appendBatchAndUpdateWatermark(...)` seam called by the real doWork path.
2. Rewrite the test to invoke the production seam on a real BackupWorker; delete `processWatermarkLoop`.
3. Retain the 3 BUG-010 cases (failure→unchanged, success→max confirmed date, partial→batch-1 date) against production code.
## Verdict
PASS — seam is production code, tests hit it, copy deleted, BUG-010 invariant unchanged, build green.
