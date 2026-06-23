---
artifact_type: review-code
story_id: "U-048"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-06-23"
blockers: 0
warnings: 2
---

# Code Review: U-048

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PASS — all three modules conform to DES-MODERNIZATION-008 §Module layout; CNTR-MODERNIZATION-006 and CNTR-MODERNIZATION-008 shapes are untouched |
| Test coverage | PASS — 642 @Test annotations on HEAD; EventModuleSingleInstanceTest rewritten with 4 tests covering identity, type, emission, and constructor reflectability |
| Code quality | PASS — clean @Binds abstract class pattern; @Inject constructors are correctly placed; no manual construction in module or App code |

## Verdict: PASS

The implementation correctly completes the Hilt DI cutover prescribed by AR-001. The
@Provides-new pattern is replaced by @Binds abstract in all three modules; @Inject
constructors are added to FlowSyncStateRepository, WorkManagerScheduler, and
PeopleApiContactsAdapter; and App.java receives both SyncStateRepository and
BackupScheduler via Hilt field injection, eliminating all parallel construction sites.
The BUG-004 dual-instance risk is structurally impossible under the new graph. Two
warnings are noted: the static bridge accessor was retained rather than deleted as AC-3
originally stated (explicitly justified as a U-049 prerequisite and structurally safe),
and the implementation log overstates the @Test count by 30. Neither is a blocker.

## Findings

### Blockers

None.

### Warnings

**W-1: AC-3 partial completion — static bridge accessor retained, not deleted**
File: `app/src/main/java/com/zegoggles/smssync/App.java` lines 103, 312

AC-3 states: "`App.syncStateRepository()` static accessor and the `App.java:101,239`
static singleton field are deleted." The implementation retains both. The story itself
notes this was intentional: the static bridge is now assigned FROM the Hilt-injected
field (not from `new FlowSyncStateRepository()`), so the BUG-004 dual-instance root
cause is eliminated. The comment at line 101-102 documents the rationale ("U-049 will
migrate all remaining callers to @Inject and this bridge can then be removed"), and there
are active callers in `MainActivity.java`, `StatusPreference.java`, `Dialogs.java`,
`AdvancedSettings.java`, `MainSettings.java`, `SMSBackupPreferenceFragment.java`,
`PackageReplacedReceiver.java`, `OAuth2WebAuthActivity.java`, `RedirectReceiverActivity.java`,
and `OAuth2CallbackTask.java` — all of which would break if the bridge were removed
before their migration. The deviation from the literal AC-3 text is justified and
controlled; however, the story's acceptance checklist marks AC-3 as [x] when it should
note "bridge retained as per story notes, deletion deferred to U-049." Track as a
warning so the next reviewer knows what U-049 must clean up.

**W-2: Implementation log @Test count overstated (672 vs actual 642)**
File: `sdlc/artifacts/build/sprints/sprint-012/U-048/implementation-log.md` line 106

The implementation log reports 672 @Test annotations. Running the authoritative command
(`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`)
on HEAD returns 642. The discrepancy is 30 tests. This is likely an artifact of a stale
count from a different HEAD state (possibly a sprint-012 merge artefact from U-049 or
U-050 that removed some tests). The count has no bearing on correctness — the build and
test results claimed in the log are what matter — but future reviewers should not rely on
the stated number.

### Observations

**O-1: `new PeopleApiContactsAdapter()` survives in OAuth2Client.java:161 — out of scope
and pre-existing, but worth noting**
File: `app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java` line 161

The single-argument `@Inject` constructor `OAuth2Client(String clientId)` delegates to
`this(clientId, new PeopleApiContactsAdapter())`. This construction site was present
before U-048 and was not touched by this story. AC-1 explicitly limits its scope to
"module files," so this is not an AC violation. However, it means the `ContactsPort`
singleton bound by `ContactsModule` is not the same instance that `OAuth2Client` uses
when Hilt constructs it via the one-arg `@Inject` constructor path — each Hilt-managed
`OAuth2Client` brings its own `PeopleApiContactsAdapter` alongside the singleton bound
for `ContactsPort`. Since `PeopleApiContactsAdapter` is stateless (no mutable fields,
only a network call), this causes no behavioral defect. A future story (AR-004 scope or
U-050 follow-up) could inject `ContactsPort` into `OAuth2Client`'s primary constructor
to use the Hilt-managed singleton uniformly.

**O-2: The old `provideSyncStateRepository()` called twice test is replaced with a
constructor-reflectability test — coverage is adequate but the "twice equals same
instance" guarantee is no longer directly tested**

The prior `EventModuleSingleInstanceTest` had a test that called
`EventModule.provideSyncStateRepository()` twice and asserted same-instance. That
contract is now enforced by Hilt's @Singleton scope (not testable via direct method call
without the Hilt harness), so the replacement test verifies the constructor and bridge
identity instead. The @Singleton guarantee is a Hilt framework invariant, not something
the unit test layer needs to duplicate. The trade-off is sound.

