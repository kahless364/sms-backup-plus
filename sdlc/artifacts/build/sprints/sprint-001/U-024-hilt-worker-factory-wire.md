---
type: story
status: planned
sprint: "000001"
artifact_type: user-story
priority: medium
complexity: medium
parallel_eligible: false
iteration: 2
requirements:
  - REQ-MODERNIZATION-008
design_docs:
  - DES-MODERNIZATION-008
integration_contracts: []
dependencies:
  - U-023
  - U-015
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-024
title: Wire HiltWorkerFactory into WorkManager Configuration.Provider and annotate BackupWorker/RestoreWorker with @HiltWorker + @AssistedInject
pipeline: ''
domain: modernization
---

# U-024: Wire HiltWorkerFactory into WorkManager Configuration.Provider and annotate BackupWorker/RestoreWorker with @HiltWorker + @AssistedInject

## Story

As a maintainer of SMS Backup+,
I want `BackupWorker` and `RestoreWorker` to receive their injected collaborators through the Hilt-managed `HiltWorkerFactory` rather than through WorkManager's default reflective no-arg factory,
so that workers are part of the same compile-time-verified dependency graph as every other application component, the no-arg worker constructor hack is eliminated, and `MainViewModel` is obtainable by Hilt-aware `Activity`/`Fragment` hosts without manual `ViewModelProvider.Factory` wiring.

## Acceptance Criteria

- [ ] AC-1: **[BackupWorker @HiltWorker + @AssistedInject annotation]**
  Given `BackupWorker` is a `CoroutineWorker` produced by U-015 (files at `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt`),
  when this story is complete,
  then `BackupWorker` carries the `@HiltWorker` class annotation and its primary constructor is annotated `@AssistedInject` with exactly two `@Assisted`-annotated parameters — `@Assisted context: Context` and `@Assisted params: WorkerParameters` — followed by any number of graph-supplied collaborator parameters (e.g., `backupUseCase: BackupUseCase`, `preferences: Preferences`, `tokenRefresher: TokenRefresher`). The `@AssistedInject` constructor must be the sole constructor of `BackupWorker`; no secondary constructor, no manual `WorkerParameters` unpacking outside the constructor, and no `@Inject` annotation (which would conflict with `@AssistedInject`). Verified by source inspection: the constructor signature matches the form `@AssistedInject constructor(@Assisted context: Context, @Assisted params: WorkerParameters, <injected deps>)`.

- [ ] AC-2: **[RestoreWorker @HiltWorker + @AssistedInject annotation]**
  Given `RestoreWorker` is a `CoroutineWorker` produced by U-015 (files at `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt`),
  when this story is complete,
  then `RestoreWorker` carries the `@HiltWorker` class annotation and its primary constructor is annotated `@AssistedInject` with `@Assisted context: Context` and `@Assisted params: WorkerParameters` as the first two parameters, followed by graph-supplied collaborators (e.g., `restoreUseCase: RestoreUseCase`, `tokenRefresher: TokenRefresher`). The constructor is the sole constructor of `RestoreWorker` and carries no `@Inject`. Verified by source inspection analogous to AC-1.

- [ ] AC-3: **[HiltWorkerFactory wired via Configuration.Provider on App]**
  Given `App` is the `@HiltAndroidApp`-annotated `Application` class (verified `App.java:52`),
  when this story is complete,
  then `App` implements `androidx.work.Configuration.Provider` and overrides `getWorkManagerConfiguration(): Configuration` to return a `Configuration.Builder().setWorkerFactory(hiltWorkerFactory).build()`, where `hiltWorkerFactory` is the `HiltWorkerFactory` instance injected into `App` via `@Inject lateinit var hiltWorkerFactory: HiltWorkerFactory`. The default WorkManager `ContentProvider` initializer (`androidx.startup.InitializationProvider` or `WorkManagerInitializer`) is removed from `app/src/main/AndroidManifest.xml` via a `<provider android:name="..." tools:node="remove"/>` entry so WorkManager does not auto-initialize before the Hilt factory is available. Verified by: (a) source inspection of `App`'s `getWorkManagerConfiguration()` body, (b) `grep -n "WorkManagerInitializer\|InitializationProvider" app/src/main/AndroidManifest.xml` confirms the removal node is present, and (c) `./gradlew :app:assembleDebug` produces a merged manifest with no active `WorkManagerInitializer` provider entry (confirmed by `aapt dump xmltree` on the built APK).

