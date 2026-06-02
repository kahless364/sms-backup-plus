# Modernization Plan: SMS Backup+

**Assessment ID:** 20260529-modernization  
**Subject:** sms-backup-plus — `app` module (`com.zegoggles.smssync`)  
**Date:** 2026-05-29  
**Report Version:** 1.0  
**Authors:** Solution Architect + Technical Writer (assessment agents)  
**Audience:** Technical Leadership  
**All source paths are relative to repo root** (`C:/Code/Android/sms-backup-plus`)

---

## Executive Summary

### Vision

SMS Backup+ will evolve from a structurally sound application sitting on a decade-old rotted substrate into a modern, Google Play-shippable, secure-by-default Android backup engine. The app's strongest assets — its immutable state machine, typed-exception hierarchy, Open/Closed DataType design, and 35-class test suite — are preserved verbatim. The work is substrate replacement: WorkManager replaces the abandoned Firebase JobDispatcher and deprecated AsyncTask; Kotlin StateFlow replaces the archived Otto event bus; EncryptedSharedPreferences closes the plaintext credentials exposure; validated TLS replaces a critical MITM-enabling trust-bypass; and the SDK/build toolchain is raised to meet Google Play's current requirements.

### Key Findings

- **Current state:** 107 production Java files, ~10,635 LOC across a clean layered architecture. Three of six core runtime dependencies are abandoned or deprecated (Otto archived 2016, Firebase JobDispatcher deprecated 2018, k-9 mail pinned to a JitPack git SHA). `targetSdk 29` is below the Google Play update floor and **the app cannot ship any update today.** Quality ratings: Portability 🔴, Security 🔴, Maintainability 🟡, Reliability 🟡. The Security rating is driven by an active Critical MITM vulnerability and plaintext credential storage on an app holding the user's entire SMS corpus and Gmail access credentials.

- **Target state:** `targetSdk`/`compileSdk` 35; AGP 8.x; WorkManager as the single background-execution platform; Kotlin StateFlow for reactive state; Hilt DI; Jetpack Security EncryptedSharedPreferences; validated TLS by default; k-9 IMAP library behind a versioned/vendored Anti-Corruption Layer. Quality ratings: Portability 🟢, Security 🟢, Maintainability 🟢, Reliability 🟢.

- **Gap magnitude:** 18 gaps across four categories (2 Critical, 8 High, 6 Medium, 2 Low). Technology gaps dominate; structural gaps are additive (introduce ports), not demolitive (no redesign required). There is exactly one hard gate — `targetSdk` — that blocks almost every other gap closure from shipping.

### Recommended Approach

**Uplift-in-place via Branch-by-Abstraction at dependency seams.** The application's macro-architecture is sound and is retained. The modernization replaces the dead substrate behind the business logic through four incremental waves, each independently shippable. This is not a rewrite: the characterization safety net (35-class test suite, JaCoCo gating) is the mechanism that makes incremental seam-by-seam migration safe. A full rewrite is estimated at 3–5× the investment with materially worse risk and zero incremental user value.

### Investment Summary

| Category | Estimate |
|----------|----------|
| Effort (non-elective spine, Phases 0–2) | ~6–9 engineer-weeks (1 senior Android engineer) |
| Effort (Phase 3 strategic hardening, elective) | ~4+ additional engineer-weeks |
| Infrastructure (CI, dependency verification) | Near-zero marginal cost (GitHub Actions free tier for OSS) |
| Training | Minimal — Kotlin/coroutines/WorkManager/Hilt are standard Jetpack; abundant documentation |
| Total (non-elective) | ~6–9 person-weeks; approximately 2–3 calendar months at maintenance cadence |

> Basis: ~10,635 prod LOC; 12 Otto-importing files; 4 JobDispatcher-importing files; 3 AsyncTask subclasses; 36 existing test files as the safety net; two dependency seams (`Driver` Strategy, `BackupTask` dual constructor) already half-built by the existing team.

### Timeline

| Phase | Focus | Duration |
|-------|-------|----------|
| Phase 0: Gate + Safety Net | SDK/AGP/Gradle uplift; jcenter removal; CI pipeline; JaCoCo | ~2–3 weeks |
| Phase 1: Security-by-Default (parallel to Phase 0) | Trust-all TLS removal; EncryptedSharedPreferences; log redaction | ~2–3 weeks (overlaps) |
| Phase 2: Substrate Swap | WorkManager; Otto → Flow; Hilt DI; k-9 ACL; durable restore checkpoint | ~4–6 weeks |
| Phase 3: Strategic Hardening (elective) | Kotlin migration; Billing 7.x; k-9 coordinate unpin; People API; observability | ~4+ weeks |

---

## Current State Summary

### Architecture

SMS Backup+ is a single-process Android monolith organized as a pragmatic three-tier layered architecture: Presentation (`activity*`) / Engine (`service*`) / Data-Integration (`mail`, `auth`, `preferences`, `contacts`, `calendar`). The application has no project-owned server component. All data flows between the device's SMS/MMS/CallLog/Calendar ContentProviders and a user-supplied IMAP mailbox.

**Strengths worth preserving:**

- **Immutable State machine** (`service/state/State.java`, `BackupState`, `RestoreState`, `SmsSyncState` enum with 11 states): transitions are pure functions `(SmsSyncState, Exception) → State`, thread-safe by construction, graded reference-quality (5/5).
- **`DataType` type-object enum** (`mail/DataType.java`): each constant (`SMS`/`MMS`/`CALLLOG`) carries its own configuration profile and behavior; adding a new data type is one enum entry. Textbook Open/Closed design (5/5).
- **Typed, localizable exception hierarchy** (`service/exception/`): 8 typed exception classes feeding 25 locale translations.
- **Disciplined preferences facade** (`preferences/Preferences.java`, `AuthPreferences.java`): centralized `enum Keys`, typed accessors; no `SharedPreferences` reached directly outside the facade.
- **35-class Robolectric/JUnit test suite** covering all major packages, with a deliberately engineered Humble-Object DI seam (`BackupTask.java:90` second constructor).

