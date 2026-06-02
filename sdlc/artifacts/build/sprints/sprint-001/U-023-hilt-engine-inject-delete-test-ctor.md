---
type: story
status: planned
sprint: "000001"
artifact_type: user-story
priority: medium
complexity: high
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-008
design_docs:
  - DES-MODERNIZATION-008
integration_contracts: []
dependencies:
  - U-022
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-023
title: Annotate @Inject constructors on eight BackupTask collaborators, remove BackupTask primary-constructor manual new-wiring, inject CalendarSyncer as Provider/Lazy, delete the 8-param test-only BackupTask constructor, and migrate BackupTaskTest to the Hilt harness
pipeline: ''
domain: modernization
requirement_source: authored
---

# U-023: Annotate @Inject Constructors on BackupTask Collaborators, Remove Manual new-Wiring, Delete Test-Only Constructor, and Migrate Tests to Hilt Harness

## Story

As a developer maintaining the SMS Backup+ engine,
I want every collaborator that `BackupTask`'s primary constructor hand-builds with `new` to carry an `@Inject`-annotated constructor, the manual construction body at `BackupTask.java:61-88` to be removed and replaced with injected constructor parameters, the conditional `CalendarSyncer` to be delivered via `Provider<CalendarSyncer>` or `dagger.Lazy<CalendarSyncer>` behind the existing `isCallLogCalendarSyncEnabled()` guard, the test-only 8-parameter `BackupTask` constructor at `:90-106` to be deleted, `RestoreTask` to receive an `@Inject` annotation on its existing single constructor (no manual wiring exists to remove), and `BackupTaskTest` to be migrated from direct constructor injection to the `@HiltAndroidTest` harness with `@BindValue`/`@TestInstallIn` doubles,
so that the construction graph for the backup engine is compile-time verified by Dagger, the duplicate test seam is eliminated, and no production code path resolves an engine collaborator by calling `new` where the Hilt graph can supply it.

## Acceptance Criteria

- [ ] **AC-1 — Eight collaborator classes carry `@Inject`-annotated constructors**

  Given the source files for `BackupItemsFetcher`, `BackupQueryBuilder`, `PersonLookup`, `ContactAccessor`, `MessageConverter`, `CalendarSyncer`, `TokenRefresher`, and `OAuth2Client`,
  when a developer reads each constructor that is already the sole construction path used by `BackupTask`'s primary constructor,
  then each constructor is annotated `@javax.inject.Inject` (or `@jakarta.inject.Inject`, whichever the project standardises on via the Hilt plugin); `./gradlew :app:compileDebugJavaSources` exits with code 0 and the Dagger annotation processor does not emit any `[Dagger/MissingBinding]` error for these eight types; no `new BackupItemsFetcher(...)`, `new BackupQueryBuilder(...)`, `new PersonLookup(...)`, `new ContactAccessor()`, `new MessageConverter(...)`, `new TokenRefresher(...)`, or `new OAuth2Client(...)` call site remains inside `BackupTask.java`.

- [ ] **AC-2 — BackupTask primary-constructor manual new-wiring is removed; collaborators become injected parameters**

  Given the file `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java`,
  when a developer reads the constructor(s),
  then the block at lines 61-88 — which directly `new`s `BackupItemsFetcher`, `BackupQueryBuilder`, `PersonLookup`, `ContactAccessor`, `MessageConverter`, `CalendarSyncer` (conditionally), `TokenRefresher`, and `OAuth2Client` — is gone; the remaining single constructor accepts all collaborators as explicit parameters and is annotated `@Inject`; `./gradlew :app:compileDebugJavaSources` exits with code 0; a `grep -n "new BackupItemsFetcher\|new BackupQueryBuilder\|new PersonLookup\|new ContactAccessor\|new MessageConverter\|new TokenRefresher\|new OAuth2Client"` inside `BackupTask.java` returns zero results.