- [ ] AC-4: **[Integration test: BackupWorker resolves injected dependencies through HiltWorkerFactory, not the default reflective factory]**
  Given the Hilt component graph and the `HiltWorkerFactory` are configured per AC-3,
  when `TestListenableWorkerBuilder.from(context, BackupWorker::class.java).build()` is called inside an `@HiltAndroidTest`-annotated instrumented test with a `HiltAndroidRule` managing the component,
  then: (a) the call does not throw `NoSuchMethodException` (which would indicate WorkManager attempted to use the default reflective no-arg constructor); (b) the constructed `BackupWorker` instance has all injected collaborator fields non-null (verified by the test asserting `assertNotNull` on at least the `backupUseCase` and `preferences` fields, accessed via an `@VisibleForTesting` accessor or reflection-based assertion in the test body); (c) the same test is repeated for `RestoreWorker`. This AC is the definitive proof that the `@AssistedInject` factory path is active end-to-end — if the default factory were active, no-arg construction would be attempted and fail, surfacing as a test failure before the assertion is reached.

- [ ] AC-5: **[Integration test: RestoreWorker resolves injected dependencies through HiltWorkerFactory]**
  Given the same Hilt test component as AC-4,
  when `TestListenableWorkerBuilder.from(context, RestoreWorker::class.java).build()` is called,
  then the constructed `RestoreWorker` has its injected collaborators non-null (minimally `restoreUseCase` and `tokenRefresher`). No `NoSuchMethodException` is thrown. Verified in the same `@HiltAndroidTest` class as AC-4, as a separate `@Test` method.

- [ ] AC-6: **[@HiltViewModel annotation on MainViewModel]**
  Given `MainViewModel` exists and is a `ViewModel` subclass (introduced by U-021, located at `app/src/main/java/com/zegoggles/smssync/activity/MainViewModel.kt` or equivalent Kotlin file),
  when this story is complete,
  then `MainViewModel` carries the `@HiltViewModel` annotation on its class declaration and its constructor is annotated `@Inject`. Any `Activity` or `Fragment` that obtains `MainViewModel` does so via `by viewModels()` (Kotlin property delegate) without passing an explicit factory parameter. Any explicit `ViewModelProvider(this, factory).get(MainViewModel::class.java)` call site is removed and replaced by the `by viewModels()` delegate. Verified by: (a) source inspection confirms `@HiltViewModel` on the class and `@Inject` on the constructor; (b) `grep -rn "ViewModelProvider.*MainViewModel\|ViewModelProviders.*MainViewModel" app/src/main/` returns zero matches; (c) `./gradlew :app:compileDebugKotlin` succeeds — Dagger validates `@HiltViewModel` at compile time and fails if the `@Inject` constructor is missing or if the ViewModel is not accessible from the Hilt component.

- [ ] AC-7: **[No default WorkerFactory construction path survives in production source]**
  Given the `HiltWorkerFactory` is the active factory per AC-3,
  when the build completes,
  then `grep -rn "WorkerFactory()\|DefaultWorkerFactory\|ListenableWorkerFactory" app/src/main/` returns zero matches in production Kotlin/Java source files. This confirms no hand-rolled fallback factory was introduced alongside the Hilt factory. The only `WorkerFactory` reference in production code is the `hiltWorkerFactory` field injected on `App`.

