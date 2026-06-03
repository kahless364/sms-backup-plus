---
type: story
status: done
sprint: '000001'
artifact_type: user-story
priority: medium
complexity: medium
parallel_eligible: true
iteration: 1
requirements:
  - REQ-MODERNIZATION-008
design_docs:
  - DES-MODERNIZATION-008
integration_contracts: []
dependencies:
  - U-020
  - U-003
change_records: []
platforms:
  - android
tags:
  - hilt
  - dependency-injection
  - singleton-component
  - bootstrap
gate_additions: []
id: U-022
title: 'Hilt bootstrap: @HiltAndroidApp on App, Gradle plugin, kapt/ksp, and all SingletonComponent @Module classes'
pipeline: modernization
domain: modernization
requirement_source: authored
updated_at: '2026-06-03T22:01:18.925Z'
resolution: done
---

# U-022: Hilt Bootstrap — @HiltAndroidApp, Gradle Plugin, and Singleton Modules

## Story

As a developer maintaining SMS Backup+,
I want the Hilt dependency-injection framework wired into the application entry point with all seven `@InstallIn(SingletonComponent::class)` provider modules present and bound to every port whose adapter already exists,
So that every subsequent Hilt story (U-023 engine injection, U-024 worker factory) can activate bindings incrementally against a verified, compile-time-checked component graph without any manual wiring remaining at the App and ServiceBase preference-construction sites.

## Acceptance Criteria

- [ ] AC-1: The `com.google.dagger.hilt.android` Gradle plugin is applied in `app/build.gradle` (or `build.gradle.kts`), Hilt 2.50 or later is declared as an `implementation` dependency, `hilt-compiler` is declared under `kapt` (or `ksp` if KSP is active for the module), and `androidx.hilt:hilt-work` is declared as an `implementation` dependency. The project assembles without error after the plugin and dependency additions.

- [ ] AC-2: `App` (currently `app/src/main/java/com/zegoggles/smssync/App.java`) carries the `@HiltAndroidApp` annotation. The existing `App.onCreate()` logic — StrictMode setup, notification channel creation, `preferences.migrate()`, receiver enable/disable, K9MailLib debug hook — is fully retained without modification. The annotation is the only structural change to `App`.

- [ ] AC-3: The `new Preferences(this)` call at `App.java:71` is removed. A `@Inject`-annotated `Preferences` field replaces it. The field is populated by Hilt before `onCreate()` body executes; `preferences.migrate()` and all other usages of the field within `App.onCreate()` continue to work correctly.

- [ ] AC-4: The three Service-Locator `new` calls in `ServiceBase` — `new Preferences(this)` at `:73`, `new Preferences(getApplicationContext())` at `:108`, and `new AuthPreferences(this)` at `:112` — are replaced by `@Inject`-annotated fields (`Preferences` and `AuthPreferences` respectively). No call site in `ServiceBase` invokes a preferences constructor directly after this change. (If `ServiceBase` is dissolved by U-023/DES-005 before this story lands, this AC is satisfied by the absence of those lines.)

- [ ] AC-5: The following seven Kotlin `@Module` classes exist, each annotated `@InstallIn(SingletonComponent::class)`, each in the package `com.zegoggles.smssync.di` (or a sub-package thereof), each in a dedicated file under `app/src/main/java/com/zegoggles/smssync/di/`:

  | Module class | Binding type | Bound interface | Bound implementation | Condition |
  |---|---|---|---|---|
  | `SchedulerModule` | `@Binds` | `BackupScheduler` | `WorkManagerScheduler` | Only when `WorkManagerScheduler` adapter exists (DES-005); otherwise module is a stub with a comment |
  | `MailModule` | `@Binds` | `MailTransport` | `K9ImapTransport` | Only when `K9ImapTransport` adapter exists (DES-009); otherwise stub |
  | `SecretModule` | `@Provides` | `SecretStore` | `EncryptedPrefsSecretStore` constructed from `@ApplicationContext` + Keystore master key | Only when adapter exists (DES-004); otherwise stub |
  | `EventModule` | `@Binds` | `SyncStateRepository` | `DefaultSyncStateRepository` | Only when repository exists (DES-007 / U-020); otherwise stub |
  | `ContactsModule` | `@Binds` / `@Provides` | `ContactsPort`, `CalendarPort` | Respective adapters (DES-011) | Only when adapters exist; otherwise stubs |
  | `PreferencesModule` | `@Provides` | `Preferences`, `AuthPreferences`, `DataTypePreferences` | Each constructed from `@ApplicationContext` | Always active — adapters are existing concrete classes |
  | `DispatcherModule` | `@Provides` `@IoDispatcher` | `CoroutineDispatcher` | `Dispatchers.IO` | Always active |

  Each module that does not yet have a live adapter contains a `// TODO(U-NNN): activate when <AdapterClass> lands` comment in place of the binding, and the module compiles without error.

