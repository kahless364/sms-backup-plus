# Mutation log (U-069)

Protocol: U-069 plan 4.8 (write-ahead entry, one Edit, one targeted run, restore by copying the pristine file, blob, raw-byte and porcelain checks, `restored` line). The production change is uncommitted while mutations run. Repository-relative paths.

- start commit: 29f30ec575eee3241a00084cc01023d1a0c825d9
- HW (worker, fixed text, blob): c47a0452d040f959df134fc8a71fda5be6b6180f
- SW (worker, sha256 of raw bytes): 6f89829f8249a990a9308f5e14c30d9c1e33471f364d01de3302fce5f814a629
- HS (scheduler, blob = HEAD): 0194308cfcf9ff34effaf08c7990c087b0bedcbd
- SS (scheduler, sha256 of raw bytes): 53430cc77c6d2899dbd854457b7a087cbef64e7961a056ed619b083c2d727a9d

Recovery note: If a session is lost while an entry says `APPLYING` without a `restored` line, nothing else runs until: the worker is restored only by `cp` from its pristine copy (never `git restore`, which would remove the fix) or, if the pristine copy is lost, by `git restore --source=HEAD --worktree -- <worker>` followed by W1 to W3 and the blob check `c47a0452...`; the scheduler is restored by `cp` from its pristine copy or by `git restore --source=HEAD --worktree -- <scheduler>` (it equals HEAD).

