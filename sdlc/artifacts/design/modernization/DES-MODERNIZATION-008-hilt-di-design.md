---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-008
related_stories: []
related_design_docs: []
integration_contracts: []
change_records: []
type: ''
id: DES-MODERNIZATION-008
title: ''
domain: modernization
---

# DES-MODERNIZATION-008: Hilt Dependency Injection Design

## Overview

This design specifies the introduction of **Hilt** (Dagger-backed, Android-first
dependency injection) to replace the codebase's hand-wired construction graph and its
manual Service-Locator. It traces to **REQ-MODERNIZATION-008** (Introduce Hilt
Dependency Injection, `status: approved`, `type: constraint`) and realizes
**ADR-008** of `target-state.md` and migration unit **MU-007**.

The scope is the **DI seam**, not the engine's behavior: Hilt formalizes the
construction graph that the code already wires by hand. It is deliberately the **last**
substrate unit in Phase 2 because the graph it injects only stabilizes after the
WorkManager swap (DES-005 / MU-005) and the Otto→Flow swap (DES-007 / MU-006) have
re-shaped what the engine constructs and consumes.

Three concrete, source-verified defects motivate this design:

1. **A manual Service-Locator** — `ServiceBase.getPreferences()` returns
   `new Preferences(getApplicationContext())` and `getAuthPreferences()` returns
   `new AuthPreferences(this)` **on every call** (verified `ServiceBase.java:107-113`);
   `onCreate()` does `new Preferences(this)` again (`:73`); `App.onCreate()` constructs
   its own `new Preferences(this)` (`App.java:71`). This is ARCH-002.
2. **A hand-wired construction graph** — `BackupTask`'s primary constructor directly
   `new`s `BackupItemsFetcher` (wrapping `new BackupQueryBuilder`), `PersonLookup`,
   `ContactAccessor`, `MessageConverter`, and — conditionally — `CalendarSyncer`
   (only when `isCallLogCalendarSyncEnabled()`), plus `TokenRefresher` (which itself
   `new`s an `OAuth2Client`) (verified `BackupTask.java:61-88`). Adding a collaborator
   means editing the constructor body.
3. **A duplicate test-only constructor** — `BackupTask` carries a second, fully-injecting
   8-parameter constructor (verified `BackupTask.java:90-106`) that exists purely as a
   Humble-Object test seam and duplicates the dependency list.

This design replaces (1) and (2) with constructor injection rooted in a Hilt component
graph, and deletes (3).

## Context

**Why now.** The construction graph is the *output* of two other migrations. Until the
engine runs as `@HiltWorker` CoroutineWorkers (DES-005) rather than `AsyncTask` instances
owned by `Service`s, and until eventing is an injected `SyncStateRepository` (DES-007)
rather than the static `App.bus`, the set of things to inject keeps changing. Sequencing
Hilt last (MU-007 depends on MU-005 and MU-006 — verified migration-units.md Dependency
Matrix) avoids re-authoring `@Module`s twice.

**Why Hilt is low-risk here.** The codebase *already does DI by hand*. The
Testability assessment (ilities-assessment.md, Testability §) grades the module Green
**only because** the team hand-built Humble-Object seams — the `BackupTask` second
constructor (`:90`) is named there explicitly. Hilt formalizes a seam the tests already
depend on; it does not introduce a new architectural concept, only a compile-time-verified
mechanism for one that exists informally.

**A verified correction to the requirement's framing.** REQ-008 and MU-007 both describe a
"`BackupTask`/`RestoreTask` dual-constructor" pattern. Source verification shows this is
**only true of `BackupTask`**. `RestoreTask` has a **single** 4-parameter constructor
(`RestoreTask(SmsRestoreService, MessageConverter, ContentResolver, TokenRefresher)` —
verified `RestoreTask.java:61-70`): it is already constructor-injected and has **no**
manual `new`-wiring to remove and **no** test-only secondary constructor to delete. This
design records the divergence so the implementing story does not chase a non-existent
`RestoreTask` second constructor. `RestoreTask`'s work under this design is limited to (a)
becoming an injectable type and (b) dropping the `App.register/unregister/post` static
calls once DES-007 lands.