- [ ] AC-6: `PreferencesModule` provides `Preferences`, `AuthPreferences`, and `DataTypePreferences` each scoped `@Singleton` via `@Provides` methods that accept `@ApplicationContext Context`. A `grep` for `new Preferences(` and `new AuthPreferences(` across all files under `app/src/main/java/` returns zero matches in `App.java` and `ServiceBase.java` after this story is complete.

- [ ] AC-7: `DispatcherModule` declares a `@Qualifier` annotation `@IoDispatcher` (in `app/src/main/java/com/zegoggles/smssync/di/IoDispatcher.kt`) and provides `@IoDispatcher CoroutineDispatcher = Dispatchers.IO` scoped `@Singleton`. The qualifier file and the module are the only new files introduced by this AC; no production logic changes.

- [ ] AC-8: The build is deliberately broken by removing one `@Provides`/`@Binds` method from `PreferencesModule` (the `Preferences` provider) in a throwaway local branch, and `./gradlew :app:compileDebugKotlin` (or `:app:kaptDebugKotlin`) fails with a Dagger "cannot be provided without an `@Inject` constructor or an `@Provides`-annotated method" error before any runtime execution. The error appears at annotation-processor / compile time, not at test runtime. This branch is then reverted; the passing build is the deliverable. The compile error is documented in the PR description as evidence of AC-8.

- [ ] AC-9: `./gradlew :app:assembleDebug` passes with zero new lint errors or warnings that are not pre-existing in the codebase. Hilt-generated source files (under `build/generated/`) are excluded from lint as generated sources via the standard `lintOptions { ignore "GeneratedCode" }` or equivalent AGP 8.x configuration. The `-Werror` / `-Xlint:deprecation` fitness function is not broken.

- [ ] AC-10: The existing test suite — `./gradlew :app:testDebugUnitTest` — passes without modification to any test class. No test class is migrated to `@HiltAndroidTest` in this story (that migration belongs to U-023); this story must not break tests that currently use `BackupTask`'s test-only constructor or direct `new Preferences(this)` instantiation in test helpers.

### Integration Criteria

- [ ] IC-1: `App` is annotated `@HiltAndroidApp` at its class declaration in `app/src/main/java/com/zegoggles/smssync/App.java` (or `App.kt` after any Kotlin conversion). The annotation is the first annotation on the class declaration line or on the line immediately preceding it.
- [ ] IC-2: Every module class in `com.zegoggles.smssync.di` is reachable from the `SingletonComponent` component graph at compile time — confirmed by a clean `./gradlew :app:kaptDebugKotlin` (or `:app:kspDebugKotlin`) run producing no "unbound" or "missing binding" warnings in its output.
- [ ] IC-3: `ServiceBase` (or its successor after DES-005 dissolves it) compiles with the injected `Preferences` and `AuthPreferences` fields; no direct constructor calls to those classes remain in the `service/` package after this story's changes.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/build.gradle` (or `.kts`) | No Hilt plugin or dependencies | Apply `com.google.dagger.hilt.android` plugin; add `implementation "com.google.dagger:hilt-android:2.50+"`, `kapt "com.google.dagger:hilt-compiler:2.50+"`, `implementation "androidx.hilt:hilt-work:1.1+"` |
| `app/src/main/java/com/zegoggles/smssync/App.java` | `public class App extends Application` with `new Preferences(this)` at line 71; no DI framework | Add `@HiltAndroidApp`; convert `new Preferences(this)` to `@Inject Preferences preferences` field |
| `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` | `new Preferences(this)` at :73; `new Preferences(getApplicationContext())` at :108; `new AuthPreferences(this)` at :112 — all per-call Service-Locator construction | Replace three constructor calls with `@Inject Preferences preferences` and `@Inject AuthPreferences authPreferences` fields |
| `app/src/main/java/com/zegoggles/smssync/di/` (new directory) | Does not exist | Create: `PreferencesModule.kt`, `DispatcherModule.kt`, `IoDispatcher.kt`, `SchedulerModule.kt`, `MailModule.kt`, `SecretModule.kt`, `EventModule.kt`, `ContactsModule.kt` |
| `build.gradle` (root, if version catalog not used) | Hilt plugin not declared | Add `classpath "com.google.dagger:hilt-android-gradle-plugin:2.50+"` to `buildscript.dependencies` |

## Existing Behavior to Preserve