### M-T1 APPLYING 2026-11-21T15:30:26Z
- file: app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt; edit: `scheduler.scheduleIncoming()` replaced by a private OneTimeWorkRequest builder (delay and tag only) and enqueueUniqueWork INCOMING REPLACE (the old worker's form)
- classes: BackupTriggerWorkerTest; expected failing (10): ac1, ac2, ac3 x2, ac4, ac5, ac6 x2, ac9 x2; passing: ac7, ac8a, ac8b, ac8c, ic1

- run: exit=1; files=1 cases=15 pass=5 fail=10 error=0 skip=0
- failing: ac1_wifiOnlyOn_incomingWorkRequiresUnmeteredNetwork[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac2_wifiOnlyOff_incomingWorkRequiresConnectedNetwork[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac3_wifiOnlyOff_triggerRequestEqualsScheduleIncomingRequest[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac3_wifiOnlyOn_triggerRequestEqualsScheduleIncomingRequest[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac4_encryptionDegradedAtRunTime_noIncomingWork_success[FAIL com.google.common.truth.AssertionErrorWithFacts] ac5_autoBackupOffAtRunTime_noIncomingWork_success[FAIL com.google.common.truth.AssertionErrorWithFacts] ac6_incomingDelayNegative_noIncomingWork_success[FAIL com.google.common.truth.AssertionErrorWithFacts] ac6_incomingDelayZero_noIncomingWork_success[FAIL com.google.common.truth.AssertionErrorWithFacts] ac9_allowedState_logsTheTriggerLineAtDebugBeforeTheSchedulerEnqueues[FAIL com.google.common.truth.AssertionErrorWithFacts] ac9_encryptionDegraded_logsTheTriggerLineThenTheSchedulersBlockedLine_success[FAIL com.google.common.truth.AssertionErrorWithFacts] 
- restored: 2026-11-21T15:35:31Z blob-ok raw-ok main-clean

### M-T7 APPLYING 2026-11-21T15:35:32Z
- file: worker; edit: `        scheduler.scheduleIncoming()` -> `        // M-T7 removed: scheduler.scheduleIncoming()`
- classes: BackupTriggerWorkerTest; expected failing (10): ac1, ac2, ac3 x2, ac7, ac8a, ac8b, ac8c, ac9 x2; passing: ac4, ac5, ac6 x2, ic1

- run: exit=1; files=1 cases=15 pass=5 fail=10 error=0 skip=0
- failing: ac1_wifiOnlyOn_incomingWorkRequiresUnmeteredNetwork[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac2_wifiOnlyOff_incomingWorkRequiresConnectedNetwork[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac3_wifiOnlyOff_triggerRequestEqualsScheduleIncomingRequest[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac3_wifiOnlyOn_triggerRequestEqualsScheduleIncomingRequest[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac7_debounce_incomingWorkWaitsEnqueuedForTheIncomingDelay[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac8a_triggerTwice_oneActiveIncomingWork_secondReplacesFirst[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac8b_triggerThenScheduleIncoming_oneActiveIncomingWork_secondReplacesFirst[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac8c_scheduleIncomingThenTrigger_oneActiveIncomingWork_secondReplacesFirst[FAIL com.google.common.truth.AssertionErrorWithFacts] ac9_allowedState_logsTheTriggerLineAtDebugBeforeTheSchedulerEnqueues[FAIL com.google.common.truth.AssertionErrorWithFacts] ac9_encryptionDegraded_logsTheTriggerLineThenTheSchedulersBlockedLine_success[FAIL com.google.common.truth.AssertionErrorWithFacts] 
- restored: 2026-11-21T15:41:08Z blob-ok raw-ok main-clean

### M-T8 APPLYING 2026-11-21T15:41:09Z
- file: worker; edit: the three lines `scheduler.scheduleIncoming()`, blank, `return Result.success()` -> `val job = scheduler.scheduleIncoming()`, blank, `return if (job == null) Result.failure() else Result.success()`
- classes: BackupTriggerWorkerTest; expected failing (5): ac4, ac5, ac6 x2, ac9_encryptionDegraded; passing: the other 10

- run: exit=1; files=1 cases=15 pass=10 fail=5 error=0 skip=0
- failing: ac4_encryptionDegradedAtRunTime_noIncomingWork_success[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac5_autoBackupOffAtRunTime_noIncomingWork_success[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac6_incomingDelayNegative_noIncomingWork_success[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac6_incomingDelayZero_noIncomingWork_success[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac9_encryptionDegraded_logsTheTriggerLineThenTheSchedulersBlockedLine_success[FAIL com.google.common.truth.ComparisonFailureWithFacts] 
- restored: 2026-11-21T15:43:07Z blob-ok raw-ok main-clean

### M-T9 APPLYING 2026-11-21T15:43:08Z
- file: worker; edit: log text `enqueueing delayed incoming backup` -> `scheduling delayed incoming backup`
- classes: BackupTriggerWorkerTest; expected failing (2): both ac9; passing: other 13

- run: exit=1; files=1 cases=15 pass=13 fail=2 error=0 skip=0
- failing: ac9_allowedState_logsTheTriggerLineAtDebugBeforeTheSchedulerEnqueues[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac9_encryptionDegraded_logsTheTriggerLineThenTheSchedulersBlockedLine_success[FAIL com.google.common.truth.ComparisonFailureWithFacts] 
- restored: 2026-11-21T15:45:01Z blob-ok raw-ok main-clean

### M-T10 APPLYING 2026-11-21T15:45:01Z
- file: worker; edit: the two lines `@HiltWorker` + `class BackupTriggerWorker @AssistedInject constructor(` -> `class BackupTriggerWorker @AssistedInject constructor(`
- classes: BackupTriggerWorkerTest; expected failing (1): ic1; passing: other 14

- generated folder after the run: BackupTriggerWorker_Factory.java 
- run: exit=1; files=1 cases=15 pass=14 fail=1 error=0 skip=0
- failing: ic1_hiltWiring_assistedFactoryIsBoundUnderThePersistedClassName[FAIL com.google.common.truth.AssertionErrorWithFacts] 
- restored: 2026-11-21T15:47:34Z blob-ok raw-ok main-clean

### M-T11 APPLYING 2026-11-21T15:47:41Z
- file: worker; edit: `class BackupTriggerWorker @AssistedInject constructor(` -> `class BackupTriggerWorker constructor(` (@HiltWorker kept)
- classes: BackupTriggerWorkerTest; expected: build fails at the KSP step (invalid, not counted)

- run: exit=1 (build step output in mutations/M-T11-build.txt)
- restored: 2026-11-21T15:48:41Z blob-ok raw-ok main-clean

### M-T2 APPLYING 2026-11-21T15:48:41Z
- file: scheduler; edit: `if (secretStore.isEncryptionDegraded()) {` -> `if (false && secretStore.isEncryptionDegraded()) {` (first of the two lines)
- classes: BackupTriggerWorkerTest, WorkManagerSchedulerTest; expected failing (3): BTW ac4, ac9_encryptionDegraded; WMS se002_scheduleIncoming_encryptionDegraded_returnsNullNoWork; passing 13 + 24

- run: exit=1; files=2 cases=40 pass=37 fail=3 error=0 skip=0
- failing: se002_scheduleIncoming_encryptionDegraded_returnsNullNoWork[FAIL com.google.common.truth.AssertionErrorWithFacts] ac4_encryptionDegradedAtRunTime_noIncomingWork_success[FAIL com.google.common.truth.AssertionErrorWithFacts] ac9_encryptionDegraded_logsTheTriggerLineThenTheSchedulersBlockedLine_success[FAIL com.google.common.truth.AssertionErrorWithFacts] 
- restored: 2026-11-21T15:52:08Z blob-ok raw-ok main-clean

### M-T3 APPLYING 2026-11-21T15:52:08Z
- file: scheduler; edit: `if (!preferences.isAutoBackupEnabled || timeoutSecs <= 0) {` -> `if (timeoutSecs <= 0) {`
- classes: BackupTriggerWorkerTest, WorkManagerSchedulerTest; expected failing (2): BTW ac5; WMS ac5_scheduleIncoming_autoBackupDisabled_returnsNull; passing 14 + 24

- run: exit=1; files=2 cases=40 pass=38 fail=2 error=0 skip=0
- failing: ac5_scheduleIncoming_autoBackupDisabled_returnsNull[FAIL com.google.common.truth.AssertionErrorWithFacts] ac5_autoBackupOffAtRunTime_noIncomingWork_success[FAIL com.google.common.truth.AssertionErrorWithFacts] 
- restored: 2026-11-21T15:54:44Z blob-ok raw-ok main-clean

### M-T4 APPLYING 2026-11-21T15:54:45Z
- file: scheduler; edit: `if (!preferences.isAutoBackupEnabled || timeoutSecs <= 0) {` -> `if (!preferences.isAutoBackupEnabled) {`
- classes: BackupTriggerWorkerTest, WorkManagerSchedulerTest; expected failing (3): BTW both ac6; WMS ac5_scheduleIncoming_zeroTimeout_returnsNull; passing 13 + 24

- run: exit=1; files=2 cases=40 pass=37 fail=3 error=0 skip=0
- failing: ac5_scheduleIncoming_zeroTimeout_returnsNull[FAIL com.google.common.truth.AssertionErrorWithFacts] ac6_incomingDelayNegative_noIncomingWork_success[FAIL com.google.common.truth.AssertionErrorWithFacts] ac6_incomingDelayZero_noIncomingWork_success[FAIL com.google.common.truth.AssertionErrorWithFacts] 
- restored: 2026-11-21T15:57:12Z blob-ok raw-ok main-clean

### M-T5 APPLYING 2026-11-21T15:57:13Z
- file: scheduler; edit: `.setInitialDelay(timeoutSecs.toLong(), TimeUnit.SECONDS)` -> `.setInitialDelay(0L, TimeUnit.SECONDS)`
- classes: BackupTriggerWorkerTest, WorkManagerSchedulerTest; expected failing (4): BTW ac7, both ac3, ac9_allowedState; passing 11 + 25

- run: exit=1; files=2 cases=40 pass=36 fail=4 error=0 skip=0
- failing: ac3_wifiOnlyOff_triggerRequestEqualsScheduleIncomingRequest[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac3_wifiOnlyOn_triggerRequestEqualsScheduleIncomingRequest[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac7_debounce_incomingWorkWaitsEnqueuedForTheIncomingDelay[FAIL com.google.common.truth.ComparisonFailureWithFacts] ac9_allowedState_logsTheTriggerLineAtDebugBeforeTheSchedulerEnqueues[FAIL com.google.common.truth.ComparisonFailureWithFacts] 
- restored: 2026-11-21T15:59:34Z blob-ok raw-ok main-clean

### M-T6 APPLYING 2026-11-21T15:59:35Z
- file: scheduler; edit: `ExistingWorkPolicy.REPLACE` -> `ExistingWorkPolicy.KEEP` in the INCOMING enqueueUniqueWork call
- classes: BackupTriggerWorkerTest, WorkManagerSchedulerTest; expected failing (3): BTW ac8a, ac8b, ac8c; passing 12 + 25

- run: exit=1; files=2 cases=40 pass=37 fail=3 error=0 skip=0
- failing: ac8a_triggerTwice_oneActiveIncomingWork_secondReplacesFirst[FAIL com.google.common.truth.AssertionErrorWithFacts] ac8b_triggerThenScheduleIncoming_oneActiveIncomingWork_secondReplacesFirst[FAIL com.google.common.truth.AssertionErrorWithFacts] ac8c_scheduleIncomingThenTrigger_oneActiveIncomingWork_secondReplacesFirst[FAIL com.google.common.truth.AssertionErrorWithFacts] 
- restored: 2026-11-21T16:02:01Z blob-ok raw-ok main-clean

### M-T12 APPLYING 2026-11-21T16:02:01Z
- file: scheduler; edit: `.setInitialDelay(timeoutSecs.toLong(), TimeUnit.SECONDS)` -> `.setInitialDelay(60L, TimeUnit.SECONDS)`
- classes: BackupTriggerWorkerTest, WorkManagerSchedulerTest; expected failing (1): BTW ac9_allowedState; passing 14 + 25

- run: exit=1; files=2 cases=40 pass=39 fail=1 error=0 skip=0
- failing: ac9_allowedState_logsTheTriggerLineAtDebugBeforeTheSchedulerEnqueues[FAIL com.google.common.truth.ComparisonFailureWithFacts] 
- restored: 2026-11-21T16:04:25Z blob-ok raw-ok main-clean

## Verdicts (each against the plan 4.8 table; every expected-failing set matched exactly, every other case passed)

| Id | Failing | Expected | Verdict |
|----|---------|----------|---------|
| M-T1 | 10 (ac1, ac2, ac3 x2, ac4, ac5, ac6 x2, ac9 x2) | same | detected |
| M-T7 | 10 (ac1, ac2, ac3 x2, ac7, ac8a, ac8b, ac8c, ac9 x2) | same | detected |
| M-T8 | 5 (ac4, ac5, ac6 x2, ac9_encryptionDegraded) | same | detected |
| M-T9 | 2 (both ac9) | same | detected |
| M-T10 | 1 (ic1); generated folder afterwards held only a stale BackupTriggerWorker_Factory.java (it compiled, N7) | same | detected |
| M-T11 | build fails at the KSP step: '@HiltWorker annotated class should contain exactly one @AssistedInject annotated constructor.' | same | invalid (does not compile), detected by the build; not counted |
| M-T2 | 3 (BTW ac4, ac9_encryptionDegraded; WMS se002_...) | same | detected |
| M-T3 | 2 (BTW ac5; WMS ac5_...autoBackupDisabled) | same | detected |
| M-T4 | 3 (BTW both ac6; WMS ac5_...zeroTimeout) | same | detected |
| M-T5 | 4 (BTW ac3 x2, ac7, ac9_allowedState) | same | detected |
| M-T6 | 3 (BTW ac8a, ac8b, ac8c) | same | detected |
| M-T12 | 1 (BTW ac9_allowedState) | same | detected |

Final state after M-T12: worker blob c47a0452d040f959df134fc8a71fda5be6b6180f, sha256 equals SW; scheduler blob 0194308c... equals HEAD, sha256 equals SS, `git diff --quiet HEAD -- <scheduler>` exit 0; `git status --porcelain -- app/src/main` is exactly the worker. 11 counted detections; M-T11 is the compile-only control.