- [ ] **AC-3 — `CalendarSyncer` is injected as `Provider<CalendarSyncer>` or `dagger.Lazy<CalendarSyncer>`; the `isCallLogCalendarSyncEnabled()` runtime guard is preserved**

  Given the rewritten `BackupTask.java` with a Hilt-injected constructor,
  when a developer reads the field declaration for the calendar syncer and the conditional guard site (previously at line 76-86),
  then the field is typed `Provider<CalendarSyncer>` or `dagger.Lazy<CalendarSyncer>`, not `CalendarSyncer`; the runtime guard `if (preferences.isCallLogCalendarSyncEnabled())` is present and gates the `.get()` call on the provider/lazy; when `isCallLogCalendarSyncEnabled()` returns `false` at runtime, no `CalendarSyncer` instance is constructed (the provider is never called); a unit test confirms this by stubbing the preference to `false`, executing a backup cycle, and asserting that the `CalendarSyncer` mock (replaced via `@BindValue`) is never invoked; the backup-loop null-guard (`calendarSyncer != null`, previously at `BackupTask.java:282`) is replaced by the equivalent provider-get-behind-guard pattern with the same behavioral contract.

- [ ] **AC-4 — The 8-parameter test-only `BackupTask` constructor is deleted**

  Given the file `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` after this story's changes,
  when a developer reads all constructors in the class,
  then there is exactly one constructor and it is `@Inject`-annotated; the 8-parameter constructor `BackupTask(SmsBackupService, BackupItemsFetcher, MessageConverter, CalendarSyncer, AuthPreferences, Preferences, ContactAccessor, TokenRefresher)` that existed at lines 90-106 is absent; `grep -c "BackupTask(" app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` returns `1`; the project still compiles (`./gradlew :app:compileDebugJavaSources` exits 0).

- [ ] **AC-5 — `RestoreTask` receives `@Inject` on its existing single constructor; no manual wiring is removed because none exists**

  Given the file `app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java`,
  when a developer reads the constructor at lines 61-70 (`RestoreTask(SmsRestoreService, MessageConverter, ContentResolver, TokenRefresher)`),
  then it is annotated `@Inject`; no second constructor is added or removed; the body of the constructor is unchanged; `./gradlew :app:compileDebugJavaSources` exits with code 0.

- [ ] **AC-6 — `BackupTaskTest` is migrated from direct constructor injection to the `@HiltAndroidTest` harness; all existing assertions pass unchanged**

  Given the file `app/src/test/java/com/zegoggles/smssync/service/BackupTaskTest.java`,
  when the migration is complete,
  then:
  (a) the class is annotated `@HiltAndroidTest` and carries a `@Rule public HiltAndroidRule hiltRule = new HiltAndroidRule(this)`;
  (b) the explicit `new BackupTask(service, fetcher, converter, syncer, authPreferences, preferences, accessor, tokenRefresher)` call at line 81 is replaced by `@Inject`-injected or `@BindValue`-bound collaborators supplied by the Hilt test component; each Mockito mock that previously appeared in the 8-param constructor call is bound as a `@BindValue`-annotated field or via a `@TestInstallIn` replacement module;
  (c) every `@Test` method body is unchanged — the same assertions, the same `verify(...)` calls, the same stubbed interactions — and all pass when `./gradlew :app:testDebugUnitTest --tests "*BackupTaskTest*"` is executed; the test command exits with code 0 and zero failures.

- [ ] **AC-7 — Full test suite is green on the Hilt harness after this story**

  Given the completed story changes,
  when `./gradlew :app:testDebugUnitTest` is executed from the repository root,
  then the command exits with code 0; zero new test failures are introduced relative to the pre-story baseline; the coverage gate from MU-002 (verified prerequisite) holds on the `service`, `mail`, and `auth` source packages; the Dagger/Hilt annotation processor emits no `[Dagger/MissingBinding]`, `[Dagger/DuplicateBindings]`, or `[Dagger/IncompatiblyScopedBindings]` errors in the build output.

