# Target Architecture Definition — SMS Backup+

**Assessment:** 20260529-modernization
**Phase / Step:** EXECUTE — Step 2.2 (Target Architecture Definition)
**Subject:** P01 `app` — `com.zegoggles.smssync` (single Android module)
**Date:** 2026-05-29
**Architect:** Solution Architect (assessment agent)
**Version:** 1.0
**Quality bar:** `knowledge/standards/expert-exceeding-depth.md`
**All paths relative to repo root** (`C:/Code/Android/sms-backup-plus`).

---

## Executive Summary

### Vision Statement

SMS Backup+ should evolve from a *structurally sound application sitting on a rotted platform substrate* into a **modern, Play-Store-shippable, secure-by-default Android backup engine** — preserving its strongest assets (the immutable state-machine, the `DataType` type-object, the typed-exception hierarchy, the 35-class characterization-test suite) verbatim while **swapping the dead substrate underneath them**: WorkManager replaces `AsyncTask` + Firebase JobDispatcher + the AlarmManager fallback as one unified background-execution platform; Kotlin `StateFlow`/`SharedFlow` behind an app-owned event boundary replaces the archived Otto bus and the static service back-references; AndroidX/AGP/Gradle/`targetSdk` uplift restores Play eligibility; the Anti-Corruption Layer at the `mail` boundary contains k-9 version risk; and `EncryptedSharedPreferences` plus removal of trust-all TLS close the security-by-default gap. This is **modernization by branch-by-abstraction at dependency seams, not a rewrite** — the business-logic boundaries are not the migration boundaries; the *dependency* boundaries are.

### Key Transformations

| From (Current) | To (Target) | Business Value |
|----------------|-------------|----------------|
| `targetSdk`/`compileSdk` 29, AGP 4.1.3, Gradle 7.2 (`app/build.gradle:10,15`; `build.gradle:8`) | `targetSdk`/`compileSdk` 35, AGP 8.x, Gradle 8.x | **Restores Google Play distribution eligibility** — the app cannot ship a single update today (Play floor is API 34+ since Aug 2024) |
| Firebase JobDispatcher 0.8.6 (deprecated, scijava-mirror-hosted) + `AlarmManagerDriver` fallback (`app/build.gradle:60`; `BackupJobs.java:25–45`) | WorkManager (`androidx.work`) — single scheduler, no fallback branch | Eliminates a Critical supply-chain dependency (DEP-002) and the `isUseOldScheduler` dual-path maintenance burden |
| `BackupTask`/`RestoreTask`/`OAuth2CallbackTask` extend `AsyncTask` (deprecated API 30) + service-as-helper hack (`BackupTask.java:50`; `SmsJobService.java:82–84`) | `CoroutineWorker` under WorkManager; OAuth on `Dispatchers.IO` | Removes deprecated primitive, serial-executor contention, and the lifecycle-bypass reliability hazard (ARCH-004) |
| Square Otto 1.3.8 process-global `Bus` singleton + `static` service back-refs (`App.java:59`; `SmsBackupService.java:66`) | App-owned `SyncStateRepository` exposing `StateFlow`/`SharedFlow`, DI-injected | Removes archived dependency (DEP-003); eliminates Context-leak hazard; makes engine unit-testable without static rigging |
| Trust-all TLS path + silent migration enabling it (`AllTrustedSocketFactory.java:42–57`; `AuthPreferences.java:276–288`) | Validated TLS only; opt-in user-pinned cert for self-hosted IMAP | Closes a **Critical** MITM exposure on the user's entire message corpus (CWE-295) |
| Credentials in plaintext `SharedPreferences` (`AuthPreferences.java:221–226`) | `EncryptedSharedPreferences` (Jetpack Security, Keystore-backed) | Closes cleartext-secret-at-rest defect (CWE-312) |
| k-9 mail library pinned to git SHA `eaf689025e` via JitPack (`app/build.gradle:58`) | Versioned/vendored k-9 behind an app-owned `MailTransport` port (ACL) | Reproducible builds; insulates domain from library churn |
| Java 8, no Kotlin, no DI container | Kotlin (incremental), Hilt DI, formalized Humble-Object seams | Carrier language for WorkManager/Flow; deletes test-only duplicate constructors |
| Google Play Billing 2.1.0 (`SkuDetails`, deprecated) (`app/build.gradle:59`) | Play Billing 7.x (`ProductDetails`/`queryProductDetailsAsync`) | Restores donation flow before Google retires the old API |

### Timeline Horizon

- **Near-term (0–3 months) — "Ship-eligibility + security gate":** Remove `jcenter()`; raise SDK/AGP/Gradle; make the four `<receiver>` elements `android:exported`-explicit; remove trust-all TLS + fix the silent migration; migrate credentials to `EncryptedSharedPreferences`; gate credential-adjacent debug logging behind `BuildConfig.DEBUG` (stop logging account-email PII — token already masked); add characterization-test gating (JaCoCo). These are the entry conditions for everything else.
- **Mid-term (3–9 months) — "Substrate swap":** WorkManager migration (replaces JobDispatcher + AsyncTask + AlarmManager); Otto → `StateFlow`/`SharedFlow` behind `SyncStateRepository`; Hilt DI; Anti-Corruption Layer at the `mail`/k-9 boundary; Play Billing 7.x; durable restore checkpoint enabled by WorkManager state.
- **Long-term (9–18 months) — "Hardening + ergonomics":** Incremental Kotlin migration (state machine and `DataType` ported first); server-side bounding/sorting of the restore search (the only true scaling cliff); People API evaluation for contacts; optional Gmail API path evaluation; opt-in privacy-respecting crash reporting; `minSdk` rationalization (21/23) to retire the `compat`/`Pre40` surface.

---

## Architectural Drivers

### Business Drivers

| Driver | Priority | Description | Success Metric |
|--------|----------|-------------|----------------|
| Restore Play Store shippability | **High** | App at `targetSdk 29` is rejected by Play (floor API 34+ since Aug 2024). No update has reached users via the primary channel. | A signed release with `targetSdk ≥ 34` passes Play pre-launch review and publishes |
| Preserve the maintainer's free/OSS, privacy-first posture | High | README declares maintenance mode; users trust it precisely because it is on-device, no-backend, no-telemetry. | Zero new mandatory network endpoints; any telemetry is opt-in and documented in an ADR |
| Keep existing users working through migration | High | 25 localisations, third-party `com.zegoggles.smssync.BACKUP` integration, existing preference/credential data on-device. | Upgrade preserves settings; no silent security downgrade; the public BACKUP broadcast contract is retained |
| Eliminate supply-chain fragility | Medium | Build depends on a scijava mirror for JobDispatcher and a JitPack SHA for k-9 (`build.gradle:27`; `app/build.gradle:58`). A mirror outage breaks the build. | Clean CI build resolves only from `google()`/`mavenCentral()`/pinned-and-verified coordinates |

### Technical Drivers (with Quality Attribute Scenarios — Bass/Clements/Kazman format)