**Dominant liability — the dead-substrate cluster:**

The application's `AsyncTask` workers (deprecated API 30, removed API 33), Firebase JobDispatcher scheduler (deprecated 2018, served only from an unofficial `maven.scijava.org` mirror), and Square Otto event bus (archived 2014) are three obsolete primitives forming one coupled problem. Together they represent the dominant modernization driver, rated by the assessment as "one problem wearing three hats." A single WorkManager + StateFlow migration retires all three.

The second architectural liability is the process-global `App.bus` static Otto singleton and `private static` service back-references in `SmsBackupService` and `SmsRestoreService`. These are Service-Locator and Singleton anti-patterns that create invisible untyped coupling across all layers, make the engine untestable without static rigging, and carry a Context-leak hazard.

```
                        Current Architecture (abridged)
────────────────────────────────────────────────────────────────
  TRIGGERS                               UI (activity*)
  BroadcastReceivers ─────────────────── MainActivity
  SmsJobService (Firebase) ──────────┐   └─────── Otto bus (static global)
                                      │             ↕ BackupState / RestoreState
  SCHEDULER                           │   ENGINE (service*)
  BackupJobs ──── GooglePlayDriver    └── SmsBackupService
               └─ AlarmManagerDriver       └── BackupTask (AsyncTask) ← deprecated
                    (EOL, scijava mirror)       └── MessageConverter → BackupImapStore
                                                    └── k-9 ImapStore (SHA pin)
────────────────────────────────────────────────────────────────
```

### Technical Debt

The codebase carries three categories of modernization-blocking debt:

**Critical / Distribution-blocking:**
- `targetSdk 29` is below Google Play's update floor (API 34 since August 2024). No update can reach users through the primary distribution channel.
- Firebase JobDispatcher dependency hosted solely on `maven.scijava.org` — a scientific-computing Maven mirror with no Google affiliation. Build reproducibility is contingent on an unreliable third-party host.
- k-9 mail library pinned to git SHA `eaf689025e` via JitPack — no semantic version, unreproducible if JitPack evicts the cache, unpatched against upstream k-9/Thunderbird security fixes.

**High / Runtime correctness:**
- `PendingIntent` constructed without `FLAG_IMMUTABLE` at `AlarmManagerDriver.java:127` and `ServiceBase.java:204` — `IllegalArgumentException` crash on API 31+ the moment `targetSdk` is raised.
- `AsyncTask` in 3 worker classes — deprecated API 30, removed API 33, each carrying `@SuppressLint("StaticFieldLeak")` as an explicit acknowledgment of a known memory-leak hazard.
- `ConnectivityManager.getNetworkInfo()` deprecated in API 29, removed in API 31.
- `AllTrustedSocketFactory` — active Critical security vulnerability (see Security section).

**Medium / Forward-policy risks:**
- Play Billing 2.1.0 is 5 major versions behind; the `SkuDetails` API it uses is deprecated and Play will enforce ≥ 6.x.
- jcenter() declared three times in `build.gradle` after the service was shut down in May 2022.
- 29 `@SuppressWarnings`/`@SuppressLint` accumulations from `-Werror` brittleness — symptom of managed suppression rather than root-cause remediation.

**Debt heatmap — highest-concentration files:**

| File | Debt Items | Priority |
|------|------------|----------|
| `service/BackupTask.java` | CD-001 (AsyncTask), AD-004 (StaticFieldLeak) | Rewrite as CoroutineWorker |
| `service/BackupJobs.java` + `SmsJobService.java` | firebase-jobdispatcher, AD-002 (lifecycle bypass) | Replace with WorkManager |
| `mail/AllTrustedSocketFactory.java` | CD-003 (TLS bypass) | Delete |
| `preferences/AuthPreferences.java` | Silent migration (ARCH-008), plaintext creds (ARCH-009) | Rewrite migrate(), encrypt credentials |
| `app/build.gradle` | targetSdk 29, billing 2.1.0, jcenter | Immediate uplift |

### Quality Assessment

| Attribute (ISO 25010) | Rating | Key Driver |
|-----------------------|--------|------------|
| Portability / Compatibility | 🔴 Red | 3 dead deps; targetSdk 29 below Play floor |
| Security | 🔴 Red | Trust-all TLS (Critical, CWE-295); plaintext credentials (High, CWE-312) |
| Maintainability | 🟡 Yellow | Otto Singleton anti-pattern; no DI container; AsyncTask engine |
| Reliability | 🟡 Yellow | No durable restore checkpoint; coarse failure classification |
| Performance Efficiency | 🟡 Yellow | O(mailbox) restore scan; 18:1 `commit()`:`apply()` ratio |
| Testability | 🟢 Green | 35 test classes; engineered Humble-Object seams |
| Observability | 🟡 Yellow | Custom AppLog adequate; no metrics/crash reporting; inconsistent redaction |
| Usability | 🟢 Green | 25 locales; AndroidX Preference UI; localizable exceptions |

---

## Target State

### Target Architecture

The target retains the layered monolith macro-structure and inserts **Hexagonal ports** at every external dependency boundary, so dead/risky libraries sit behind app-owned interfaces. One unified background-execution platform (WorkManager) replaces three obsolete primitives. One unified reactive state channel (StateFlow/SharedFlow) replaces the archived Otto bus.