---

## System Architecture

### Decision: Adopt Hilt as the application-wide DI container (realizes ADR-008)

Hilt is introduced as the single dependency-injection container for the app. The
component graph is rooted at the `Application` and provides every collaborator the engine,
services, receivers, and (eventually) workers consume.

**Object-graph entry point.** `App` is annotated `@HiltAndroidApp`
(verified target: `App.java:52`, `public class App extends Application`). This generates
the `SingletonComponent` root and the `ApplicationComponent` machinery. The existing
`App.onCreate()` work (StrictMode, notification channel, `preferences.migrate()`,
receiver enable/disable, K9MailLib debug hook) is **retained** — Hilt does not replace
`onCreate`; it adds field/constructor injection beneath it. The `new Preferences(this)`
at `App.java:71` becomes an injected `Preferences` field.

> **Coordination note (verified):** `App.java` is a shared file across MU-006 and MU-007
> (migration-units.md Shared-File Allocation). MU-006 removes the `static Bus bus`
> (`App.java:59`) and the `register/unregister/post` pass-throughs (`:116-134`); MU-007
> (this design) adds `@HiltAndroidApp` and converts `new Preferences(this)` to injection.
> Because MU-006 precedes MU-007, by the time this design is implemented the Otto static
> surface is already gone — this design **must not** re-introduce or depend on it.

### Component graph and scope

| Hilt scope | Bound here | Rationale |
|------------|-----------|-----------|
| `@Singleton` (SingletonComponent) | `Preferences`, `AuthPreferences`, `DataTypePreferences`, the four ports (`BackupScheduler`, `MailTransport`, `SecretStore`, `ContactsPort`/`CalendarPort`), `SyncStateRepository` | Process-lived, stateless-or-app-state collaborators. Replaces the per-call `new Preferences(...)` Service-Locator (ARCH-002). One instance, injected everywhere. |
| `@HiltWorker` (assisted, per-work) | `BackupWorker`, `RestoreWorker` | Workers are instantiated by WorkManager, not by the graph directly; Hilt supplies their dependencies via `@AssistedInject` + the `HiltWorkerFactory`. See "Integration with DES-005". |
| Unscoped (`@Inject` constructor, new instance per request) | `MessageConverter`, `MessageGenerator`, `HeaderGenerator`, `PersonLookup`, `ContactAccessor`, `CalendarSyncer`, `BackupItemsFetcher`, `BackupQueryBuilder`, `TokenRefresher`, `OAuth2Client` | Cheap, mostly-stateless conversion/fetch collaborators. No need to cache; constructor-inject and let Hilt build them. These are exactly the types `BackupTask`'s primary constructor `new`s today (verified `BackupTask.java:67-87`). |

### Provider strategy: `@Inject` constructors for app-owned types; `@Module @Provides`/`@Binds` for the ports

Two binding mechanisms are used deliberately:

1. **`@Inject` constructors** for every app-owned concrete collaborator the engine builds
   today. `MessageConverter`, `PersonLookup`, `ContactAccessor`, `BackupItemsFetcher`,
   `BackupQueryBuilder`, `CalendarSyncer`, `TokenRefresher`, `OAuth2Client` each gain an
   `@Inject`-annotated constructor. This is the lowest-ceremony binding and is correct
   because the app owns these types and can annotate them.

2. **`@Module` providers for the ports** (`BackupScheduler`, `MailTransport`,
   `SecretStore`, `SyncStateRepository`, `ContactsPort`, `CalendarPort`). The ports are
   **interfaces** introduced by sibling units (DES-004/005/007/009/011). Their *bindings*
   live here:
   - **`@Binds`** where a single adapter implements a port the app owns
     (e.g., `@Binds BackupScheduler bindBackupScheduler(WorkManagerScheduler impl)` —
     adapter authored by DES-005). `@Binds` is preferred over `@Provides` for
     interface→implementation because it generates no factory and is the idiomatic Hilt
     form for a 1:1 interface binding.
   - **`@Provides`** where construction needs framework objects Hilt cannot inject
     directly — most importantly anything requiring `@ApplicationContext Context`, the
     `WorkManager` instance, `EncryptedSharedPreferences` (the `SecretStore` adapter from
     DES-004), or a `CoroutineDispatcher`. Example: `SecretStore` is `@Provides`-bound
     because `EncryptedPrefsSecretStore` is constructed from a Keystore-derived master key
     and `@ApplicationContext`.