| Driver (ISO 25010) | Priority | Description | Success Metric |
|--------|----------|-------------|----------------|
| **Portability / Compatibility** | High | Three dead deps (Otto, JobDispatcher, SHA-pinned k-9) + sub-floor `targetSdk`. The dominant modernization driver (ilities-assessment §Portability 🔴). | All three dead deps retired; `targetSdk ≥ 34`; build resolves with no mirror/SHA dependencies |
| **Security** | High | Trust-all TLS (Critical, ARCH-007), silent migration that enables it (ARCH-008), plaintext credentials (ARCH-009), token logging (ARCH-010). | No trust-all code path; credentials encrypted at rest; no secret in release logs; STRIDE-Tampering/Info-Disclosure mitigations in place |
| **Maintainability** | High | Otto Singleton + static service refs + no DI + `AsyncTask` engine (ARCH-001/002/003). | Engine unit-testable without static rigging; DI graph explicit; deprecated primitives removed |
| **Reliability** | Medium | No durable restore checkpoint (ARCH-013); coarse `catch(MessagingException)→ERROR` (ARCH-014); 10-min wakelock cap truncates large backups. | Restore resumes after process death; transient/permanent failures classified and retried differentially |
| **Performance Efficiency** | Medium | `UID SEARCH 1:*` full-folder scan + in-memory sort before truncation (ARCH-016); per-message `commit()` fsync (ARCH-017). | Restore candidate-set bounded server-side; progress writes use `apply()` |

**Quality Attribute Scenario — Background reliability (Reliability/Recoverability):**

| Element | Value |
|---|---|
| Source | OS background-execution limits / Doze / process death (OOM, user swipe, wakelock expiry) |
| Stimulus | A scheduled regular backup of a large mailbox is interrupted mid-run |
| Artifact | The backup/restore worker |
| Environment | Device in Doze, on metered/unmetered network per constraint, target API 34 background limits |
| Response | WorkManager persists work state and the data-type `maxSyncedDate` checkpoint; on next opportunity the worker resumes from the last checkpoint; restore writes are idempotent (dedupe on message identity) |
| Response Measure | No duplicate restored messages; backup resumes within one WorkManager retry window; ≤ 0 data loss |

**Quality Attribute Scenario — Transport security (Security/Confidentiality+Integrity):**

| Element | Value |
|---|---|
| Source | Active network attacker (hostile Wi-Fi / on-path) |
| Stimulus | Presents a forged/self-signed certificate during the IMAP TLS handshake |
| Artifact | `MailTransport` adapter (k-9-backed `BackupImapStore`) |
| Environment | User on untrusted network; default configuration |
| Response | TLS chain validation rejects the forged cert; connection aborts; for explicitly self-hosted IMAP, only a user-pinned cert/CA is trusted |
| Response Measure | 0 successful MITM; no code path accepts an unvalidated cert; no silent migration enables trust-all |

### Constraints

| Constraint | Type | Impact | Mitigation |
|------------|------|--------|------------|
| `minSdk 14` (ICS) and 25 localisations (`app/build.gradle:14`; `res/values-*`) | Technical | Large `compat`/`Pre40` shim surface; WorkManager artifacts and `EncryptedSharedPreferences` ergonomics assume API 21/23+; string changes ripple across 25 locales | WorkManager supports minSdk 14; raise `minSdk` to 21 (eliminates much `compat`) deliberately with an ADR; minimize user-facing string churn |
| `warningsAsErrors true` + `-Werror -Xlint:deprecation` (`app/build.gradle:41,75`) | Technical | Any newly-introduced deprecation fails the build mid-migration | Stage migrations so deprecations are removed, not added; keep `-Werror` as a fitness function for deprecation debt |
| No project-owned backend; entirely on-device (systems.md) | Architectural | No server-side seam to modernize; cannot move work off-device; no central observability | Target stays a layered on-device monolith; observability via exportable `AppLog` + opt-in crash reporting only |
| Gmail policy: XOAuth2 cannot write to Gmail IMAP since Jun 2019; app-password required (endpoints.md; `BUGS.md`) | Regulatory/External | Primary backup auth path is app-password; OAuth2 retained only for contacts/calendar | Keep app-password IMAP as the supported backup path; evaluate (not assume) Gmail REST API as a long-term option; do not break the OAuth2 contacts/calendar path |
| Single-maintainer, maintenance-mode OSS (README) | Resource | No appetite for a rewrite; effort must be incremental and reversible | Branch-by-abstraction at each seam; the 35-class test suite is the safety net; every step independently shippable |
| Apache 2.0 license (COPYING) | Regulatory | All target deps must be license-compatible | All proposed deps (WorkManager, Hilt, Jetpack Security, Billing, k-9) are Apache 2.0 / Android SDK License — compatible (dependencies.md License Summary) |

---

## Target Architecture Overview

### Architecture Style

**Primary Style:** **Layered on-device monolith** (3-tier: Presentation / Engine / Data-Integration) with a **Hexagonal (Ports & Adapters) seam at every external dependency boundary**, and a **reactive state spine** (`StateFlow`/`SharedFlow`) replacing the Otto Observer bus.

**Rationale (trade-off explicit):** For an app whose entire "backend" is the device itself (no project-owned server — systems.md), microservices/SOA/event-sourcing would be **accidental complexity** (Golden Hammer). The current layered monolith is the *correct* macro-architecture and is well-factored (patterns.md graded it "not a Big Ball of Mud"). The modernization problem is **vendor/platform obsolescence wrapped around a sound core**, so the target *keeps the macro-style* and changes only the substrate. The one structural addition is **explicit ports** (`BackupScheduler`, `MailTransport`, `EventBus`/`SyncStateRepository`, `SecretStore`) so the dead/risky dependencies sit behind app-owned interfaces — this is what makes each swap mechanical, reversible, and independently testable (Hexagonal architecture, Cockburn; branch-by-abstraction, Fowler). The alternative — a Kotlin Multiplatform / Clean-Architecture ground-up rewrite — was rejected: it discards the 35-class characterization safety net, multiplies risk against a single maintainer, and is not justified by any business driver (no new feature demand; README is explicit maintenance mode).

**Key Characteristics:**
- Unchanged macro-layering; the package map (`activity`/`service`/`mail`/`auth`/`preferences`) is preserved.
- Each external-system coupling becomes an explicit **port + adapter** so vendor risk is contained at the boundary (ACL/Hexagonal).
- One unified background-execution platform (**WorkManager**) replaces the three concurrency/scheduling primitives.
- One unified, lifecycle-aware **reactive state channel** (`StateFlow` sticky + `SharedFlow` one-shot) replaces Otto, its `@Produce` sticky semantics, and the static "is running" queries.
- **Secure-by-default**: validated TLS only, encrypted secrets, redacted logging — applied as a *policy*, not per-call discretion.