```
                        Target Architecture
────────────────────────────────────────────────────────────────
  PRESENTATION (activity*)
  MainActivity + MainViewModel  ←── collect StateFlow (lifecycle-aware)
  Auth / Donation (Billing 7.x)
                │
  SyncStateRepository  (StateFlow<SyncState> + SharedFlow<SyncEvent>)
                ↕
  ENGINE (service*)
  BackupWorker : CoroutineWorker (WorkManager, setForeground)
  RestoreWorker : CoroutineWorker (durable checkpoint)
       │  Immutable State machine PRESERVED
       │
  PORTS (app-owned interfaces)
  BackupScheduler  MailTransport  SecretStore  ContactsPort  CalendarPort
       │               │              │
  ADAPTERS
  WorkManagerScheduler  K9ImapTransport (+ACL)  EncryptedPrefsSecretStore
       │
  WorkManager ← single scheduler (subsumes JobScheduler + AlarmManager)
  k-9 (versioned/vendored)
  Jetpack Security (AES-256, Keystore)
────────────────────────────────────────────────────────────────
```

**Communication patterns:**

| Pattern | Use Case | Mechanism |
|---------|----------|-----------|
| Reactive sticky state | Engine → UI current status | `StateFlow<SyncState>` (replaces Otto `@Produce`) |
| Reactive one-shot events | Cancel, terminal errors | `SharedFlow<SyncEvent>` |
| Command / trigger | UI/receiver → engine | `BackupScheduler` port → WorkManager enqueue (REPLACE policy) |
| Port call | Engine → external system | App-owned interface; adapter behind it |

### Technology Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Background execution | WorkManager 2.9.x+ | Subsumes JobScheduler (≥API 23) and AlarmManager fallback; owns worker lifecycle and foreground promotion; replaces 3 obsolete primitives in one migration (ADR-001) |
| Event/state channel | Kotlin `StateFlow` + `SharedFlow` | Lifecycle-aware; `StateFlow.value` reproduces Otto `@Produce` sticky semantics; removes abandonware dependency (ADR-003) |
| Dependency injection | Hilt (Dagger) | Formalizes existing Humble-Object dual-constructor seams; removes Service-Locator pattern; deletes test-only constructors (ADR-008). Rejected: Koin (weaker compile-time safety) |
| Credential storage | Jetpack Security `EncryptedSharedPreferences` | AES-256, Android Keystore master key; single `SecretStore` port seam (ADR-007). CWE-312 closure |
| TLS trust | Validated TLS only; user-pinned cert for self-hosted IMAP | Remove `AllTrustedSocketFactory`; fix silent migration that enables trust-all (ADR-007). CWE-295 closure |
| k-9 IMAP client | Versioned coordinate or vendored module behind `MailTransport` ACL | Closes JitPack SHA reproducibility risk; Anti-Corruption Layer insulates domain State from library internals (ADR-004) |
| SDK / Platform | `compileSdk`/`targetSdk` 35, `minSdk` 21 | Play floor is 34; minSdk 21 eliminates much of the `compat/Pre40` shim surface (ADR-006) |
| Build toolchain | AGP 8.x / Gradle 8.x (staged 4.1.3 → 7.4 → 8.x) | Required for compileSdk 34+, R8, configuration cache (DEP-005/006) |
| Language | Kotlin (incremental) carrier for WorkManager/Flow/Hilt | Kotlin-first Jetpack APIs; domain core preserved verbatim; staged migration, not big-bang (ARCH-020 explicit) |
| Otto event bus replacement | Kotlin StateFlow/SharedFlow | Removes archived dependency; compile-time-typed events. Rejected: Greenrobot EventBus (perpetuates global-bus anti-pattern, adds rather than removes a dependency) |

### Quality Targets

| Attribute | Current | Target | Gap Priority |
|-----------|---------|--------|--------------|
| Portability / Compatibility | 🔴 Red | 🟢 Green | Critical — the gate |
| Security | 🔴 Red | 🟢 Green | Critical — start day-1 |
| Maintainability | 🟡 Yellow | 🟢 Green | High |
| Reliability | 🟡 Yellow | 🟢 Green | Medium |
| Performance Efficiency | 🟡 Yellow | 🟡→🟢 Green | Medium (power users) |
| Testability | 🟢 Green (hand-built) | 🟢+ Green (DI-formalized) | Low |
| Observability | 🟡 Yellow | 🟡→🟢 Green | Low |

---

## Gap Analysis Summary

### Finding Counts by Severity

Aggregated across the three findings-format outputs (`assessment.md` — 12 findings, `gaps.md` — 18 findings, `candidates.md` — 8 strangler-candidate findings):

| Output | Critical | High | Medium | Low | Total |
|--------|:--------:|:----:|:------:|:---:|:-----:|
| `assessment.md` (Step 2.1) — modernization-decision findings | 2 | 6 | 3 | 1 | **12** |
| `gaps.md` (Step 2.3) — gap findings | 2 | 8 | 6 | 2 | **18** |
| `candidates.md` (Step 3.1) — strangler suitability findings | 0 | 3 | 4 | 1 | **8** |

> Note: findings across outputs overlap by design — gaps.md derives from assessment.md; candidates.md re-projects the gap seams. The canonical gap set is the 18 findings in `gaps.md`.

### Critical Gaps

| Gap ID | Gap Name | Category | Effort | Blocks |
|--------|----------|----------|:------:|--------|
| TECH-001 / DEBT-001 | targetSdk 29 → 35; AGP/Gradle uplift | Technology | L | TECH-002, TECH-003, TECH-005, STRUCT-001, CAP-001/002, and all shipping |
| SEC-001 / ARCH-003 | Trust-all TLS + silent migration enabling MITM | Security | M | Parallel — no SDK dependency |

### High-Severity Gaps

| Gap ID | Gap Name | Category | Effort |
|--------|----------|----------|:------:|
| TECH-002 / ARCH-001 | Firebase JobDispatcher + AsyncTask + AlarmManager → WorkManager | Technology | L |
| TECH-003 / ARCH-002 | Square Otto 1.3.8 → Kotlin StateFlow/SharedFlow | Technology | M |
| TECH-004 / DEP-004 | Robolectric 4.3.1 (blocks SDK bump) + Play Billing 5 majors behind | Technology | M |
| TECH-005 / DEP-002 | jcenter() declared after 2022 shutdown | Technology | S |
| ARCH-001 / STRUCT-001 | No Hexagonal ports at dependency boundaries | Structural | L |
| ARCH-002 / STRUCT-002 | Process-global Otto singleton — no reactive state spine | Structural | M |
| CAP-001 / ARCH-006 | No durable restore checkpoint; O(mailbox) IMAP scan | Capability | L |
| OPS-001 / PROC-001 | No CI/CD in repository | Process | M |
| OPS-002 / PROC-002 | Test coverage unmeasured (0.33:1 LOC); no fitness function | Process | M |