- [ ] AC-8: **[Full build passes with Hilt annotation processor — compile-time graph verified for worker bindings]**
  Given `@HiltWorker` triggers Dagger's annotation processor to generate `BackupWorker_AssistedFactory` and `RestoreWorker_AssistedFactory` and add both to the `HiltWorkerFactory`'s multi-binding map,
  when `./gradlew :app:kaptDebugKotlin` (or `:app:kspDebugKotlin` if KSP is used) runs,
  then it completes with zero errors. Deliberately introduce a missing binding for one of `BackupWorker`'s collaborator types (e.g., remove the `@Provides` entry for `Preferences` in a throwaway branch), confirm the Dagger processor fails the build with a "[Dagger/MissingBinding]" error naming the missing type, then restore the binding and confirm the build is green. This demonstrates the graph is compile-time verified for worker dependencies, satisfying REQ-MODERNIZATION-008 AC-5.

### Integration Criteria

- [ ] IC-1: `App` implements `Configuration.Provider` — verifiable by `App.kt` (or `App.java`) carrying `implements Configuration.Provider` / `: Configuration.Provider` and the `getWorkManagerConfiguration()` override at the class declaration level. The Hilt `SingletonComponent` must inject `HiltWorkerFactory` into `App` as a field-injected member (field injection on `Application` is the only injection pattern Hilt supports for `@HiltAndroidApp` classes — constructor injection is not available on `Application`).

- [ ] IC-2: The `androidx.hilt:hilt-work` artifact is declared as an `implementation` dependency in `app/build.gradle` (or `app/build.gradle.kts`), and the corresponding `kapt`/`ksp` entry for `androidx.hilt:hilt-compiler` is present. Without `hilt-work`, the `HiltWorkerFactory` class does not exist on the classpath and the build fails at the import resolution stage. Verified by `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep hilt-work` returning a non-empty result.

- [ ] IC-3: `WorkManagerScheduler` (introduced by U-014 / U-015, at `app/src/main/java/com/zegoggles/smssync/service/WorkManagerScheduler.kt`) enqueues `BackupWorker::class.java` and `RestoreWorker::class.java` by class reference. After this story's `@HiltWorker` annotation is applied, those class references still compile without change — the `@HiltWorker` annotation is transparent to the `WorkManager.enqueue(OneTimeWorkRequestBuilder<BackupWorker>()...)` call site. Verified by confirming `WorkManagerScheduler` compiles and that an instrumented test that calls `scheduleImmediate()` results in a `WorkInfo` whose `workerClassName` is `com.zegoggles.smssync.service.BackupWorker`.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` | `@HiltWorker` annotation absent (or present as a stub); constructor may not yet carry `@AssistedInject` | Add `@HiltWorker` class annotation; annotate constructor `@AssistedInject`; verify `@Assisted Context` and `@Assisted WorkerParameters` are the first two parameters |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` | `@HiltWorker` annotation absent or stub | Add `@HiltWorker` class annotation; annotate constructor `@AssistedInject`; verify `@Assisted Context` and `@Assisted WorkerParameters` are the first two parameters |
| `app/src/main/java/com/zegoggles/smssync/App.java` (or migrated `App.kt`) | Implements `@HiltAndroidApp`; `onCreate()` contains `new Preferences(this)` Service-Locator (verified `App.java:71`); no `Configuration.Provider` implementation | Add `implements Configuration.Provider`; inject `HiltWorkerFactory` via `@Inject`; override `getWorkManagerConfiguration()` to return `Configuration.Builder().setWorkerFactory(hiltWorkerFactory).build()` |
| `app/src/main/AndroidManifest.xml` | May contain active `WorkManagerInitializer` or `InitializationProvider` entry allowing WorkManager auto-initialization | Add `<provider ... tools:node="remove"/>` for `androidx.startup.InitializationProvider` or the WorkManager `ContentProvider` to disable auto-initialization; only one of these entries is needed depending on the WorkManager version in use — verify the exact provider name with `./gradlew :app:processDebugManifest` |
| `app/src/main/java/com/zegoggles/smssync/activity/MainViewModel.kt` | `ViewModel` subclass (from U-021); obtainable via explicit factory or no annotation | Add `@HiltViewModel` annotation; annotate primary constructor with `@Inject`; remove any explicit `ViewModelProvider.Factory` usage at call sites |
| `app/build.gradle` (or `app/build.gradle.kts`) | May lack `androidx.hilt:hilt-work` dependency | Add `implementation "androidx.hilt:hilt-work:<version>"` and `kapt "androidx.hilt:hilt-compiler:<version>"` (or `ksp` equivalent); version must be consistent with the Hilt version introduced by U-022 |

