---
artifact_type: qa-results
story_id: "U-031"
verdict: "PASS"
qa_agent: "Developer (self-QA)"
timestamp: "2026-06-04"
---

# QA Results: U-031 — Remove Legacy AsyncTask Execution Path

## Build Verification

| Command | Result |
|---------|--------|
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL, 0 failures |
| `./gradlew :app:jacocoTestCoverageVerification` | BUILD SUCCESSFUL (service* ≥70% LINE gate passes) |

## AC Verification Checklist

| AC | Description | Verified |
|----|-------------|---------|
| AC-1 | WorkInfo observer bridge drives backupStateChanged()/restoreStateChanged() | PASS — bridge registered in registerBackupWorkInfoObserver() / registerRestoreWorkInfoObserver() |
| AC-2 | backup() calls scheduleManual(backupType) | PASS — SmsBackupService.java:169 |
| AC-3 | handleIntent() calls scheduleRestore(RestoreSchedulerConfig) | PASS — SmsRestoreService.java:139–144 |
| AC-4 | SmsRestoreService: canWriteToSmsProvider() guard preserved | PASS — SmsRestoreService.java:121–125 |
| AC-5 | SyncEvent.Cancel routed to WorkManager.cancelUniqueWork() | PASS — WorkManagerCancelCollector, registered in both services |
| AC-6 | BackupCancelCollector.kt deleted | PASS — file not present, grep-zero confirmed |
| AC-7 | BackupTask.java, RestoreTask.java, getBackupTask(), getRestoreTask() deleted | PASS — all grep-zero verified |
| AC-8 | BackupTaskTest.java, RestoreTaskTest.java deleted | PASS — files not present |
| AC-9 | SmsRestoreService acquires/releases FULL_WAKE_LOCK around enqueue+observe | PASS — acquireLocks() before scheduleRestore(), releaseLocks() in terminal handler |
| AC-10 | jacocoFileFilter entries for BackupTask/RestoreTask removed | PASS — grep-zero for BackupTask.class/RestoreTask.class in build.gradle |
| AC-11 | CalendarSyncer @Inject NOT removed | PASS — CalendarSyncer.java unchanged |
| AC-12 | Worker code NOT changed | PASS — BackupWorker.kt and RestoreWorker.kt not modified |

## Grep-Zero Verification

```
grep -rn "new BackupTask|new RestoreTask|getBackupTask|getRestoreTask" app/src/main/java/
→ ZERO functional invocations (only comments)

grep -rn "import.*BackupTask|import.*RestoreTask" app/src/main/java/
→ ZERO

find app/src -name "BackupCancelCollector.kt"
→ ZERO

find app/src -name "BackupTask.java" -o -name "RestoreTask.java"
→ ZERO

find app/src/test -name "BackupTaskTest*" -o -name "RestoreTaskTest*"
→ ZERO

grep -n "BackupTask.class|RestoreTask.class" app/build.gradle
→ ZERO

grep -n "nine operations" app/src/main/java/.../BackupScheduler.java
→ ZERO (javadoc now reads "ten operations")
```

## Test Coverage

- `com.zegoggles.smssync.service*` package: passes 70% LINE gate (was 61.8% before new restore service tests)
- `com.zegoggles.smssync.mail*` package: passes 70% LINE gate (unchanged)
- `com.zegoggles.smssync.auth*` package: passes 70% LINE gate (unchanged)

## Notes

- The `extends AsyncTask` occurrences in `PinCertificateEnrollmentFlow.java` and `OAuth2CallbackTask.java` are out of scope and NOT removed in this story
- Robolectric test warnings about WorkManager re-initialization (`WorkManagerTestInitHelper.initializeTestWorkManager` called per-test in `@Before`) are benign — all tests pass