### Dependencies

The dependency topology has one unconditional gate and two parallel fast-start tracks:

```
Day 1 parallels (unblocked by gate):
  SEC-001 (trust-all TLS removal) ──────────────────────────────────┐
  OPS-001 (CI setup) → OPS-002 (JaCoCo gate) ───────────────────────┤
                                                                      │
  TECH-001 (SDK gate) → TECH-004 (test stack co-dep)                 │
       ↓ unblocks                                                     │
  STRUCT-001 (Hexagonal ports) ← requires gate + coverage gate       │
       ↓                                                             │
  TECH-002 (WorkManager) ← requires gate + ports + coverage gate     │
       ↓                                                             │
  TECH-003/STRUCT-002 (Otto→Flow) ← after WorkManager               │
       ↓                                                             │
  CAP-001 (durable checkpoint) ← after WorkManager ──────────────────┘
```

**Critical path:** TECH-001 (SDK gate) → STRUCT-001 (ports) → TECH-002 (WorkManager) → CAP-001 (durable checkpoint).

**Independent tracks:** SEC-001, OPS-001/002, and the server-bounding half of CAP-001 have no upstream dependencies and start day-1.

---

## Strategy

### Approach

**Branch-by-Abstraction (in-process Strangler Fig analog) at every dependency seam.** The application retains its identity, layering, and domain logic. The strategy is:

1. Introduce an **app-owned port interface** in front of each dead/risky dependency (making the existing implementation the "first adapter" with zero behavior change — reversible and independently shippable).
2. Add the **new adapter** (WorkManager, StateFlow, EncryptedSharedPreferences, etc.) compiling side-by-side with the old.
3. **Flip the binding** via DI — a single change, instantly reversible.
4. **Delete the EOL dependency** from `app/build.gradle`.

This sequence makes every swap independently shippable, characterization-test-gated, and reversible. The "Replace" happens at the dependency layer; the calling code experiences a "Refactor" of bounded scope.

The rewrite decision for `AsyncTask` workers (`BackupTask`, `RestoreTask`, `OAuth2CallbackTask`) is the one legitimate exception: `AsyncTask` is *removed* at API 33, so there is no incremental refactor path from a deleted superclass. These three execution-shell classes (not the orchestration logic they call) are rewritten as `CoroutineWorker` — mechanical, gated by existing engine tests.

**Do not rewrite the application:** A full rebuild would discard the 35-class characterization safety net, the correct immutable State design, the hard-won k-9 IMAP edge-case knowledge encoding years of carrier/encoding behavior, and the 25-locale translation surface — for zero user-visible feature gain. A rewrite is estimated at 3–5× the uplift investment with materially worse risk.

### Strangler Candidates

Eight in-process module seams were scored across six suitability factors (interface clarity, coupling, data isolation, change frequency, business value, risk level):

| Component (CAND-ID) | Suitability Score | Effort | Value | Recommended Wave |
|---------------------|:-----------------:|:------:|:-----:|:----------------:|
| CAND-001: Scheduler/Worker → WorkManager | **88** (Excellent) | L | High | Wave 1 (facade) → Wave 2 (full swap) |
| CAND-003: Secret store → EncryptedSharedPreferences | **84** (Excellent) | M | Med | Wave 3 |
| CAND-005: Play Billing 2.1.0 → 7.x | **80** (Excellent, low priority) | M | Low | Wave 4 (elective) |
| CAND-008: Transport security — delete trust-all TLS | **74** (Good) | M | Critical security | **Wave 1, day-1** |
| CAND-002: Event spine → StateFlow/SharedFlow | **72** (Good) | M | Med | Wave 3 |
| CAND-004: Mail/k-9 ACL + `MailTransport` port | **70** (Good) | M | Med | Wave 3 |
| CAND-006: Contacts → People API | **66** (Good, low priority) | M | Low | Wave 4 (elective) |
| CAND-007: Hilt DI (enabling seam) | **52** (Moderate) | M | Med | Wave 4 |

**Not recommended for strangling (preserved verbatim):**
- Immutable `State` machine — it is the protected core the ports insulate.
- `DataType` type-object enum — exemplary Open/Closed design with no vendor coupling.
- Typed `Preferences` facade — already a clean boundary; only the credential accessor (CAND-003) is carved out.

**Highest-leverage finding:** CAND-001 (Scheduler) is the flagship candidate — and the existing `Driver` Strategy (`BackupJobs.java:66-72`) already implements the "first adapter" slot. The `BackupScheduler` port is 50% built by the existing codebase. Introducing the port is mechanical and free; the full WorkManager swap is the highest-leverage single migration in the engagement.

### Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|:----------:|------------|
| `targetSdk` bump surfaces background-execution / FGS-type / notification-permission regressions | High | High | Stage 29→31→33→34/35; JaCoCo fitness function gates each step; `FLAG_IMMUTABLE` co-landed with gate |
| WorkManager lifecycle semantics differ from JobDispatcher → missed/duplicate scheduled backups | High | Medium | Characterization-test retry `30/300` and network constraints before swap; Parallel-Run the new scheduler in debug build before cut-over |
| scijava/JitPack outage breaks CI mid-modernization | High | Medium | Phase 0 removes jcenter + adds `verification-metadata.xml`; Phase 2 WorkManager migration retires the scijava mirror entirely |
| Removing trust-all TLS breaks genuine self-signed-IMAP users | Medium | Medium | Explicit user-pinned-cert path + one-time migration notice; ADR documented |
| k-9 has no API-compatible released coordinate; vendoring required | Medium | Medium | `MailTransport` ACL makes either path swappable; vendor as fallback (ADR-004) |
| Coverage unmeasured — refactors regress silently | High | Medium | JaCoCo + coverage fitness function lands in Phase 0 *before* any refactor; backfill `MainActivity`/`OAuth2Client` tests |
| Single maintainer / maintenance-mode — modernization stalls | Medium | Medium | Phase 0 alone restores Play eligibility + de-risks supply chain; each phase is independently shippable |
| `-Werror -Xlint:deprecation` blocks build mid-migration | Low | High | Relax to `lint-baseline.xml` during transition; restore `-Werror` once deprecated APIs are eliminated |

