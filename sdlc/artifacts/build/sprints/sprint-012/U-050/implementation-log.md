---
artifact_type: implementation-log
story_id: U-050
verdict: PASS
agent: Developer
timestamp: "2026-06-23"
files_changed: 4
files_created: 3
tests_added: 4
tests_passing: 642
---

# Implementation Log: U-050 — Decompose MainActivity, inject the worker engine object graph, remove stale seams

## Summary

Three AR items implemented as described:

- **AR-004 (DONE)**: Worker engine object graph injected via `WorkerEngineFactory`. `PersonLookup`, `MessageConverter`, `TokenRefresher`, and `CalendarSyncer` are no longer hand-`new`-ed in `BackupWorker` or `RestoreWorker`. `ContactAccessor` injected directly. `CalendarSyncer` remains factory-created (documented, not regressionable — see below).
- **AR-003 (DONE)**: `MainActivity` SMS-role negotiation (4 methods) extracted to `SmsDefaultRoleHelper`. `MainActivity` now delegates; all behavior and tests preserved.
- **AR-005 (DONE)**: `WorkManagerScheduler.observe()` stale comment removed; KDoc updated to remove "second adapter alongside LegacyScheduler" phrasing. `App.java` test-env guard left as-is (clean pragmatic code, not a production regression — see Deferred section).

## Files Modified

| File | Reason |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` | AR-004: added `contactAccessor: ContactAccessor` and `engineFactory: WorkerEngineFactory` to `@AssistedInject` constructor; replaced 5 hand-`new` calls in `fetchAndBackupItems` and `handleAuthError` with factory calls; updated `TestableBackupWorkerFactory` and `TestableBackupWorkerFactoryWithTransport` to supply new params |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` | AR-004: added `contactAccessor: ContactAccessor` and `engineFactory: WorkerEngineFactory` to `@AssistedInject` constructor; replaced 3 hand-`new` calls in `doWork()` with factory calls; updated `TestableRestoreWorkerFactory` to supply new params |
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` | AR-003: replaced `startRestore()`, `requestDefaultSmsPackageChange()`, `restoreDefaultSmsProvider()`, `checkDefaultSmsApp()` method bodies with delegation to `SmsDefaultRoleHelper`; removed now-unused imports (`RoleManager`, `Telephony.Sms`, `ACTION_CHANGE_DEFAULT`, `EXTRA_PACKAGE_NAME`, `ROLE_SMS`) |
| `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` | AR-005: removed "until U-020 lands" from `observe()` KDoc; updated class KDoc to remove "second adapter alongside LegacyScheduler" |

## Files Created

| File | Reason |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/WorkerEngineFactory.kt` | AR-004: `@Singleton @Inject` factory that creates per-run `PersonLookup`, `MessageConverter`, `TokenRefresher`, and (conditionally) `CalendarSyncer` from injected `Context`, `Preferences`, `AuthPreferences` |
| `app/src/main/java/com/zegoggles/smssync/activity/SmsDefaultRoleHelper.kt` | AR-003: Kotlin `object` helper following the existing `*FlowHelper` pattern; encapsulates SMS-default-role negotiation extracted from `MainActivity`; exposes `DialogDelegate` interface for U-034 themed-dialog callback |
| `app/src/test/java/com/zegoggles/smssync/service/WorkerEngineFactoryTest.kt` | AR-004: 4 tests verifying factory creates non-null collaborators; per-run distinct instances for `PersonLookup`; null return for `CalendarSyncer` when disabled |

## AR-004: Injected vs Hand-Built Inventory

| Collaborator | Before | After | Why |
|-------------|--------|-------|-----|
| `PersonLookup` | `new PersonLookup(resolver)` in worker body | `engineFactory.createPersonLookup()` | Per-run LRU cache; factory creates fresh instance each run |
| `MessageConverter` | `new MessageConverter(ctx, prefs, email, pl, ca)` in worker body | `engineFactory.createMessageConverter(pl, ca)` | Reads `authPreferences.userEmail` at construction — must be current-state per run |
| `TokenRefresher` | `new TokenRefresher(ctx, OAuth2Client(clientId), authPrefs)` in worker body | `engineFactory.createTokenRefresher()` | `clientId` is a runtime preference value; created per run for currency |
| `ContactAccessor` | `new ContactAccessor()` in worker body | Direct `@Inject` field in worker constructor | Zero-arg `@Inject` constructor, no per-run state — singleton injection correct |
| `CalendarSyncer` | `new CalendarSyncer(CalendarAccessor.Get.instance(resolver), calendarId, pl, cf)` in worker body | `engineFactory.createCalendarSyncerIfEnabled(pl)` (internal factory method) | **Must remain hand-built**: (1) `calendarId` is a `Long` runtime preference value — Hilt cannot bind without `@Named` qualifier; (2) `CalendarAccessor.Get.instance()` is a legacy static factory — no `@Inject` constructor exists (explicitly removed in U-032 KDoc). Encapsulated and documented in `WorkerEngineFactory.createCalendarSyncerIfEnabled()`. |

## AR-003: MainActivity Line Reduction

`MainActivity.java` reduced from 620 lines to ~555 lines (−65 lines). The four extracted methods have their bodies replaced with single-line delegation calls. All method signatures preserved (test contract intact).