Proposed module layout (Hilt convention — one module per concern, all `@InstallIn(SingletonComponent::class)`):

| Module | Installs | Binds / Provides |
|--------|----------|------------------|
| `SchedulerModule` | SingletonComponent | `@Binds BackupScheduler ← WorkManagerScheduler` (impl from DES-005) |
| `MailModule` | SingletonComponent | `@Binds MailTransport ← K9ImapTransport` (impl from DES-009) |
| `SecretModule` | SingletonComponent | `@Provides SecretStore` from `@ApplicationContext` + Keystore (impl from DES-004) |
| `EventModule` | SingletonComponent | `@Binds SyncStateRepository ← DefaultSyncStateRepository` (impl from DES-007) |
| `ContactsModule` | SingletonComponent | `@Binds ContactsPort ← PeopleApiContactsAdapter` / `@Provides CalendarPort` (impl from DES-011) |
| `PreferencesModule` | SingletonComponent | `@Provides Preferences/AuthPreferences/DataTypePreferences` from `@ApplicationContext` |
| `DispatcherModule` | SingletonComponent | `@Provides @IoDispatcher CoroutineDispatcher = Dispatchers.IO` (for OAuth on `Dispatchers.IO` per target-state) |

> The port **interfaces** are owned by their sibling designs; this design owns only the
> *bindings*. If a sibling adapter is not yet present at implementation time, the
> corresponding module entry is staged behind the coexistence path below (manual binding
> retained until the adapter lands).

### BackupTask / RestoreTask: constructor injection, manual wiring removed

`BackupTask` is converted from "primary constructor that `new`s eight collaborators" to a
type whose collaborators are supplied by the graph. Concretely:

- The **primary constructor** (`BackupTask.java:61-88`) — which builds
  `BackupItemsFetcher`/`BackupQueryBuilder`/`PersonLookup`/`ContactAccessor`/
  `MessageConverter`/`CalendarSyncer`/`TokenRefresher`/`OAuth2Client` by hand — is
  **removed**. Those collaborators become injected dependencies (each carrying its own
  `@Inject` constructor per the provider strategy above).
- The **conditional `CalendarSyncer`** (built only when
  `preferences.isCallLogCalendarSyncEnabled()`, `BackupTask.java:76-86`) is handled by
  injecting a `Provider<CalendarSyncer>` (or a `dagger.Lazy<CalendarSyncer>`) and
  resolving it behind the same runtime guard, so the calendar dependency is **not**
  eagerly constructed when calendar sync is disabled. This preserves the current behavior
  exactly (no `CalendarSyncer` built when the preference is off) while removing the manual
  `new`.
- The **`OAuth2Client(authPreferences.getOAuth2ClientId())`** built inside the
  `TokenRefresher` construction (`BackupTask.java:87`) is replaced by injecting
  `OAuth2Client` and `TokenRefresher` directly; the client-id is resolved by the
  `OAuth2Client` provider from `AuthPreferences`.

`RestoreTask` (single 4-param constructor, verified `:61-70`) becomes injectable as-is —
its dependencies (`MessageConverter`, `ContentResolver`, `TokenRefresher`) are already the
graph's responsibility; no manual `new`-wiring exists to remove.

> **Note on the worker transition.** DES-005 converts `BackupTask`→`BackupWorker` and
> `RestoreTask`→`RestoreWorker` (CoroutineWorkers) and absorbs the foreground lifecycle
> from `SmsBackupService`/`SmsRestoreService`. Whether the injected type at the end of
> Phase 2 is still named `BackupTask` or has become `BackupWorker` is **owned by DES-005**.
> This design's contract is: *whatever the engine entry type is after DES-005, its
> collaborators are constructor-injected and its test-only constructor is gone.* The two
> designs are sequenced (MU-005 before MU-007) precisely so this design injects the
> post-WorkManager shape, not the `AsyncTask` shape.

