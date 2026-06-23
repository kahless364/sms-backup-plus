---
verdict: PASS
story_id: U-050
phase: planning
---

# Plan: U-050 — Decompose MainActivity, inject the worker engine object graph, remove stale seams

## Scope Clarification

U-049 already completed AC-2 (MainViewModelFactory deletion; MainActivity uses ViewModelProvider directly). The remaining three items are:

- **AR-004 (primary)**: inject the worker engine object graph
- **AR-003**: decompose MainActivity SMS-role concern
- **AR-005**: remove stale scheduler seams

## AR-004: Worker Engine Object Graph Injection

**Approach**: Create `WorkerEngineFactory` (`@Singleton`, `@Inject` constructor) in `com.zegoggles.smssync.service` that captures `@ApplicationContext Context`, `Preferences`, `AuthPreferences` from Hilt and provides factory methods for per-run collaborators.

**Why factory pattern, not direct injection:**
- `PersonLookup` holds a per-run LRU cache — a fresh instance per run is required.
- `MessageConverter` reads `authPreferences.userEmail` at construction time — must be created from current prefs state per run.
- `TokenRefresher` captures `OAuth2Client(authPreferences.oAuth2ClientId)` — clientId is a runtime value.
- `CalendarSyncer` takes a `Long calendarId` from prefs and uses `CalendarAccessor.Get.instance(resolver)` (static factory) — cannot be injected; documented in factory method.
- `ContactAccessor` has a zero-arg `@Inject` constructor with no per-run state — injected directly into workers.

**Changes:**
1. Create `WorkerEngineFactory.kt` with `@Singleton @Inject` constructor
2. Add `contactAccessor: ContactAccessor` and `engineFactory: WorkerEngineFactory` params to `BackupWorker` `@AssistedInject` constructor
3. Add same to `RestoreWorker` `@AssistedInject` constructor
4. Replace hand-`new` calls in `fetchAndBackupItems` / `doWork` with factory calls
5. Update `TestableBackupWorkerFactory` and `TestableRestoreWorkerFactory` to supply the new params

## AR-003: MainActivity Decomposition

**Approach**: Extract SMS-default-role negotiation into a new `SmsDefaultRoleHelper` Kotlin object following the existing `*FlowHelper` pattern.

**Extracted methods**: `startRestore()`, `requestDefaultSmsPackageChange()`, `restoreDefaultSmsProvider()`, `checkDefaultSmsApp()`

**`DialogDelegate` interface**: allows `SmsDefaultRoleHelper` to call back into `MainActivity` for dialog display, preserving the U-034 themed-dialog path.

**Preserved behaviors**: BUG-009/U-041 Q+ role-request path, `MainActivityRestoreTest` test contract (same package-private method signatures), U-034 themed dialogs.

## AR-005: Stale Seams Removal

1. `WorkManagerScheduler.kt`: remove "until U-020 lands" stale comment from `observe()` KDoc; update class KDoc removing "second adapter alongside LegacyScheduler"
2. `App.java`: the `if (hiltWorkerFactory != null)` guard is clean, pragmatic test-env support; leave as-is with existing comment

## Build verification

`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` — all must pass.
