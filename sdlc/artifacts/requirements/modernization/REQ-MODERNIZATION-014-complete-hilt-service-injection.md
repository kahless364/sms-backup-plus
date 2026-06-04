---
status: approved
type: nfr
artifact_type: requirement
priority: medium
id: REQ-MODERNIZATION-014
title: Complete Hilt service injection (@AndroidEntryPoint, remove coexistence shims)
domain: modernization
epic: EPIC-MODERNIZATION-005
related_requirements:
  - REQ-MODERNIZATION-008
  - REQ-MODERNIZATION-013
related_stories: []
related_design_docs: []
traces_to:
  - REQ-MODERNIZATION-008
change_records: []
notes: |
  Dependency ordering: REQ-MODERNIZATION-013 determines which service classes remain.
  If REQ-013 deletes both SmsBackupService and SmsRestoreService, the scope of this
  requirement narrows to shim removal in ServiceBase only (AC-1 and AC-2 are vacuously
  satisfied; AC-3 through AC-6 still apply).
  Robolectric blocker documented in U-022 implementation-log (AC-4 deviation) and
  U-023 implementation-log (AC-10 / coexistence pattern).
---

# REQ-MODERNIZATION-014: Complete Hilt service injection (@AndroidEntryPoint, remove coexistence shims)

## Description

Apply `@AndroidEntryPoint` to every concrete service class that survives
REQ-MODERNIZATION-013, convert its dependencies to Hilt field injection, and remove all
manual-construction coexistence shims introduced during the U-022/U-023 bootstrap phase:
the null-check fallbacks in `ServiceBase.getPreferences()` /
`ServiceBase.getAuthPreferences()`, and the `getBackupTask()` /
`getRestoreTask()` factory methods in `SmsBackupService` / `SmsRestoreService`.

## Context

Three successive stories deferred the final Hilt wiring for `SmsBackupService` and
`SmsRestoreService`. U-022 introduced `@Inject` fields on `ServiceBase` but left
null-check fallbacks in `getPreferences()` and `getAuthPreferences()` because applying
`@AndroidEntryPoint` to a concrete service breaks Robolectric tests that instantiate
anonymous service subclasses directly — those subclasses call `service.onCreate()` without
a Hilt component bound to the application, producing an NPE inside
`Hilt_SmsBackupService.onCreate()`. U-023 added `getBackupTask()` and `getRestoreTask()`
factory methods that manually construct all task collaborators using fully-qualified class
names as a second coexistence shim. U-024 completed `@HiltWorker` injection for the
WorkManager workers but explicitly left the legacy services pending.

The result is a two-mechanism construction system: Hilt owns workers and the
`App`-level singletons, while the services still build their task graphs by hand.
This divergence creates two risks: a dependency silently constructed by hand in
`getBackupTask()` / `getRestoreTask()` will not benefit from the Hilt-managed
singleton scope and will not be wired if a new collaborator is added to the
`@Module` graph without also updating the factory methods. Completing the migration
eliminates both risks and satisfies REQ-MODERNIZATION-008 AC-5 (no `App`-level
Service-Locator lookups remain) and AC-6 (full test suite passes on the Hilt test
harness).

## Requirements

### Apply @AndroidEntryPoint to surviving service classes

Every service class that exists after REQ-MODERNIZATION-013 executes must carry
`@AndroidEntryPoint`. The annotation causes the Hilt-generated base class
(`Hilt_SmsBackupService`, `Hilt_SmsRestoreService`) to be inserted into the inheritance
chain, which fires member injection on `ServiceBase`'s `@Inject` fields
(`injectedPreferences`, `injectedAuthPreferences`) before `onCreate()` runs.

If REQ-MODERNIZATION-013 deletes a service class entirely, this sub-requirement is
vacuously satisfied for that class — there is nothing to annotate.

#### Acceptance Criteria

