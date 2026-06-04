---
status: approved
type: functional
artifact_type: requirement
priority: medium
related_requirements:
  - REQ-MODERNIZATION-006
  - REQ-MODERNIZATION-008
  - REQ-MODERNIZATION-014
related_stories: []
related_design_docs: []
traces_to:
  - EPIC-MODERNIZATION-005
change_records: []
id: REQ-MODERNIZATION-013
title: Remove the legacy AsyncTask backup/restore execution path
domain: modernization
epic: EPIC-MODERNIZATION-005
---

# REQ-MODERNIZATION-013: Remove the legacy AsyncTask backup/restore execution path

## Description

Delete `BackupTask.java` and `RestoreTask.java` — the `AsyncTask`-based backup and restore
execution classes that became dead code when `WorkManagerScheduler` was flipped to route all
backup and restore operations through `BackupWorker` and `RestoreWorker` (U-017). Before
deleting them, confirm by static call-graph analysis that no production code path constructs
or executes them; if any live invocation is found, migrate it to the worker path first. Remove
the now-obsolete `jacocoFileFilter` exclusions for the deleted classes from `app/build.gradle`
so the 70% coverage gate measures only the surviving code.

## Context

Sprint-001 completed a branch-by-abstraction replacement of the AsyncTask-based execution
engine with CoroutineWorkers:

- U-015 added `BackupWorker.kt` and `RestoreWorker.kt` alongside the existing
  `BackupTask.java` / `RestoreTask.java` (no cutover; both paths live simultaneously).
- U-017 flipped the production scheduler binding to `WorkManagerScheduler`, which routes all
  backup and restore enqueue calls to `BackupWorker` / `RestoreWorker`. `BackupTask` and
  `RestoreTask` were explicitly noted as dead code in U-017's implementation log, with
  deletion deferred to U-018.
- U-018 addressed minSdk/dead-code cleanup for other targets and did not delete these classes,
  leaving the deferral open.
- U-023 added `@Inject` constructors to `BackupTask` and `RestoreTask` and added factory
  methods `SmsBackupService.getBackupTask()` and `SmsRestoreService.getRestoreTask()`.
- U-026 rewired both `BackupTask`/`RestoreTask` and `BackupWorker`/`RestoreWorker` onto the
  `MailTransport` ACL port, meaning the same backup/restore IMAP logic now exists in both
  execution paths simultaneously — a live duplication risk.

**Critical finding established during requirements authoring (static analysis):**
`SmsBackupService` and `SmsRestoreService` are NOT dead code. `MainActivity.java:406` calls
`startService(new Intent(this, SmsBackupService.class))` for manual backup, and
`MainActivity.java:411` calls `startService(new Intent(this, SmsRestoreService.class))` for
restore. Both services are declared with `foregroundServiceType="dataSync"` in
`AndroidManifest.xml`. As long as these service entry points remain, `getBackupTask()` and
`getRestoreTask()` are reachable production call sites. The services themselves must be
migrated away from the AsyncTask dispatch path — either by having `SmsBackupService` /
`SmsRestoreService` delegate to the WorkManager scheduler rather than constructing and
executing AsyncTasks directly, or by replacing the `startService` call sites in
`MainActivity` with WorkManager enqueue calls — before `BackupTask` and `RestoreTask` can
be deleted.

Until REQ-MODERNIZATION-014 completes Hilt injection for the services, the `ServiceBase`
foreground-service promotion, wakelock/wifi-lock lifecycle, and `clearCache()` on
`SmsRestoreService` must be verified to have a live equivalent in the worker path before
the service-to-AsyncTask dispatch is cut over.

Retaining dead execution code alongside the live worker code creates a correctness risk
(both paths evolve in parallel, divergence goes undetected), increases maintenance burden,
and forces the JaCoCo gate to exclude `BackupTask`/`RestoreTask` from line coverage
measurement, masking the actual coverage ratio of the `service.*` package.

## Requirements

### Confirm non-invocation of AsyncTask classes before deletion