### Delete the test-only secondary constructor (realizes AC-3)

The 8-parameter `BackupTask(SmsBackupService, BackupItemsFetcher, MessageConverter,
CalendarSyncer, AuthPreferences, Preferences, ContactAccessor, TokenRefresher)`
constructor (`BackupTask.java:90-106`) is **deleted**. Its sole purpose is the test seam;
once the production type is constructor-injected, the seam *is* the constructor, and tests
obtain doubles via the Hilt test harness (see Design Validation). This is the explicit
remediation the ilities assessment names ("Hilt formalizes what the codebase already does
by hand", ARCH-002 remediation).

### `@HiltWorker` for the CoroutineWorkers (realizes AC-4)

Once DES-005 has made backup/restore `CoroutineWorker`s, each is annotated `@HiltWorker`
with an `@AssistedInject` constructor taking `@Assisted Context` + `@Assisted
WorkerParameters` plus its graph-supplied collaborators. The application enables the
`HiltWorkerFactory` by:

- providing it through Hilt (Hilt generates the multibinding), and
- configuring WorkManager's `Configuration.Provider` on `App` (the
  `androidx.hilt:hilt-work` integration) so WorkManager uses the Hilt factory rather than
  the default no-arg worker factory.

This is the mechanism by which workers — which WorkManager instantiates reflectively —
still receive injected dependencies. It is the integration point with DES-005 and is why
AC-4 is phrased as an integration requirement, not a standalone one.

### No `App`-level Service-Locator lookups remain (realizes AC-5)

After this design:

- `App.java:71` `new Preferences(this)` → injected `Preferences`.
- `ServiceBase.java:73,108,112` `new Preferences(...)`/`new AuthPreferences(...)` → injected
  (or, if `ServiceBase` is dissolved into the workers by DES-005, the lookups disappear
  with it).
- No production code path resolves a collaborator by calling a constructor where the graph
  could supply it. The dependency graph becomes **compile-time verified** by Dagger's
  annotation processor — a missing binding is a build failure, not a runtime NPE. This is
  the substantive meaning of AC-5 and is the property `warningsAsErrors` + Dagger together
  enforce.

### Build configuration

Hilt requires the Gradle plugin (`com.google.dagger.hilt.android`) and annotation
processing (`kapt`/`ksp` for the Kotlin-facing modules; Dagger's Java annotation processor
for any remaining Java types). This depends on MU-001 having raised AGP/Gradle and
introduced Kotlin (target-state.md Technology Stack: Hilt 2.50+, requires AGP 8.x). The
`-Werror -Xlint:deprecation` fitness function (verified present in the engagement
constraints) is preserved; Hilt-generated code is excluded from lint as generated sources
per standard configuration.

### Architecture Decision Record — DI mechanism

#### ADR (local): Hilt vs. Koin vs. continued manual DI

**Status:** Accepted (inherits and refines target-state.md ADR-008).

**Context:** No DI container exists. The construction graph is hand-wired
(`BackupTask.java:61-88`), preferences are resolved via a per-call Service-Locator
(`ServiceBase.java:107-113`), and a duplicate test-only constructor (`BackupTask.java:90`)
keeps the seam open by hand. Three options were considered.

**Options:**

| Option | Compile-time safety | Android integration | Runtime cost | Fit |
|--------|--------------------|--------------------|--------------|-----|
| **Hilt (Dagger)** | **Full** — missing binding is a build error | First-party Android DI; `@HiltWorker`, `@HiltAndroidApp`, `@HiltViewModel` | Annotation-processing build cost; ~zero runtime reflection | **Chosen** |
| **Koin** | None — bindings resolved at runtime; missing binding is a runtime crash | Library, not first-party; no `@HiltWorker` equivalent | Runtime service-locator lookups | Rejected |
| **Continued manual DI** | N/A — the status quo | n/a | Verbose; duplicated test constructors | Rejected |