## Existing Behavior to Preserve

- WorkManager's existing work request enqueue logic in `WorkManagerScheduler` must continue to function: `scheduleImmediate()`, `scheduleRegular()`, and `scheduleRestore()` must enqueue workers by class reference as before. The `@HiltWorker` annotation does not change worker class names or the `OneTimeWorkRequestBuilder` / `PeriodicWorkRequestBuilder` call sites.
- `BackupWorker.doWork()` and `RestoreWorker.doWork()` business logic (introduced by U-015) must not be altered by this story. This story's scope is strictly the DI wiring annotations and the factory configuration on `App` — not the worker execution logic.
- `App.onCreate()` behavior (StrictMode flags, notification channel registration, `preferences.migrate()`, receiver enable/disable, K9MailLib debug hook — verified in `App.java`) must not regress. Adding `Configuration.Provider` adds a new interface implementation to `App` without touching `onCreate()`.
- The XOAuth2 auth-retry path in the workers (U-015 AC-10b / AC-11c) constructs a fresh IMAP store per retry. Injecting `MailTransport` as a `Provider<MailTransport>` (rather than a singleton instance) must be confirmed at implementation time to preserve this per-retry construction semantics — this story does not alter the retry logic itself, only ensures the binding is not accidentally collapsed into a singleton.
- Any existing `@Singleton`-scoped bindings established by U-022 (`Preferences`, `AuthPreferences`, `SyncStateRepository`, port adapters) remain unchanged. This story adds only `HiltWorkerFactory` injection on `App` and worker-level `@HiltWorker` annotations.

## Verification Steps

1. **AC-1/AC-2 (annotation presence — source inspection):** Open `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` and `RestoreWorker.kt`. Confirm each file contains `@HiltWorker` immediately before the class declaration and `@AssistedInject` on the constructor. Confirm `@Assisted context: Context` and `@Assisted params: WorkerParameters` are the first two constructor parameters. Run `grep -n "@HiltWorker\|@AssistedInject\|@Assisted" app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` and verify each file has exactly one `@HiltWorker`, one `@AssistedInject`, and at least two `@Assisted` occurrences.

2. **AC-3 (Configuration.Provider wiring — source + manifest + build):**
   a. Run `grep -n "Configuration.Provider\|getWorkManagerConfiguration\|hiltWorkerFactory" app/src/main/java/com/zegoggles/smssync/App.java` (or `App.kt`). Must return at least three matches.
   b. Run `grep -n "WorkManagerInitializer\|InitializationProvider" app/src/main/AndroidManifest.xml`. Must return a line containing `tools:node="remove"`.
   c. Run `./gradlew :app:assembleDebug`. After assembly, run `./gradlew :app:processDebugManifest` and inspect the merged manifest at `app/build/intermediates/merged_manifest/debug/AndroidManifest.xml`. Confirm no active `WorkManagerInitializer` or `InitializationProvider` `<provider>` entry is present (only the `tools:node="remove"` tombstone). If the merged manifest still contains an active entry, the `tools:node="remove"` was applied to the wrong provider name — correct the manifest entry and re-run.