Before any file is deleted, perform a static call-graph analysis to confirm that no
production code path constructs or executes `BackupTask` or `RestoreTask`. The analysis
must cover the full reachability graph from all Android entry points (Activity, Service,
BroadcastReceiver, WorkManager Workers, and any `@Inject`-constructed components). If any
live invocation is found, it must be migrated to the `BackupWorker`/`RestoreWorker` path
and verified before deletion proceeds.

Specifically: `MainActivity.startBackup()` calls
`startService(new Intent(this, SmsBackupService.class))`, which invokes
`SmsBackupService.handleIntent()` → `backup()` → `getBackupTask().execute(...)`.
Likewise, `MainActivity.startRestore()` calls
`startService(new Intent(this, SmsRestoreService.class))`, which invokes
`SmsRestoreService.handleIntent()` → `getRestoreTask().execute(...)`. These call sites are
live. Deletion of `BackupTask`/`RestoreTask` is therefore **blocked** until either:

- (a) `SmsBackupService.backup()` and `SmsRestoreService.handleIntent()` are rewritten to
  enqueue via `WorkManagerScheduler` instead of constructing and executing AsyncTasks, OR
- (b) `MainActivity.startBackup()` and `MainActivity.startRestore()` are rewritten to
  call `WorkManagerScheduler` directly, bypassing the services.

Option (a) is preferred because it eliminates the manual-backup codepath duplication
without removing the foreground-service scaffolding prematurely (that scaffolding is
needed by REQ-MODERNIZATION-014 for Hilt injection).

#### Acceptance Criteria

1. A grep search for `BackupTask` and `RestoreTask` across `app/src/main/java/` returns
   zero results for `new BackupTask`, `new RestoreTask`, `.execute(` on instances of those
   types, and `getBackupTask()` / `getRestoreTask()` factory method calls — confirming all
   construction and execution sites have been removed or migrated before any deletion occurs.
   If this criterion is not met, the implementation is blocked and must migrate the live
   call sites first.

### Delete BackupTask.java and RestoreTask.java

Once AC-1 is satisfied (all invocations removed or migrated), delete
`app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` and
`app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java`.
Delete any companion test files that test only the deleted classes:
`BackupTaskTest.java` and `RestoreTaskTest.java`.

#### Acceptance Criteria

2. `BackupTask.java` and `RestoreTask.java` are absent from the repository
   (`find app/src -name "BackupTask.java" -o -name "RestoreTask.java"` returns zero lines).
3. `BackupTaskTest.java` and `RestoreTaskTest.java` are absent from the repository
   (`find app/src/test -name "BackupTaskTest*" -o -name "RestoreTaskTest*"` returns zero lines).
4. A grep search for `import.*BackupTask`, `import.*RestoreTask`, `extends AsyncTask` across
   `app/src/main/java/` returns zero results, confirming no remaining production file
   references the deleted classes.

### Scope SmsBackupService and SmsRestoreService deletion conditionally

`SmsBackupService` and `SmsRestoreService` are NOT deleted by this requirement. They remain
as live foreground services invoked from `MainActivity` and declared in
`AndroidManifest.xml`. Their deletion is in scope only after REQ-MODERNIZATION-014 (Hilt
service injection) is complete AND `MainActivity` has been migrated away from
`startService` to WorkManager enqueue. That migration is explicitly out of scope for
REQ-MODERNIZATION-013.

The `getBackupTask()` and `getRestoreTask()` factory methods on those services ARE deleted
as part of this requirement (they are the direct construction sites for the deleted
AsyncTask classes), after the services' `handleIntent()` methods have been rewritten per
AC-1 above to no longer call them.

#### Acceptance Criteria

5. `SmsBackupService.getBackupTask()` and `SmsRestoreService.getRestoreTask()` are absent
   from the codebase (grep for `getBackupTask` and `getRestoreTask` in
   `app/src/main/java/` returns zero results).