- [ ] **AC-8 — No `new`-wiring of engine collaborators remains in production source after this story**

  Given the full production source tree after this story's changes,
  when the following grep is run from the repository root:
  `grep -rn "new BackupItemsFetcher\|new BackupQueryBuilder\|new PersonLookup\|new ContactAccessor\(\|new MessageConverter\|new CalendarSyncer\|new TokenRefresher\|new OAuth2Client" app/src/main/java/`,
  then zero results are returned; any surviving `new` calls for these types appear only in test source (`app/src/test/`) as explicit mock-setup stubs, not as production construction paths; this satisfies REQ-MODERNIZATION-008 AC-2 and AC-5 jointly.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` | Two constructors: primary (lines 61-88) hand-builds eight collaborators with `new`; secondary (lines 90-106) is an 8-param test-only constructor | Remove primary constructor body and manual `new`-wiring; convert secondary constructor to the single `@Inject`-annotated constructor accepting all collaborators as parameters; change `CalendarSyncer` field type to `Provider<CalendarSyncer>` or `dagger.Lazy<CalendarSyncer>`; delete the former test-only constructor entirely |
| `app/src/main/java/com/zegoggles/smssync/service/BackupItemsFetcher.java` | Constructor exists with no `@Inject` annotation | Add `@Inject` to constructor |
| `app/src/main/java/com/zegoggles/smssync/service/BackupQueryBuilder.java` | Constructor exists with no `@Inject` annotation | Add `@Inject` to constructor |
| `app/src/main/java/com/zegoggles/smssync/mail/PersonLookup.java` | Constructor exists with no `@Inject` annotation | Add `@Inject` to constructor |
| `app/src/main/java/com/zegoggles/smssync/contacts/ContactAccessor.java` | Constructor exists with no `@Inject` annotation | Add `@Inject` to constructor |
| `app/src/main/java/com/zegoggles/smssync/mail/MessageConverter.java` | Constructor exists with no `@Inject` annotation | Add `@Inject` to constructor |
| `app/src/main/java/com/zegoggles/smssync/calendar/CalendarSyncer.java` | Constructor exists with no `@Inject` annotation | Add `@Inject` to constructor |
| `app/src/main/java/com/zegoggles/smssync/auth/TokenRefresher.java` | Constructor exists with no `@Inject` annotation | Add `@Inject` to constructor |
| `app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java` | Constructor exists with no `@Inject` annotation | Add `@Inject` to constructor |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java` | Single 4-param constructor (lines 61-70) with no `@Inject` annotation; no manual `new`-wiring; no test-only secondary constructor | Add `@Inject` to the existing constructor only; no other changes |
| `app/src/test/java/com/zegoggles/smssync/service/BackupTaskTest.java` | Uses the 8-param test-only constructor directly at line 81 to inject Mockito doubles | Replace the direct constructor call with `@HiltAndroidTest` + `HiltAndroidRule`; bind Mockito mocks via `@BindValue` or `@TestInstallIn`; keep all test assertions unchanged |

## Existing Behavior to Preserve

- **Conditional CalendarSyncer construction.** `BackupTask` currently constructs `CalendarSyncer` only when `preferences.isCallLogCalendarSyncEnabled()` returns `true` (lines 76-86); when `false`, the field is `null` and the backup loop skips calendar sync at the `calendarSyncer != null` guard (line 282). Injecting `Provider<CalendarSyncer>` or `dagger.Lazy<CalendarSyncer>` and calling `.get()` only inside the existing preference guard preserves this behavior exactly — no `CalendarSyncer` instance is created when calendar sync is disabled.
- **`OAuth2Client` is constructed from the auth client-id stored in `AuthPreferences`.** Currently built inside `TokenRefresher` construction at line 87 of `BackupTask.java` via `new OAuth2Client(authPreferences.getOAuth2ClientId())`. After injection, `OAuth2Client` and `TokenRefresher` are separate unscoped bindings; the `OAuth2Client` provider reads the client-id from the injected `AuthPreferences`. The runtime value must be the same.
- **Immutable-auth retry path in `BackupTask.java:189-192`.** The retry path constructs a new `BackupImapStore` because auth parameters are immutable. This is engine behavior, not DI; the store must not become a cached singleton. The `MailTransport`/store binding in U-022's modules must use `Provider`-scoped access so the retry path obtains a fresh store. This story must not introduce any change to the retry logic itself.
- **All existing `BackupTaskTest` assertions.** Every `verify(...)`, `assertThat(...)`, and stubbing interaction in the pre-story `BackupTaskTest.java` must survive the harness migration unchanged. Only the construction/injection mechanism changes, not any test logic.
- **`RestoreTask` single-constructor body.** The body of `RestoreTask`'s constructor at lines 61-70 is unchanged by this story. Only the `@Inject` annotation is added.