### High-Level Target Architecture Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                          SMS Backup+  (single on-device app)               │
│                              TARGET ARCHITECTURE                            │
├───────────────────────────────────────────────────────────────────────────┤
│  PRESENTATION  (activity*)                                                  │
│   ┌──────────────┐  ┌───────────────┐  ┌──────────────┐                     │
│   │ MainActivity │  │ Auth screens  │  │ Donation     │                     │
│   │ + ViewModel  │  │ (OAuth2/AcctMgr)│ │ (Billing 7.x)│                     │
│   └──────┬───────┘  └──────┬────────┘  └──────┬───────┘                     │
│          │ collect StateFlow / SharedFlow (lifecycle-aware)                 │
│   ┌──────▼─────────────────────────────────────────────────────┐           │
│   │  SyncStateRepository  (app-owned)                           │           │
│   │   • StateFlow<SyncState>  (sticky — replaces Otto @Produce)  │           │
│   │   • SharedFlow<SyncEvent> (one-shot — cancel, errors, toasts)│           │
│   └──────▲───────────────────────────────────────────▲─────────┘           │
│          │ emit                                       │ emit                 │
│  ENGINE  (service*)                                   │                      │
│   ┌──────┴───────────────┐   ┌───────────────────┐   │                      │
│   │ BackupWorker         │   │ RestoreWorker     │   │                      │
│   │  (CoroutineWorker,   │   │ (CoroutineWorker, │   │                      │
│   │   setForeground)     │   │  durable checkpt) │   │                      │
│   └──────┬───────────────┘   └─────────┬─────────┘   │                      │
│          │  uses immutable State machine (PRESERVED)  │                      │
│          │  ┌──────────────────────────────────────┐ │                      │
│          │  │ SyncState (sealed) ← State/BackupState│ │                      │
│          │  │ /RestoreState/SmsSyncState (preserved)│ │                      │
│          │  └──────────────────────────────────────┘ │                      │
│   ┌──────▼─────────────────────────────────────────────────────┐           │
│   │  PORTS (app-owned interfaces) — Hexagonal boundary          │           │
│   │  ┌──────────────┐ ┌─────────────┐ ┌──────────┐ ┌──────────┐ │           │
│   │  │BackupScheduler│ │ MailTransport│ │SecretStore│ │Contacts/ │ │           │
│   │  │  (port)       │ │  (port, ACL) │ │ (port)   │ │CalendarPort│           │
│   │  └──────┬───────┘ └──────┬──────┘ └────┬─────┘ └────┬─────┘ │           │
│   └─────────┼────────────────┼─────────────┼────────────┼───────┘           │
│  DATA / INTEGRATION (adapters)│             │            │                    │
│   ┌─────────▼──────┐ ┌────────▼─────────┐ ┌─▼────────┐ ┌─▼──────────────┐    │
│   │WorkManager     │ │ k-9 ImapStore    │ │Encrypted │ │ContentResolver  │   │
│   │Scheduler adapter│ │ adapter (+ACL:   │ │SharedPrefs│ │ + People API/   │   │
│   │(work-runtime)  │ │ typed exceptions)│ │(Keystore)│ │ Calendar provider│  │
│   └────────┬───────┘ └────────┬─────────┘ └──────────┘ └────────┬────────┘    │
│            │                  │                                 │             │
│  CROSS-CUTTING: Hilt DI · redaction policy · StrictMode · AppLog + opt-in    │
│                 crash reporting · WorkManager observability (WorkInfo)        │
└────────────┼──────────────────┼─────────────────────────────────┼───────────┘
             │ IMAPS 993         │ HTTPS                            │ IPC
        ┌────▼─────┐        ┌────▼──────────────┐            ┌──────▼─────────┐
        │ IMAP     │        │ Google OAuth2     │            │ Android SMS/MMS/│
        │ (Gmail/  │        │ token + People API│            │ CallLog/Calendar│
        │ custom)  │        │ (contacts/calendar)│           │ ContentProviders│
        └──────────┘        └───────────────────┘            └────────────────┘
```

### Target Background-Execution Flow (replacing JobDispatcher + AsyncTask)

```
Trigger (SMS_RECEIVED / BOOT / BACKUP broadcast / manual / contentUriTrigger)
        │
        ▼
BackupScheduler (port)  ──adapter──▶  WorkManager
        │                               │  • OneTimeWorkRequest (immediate / broadcast)
        │                               │  • PeriodicWorkRequest (regular)
        │                               │  • Constraints: NetworkType.UNMETERED|CONNECTED
        │                               │  • BackoffPolicy.EXPONENTIAL (preserve 30s base)
        │                               │  • content-URI observation via Constraints
        ▼                               ▼
   enqueue uniquely (REPLACE)     BackupWorker : CoroutineWorker
                                        │ setForeground(notification)  ← owns FG promotion
                                        │ runs immutable State machine
                                        │ setProgress(BackupState)  → SyncStateRepository
                                        ▼
                                   Result.success / retry() / failure()
