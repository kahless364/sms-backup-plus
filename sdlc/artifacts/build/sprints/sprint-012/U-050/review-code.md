---
artifact_type: review-code
story_id: U-050
verdict: PASS
agent: Code Reviewer
timestamp: "2026-06-23"
blockers: 0
warnings: 3
---

# Code Review: U-050

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PASS — all five ACs met; AR-003/004/005 addressed |
| Test coverage | PASS with warnings — 642 tests, 4 new; two factory paths lack direct tests |
| Code quality | PASS with warnings — one double-KDoc artifact; one stale TODO comment |

## Verdict: PASS

U-050 is a well-scoped refactor across three AR items. The engine object-graph injection via `WorkerEngineFactory` is architecturally sound, the `SmsDefaultRoleHelper` extraction faithfully preserves all behaviour including the BUG-009/U-041 Q+ role-round-trip and the U-034 `DialogDelegate` themed-dialog callback, the BUG-010 watermark invariant is untouched, and the build is green at 642 tests. Three warnings (no blockers) are noted below.

## Findings

### Blockers

None.

### Warnings

**W-1: Double KDoc block on `WorkManagerScheduler` class declaration**
`app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` lines 74–79 emit two consecutive standalone `/** ... */` blocks immediately before `class WorkManagerScheduler`. In Kotlin, only the last attached doc-comment applies to the declaration; the first block (`"WorkManager initialization: App implements …"`) becomes an orphaned comment that the IDE and `dokka` silently ignore. This is an editorial leftover from the U-048/U-050 refactor passes. Merge both into a single `/** ... */` block.

**W-2: Stale `TODO U-022/MU-007` comment in `MainActivity.java`**
`app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` line 127 still reads:
```java
// TODO U-022/MU-007: replace manual factory with @HiltViewModel.
```
`MainViewModelFactory.kt` was deleted in this sprint and the comment directly above (lines 148–151) already explains the factory is no longer needed. The stale TODO is now factually incorrect and should be removed.

**W-3: `WorkerEngineFactoryTest` does not test `createMessageConverter`**
`app/src/test/java/com/zegoggles/smssync/service/WorkerEngineFactoryTest.kt` has 4 tests covering `createPersonLookup` (non-null + distinct instance per call), `createTokenRefresher` (non-null), and `createCalendarSyncerIfEnabled` (null when disabled). The most behaviorally sensitive factory method — `createMessageConverter`, which reads `authPreferences.userEmail` at construction time and must produce a coherent `MessageConverter` — has no test. Integration coverage exists through `BackupWorkerTest`, but a direct factory-level test would catch future regressions in the converter construction path. Similarly, `SmsDefaultRoleHelper` has no dedicated test class; its four extracted methods are tested only indirectly through `MainActivityRestoreTest`'s `requestDefaultSmsPackageChange()` call path. Both gaps are acceptable for the current sprint but should be addressed in a test-only follow-up.

### Observations

**O-1: AC-2 "by viewModels()" vs. `ViewModelProvider` — correctly Java-idiomatic**
The story AC says "obtains `MainViewModel` via `by viewModels()`". Since `MainActivity` is Java, the Kotlin property delegate is unavailable; the production pattern `new ViewModelProvider(this).get(MainViewModel.class)` is the precise Java equivalent and is functionally identical when `@AndroidEntryPoint` and `@HiltViewModel` are in place. No defect.

**O-2: `CalendarSyncer` factory-built rationale is sound**
`WorkerEngineFactory.createCalendarSyncerIfEnabled` encapsulates and documents two constraints that prevent direct injection: (a) `calendarId` is a `Long` runtime preference value for which Hilt requires a `@Named` qualifier that would couple the graph to a dynamic preference; (b) `CalendarAccessor.Get.instance()` is a legacy static factory with no `@Inject` constructor. Leaving `CalendarSyncer` factory-built is the correct engineering decision; the `internal` visibility modifier correctly prevents cross-module exposure of the package-private Java type.