## Verification Steps

1. **AC-1 (eight collaborators have `@Inject` constructors):** For each of the eight files listed in Affected Code, open the file and confirm `@Inject` appears on the constructor. Run `./gradlew :app:compileDebugJavaSources`; confirm exit code 0. Confirm the Dagger annotation processor output (check `app/build/generated/source/kapt/` or `app/build/generated/ap_generated_sources/`) contains factory classes for each of the eight types (`BackupItemsFetcher_Factory.java`, etc.).

2. **AC-2 (BackupTask manual wiring removed):** Open `BackupTask.java`. Confirm lines 61-88 (primary constructor body) are absent. Confirm the single remaining constructor takes all collaborators as parameters. Run `grep -n "new BackupItemsFetcher\|new BackupQueryBuilder\|new PersonLookup\|new ContactAccessor\|new MessageConverter\|new TokenRefresher\|new OAuth2Client" app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` from the repo root and confirm zero results. Run `./gradlew :app:compileDebugJavaSources` and confirm exit code 0.

3. **AC-3 (`CalendarSyncer` injected as `Provider`/`Lazy`; conditional guard preserved):** Open `BackupTask.java`. Confirm the `calendarSyncer` field is typed `Provider<CalendarSyncer>` or `dagger.Lazy<CalendarSyncer>`, not `CalendarSyncer`. Confirm the `isCallLogCalendarSyncEnabled()` guard is present before any `.get()` call on the provider/lazy. Run `./gradlew :app:testDebugUnitTest --tests "*BackupTaskTest*"` and confirm the test that stubs the preference to `false` passes (zero invocations on the `CalendarSyncer` mock).

4. **AC-4 (8-param test-only constructor deleted):** Open `BackupTask.java`. Confirm only one constructor exists. Run `grep -c "BackupTask(" app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` and confirm the output is `1`. Run `./gradlew :app:compileDebugJavaSources` and confirm exit code 0.

5. **AC-5 (`RestoreTask` gets `@Inject` only):** Open `RestoreTask.java`. Confirm the constructor at lines 61-70 is now annotated `@Inject`. Confirm no second constructor is present. Confirm the constructor body is unchanged. Run `./gradlew :app:compileDebugJavaSources` and confirm exit code 0.

6. **AC-6 (`BackupTaskTest` migrated to Hilt harness; assertions unchanged):** Open `BackupTaskTest.java`. Confirm `@HiltAndroidTest` is present on the class declaration. Confirm `HiltAndroidRule` is declared as a `@Rule`. Confirm the `new BackupTask(service, fetcher, converter, ...)` call at the former line 81 is absent. Confirm each Mockito mock previously passed to the test constructor is bound via `@BindValue` or a `@TestInstallIn` module. Run `./gradlew :app:testDebugUnitTest --tests "*BackupTaskTest*"` and confirm exit code 0 and zero failures.

7. **AC-7 (full suite green):** Run `./gradlew :app:testDebugUnitTest` from the repo root. Confirm exit code 0. Review the test report at `app/build/reports/tests/testDebugUnitTest/index.html` and confirm zero new failures. Confirm the Dagger/Hilt processor emits no `[Dagger/MissingBinding]` errors in the build log.

8. **AC-8 (no surviving manual new-wiring in production):** Run `grep -rn "new BackupItemsFetcher\|new BackupQueryBuilder\|new PersonLookup\|new ContactAccessor(\|new MessageConverter\|new CalendarSyncer\|new TokenRefresher\|new OAuth2Client" app/src/main/java/` from the repo root. Confirm zero results.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java/Kotlin) | Add `@Inject` to eight collaborator constructors; rewrite `BackupTask` to use injected parameters with `Provider<CalendarSyncer>`; delete the 8-param test-only constructor; add `@Inject` to `RestoreTask`'s single constructor; migrate `BackupTaskTest` to `@HiltAndroidTest` + `@BindValue`/`@TestInstallIn` | Developer |