3. **AC-4/AC-5 (HiltWorkerFactory integration test — instrumented):** Run `./gradlew :app:connectedDebugAndroidTest --tests "*.HiltWorkerFactoryTest"`. The test class must be annotated `@HiltAndroidTest` with a `@get:Rule val hiltRule = HiltAndroidRule(this)` and `@get:Rule val workerRule = ...` as appropriate. The test for `BackupWorker` calls `TestListenableWorkerBuilder.from(context, BackupWorker::class.java).setWorkerFactory(hiltRule.component ?: applicationContext.getSystemService(WorkManager::class.java).configuration.workerFactory).build()` (exact API depends on the `work-testing` version — verify against `androidx.work:work-testing` documentation at implementation time). Assert the result is non-null and that calling `worker.doWork()` does not throw `NoSuchMethodException`. Repeat for `RestoreWorker`. Both assertions must pass. The test must be in `app/src/androidTest/java/com/zegoggles/smssync/service/HiltWorkerFactoryTest.kt`.

4. **AC-6 (@HiltViewModel — source + grep + compile):**
   a. Run `grep -n "@HiltViewModel\|@Inject" app/src/main/java/com/zegoggles/smssync/activity/MainViewModel.kt`. Must return `@HiltViewModel` on the line before the class declaration and `@Inject` on the constructor.
   b. Run `grep -rn "ViewModelProvider.*MainViewModel\|ViewModelProviders.*MainViewModel\|ViewModelProvider\.Factory" app/src/main/`. Must return zero matches.
   c. Run `./gradlew :app:compileDebugKotlin`. Must complete with zero Dagger errors. A missing `@Inject` on the `MainViewModel` constructor produces a Dagger compile error naming `MainViewModel` — if this error is seen, add `@Inject` to the constructor and re-run.

5. **AC-7 (no default factory remnants — grep):** Run `grep -rn "WorkerFactory()\|DefaultWorkerFactory\|ListenableWorkerFactory" app/src/main/java/`. Must return zero matches.

6. **AC-8 (compile-time graph verified — deliberate missing binding):**
   a. In a local throwaway branch, comment out the `@Provides fun providePreferences(...)` method in the Hilt `PreferencesModule` (introduced by U-022). Run `./gradlew :app:kaptDebugKotlin`. Confirm the build fails with `[Dagger/MissingBinding] Preferences cannot be provided`. Restore the method and confirm `./gradlew :app:kaptDebugKotlin` is green.
   b. Record in the PR description that the deliberate-missing-binding failure was reproduced and the binding was restored.

7. **IC-2 (hilt-work dependency — classpath check):** Run `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep hilt-work`. Output must include `androidx.hilt:hilt-work:<version>`. If missing, add the dependency and re-run.

8. **IC-3 (WorkManagerScheduler call sites unchanged — compile + instrumented test):** Run `./gradlew :app:compileDebugKotlin`. Must succeed. Run `./gradlew :app:connectedDebugAndroidTest --tests "*.WorkManagerSchedulerTest"` (test introduced by U-014/U-015). The test that calls `scheduleImmediate()` and asserts `WorkInfo.workerClassName == "com.zegoggles.smssync.service.BackupWorker"` must pass.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android / Kotlin (app module) | `BackupWorker.kt` and `RestoreWorker.kt` annotation updates (`@HiltWorker`, `@AssistedInject`, `@Assisted`); `App` `Configuration.Provider` implementation with field-injected `HiltWorkerFactory`; `AndroidManifest.xml` WorkManager initializer removal; `@HiltViewModel` on `MainViewModel` | Developer |
| Android build (Gradle) | Add `implementation "androidx.hilt:hilt-work"` + `kapt/ksp "androidx.hilt:hilt-compiler"` to `app/build.gradle`; confirm version alignment with the Hilt version from U-022 | Developer |
| Android / Instrumented tests | `HiltWorkerFactoryTest.kt` — `@HiltAndroidTest` test confirming `BackupWorker` and `RestoreWorker` resolve dependencies through the factory; AC-4 and AC-5 | Developer |

