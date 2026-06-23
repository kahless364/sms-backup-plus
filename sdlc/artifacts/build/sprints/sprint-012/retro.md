---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-012
started: 2026-06-23
completed: 2026-06-23
---

# Retro: sprint-012 — DI & Legacy Retirement (Batch A)

## Business Summary
**Objective.** Top maintainability win from the post-migration assessment: finish the Hilt DI cutover and retire the legacy Service dual-dispatch layer (AR-001/002/003).

**Outcome.** All 3 stories done; 643 tests green, jacoco LINE ≥70%, assembleDebug green. Net ~1,500 lines removed.
- **U-048 / AR-001:** `@Provides`→`@Binds` for SchedulerModule/EventModule/ContactsModule; `@Inject` constructors; removed the `new` construction paths. BUG-004 single-instance invariant now structurally guaranteed by a single `@Singleton @Binds` (the `App.syncStateRepository()` static now bridges *from* the Hilt instance, not a parallel `new`). The static accessor is retained as a bridge for ~30 legacy callers (documented).
- **U-049 / AR-002:** deleted `SmsBackupService`/`SmsRestoreService`/`ServiceBase` (~1,200 lines) + manifest `<service>` entries; foreground moved into the workers (`setForeground`/`ForegroundInfo`); progress observation re-homed into `MainViewModel` (`viewModelScope`/`onCleared` preserves the BUG-005 no-leak fix); `MainActivity` enqueues via the injected scheduler. CNTR-MODERNIZATION-005 broadcast path and the BUG-009 Q+ role round-trip preserved.
- **U-050 / AR-003/004/005:** new `WorkerEngineFactory` injects PersonLookup/ContactAccessor/MessageConverter/TokenRefresher into the workers (CalendarSyncer left factory-created — runtime calendarId, documented); `SmsDefaultRoleHelper` extracted from MainActivity (preserves Q+ role + U-034 dialog); stale seams cleaned.

## Process Observations
- **Wave sequencing for shared files.** U-049 and U-050 both edit `MainActivity.java`, so wave-2 was run sequentially (U-049 → merge → U-050 off the updated base) rather than in parallel — avoided a merge conflict. U-049 also pre-empted part of U-050's scope (deleted MainViewModelFactory), so U-050 was re-briefed before dispatch.
- **Review caught a real runtime crash.** The U-049 code review FAILED on a BLOCKER: the 2-arg `ForegroundInfo` constructor → `IllegalArgumentException` on targetSdk 35 (foreground-service-type mask 0x0 vs declared `dataSync`). This is exactly the class of bug deferred on-device testing would have hit at runtime — static review caught it first. Remediated to the 3-arg `FOREGROUND_SERVICE_TYPE_DATA_SYNC` form (API 29+); re-verified.
- **QA caught a stale-AC miss.** U-050 QA FAILED on the leftover `TODO U-022` markers (AC-2). Remediated (removed 3 markers + double KDoc + stale jacoco exclusions; added a BUG-005 onCleared test). Both FAILs → PASS after remediation.
- **Worktree isolation** used throughout (origin/master local-ref workaround); every branch verified `--is-ancestor` before merge.
- **On-device validation DEFERRED** per user instruction (hold until all-clear). Build + unit + code/security/QA gates all satisfied; emulator round-trip (backup/restore still works after the service retirement) is the outstanding confidence step.

## Gate Results
U-048 code PASS / sec PASS / QA PASS. U-049 code FAIL→PASS (remediated) / sec PASS / QA PASS. U-050 code PASS / sec PASS / QA FAIL→PASS (remediated). Wave gate PASS.

## Issues / Recommendations
1. Closing-bookend status mis-derivation (now 12×) — corrected to `completed`.
2. **Deferred on-device check (when all-clear):** confirm a backup AND restore still run end-to-end after the service retirement + worker-foreground change (the ForegroundInfo path especially), and that the notification shows.
3. Minor warnings logged (not blocking): MainViewModel new methods have light test coverage beyond the onCleared test; `OAuth2Client` still has one hand-built `PeopleApiContactsAdapter` (pre-existing, out of scope).