6. `SmsBackupService` and `SmsRestoreService` remain present in `app/src/main/java/` and
   in `AndroidManifest.xml` with `android:foregroundServiceType="dataSync"` unchanged.
7. `SmsBackupService.backup()` and `SmsRestoreService.handleIntent()` no longer construct
   or execute an `AsyncTask`; they delegate to the `BackupScheduler` (WorkManager) path
   instead, confirmed by reading the modified method bodies.

### Preserve foreground-service and wakelock semantics in the worker path

`SmsBackupService` manages foreground-service promotion (`startForeground(BACKUP_ID,
notification)`) for manual backups and `SmsRestoreService` manages `FULL_WAKE_LOCK`
acquisition on API 19+. Before the AsyncTask path is cut over, confirm that the
`CoroutineWorker` path provides equivalent guarantees. `BackupWorker` and `RestoreWorker`
run inside WorkManager, which holds a `PARTIAL_WAKE_LOCK` via `WorkManager`'s own wakelock
mechanism. The foreground-service promotion behavior for manual backups must be documented
as intentionally removed (WorkManager handles this via `setForeground()` / `ForegroundInfo`
if desired) or retained via a separate mechanism.

#### Acceptance Criteria

8. A documented decision exists (in the story plan or implementation log for the story
   implementing this requirement) stating whether foreground-service promotion and the API
   19+ `FULL_WAKE_LOCK` in `SmsRestoreService` are preserved via `CoroutineWorker.setForeground()`
   or intentionally dropped, with a behavioral justification. The decision must reference
   WorkManager's built-in wakelock semantics for `CoroutineWorker`.

### Remove jacocoFileFilter exclusions for deleted classes

Remove the `jacocoFileFilter` entries for `BackupTask` and `RestoreTask` from
`app/build.gradle` after the classes are deleted, so the 70% coverage gate evaluates the
full `service.*` package without artificial exclusions.

#### Acceptance Criteria

9. The following four glob patterns are absent from the `jacocoFileFilter` list in
   `app/build.gradle` after deletion:
   `'**/BackupTask.class'`, `'**/BackupTask$*.class'`,
   `'**/RestoreTask.class'`, `'**/RestoreTask$*.class'`.
10. `./gradlew :app:jacocoTestCoverageVerification` passes with the updated filter,
    confirming the 70% gate still holds for `com.zegoggles.smssync.service*` after the
    exclusion removal. The gate must pass because `BackupTask` and `RestoreTask` no longer
    contribute uncovered lines; removing them from the exclusion list and from the codebase
    simultaneously must not cause the gate to fall below 70%.

### Full test suite passes

#### Acceptance Criteria

11. `./gradlew :app:testDebugUnitTest` completes with zero test failures after all
    deletions and migrations are applied.
12. `./gradlew :app:assembleDebug` completes successfully, confirming no compile-time
    references to the deleted classes remain in production or test source.

### On-device smoke verification

#### Acceptance Criteria

13. A manual backup triggered from the app UI (MainActivity "Backup Now") enqueues a
    `BackupWorker` job via WorkManager and completes without crash, verified by inspecting
    the WorkManager status in the device/emulator logcat: a log line containing
    `WorkManagerScheduler` and a `BackupWorker` UUID appears within 5 seconds of the
    trigger.
14. A manual restore triggered from the app UI enqueues a `RestoreWorker` job via
    WorkManager and completes without crash, verified by the same logcat inspection method.
15. A scheduled (REGULAR) backup that fires via the WorkManager periodic work request
    completes successfully, confirmed by the `BackupState(FINISHED_BACKUP, ...)` state
    emission visible in the app log or logcat.

## Rationale

`BackupTask` and `RestoreTask` exist in a coexistence state that was explicitly declared
temporary in U-015 and U-017. Their continued presence creates three concrete risks:

1. **Logic duplication and divergence**: Both `BackupTask`/`RestoreTask` and
   `BackupWorker`/`RestoreWorker` were rewired onto `MailTransport` in U-026, meaning
   identical IMAP logic now lives in two places. Any future bug fix or enhancement must be
   applied twice, and a missed update in one path silently diverges behavior.
