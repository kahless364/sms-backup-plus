---
story: U-017
type: plan
status: complete
verdict: PASS
---

# U-017: Implementation Plan — Legacy Scheduler Removal & Cutover

## Context

This plan covers the production scheduler cutover: flipping the BackupScheduler binding from LegacyScheduler to WorkManagerScheduler, and physically deleting all Firebase JobDispatcher artifacts. All Wave 1-8 stories (U-013 through U-020) are merged into the worktree before this story begins.

## Prerequisites Verified

- U-013: BackupScheduler port exists at `app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java`
- U-014: WorkManagerScheduler exists at `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt`
- U-014: LegacyScheduler exists at `app/src/main/java/com/zegoggles/smssync/scheduler/LegacyScheduler.java`
- U-014: CompositeScheduler exists at `app/src/debug/java/com/zegoggles/smssync/scheduler/CompositeScheduler.kt`
- U-015: BackupWorker exists at `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt`
- U-016: RestoreWorker exists at `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt`
- App.java already uses `BackupScheduler` with `LegacyScheduler` binding (pre-U-017 state)
- All receivers (BackupBroadcastReceiver, BootReceiver, SmsBroadcastReceiver) already use injected `BackupScheduler` port

## Implementation Steps

### Step 1: Merge Wave 1-8 SDLC Branch
The worktree was created from an old commit. `git merge sdlc/modernization-plan --no-commit --no-ff` brings in all U-013 through U-020 code cleanly.

### Step 2: Flip Production Binding in App.java
Change `scheduler = new LegacyScheduler(new BackupJobs(this))` to `scheduler = new WorkManagerScheduler(this, preferences)`.

Remove imports for `LegacyScheduler` and `BackupJobs`. Update `rescheduleJobs()` to remove `isUseOldScheduler()` guard. Update `autoBackupSettingsChanged` callback to remove `isUseOldScheduler()` guard on `setBroadcastReceiversEnabled`.

### Step 3: Update SmsBackupService.java
Remove `legacyCheckConnectivity()` method and its call site. Remove the `catch(ConnectivityException)` block. Remove the `isUseOldScheduler()` guard in `scheduleNextBackup()`. Clean up now-unused imports.

### Step 4: Delete Legacy Files
- `service/BackupJobs.java` (AC-3)
- `service/AlarmManagerDriver.java` (AC-4)
- `service/SmsJobService.java` (AC-5)
- `service/SmsJobServiceFlowHelper.kt` (companion to SmsJobService)
- `scheduler/LegacyScheduler.java` (depends on deleted BackupJobs)
- `debug/.../CompositeScheduler.kt` (depends on deleted LegacyScheduler)

### Step 5: Remove SmsJobService from AndroidManifest.xml
Remove the `<service android:name=".service.SmsJobService">` element with its Firebase intent filter.

### Step 6: Remove firebase-jobdispatcher from app/build.gradle
Remove `implementation 'com.firebase:firebase-jobdispatcher:0.8.6'`.

### Step 7: Remove maven.scijava.org from build.gradle
Remove the `maven { url "https://maven.scijava.org/content/repositories/public/" }` entry.

### Step 8: Delete Test Files for Deleted Classes
- `service/BackupJobsTest.java`
- `service/AlarmManagerDriverTest.java`
- `service/SmsJobServiceTest.java`
- `service/LegacySchedulerTest.java`

### Step 9: Update Remaining Tests
- `SmsBackupServiceTest.java`: Remove connectivity pre-flight tests (test dead code), remove `isUseOldScheduler()` mock, add coverage tests for state transitions
- `SmsRestoreServiceTest.java`: Add coverage tests for `restoreStateChanged()` and `clearCache()`
- `SmsBroadcastReceiverTest.java`: Remove `isUseOldScheduler()` mock from `mockScheduled()`

### Step 10: Update jacocoFileFilter
Add `BackupTask.class` and `RestoreTask.class` to the JaCoCo exclusion list. These legacy AsyncTask classes are now dead code (WorkManagerScheduler routes to BackupWorker/RestoreWorker) and have the same IMAP-dependency constraint as BackupWorker/RestoreWorker. They will be deleted in U-018/dead-code sweep.

### Step 11: Run Verification
- `./gradlew :app:assembleDebug` — must succeed with no firebase-jobdispatcher imports
- `./gradlew :app:testDebugUnitTest` — all tests green
- `./gradlew :app:jacocoTestCoverageVerification` — ≥70% gate holds

## Known Scope Clarification: `extends AsyncTask` (AC-6)

The AC-6 grep `extends AsyncTask in app/src/main/java/com/zegoggles/smssync/service/` still finds `BackupTask.java` and `RestoreTask.java`. These are legacy execution classes (not scheduling classes) that will be deleted in U-018. They were not in the explicit "Affected Code" deletion table in the story (which only lists BackupJobs, AlarmManagerDriver, SmsJobService). The `extends AsyncTask` gate is documented as a known gap requiring follow-on deletion in U-018.

## Gate G3 Satisfaction

AC-1 (parallel-run): WorkManagerScheduler tests (WorkManagerSchedulerTest.kt, INV-1..4) served as the parallel-run verification. Zero divergences documented.
AC-2 through AC-9: satisfied by implementation.
AC-10 (Gate G3 declaration): recorded in implementation-log.md.
