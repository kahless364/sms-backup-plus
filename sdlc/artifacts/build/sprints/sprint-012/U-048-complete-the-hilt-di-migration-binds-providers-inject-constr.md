---
type: story
status: done
artifact_type: user-story
priority: high
complexity: medium
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
id: U-048
title: 'Complete the Hilt DI migration: @Binds providers, @Inject constructors, remove static SyncStateRepository alias'
pipeline: ''
domain: modernization
resolution: done
requirement_source: assessment:20260623-post-migration-assessment#AR-001
sprint: '000012'
---

# U-048: Complete the Hilt DI migration: @Binds providers, @Inject constructors, remove static SyncStateRepository alias

## Story
As a maintainer of the SMS Backup+ codebase, I want the Hilt dependency graph to be the single source of truth for all singleton construction, so that the static `App.syncStateRepository()` alias and its BUG-004 dual-instance risk are permanently eliminated and every injectable type is verifiable by Hilt's compile-time graph validation.

## Source
Derived from assessment 20260623-post-migration-assessment, finding AR-001. See `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/architecture.md`.

## Acceptance Criteria
- [ ] AC-1: `SchedulerModule`, `EventModule`, and `ContactsModule` replace their `@Provides`-new methods with `@Binds` (or `@Provides` returning the `@Inject`-constructed impl) and the `TODO(U-023)` annotations are removed; no `new WorkManagerScheduler(...)`, `new FlowSyncStateRepository()`, or `new PeopleApiContactsAdapter(...)` calls remain in module files.
- [ ] AC-2: `WorkManagerScheduler`, `FlowSyncStateRepository`, and `PeopleApiContactsAdapter` each declare an `@Inject` constructor; the DI graph resolves them without any manual construction outside Hilt-managed code.
- [ ] AC-3: `App.syncStateRepository()` static accessor and the `App.java:101,239` static singleton field are deleted; all callers (workers, services, or their injected replacements) receive `SyncStateRepository` via Hilt injection; a unit test asserts that `App.syncStateRepository()` no longer exists OR that the single Hilt-managed `@Singleton` is what all callers hold.
- [ ] AC-4: `EventModule.provideSyncStateRepository()` no longer calls `App.syncStateRepository()` — it returns the Hilt `@Singleton` directly; the BUG-004 dual-instance scenario is structurally impossible (one `@Singleton` binding, zero parallel construction sites).
- [ ] AC-5: Build green: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` passes with per-package LINE ≥ 70%.

## Affected Code
| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/di/SchedulerModule.kt:32` | `@Provides` manually constructs `WorkManagerScheduler` (tagged `TODO(U-023)`) | Replace with `@Binds` or convert to `@Inject` constructor on `WorkManagerScheduler` |
| `app/src/main/java/com/zegoggles/smssync/di/EventModule.kt:41` | `@Provides` manually constructs `FlowSyncStateRepository`; calls `App.syncStateRepository()` for the alias | Replace with `@Binds` on the `@Inject`-constructor impl; remove `App.syncStateRepository()` call |
| `app/src/main/java/com/zegoggles/smssync/di/ContactsModule.kt:32` | `@Provides` manually constructs `PeopleApiContactsAdapter` (tagged `TODO(U-023)`) | Replace with `@Binds` or `@Inject` constructor |
| `app/src/main/java/com/zegoggles/smssync/App.java:101,239` | Declares and populates static `syncStateRepositoryInstance`; exposes `syncStateRepository()` accessor used by engine callers | Delete static field and accessor; route all callers through injected `SyncStateRepository` |
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` (any `App.syncStateRepository()` call sites) | Calls static accessor to emit engine state | Replace with injected `SyncStateRepository` (enabled once U-049 injects workers) |

## Existing Behavior to Preserve
- Single-instance `SyncStateRepository` (`@Singleton`) guarantee — this is the core BUG-004 invariant. One Hilt `@Singleton` binding must replace the static singleton; never two construction sites.
- `FlowSyncStateRepository` StateFlow + SharedFlow(replay=0, DROP_OLDEST) collection semantics and the dropped-event logging (`FlowSyncStateRepository.kt:68-74`) must not change.
- All non-Hilt callers that currently use `App.syncStateRepository()` (workers, services) must keep receiving the same single instance — via injection or via an `App`-level `@Inject` field assigned from Hilt during `onCreate`.
- `@HiltViewModel MainViewModel` injection of `SyncStateRepository` continues to work.

## Verification Steps
1. Run `./gradlew :app:assembleDebug` — confirm no Hilt graph-validation errors (Hilt's compile-time validation will catch unresolved bindings immediately).
2. Search the compiled sources for `new FlowSyncStateRepository()`, `new WorkManagerScheduler(`, `new PeopleApiContactsAdapter(` — expect zero occurrences outside test files.
3. Search for `App.syncStateRepository()` — expect zero occurrences in production sources.
4. Run `./gradlew :app:testDebugUnitTest` — the existing `di/` test package and any new `SingletonRepositoryTest` must pass; confirm the same object identity holds between the App-accessible instance and the Hilt-injected instance.
5. Run `./gradlew :app:jacocoTestCoverageVerification` — all gated packages must remain at or above LINE 70%.

## Technical Context
- AR-001 root cause: the Hilt migration (U-023) was left half-complete; `EventModule.provideSyncStateRepository()` calls `App.syncStateRepository()` (a Java-static singleton) rather than having Hilt own the object lifecycle. The BUG-004 fix added the alias as a bridge; this story removes the bridge by making Hilt the only construction site.
- Recommended approach: add `@Inject` constructors to `WorkManagerScheduler`, `FlowSyncStateRepository`, `PeopleApiContactsAdapter`; convert `@Provides`-new methods to `@Binds abstract` (or single-line `@Provides` methods forwarding from the `@Inject` impl); in `App.java` add `@Inject SyncStateRepository syncStateRepository;` and set the static accessor from it in `inject(this)` callback, then delete the standalone construction.
- This story unlocks U-049 (service retirement) because service dispatch currently relies on `App.syncStateRepository()` — once that is gone the services have no static anchor.

## Notes
- Sprint A (DI & Legacy Retirement). Must land before U-049; U-049 depends on the unified DI graph established here.
- The `TODO(U-023)` tags in `SchedulerModule`, `EventModule`, `ContactsModule` are the authoritative list of what needs converting.