## Technical Notes

**`@HiltWorker` mechanism — why `@AssistedInject` and not `@Inject`.** WorkManager instantiates workers reflectively using a `WorkerFactory`; Hilt overrides this factory via `HiltWorkerFactory`, which delegates to Dagger-generated `AssistedFactory` implementations per worker class. The `@HiltWorker` annotation marks the class for this delegation; `@AssistedInject` on the constructor tells Dagger to generate an `AssistedFactory` (the `BackupWorker_AssistedFactory`) that accepts `Context` and `WorkerParameters` at instantiation time (the `@Assisted` parameters) and resolves all remaining constructor parameters from the `SingletonComponent` graph. Using `@Inject` instead of `@AssistedInject` would cause a Dagger compile error because `Context` and `WorkerParameters` have no bindings in the component graph — they can only be passed as assisted parameters by the factory.

**`Configuration.Provider` on `Application` — field injection is mandatory.** `@HiltAndroidApp` generates the `Hilt_App` base class, which performs `inject(this)` during `App.onCreate()`. As a result, `HiltWorkerFactory` must be declared as a field-injected member (`@Inject lateinit var hiltWorkerFactory: HiltWorkerFactory`) on `App`, not a constructor parameter. `getWorkManagerConfiguration()` is called by WorkManager before (or independently of) `onCreate()` on some API levels, which means the field must be non-null by the time that method is called. The safe pattern is to call `WorkManager.initialize(this, getWorkManagerConfiguration())` explicitly inside `App.onCreate()` after Hilt's injection completes (i.e., after `super.onCreate()`), rather than relying on WorkManager's automatic initialization from the `ContentProvider`. This is the standard `Configuration.Provider` + Hilt integration pattern documented by the `androidx.hilt:hilt-work` library.

**Disabling WorkManager auto-initialization — manifest entry.** WorkManager 2.6+ uses `androidx.startup.AppStartup` (via `InitializationProvider`) for auto-initialization. The correct `tools:node="remove"` target is:
```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    tools:node="remove" />
```
For WorkManager versions prior to 2.6 that use a standalone `ContentProvider`, the target is `androidx.work.impl.WorkManagerInitializer`. Verify the exact class name for the WorkManager version in use by running `./gradlew :app:processDebugManifest` and inspecting the merged manifest before the removal entry is added, then confirm the entry is gone after.

**`@HiltViewModel` on MainViewModel — compile-time contract.** Hilt validates `@HiltViewModel` at Dagger annotation-processing time. If `MainViewModel`'s constructor contains parameters that are not bound in the `ActivityRetainedComponent` (the Hilt component that backs ViewModels), Dagger will emit a `[Dagger/MissingBinding]` error at compile time. This is the correct behavior: it converts a runtime `ViewModelProvider` instantiation crash into a build failure. Ensure every `MainViewModel` constructor parameter is either `@Singleton`-scoped (accessible from `ActivityRetainedComponent` via parent component inheritance) or has a binding in `ViewModelComponent` or `ActivityRetainedComponent`. `SyncStateRepository`, `Preferences`, and `AuthPreferences` are `@Singleton`-scoped by U-022 and are therefore accessible; if `MainViewModel` consumes any of the ports (e.g., `BackupScheduler`), those must also be `@Singleton`-scoped (they are, per DES-MODERNIZATION-008 `SchedulerModule`).

**Dependency boundary with U-022 and U-023.** U-022 (Hilt bootstrap) must have introduced `@HiltAndroidApp` on `App` and the `PreferencesModule`/`EventModule`/`SchedulerModule`/etc. singleton bindings. U-023 (engine injection + test-ctor deletion) must have applied `@Inject` constructors on the engine collaborators (`BackupUseCase`, `RestoreUseCase`, `MessageConverter`, etc.). This story depends on both being complete before the `@AssistedInject` constructor of `BackupWorker` can be satisfied: Dagger will emit `[Dagger/MissingBinding]` for any collaborator type that has no `@Inject` constructor and no `@Provides`/`@Binds` entry. If U-022 or U-023 is not yet merged at implementation time, this story is blocked; do not work around the missing bindings with manual construction — the compile error is the correct signal.

