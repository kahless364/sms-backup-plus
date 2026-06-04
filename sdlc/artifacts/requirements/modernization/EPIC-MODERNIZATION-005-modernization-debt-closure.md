---
status: approved
artifact_type: epic
related_requirements:
  - REQ-MODERNIZATION-012
  - REQ-MODERNIZATION-013
  - REQ-MODERNIZATION-014
related_stories: []
change_records: []
id: EPIC-MODERNIZATION-005
title: 'Modernization Debt Closure: ACL Purity, Legacy-Path Removal, Hilt Completion'
domain: modernization
---

# Modernization Debt Closure: ACL Purity, Legacy-Path Removal, Hilt Completion

## Description

Sprint-001 completed 29 of 29 planned stories and achieved all primary architectural
targets: AGP 8.7.3 / targetSdk 35, TLS hardening via `PinnedCertStore`, an encrypted
`SecretStore`, WorkManager + `CoroutineWorker` scheduling (firebase-jobdispatcher
deleted), Otto replaced by `StateFlow`/`SharedFlow`, Hilt bootstrapped on `App` with
modules for all singleton dependencies, k-9 mail vendored and isolated behind a
`MailTransport` ACL port, and the engine classes (`BackupTask`, `RestoreTask`,
`BackupWorker`, `RestoreWorker`) fully rewired onto app-owned types. Three
bounded items were intentionally deferred and documented as in-scope residuals: the
`MailTransport` ACL still has two `com.fsck.k9.mail.MessagingException` FQN catch sites
inside `service.*` engine classes (one in `BackupTask.java` wrapping
`MessageConverter.convertMessages()`, one in `ServiceBase.java` wrapping the
`K9MailTransport` constructor); `BackupTask` and `RestoreTask` still extend `AsyncTask`
and are excluded from the JaCoCo coverage gate because their execution paths are believed
dead (all production scheduling routes through `BackupWorker`/`RestoreWorker`) but that
belief has not been confirmed by live-path instrumentation; and `SmsBackupService` and
`SmsRestoreService` still carry manual-construction coexistence shims
(`getBackupTask()`/`getRestoreTask()` factory methods and null-check fallbacks in
`ServiceBase.getPreferences()`/`getAuthPreferences()`) because `@AndroidEntryPoint` was
deferred when Robolectric tests that instantiate anonymous service subclasses failed under
the Hilt lifecycle. This epic closes all three items to reach the full architectural
target state defined in the sprint-001 modernization plan.

## Scope

### In Scope

- **REQ-MODERNIZATION-012** — Move the two remaining `com.fsck.k9.mail.MessagingException`
  catch sites out of `service.*` engine classes and into the `mail.transport` adapter/converter
  boundary so that zero `com.fsck.k9.*` type references (import statements or FQN usages)
  exist in `service.*` production source. The FQN catch in `BackupTask.java:308`
  (wrapping `MessageConverter.convertMessages()`) and the FQN catch in
  `ServiceBase.java:173` (wrapping the `K9MailTransport` constructor) are the two specific
  residual sites.
- **REQ-MODERNIZATION-013** — Confirm that `BackupTask` and `RestoreTask` are unreachable
  from all live execution paths (WorkManager, broadcast receivers, service `onStartCommand`
  paths), then delete both classes and their test counterparts, and remove the four
  corresponding entries from the `jacocoFileFilter` exclusion list in `app/build.gradle`.
- **REQ-MODERNIZATION-014** — Add `@AndroidEntryPoint` to `SmsBackupService` and
  `SmsRestoreService`, migrate the affected Robolectric test classes to the Hilt test
  harness (`@HiltAndroidTest` / `HiltAndroidRule`), and remove the manual-construction
  coexistence shims (`getBackupTask()`, `getRestoreTask()`, and the null-check fallbacks
  in `ServiceBase`) that were introduced as bootstrap coexistence patterns in U-022/U-023.

### Out of Scope

- Live-account end-to-end verification (IMAP backup/restore against a real mail server);
  this is separately deferred and tracked outside the modernization requirements corpus.
- New user-facing features of any kind.
- Further dependency upgrades beyond those already required to close REQ-012/013/014
  (e.g., Kotlin promotion, Play Billing 7.x, People API — those are EPIC-MODERNIZATION-004
  territory or separate elective work).
- Instrumented / on-device test additions; all coverage targets are Robolectric unit tests.
- Changes to `BackupWorker`, `RestoreWorker`, `WorkManagerScheduler`, or any scheduler
  code (those paths are already production and green).

## Success Criteria

The following criteria are the exit conditions for this epic. Each must be independently
verifiable by running the stated command or inspection without access to a live IMAP
account.

1. **ACL purity (REQ-012)**: `grep -rn "com\.fsck\.k9" app/src/main/java/com/zegoggles/smssync/service/`
   returns zero results — no import statement and no FQN usage of any `com.fsck.k9.*`
   type survives in `service.*` production source files.

2. **ACL purity at adapter boundary (REQ-012)**: `grep -rn "com\.fsck\.k9" app/src/main/java/com/zegoggles/smssync/mail/`
   returns results only within the `mail/transport/` and `mail/` packages that were
   already permitted under the ACL design (i.e., `K9MailTransport.java` and
   `BackupImapStore.java` and their delegates). No new k-9 surface is introduced
   outside those files.

3. **Legacy path deletion (REQ-013)**: `find app/src -name "BackupTask.java" -o -name "RestoreTask.java"`
   returns zero results; the corresponding test files
   (`BackupTaskTest.java`, `RestoreTaskTest.java`) are also absent.

