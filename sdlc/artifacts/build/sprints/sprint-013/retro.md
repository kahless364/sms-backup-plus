---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-013
started: 2026-06-23
completed: 2026-06-23
---
# Retro: sprint-013 — Test Visibility (Batch B)

## Business Summary
**Objective.** Make the riskiest code measurable (assessment findings TE-001/002/003): widen the jacoco gate to all packages, add IMAP integration coverage to un-exclude the worker/transport classes, and test the real BackupWorker body.
**Outcome.** All 3 stories done; 669 tests green (was 642 → +27, incl. 26 GreenMail integration tests), jacoco gate now covers all packages, dep-verification green. No emulator used (all JVM/GreenMail).
- **U-051 (TE-001):** `jacocoTestCoverageVerification` extended from 3 packages to all ~22, with honest measured floors (existing service*/mail*/auth* unchanged at ≥70%); explicit CI gate step added.
- **U-052 (TE-002):** GreenMail 2.1.3 added (testImplementation; specific SHA-256 in verification-metadata, no wildcard); 26 integration tests; the 3 jacocoFileFilter exclusions (BackupImapStoreDelegate/BackupWorker/RestoreWorker) REMOVED with the gate still green (87%/56%/80% class coverage; mail* 71.5%, service* 75.6%).
- **U-053 (TE-003):** extracted `appendBatchAndUpdateWatermark` production seam; watermark tests now hit real code (processWatermarkLoop deleted); BUG-010 invariant unchanged.

## Process Observations
- **Coverage-gate interaction handled correctly.** U-051 widened the gate without gaming (floors at measured values; nothing excluded to dodge it). U-052 added GreenMail coverage BEFORE removing the exclusions and verified the package floors still clear 70% — the right order.
- **Supply-chain discipline.** The new GreenMail dependency required SHA-256 entries in `verification-metadata.xml` (verify-metadata=true); added specifically, no `.*` wildcard, dep-verification gate stayed green.
- **Reviews all PASS** (code 3/3, security 3/3, QA 15/15 ACs) — only advisory warnings: ~0% floors on a few packages (visibility, not enforcement — accepted per the story), an unused import, impl-log line-number drift. No remediation needed.
- **Worktree artifact gap recurred:** the 3 plan.md files didn't propagate from the worktrees (same as sprint-009/U-046); backfilled from the implementation logs.
- **On-device:** N/A for this sprint (test/build-config only).

## Gate Results
U-051/052/053 each: code PASS, security PASS, QA PASS. Wave-1 (U-051, U-053) + wave-2 (U-052) gates PASS.

## Recommendations
1. Bookend status mis-derivation (now 13×) — corrected to completed.
2. Follow-up (optional): raise the ~0% package floors as tests are added (TODOs in build.gradle); seed Robolectric SMS data to cover `BackupWorker.backupCursors` (the main remaining 0% block).
3. Investigate the recurring worktree plan.md propagation gap (framework).
