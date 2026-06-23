---
type: plan
story: U-048
verdict: PASS
---

# U-048 Implementation Plan: Complete Hilt DI Cutover

## Root Cause (AR-001)

The Hilt migration (U-023) was left half-complete. `EventModule.provideSyncStateRepository()` called `App.syncStateRepository()` (a Java-static singleton) rather than having Hilt own the object lifecycle. `SchedulerModule` and `ContactsModule` similarly used `@Provides`-new methods because `WorkManagerScheduler`, `FlowSyncStateRepository`, and `PeopleApiContactsAdapter` lacked `@Inject` constructors.

The BUG-004 patch (U-036) resolved the dual-instance risk by having `EventModule` delegate to the App-static, but this left the static as the source of truth rather than Hilt.

## Approach

### Step 1 — Add `@Inject` constructors to implementation classes

- `FlowSyncStateRepository`: no-arg `@Inject constructor()` (no dependencies)
- `WorkManagerScheduler`: `@Inject constructor(@ApplicationContext Context, Preferences)` (both already in the Hilt graph)
- `PeopleApiContactsAdapter`: no-arg `@Inject constructor()` (no dependencies)

### Step 2 — Convert DI modules from `object @Provides` to `abstract class @Binds`

`@Binds` requires abstract class; converts the three `object` modules to `abstract class`:
- `EventModule`: `@Binds @Singleton abstract fun bindSyncStateRepository(impl: FlowSyncStateRepository): SyncStateRepository`
- `SchedulerModule`: `@Binds @Singleton abstract fun bindBackupScheduler(impl: WorkManagerScheduler): BackupScheduler`
- `ContactsModule`: `@Binds @Singleton abstract fun bindContactsPort(impl: PeopleApiContactsAdapter): ContactsPort`

### Step 3 — Wire App.java through the Hilt graph

- Add `@Inject SyncStateRepository syncStateRepository;` field (Hilt provides the @Singleton)
- Add `@Inject BackupScheduler scheduler;` (SchedulerModule @Binds)
- Remove `new FlowSyncStateRepository()` in `onCreate()` — replace with `syncStateRepositoryInstance = syncStateRepository;`
- Remove `new WorkManagerScheduler(this, preferences)` — Hilt now owns the instance
- Remove unused imports (`FlowSyncStateRepository`, `WorkManagerScheduler`)
- Static bridge `App.syncStateRepository()` retained (returns `syncStateRepositoryInstance` which is now the Hilt-managed instance) — migration to @Inject for all callers is U-049

### Single-Instance Guarantee (BUG-004 permanent fix)

- Hilt `@Singleton` scope in `SingletonComponent` guarantees exactly one `FlowSyncStateRepository` instance per app process
- `EventModule` no longer calls `App.syncStateRepository()` — the Hilt graph is the sole construction site
- `App.java` receives the same instance via `@Inject SyncStateRepository` and assigns it to the static bridge
- Zero parallel construction paths exist in production code

### Step 4 — Update tests

- Rewrite `EventModuleSingleInstanceTest.kt` to match the new architecture (no longer tests `EventModule.provideSyncStateRepository()`, which no longer exists as a method; tests the static bridge receives the Hilt instance and state emission still works)