- [ ] AC-1: After REQ-MODERNIZATION-013 has executed, every `*.java` or `*.kt` file in
  `app/src/main/java/com/zegoggles/smssync/service/` whose class directly extends
  `android.app.Service` (or `ServiceBase`) carries `@dagger.hilt.android.AndroidEntryPoint`.
  Verified by: `grep -rn "extends ServiceBase\|extends Service" app/src/main/java/` lists
  only classes that also have `@AndroidEntryPoint` in the same file.

- [ ] AC-2: `./gradlew :app:kaptDebugKotlin` (or `kaptDebugJava`) completes with zero
  Dagger processor errors (`[Dagger/MissingBinding]`, `[Dagger/DuplicateBindings]`,
  `[Dagger/IncompatiblyScopedBindings]`), confirming that the Hilt component graph
  resolves all injection points declared on the annotated service(s).

### Remove ServiceBase null-check coexistence fallbacks

`ServiceBase.getPreferences()` and `ServiceBase.getAuthPreferences()` currently guard
the injected fields with a null check and fall back to `new Preferences(...)` /
`new AuthPreferences(...)` when the fields are null (i.e., when Hilt has not fired
because `@AndroidEntryPoint` is absent). Once `@AndroidEntryPoint` is on every surviving
concrete service, the null branch is unreachable dead code and must be removed. The
methods become direct field returns: `return injectedPreferences;` and
`return injectedAuthPreferences;`.

The `injectedPreferences` and `injectedAuthPreferences` field names may simultaneously
be renamed to `preferences` and `authPreferences` respectively, since the
field-shadowing hazard that motivated the `injected`-prefix workaround (documented in
U-022 implementation-log §AC-4 Field Naming) no longer applies once the
anonymous-subclass Robolectric tests are migrated (see AC-5 below).

#### Acceptance Criteria

- [ ] AC-3: `grep -n "injectedPreferences != null\|injectedAuthPreferences != null" app/src/main/java/`
  returns zero matches. The null-check conditional branches are removed from
  `ServiceBase.getPreferences()` and `ServiceBase.getAuthPreferences()`.

- [ ] AC-4: `grep -n "new Preferences(getApplicationContext\|new AuthPreferences(this)" app/src/main/java/com/zegoggles/smssync/service/`
  returns zero matches. No on-demand manual construction of `Preferences` or
  `AuthPreferences` remains anywhere in the `service/` package.

### Remove getBackupTask() and getRestoreTask() factory methods

`SmsBackupService.getBackupTask()` and `SmsRestoreService.getRestoreTask()` were
introduced in U-023 as coexistence shims: they manually construct `BackupTask` and
`RestoreTask` with all collaborators using fully-qualified class names. Once
`@AndroidEntryPoint` is applied to the services and `BackupTask` / `RestoreTask`
are obtained from the Hilt component graph (or, if REQ-MODERNIZATION-013 deletes the
services, these methods are deleted with the service classes), the factory methods must
be removed.

If REQ-MODERNIZATION-013 deletes `SmsBackupService` or `SmsRestoreService`, the
corresponding factory method is deleted with the class; this sub-requirement is
vacuously satisfied for that class.

#### Acceptance Criteria

- [ ] AC-5: `grep -n "getBackupTask\|getRestoreTask" app/src/main/java/` returns zero
  matches in production source. The factory methods are removed (or removed with their
  containing class). Verified after the REQ-013 deletion step completes.

- [ ] AC-6: `grep -rn "new com.zegoggles.smssync.service.BackupItemsFetcher\|new com.zegoggles.smssync.service.BackupQueryBuilder\|new com.zegoggles.smssync.mail.PersonLookup\|new com.zegoggles.smssync.contacts.ContactAccessor\|new com.zegoggles.smssync.mail.MessageConverter\|new com.zegoggles.smssync.auth.OAuth2Client\|new com.zegoggles.smssync.auth.TokenRefresher\|new com.zegoggles.smssync.service.CalendarSyncer" app/src/main/java/com/zegoggles/smssync/service/`
  returns zero matches. The fully-qualified manual-construction sites that comprised
  the coexistence factory methods are fully removed from the `service/` package.