## Technical Notes

**RestoreTask has a single constructor — no dual-constructor pattern to dismantle.** REQ-MODERNIZATION-008 and MU-007 both refer to a "`BackupTask`/`RestoreTask` dual-constructor" pattern. Source verification (verified `RestoreTask.java:61-70` this session) shows this description applies only to `BackupTask`. `RestoreTask` has exactly one constructor — `RestoreTask(SmsRestoreService, MessageConverter, ContentResolver, TokenRefresher)` — which is already the construction form the graph will call. There is no manual `new`-wiring body to remove and no second test-only constructor to delete. `RestoreTask`'s sole change under this story is adding `@Inject` to its existing constructor. This correction is recorded in DES-MODERNIZATION-008 §Context ("A verified correction to the requirement's framing") and must not cause an implementer to look for a non-existent second constructor.

**`Provider<CalendarSyncer>` vs. `dagger.Lazy<CalendarSyncer>` — which to choose.** Both satisfy the behavioral requirement (deferred, non-eager construction). `dagger.Lazy<T>` is constructed at most once per injection point and caches the result; `Provider<T>` constructs a new instance on each `.get()` call (for unscoped bindings). Because `CalendarSyncer` is unscoped (see DES-MODERNIZATION-008 §Component graph scope table), `Provider` delivers a fresh instance each time `.get()` is called. For `BackupTask`, which is itself unscoped (created per work request), there is one `.get()` call per backup run when calendar sync is enabled, so the cache distinction is irrelevant in practice. Prefer `dagger.Lazy<CalendarSyncer>` as it expresses "build at most once per `BackupTask` instance" more precisely and is idiomatic in Dagger when the result is used at most once per construction context.

**Sequence within this story: annotation before wiring removal.** Within a single PR, annotate all eight collaborator constructors with `@Inject` first (AC-1), then convert `BackupTask`'s constructor (AC-2/AC-3), then delete the test-only constructor (AC-4), then migrate the test (AC-6). Dagger will only validate the complete binding graph after `@HiltAndroidApp` is present (from U-022); by the time this story is implemented, U-022 has already introduced `@HiltAndroidApp` and the `SingletonComponent` root. Each incremental compilation step should therefore remain green.

**`@BindValue` vs. `@TestInstallIn` for `BackupTaskTest` doubles.** Use `@BindValue` for any collaborator whose type is directly injectable into `BackupTask` (e.g., `@BindValue @Mock MessageConverter converter`). Use `@TestInstallIn` for module-level replacements (e.g., replacing the `SchedulerModule` binding). For this story's scope, `@BindValue` is sufficient for all eight collaborator mocks because they are unscoped, directly-injectable types. Do not reach for `@TestInstallIn` unless a binding requires replacing a `@Provides` method that constructs from non-injectable primitives.

**`TestListenableWorkerBuilder` is not required for this story.** AC-6 migrates `BackupTaskTest`, which tests the engine logic, not the WorkManager worker wrapper. The `@HiltWorker` worker integration (AC-4 of REQ-MODERNIZATION-008) is U-024's scope. This story only migrates the existing task-level tests to the Hilt harness; worker factory wiring is explicitly out of scope.

**Dagger graph compilation: watch for `[Dagger/MissingBinding]` on `SmsBackupService`.** `BackupTask`'s injected constructor currently receives `SmsBackupService` as its first parameter. `SmsBackupService` is an `android.app.Service` subclass and is not injectable by Hilt directly (Android Services are `@AndroidEntryPoint` entry points, not injected dependencies). The design (DES-MODERNIZATION-005) resolves this by moving the engine into a `@HiltWorker` `CoroutineWorker` (which receives `@Assisted Context` + `@Assisted WorkerParameters`, not a `Service` reference). If U-015 (CoroutineWorker rewrite) has already landed before this story, `SmsBackupService` as a constructor parameter is already gone. If not, the `SmsBackupService` field must be supplied via `@Assisted` or the binding must be temporarily satisfied by a `@Provides` method that returns the service from `SmsBackupService.onCreate()` — the exact coexistence approach is at the implementer's discretion, consistent with DES-MODERNIZATION-008 §Incremental coexistence. Record the chosen approach in Implementation Notes before marking this story done.