```

---

## Component Architecture

### Presentation Layer (`activity*`)

| Component | Purpose | Technology | Notes |
|-----------|---------|------------|-------|
| `MainActivity` + `MainViewModel` | Settings UI; manual backup/restore trigger; status display | Kotlin/Java + AndroidX `ViewModel` + `PreferenceFragmentCompat` 1.2.x | Decompose the 499-LOC God-Activity-tendency (ARCH-004): move state to `ViewModel`, dialogs to `Dialogs`, collect `StateFlow` via `repeatOnLifecycle` |
| Auth activities (`OAuth2WebAuthActivity`, `AccountManagerAuthActivity`, `RedirectReceiverActivity`) | OAuth2 (contacts/calendar) + AccountManager + redirect handling | AndroidX | OAuth2 retained for contacts/calendar only (Gmail-write path is app-password) |
| `DonationActivity` + `DonationListFragment` + `Sku` | In-app donation | **Play Billing 7.x** (`ProductDetails`, `queryProductDetailsAsync`) | Migrate off deprecated `SkuDetails` (DEP-008); isolated to 3 files |
| `StatusPreference` | Live status row | Collects `SyncStateRepository.StateFlow` | Replaces Otto `@Subscribe`; lifecycle-aware |

**Key Decisions:** UI never calls down into the engine directly (preserve the current discipline — patterns.md "0 direct downward calls"); it triggers via `BackupScheduler`/Intent and observes via the reactive repository. The `@Produce` "sticky last state" capability is preserved by `StateFlow.value`, not lost.

### Engine / Service Layer (`service*`)

| Component | Domain | Responsibilities | Dependencies |
|-----------|--------|------------------|--------------|
| `BackupWorker` (was `BackupTask` + `SmsBackupService`) | Backup orchestration | Off-main execution via `CoroutineWorker`; `setForeground` for long runs; checkpoint per `DataType` | `MailTransport`, content cursors, `SyncStateRepository`, `SecretStore` |
| `RestoreWorker` (was `RestoreTask` + `SmsRestoreService`) | Restore orchestration | Durable restore checkpoint (ARCH-013); idempotent provider writes | `MailTransport`, `SyncStateRepository` |
| `SyncState` machine (preserve) | Domain state | Pure `(SmsSyncState, Exception) → State` transitions; immutable | none (language-portable) |
| `BackupScheduler` (new port) + `WorkManagerScheduler` (adapter) | Scheduling | `scheduleRegular/scheduleIncoming/scheduleContentTrigger/scheduleImmediate/cancelAll` | WorkManager |
| `BackupItemsFetcher` / `BackupQueryBuilder` / `BackupCursors` (preserve) | Data access | ContentResolver cursor reads | Android providers |

**Service Boundaries:** Determined by **data ownership and external-system coupling**, not by team or deployment (there is one process). The engine owns the sync state and the orchestration; each external system (scheduler, mail, secrets, contacts/calendar) is reached only through its port. This collapses the current `SmsBackupService ↔ SmsRestoreService` static bidirectional coupling (ARCH-001) into queries against the injected `SyncStateRepository`.

**Communication Patterns:**

| Pattern | Use Case | Mechanism |
|---------|----------|-----------|
| Reactive sticky state | Engine → UI current status | `StateFlow<SyncState>` (replaces Otto `@Produce`) |
| Reactive one-shot events | Cancel, terminal error, transient toast | `SharedFlow<SyncEvent>` (replaces transient Otto events) |
| Command/trigger | UI/receiver → engine | `BackupScheduler` port → WorkManager enqueue (uniquely, REPLACE policy — preserves current `setReplaceCurrent(true)`, `BackupJobs.java:188`) |
| Port call (sync) | Engine → external system | App-owned interface; adapter behind it |

### Data / Integration Layer (adapters behind ports)

| Port (app-owned) | Adapter (target) | Replaces | Anti-Corruption |
|------------------|------------------|----------|-----------------|
| `BackupScheduler` | `WorkManagerScheduler` (`androidx.work:work-runtime`) | `BackupJobs` + `GooglePlayDriver` + `AlarmManagerDriver` + `SmsJobService` (`BackupJobs.java`; `AlarmManagerDriver.java`; `SmsJobService.java`) | n/a |
| `MailTransport` | `K9ImapTransport` (k-9 versioned/vendored) | `BackupImapStore extends ImapStore` direct coupling | **Yes** — translate `com.fsck.k9.mail.MessagingException` (and the `"Unable to get IMAP prefix"` magic-string match, `State.java:32`) into app-owned `LocalizableException` subtypes *before* they reach the domain `State` |
| `SecretStore` | `EncryptedPrefsSecretStore` (Jetpack Security `androidx.security:security-crypto`) | `AuthPreferences.getCredentials()` plaintext (`AuthPreferences.java:221–226`) | n/a |
| `ContactsPort` | `PeopleApiContactsAdapter` / `ContentResolver` adapter | `contacts/*` + the legacy `m8/feeds/` Contacts GData call (endpoints.md) | Yes (translate API errors) |
| `CalendarPort` | `CalendarProviderAdapter` | `CalendarAccessorPost40`/`Pre40` | n/a (collapses on `minSdk` raise) |

**Data Ownership:**

| Data Domain | Owner | Access Pattern |
|-------------|-------|----------------|
| SMS/MMS/CallLog records | Android system ContentProviders | Read via cursors (backup) / write via provider (restore) — unchanged |
| Backup archive | User-supplied IMAP mailbox | IMAP via `MailTransport` port |
| Credentials (IMAP app-password, OAuth2 tokens) | `SecretStore` (encrypted, Keystore-backed) | Single accessor seam (was `getCredentials()`) |
| App settings + sync checkpoints (`maxSyncedDate`, restore cursor) | `SharedPreferences` facade (`Preferences`) + WorkManager work-data | Typed facade; checkpoint persisted for resumability |

**Data Consistency:** Backup is idempotent by `maxSyncedDate` checkpoint per `DataType` (already present, preserve). **Restore gains a durable checkpoint** (new — ARCH-013) plus idempotent writes keyed on message identity, so a process kill mid-restore neither loses nor duplicates messages. WorkManager's durable work state is the persistence mechanism that makes this cheap.

---

## Technology Stack

### Core Technologies

| Layer | Technology | Target Version | Rationale (trade-off explicit) |
|-------|------------|----------------|-----------|
| Language | Kotlin (incremental) + Java 8/11 interop | Kotlin 1.9.x+ | Carrier for WorkManager/Flow/Hilt which are Kotlin-first; preserve Java where untouched. *Trade-off:* dual-language build complexity vs. idiomatic coroutines/Flow; mitigated by migrating leaf-first (State, DataType) and leaving stable Java as-is. Rejected: full Kotlin rewrite (unjustified risk against a single maintainer). |
| Platform / SDK | Android `compileSdk`/`targetSdk` | **35** (min Play floor 34) | **Gate.** Cannot ship below 34 (DEP-007). *Trade-off:* each SDK bump forces behavioral conformance (exact-alarm, FGS type, notification permission) — staged and test-gated. |
| `minSdk` | Android API | **21** (Lollipop) | Eliminates much of `compat`/`Pre40`; simplifies runtime permissions and `EncryptedSharedPreferences`. *Trade-off:* drops Android 4.x (ICS–KitKat ~ <1% of devices today) — decision recorded in ADR-006. WorkManager itself supports 14, so this is a deliberate simplification, not a forced one. |
| Background execution | **WorkManager** (`androidx.work:work-runtime[-ktx]`) | 2.9.x+ | Single platform subsuming JobScheduler (≥API 23) + AlarmManager (below) — collapses JobDispatcher *and* the AlarmManager fallback *and* AsyncTask into one supported API (ARCH-003/004/018). Owns FG promotion (`setForeground`) and durable work state. *Trade-off:* WorkManager does not guarantee sub-minute exactness; the current `contentUriTrigger` for incoming SMS maps to a WorkManager content constraint with bounded latency — acceptable for backup (not real-time). Rejected: bare `JobScheduler` (re-implements the fallback the team is trying to delete) and `foreground-service-only` (no scheduling guarantees under Doze). |
| Event/state channel | Kotlin `StateFlow` + `SharedFlow` behind `SyncStateRepository` | coroutines 1.7.x+ | `StateFlow` reproduces Otto `@Produce` sticky semantics via `.value`; `SharedFlow` covers one-shot events; both are lifecycle-aware via `repeatOnLifecycle`. *Trade-off vs. LiveData:* Flow composes/maps better and is the modern guidance; LiveData would also work but is in soft-maintenance. Rejected: Greenrobot EventBus (dependencies.md suggested it as a near-drop-in) — it perpetuates a global-bus/Service-Locator anti-pattern (ARCH-001) and adds a dependency rather than removing one. |
| DI | **Hilt** (`com.google.dagger:hilt-android`) | 2.50+ | Formalizes the existing Humble-Object dual-constructor seams (`BackupTask.java:90`); deletes test-only constructors; removes `new Preferences(this)`-per-call Service-Locator (ARCH-002). *Trade-off:* annotation-processing build cost vs. explicit, testable graph. Rejected: Koin (runtime resolution, weaker compile-time safety) and continued manual DI (verbose, duplicated). |
| IMAP client | k-9 mail — **versioned coordinate or vendored module** behind `MailTransport` | maintained release / pinned-verified | Removes the JitPack SHA reproducibility risk (DEP-004) and unpatched-IMAP risk. *Trade-off:* vendoring adds maintenance surface but guarantees reproducibility; a versioned coordinate is cleaner if an API-compatible release exists (evaluate, do not assume). The ACL makes either choice swappable. |
| Secret storage | **Jetpack Security** `EncryptedSharedPreferences` (`androidx.security:security-crypto`) | 1.1.x | AES-256, Keystore master key; single seam at `SecretStore`. *Trade-off:* Keystore key loss (factory reset / Keystore corruption) invalidates stored secrets — acceptable; user re-authenticates. Migration must preserve the existing backup-exclusion of `credentials.xml`. |
| Billing | **Play Billing 7.x** | 7.x | `SkuDetails` is deprecated (DEP-008); donation flow will break. Isolated to `activity/donation/*`. |

### Supporting Technologies

| Purpose | Technology | Rationale |
|---------|------------|-----------|
| Build | AGP 8.x + Gradle 8.x (staged 4.1.3 → 7.4 → 8.x) | Required for `compileSdk 34+`, R8, config cache (DEP-005/006) |
| Code shrinking | R8 (`minifyEnabled true` + rules) | Currently off (`app/build.gradle:33`); reduces APK + obfuscation. *Trade-off:* requires keep-rules for k-9/reflection — gated by a smoke test. |
| Test | JUnit 4.13.2, Robolectric 4.12.x (tied to SDK bump), Truth 1.4.x, **mockito-core 5.x** (replace `mockito-all`) | Current versions block the SDK bump (Robolectric 4.3.1 caps at SDK 29 — DEP-010); `junit 4.12` carries CVE-2020-15250 |
| Coverage gate | JaCoCo `jacocoTestReport` + coverage fitness function | Convert test *breadth* into measured assurance; gate CI on engine-package coverage (ARCH-005) — the characterization safety net for every refactor |
| CI | GitHub Actions workflow (replace Travis) | No workflow exists in-repo (KO-08); CI must be reproducible and run `test`/`lint`/coverage on every PR |
| Supply-chain | `gradle/verification-metadata.xml`; remove `jcenter()`/scijava; Renovate/Dependabot | Detect tampering; eliminate dead repos (DEP-001/002) |
| Observability | `AppLog` (preserve) + centralized redaction utility + opt-in crash reporter | On-device exportable log is the right primitive for a no-backend app; redaction becomes policy not per-call discretion (ARCH-010/021) |

### Technology Principles

1. **Branch-by-abstraction at every dependency seam.** No external dependency is referenced from the domain directly; each sits behind an app-owned port. This is what makes every swap reversible and independently shippable (Fowler; Cockburn Hexagonal).
2. **Preserve the assets, replace the substrate.** The immutable `State` machine, `DataType` type-object, typed-exception hierarchy, and `Preferences` facade are reference-quality and language-portable — they migrate verbatim. Only dead/risky dependencies change.
3. **Secure-by-default as policy.** Validated TLS, encrypted secrets, and redacted logging are enforced centrally, never left to per-call-site discretion (the root cause of the four security findings — ilities-assessment Cross-Cutting #3).
4. **Every refactor is gated by characterization tests.** No seam is swapped until the existing behavior is pinned by a test (Feathers Ch. 2/13). The 35-class suite + JaCoCo gating is the safety net.
5. **No new mandatory network surface; privacy-first.** The app's trust model is "on-device, no telemetry." Any crash reporting is opt-in and ADR-documented.

---

## Quality Attributes

### Target Quality Levels

| Attribute (ISO 25010) | Current (ilities-assessment) | Target | Gap | Priority |
|-----------|---------|--------|-----|----------|
| Portability/Compatibility | 🔴 Red — 3 dead deps, `targetSdk 29` | 🟢 — `targetSdk ≥ 34`, 0 dead deps, reproducible build | Critical | High |
| Security | 🔴 Red — trust-all TLS, plaintext secrets | 🟢 — validated TLS only, encrypted secrets, redacted logs | Critical | High |
| Maintainability | 🟡 Yellow — Otto singleton, no DI, AsyncTask | 🟢 — DI, ports, no static globals, no deprecated primitives | Medium | High |
| Reliability | 🟡 Yellow — no restore checkpoint | 🟢 — durable resumable restore, idempotent writes, classified retry | Medium | Medium |
| Performance Efficiency | 🟡 Yellow — O(mailbox) restore scan | 🟡→🟢 — server-bounded candidate set; `apply()` progress writes | Medium (power users) | Medium |
| Testability | 🟢 Green (hand-built seams) | 🟢+ — DI-formalized seams, measured coverage gate | Low | Medium |
| Observability | 🟡 Yellow — unstructured, no metrics/crash | 🟡→🟢 — centralized redaction, opt-in crash, WorkInfo observability | Low | Low |

### Quality Attribute Scenarios (additional)

**Performance — large mailbox restore (Time Behavior / Capacity):**

| Element | Value |
|---|---|
| Stimulus | Power user with a multi-year, 50k-message Gmail backup initiates restore |
| Response | Restore bounds the candidate set server-side (IMAP `SINCE`/UID windowing or `SORT` where available) before fetch; never materializes the whole folder client-side (replaces `UID SEARCH 1:*` + in-memory sort, `BackupImapStore.java:189,173`) |
| Response Measure | Peak memory bounded by page size, not mailbox size; first page returns within seconds |

**Maintainability — add a new backup data type (Modifiability):**

| Element | Value |
|---|---|
| Stimulus | Maintainer adds a fourth `DataType` |
| Response | Add one enum constant; consumers iterate `DataType.values()` polymorphically (Open/Closed preserved — patterns.md Deep Dive 5) |
| Response Measure | ≤ 1 enum entry + folder string; no changes to engine control flow |

---

## Integration Architecture

### External Integrations (target — note: integration *surface* is unchanged; the *bindings* are modernized)

| System | Integration Type | Pattern | Data Flow |
|--------|-----------------|---------|-----------|
| IMAP server (Gmail `imap.gmail.com:993` / custom) | IMAPS via `MailTransport` port | Adapter + Anti-Corruption Layer; validated TLS only | Bidirectional (backup write / restore read) |
| Google OAuth2 token endpoint (`googleapis.com/oauth2/v3/token`) | HTTPS | Port; retained for contacts/calendar; tokens in `SecretStore` | Inbound token |
| Google contacts | **People API** (replace legacy `m8/feeds/` GData, endpoints.md) | Adapter behind `ContactsPort` | Inbound name lookup |
| Android SMS/MMS/CallLog/Calendar providers | IPC `ContentResolver` | Adapter behind ports | Bidirectional |
| Google Play Billing | IPC SDK 7.x | `ProductDetails` flow | Donation purchase |
| Third-party `com.zegoggles.smssync.BACKUP` broadcast | BroadcastReceiver | **Contract preserved** | Inbound trigger → `BackupScheduler` |

### Integration Pattern — Port & Adapter with Anti-Corruption (target)

```
┌──────────────┐   app-owned port   ┌────────────────────┐   vendor SDK   ┌──────────────┐
│ Engine        │ ─── MailTransport ─▶│ K9ImapTransport    │ ─── k-9 API ──▶│ IMAP server  │
│ (immutable    │ ◀── LocalizableExc ─│  adapter (ACL:      │ ◀── Messaging ─│              │
│  State)       │     (app-owned)     │  translate exc.,    │     Exception   │              │
└──────────────┘                     │  validated TLS only)│                └──────────────┘
                                      └────────────────────┘
   No com.fsck.k9.* type crosses the port. State.java no longer string-matches k-9 internals.
```

### Scheduling Contract Mapping (Firebase JobDispatcher → WorkManager) — preserve behavior

| Current (`BackupJobs.java` / manifest) | Target (WorkManager) | Notes |
|---|---|---|
| `GooglePlayDriver` / `AlarmManagerDriver` selected by `isUseOldScheduler` (`:68–72`) | Single `WorkManagerScheduler`; **delete the dual path** | WorkManager picks JobScheduler/AlarmManager internally |
| `setReplaceCurrent(true)` (`:188`) | `enqueueUniqueWork(..., REPLACE, ...)` / `enqueueUniquePeriodicWork(..., UPDATE, ...)` | Same dedupe semantics |
| `RETRY_POLICY_EXPONENTIAL, 30, 300` (`:205`) | `setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30s, ...)` | Preserve 30s base; WorkManager caps differ — document in ADR |
| `ON_UNMETERED_NETWORK` / `ON_ANY_NETWORK` (`:199`) | `Constraints.setRequiredNetworkType(UNMETERED|CONNECTED)` | Same wifi-only behavior |
| `contentUriTrigger(SMS_PROVIDER, CALLLOG_PROVIDER)` (`:177–183`) | `Constraints.addContentUriTrigger(...)` (API 24+) | Below API 24, fall back to `SmsBroadcastReceiver` → enqueue (already present) |
| `SmsJobService extends firebase.jobdispatcher.JobService` + manifest `ACTION_EXECUTE` filter (`:125–129`) | `BackupWorker : CoroutineWorker`; **remove the `<service>` + intent-filter** | Eliminates the service-as-helper lifecycle bypass (ARCH-004) |

---

## Security Architecture

### Security Layers (STRIDE-framed)

| Layer | Target Controls | STRIDE addressed |
|-------|-----------------|------------------|
| Transport | Validated TLS only; user-pinned cert for self-hosted IMAP; **remove `AllTrustedSocketFactory`** | Tampering, Information Disclosure (ARCH-007/008; CWE-295) |
| Secrets at rest | `EncryptedSharedPreferences` (Keystore AES-256); preserve `credentials.xml` backup exclusion | Information Disclosure (ARCH-009; CWE-312) |
| Logging | Centralized redaction policy; no token bytes in release; gate credential-adjacent logs on `BuildConfig.DEBUG` | Information Disclosure (ARCH-010) |
| Manifest surface | Make four `<receiver>` elements `android:exported`-explicit (API 31 requirement; currently absent — manifest `:131–156`); reconsider `allowBackup` (ARCH-011, ADR) | Spoofing, Tampering |
| Migration safety | `migrate()` must **preserve validated TLS**, never silently enable trust-all (`AuthPreferences.java:276–288`) | Elevation-of-risk-without-consent (ARCH-008) |

### Authentication & Authorization

- **Backup IMAP:** app-password (Gmail policy since Jun 2019; endpoints.md). OAuth2 retained for **contacts/calendar only**.
- **Authorization model:** Android runtime permissions (per-`DataType` permission arrays already modeled in `DataType` — preserve). On `minSdk 21+`, the runtime-permission flow simplifies; on `targetSdk 33+`, add `POST_NOTIFICATIONS` request for the foreground-service notification.

### Data Protection

| Data Type | Classification | Target Protection |
|-----------|----------------|-------------------|
| SMS/MMS/CallLog corpus | Confidential (most sensitive on device) | TLS-validated in transit; never logged |
| IMAP app-password, OAuth2 refresh token | Secret | `EncryptedSharedPreferences`; excluded from cloud backup; never logged |
| Account email / contact names | PII | Redacted in logs; People-API access scoped minimally |

---

## Operational Architecture

### Deployment Architecture

There is no server tier (systems.md). "Deployment" is **APK/AAB distribution** to Google Play + F-Droid.

```
┌──────────────────────────────────────────────────────────────┐
│  Source (GitHub)                                              │
│    └─ GitHub Actions CI (replaces Travis)                     │
│         ├─ ./gradlew test  (JUnit/Robolectric 4.12)           │
│         ├─ ./gradlew lint  (warningsAsErrors)                 │
│         ├─ jacocoTestReport (coverage fitness function — gate)│
│         └─ assembleRelease (R8 on; signed via keystore.props) │
│                          │                                     │
│              ┌───────────┴───────────┐                         │
│              ▼                       ▼                          │
│        Google Play (AAB)        F-Droid (APK)                   │
│        targetSdk ≥ 34           reproducible build             │
└──────────────────────────────────────────────────────────────┘
                          │ install
                          ▼
              On-device app (single process)
              WorkManager schedules background backup
```

### Observability

| Pillar | Target Tooling | Purpose |
|--------|----------------|---------|
| Logs | `AppLog` (preserve) + centralized redaction utility | Exportable on-device diagnostics; secret-safe by policy |
| Metrics | (Lightweight, on-device) backup success/failure counters in `AppLog` | Field-failure visibility without a backend |
| Work state | WorkManager `WorkInfo` / `getWorkInfosByTag` observation | Replaces the static `isServiceWorking()` global query (ARCH-001) |
| Crash reporting | **Opt-in** crash reporter (ADR-documented) | Privacy-respecting; off by default |

### Disaster Recovery (on-device data integrity, not server DR)

| Metric | Target | Strategy |
|--------|--------|----------|
| Backup resumability | Resume from last `maxSyncedDate` checkpoint | Already present; preserve |
| Restore resumability (RPO ≈ 0 dup/loss) | Resume from durable restore checkpoint | **New** — WorkManager work-data + idempotent writes (ARCH-013) |

---

## Transition Considerations

### Coexistence Strategy (branch-by-abstraction; each step independently shippable)

- **Scheduler:** Introduce `BackupScheduler` port; make the existing `BackupJobs` the first adapter (no behavior change, characterization-tested); add `WorkManagerScheduler`; flip the binding; delete JobDispatcher + `AlarmManagerDriver` + `SmsJobService` + the manifest `<service>`/intent-filter.
- **Event bus:** Wrap `App.bus` behind an `EventBus`/`SyncStateRepository` interface (mechanical, no behavior change — Feathers "Introduce Instance Delegator", Ch. 25); migrate emitters/collectors per type to `StateFlow`/`SharedFlow`; delete Otto.
- **Secrets/TLS:** Land `SecretStore` + `EncryptedSharedPreferences` migration and the trust-all removal independently — these are unblocked by the SDK raise except for Keystore ergonomics.

### Feature Parity (must hold across migration)

| Feature | Current | Target | Priority |
|---------|---------|--------|----------|
| SMS/MMS/CallLog backup to IMAP | Works (app-password) | Preserved | High |
| SMS/CallLog restore | Works | Preserved + resumable | High |
| Scheduled/auto/incoming backup | JobDispatcher + content trigger | WorkManager equivalents | High |
| Wifi-only / metered constraint | `ON_UNMETERED_NETWORK` | `NetworkType.UNMETERED` | High |
| Third-party `BACKUP` broadcast | Public contract | **Preserved** | High |
| Calendar call-log sync | Works | Preserved | Medium |
| In-app donation | Billing 2.1.0 | Billing 7.x | Low |
| 25 localisations | Present | Preserved (minimize string churn) | Medium |

### Migration Path

```
Phase 0: Safety net + gate         Phase 1: Security-by-default        Phase 2: Substrate swap            Phase 3: Hardening
─ JaCoCo + characterization tests  ─ Remove trust-all TLS              ─ BackupScheduler→WorkManager      ─ Kotlin (State/DataType first)
─ Remove jcenter()                 ─ Fix silent migration             ─ Otto→StateFlow/SharedFlow         ─ Server-bounded restore search
─ SDK 35 / AGP 8 / Gradle 8        ─ EncryptedSharedPreferences       ─ Hilt DI                           ─ People API contacts
─ exported-explicit receivers      ─ Gate debug log (PII; token masked) ─ MailTransport ACL + k-9 unpin   ─ Opt-in crash reporting
  (0–3 mo)                           (0–3 mo, parallel)                 ─ Billing 7.x                       ─ minSdk 21 cleanup
                                                                        ─ durable restore checkpoint        (9–18 mo)
                                                                         (3–9 mo)
```

**Sequencing rationale (Self-Check #7):** Phase 0 is a hard gate — the SDK raise is the entry condition for shipping *anything* (ilities Cross-Cutting #4), and the test net must exist before any seam is swapped (Feathers). Phase 1 is parallelizable with Phase 0 (security fixes do not depend on the SDK raise except Keystore ergonomics) and is sequenced early because the trust-all path is an *active* Critical exposure that the upgrade *worsens*. Phase 2 depends on Phase 0 (SDK + tests). Within Phase 2, WorkManager goes first (largest blast radius, removes the most dead deps, unblocks the durable checkpoint), then Otto (avoid double-churning the engine), then Hilt (settles once the construction graph is stable). Phase 3 is independent hardening that can parallelize.

---

## Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| WorkManager exactness/latency differs from JobDispatcher content-trigger for incoming SMS | Medium | Medium | Map content-URI trigger to WorkManager content constraint (API 24+); below 24 keep the `SmsBroadcastReceiver`→enqueue path (already present); document bounded latency in ADR-002 |
| `targetSdk` bump surfaces background-execution/FGS-type/notification-permission regressions | High | High | Stage bumps (29→31→33→34/35); test each against background limits; add `FOREGROUND_SERVICE_DATA_SYNC` type + `POST_NOTIFICATIONS` |
| k-9 has no API-compatible released coordinate; vendoring required | Medium | Medium | ACL (`MailTransport`) makes either path swappable; vendor as a fallback to guarantee reproducibility (DEP-004) |
| `EncryptedSharedPreferences` migration loses existing credentials → users must re-auth | Medium | Low | One-time migration with user notice; preserve `credentials.xml` backup exclusion; characterization-test `migrate()` first |
| R8 (`minifyEnabled true`) breaks k-9/reflection paths | Medium | Medium | Author keep-rules; gate behind a backup/restore smoke test before release |
| `-Werror -Xlint:deprecation` blocks the build mid-migration | High | Low | Stage so each step *removes* a deprecation; treat `-Werror` as a deprecation-debt fitness function |
| String churn across 25 locales from UI/permission changes | Medium | Low | Minimize user-facing string changes; batch translation updates |

---

## Architecture Decision Records

### ADR-001: Adopt WorkManager as the single background-execution platform

**Status:** Accepted (target)
**Context:** Background work runs on three obsolete primitives — `AsyncTask` (deprecated API 30, `BackupTask.java:50`), Firebase JobDispatcher (deprecated, scijava-mirror-hosted, `app/build.gradle:60`), and an `AlarmManagerDriver` fallback — with a service-as-helper lifecycle bypass (`SmsJobService.java:82–84`). `targetSdk 29` masks the `AsyncTask` deprecation enforcement.
**Decision:** Replace all three with WorkManager behind an app-owned `BackupScheduler` port; backup/restore run as `CoroutineWorker` with `setForeground`. Delete `BackupJobs`' dual-driver path, `AlarmManagerDriver`, `SmsJobService`, and the manifest `<service>`/`ACTION_EXECUTE` filter.
**Consequences:** (+) Removes the two highest-severity EOL dependencies and the lifecycle bypass in one migration; unblocks `targetSdk` raise; durable work state enables the restore checkpoint (ADR-005). (−) WorkManager does not guarantee sub-minute exactness; content-trigger latency is bounded not immediate — acceptable for backup, documented in ADR-002. (−) Coroutine/Kotlin adoption required in the engine.

### ADR-002: Map content-URI triggers to WorkManager content constraints; keep broadcast fallback below API 24

**Status:** Accepted
**Context:** Incoming-SMS backup uses JobDispatcher `contentUriTrigger` on `SMS_PROVIDER`/`CALLLOG_PROVIDER` (`BackupJobs.java:177–183`).
**Decision:** Use WorkManager `Constraints.addContentUriTrigger` (API 24+); below API 24 enqueue from the existing `SmsBroadcastReceiver`.
**Consequences:** (+) Native, supported mechanism; (−) bounded (not instant) trigger latency — acceptable for a backup tool, not a messenger.

### ADR-003: Replace Otto with `StateFlow`/`SharedFlow` behind an app-owned `SyncStateRepository`

**Status:** Accepted
**Context:** Otto 1.3.8 is archived (DEP-003); the process-global `Bus` singleton + static service back-refs are a Service-Locator/Singleton anti-pattern with a Context-leak hazard (ARCH-001). The `@Produce` sticky-state capability must be preserved.
**Decision:** App-owned repository exposes `StateFlow<SyncState>` (sticky, replaces `@Produce` via `.value`) and `SharedFlow<SyncEvent>` (one-shot). UI collects with `repeatOnLifecycle`. The static `isServiceWorking()` query becomes a `WorkInfo`/repository read.
**Consequences:** (+) Removes archived dependency and Context leak; compile-time-typed events; engine testable without static rigging; allows deleting test-only constructors. (−) Coroutines/Flow learning curve. **Rejected alternative:** Greenrobot EventBus — perpetuates the global-bus anti-pattern and adds rather than removes a dependency.

### ADR-004: Introduce Hexagonal ports + Anti-Corruption Layer at the `mail`/k-9 boundary

**Status:** Accepted
**Context:** `BackupImapStore extends ImapStore` couples to k-9 protected fields; the domain `State` string-matches a k-9 message (`State.java:32`) — a leaky abstraction. k-9 is pinned to a JitPack SHA (DEP-004).
**Decision:** Define `MailTransport`, `BackupScheduler`, `SecretStore`, `ContactsPort`, `CalendarPort`. The k-9 adapter translates `MessagingException` into app-owned `LocalizableException` subtypes; no `com.fsck.k9.*` type crosses the port. Pin k-9 to a versioned coordinate or vendor it.
**Consequences:** (+) Domain insulated from library churn; reproducible builds; each dependency swap is mechanical. (−) More interfaces/adapters to maintain — justified by the vendor-risk containment.

### ADR-005: Add a durable restore checkpoint with idempotent provider writes

**Status:** Accepted
**Context:** Restore has no persisted cursor (ARCH-013); a process kill mid-restore can lose or duplicate messages; there is no idempotency key on SMS-provider writes.
**Decision:** Persist a restore checkpoint in WorkManager work-data; dedupe writes on message identity. Backup checkpoint (`maxSyncedDate`) is already durable — preserve it and switch its per-message `commit()` to `apply()` (ARCH-017).
**Consequences:** (+) Restore is resumable; no duplicates; (−) requires a stable message-identity key — derived from existing IMAP UID / message headers.

### ADR-006: Raise `targetSdk`/`compileSdk` to 35 and `minSdk` to 21

**Status:** Accepted
**Context:** `targetSdk 29` is below the Play floor (cannot ship — DEP-007); `minSdk 14` forces a large `compat`/`Pre40` shim surface.
**Decision:** `compileSdk`/`targetSdk` 35 (Play floor 34); `minSdk` 21. Stage AGP 4.1.3→7.4→8.x and Gradle to 8.x; make the four `<receiver>` elements `android:exported`-explicit; add `POST_NOTIFICATIONS` + `FOREGROUND_SERVICE_DATA_SYNC`.
**Consequences:** (+) Restores Play eligibility; removes much `compat`; modern Keystore/permission ergonomics. (−) Drops Android 4.x (recorded as a deliberate trade-off); each bump requires behavioral conformance testing.

### ADR-007: Migrate secrets to EncryptedSharedPreferences and remove the trust-all TLS path

**Status:** Accepted
**Context:** Plaintext credentials (ARCH-009, CWE-312); trust-all TLS (ARCH-007, CWE-295, Critical); `migrate()` silently enables trust-all on upgrade (ARCH-008).
**Decision:** `SecretStore` backed by `EncryptedSharedPreferences`; remove `AllTrustedSocketFactory`; for self-hosted IMAP, support only an explicit user-pinned cert. `migrate()` must preserve validated TLS.
**Consequences:** (+) Closes the Critical + two High security findings; (−) affected legacy users may need to re-pin a self-hosted cert or re-authenticate (one-time notice).

### ADR-008: Adopt Hilt for dependency injection

**Status:** Accepted
**Context:** No DI container; `new Preferences(this)` per call (ARCH-002); dual real/test constructors are hand-rolled DI.
**Decision:** Hilt; constructor-inject `Preferences`, ports, and engine collaborators; delete test-only constructors.
**Consequences:** (+) Explicit, testable graph; formalizes existing Humble-Object seams; (−) annotation-processing build cost. **Rejected:** Koin (weaker compile-time safety), continued manual DI (verbose/duplicated).

---

## Appendix: Reference Architecture & Standards

### Similar Implementations / Patterns Applied
- **Modern Android "Guide to App Architecture"** — UI ← `ViewModel` + `StateFlow` ← repository ← data sources; directly informs ADR-003 and the `MainActivity` decomposition (ARCH-004).
- **Hexagonal Architecture (Cockburn) / Ports & Adapters** — ADR-004; each external system behind an app-owned port.
- **Branch-by-Abstraction & Strangler-adjacent dependency replacement (Fowler)** — the whole transition strategy; the existing `Driver` Strategy proves the team already understands the indirection.
- **Feathers, *Working Effectively with Legacy Code*** — Ch. 2 (characterization tests as the gate), Ch. 25 (Introduce Instance Delegator for the Otto wrap), Ch. 9–10 (parameterize-constructor seams already present).

### Relevant Standards
- **ISO/IEC 25010** — quality-attribute targets (Portability, Security, Maintainability, Reliability, Performance, Testability, Observability) mapped above.
- **STRIDE** — transport (Tampering/Info-Disclosure), secrets (Info-Disclosure), manifest surface (Spoofing/Tampering), migration (Elevation-of-risk).
- **CWE-295** (Improper Certificate Validation), **CWE-312** (Cleartext Storage of Sensitive Info) — the two security gates.
- **Bass/Clements/Kazman** — quality-attribute scenarios (Ch. 4 format) used for the four scenarios above.
- **Google Play target-API policy** — the shippability gate (`targetSdk ≥ 34`).

---

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| Target-architecture prompt | `assessments/prompts/modernization/02-target-architecture.md` (plugin cache) | Required output structure |
| Expert-Exceeding Depth | `knowledge/standards/expert-exceeding-depth.md` (plugin cache) | Quality bar |
| Project Overview (Step 1.1) | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/discovery/project-overview.md` | Tech stack, entry points, KO-01..10 |
| Patterns Analysis (Step 1.5) | `.../outputs/sms-backup-plus/architecture/patterns.md` | ARCH-001..004, design patterns, target decision matrix |
| Ilities Assessment (Step 1.4) | `.../outputs/sms-backup-plus/architecture/ilities-assessment.md` | ARCH-001..021, quality ratings, WSJF sequencing |
| Dependency Analysis | `.../outputs/sms-backup-plus/discovery/dependencies.md` | DEP-001..010, supply-chain risks, versions |
| Code Location (engagement) | `sdlc/artifacts/engagement/code-location.md` | Build/test commands, SDK levels, constraints |
| Systems (engagement) | `sdlc/artifacts/engagement/systems.md` | No-backend confirmation, layer map |
| Endpoints (engagement) | `sdlc/artifacts/engagement/endpoints.md` | IMAP/OAuth2/contacts/calendar endpoints, Gmail policy |
| Solution Inventory | `sdlc/analysis/20260529-modernization/inputs/solution-inventory.md` | Package map, manifest components, DataType table |
| `app/build.gradle` | `app/build.gradle` | Verified SDK 29, deps, `warningsAsErrors`, `-Werror`, `minifyEnabled false` |
| `build.gradle` (root) | `build.gradle` | Verified AGP 4.1.3, jcenter, scijava mirror |
| `AndroidManifest.xml` | `app/src/main/AndroidManifest.xml` | Verified `SmsJobService` `ACTION_EXECUTE` filter, exported flags, receivers lacking explicit `exported`, `allowBackup=true` |
| `service/BackupJobs.java` | `app/src/main/java/com/zegoggles/smssync/service/BackupJobs.java` | Verified scheduler contract: REPLACE, retry 30/300, network constraints, content-URI triggers |

---

## Verdict

**PASS** — Target architecture defined and grounded in verified source (build files, manifest, and `BackupJobs.java` read in this session; all four Phase 1 outputs and engagement context consulted). Architecture style selected with explicit trade-off analysis against rejected alternatives (microservices, full Kotlin rewrite, Greenrobot EventBus, Koin); every technology choice carries a rationale and a named alternative-not-chosen. Eight ADRs emitted with Context/Decision/Consequences and rejected alternatives. Quality-attribute targets mapped to ISO 25010 with current→target gaps and four Bass/Clements/Kazman scenarios. Integration architecture preserves the external surface while modernizing bindings; the JobDispatcher→WorkManager contract mapping is specified line-for-line against the current `BackupJobs.java`. Security architecture is STRIDE-framed with CWE citations. Migration path is sequenced with explicit dependency/parallelism rationale. All eight expert-depth self-check criteria satisfied.

---

*Assessment status: in-progress (Step 2.2 of EXECUTE). Recommended follow-up: Step 2.3 gap analysis / transition roadmap converting this target into sequenced, estimated work items against the WSJF priorities in the ilities assessment.*