### Migrate Robolectric anonymous-subclass tests to a Hilt-compatible pattern

The blocking constraint for all prior deferrals is the Robolectric test pattern in
`SmsBackupServiceTest`: the `@Before` method creates an anonymous subclass of
`SmsBackupService` that overrides `getPreferences()`, `getAuthPreferences()`,
`getBackupTask()`, and `getScheduler()` to return mocks. This pattern is incompatible
with `@AndroidEntryPoint` because the Hilt-generated superclass
`Hilt_SmsBackupService` calls `inject(this)` in `attachBaseContext()`, which requires
the `Application` to be a `HiltTestApplication`. Under standard Robolectric, the
application is `RuntimeEnvironment.application` (a plain `Application`), so
`inject(this)` throws `IllegalStateException`.

Two approaches are acceptable; the implementing story must choose exactly one and
state the choice explicitly in its implementation log:

**Option A — Constructor-injection refactor** (preferred, lower ceremony): Refactor
`SmsBackupService` and `SmsRestoreService` to obtain their dependencies (preferences,
authPreferences, backupTask/restoreTask, scheduler) via constructor parameters rather
than service-lifecycle injection. The `Service` is still registered in
`AndroidManifest.xml`, but its collaborators are supplied at construction time (or via
a factory), making tests constructable without Hilt lifecycle. This approach avoids
`HiltAndroidRule`, custom test runners, and `HiltTestApplication` entirely.

**Option B — @HiltAndroidTest harness** (higher ceremony, full Hilt test path):
Configure a custom Robolectric test runner with `HiltTestApplication` as the
application class (via `@Config(application = HiltTestApplication.class)` on each
test class or via `robolectric.properties`). Annotate service tests with
`@HiltAndroidTest`, declare a `@HiltAndroidRule` in `@Before`, and inject doubles
via `@BindValue` / `@UninstallModules`. Replace anonymous-subclass mock overrides with
Hilt `@BindValue` fields.

The implementing story must NOT defer the anonymous-subclass problem again. A test that
creates an anonymous subclass of a `@AndroidEntryPoint`-annotated service without Hilt
lifecycle will fail at runtime; the AC below treats a green test suite as the
verification gate.

#### Acceptance Criteria

- [ ] AC-7: The implementing story's implementation log explicitly names the chosen
  approach (Option A or Option B) and includes a brief rationale. This is a
  documentation AC verified during code review, not by a grep or build command.

- [ ] AC-8: `app/src/test/java/com/zegoggles/smssync/service/SmsBackupServiceTest.java`
  and `SmsRestoreServiceTest.java` contain no anonymous subclass instantiation of
  `SmsBackupService` or `SmsRestoreService` (i.e., `new SmsBackupService() {` and
  `new SmsRestoreService() {` do not appear in test source).
  Verified by: `grep -n "new SmsBackupService() {" app/src/test/` and
  `grep -n "new SmsRestoreService() {" app/src/test/` both return zero matches.

### Full test suite and coverage gate

#### Acceptance Criteria

- [ ] AC-9: `./gradlew :app:testDebugUnitTest` completes with zero test failures. All
  tests that existed before this story began continue to pass; no test that previously
  tested service behavior is removed without a replacement that covers the same
  behavior.

- [ ] AC-10: `./gradlew :app:jacocoTestCoverageVerification` completes with
  `BUILD SUCCESSFUL`. The JaCoCo instruction coverage for the `:app` module remains at
  or above 70%.

### On-device smoke: backup and restore function after injection is live

#### Acceptance Criteria

- [ ] AC-11: After installing a debug APK built from this story's branch on a physical
  device or emulator (API 26+), a manual backup run initiated from
  `MainActivity` completes without a `NullPointerException`, `IllegalStateException
  "Hilt component not initialized"`, or `IllegalArgumentException` in logcat tagged
  `SmsSyncPlus`. Verified by reading logcat output at `adb logcat -s SmsSyncPlus`
  during a backup run.

- [ ] AC-12: A manual restore run initiated from `MainActivity` completes without any
  Hilt DI initialization error in logcat. Verified by the same logcat method as AC-11.