---

## Roadmap

### Phase Overview

```
Calendar        Month 1          Month 2          Month 3+         Month 6+
────────────────────────────────────────────────────────────────────────────────
Phase 0:        ████████████
Gate + Safety   SDK/AGP/Gradle
Net             CI + JaCoCo

Phase 1:        ████████████████
Security-by-    Trust-all TLS
Default         EncryptedPrefs
(parallel)

Phase 2:                         ████████████████████████████████
Substrate                        Hexagonal Ports
Swap                             WorkManager + CoroutineWorker
                                 Otto → StateFlow
                                 Hilt DI + k-9 ACL
                                 Durable Checkpoint

Phase 3:                                                          ██████████████
Strategic       (Elective)                                        Kotlin migration
Hardening                                                         Billing 7.x
                                                                  k-9 unpin
                                                                  People API
────────────────────────────────────────────────────────────────────────────────
```

### Phase 0: Foundation — Gate + Safety Net (Weeks 1–3)

**Objective:** Restore Play Store shippability and stand up the quality gate that protects all subsequent refactors.

| Initiative (MU) | Scope | Effort | Dependencies |
|-----------------|-------|:------:|:-------------|
| MU-01: SDK/AGP/Gradle uplift | compileSdk/targetSdk → 35; minSdk → 21; AGP 4.1.3→7.4→8.x; Gradle 8.x; `buildTools` 34.x; `FLAG_IMMUTABLE` co-land; `android:exported` on all receivers; `POST_NOTIFICATIONS`; `FOREGROUND_SERVICE_DATA_SYNC` | L | None upstream — **the gate** |
| MU-05 (test co-dep): Upgrade test toolchain | Robolectric 4.3.1 → 4.12.x (must move with SDK); JUnit 4.12 → 4.13.2; mockito-all → mockito-core 5.x; Truth 0.39 → 1.4.x | M | Co-required with MU-01 |
| MU-03: CI + dependency verification | GitHub Actions (build + `./gradlew test` + lint baseline + assembleRelease with R8); `gradle/verification-metadata.xml`; remove jcenter() + scijava entries from `build.gradle` | M | Parallel — day 1 |
| MU-04: JaCoCo coverage fitness function | `jacocoTestReport` + coverage gate ≥ 70% on `service`/`mail`/`auth`; backfill characterization tests for `AuthPreferences.migrate()`/`getStoreUri()` and `BackupJobs` retry/constraints | M | After MU-03; must precede all Phase 2 refactors |
| MU-11 (partial): Dead-code sweep | Remove `CalendarAccessorPre40` (unreachable at minSdk 21); implement `StatusPreference` save/restore stubs; sweep compat shims made dead by minSdk raise | S | With MU-01 minSdk work |
| Enable R8 | `minifyEnabled true` + `shrinkResources true` + k-9/reflection keep-rules; validate with backup smoke test | S | After MU-01 |

**Success criteria:**
- A signed AAB with `targetSdk 35` passes Play pre-launch review
- CI runs on every PR; `./gradlew test` green
- `service`/`mail`/`auth` coverage ≥ 70%
- jcenter() and scijava mirror removed from all build.gradle files

### Phase 1: Security-by-Default (Weeks 1–5, parallel to Phase 0)

**Objective:** Close the two active security findings — both are independent of the SDK gate.

| Initiative (MU) | Scope | Effort | Dependencies |
|-----------------|-------|:------:|:-------------|
| MU-02: Transport security hardening | Delete `AllTrustedSocketFactory`; replace with validated TLS; add user-pinned-cert path for self-hosted IMAP; rewrite `AuthPreferences.migrate()` to preserve validated TLS with a one-time user notice; centralize log-redaction helper; gate all credential-adjacent logging on `BuildConfig.DEBUG` | M | **None — parallel to Phase 0** |
| MU-04 (partial): Characterization tests | Backfill `AuthPreferencesTest` covering `migrate()`, `getStoreUri()`, and the trust-factory selection before modifying these files | M | Parallel with MU-02; required gate |
| MU-08: EncryptedSharedPreferences | Migrate `getCredentials()` seam to `SecretStore` port backed by `EncryptedSharedPreferences` (Jetpack Security, AES-256, Keystore master key); preserve `credentials.xml` backup exclusion; one-time credential migration | M | Eased by MU-01 (Keystore ergonomics at minSdk 21+) |

**Success criteria:**
- No code path accepts an unvalidated TLS certificate (`AllTrustedSocketFactory` deleted)
- `migrate()` verified by test to never silently set `SERVER_TRUST_ALL_CERTIFICATES=true`
- Credentials AES-256 encrypted at rest; `credentials.xml` still excluded from Android auto-backup
- No account-email PII (or token material) appears in release logs — credential-adjacent logging gated behind BuildConfig.DEBUG (token is already masked; confirmed by test)

### Phase 2: Substrate Swap (Weeks 5–11)

**Objective:** Replace the three dead platform primitives; migrate the event spine; introduce DI; add the durable restore checkpoint.