**Dependency on U-022.** U-022 must be merged and CI-green before this story begins. U-022 introduces `@HiltAndroidApp` on `App`, the `SingletonComponent` root, and the `@InstallIn` modules. Without those, Dagger has no component graph to validate the `@Inject` constructors against, and the `@HiltAndroidTest` harness is unavailable. The `depends_on: U-022` frontmatter encodes this as a hard sequencing constraint.

**Do not touch `App.java` static Bus surface.** By the time this story is implemented, U-019/U-020 (the Otto-to-Flow swap, MU-006) has already removed the `static Bus bus` from `App.java` (lines 59, 116-134). This story must not re-introduce any reference to `App.register`, `App.unregister`, or `App.post` — those are gone. If this story is implemented before MU-006 lands (non-standard sequencing), the static Bus calls in `BackupTask.java:110` and `RestoreTask.java:74` are MU-006's scope, not this story's.

**`CalendarSyncer` null-guard migration.** The existing null-check `cursor.type == CALLLOG && calendarSyncer != null` at `BackupTask.java:282` guards calendar sync execution. After this story, `calendarSyncer` is no longer a nullable field — it is a `Provider<CalendarSyncer>`. The guard becomes `cursor.type == CALLLOG && preferences.isCallLogCalendarSyncEnabled()`, which matches the condition under which the provider is resolved. The behavioral contract is identical: calendar sync runs if and only if the preference is enabled and the data type is CALLLOG.

**Estimation rationale (high complexity).** This story touches nine production Java files (one rewrite, eight annotation additions) plus one test class with a migration from direct construction to Hilt harness injection. The `Provider<CalendarSyncer>` substitution requires understanding the existing null-guard semantics at `BackupTask.java:282` and the conditional construction at lines 76-86. The test migration requires configuring `@HiltAndroidTest` infrastructure and verifying that eight Mockito doubles are correctly re-bound via `@BindValue`. The interaction with the potential `SmsBackupService` non-injectable type (Technical Notes above) adds an ambiguity the implementer must resolve before committing. Collectively this justifies a high complexity rating.

## Supporting Documentation

- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-008-introduce-hilt-dependency-injection.md` — governing requirement; AC-2 (manual wiring removed), AC-3 (test-only constructors deleted), AC-6 (full suite green on Hilt harness)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-008-hilt-di-design.md` — §BackupTask/RestoreTask constructor injection, §Delete the test-only secondary constructor, §Tests move to the Hilt test harness, §Behavior-preservation guarantees (CalendarSyncer conditional, OAuth retry path), §Verified correction to the requirement's framing (RestoreTask single-constructor)
- `sdlc/artifacts/stories/U-022-hilt-bootstrap-singleton-modules.md` — hard predecessor; provides `@HiltAndroidApp`, `SingletonComponent` root, and the module installation that makes AC-1's bindings resolvable

## Integration Contract References

This story consumes bindings introduced by U-022. It produces no new cross-component contract. The `@Inject`-annotated constructors on the eight collaborators are their own binding declarations; no CNTR artifact is required.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Not finalized. Key load-bearing implementation decisions are: (1) choice of `dagger.Lazy<CalendarSyncer>` vs. `Provider<CalendarSyncer>` (Technical Notes recommends `Lazy`); (2) resolution of the `SmsBackupService` non-injectable type if U-015 has not yet landed (Technical Notes describes the coexistence options); (3) `@BindValue` is the recommended test-double mechanism for all eight collaborator mocks in `BackupTaskTest`. The RestoreTask single-constructor correction (no dual-constructor pattern exists) is verified from source and recorded in AC-5 and Technical Notes.