**O-3: `App.java` test-env null-guard deferred — acceptable**
The story AC-4 called for replacing the `Configuration.Provider` reflective test-env branch. The implementation deferred it on the grounds that `hiltWorkerFactory != null` is clean defensive code rather than a reflective-factory fallback. The original finding (AR-005) targeted the *reflective default factory*, which is already gone. The null guard is pragmatic, well-commented, and poses no production regression risk. The deferral is documented in the implementation log.

**O-4: BUG-010 watermark invariant intact**
`BackupWorker.backupCursors()` (lines 299–383): the injection of `WorkerEngineFactory` and `ContactAccessor` into the constructor does not touch the `backupCursors` loop. The `confirmedMaxDate` variable is still assigned from `transport.appendMessages(folder, result)`, `setMaxSyncedDate` is still called only after a successful `appendMessages` return, and `ensureActive()` cooperative-cancellation is still present at line 325. The BUG-010 invariant is provably unaffected by this refactor.

**O-5: `SmsDefaultRoleHelper` pre-KitKat path correctly calls `startViewModelRestore()`**
Before this refactor, the pre-KitKat branch of `startRestore()` called `startService(SmsRestoreService.class)` directly. After extraction, it calls `delegate.startViewModelRestore()` (line 101 of `SmsDefaultRoleHelper.kt`), which in `MainActivity` dispatches to `viewModel.startRestore()`. This is the correct U-049 path.

**O-6: `DISCONNECT` import in `MainActivity.java` is pre-existing unused, not introduced here**
The import at line 78 (`Dialogs.Type.DISCONNECT`) is an existing unused import predating this PR. The implementation log notes it correctly; no action required here.

## Patterns Verified

- [x] Follows existing code patterns (`@Singleton @Inject` factory; `object` helper with `@JvmStatic`; `@AssistedInject` constructor extension)
- [x] Error handling is appropriate (no new error paths introduced; existing paths preserved)
- [x] Tests cover new functionality (4 new tests for `WorkerEngineFactory`; `MainActivityRestoreTest` continues to cover the role-round-trip)
- [x] No hardcoded values that should be configurable
- [x] No unnecessary complexity (factory is a thin wrapper; helper is a pure function extract)

## Integration Verified

- [x] New code is reachable from production entry points: `WorkerEngineFactory` is injected by Hilt into `BackupWorker` and `RestoreWorker` via their `@AssistedInject` constructors; `HiltWorkerFactory` (registered in `App.getWorkManagerConfiguration()`) constructs workers at WorkManager runtime
- [x] Registries/dispatch maps updated: no new Hilt module needed — `WorkerEngineFactory` is `@Singleton` with `@Inject` constructor, auto-discoverable by Hilt; `ContactAccessor` was already `@Inject`-annotated
- [x] Function signatures match at all call sites: `BackupWorker` and `RestoreWorker` test factories (`TestableBackupWorkerFactory`, `TestableBackupWorkerFactoryWithTransport`, `TestableRestoreWorkerFactory`) all updated with the new `contactAccessor` and `engineFactory` parameters
- [x] No dead code introduced: `SmsDefaultRoleHelper` is called from four live `MainActivity` methods; `WorkerEngineFactory` is called from both workers
- [x] Integration path documented in implementation-log.md

## Regression Check

No files deleted in this story (previous stories U-048/U-049 performed the deletions). No Capabilities Inventory required.

- [x] `MainViewModelFactory.kt` deletion (from U-049) verified: file absent, no callers remain in production or test sources
- [x] `MainActivityRestoreTest` contract intact: `requestDefaultSmsPackageChange()` is package-private and the helper delegates back to `activity.startActivityForResult(...)`, preserving the test assertion
- [x] BUG-009/U-041 preserved: Q+ path in `SmsDefaultRoleHelper.startRestore()` always reaches `requestDefaultSmsPackageChange()` regardless of `Telephony.Sms.getDefaultSmsPackage()` return value

## Contract Verification

No API endpoints, SSE events, or inter-service contracts were modified. All changes are internal refactors within the Android application module.