| Initiative (MU) | Scope | Effort | Dependencies |
|-----------------|-------|:------:|:-------------|
| MU-06: Hexagonal ports | Define `BackupScheduler`, `MailTransport`, `SecretStore`, `ContactsPort`, `CalendarPort` interfaces; make existing implementations first adapters (zero behavior change); route engine through ports — additive structure only | L | MU-01 + MU-04 |
| MU-07: WorkManager migration | `BackupScheduler` → `WorkManagerScheduler`; `BackupTask`/`RestoreTask` → `CoroutineWorker`; `OAuth2CallbackTask` → coroutine; delete firebase-jobdispatcher, AlarmManagerDriver, SmsJobService, scijava mirror; preserve BACKUP broadcast contract; map scheduling contract line-for-line (REPLACE policy, 30s backoff, UNMETERED constraint, content-URI triggers) | L | MU-01 + MU-06 |
| MU-06 (Otto): Event spine → Flow | Wrap `App.bus` behind injected `EventBus`/`SyncStateRepository` facade (no behavior change); swap implementation to `StateFlow<SyncState>` (sticky, replaces Otto `@Produce`) + `SharedFlow<SyncEvent>` (one-shot); fold static `isService*()` queries into repository; fold in `MainActivity` → `MainViewModel` extraction | M | MU-07 (avoid engine double-churn) |
| MU-08 (Mail): k-9 ACL | Wrap `BackupImapStore` behind `MailTransport` port with Anti-Corruption Layer (translate `MessagingException` → app-owned `LocalizableException`; remove `State.java:33` magic-string match; no `com.fsck.k9.*` type crosses the boundary); pin k-9 to versioned coordinate or vendor the IMAP subset | M | MU-06 + MU-07 (both touch `BackupImapStore.java`) |
| MU-07 (Hilt): DI formalization | Adopt Hilt `@HiltAndroidApp`; constructor-inject `Preferences`, ports, and engine collaborators; delete test-only duplicate constructors | M | MU-07 + MU-06-Flow (construction graph settled) |
| MU-10 (partial): Durable restore checkpoint | Persist restore cursor in WorkManager work-data; idempotent SMS-provider writes keyed on message identity; transient/permanent failure classification with bounded retry-with-jitter at the task layer | M | MU-07 (WorkManager durable work state) |
| MU-10 (search bounding — parallel): Server-side IMAP search | Replace `UID SEARCH 1:*` + in-memory sort with server-side bounding (`SORT`/`SINCE`/UID windowing); `commit()` → `apply()` for progress writes | L | Independent — can parallelize |

**Success criteria:**
- firebase-jobdispatcher, Square Otto removed from `app/build.gradle`
- Third-party `com.zegoggles.smssync.BACKUP` broadcast still triggers a backup
- All four scheduling-contract invariants pass (REPLACE semantics, 30/300 backoff, UNMETERED constraint, content-URI trigger with broadcast fallback below API 24)
- No `com.fsck.k9.*` type crosses `MailTransport`; `StateTest` passes without magic-string match
- Restore is resumable after process kill with no duplicate or lost messages

### Phase 3: Strategic Hardening (Weeks 12+, elective)

**Objective:** Language modernization, remaining dependency upgrades, and observability.

| Initiative (MU) | Scope | Effort | Notes |
|-----------------|-------|:------:|:------|
| Kotlin migration | Staged; `State` machine and `DataType` first (promote to Kotlin `sealed interface`/enum); WorkManager/Flow seams already in Kotlin from Phase 2; remaining Java at discretion | L | Kotlin rides with seams; never a standalone big-bang |
| MU-09: Play Billing 7.x | Rewrite `DonationActivity`, `DonationListFragment`, `Sku.java` to use `ProductDetails`/`queryProductDetailsAsync` | M | Isolated to 3 files; low risk |
| MU-11 (k-9 unpin): Resolve SHA pin | Confirm k-9 to a maintained versioned coordinate or complete vendoring behind the `MailTransport` ACL from Phase 2 | L | ACL (Phase 2) makes either choice swappable |
| MU-10 (Contacts): People API | Replace legacy GData `m8/feeds/` call in `OAuth2Client.java:145` with People API adapter behind `ContactsPort`; **evaluate GData endpoint liveness before committing** | M | OAuth2 contacts/calendar path must remain intact |
| Opt-in crash reporting | Privacy-respecting, ADR-documented, off-by-default | M | Any telemetry is opt-in; documented explicitly |
| MU-05 (billing): Billing 7.x | (See above — same initiative as MU-09) | — | — |

### Key Milestones

| Milestone | Target | Success Criteria |
|-----------|--------|-----------------|
| M0: Play Eligibility Restored | End of Phase 0 (Week 3) | Signed AAB with `targetSdk 35` published to Play Store |
| M1: Critical Security Closed | End of Phase 1 (Week 5) | `AllTrustedSocketFactory` deleted; credentials encrypted; no release-log token leakage |
| M2: Supply-Chain De-risked | End of Phase 2 (Week 9) | firebase-jobdispatcher and scijava mirror removed; builds resolve from google()/mavenCentral() only |
| M3: Substrate Swap Complete | End of Phase 2 (Week 11) | Otto removed; WorkManager scheduling; durable restore checkpoint; k-9 ACL |
| M4: Strategic Hardening | Phase 3 (Month 4+) | Kotlin migration initiated; Billing 7.x deployed; k-9 coordinate pinned |

---

## Resource Requirements

### Team

The modernization is sized for **one senior Android engineer** at a maintenance cadence. Each phase is independently shippable, which means the engagement can absorb interruption without losing progress.

| Phase | Duration | Skills Emphasis |
|-------|----------|-----------------|
| Phase 0 (Gate + Safety Net) | ~2–3 weeks | Android build systems; AGP upgrade path; Gradle; manifest/permission audit; GitHub Actions CI |
| Phase 1 (Security) | ~2–3 weeks (parallel) | Android TLS/Keystore APIs; Jetpack Security; credential migration patterns |
| Phase 2 (Substrate Swap) | ~4–6 weeks | WorkManager; Kotlin coroutines; StateFlow; Hilt; IMAP/ACL patterns |
| Phase 3 (Hardening) | ~4+ weeks | Kotlin migration; Play Billing 7.x; Google People API |