## Rationale

REQ-MODERNIZATION-008 AC-5 requires that no `App`-level Service-Locator lookups remain
and the dependency graph is compile-time verified. The coexistence shims introduced in
U-022 and U-023 are deliberate, bounded deferrals of that requirement — they exist
precisely because `@AndroidEntryPoint` could not be applied without breaking the
Robolectric test suite. This requirement closes that deferral: it mandates resolving
the Robolectric blocker (by choosing and executing a migration strategy), applying
`@AndroidEntryPoint`, and removing every manual-construction fallback. Once complete,
object construction has a single source of truth — the Hilt component graph — and
adding or changing a collaborator requires only a module binding change, not a
coordinated edit of both a module and a factory method.

## Constraints

- **Behavior preserved**: All backup and restore behaviors observable by the user must
  be identical before and after this change. No data is lost; no scheduled backup is
  skipped; no restore behavior changes.
- **Depends on REQ-MODERNIZATION-013**: The set of target service classes is whatever
  REQ-013 leaves in place. This requirement must be executed after REQ-013 completes, or
  in a coordinated sprint where the REQ-013 deletion and the `@AndroidEntryPoint`
  application are sequenced such that the build is green after each step. If REQ-013
  deletes all legacy services, the scope of this requirement reduces to: remove the
  `ServiceBase` null-check fallbacks (AC-3, AC-4) and verify no residual
  manual-construction sites remain (AC-6), then satisfy the test suite and coverage
  gates (AC-9, AC-10). AC-1, AC-2, AC-5, AC-7, AC-8, AC-11, and AC-12 are vacuously
  satisfied if no service class survives REQ-013.
- **Robolectric anonymous-subclass pattern must be resolved, not deferred again**: The
  implementing story may not add a new TODO deferral for the anonymous-subclass issue.
  AC-7 and AC-8 are the enforcement gate.
- **Hilt version**: Hilt 2.51.1 with kapt (established in U-022). Do not upgrade Hilt
  or switch to KSP as part of this story.

## Verification Method

1. Static analysis: run all `grep` commands specified in AC-3, AC-4, AC-5, AC-6, and AC-8.
   All must return zero matches.
2. Compiler gate: `./gradlew :app:kaptDebugKotlin` (AC-2) — zero Dagger processor errors.
3. Unit test gate: `./gradlew :app:testDebugUnitTest` (AC-9) — zero failures.
4. Coverage gate: `./gradlew :app:jacocoTestCoverageVerification` (AC-10) — BUILD SUCCESSFUL.
5. Code review: reviewer confirms the chosen migration approach is documented in the
   implementation log (AC-7).
6. On-device: smoke test backup and restore flows on an emulator (API 26+) as described
   in AC-11 and AC-12.

## Notes

The Robolectric anonymous-subclass blocker is the central technical risk for this
requirement. The choice between Option A (constructor-injection refactor) and Option B
(`@HiltAndroidTest` harness) has different downstream implications:

- Option A keeps the services structurally similar to the current code but makes them
  constructable without Android lifecycle, which aligns with the long-term direction of
  moving execution to `@HiltWorker` CoroutineWorkers (see REQ-MODERNIZATION-008 notes).
  It is the lower-ceremony approach and was successfully applied to `BackupTask` in
  U-023 (the `dagger.Lazy<T>` lambda pattern).

- Option B fully exercises the Hilt test harness and proves that the production injection
  path works end-to-end in tests, but requires `HiltTestApplication`, a custom Robolectric
  runner, and `@BindValue` declarations for every mock. It is the higher-confidence
  approach if the services are expected to remain long-lived.

The implementing story's author must weigh test confidence against added ceremony and
must not leave the decision implicit.

`SmsRestoreServiceTest` uses `Robolectric.setupService(SmsRestoreService.class)` rather
than the anonymous-subclass pattern, so it may require a different migration path from
`SmsBackupServiceTest`. The implementing story must address both test classes explicitly.
