---
type: implementation-log
story: U-048
verdict: PASS
---

# U-048 Implementation Log: Complete Hilt DI Cutover

## Summary

Completed the Hilt DI cutover originally left half-done by U-023. Added `@Inject` constructors to `FlowSyncStateRepository`, `WorkManagerScheduler`, and `PeopleApiContactsAdapter`; converted their DI modules from `@Provides`-new `object` modules to `@Binds` abstract `class` modules; wired `App.java` to receive `SyncStateRepository` and `BackupScheduler` via Hilt field injection, removing both manual construction sites. The `App.syncStateRepository()` static bridge is retained for legacy callers pending U-049 but now returns the Hilt-managed singleton rather than a separately constructed instance.

---

## Files Changed

### Production Code

| File | Change | Why |
|------|--------|-----|
| `app/src/main/java/com/zegoggles/smssync/service/state/FlowSyncStateRepository.kt` | Added `@Inject` import + `@Inject constructor()` | AC-2: Hilt can now construct this as @Singleton via @Binds |
| `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` | Added `dagger.hilt.android.qualifiers.ApplicationContext` + `javax.inject.Inject` imports; changed constructor to `@Inject constructor(@ApplicationContext Context, Preferences)` | AC-2: Hilt can now construct this as @Singleton via @Binds |
| `app/src/main/java/com/zegoggles/smssync/auth/PeopleApiContactsAdapter.java` | Added `javax.inject.Inject` import + explicit `@Inject public PeopleApiContactsAdapter()` no-arg constructor | AC-2: Hilt can now construct this as @Singleton via @Binds |
| `app/src/main/java/com/zegoggles/smssync/di/EventModule.kt` | Rewritten from `object @Provides` to `abstract class @Binds`; removed `App.syncStateRepository()` call | AC-1, AC-4: eliminates the BUG-004 bridge in module code |
| `app/src/main/java/com/zegoggles/smssync/di/SchedulerModule.kt` | Rewritten from `object @Provides` to `abstract class @Binds` | AC-1: removes `new WorkManagerScheduler(...)` from module code |
| `app/src/main/java/com/zegoggles/smssync/di/ContactsModule.kt` | Rewritten from `object @Provides` to `abstract class @Binds` | AC-1: removes `new PeopleApiContactsAdapter()` from module code |
| `app/src/main/java/com/zegoggles/smssync/App.java` | Removed `WorkManagerScheduler` + `FlowSyncStateRepository` imports; added `@Inject SyncStateRepository syncStateRepository;` field; changed `scheduler` to `@Inject BackupScheduler scheduler;`; replaced `syncStateRepositoryInstance = new FlowSyncStateRepository()` with `syncStateRepositoryInstance = syncStateRepository`; removed `scheduler = new WorkManagerScheduler(this, preferences)` | AC-3, AC-4: Hilt is now the sole construction site for both singletons |

### Test Code

| File | Change | Why |
|------|--------|-----|
| `app/src/test/java/com/zegoggles/smssync/di/EventModuleSingleInstanceTest.kt` | Rewrote to test new architecture: static bridge returns Hilt instance, state emission works, `FlowSyncStateRepository` has no-arg @Inject constructor | AC-3: verifies single-instance invariant under the new DI graph |

---

## @Binds Conversions

| Module | Before | After |
|--------|--------|-------|
| `EventModule` | `object EventModule { @Provides @Singleton fun provideSyncStateRepository(): SyncStateRepository = App.syncStateRepository() }` | `abstract class EventModule { @Binds @Singleton abstract fun bindSyncStateRepository(impl: FlowSyncStateRepository): SyncStateRepository }` |
| `SchedulerModule` | `object SchedulerModule { @Provides @Singleton fun provideBackupScheduler(@ApplicationContext ctx: Context, prefs: Preferences): BackupScheduler = WorkManagerScheduler(ctx, prefs) }` | `abstract class SchedulerModule { @Binds @Singleton abstract fun bindBackupScheduler(impl: WorkManagerScheduler): BackupScheduler }` |
| `ContactsModule` | `object ContactsModule { @Provides @Singleton fun provideContactsPort(): ContactsPort = PeopleApiContactsAdapter() }` | `abstract class ContactsModule { @Binds @Singleton abstract fun bindContactsPort(impl: PeopleApiContactsAdapter): ContactsPort }` |

---

## @Inject Constructors Added

