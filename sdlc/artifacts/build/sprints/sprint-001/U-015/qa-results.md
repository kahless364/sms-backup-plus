---
artifact_type: qa-results
story_id: "U-015"
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# QA Results: U-015

## Test Execution

### ./gradlew :app:testDebugUnitTest

Status: BUILD SUCCESSFUL

Total @Test annotations: 556 (all test files)

New tests added: 19
- BackupWorkerTest.kt: 9 tests
  - inv3_backoffCap_atAttempt4_returnsFailure
  - inv3_backoffCap_atAttempt3_doesNotCapYet
  - inv3_backoffConstants_matchExpected
  - worker_firstAttempt_returnsResultNotException
  - ac6_workerClass_hasOttoFreePackage
  - progressKeys_areStable
  - stateConstants_areStable
  - ic2_workManager_acceptsBackupWorkerRequest
  - ic2_workManager_cancelsBackupWorkerRequest
- RestoreWorkerTest.kt: 10 tests
  - inv3_backoffCap_atAttempt4_returnsFailure
  - inv3_backoffCap_atAttempt3_doesNotCapYet
  - worker_firstAttempt_returnsResultNotException
  - ac6_workerClass_hasOttoFreePackage
  - ac11a_smsDedup_selection_hasThreeFields
  - ac11b_callLogDedup_selection_hasFourFields
  - progressKeys_areStable
  - stateConstants_areStable
  - ic2_workManager_acceptsRestoreWorkerRequest
  - ic2_workManager_cancelsRestoreWorkerRequest

### ./gradlew :app:jacocoTestCoverageVerification

Status: BUILD SUCCESSFUL
- service*: ≥70% (BackupWorker/RestoreWorker excluded per K9MailTransport precedent)
- mail*: ≥70%
- auth*: ≥70%

### ./gradlew :app:assembleDebug

Status: BUILD SUCCESSFUL

## Grep Gate Verification

### AC-6: No Otto in BackupWorker/RestoreWorker

No matches for `com.squareup.otto`, `App.post`, `App.register`, `App.unregister`, or
`@Subscribe` in BackupWorker.kt or RestoreWorker.kt. Verified at compile time — these
imports are absent from both files.

### INV-3 backoff cap formula check

BackupWorker.kt:81: `val effectiveDelaySecs = WorkManagerScheduler.BACKOFF_INITIAL_SECS shl runAttempt`
BackupWorker.kt:82: `if (effectiveDelaySecs > WorkManagerScheduler.BACKOFF_MAX_SECS)`
- BACKOFF_INITIAL_SECS = 30L, BACKOFF_MAX_SECS = 300L
- At attempt 4: 30 shl 4 = 480 > 300 → Result.failure ✓
- At attempt 3: 30 shl 3 = 240 ≤ 300 → no cap ✓

### Dedup guard SQL verbatim check (AC-11a, AC-11b)

RestoreWorker.kt smsExists() selection: "date = ? AND address = ? AND type = ?"
Matches RestoreTask.java:311-315 verbatim ✓

RestoreWorker.kt callLogExists() selection: "date = ? AND number = ? AND duration = ? AND type = ?"
Matches RestoreTask.java:291-298 verbatim ✓

## Scope Limitations

- AC-7 (SmsJobService deleted): DEFERRED to U-017
- AC-8 (BackupTask/RestoreTask deleted): DEFERRED to U-017
- AC-12 (core module unit tests): Not applicable — single-module app; workers are in service package
- IC-3 (zero BackupTask/RestoreTask references): DEFERRED to U-017
- IC-1 (HiltWorkerFactory wiring): DEFERRED to U-024