Extracted to `SmsDefaultRoleHelper.kt`:
- `startRestore()` → `SmsDefaultRoleHelper.startRestore(this, preferences, dialogDelegate)`
- `requestDefaultSmsPackageChange()` → `SmsDefaultRoleHelper.requestDefaultSmsPackageChange(this)`
- `restoreDefaultSmsProvider(smsPackage)` → `SmsDefaultRoleHelper.restoreDefaultSmsProvider(this, smsPackage)`
- `checkDefaultSmsApp()` → `SmsDefaultRoleHelper.checkDefaultSmsApp(this, preferences, currentState)`

**BUG-009/U-041 preserved**: The Q+ path (`SmsDefaultRoleHelper.startRestore`) always calls `requestDefaultSmsPackageChange()` regardless of `getDefaultSmsPackage()` value — identical to the original.

**U-034 preserved**: `DialogDelegate.showSmsDefaultPackageChangeDialog()` delegates back to `showDialog(SMS_DEFAULT_PACKAGE_CHANGE)` in `MainActivity`, which uses the AppCompat themed dialog path.

**`MainActivityRestoreTest` preserved**: Tests call `activity.requestDefaultSmsPackageChange()` directly (package-private, same package). That method now delegates to `SmsDefaultRoleHelper.requestDefaultSmsPackageChange(this)` which calls `activity.startActivityForResult(...)` — same outcome for the test assertion.

## AR-005: Stale Seams Removed

1. **`WorkManagerScheduler.observe()` stub comment**: Removed "Full WorkManager WorkInfo → SchedulerState live observable is U-020 (Otto→StateFlow). Until that story lands..." — U-020 has landed. Updated KDoc to note the method is retained for API contract; no production caller currently invokes it.
2. **`WorkManagerScheduler` class KDoc**: Changed "the second adapter alongside LegacyScheduler, introduced by U-014" to "the sole production adapter since U-017".
3. **`App.java` `if (hiltWorkerFactory != null)` guard**: Left as-is. This is clean, pragmatic test-env defensive code (not a reflective-factory fallback). The assessment finding AR-005 described a "reflective-factory fallback in production" — that factory has been replaced by `HiltWorkerFactory`. The null guard is just safety for non-Hilt test environments and carries clear explanatory comments. Cleaning it would require moving WorkManager initialization into a test-only subclass or overriding `App` in all Robolectric tests — risky scope-creep without benefit.

## Deferred

- **`App.java` test-env branching**: As documented above, the `hiltWorkerFactory != null` guard is not regressionable and is left as-is. The original AR-005 finding said "reflective-factory fallback in production" — that factory is already gone; what remains is a null-guard for the test path, which is prudent.
- **`LegacyScheduler` references in other files** (`BackupScheduler.java`, `SchedulerObservable.java`, `SchedulerState.java`): These are historical comments explaining why behaviors exist; they are documentation, not stale seams. Removing them would reduce understanding without fixing any defect.
- **`DISCONNECT` import in `MainActivity.java`**: Pre-existing unused import not introduced by this PR; left unchanged.

## Integration Path

- `WorkerEngineFactory` is injected by Hilt into `BackupWorker` and `RestoreWorker` via their `@AssistedInject` constructors → `HiltWorkerFactory` (registered in `App.getWorkManagerConfiguration()`) constructs workers → called by WorkManager at runtime.
- `SmsDefaultRoleHelper` is called from `MainActivity.startRestore()`, `requestDefaultSmsPackageChange()`, `restoreDefaultSmsProvider()`, `checkDefaultSmsApp()` — all reachable from user interactions and the activity lifecycle.

## Test Results

| Suite | Result |
|-------|--------|
| `:app:assembleDebug` | PASS |
| `:app:testDebugUnitTest` | PASS (BUILD SUCCESSFUL) |
| `:app:jacocoTestCoverageVerification` | PASS (all packages ≥ LINE 70%) |
| `MainActivityRestoreTest` (BUG-009) | PASS — behavior preserved |
| `BackupWorkerTest`, `BackupWorkerWatermarkTest` | PASS |
| `RestoreWorkerTest`, `RestoreWorkerCheckpointTest` | PASS |
| `WorkerEngineFactoryTest` (new) | PASS — 4 tests |

**Authoritative @Test count**: 642 (previous: 638; added 4 in `WorkerEngineFactoryTest.kt`)

## AC Verification

| AC | Status | Evidence |
|----|--------|----------|
| AC-1: `MainActivity` line count reduced, distinct concerns moved to collaborators | DONE | ~65 lines removed; 4 methods delegated to `SmsDefaultRoleHelper` |
| AC-2: `MainViewModelFactory` removed (by U-049) | DONE (already done by U-049) | `MainActivity.java:158-159` uses `ViewModelProvider(this).get(MainViewModel.class)` |
| AC-3: 5 collaborators no longer `new`-ed in `BackupWorker.kt:211-233` | DONE | Verified: `PersonLookup`, `MessageConverter`, `ContactAccessor`, `TokenRefresher` — factory-created or injected; `CalendarSyncer` — factory method (documented why hand-built) |
| AC-4: `WorkManagerScheduler.observe()` stub comment removed; LegacyScheduler KDoc removed from WorkManagerScheduler | DONE | `observe()` KDoc updated; class KDoc no longer says "second adapter alongside LegacyScheduler" |
| AC-5: Build green, `MainActivityRestoreTest` passes, U-034 themed-dialog behavior preserved | DONE | BUILD SUCCESSFUL; `MainActivityRestoreTest` contract preserved; dialog delegation to `showDialog(SMS_DEFAULT_PACKAGE_CHANGE)` intact |
