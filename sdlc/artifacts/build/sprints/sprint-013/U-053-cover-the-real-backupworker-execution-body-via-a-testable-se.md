---
type: story
status: done
artifact_type: user-story
priority: medium
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-053
title: Cover the real BackupWorker execution body via a testable seam
pipeline: ''
domain: modernization
requirement_source: assessment:20260623-post-migration-assessment#TE-003
sprint: '000013'
updated_at: '2026-06-23T22:24:56.398Z'
resolution: done
---

# U-053: Cover the real BackupWorker execution body via a testable seam

## Story
As a maintainer of the SMS Backup+ codebase, I want the watermark-gating and backup-cursor-iteration logic extracted from `BackupWorker.backupCursors` into a named, injectable seam that `BackupWorkerWatermarkTest` (and future tests) invoke directly against the production code path, so that the test validates real worker behavior rather than a local re-implementation of `processWatermarkLoop`.

## Source
Derived from assessment 20260623-post-migration-assessment, finding TE-003. See `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/testing.md`.

## Acceptance Criteria
- [ ] AC-1: A named method or class (e.g. `BackupWorker.backupCursors(...)` or a new `BackupEngine` collaborator) encapsulates the watermark-gating and backup-loop logic that is currently inline in `BackupWorker`; this extracted unit is the single implementation of that logic — no duplication exists between the production code and any test helper.
- [ ] AC-2: `BackupWorkerWatermarkTest` is updated (or replaced) to invoke the extracted seam on the production `BackupWorker` (or `BackupEngine`) rather than calling its own `processWatermarkLoop` local copy; the test still asserts the BUG-010 invariant: watermark advances only when `appendMessages` returns successfully, and remains unchanged on failure or cancellation.
- [ ] AC-3: The local `processWatermarkLoop` helper in `BackupWorkerWatermarkTest` is deleted; a `grep` for `processWatermarkLoop` in test sources returns zero results.
- [ ] AC-4: All existing assertions in `BackupWorkerWatermarkTest` continue to pass with the same semantics (the BUG-010 invariant is not weakened); no regression in what the test verifies, only in how it invokes the subject under test.
- [ ] AC-5: Build green: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` passes; the extracted seam is covered by the updated test.

## Affected Code
| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` (backup loop / `backupCursors` area) | Watermark-gating and cursor-iteration logic is inline in the worker body | Extract the loop into a named internal method or `BackupEngine` collaborator accessible from tests |
| `app/src/test/java/.../service/BackupWorkerWatermarkTest.kt` (or `.java`) | Contains a local `processWatermarkLoop` re-implementation; tests prove the invariant's intent but not the real code | Replace local helper call with invocation of the extracted production seam; delete `processWatermarkLoop` |

## Existing Behavior to Preserve
- BUG-010 invariant (compiler-enforced via `appendMessages` return value at `BackupWorker.kt:334-345`): `setMaxSyncedDate` is fed only the `appendMessages` return; a throw exits before the watermark write. This must remain true in the production code after refactoring.
- Cooperative cancellation (`ensureActive()` at `BackupWorker.kt:315`) and `finally` cursor cleanup must survive extraction intact.
- The `MailTransportFactory` test-seam that enables fault injection must remain usable from the updated test.

## Verification Steps
1. Run `grep -r "processWatermarkLoop" app/src/test/` — expect zero results after the change.
2. Run `./gradlew :app:testDebugUnitTest` — `BackupWorkerWatermarkTest` passes; all assertions about watermark-on-failure and watermark-on-success still fire.
3. Introduce an artificial fault that makes `appendMessages` throw in the test, and confirm the test asserts the watermark is unchanged — this confirms the test is exercising production code, not a copy.
4. Run `./gradlew :app:jacocoTestCoverageVerification` — the extracted seam contributes to `worker*` or `service*` LINE coverage; gate passes.

## Technical Context
- TE-003 root cause: `BackupWorkerWatermarkTest` was written before the worker body was testable; it copied the loop logic into a local helper to prove the invariant's intent. The risk is that a regression introduced in the production loop would not be caught by the test (the test's local copy would still pass).
- Extraction approach: the simplest form is promoting the inline logic in `BackupWorker.backupCursors` to an `internal` (Kotlin) or package-private (Java) method, making it accessible from tests in the same package without reflection. A `BackupEngine` collaborator (extracted class) is a stronger refactor but may be larger than a single sprint can absorb — the story accepts either approach as long as the test invokes production code.
- This story is enabled by U-052's seam work (injected `MailTransportFactory`) and should sequence after U-052 within Sprint B, but it is independent enough to be parallelized if needed.

## Notes
- Sprint B (Test Visibility & Coverage). Sequence after U-052 conceptually; can run in parallel if U-052 integration tests are in progress.
- The goal is correctness of the test assertion, not a full `BackupEngine` decomposition. Keep the extraction minimal — just enough to make the test invoke production code.