**O-3: `FlowCollectHelper.kt` still carries a `TODO U-022/MU-007` comment**
File: `app/src/main/java/com/zegoggles/smssync/FlowCollectHelper.kt` line 17

`// TODO U-022/MU-007: remove once Hilt injection replaces manual-DI pattern.` — U-048
is the story that completes MU-007. This comment is stale. Not a defect in this story's
scope (it was not listed in Affected Code), but should be cleaned up in U-049 or U-050.

**O-4: `@Singleton` on `@Binds` in abstract class — correct Dagger pattern confirmed**

All three modules (`EventModule`, `SchedulerModule`, `ContactsModule`) correctly use
`abstract class` (required by Dagger when `@Binds` is used), are annotated
`@InstallIn(SingletonComponent::class)`, and each `@Binds` method carries `@Singleton`.
The `@Singleton` annotation on the `@Binds` method is the correct mechanism for scoping
the binding — not on the implementation class — because the scope annotation must match
the component. This is idiomatic Hilt/Dagger.

**O-5: BUG-004 invariant is structurally guaranteed, not just contractually**

The single-instance guarantee (BUG-004 permanent fix) holds by structural analysis:
(a) `EventModule.bindSyncStateRepository()` is `@Singleton` in `SingletonComponent` —
Hilt calls the `@Inject constructor()` of `FlowSyncStateRepository` exactly once per
app process. (b) `App.java` receives this single instance via `@Inject SyncStateRepository
syncStateRepository` and assigns `syncStateRepositoryInstance = syncStateRepository` in
`onCreate()`. (c) `EventModule` no longer calls `App.syncStateRepository()` — the
delegation direction is reversed. There is no second construction path. The `new
FlowSyncStateRepository()` call in `EventModuleSingleInstanceTest.setUp()` is test-only
and isolated by the `@Before`/`@After` reflection teardown.

## Patterns Verified

- [x] Follows existing code patterns — @Binds abstract class with @InstallIn(SingletonComponent) is the established module pattern; @Inject constructors follow existing style (FlowSyncStateRepository, WorkManagerScheduler, PeopleApiContactsAdapter)
- [x] Error handling is appropriate — no error-path changes introduced; FlowSyncStateRepository semantics (tryEmitEvent logging, StateFlow seeding) are preserved verbatim
- [x] Tests cover new functionality — 4 tests in EventModuleSingleInstanceTest cover identity, type checking, state emission round-trip, and constructor reflectability
- [x] No hardcoded values that should be configurable — no new literals introduced; WorkManagerScheduler constants are unchanged
- [x] No unnecessary complexity — @Binds is strictly simpler than the prior @Provides-new pattern; the static bridge is the minimum necessary shim for pre-U-049 callers

## Integration Verified

- [x] New code is reachable from production entry points — `App.java`'s `@Inject SyncStateRepository syncStateRepository` field is populated by Hilt_App (generated), which calls `EventModule.bindSyncStateRepository(FlowSyncStateRepository)`. The FlowSyncStateRepository @Inject constructor is the only construction site. App.syncStateRepository() (line 311) delegates to syncStateRepositoryInstance (line 312), which is set at line 178. All legacy callers reach the Hilt-managed singleton.
- [x] Registries/dispatch maps updated for new implementations — the three modules are registered via `@InstallIn(SingletonComponent::class)` and `@HiltAndroidApp` on App. No additional manifest or registry entry is required for module registration in Hilt.
- [x] Function signatures match at all call sites — WorkManagerScheduler constructor changes from `WorkManagerScheduler(Context, Preferences)` (called by App.java line 247 pre-U-048) to `@Inject constructor(@ApplicationContext context, preferences)`. The old call site in App.java is deleted; Hilt supplies the arguments. No other caller constructed WorkManagerScheduler directly.
- [x] No dead code introduced — all three modules and all three @Inject constructors are reachable; the static bridge is actively called by ~30 production call sites pending U-049 migration.
- [x] Integration path documented in implementation-log.md — implementation log §Integration Path documents the full wiring chain for both SyncStateRepository and BackupScheduler.

## Regression Check (not a replacement/rewrite story)

N/A — this story augments existing files rather than replacing or deleting them. No Capabilities Inventory is required.

## Contract Verification

- [x] All consumers of modified interfaces identified — SyncStateRepository interface is unchanged; `CNTR-MODERNIZATION-006` field shapes (StateFlow, SharedFlow, emitState, emitEvent, tryEmitEvent) are identical. FlowSyncStateRepository behavior (DROP_OLDEST, replay=0, seed BackupState()) is preserved.
- [x] Field names, event names, data shapes agree between producer and consumer — no contract fields changed; Java callers reach `getState()` (Kotlin property accessor), `tryEmitEvent()`, and `emitState()` identically to before.
- [x] No contract mismatches between backend and frontend — `StatusPreference.java` uses `App.syncStateRepository().getState().getValue()` and `App.syncStateRepository().tryEmitEvent(...)`. Both remain valid — the static accessor now returns the Hilt singleton, which is `FlowSyncStateRepository` implementing the same interface. No field or method name changed.