### Skills

**Required now (Phases 0–2):**
- Android Gradle Plugin upgrade path (4.x → 7.x → 8.x)
- WorkManager lifecycle model (`CoroutineWorker`, `setForeground`, content-URI triggers)
- Kotlin coroutines and Kotlin Flow (`StateFlow`, `SharedFlow`)
- Hilt dependency injection (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@Inject`)
- Jetpack Security (`EncryptedSharedPreferences`, Android Keystore)
- Android manifest conformance for API 31–34 (`android:exported`, FGS types, notification permissions)
- Feathers' characterization-test technique (*Working Effectively with Legacy Code*, Ch. 2/13)

**Gaps / learning curve:**
- Kotlin coroutines — moderate learning curve for Java-only background; abundant official documentation and codelabs
- WorkManager — well-documented replacement for JobScheduler; the existing scheduling contract (constraints, backoff, content triggers) maps line-for-line to WorkManager equivalents (documented in `target-state.md §Scheduling Contract Mapping`)

### Infrastructure

| Item | Notes |
|------|-------|
| CI pipeline | GitHub Actions (free for OSS); replaces defunct Travis CI badge |
| Gradle dependency verification | `gradle/verification-metadata.xml` — generated by `./gradlew --write-verification-metadata` |
| JaCoCo coverage gate | Gradle plugin; configured as a fitness function with a per-package threshold |
| Code signing | Existing `keystore.properties` mechanism — no change |
| Distribution | Google Play (AAB) + F-Droid (APK reproducible build) — no change |
| Crash reporting (Phase 3, opt-in) | Firebase Crashlytics or Sentry — evaluated at Phase 3; must be ADR-documented as opt-in |

---

## Governance

### Decision Gates

| Gate | Timing | Decision | Owner |
|------|--------|----------|-------|
| G0: Play Eligibility | End of Phase 0 | Signed AAB with targetSdk 35 passes Play pre-launch review; all Phase 0 CI checks green | Maintainer |
| G1: Security Cleared | End of Phase 1 | ARCH-007/008/009 findings closed; no trust-all code path; credentials encrypted | Maintainer |
| G2: Substrate Swap Entry | Before Phase 2 | JaCoCo coverage ≥ 70% on service/mail/auth; characterization tests for BackupJobs and AuthPreferences passing | Maintainer |
| G3: WorkManager Flip | Within Phase 2, before deleting JobDispatcher | BackupJobs characterization assertions pass against WorkManagerScheduler; public BACKUP broadcast tested; no scheduling regression in 48h on-device soak | Maintainer |
| G4: Otto Removal | Within Phase 2, after WorkManager flip | All 12 Otto-importing files migrated; `com.squareup.otto` removed from `app/build.gradle`; sticky last-state behavior verified | Maintainer |
| G5: Phase 3 Entry | Optional, at maintainer discretion | Phases 0–2 complete; Kotlin readiness assessed | Maintainer |

### Architecture Decision Records

Eight ADRs were documented in `target-state.md` and govern the key decisions:

| ADR | Decision | Status |
|-----|----------|--------|
| ADR-001 | WorkManager as single background-execution platform | Accepted |
| ADR-002 | Content-URI triggers → WorkManager content constraints; broadcast fallback below API 24 | Accepted |
| ADR-003 | Otto → StateFlow/SharedFlow behind SyncStateRepository; rejected Greenrobot EventBus | Accepted |
| ADR-004 | Hexagonal ports + Anti-Corruption Layer at mail/k-9 boundary | Accepted |
| ADR-005 | Durable restore checkpoint + idempotent provider writes | Accepted |
| ADR-006 | compileSdk/targetSdk 35; minSdk 21 | Accepted |
| ADR-007 | EncryptedSharedPreferences for credentials; remove trust-all TLS path | Accepted |
| ADR-008 | Hilt DI; rejected Koin | Accepted |

### Success Metrics

| Metric | Current | Target | Measurement |
|--------|---------|--------|-------------|
| Deployment Frequency | Blocked (Play rejects targetSdk 29) | On-demand | Play Console release cadence post-Phase-0 |
| Lead Time (commit→shippable) | Unmeasured (no CI) | < 1 day | CI pipeline duration |
| Change Failure Rate | Unmeasured | < 15% | CI test-fail rate per merge |
| Abandoned dependencies | **3** (Otto, JobDispatcher, k-9 SHA) | **0** | `app/build.gradle` dep manifest audit |
| targetSdk vs. Play floor | **29 (6 levels below)** | ≥ 35 | `app/build.gradle` |
| Measured engine coverage (service/mail/auth) | **Unknown** (0.33:1 LOC) | ≥ 70% line/branch | JaCoCo `jacocoTestReport` in CI |
| Trust-all TLS code path | **1 (active)** | **0** | `grep AllTrustedSocketFactory` removed |
| Supply-chain mirrors | **2** (scijava + JitPack SHA pin) | **0** | Repository block audit + `verification-metadata.xml` |
| Restore checkpoint (data integrity) | None | Durable, idempotent | Test: interrupted restore produces zero duplicates |
| Account-email PII in release debug log (token already masked) | Present (`OAuth2Client.java:145`) | **0** | `BuildConfig.DEBUG` guard verified by CI lint |

---

## Recommendations

1. **Start Phase 0 (SDK gate) and Phase 1 (trust-all TLS removal) in parallel today.** Phase 0 is the unconditional prerequisite for shipping anything to the Play Store. Phase 1 (specifically the trust-all TLS removal and `migrate()` fix) has no SDK dependency — every day the active MITM exposure on users' message corpora is unaddressed is a day of real, remediable risk.

2. **Establish the JaCoCo coverage fitness function before touching any seam.** The 35-class test suite is the safety net for every subsequent refactor. Its adequacy is currently *unmeasured* (0.33:1 LOC ratio). Refactoring an unmeasured codebase is the single largest self-inflicted risk in this engagement. The coverage gate must land in Phase 0, before Phase 2's substrate swaps begin.

3. **Treat the WorkManager migration as the highest-leverage single engineering investment.** One coherent migration (CAND-001) retires Firebase JobDispatcher, `AsyncTask`, the `AlarmManagerDriver` fallback, the `maven.scijava.org` supply-chain mirror, and the `SmsJobService` lifecycle bypass simultaneously, and it unblocks the durable restore checkpoint. The existing `Driver` Strategy at `BackupJobs.java:66–72` is already 50% of the port abstraction this migration requires.

4. **Preserve the domain core verbatim.** The immutable `State` machine, `DataType` type-object enum, typed exception hierarchy, and `Preferences` facade are reference-quality assets. They are not modernization targets; they are the foundation the modernization builds on. The one permitted touch to the preserved core is `State.java:33` — the magic-string k-9 match — which is owned by the `MailTransport` Anti-Corruption Layer cleanup in Phase 2.

5. **Sequence Kotlin as a carrier, not a driver.** WorkManager, StateFlow, and Hilt are Kotlin-first APIs and Kotlin will naturally enter the codebase with Phase 2. A standalone Kotlin big-bang is not warranted and would double-churn the engine. Kotlin follows the seams; the seams do not wait for Kotlin.

6. **Evaluate the Gmail REST API as a long-term product decision (Phase 3+), not an engineering one.** The current IMAP app-password path works but requires a poor user experience (manual password generation). Whether to move to the Gmail REST API for programmatic access is a product scope decision that is out of scope for the technical modernization and should be assessed separately against user impact.

7. **Consider vendoring the k-9 IMAP library as a build-reproducibility safety measure.** The `MailTransport` ACL (Phase 2, MU-08) makes the k-9 binding swappable, but until a verified versioned coordinate is confirmed, vendoring a minimal IMAP subset is the safest path to reproducible builds on a privacy-critical app.

---

## Next Steps

**Immediate (this week):**
1. Begin the Phase 1 security track: characterization-test `AuthPreferences.migrate()` and `getStoreUri()`, then delete `AllTrustedSocketFactory` and rewrite the migration logic.
2. Open a GitHub Actions workflow for the CI pipeline (`./gradlew test` + lint baseline).
3. Remove the three `jcenter()` and `jcenter.bintray.com` declarations from `build.gradle` — a 30-minute fix with immediate build-stability benefit.

**Short-term (Month 1):**
1. Complete the Phase 0 SDK/AGP/Gradle gate: raise compile/target SDK to 35, minSdk to 21; co-land `FLAG_IMMUTABLE` fix; update all manifest `android:exported` attributes; bump AGP incrementally (4.1.3 → 7.4 → 8.x).
2. Upgrade the test toolchain (Robolectric 4.12.x, JUnit 4.13.2, mockito-core 5.x) as a co-dependency of the SDK bump.
3. Wire JaCoCo and establish the ≥70% coverage fitness function on `service`/`mail`/`auth`.
4. Complete Phase 1 EncryptedSharedPreferences migration at the `getCredentials()` seam.

**Medium-term (Months 2–3):**
1. Introduce the four Hexagonal port interfaces with existing implementations as first adapters (zero behavior change, reversible).
2. Execute the WorkManager migration (the highest-leverage single move in the engagement): characterization-test the scheduling contract first, build `WorkManagerScheduler` and `CoroutineWorker`, flip the binding, delete firebase-jobdispatcher and the scijava mirror.
3. Migrate the Otto event spine to `SyncStateRepository`/`StateFlow`/`SharedFlow`, folding in `MainActivity` → `MainViewModel` extraction.
4. Add the durable restore checkpoint via WorkManager work-data and idempotent provider writes.

---

## Appendix

### A: Detailed Work Breakdown

Full migration-unit specifications are in:
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md` — MU-001 through MU-011 with file-by-file scope, coexistence/cutover steps, and dependency edges.
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/prioritized-backlog.md` — WSJF scoring, additive-formula cross-check, and topological ordering for MU-01 through MU-11.

### B: Architecture Decision Records

Full ADR text (Context/Decision/Consequences/Rejected Alternatives) for ADR-001 through ADR-008 is in:
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/target-state.md` — §Architecture Decision Records section.

### C: Risk Register

Full risk analysis with likelihood/impact/mitigation across all 18 gap findings is in:
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/gaps.md` — §Risk Assessment section.
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/assessment.md` — §Risk Analysis section.

### D: Strangler Candidate Scoring Detail

Full six-factor suitability scoring for all 8 CAND-NNN candidates with per-factor rationale is in:
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/candidates.md`

### E: Requirements Extracted for Rewrite-Scope Components

Phase 2.5 requirements extraction artifacts (for UI and non-visual components targeted for rewrite):
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/code-classification.md`
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/ui-requirements/` (SCR-MAIN-001, SCR-SP-001, SCR-ADV-001, SCR-DON-001)
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/non-visual-specs/non-visual-specs.md`
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/dead-code-candidates.md`
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/tribal-knowledge-gaps.md`

### F: Source Analysis Artifacts (Phase 1)

- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/discovery/project-overview.md` — technology stack, architecture, key observations KO-01..10
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/discovery/file-inventory.md` — 107 production files, 10,635 LOC, complexity hotspots
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/discovery/dependencies.md` — DEP-001..010 findings; supply-chain risks; license summary
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/architecture/ilities-assessment.md` — ISO 25010 ratings; ARCH-001..021 findings; WSJF priority table
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/architecture/patterns.md` — design pattern inventory; ARCH-001..004 anti-patterns; target decision matrix
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/tech-debt/debt-inventory.md` — CD/AD/TD/DD/IF debt items; heatmap; remediation budget
