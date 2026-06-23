---
type: story
status: done
artifact_type: user-story
priority: high
complexity: high
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
id: U-049
title: Retire the legacy backup/restore Service dual-dispatch layer
pipeline: ''
domain: modernization
resolution: done
requirement_source: assessment:20260623-post-migration-assessment#AR-002
sprint: '000012'
---

# U-049: Retire the legacy backup/restore Service dual-dispatch layer

## Story
As a maintainer of the SMS Backup+ codebase, I want to delete `SmsBackupService`, `SmsRestoreService`, and `ServiceBase` along with their manifest entries and move foreground notification and WorkInfo observation into the workers directly, so that the ~1,200-line transitional dual-dispatch layer is gone and `MainActivity` dispatches backup/restore through the Hilt-injected scheduler without any intermediate Service hop.

## Source
Derived from assessment 20260623-post-migration-assessment, finding AR-002. See `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/architecture.md`.

## Acceptance Criteria
- [ ] AC-1: `SmsBackupService.java`, `SmsRestoreService.java`, and `ServiceBase.java` are deleted from the source tree; their `<service>` entries are removed from `AndroidManifest.xml`; no import of these classes exists anywhere in production sources.
- [ ] AC-2: `BackupWorker` and `RestoreWorker` each call `setForeground(ForegroundInfo(...))` at the start of execution with the same notification channel and content that `ServiceBase` previously provided, so the foreground-service notification continues to appear while backup/restore is running.
- [ ] AC-3: The WorkInfo progress observer that was live in the Service layer is re-homed (either into the workers themselves via `setProgress`, or into `MainViewModel` collecting from `WorkManager.getWorkInfoByIdLiveData`), and the BUG-005 observer-leak fix (observer deregistered in `onDestroy`) is preserved in the new location.
- [ ] AC-4: `MainActivity.java:420` no longer calls `startService(...)`; it calls the injected `WorkManagerScheduler.scheduleManual(...)` (or equivalent scheduler method) directly; there is no remaining `startService` / `bindService` call for backup or restore in `MainActivity`.
- [ ] AC-5: Build green: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` passes; the `BackupBroadcastReceiver` broadcast path (`CNTR-MODERNIZATION-005`) still reaches the scheduler and triggers a worker without any Service intermediary.

## Affected Code
| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` (~505 lines) | Live manifest-registered service; UI entry point for backup dispatch | Delete entirely |
| `app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` (~378 lines) | Live manifest-registered service; UI entry point for restore dispatch | Delete entirely |
| `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` (~311 lines) | Base class providing foreground notification + LiveData↔Flow bridge | Delete entirely |
| `app/src/main/AndroidManifest.xml:~(service entries)` | Declares `SmsBackupService` and `SmsRestoreService` as `<service>` | Remove both `<service>` elements |
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java:420` | Calls `startService(Intent(..., SmsBackupService.class))` | Replace with direct call to injected `WorkManagerScheduler.scheduleManual(...)` |
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` | Does not call `setForeground` | Add `setForeground(ForegroundInfo(...))` at worker start |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` | Does not call `setForeground` | Add `setForeground(ForegroundInfo(...))` at worker start |

## Existing Behavior to Preserve
- Public broadcast contract `CNTR-MODERNIZATION-005`: `com.zegoggles.smssync.BACKUP` received by `BackupBroadcastReceiver` must continue to trigger a backup without any permission or exported-flag change — the receiver dispatches to the scheduler, not to a Service. Do not add any `android:permission` to `BackupBroadcastReceiver`.
- Backup and restore progress UI: the user must see a foreground notification ("Backup running…" / "Restore running…") with the same channel and notification ID that `ServiceBase` used; no regression in notification appearance or cancellation.
- BUG-005 observer-leak fix: the WorkInfo observer must be deregistered when the owning lifecycle (Activity or ViewModel) is destroyed; this invariant must be preserved in whatever component takes over observation.
- The scheduler's ability to cancel an in-flight backup/restore (via `WorkManager.cancelUniqueWork`) must continue to work.

## Verification Steps
1. Run `grep -r "SmsBackupService\|SmsRestoreService\|ServiceBase" app/src/main/` — expect zero results.
2. Run `grep -r "startService" app/src/main/java/com/zegoggles/smssync/activity/` — expect zero results for backup/restore invocations.
3. Build: `./gradlew :app:assembleDebug` with no compilation errors.
4. Run unit tests: `./gradlew :app:testDebugUnitTest` — all existing service-layer tests (if any) are either deleted alongside the classes they tested or updated; net test count does not regress without explanation.
5. Send the `com.zegoggles.smssync.BACKUP` broadcast via `adb shell am broadcast -a com.zegoggles.smssync.BACKUP com.zegoggles.smssync` with `third_party_integration` enabled — confirm a WorkManager job is enqueued (visible in `adb shell dumpsys jobscheduler`).
6. Run `./gradlew :app:jacocoTestCoverageVerification` — all gated packages at or above LINE 70%.

## Technical Context
- AR-002 root cause: the Services were retained as the live UI dispatch layer during the WorkManager migration because `MainActivity` was wired to `startService`. They no longer execute backup/restore logic (they call `scheduleManual` → worker) but add a LiveData↔Flow bridge (~1,200 lines of transitional glue) and a second scheduling surface.
- `setForeground`/`ForegroundInfo` is the WorkManager-native replacement for a foreground Service; it must be called early in `doWork()` to avoid ANR on Android 12+ (foreground-service start restriction). Use `WorkManager.setExpedited` or `setForeground` with the same notification channel the Services used.
- The BUG-005 fix (`sdlc/artifacts/stories/BUG-005-worker-foreground-observer-leak-on-destroy.md`) removed an observer leak in the Service layer; ensure the replacement observation site cleans up in `onCleared()` (ViewModel) or `onDestroy()` (Activity).
- This story depends on U-048 landing first: once the static `App.syncStateRepository()` alias is gone, the Services lose their state-emission anchor, making deletion straightforward.

## Notes
- Sprint A (DI & Legacy Retirement). Sequence after U-048.
- ~1,200 lines net removed. This is the single largest maintainability payoff in the sprint.
- The `BackupBroadcastReceiver` is NOT touched by this story — it already dispatches to the scheduler, not to a Service. Do not modify it.