2. **Coverage gate distortion**: The JaCoCo exclusion of `BackupTask.class` and
   `RestoreTask.class` suppresses potentially hundreds of uncovered lines from the 70% gate
   measurement. After deletion the gate will reflect actual coverage of the surviving code.
3. **Maintenance cost**: `BackupTask` and `RestoreTask` carry `@Inject` constructors
   (U-023), `@SuppressWarnings("deprecation")` for `AsyncTask`, and FQN catch blocks for
   the bounded-residual `MessagingException` (U-026). Every future story that touches the
   service layer must reason about both execution paths.

## Constraints

- Deletion of `BackupTask` and `RestoreTask` is blocked until AC-1 is satisfied. Do not
  delete the files before confirming zero live invocations.
- `SmsBackupService` and `SmsRestoreService` must not be deleted by this requirement. Their
  deletion depends on REQ-MODERNIZATION-014 (Hilt service injection completion) and a
  separate `MainActivity` migration that routes manual backup/restore through WorkManager.
- The behavior of `BackupWorker` and `RestoreWorker` must not be changed by this
  requirement. The worker code is the surviving execution path; this requirement removes
  only the dead parallel path.
- Sequence with REQ-MODERNIZATION-014: REQ-MODERNIZATION-014 adds `@AndroidEntryPoint` to
  `SmsBackupService` and `SmsRestoreService` and activates Hilt field injection. Both
  requirements touch `SmsBackupService` and `SmsRestoreService`. To avoid merge conflicts,
  one of the following sequencing rules must be observed:
  - Option A: Complete REQ-MODERNIZATION-013 first (migrate service dispatch away from
    AsyncTask, delete `BackupTask`/`RestoreTask`), then REQ-MODERNIZATION-014 adds
    `@AndroidEntryPoint` to the modified services.
  - Option B: Complete REQ-MODERNIZATION-014 first (Hilt injection on services), then
    REQ-MODERNIZATION-013 migrates the now-Hilt-injected services.
  - The implementing developer must confirm the chosen sequence in the story plan to
    prevent simultaneous conflicting edits.

## Verification Method

1. Static grep verification: all grep-zero checks listed in the acceptance criteria above
   are run and return zero matches.
2. Build verification: `./gradlew :app:assembleDebug` succeeds with no compile errors.
3. Test suite verification: `./gradlew :app:testDebugUnitTest` passes with zero failures.
4. Coverage gate verification: `./gradlew :app:jacocoTestCoverageVerification` passes at
   70% or above for `com.zegoggles.smssync.service*` with the updated `jacocoFileFilter`.
5. On-device smoke: manual backup and restore triggered from the UI enqueue WorkManager
   jobs confirmed in logcat; scheduled backup completes via the REGULAR periodic request.

## Notes

- U-018 was the originally planned deletion story but addressed a different scope (minSdk
  cleanup) and explicitly did not delete `BackupTask`/`RestoreTask`. This requirement
  closes that deferred item.
- The `@Inject` annotation on `BackupTask` and `RestoreTask` constructors (added by U-023)
  does not make them Hilt-managed components — the Hilt graph is never asked to build
  them in the production path. Their deletion does not affect the Hilt component graph.
- `BackupWorker` and `RestoreWorker` are also currently excluded from the `jacocoFileFilter`
  (added by U-015 for the same IMAP-dependency reason). Those exclusions are not removed
  by this requirement — only the `BackupTask`/`RestoreTask` exclusions are removed.
  Coverage of the worker classes themselves is deferred to an integration/instrumented test
  story.
- The `SmsRestoreService.clearCache()` method and the `asyncClearCache()` call in
  `onCreate()` mirror `RestoreWorker.clearAppCache()` (already present per U-015 capabilities
  inventory). Confirm behavioral equivalence before the service's AsyncTask dispatch is
  removed.