- `App.onCreate()` execution order: StrictMode policy installation runs first, then notification channel creation, then `preferences.migrate()`, then receiver enable/disable based on account configuration, then K9MailLib debug hook. None of these steps may change order or be omitted.
- `ServiceBase.getPreferences()` and `ServiceBase.getAuthPreferences()` return the same singleton instance on every call after injection — the semantics change from per-call construction (ARCH-002 defect) to injected singleton, but the returned object must behave identically to a freshly constructed `Preferences(getApplicationContext())` for all read operations.
- The K9MailLib debug hook at the end of `App.onCreate()` (which sets K9's debug flag from the `Preferences` object) must still see the same `Preferences` value it currently reads from `new Preferences(this)`.
- No behavior change in backup or restore flows. This story changes only the construction site of `Preferences`/`AuthPreferences`; the objects themselves are unmodified.
- The `-Werror` lint fitness function must not be relaxed. Any new Hilt-generated sources that trigger lint warnings must be excluded via `generated source` configuration, not by weakening the lint rules.

## Verification Steps

1. **AC-1 — Plugin and dependency addition:** Run `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -i hilt`. Confirm `hilt-android`, `hilt-work`, and generated code artifacts appear. Run `./gradlew :app:assembleDebug` and confirm it exits 0.

2. **AC-2 / AC-3 — @HiltAndroidApp and injected Preferences field:** Open `App.java` (or `.kt`) and confirm the `@HiltAndroidApp` annotation is present on the class. Confirm `new Preferences(this)` does not appear. Confirm a `@Inject Preferences preferences` (or `lateinit var preferences: Preferences`) field is present. Run the app on an emulator (API 26+), open Settings, and verify preferences are visible and functional (preferences.migrate() ran).

3. **AC-4 — ServiceBase Service-Locator removal:** Run `grep -rn "new Preferences\|new AuthPreferences" app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java`. The command must return zero lines. Run `./gradlew :app:assembleDebug` to confirm the service still compiles.

4. **AC-5 — Seven module classes exist:** Run `ls app/src/main/java/com/zegoggles/smssync/di/`. Confirm all seven `*Module.kt` files are present. Open each file and verify `@Module` and `@InstallIn(SingletonComponent::class)` annotations are present. For stub modules, confirm the `// TODO` comment names the story that will activate the binding.

5. **AC-6 — No new Preferences constructor calls at App/ServiceBase:** Run `grep -rn "new Preferences(" app/src/main/java/com/zegoggles/smssync/App.java app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java`. The command must return zero results.

6. **AC-7 — IoDispatcher qualifier:** Open `app/src/main/java/com/zegoggles/smssync/di/IoDispatcher.kt` and confirm it is annotated `@Qualifier @Retention(AnnotationRetention.BINARY)`. Open `DispatcherModule.kt` and confirm the `@Provides @IoDispatcher @Singleton` method returns `Dispatchers.IO`.

7. **AC-8 — Compile-time graph verification:** In a local branch, comment out the `@Provides fun providePreferences(...)` method in `PreferencesModule`. Run `./gradlew :app:kaptDebugKotlin` (or `kspDebugKotlin`). Confirm the build fails with a Dagger "cannot be provided" error. Revert the change. Run `./gradlew :app:kaptDebugKotlin` again and confirm it exits 0.

8. **AC-9 — No new lint errors:** Run `./gradlew :app:lintDebug`. Compare the lint report `app/build/reports/lint-results-debug.html` against the pre-story baseline (capture baseline before applying this story's changes). No new error-level findings may appear. Warning-level findings from Hilt-generated sources must be suppressed by the generated-source exclusion, not by new `@SuppressLint` annotations.

9. **AC-10 — Existing tests pass unmodified:** Run `./gradlew :app:testDebugUnitTest`. All tests that passed before this story must still pass. No test file is modified by this story.

10. **IC-2 — SingletonComponent reachable:** After the first clean build with Hilt, inspect `app/build/generated/source/kapt/debug/` (or ksp equivalent). Confirm `Hilt_App.java` (or `.kt`) and `DaggerApp_HiltComponents_SingletonC.java` are generated. A missing generated file indicates a graph failure that would have been caught by AC-8.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (app module) | All changes — Gradle plugin, App annotation, ServiceBase injection, seven DI module files | Developer |

## Technical Context

**Hilt + manual coexistence strategy.** This story establishes the Hilt graph but deliberately does not activate bindings for ports whose adapters do not yet exist (`SchedulerModule`, `MailModule`, `SecretModule`, `EventModule`, `ContactsModule`). Those modules are present as compilable stubs. This is the coexistence path specified in DES-MODERNIZATION-008 "Incremental coexistence" section: manual `new` sites that have no live binding yet are left untouched (they are in the engine, not in `App`/`ServiceBase`). Only `PreferencesModule` and `DispatcherModule` are fully live after this story because `Preferences`, `AuthPreferences`, `DataTypePreferences`, and `Dispatchers.IO` require no sibling-story adapter.

**kapt vs. ksp decision.** Use `kapt` unless U-003 has already migrated the module to KSP. Hilt 2.50+ supports both. The choice must match what AGP 8.x + the current Kotlin version supports in this module. If `kapt` is already present (for other annotation processors), add `hilt-compiler` alongside existing kapt declarations. Do not introduce KSP for the first time in this story — that is a scope expansion beyond U-003's responsibility.

**@HiltAndroidApp and Application subclass.** `App.java` uses `extends Application`, not a Hilt-generated base class. `@HiltAndroidApp` generates `Hilt_App` which extends `Application`, and the annotation processor rewrites `App` to extend `Hilt_App`. This is handled transparently by the plugin; no manual base-class change is required.

**ServiceBase injection prerequisite.** `ServiceBase` is an `abstract class` extended by `SmsBackupService` and `SmsRestoreService`, both of which are Android `Service` subclasses. For field injection to work, each concrete `Service` subclass must be annotated `@AndroidEntryPoint`. Annotate `SmsBackupService` and `SmsRestoreService` with `@AndroidEntryPoint` as part of this story. Without that annotation, Hilt will not inject the `@Inject` fields in `ServiceBase`. The `AndroidManifest.xml` entry for each service is unchanged — `@AndroidEntryPoint` is a source annotation only.

**Dagger `@Binds` vs. `@Provides` for stubs.** A stub module that has a `@Binds` method referencing an interface that does not yet have an implementation will fail to compile if the implementation class is absent. For stub modules where the implementation class does not yet exist, the `@Binds` method must be commented out entirely (with a `// TODO` comment); do not leave a `@Binds` referencing a missing class. A `@Provides` stub that returns `TODO()` (Kotlin) will compile but fail at runtime — do not use that pattern. Comment out the entire binding method.

**`@Singleton` scope on PreferencesModule providers.** `Preferences`, `AuthPreferences`, and `DataTypePreferences` are currently constructed per-call at their Service-Locator sites. Converting them to `@Singleton` changes the construction count from "many" to "one". This is safe because both classes are stateless readers of `SharedPreferences` (the underlying Android `SharedPreferences` instance is already a singleton via `PreferenceManager.getDefaultSharedPreferences`). The `@Singleton` scope is correct and removes the ARCH-002 defect.

**Verification of compile-time graph property (AC-8).** The deliberate-breakage test is required, not optional. It is the mechanical proof of REQ-008 AC-5 ("the dependency graph is compile-time verified") and the primary ADR-008 differentiator over Koin. The PR must include the build-failure output (screenshot or log excerpt) as evidence.

**Dependency on U-020.** U-020 delivers `SyncStateRepository` / `DefaultSyncStateRepository` (DES-007). `EventModule`'s `@Binds` binding for `SyncStateRepository` can only be activated after U-020 is merged. In this story, `EventModule` is a compilable stub. The `depends_on: U-020` dependency is a logical ordering constraint, not a hard compile-time block for this story (because the binding is stubbed).

**Dependency on U-003.** U-003 raises AGP to 8.x and confirms Kotlin is present in the `app` module. Hilt 2.50+ requires AGP 8.x for its Gradle plugin compatibility. Do not attempt to apply the Hilt plugin in this story until U-003 is merged and the AGP upgrade is confirmed.

## Supporting Documentation

- REQ-MODERNIZATION-008 — AC-1 (Hilt configured), AC-5 (no App-level Service-Locator, compile-time verified)
- DES-MODERNIZATION-008 — "System Architecture: Component graph and scope" table; "Provider strategy: @Inject constructors for app-owned types; @Module @Provides/@Binds for the ports"; "Incremental coexistence: Hilt and manual construction during conversion"; "Build configuration"
- DES-MODERNIZATION-008 — "No App-level Service-Locator lookups remain (realizes AC-5)" — verified source lines: `App.java:71`, `ServiceBase.java:73,108,112`
- DES-MODERNIZATION-008 — "Architecture Decision Record: Hilt vs. Koin vs. continued manual DI" — rationale for compile-time graph requirement

## Integration Contract References

This story consumes no CNTR artifacts. It produces the DI module stubs that downstream stories (`U-023`, `U-024`) will activate. The port contracts consumed by the live modules (`PreferencesModule`, `DispatcherModule`) are internal app types (`Preferences`, `AuthPreferences`, `DataTypePreferences`) with no cross-component contract artifact.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Bootstrap slice only: this story wires Hilt into the app entry point and establishes all seven module stubs, removes App/ServiceBase Service-Locator construction, and validates the compile-time graph property; engine constructor injection and test-harness migration are U-023, and `@HiltWorker` factory wiring is U-024.