**Decision:** **Hilt.** It is the only option that delivers AC-5's "compile-time verified
dependency graph" — Dagger's processor fails the build on a missing or cyclic binding,
which converts a whole class of runtime DI defects into build defects. It is Android's
first-party DI and provides the exact integrations this engagement needs:
`@HiltWorker` (AC-4, the WorkManager seam from DES-005) and `@HiltAndroidApp` (AC-1). Its
cost — annotation processing at build time — is acceptable and is itself a fitness
function (the build refuses to compile an unsatisfiable graph).

**Rejected — Koin:** Koin resolves bindings at runtime, so a missing binding surfaces as a
runtime crash, not a build failure. That directly contradicts AC-5 ("the dependency graph
is compile-time verified"). Koin also has no first-class `@HiltWorker` analogue, which
would force a hand-rolled `WorkerFactory` and re-introduce exactly the manual wiring this
unit exists to delete. Weaker compile-time safety, worse Android-worker fit.

**Rejected — continued manual DI:** This is the status quo the requirement exists to
remove. It keeps the per-call Service-Locator (ARCH-002), keeps the duplicate test-only
constructor, and offers no compile-time graph verification. Rejected by REQ-008 itself.

**Consequences:** (+) Compile-time-verified graph; deletes the test-only constructor;
removes the Service-Locator; idiomatic worker injection. (−) Annotation-processing build
cost; introduces Dagger/Hilt as a build-time dependency; requires Kotlin/kapt-or-ksp
(already a prerequisite via MU-001).

---

## Integration Design

This unit is almost entirely an **integration** concern: Hilt is the wiring that binds the
ports and collaborators the *other* units author. It therefore depends on, and consumes
the outputs of, several sibling designs. It introduces **no new external integration
surface** of its own.

### Dependency on DES-007 (SyncStateRepository) — the repository becomes injectable

DES-007 replaces the Otto `App.bus` with an app-owned `SyncStateRepository`
(`StateFlow<SyncState>` sticky + `SharedFlow<SyncEvent>` one-shot). This design binds that
repository as a `@Singleton` and **injects** it into the engine entry type(s) and the UI
collectors, replacing the static `App.post(...)` / `App.register(...)` calls verified in
`BackupTask.java:110,235,244,250,255` and `RestoreTask.java:74,192-213`. Sequencing
(MU-006 before MU-007) guarantees the repository interface exists before this design binds
it. **This design consumes DES-007's interface; it does not define it.**

### Dependency on DES-005 (WorkManager / CoroutineWorker) — `@HiltWorker` factory

DES-005 converts the `AsyncTask` engine to `CoroutineWorker`s and introduces the
`BackupScheduler` port + `WorkManagerScheduler` adapter. This design:

- binds `BackupScheduler ← WorkManagerScheduler` (`@Binds`, `SchedulerModule`),
- supplies the `HiltWorkerFactory` and the `WorkManager` `Configuration.Provider` so the
  DES-005 workers are `@HiltWorker`-injected (AC-4),
- injects the workers' collaborators (the same set `BackupTask` builds today).

Sequencing (MU-005 before MU-007) guarantees the workers and the scheduler port exist
before this design injects them. **This design consumes DES-005's worker and port types;
it does not author them.**

### Incremental coexistence: Hilt and manual construction during conversion

Per REQ-008 Constraints and target-state.md Transition Considerations, Hilt and manual
construction **coexist** during the conversion. The mechanism:

1. Introduce `@HiltAndroidApp` and the `SingletonComponent` graph **first**, binding only
   the collaborators whose adapters already exist. Manual `new` sites that have no binding
   yet are left untouched.
2. Convert one collaborator graph at a time — each conversion replaces a `new X(...)` site
   with an injected `X`, behind the existing seam, with **no behavior change**. This is the
   branch-by-abstraction cadence the whole engagement uses (target-state.md Technology
   Principle #1).
3. Delete the `BackupTask` test-only constructor **last**, once every collaborator on the
   primary path is injected, so the test harness migration (below) and the production
   injection land together.

Reversibility: until a `new` site is removed, the manual path is intact; a binding can be
removed and the manual path restored. This matches MU-007's "Coexistence / Cutover:
incremental — inject one collaborator graph at a time" (verified migration-units.md).

### Tests move to the Hilt test harness

The current tests inject Mockito doubles through `BackupTask`'s 8-parameter constructor
(the Humble-Object seam, verified present at `BackupTask.java:90-106` and named in
ilities-assessment.md Testability §). When that constructor is deleted, tests obtain
doubles via the Hilt test harness instead:

- **`@HiltAndroidTest`** on the test classes, with **`HiltAndroidRule`** managing the
  test component lifecycle.
- **`@TestInstallIn`** / **`@BindValue`** to replace production bindings with fakes/mocks
  (e.g., bind a fake `MailTransport`, a fake `SecretStore`, a Mockito `MessageConverter`).
- For worker tests, `TestListenableWorkerBuilder` with the Hilt-supplied factory so
  `@HiltWorker` injection is exercised under Robolectric.

The existing 35-class characterization suite (verified count in ilities-assessment.md) is
the safety net: each engine test is migrated from constructor-injection to harness-injection
with its **assertions unchanged**, so a behavior regression during the DI swap fails an
existing characterization test. This is gated by MU-002 (the test/coverage net) being in
place — a verified prerequisite (migration-units.md Dependency Matrix lists MU-002 as
blocking the refactor units).

---

## Design Validation

This section maps each acceptance criterion of REQ-MODERNIZATION-008 to a verifiable
outcome and the evidence that proves it.

| AC | Requirement | Validation method | Pass condition |
|----|-------------|-------------------|----------------|
| AC-1 | Hilt configured (`@HiltAndroidApp` on `App`, component graph, `@Module` providers) | Source inspection + build | `App` carries `@HiltAndroidApp`; the seven `@InstallIn(SingletonComponent::class)` modules exist; project compiles with the Hilt plugin + kapt/ksp |
| AC-2 | `BackupTask`/`RestoreTask` collaborators constructor-injected; manual `new`-wiring removed | Source inspection | No `new BackupItemsFetcher/PersonLookup/ContactAccessor/MessageConverter/CalendarSyncer/TokenRefresher/OAuth2Client` in the engine entry type; collaborators are constructor params bound by the graph; `CalendarSyncer` injected as `Provider`/`Lazy` behind the `isCallLogCalendarSyncEnabled()` guard |
| AC-3 | Test-only secondary constructor deleted; tests use the Hilt harness | Source inspection + test run | `BackupTask` has exactly one (injected) constructor; no 8-param test constructor; tests use `@HiltAndroidTest` + `@BindValue`/`@TestInstallIn` |
| AC-4 | Workers are `@HiltWorker`-injected | Source inspection + integration test | DES-005 workers carry `@HiltWorker` + `@AssistedInject`; `HiltWorkerFactory` is wired via `Configuration.Provider`; a worker test resolves dependencies through the factory |
| AC-5 | No `App`-level Service-Locator lookups; graph compile-time verified | grep + build | grep for `new Preferences(`/`new AuthPreferences(` in production returns **zero** at the App/ServiceBase layer; Dagger compile fails on any missing binding (compile-time verification demonstrated by a deliberately-removed binding failing the build) |
| AC-6 | Full test suite passes on the Hilt test harness | CI | `./gradlew test` green; coverage gate (MU-002) holds on `service`/`mail`/`auth` |

### Validation invariants

- **Compile-time-verified graph.** The defining property. Dagger's annotation processor
  resolves the entire graph at build time; an unsatisfiable or cyclic graph is a build
  error. This is the mechanical realization of AC-5 and the primary reason Hilt was chosen
  over Koin. Validation: introduce a deliberate missing binding in a throwaway branch and
  confirm the build fails (not the runtime).
- **No App-level Service-Locator lookups.** Validation by grep, per Scope Verification
  doctrine: after implementation, `new Preferences(`/`new AuthPreferences(` must not appear
  in `App.java` or `ServiceBase.java` (current call sites verified this session at
  `App.java:71`, `ServiceBase.java:73,108,112`). Any remaining occurrence is an AC-5
  failure.
- **Test-only constructor deleted.** Validation by source: the 8-param
  `BackupTask` constructor (verified `:90-106`) is absent; the suite still compiles and
  passes via harness injection.
- **Suite green on the Hilt harness.** Validation by CI: the 35-class suite runs under
  `@HiltAndroidTest` with assertions unchanged; a behavior regression fails a
  characterization test.

### Behavior-preservation guarantees (Preserved Core)

This unit must not alter engine behavior (MU-000 Preserved Core). Two source-verified
hazards are explicitly handled:

1. **Conditional CalendarSyncer.** Today `CalendarSyncer` is built only when
   `isCallLogCalendarSyncEnabled()` is true (`BackupTask.java:76-86`); when false it is
   `null` and the backup loop skips calendar sync (`BackupTask.java:282`,
   `cursor.type == CALLLOG && calendarSyncer != null`). Injecting an eager
   `CalendarSyncer` would change this. The design therefore injects
   `Provider<CalendarSyncer>`/`Lazy<CalendarSyncer>` and resolves it behind the same
   guard, preserving both the no-construction-when-disabled behavior and the null-check
   contract.
2. **OAuth retry constructs a fresh store.** The auth-retry path builds a new
   `BackupImapStore` because auth params are immutable
   (`BackupTask.java:189-192`, `RestoreTask.java:163-165`). This is engine behavior owned
   by DES-005/009, not DI; this design must not let injection convert that per-retry
   construction into a cached singleton store. The `MailTransport`/store is bound such that
   the retry path still obtains a *fresh* store (via `Provider`), not the same instance.

---

## Integration Contracts

> This design introduces **no new cross-component contract**. Hilt is the *binding*
> mechanism for ports whose contracts are defined by sibling designs. It **consumes** those
> contracts; it does not produce a new boundary.

| Boundary | Producer | Consumer(s) | Contract Type | CNTR Artifact | Status |
|----------|----------|-------------|---------------|---------------|--------|
| `BackupScheduler` port | DES-MODERNIZATION-005 | DI graph (this design `@Binds` it) | service | (owned by DES-005) | consumed |
| `MailTransport` port | DES-MODERNIZATION-009 | DI graph (this design `@Binds` it) | service | (owned by DES-009) | consumed |
| `SecretStore` port | DES-MODERNIZATION-004 | DI graph (this design `@Provides` it) | service | (owned by DES-004) | consumed |
| `SyncStateRepository` | DES-MODERNIZATION-007 | DI graph (this design `@Binds` it) | service | (owned by DES-007) | consumed |
| `ContactsPort`/`CalendarPort` | DES-MODERNIZATION-011 | DI graph (this design binds them) | service | (owned by DES-011) | consumed |

### Contracts Needed (pre-sprint gate)

- **None new for this design.** The ports this design binds are defined by DES-004, DES-005,
  DES-007, DES-009, and DES-011. The Contract Gate for any story implementing
  DES-MODERNIZATION-008 is satisfied by the *consumed* port contracts being `approved`
  before the corresponding `@Binds`/`@Provides` entry is activated. If a sibling port
  contract is not yet approved at sprint-planning time, the coexistence path (manual
  binding retained) keeps this design buildable, but the binding for that specific port
  must not be claimed "done" until its contract is approved.

---

## Trade-offs

- **Annotation-processing build cost vs. compile-time graph safety.** Hilt adds kapt/ksp
  build time. Accepted: the build-time cost buys compile-time verification (AC-5), which is
  the requirement's core value and the reason Koin was rejected.
- **Sequenced-last vs. earlier adoption.** Adopting Hilt before MU-005/MU-006 would force
  re-authoring `@Module`s as the engine shape changes (AsyncTask→Worker, Bus→repository).
  Accepted: MU-007 depends on MU-005 and MU-006 (verified) so the graph is bound once,
  against its final shape.
- **`Provider`/`Lazy` for the conditional CalendarSyncer vs. eager injection.** Eager
  injection is simpler but would construct `CalendarSyncer` even when calendar sync is
  disabled, changing verified behavior. Accepted: `Provider`/`Lazy` preserves behavior at
  the cost of slightly more annotation ceremony.

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Implementer chases a non-existent `RestoreTask` second constructor (REQ/MU framing) | Medium | Low | This design records the verified divergence: `RestoreTask` has a single injected constructor (`:61-70`); only `BackupTask` has the dual/test constructor (`:90`) |
| Eager `CalendarSyncer` injection changes calendar-disabled behavior | Medium | Medium | Inject `Provider`/`Lazy` behind the existing `isCallLogCalendarSyncEnabled()` guard |
| Injecting a singleton store breaks the immutable-auth retry path | Low | Medium | Bind the store as a `Provider` so the retry path obtains a fresh store (preserves `BackupTask.java:189-192` behavior) |
| Sibling port adapter not ready when binding is authored | Medium | Low | Coexistence path: retain manual binding until the adapter (DES-004/005/007/009/011) lands |
| `@HiltWorker` factory not wired → workers crash at instantiation | Low | High | AC-4 validation requires a worker test that resolves dependencies through `HiltWorkerFactory`; `Configuration.Provider` on `App` is mandatory, not optional |
| kapt/ksp + `-Werror` interaction blocks build on generated code | Low | Low | Exclude Hilt-generated sources from lint (standard config); MU-001 already established the toolchain |

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| REQ-MODERNIZATION-008 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-008-introduce-hilt-dependency-injection.md | Governing requirement; 6 ACs, constraints, boundary |
| Target State (ADR-008) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/target-state.md | ADR-008 (Hilt), component graph, ports, technology stack, transition strategy |
| Migration Units (MU-007) | sdlc/analysis/.../modernization/requirements/migration-units.md | MU-007 scope, injected-collaborator list, dependency edges (MU-005/MU-006 precede), shared-file allocation for App.java |
| Ilities Assessment (ARCH-001/002) | sdlc/analysis/.../architecture/ilities-assessment.md | Service-Locator (ARCH-002), Humble-Object test seam, Testability rationale |
| App.java | app/src/main/java/com/zegoggles/smssync/App.java | VERIFIED: `extends Application` (:52), `new Preferences(this)` (:71), static Bus (:59), register/post (:116-134) |
| service/BackupTask.java | app/src/main/java/com/zegoggles/smssync/service/BackupTask.java | VERIFIED: hand-wired primary ctor (:61-88), conditional CalendarSyncer (:76-86), 8-param test ctor (:90-106), App.post/register (:110,235,244,250,255), immutable-auth retry (:189-192) |
| service/RestoreTask.java | app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java | VERIFIED: single 4-param injected ctor (:61-70) — NO dual/test ctor; App.post/register (:74,192-213); immutable-auth retry (:163-165) |
| service/ServiceBase.java | app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java | VERIFIED: Service-Locator `new Preferences`/`new AuthPreferences` (:73,108,112); `getBackupImapStore()` constructs store (:99-105) |
| DES-MODERNIZATION-005 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md | Consumed: CoroutineWorker + BackupScheduler port (interface owner) — not re-read this session; referenced via MU-007/target-state |
| DES-MODERNIZATION-007 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-007-syncstate-repository-design.md | Consumed: SyncStateRepository (interface owner) — not re-read this session; referenced via MU-006/target-state |

## Notes

**Verification honesty (truth-and-accuracy).** App.java, BackupTask.java, RestoreTask.java,
and ServiceBase.java were read in full this session and every source claim above is grounded
in that read. REQ-008, target-state.md, migration-units.md, and ilities-assessment.md were
read in full. The sibling design documents **DES-005 and DES-007 were not re-read in this
session** (tool channel stalled on the second read batch); this design therefore references
their *interfaces as consumed contracts* — sourced from target-state.md and migration-units.md,
which were read in full — and explicitly does **not** assert anything about their internal
content. `SmsBackupService.java`/`SmsRestoreService.java` bodies (which instantiate the tasks)
were likewise not re-read this session; the worker/service transition is correctly scoped to
DES-005 ownership rather than asserted here. An implementing story should re-verify the
sibling port interfaces against DES-004/005/007/009/011 at build time before activating each
`@Binds`/`@Provides` entry.