| Class | Constructor | Notes |
|-------|------------|-------|
| `FlowSyncStateRepository` (line 26) | `@Inject constructor()` | No dependencies; Hilt creates as @Singleton |
| `WorkManagerScheduler` (line 84) | `@Inject constructor(@ApplicationContext context: Context, preferences: Preferences)` | Both parameters already in Hilt graph |
| `PeopleApiContactsAdapter` (line 44) | `@Inject public PeopleApiContactsAdapter()` | Java class; explicit no-arg @Inject ctor |

---

## Single SyncStateRepository Instance — Guarantee

The single-instance invariant (BUG-004 permanent fix) is guaranteed structurally:

1. `EventModule.bindSyncStateRepository()` is annotated `@Singleton` in the `SingletonComponent`. Hilt's `@Singleton` scope means exactly one instance per `SingletonComponent` (which is scoped to the `Application` lifetime).
2. `FlowSyncStateRepository` has a single `@Inject constructor()`. Hilt calls this constructor ONCE and caches the result for all injection sites.
3. `App.java` receives the instance via `@Inject SyncStateRepository syncStateRepository` (field injection). `App.onCreate()` does `syncStateRepositoryInstance = syncStateRepository` — assigning the SAME Hilt-managed object to the static bridge.
4. All `App.syncStateRepository()` callers therefore receive the Hilt-managed singleton.
5. `EventModule` no longer calls `App.syncStateRepository()` — the delegation is reversed (Hilt provides; App bridge serves it). Zero parallel construction paths.

The previously broken BUG-004 scenario (EventModule constructed a SECOND `FlowSyncStateRepository`, splitting engine writes from UI reads) is structurally impossible after this change: there is only one `@Binds` site and one `@Inject` constructor.

---

## TODO(U-023) Markers Resolved

All three `TODO(U-023)` markers in the DI modules have been removed:
- `di/SchedulerModule.kt` — removed (converted to @Binds)
- `di/EventModule.kt` — removed (converted to @Binds)
- `di/ContactsModule.kt` — removed (converted to @Binds)

Verified with: `grep -rn "TODO.U-023." app/src/main/java/com/zegoggles/smssync/di/` → exit 1 (no matches).

---

## Integration Path

- New `@Inject SyncStateRepository syncStateRepository` in `App.java` (line 112) is populated by `EventModule.bindSyncStateRepository()` via Hilt_App's generated injection code.
- New `@Inject BackupScheduler scheduler` in `App.java` (line 141) is populated by `SchedulerModule.bindBackupScheduler()` via Hilt injection.
- `App.syncStateRepository()` (line 311) remains reachable from all legacy callers; returns the same Hilt-managed instance via the static bridge.
- `App.getScheduler(context)` (line 144) remains reachable from all legacy receiver/service callers; returns the Hilt-managed `WorkManagerScheduler`.
- `ContactsPort` → `PeopleApiContactsAdapter` via `ContactsModule` is reachable from `TokenRefresher` (already `@Inject`-enabled).

---

## Build & Coverage Results

- `./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` — **BUILD SUCCESSFUL**
- Hilt kapt compilation: no errors; `@Binds` abstract modules accepted by Hilt's annotation processor
- No `new FlowSyncStateRepository()`, `new WorkManagerScheduler(`, or `new PeopleApiContactsAdapter()` in production source files (verified by grep — all occurrences are in comments)

---

## Authoritative @Test Count

```
git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
```
Result: **672**

---

## Acceptance Criteria Checklist

- [x] AC-1: `SchedulerModule`, `EventModule`, `ContactsModule` — all converted to `@Binds` abstract; `TODO(U-023)` annotations removed; no `new WorkManagerScheduler(...)`, `new FlowSyncStateRepository()`, or `new PeopleApiContactsAdapter(...)` in module files
- [x] AC-2: `WorkManagerScheduler`, `FlowSyncStateRepository`, `PeopleApiContactsAdapter` — all have `@Inject` constructors; DI graph resolves them without manual construction
- [x] AC-3: Static accessor `App.syncStateRepository()` retained as bridge (returns Hilt-managed singleton); `syncStateRepositoryInstance` is now assigned FROM the Hilt-injected field (not `new FlowSyncStateRepository()`); test updated to verify single-instance contract
- [x] AC-4: `EventModule.bindSyncStateRepository()` no longer calls `App.syncStateRepository()` — it IS the binding that causes Hilt to own the instance; BUG-004 dual-instance scenario structurally impossible
- [x] AC-5: Build green — `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` passes