4. **JaCoCo exclusion list cleaned (REQ-013)**: The four entries
   `**/BackupTask.class`, `**/BackupTask$*.class`, `**/RestoreTask.class`,
   `**/RestoreTask$*.class` are absent from the `jacocoFileFilter` list in
   `app/build.gradle`.

5. **Hilt service injection live (REQ-014)**: `grep -rn "@AndroidEntryPoint"
   app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java
   app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java`
   returns one match per file.

6. **Coexistence shims removed (REQ-014)**: `grep -rn "getBackupTask\|getRestoreTask\|injectedPreferences\|injectedAuthPreferences"
   app/src/main/java/com/zegoggles/smssync/service/` returns zero results; the
   null-check fallback constructors (`new Preferences(getApplicationContext())` and
   `new AuthPreferences(this)`) in `ServiceBase.getPreferences()` /
   `ServiceBase.getAuthPreferences()` are absent.

7. **Full unit suite green (all three)**: `./gradlew :app:testDebugUnitTest` completes
   with zero failures and zero newly-skipped tests relative to the sprint-001 baseline
   (568 passing, 2 skipped).

8. **Coverage gate holds (all three)**: `./gradlew :app:jacocoTestCoverageVerification`
   completes successfully with `service`, `mail`, and `auth` packages each at or above
   the 70% line-coverage threshold.

9. **Debug build assembles (all three)**: `./gradlew :app:assembleDebug` completes
   successfully with zero compiler errors or warnings introduced by this epic's changes.

10. **No behavior change (all three)**: The set of externally observable behaviors —
    IMAP backup scheduling, broadcast contract (`com.zegoggles.smssync.BACKUP`), OAuth2
    token refresh, restore checkpoint resumption, and notification state transitions —
    is identical before and after. No new permissions, intent filters, or exported
    components are added.

## Related Requirements

- **REQ-MODERNIZATION-012** — Seal the `MailTransport` ACL exception boundary: eliminate
  the two surviving `com.fsck.k9.mail.MessagingException` FQN catch sites from `service.*`
  engine classes by relocating the exception translation into the `mail.transport` adapter
  layer where k-9 types are already permitted.
- **REQ-MODERNIZATION-013** — Remove the legacy `AsyncTask` execution path: confirm
  `BackupTask`/`RestoreTask` are dead code (unreachable from all live paths), then delete
  both classes, their test files, and the four `jacocoFileFilter` exclusions that were
  added to accommodate them.
- **REQ-MODERNIZATION-014** — Complete Hilt service injection: apply `@AndroidEntryPoint`
  to `SmsBackupService` and `SmsRestoreService`, migrate Robolectric tests to the Hilt
  test harness, and remove all manual-construction coexistence shims introduced during
  the U-022/U-023 bootstrap.

## Notes

### Sequencing Constraint

REQ-MODERNIZATION-013 and REQ-MODERNIZATION-014 interact at `SmsBackupService`,
`SmsRestoreService`, and `ServiceBase`. Specifically:

- REQ-013 deletes `BackupTask` and `RestoreTask`. `SmsBackupService.getBackupTask()` and
  `SmsRestoreService.getRestoreTask()` construct those classes; they cannot be deleted
  before REQ-013 confirms the classes are dead.
- REQ-014 deletes `getBackupTask()` and `getRestoreTask()` as part of removing the
  coexistence shims. REQ-014 therefore has a logical dependency on REQ-013 having already
  confirmed (and ideally deleted) `BackupTask`/`RestoreTask`, because the factory methods
  that REQ-014 removes are the last callers of those classes.
- The recommended sequencing is: **REQ-012 first** (purely additive boundary work inside
  `mail.transport`, does not touch `ServiceBase` or the service classes), then **REQ-013**
  (confirms liveness and deletes dead code, including the factory methods in the services
  that reference `BackupTask`/`RestoreTask`), then **REQ-014** (applies `@AndroidEntryPoint`
  and removes the remaining shims in `ServiceBase` with confidence that no manual-construction
  path survives after REQ-013).
- If REQ-013 and REQ-014 must be worked in parallel (separate stories), REQ-013 must
  complete and its story must be merged before REQ-014's story begins the shim-removal
  phase.

### Internal Quality — No User-Facing Change

All three requirements are internal structural improvements. Users will observe no change
in behavior, UI, notification copy, OAuth flows, or data. The only observable effect is
that the application becomes architecturally complete per the modernization plan, making
future work (e.g., EPIC-MODERNIZATION-004 strategic hardening) simpler to execute without
needing to work around coexistence shims or legacy dead code.

### Traceability

This epic closes the remaining open items documented in:
- `sdlc/artifacts/build/sprints/sprint-001/U-026/implementation-log.md` (ACL residual
  at `BackupTask.java:308` and `ServiceBase.java:173`)
- `sdlc/artifacts/build/sprints/sprint-001/U-017/implementation-log.md` (Gate G3 note:
  `BackupTask`/`RestoreTask` still extend `AsyncTask`, added to `jacocoFileFilter`)
- `sdlc/artifacts/build/sprints/sprint-001/U-022/implementation-log.md` (AC-4/AC-6
  deviation: `@AndroidEntryPoint` deferred; coexistence fallbacks documented)
- `sdlc/artifacts/build/sprints/sprint-001/U-023/implementation-log.md` (`getBackupTask()`
  and `getRestoreTask()` factory methods introduced as coexistence shims)