**`MailTransport` binding and the auth-retry hazard.** DES-MODERNIZATION-008 §Behavior-preservation guarantees (Preserved Core, point 2) explicitly flags that the IMAP store must not be a cached singleton, because the auth-retry path at `BackupTask.java:189-192` / `RestoreTask.java:163-165` constructs a fresh store per retry. The `MailTransport` binding in `MailModule` must use `Provider<MailTransport>` at the worker's injection site (or bind it as unscoped) so each retry obtains a new transport instance. Confirm this binding decision at implementation time and record in Implementation Notes.

**Estimation: Medium.** The annotation changes themselves are mechanical (2–4 lines per worker). The `Configuration.Provider` wiring on `App` is ~10 lines. The manifest change is 1–3 lines. The instrumented test is ~40–60 lines. The `@HiltViewModel` annotation is 1–2 lines plus call-site cleanup. The total implementation surface is small but integration-sensitive: the most common defect is the wrong `tools:node="remove"` target (wrong provider class name), which causes WorkManager to initialize before Hilt's factory is registered, producing a silent fallback to the default reflective factory and a runtime crash when a worker is first enqueued. AC-4/AC-5 exist specifically to catch this; they must be run on a real device or emulator, not just a local unit test.

## Supporting Documentation

- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-008-introduce-hilt-dependency-injection.md` — AC-4 (workers are `@HiltWorker`-injected), AC-5 (no Service-Locator; compile-time verified graph)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-008-hilt-di-design.md` — §`@HiltWorker` for the CoroutineWorkers (AC-4); §No `App`-level Service-Locator lookups remain (AC-3, AC-7); §Build configuration (IC-2); §Validation table rows AC-4 and AC-5; §Behavior-preservation guarantees (MailTransport/auth-retry hazard)

## Integration Contract References

- DES-MODERNIZATION-008 §Component graph and scope — `@HiltWorker` (assisted, per-work) row: `BackupWorker`, `RestoreWorker` — defines that workers are instantiated via `HiltWorkerFactory` with `@AssistedInject` constructors taking `@Assisted Context` + `@Assisted WorkerParameters`
- DES-MODERNIZATION-008 §`@HiltWorker` for the CoroutineWorkers — specifies that `App` implements `Configuration.Provider` with the `androidx.hilt:hilt-work` integration (AC-3); identifies the factory wiring as the mechanism by which WorkManager-instantiated workers receive injected dependencies (AC-4/AC-5)
- DES-MODERNIZATION-008 §Design Validation table row AC-4 — pass condition: "DES-005 workers carry `@HiltWorker` + `@AssistedInject`; `HiltWorkerFactory` is wired via `Configuration.Provider`; a worker test resolves dependencies through the factory" — this is the direct source for AC-4/AC-5 of this story

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Draft content written against DES-MODERNIZATION-008 (read in full this session, verified `@HiltWorker` section, `Configuration.Provider` pattern, ADR-008, `App.java:52/71` source verification, worker annotation shape) and REQ-MODERNIZATION-008 (read in full, AC-4 `@HiltWorker`-injected workers, AC-5 compile-time-verified graph). U-015 story read (verified `BackupWorker`/`RestoreWorker` as `@HiltWorker CoroutineWorker` outputs from that story, `@AssistedInject` constructor shape). NOT finalized; load-bearing open item for review: the exact `tools:node="remove"` target class name depends on the WorkManager version adopted in U-014/U-015 — the implementing developer must run `./gradlew :app:processDebugManifest`, inspect the merged manifest before removal, and record the confirmed class name in Implementation Notes before this story is marked Done.
