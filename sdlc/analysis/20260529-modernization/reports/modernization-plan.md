# SMS Backup+ Modernization Plan

**Assessment ID:** 20260529-modernization
**Repository:** sms-backup-plus
**Date:** 2026-05-29
**Version:** 1.0 (Synthesize Phase — Final Deliverable)
**Audience:** Technical Leadership
**All source paths are relative to repository root**

---

## Executive Summary

SMS Backup+ is an anomaly among legacy Android applications: it has a domain core that was engineered correctly, sitting on a substrate that has quietly rotted for a decade. The application's internal design — an immutable state machine, a type-object enum for data classification, a typed exception hierarchy serving 25 locales, and a disciplined preferences facade — represents reference-quality Android engineering. None of that is the problem.

The problem is exclusively external: three of the six core runtime dependencies are abandoned or dead, the build targets an Android SDK version that Google Play has blocked since 2024, and a critical security vulnerability allows a network attacker to intercept the user's entire SMS history and email credentials in transit. The app cannot currently ship a single update to its primary distribution channel.

**The recommended path is not a rewrite.** The business logic, the state machine, and the 35-class test suite are the assets. The work is substrate replacement — swapping dead libraries behind the app's own interfaces so the business logic never notices the change. This approach costs approximately 6–9 engineer-weeks for a single senior Android developer to restore full Play Store eligibility, eliminate the active security exposure, and retire every abandoned dependency.

### The Non-Negotiable Actions

Two outcomes must happen regardless of any other decision:

1. **Restore Play Store eligibility.** The `targetSdkVersion 29` setting is 5 API levels below Google Play's enforced floor. No improvement, bug fix, or security patch can reach users through the primary distribution channel until this is resolved. The investment is a build-configuration change followed by a handful of Android API conformance fixes — estimated at two to three weeks of focused engineering.

2. **Close the active MITM vulnerability.** The `AllTrustedSocketFactory` class disables TLS certificate validation entirely. A silent upgrade migration has been actively enrolling legacy users into this trust-all mode without their knowledge. This means a network attacker can intercept the full contents of a user's SMS and MMS backup stream, plus the Gmail app password or OAuth2 token used to access it. This fix has no dependency on the Play Store work and must begin immediately.

### Investment Summary

| Workstream | Engineer-Weeks | Outcome |
|---|---|---|
| Phase 0: Build gate + CI + security (parallel) | ~3–4 weeks | Play eligibility restored; active MITM closed; CI live |
| Phase 1: EncryptedSharedPreferences + dead-code sweep | ~2 weeks (overlaps Phase 0) | Credentials encrypted at rest; supply-chain debt cleared |
| Phase 2: Substrate swap (WorkManager, StateFlow, Hilt, k-9 ACL) | ~5–7 weeks | All abandoned dependencies retired; durable restore checkpoint |
| Phase 3: Strategic hardening (elective) | ~3–4 weeks | Kotlin migration initiated; Billing 7.x; k-9 coordinate pinned |
| **Total (non-elective core)** | **~9 engineer-weeks** | **App fully modernized, shippable, secure** |

Infrastructure cost: $0. All target libraries are Apache-2.0 or Android SDK License. GitHub Actions CI runs free for open-source projects.

---

## Finding Aggregation

### Summary Counts by Output File

| Output | Critical | High | Medium | Low | Total |
|---|:---:|:---:|:---:|:---:|:---:|
| `assessment.md` (Step 2.1) — modernization-decision findings | 2 | 6 | 3 | 1 | **12** |
| `gaps.md` (Step 2.3) — gap findings | 2 | 8 | 6 | 2 | **18** |
| `candidates.md` (Step 3.1) — strangler suitability findings | 0 | 3 | 4 | 1 | **8** |

> The three finding sets derive from the same underlying evidence and overlap by design: `gaps.md` derives its gap characterizations from `assessment.md`; `candidates.md` re-projects the same seams through a strangler-suitability lens. The canonical gap set for remediation planning is the 18 findings in `gaps.md`. The 8 strangler candidates in `candidates.md` are a scoring overlay on that gap set, not additive findings.

### Counts by Severity — Canonical Gap Set (`gaps.md`, 18 findings)

| Severity | Count | Findings |
|---|:---:|---|
| Critical | 2 | TECH-001 (SDK/Play gate), SEC-001 (trust-all TLS + silent migration) |
| High | 8 | STRUCT-001, STRUCT-002, TECH-002, TECH-003, TECH-004, CAP-001, OPS-001, OPS-002 |
| Medium | 6 | STRUCT-003, STRUCT-004, TECH-005, CAP-002, SEC-002, OPS-003 |
| Low | 2 | STRUCT-005 (dead shim), TECH-007 (jcenter declarations) |
| **Total** | **18** | |

### Counts by Domain — Canonical Gap Set

| Domain | Critical | High | Medium | Low | Total |
|---|:---:|:---:|:---:|:---:|:---:|
| Structural (Hexagonal ports, global bus, God-Activity) | 0 | 2 | 2 | 1 | **5** |
| Technology (SDK, WorkManager, Otto, test stack, k-9, jcenter) | 2 | 3 | 1 | 1 | **7** |
| Capability (restore checkpoint, FLAG_IMMUTABLE, EncryptedPrefs) | 0 | 1 | 2 | 0 | **3** |
| Process (CI/CD, coverage gate, build policy) | 0 | 2 | 1 | 0 | **3** |

---

## Domain Sections

### Domain 1: Technology Currency

**Summary:** Seven of the 18 gaps are technology gaps. Two are Critical; both are non-discretionary.

#### TECH-001 / DEBT-001: targetSdk 29 — the Play Store gate (Critical)

`compileSdkVersion 29` and `targetSdkVersion 29` in `app/build.gradle:10,15` place the app below Google Play's update floor (API 34 enforced since August 2024). The app cannot ship any update to its primary distribution channel. This is the unconditional prerequisite for every other gap closure that needs to reach users.

The technical conformance work required when raising to `targetSdk 35` includes: declaring `android:exported` on the four `<receiver>` elements in the manifest, adding `FOREGROUND_SERVICE_DATA_SYNC` as the foreground service type, requesting `POST_NOTIFICATIONS` at runtime, and adding `FLAG_IMMUTABLE` to two `PendingIntent` constructions that will hard-crash on API 31+ once the target is raised (`AlarmManagerDriver.java:127`, `ServiceBase.java:204`). The AGP upgrade follows an incremental path: 4.1.3 → 7.4 → 8.x to avoid skipping too many breaking changes in a single step.

#### SEC-001 / ARCH-003: Trust-all TLS + silent security-downgrade migration (Critical)

`AllTrustedSocketFactory.java:42-57` contains an `InsecureX509TrustManager` whose `checkServerTrusted()` method is empty — it accepts any certificate from any server unconditionally. `AuthPreferences.migrate()` silently enables this trust-all path for users who previously configured a `+ssl` or `+tls` IMAP server address, upgrading them into an MITM-vulnerable configuration without their knowledge.

The exploit path is direct: a user with trust-all enabled connects to their IMAP server over a hostile Wi-Fi network; an on-path attacker presents a self-signed certificate for `imap.gmail.com`; `checkServerTrusted` accepts it without complaint; the attacker terminates TLS, reads the complete SMS/MMS/call-log backup stream, and captures the Gmail app password in transit. This fix has no dependency on TECH-001 and must start immediately, in parallel.

The correct resolution is: delete `AllTrustedSocketFactory`; replace with validated TLS; provide an explicit user-pinned-certificate path for users who genuinely self-host IMAP; rewrite `migrate()` to preserve validated TLS and surface a one-time notice rather than silently downgrading security.

#### TECH-002 / DEP-002: Firebase JobDispatcher + AsyncTask + AlarmManager (High)

Three obsolete background-execution primitives form one coupled problem. `firebase-jobdispatcher:0.8.6` was deprecated by Google in 2018 and is now served exclusively from `maven.scijava.org` — a scientific-computing Maven mirror with no Google affiliation. If that mirror experiences an outage or evicts the artifact, all CI builds break with no fallback. `AsyncTask` was deprecated in Android API 30 and removed in API 33; three classes extend it (`BackupTask.java`, `RestoreTask.java`, `tasks/OAuth2CallbackTask.java`), each carrying `@SuppressLint("StaticFieldLeak")` as an explicit acknowledgment of a known memory-leak hazard. The `AlarmManagerDriver` fallback adds a third scheduling path that must be maintained separately.

WorkManager replaces all three in a single migration. It natively subsumes `JobScheduler` (API 23+) and `AlarmManager` (below API 23), owns worker lifecycle and foreground service promotion, and provides durable work state that enables the restore checkpoint (CAP-001). The existing `Driver` Strategy pattern in `BackupJobs.java:66-72` is already half of the `BackupScheduler` port this migration requires.

#### TECH-003 / DEP-003: Square Otto 1.3.8 → Kotlin StateFlow/SharedFlow (High)

`com.squareup:otto:1.3.8` (archived 2014) is imported in 12 production files across all layers. It provides no compile-time subscriber validation, uses reflection, and has never been patched for security issues. `App.bus` is a static singleton referenced from every layer of the application, creating invisible coupling that makes individual components difficult to test in isolation.

The replacement is Kotlin `StateFlow` (which reproduces Otto's `@Produce` sticky-state semantics via `.value`) and `SharedFlow` (for one-shot events). This is sequenced after the WorkManager migration to avoid double-churning the engine concurrently.

#### TECH-004 / DEP-004: Test toolchain and Play Billing (High)

`robolectric:4.3.1` is a hard co-dependency of TECH-001: it cannot execute tests against an SDK higher than 29. It must be upgraded to 4.12.x as part of the same build sweep that raises the target SDK, or the existing test suite will break immediately. `junit:4.12` carries CVE-2020-15250. `mockito-all:1.10.17` is a 2015 uber-jar that conflicts with `mockito-core` 5.x.

`play-services-billing:2.1.0` is five major versions behind the current 7.x series. The `SkuDetails` API it uses is deprecated and Google Play's billing policy enforcement is expected to mandate the 6.x+ `ProductDetails` API. This is isolated to three donation-subsystem files and is a lower-priority Phase 3 item.

#### TECH-005 / DEP-005: k-9 mail library pinned to JitPack SHA (Medium)

`com.github.jberkel.k-9:k9mail-library:eaf689025e` is pinned to a raw git commit hash via JitPack. There is no semantic version, which means the build is unreproducible if JitPack evicts the cached artifact. More significantly, the k-9 / Thunderbird project has continued development — any security fixes in the IMAP library are not available to this application. This gap closes behind the `MailTransport` Anti-Corruption Layer introduced in the Phase 2 substrate swap.

#### TECH-007 / DEP-006: jcenter after 2022 shutdown (Low)

`jcenter()` is declared twice in the root `build.gradle` (lines 4 and 21) and `jcenter.bintray.com` at line 25, listed before `mavenCentral()`. The JCenter service was shut down in February 2022. This is a 30-minute fix.

---

### Domain 2: Structural Architecture

**Summary:** Five structural gaps, all in the additive direction — introducing ports and abstractions rather than redesigning existing logic.

#### STRUCT-001 / ARCH-001: No Hexagonal ports at dependency boundaries (High)

The engine references vendor library types directly. `BackupImapStore extends ImapStore` couples to k-9 internal fields; the domain `State.java:32` contains a magic-string match against a k-9 exception message; `BackupJobs` imports `com.firebase.jobdispatcher` types directly. There are only 7 interfaces in the entire module.

The target architecture introduces four app-owned port interfaces — `BackupScheduler`, `MailTransport`, `SecretStore`, and `ContactsPort/CalendarPort` — so every dead or risky library sits behind an interface. This is additive structure: the existing implementations become "first adapters" with zero behavior change. It is the structural precondition that makes every subsequent technology-gap swap cheap and reversible.

#### STRUCT-002 / ARCH-002: Process-global Otto Singleton (High)

`App.bus` is a static field reached from 12 files across all architectural layers. `private static` back-references to `SmsBackupService` and `SmsRestoreService` create hidden bidirectional coupling and a Context-leak hazard. `App.register()` swallows `IllegalArgumentException` silently. The `@Produce` annotation provides sticky last-state semantics that must be preserved when migrating to Flow.

The migration strategy (Feathers *Working Effectively with Legacy Code*, Ch. 25 "Introduce Instance Delegator") is: first wrap `App.bus` behind an injected `SyncStateRepository` interface with zero behavior change; then swap the implementation to `StateFlow<SyncState>` + `SharedFlow<SyncEvent>`; finally fold the static `isServiceWorking()` query into a `WorkInfo`/repository read.

#### STRUCT-003 / ARCH-003: Service-as-helper lifecycle bypass (Medium)

`SmsJobService.java:82-84` manually instantiates `SmsBackupService`, calls `attachBaseContext(this)`, and directly invokes `handleIntent()` to circumvent Android's API 26 background-start restriction. Because `onCreate()` never runs on this path, `AppLog` initialization and `App.register()` are silently skipped. This hazard is structurally entangled with the JobDispatcher dependency and is retired automatically by the WorkManager migration.

#### STRUCT-004 / ARCH-004: MainActivity carries multiple responsibilities (Medium)

`MainActivity.java` (499 LOC, highest branch-token complexity hotspot in the module) simultaneously orchestrates permission flows, dialog management, Otto bus subscription, and backup/restore triggering. There is no `ViewModel`; UI state lives directly in the Activity. This is the standard God-Activity pattern. The refactor — extract `MainViewModel`, migrate dialogs to the existing `Dialogs.java`, collect state from `SyncStateRepository` via `repeatOnLifecycle` — pairs naturally with the Otto→Flow migration and should be sequenced there.

#### STRUCT-005 / ARCH-005: Dead compatibility shim (Low)

`CalendarAccessorPre40.java` targets API < 14 and is unreachable at the current `minSdkVersion 14`. At `minSdk 21` (the TECH-001 target), it is provably dead by two API levels. `StatusPreference.java:131-139` contains stubbed `// TODO` instance-state save/restore, causing a visual-state flash on screen rotation. Both are swept opportunistically during the SDK uplift.

---

### Domain 3: Capability

**Summary:** Three capability gaps. CAP-001 (durable restore) is High severity and is the primary data-integrity concern.

#### CAP-001 / ARCH-006: No durable restore checkpoint; O(mailbox) IMAP scan (High)

`RestoreTask.java` has no persisted cursor. A process kill mid-restore can produce duplicate or lost messages, with no idempotency key to detect and prevent duplicates on a retry. `catch(MessagingException) → ERROR` collapses transient network failures (retry-able) and permanent protocol errors (not retry-able) into the same failure code, preventing intelligent retry behavior.

`BackupImapStore.java:148-186` performs `UID SEARCH 1:*` to retrieve all IMAP UIDs, sorts the full result set in memory, and only then applies the per-session message count cap. For users with large backup mailboxes, this is an O(mailbox) memory and latency cost that must be paid on every restore session.

The checkpoint fix depends on WorkManager (which provides durable work-state natively). Server-side bounding with `SORT`/`SINCE`/UID windowing is independent and can be parallelized.

#### CAP-002 / QUAL-001: PendingIntent without FLAG_IMMUTABLE (Medium)

`AlarmManagerDriver.java:127` and `ServiceBase.java:204` construct `PendingIntent` with `FLAG_UPDATE_CURRENT` but without `FLAG_IMMUTABLE` or `FLAG_MUTABLE`. This is currently latent at `targetSdk 29` but becomes an `IllegalArgumentException` hard crash on API 31+ the moment TECH-001 raises the target SDK. This two-line fix must co-land with the SDK gate.

#### SEC-002 / CAP-003: Plaintext credential storage (Medium)

`AuthPreferences.java:221-226` stores the Gmail app password and OAuth2 refresh token in plaintext `SharedPreferences`. The existing defensive design — using a separate `credentials.xml` preferences file with Android auto-backup exclusion — is good but insufficient. Segregation is not encryption: ADB backup, rooted-device extraction, or forensic imaging can read the plaintext credentials. Jetpack Security `EncryptedSharedPreferences` (AES-256, Android Keystore master key) closes this at the single `getCredentials()` accessor seam.

---

### Domain 4: Process and DevOps

**Summary:** Three process gaps. The absence of CI and measurable coverage is the silent risk multiplier for the entire modernization — every substrate swap becomes a refactor against an unmeasured codebase with no regression gate.

#### OPS-001 / PROC-001: No CI/CD in the repository (High)

There is no `.github/workflows/` directory. The repository has a Travis CI badge, which references an external service that is no longer free for open-source projects. No automated build, test, lint, or release pipeline runs on pull requests or commits. `minifyEnabled false` in `app/build.gradle:33` means that release builds do not exercise R8 code shrinking and keep-rules issues are discovered only at user-facing runtime.

The target is a GitHub Actions workflow running `./gradlew test lint jacocoTestReport assembleRelease` on every pull request, plus `gradle/verification-metadata.xml` for supply-chain verification. This has no upstream dependency and starts in parallel with everything else.

#### OPS-002 / PROC-002: Coverage unmeasured — no fitness function protecting refactors (High)

The 35-class Robolectric test suite provides good breadth coverage (one test class per major package), but total coverage is unmeasured — there is no JaCoCo plugin configuration in `app/build.gradle`. The test:prod LOC ratio is 0.33:1, below the 0.5–1.0 ratio typical of well-covered Android engines. `MainActivity` (the highest-complexity file) has no test. `OAuth2Client` has no behavioral test. `SmsBackupServiceTest.java:212` contains an empty `// TODO` test body.

This is the single most important governance rule: **no seam swap begins until its behavior is pinned by a characterization test** (Feathers Ch. 2). The JaCoCo coverage gate, with a ≥70% threshold on `service/`, `mail/`, and `auth/` packages, must land before Phase 2 substrate work begins.

#### OPS-003 / PROC-003: Build policy brittleness and absent observability (Medium)

`warningsAsErrors true` (lint) and `-Werror -Xlint:deprecation` (javac) are both active. These are sound practices in steady-state, but will repeatedly break the build during the SDK migration as each API-level bump surfaces new deprecation warnings. The current response — 27–29 `@SuppressWarnings` and `@SuppressLint` annotations accumulated across the codebase — is managed suppression rather than root-cause remediation.

`OAuth2Client.java:145` logs the resolved Google account **email (PII)** in clear via an **ungated `Log.d`** that ships in release builds; the OAuth2 token itself is **masked** by `getTokenForLogging()` (`OAuth2Token.java:61-69`, `replaceAll(".", "X")`) and is **not** in cleartext. The defects are the unredacted PII, debug logging shipping in release, and decentralized redaction (no centralized helper). On a backendless app, there is no crash reporting visibility into field failures.

---

## Strangler Candidate Summary

Eight in-process module seams were scored against six suitability factors (interface clarity, coupling, data isolation, change frequency, business value, risk level). The scores and recommended sequencing are:

| ID | Component | Score | Rating | Wave |
|---|---|:---:|---|:---:|
| STR-001 / CAND-001 | Scheduler/Worker → WorkManager | **88** | Excellent | 2 (facade in Wave 1) |
| STR-002 / CAND-003 | Secret store → EncryptedSharedPreferences | **84** | Excellent | 3 |
| STR-003 / CAND-005 | Play Billing 2.1.0 → 7.x | **80** | Excellent (low priority) | 4 |
| STR-004 / CAND-008 | Transport security — delete trust-all TLS | **74** | Good | 1 (day-1, parallel) |
| STR-005 / CAND-002 | Event spine → StateFlow/SharedFlow | **72** | Good | 3 |
| STR-006 / CAND-004 | Mail/k-9 ACL + MailTransport port | **70** | Good | 3 |
| STR-007 / CAND-006 | Contacts → People API | **66** | Good (evaluate first) | 4 |
| STR-008 / CAND-007 | Hilt DI formalization (enabling seam) | **52** | Moderate | 4 |

The highest-leverage finding: CAND-001 (Scheduler) is already 50% built. The `Driver` Strategy at `BackupJobs.java:66-72` is exactly the "first adapter" slot that branch-by-abstraction requires. Introducing the `BackupScheduler` port and making `BackupJobs` its first adapter is a mechanical, zero-behavior-change, independently shippable step that can land in Week 1.

Three components are explicitly not candidates for strangling: the immutable `State` machine, the `DataType` type-object enum, and the typed `Preferences` facade. These are the protected domain core that the ports exist to insulate.

---

## Target Architecture

The target retains the layered monolith macro-structure and inserts Hexagonal ports at every external dependency boundary. One unified background-execution platform (WorkManager) replaces three obsolete primitives. One unified reactive state channel (StateFlow/SharedFlow) replaces the archived Otto bus.

```
PRESENTATION (activity*)
MainActivity + MainViewModel  <-- collect StateFlow (lifecycle-aware)
Auth / Donation (Billing 7.x)
            |
SyncStateRepository  (StateFlow<SyncState> + SharedFlow<SyncEvent>)
            |
ENGINE (service*)
BackupWorker : CoroutineWorker (WorkManager, setForeground)
RestoreWorker : CoroutineWorker (durable checkpoint)
     |  Immutable State machine PRESERVED VERBATIM
     |
PORTS (app-owned interfaces)
BackupScheduler  MailTransport  SecretStore  ContactsPort  CalendarPort
     |               |              |
ADAPTERS
WorkManagerScheduler  K9ImapTransport (+ACL)  EncryptedPrefsSecretStore
     |
WorkManager <-- single scheduler (subsumes JobScheduler + AlarmManager)
k-9 (versioned coordinate or vendored)
Jetpack Security (AES-256, Keystore)
```

**Technology decisions:**

| Decision | Choice | Rationale |
|---|---|---|
| Background execution | WorkManager 2.9.x+ | Subsumes JobScheduler and AlarmManager; owns worker lifecycle and foreground promotion |
| Event/state channel | Kotlin StateFlow + SharedFlow | Lifecycle-aware; replaces Otto `@Produce` sticky semantics; removes abandonware dependency |
| Dependency injection | Hilt (Dagger) | Formalizes existing Humble-Object seams; removes Service-Locator pattern |
| Credential storage | Jetpack Security EncryptedSharedPreferences | AES-256, Android Keystore master key; closes CWE-312 |
| TLS trust | Validated TLS only; user-pinned cert for self-hosted IMAP | Removes AllTrustedSocketFactory; closes CWE-295 |
| k-9 IMAP client | Versioned coordinate or vendored module behind MailTransport ACL | Closes JitPack SHA reproducibility risk |
| SDK / Platform | compileSdk/targetSdk 35, minSdk 21 | Play floor is 34; minSdk 21 eliminates Pre40 compat surface |
| Build toolchain | AGP 8.x / Gradle 8.x (staged 4.1.3 → 7.4 → 8.x) | Required for compileSdk 34+, R8, configuration cache |
| Language | Kotlin as carrier for WorkManager/Flow/Hilt seams | Kotlin-first Jetpack APIs; domain core preserved in Java, migrated incrementally |

---

## Remediation Roadmap

### Time-Banded Work Items

The roadmap is organized into three time bands. All MU IDs are canonical from `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md`; WSJF scores from `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/prioritized-backlog.md`.

#### Immediate (0–30 days)

These items have no upstream blockers and must start in parallel with each other on day one.

| MU | Initiative | Gap IDs | WSJF | Effort | Outcome |
|---|---|---|:---:|:---:|---|
| MU-001 | Build & Platform Uplift (the gate) | TECH-001, DEP-006, QUAL-001, ARCH-005 | 4.67 | L | targetSdk 35; FLAG_IMMUTABLE co-landed; jcenter removed; Play-eligible |
| MU-002 | Test-Harness & Coverage Gate (safety net) | OPS-001, OPS-002, DEP-004 (test) | 6.50 | M | CI live; JaCoCo ≥70% on service/mail/auth; characterization tests for migrate() and BackupJobs |
| MU-003 | Transport Security Hardening | SEC-001, OPS-003 (redaction) | 5.50 | M | AllTrustedSocketFactory deleted; migrate() preserves validated TLS; credentials not logged |

**Key constraints:**
- MU-002 (Robolectric) is a co-requisite of MU-001: Robolectric 4.3.1 caps at SDK 29 and must be upgraded in the same build transaction.
- MU-003 has no dependency on MU-001 and runs in parallel from day one.
- QUAL-001 (`FLAG_IMMUTABLE`) must co-land inside MU-001 or the SDK raise introduces a hard crash on the scheduling path.

**Decision gates:**
- G0: A signed AAB with targetSdk 35 passes Play pre-launch review; CI is green; jcenter declarations are absent.
- G1: No code path accepts an unvalidated TLS certificate; `migrate()` is covered by a test that confirms it never silently sets `SERVER_TRUST_ALL_CERTIFICATES = true`.

#### Short-term (30–90 days)

These items depend on the Immediate phase completing (SDK gate + CI + coverage gate).

| MU | Initiative | Gap IDs | WSJF | Effort | Outcome |
|---|---|---|:---:|:---:|---|
| MU-004 | Secret-at-Rest (EncryptedSharedPreferences) | SEC-002, CAP-003 | 3.50 | M | Credentials AES-256 encrypted; credentials.xml backup exclusion preserved |
| MU-011 | Dead-code sweep + minSdk cleanup | ARCH-005, STRUCT-005 | — | S | CalendarAccessorPre40 deleted; StatusPreference instance-state implemented |
| MU-006 | Hexagonal ports (structural precondition) | STRUCT-001, ARCH-001 | 4.00 | L | BackupScheduler, MailTransport, SecretStore, ContactsPort interfaces introduced; existing implementations become first adapters |
| MU-005 | Scheduler → WorkManager (spine) | TECH-002, ARCH-003, STRUCT-003 | 4.33 | L | Firebase JobDispatcher, AsyncTask, AlarmManagerDriver, SmsJobService, scijava mirror all retired; WorkManagerScheduler live; durable restore checkpoint active |

**Key constraints:**
- MU-006 (Hexagonal ports) is the structural precondition for MU-005. The ports must be introduced with existing implementations as first adapters before the WorkManager swap begins.
- MU-005 is the highest-leverage single migration in the engagement. One binding flip retires four dead dependencies and the service-as-helper lifecycle bypass simultaneously.
- The public `com.zegoggles.smssync.BACKUP` broadcast contract must be preserved through the WorkManager migration — third-party apps depend on it.

**Decision gates:**
- G2: JaCoCo ≥70% on service/mail/auth; characterization tests for BackupJobs retry 30/300 and constraints pass before MU-005 begins.
- G3: All four scheduling-contract invariants pass after the WorkManager flip (REPLACE semantics, 30/300 exponential backoff, UNMETERED constraint, content-URI trigger with broadcast fallback below API 24).

#### Medium-term (90–180 days)

These items complete the substrate swap and constitute the fully modernized application.

| MU | Initiative | Gap IDs | WSJF | Effort | Outcome |
|---|---|---|:---:|:---:|---|
| MU-006e | Otto → SyncStateRepository/StateFlow/SharedFlow | TECH-003, STRUCT-002, STRUCT-004 | 3.20 | M | Otto removed; SyncStateRepository live; MainActivity decomposed with MainViewModel |
| MU-007 | Hilt DI formalization | TECH-005, STRUCT-001 (binding) | — | M | Constructor injection; Service-Locator pattern removed; test-only constructors deleted |
| MU-008 | Mail ACL + k-9 unpin | DEP-005, ARCH-001 (leak) | — | M | No com.fsck.k9.* type crosses MailTransport; State.java:32 magic-string removed; k-9 on reproducible coordinate |
| MU-010 | Durable restore checkpoint + classified retry + server-bounded IMAP search | CAP-001, ARCH-006 | 3.00 | L | Restore resumable after process kill; zero message duplication; O(page) not O(mailbox) memory on restore |

**Phase 3 items (elective, Month 4+):**

| MU | Initiative | WSJF | Effort | Outcome |
|---|---|:---:|:---:|---|
| MU-009 | Play Billing 7.x | 2.00 | M | Donation flow uses ProductDetails API; SkuDetails retired |
| Kotlin promotion | Staged Kotlin migration; State → sealed interface | — | L | Modern language idioms; WorkManager/Flow seams already in Kotlin from Phase 2 |
| Opt-in observability | Privacy-respecting crash reporting; WorkInfo observability | — | M | Field failure visibility; ADR-documented, off-by-default |

### Critical Path

```
MU-001 (SDK gate) --> MU-002 (safety net, co-req) --> MU-006 (Hexagonal ports) --> MU-005 (WorkManager)
                                                                                          |
                                                                      MU-010 (durable restore checkpoint)

Independent parallel tracks (start day 1, no upstream dependencies):
  MU-003 (trust-all TLS removal)
  MU-002 CI setup
```

### Key Milestones

| Milestone | Target | Success Criteria |
|---|---|---|
| M0: Play Eligibility Restored | End of Month 1 | Signed AAB with targetSdk 35 published to Play Store |
| M1: Critical Security Closed | End of Month 1 (parallel) | AllTrustedSocketFactory deleted; credentials encrypted; no release-log token leakage |
| M2: Supply-Chain De-risked | End of Month 2 | firebase-jobdispatcher and scijava mirror removed; builds from google()/mavenCentral() only |
| M3: Substrate Swap Complete | End of Month 3 | Otto removed; WorkManager scheduling live; durable restore checkpoint; k-9 ACL |
| M4: Strategic Hardening | Month 4+ (elective) | Kotlin migration initiated; Billing 7.x deployed; k-9 coordinate pinned |

---

## Success Metrics

| Metric | Current | Target | How to Measure |
|---|---|---|---|
| Deployment Frequency | Blocked (Play rejects targetSdk 29) | On-demand | Play Console release cadence post-M0 |
| Lead Time (commit → shippable) | Unmeasured (no CI) | < 1 day | CI pipeline duration |
| Change Failure Rate | Unmeasured | < 15% | CI test-fail rate per merge |
| Abandoned dependencies | 3 (Otto, JobDispatcher, k-9 SHA) | 0 | app/build.gradle dependency audit |
| targetSdk vs. Play floor | 29 (5 levels below API 34) | 35 | app/build.gradle |
| Engine coverage (service/mail/auth) | Unknown (0.33:1 test:prod LOC) | ≥ 70% line/branch | JaCoCo jacocoTestReport in CI |
| Trust-all TLS code path | 1 (active, CWE-295) | 0 | grep AllTrustedSocketFactory — absent |
| Supply-chain mirrors | 2 (scijava + JitPack SHA pin) | 0 | Repository block audit + verification-metadata.xml |
| Restore checkpoint (data integrity) | None | Durable, idempotent | Test: interrupted restore produces zero duplicates |
| Token/PII in release logs | Present (OAuth2Client.java:145) | 0 | BuildConfig.DEBUG guard verified by CI lint |

---

## Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|:---:|:---:|---|
| targetSdk bump surfaces background-execution / FGS-type / notification-permission regressions | High | High | Stage 29→31→33→34/35; JaCoCo fitness function gates each step; FLAG_IMMUTABLE co-landed with gate |
| WorkManager lifecycle semantics differ from JobDispatcher → missed or duplicate scheduled backups | Medium | High | Characterization-test retry 30/300 and network constraints before swap; Parallel-Run new scheduler in debug build before cut-over |
| scijava/JitPack outage breaks CI mid-modernization | High | Medium | Phase 0 removes jcenter + adds verification-metadata.xml; MU-005 WorkManager migration retires the scijava mirror entirely |
| Removing trust-all TLS breaks genuine self-signed-IMAP users | Medium | Medium | Explicit user-pinned-cert path + one-time migration notice; ADR documented |
| k-9 has no API-compatible released coordinate; vendoring required | Medium | Medium | MailTransport ACL makes either path swappable; vendor as fallback (ADR-004) |
| Coverage unmeasured — refactors regress silently | High | Medium | JaCoCo + coverage fitness function lands in Phase 0 before any refactor; backfill MainActivity/OAuth2Client tests |
| Single maintainer / maintenance-mode — modernization stalls | Medium | Medium | Phase 0 alone restores Play eligibility + de-risks supply chain; each phase is independently shippable |
| -Werror / warningsAsErrors blocks build mid-migration | High | Low | Relax to lint-baseline.xml during transition; restore -Werror once deprecated APIs are eliminated |

---

## Architecture Decision Records

Eight ADRs were documented in `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/target-state.md` and govern the key decisions:

| ADR | Decision | Status |
|---|---|---|
| ADR-001 | WorkManager as single background-execution platform | Accepted |
| ADR-002 | Content-URI triggers → WorkManager content constraints; broadcast fallback below API 24 | Accepted |
| ADR-003 | Otto → StateFlow/SharedFlow behind SyncStateRepository; Greenrobot EventBus rejected | Accepted |
| ADR-004 | Hexagonal ports + Anti-Corruption Layer at mail/k-9 boundary | Accepted |
| ADR-005 | Durable restore checkpoint + idempotent provider writes | Accepted |
| ADR-006 | compileSdk/targetSdk 35; minSdk 21 | Accepted |
| ADR-007 | EncryptedSharedPreferences for credentials; remove trust-all TLS path | Accepted |
| ADR-008 | Hilt DI; Koin rejected | Accepted |

**Key rejected alternatives documented:**
- Rebuild/rewrite: rejected because it discards a correct domain core, 35 characterization tests, and 25-locale translation surface for zero user-visible gain at 3–5x the engineering investment.
- Greenrobot EventBus as Otto replacement: rejected because it perpetuates the global-bus anti-pattern and adds a dependency rather than removing one.
- Raw JobScheduler as AsyncTask/JobDispatcher replacement: rejected because it does not subsume the AlarmManager fallback and offers no Jetpack lifecycle or test support.
- Koin DI: rejected over Hilt due to weaker compile-time safety (runtime resolution vs. compile-time validation).

---

## Recommendations

1. **Start Phase 0 and Phase 1 in parallel today.** Phase 0 (SDK gate) is the unconditional prerequisite for shipping to the Play Store. Phase 1 (trust-all TLS removal and the `migrate()` fix) has zero SDK dependency — every day the active MITM exposure on users' message corpora is unaddressed is a day of real, remediable risk.

2. **Establish the JaCoCo coverage fitness function before touching any seam.** The 35-class test suite is the safety net for every subsequent refactor. Its adequacy is currently unmeasured. Refactoring an unmeasured codebase is the single largest self-inflicted risk in this engagement. The coverage gate must land before Phase 2's substrate swaps begin.

3. **Treat the WorkManager migration as the highest-leverage single engineering investment.** One coherent migration retires Firebase JobDispatcher, `AsyncTask`, the `AlarmManagerDriver` fallback, the `maven.scijava.org` supply-chain mirror, and the `SmsJobService` lifecycle bypass simultaneously, and it enables the durable restore checkpoint. The existing `Driver` Strategy at `BackupJobs.java:66-72` is already 50% of the port abstraction this migration requires.

4. **Preserve the domain core verbatim.** The immutable `State` machine, `DataType` type-object enum, typed exception hierarchy, and `Preferences` facade are reference-quality assets. They are not modernization targets; they are the foundation the modernization builds on. The one permitted touch to the preserved core is `State.java:32` — the magic-string k-9 match — which is owned by the `MailTransport` Anti-Corruption Layer cleanup in Phase 2.

5. **Sequence Kotlin as a carrier, not a driver.** WorkManager, StateFlow, and Hilt are Kotlin-first APIs and Kotlin will naturally enter the codebase with Phase 2. A standalone Kotlin big-bang would double-churn the engine. Kotlin follows the seams.

6. **Evaluate the Gmail REST API as a long-term product decision, not an engineering one.** The current IMAP app-password path works but requires a poor user experience. Whether to move to the Gmail REST API is a product scope decision out of scope for the technical modernization.

7. **Consider vendoring the k-9 IMAP library.** The `MailTransport` ACL (Phase 2) makes the k-9 binding swappable. Until a verified versioned coordinate is confirmed, vendoring a minimal IMAP subset is the safest path to reproducible builds on a privacy-critical app.

---

## Evidence Index

The following table maps each major claim, finding, or recommendation in this report to its authoritative source output file. All paths are relative to repository root.

| Claim / Finding | Source File(s) | Key Reference |
|---|---|---|
| compileSdkVersion 29, targetSdkVersion 29, minSdkVersion 14 | `app/build.gradle:10,14,15` | TECH-001 / DEBT-001 |
| AGP 4.1.3, Gradle 7.2 | `build.gradle:8`; `gradle/wrapper/gradle-wrapper.properties` | DEP-005/006 |
| jcenter() declared at lines 4, 21, 25 | `build.gradle:4,21,25` | DEP-006 / TECH-007 |
| maven.scijava.org mirror for firebase-jobdispatcher | `build.gradle:27` | DEP-001 / DEP-002 |
| k-9 SHA pin via JitPack | `app/build.gradle:58` | DEP-004 / TECH-005 |
| firebase-jobdispatcher in 4 files | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/gaps.md` | TECH-002 evidence |
| extends AsyncTask in 3 files | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/gaps.md` | TECH-002 evidence |
| import com.squareup.otto in 12 files | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/gaps.md` | STRUCT-002 / TECH-003 |
| Trust-all TLS (empty checkServerTrusted) | `app/src/main/java/com/zegoggles/smssync/mail/AllTrustedSocketFactory.java:42-57` | SEC-001 / ARCH-003 |
| Silent migration to trust-all | `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java:276-288` | SEC-001 / ARCH-008 |
| PendingIntent without FLAG_IMMUTABLE | `app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java:127`; `service/ServiceBase.java:204` | CAP-002 / QUAL-001 |
| Plaintext credential storage | `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java:221-226` | SEC-002 / ARCH-009 |
| Account-email PII in ungated debug log (token masked) | `app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java:145` | OPS-003 / ARCH-010 |
| Driver Strategy (existing seam) | `app/src/main/java/com/zegoggles/smssync/service/BackupJobs.java:66-72` | CAND-001 / STR-001 |
| k-9 magic-string in domain State | `app/src/main/java/com/zegoggles/smssync/service/state/State.java:32` | STRUCT-001 / ARCH-003 (patterns) |
| Service-as-helper bypass | `app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java:82-84` | STRUCT-003 / ARCH-003 |
| minifyEnabled false | `app/build.gradle:33` | OPS-001 / IF-003 |
| No .github/workflows directory | (confirmed by glob this session) | OPS-001 / IF-004 |
| 107 production Java files, ~10,635 LOC | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/discovery/file-inventory.md` | Scope |
| 36 test files, ~3,550 test LOC | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/discovery/file-inventory.md` | Scope |
| ISO 25010 quality ratings | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/architecture/ilities-assessment.md` | ARCH-001..021 |
| Design pattern inventory, Driver Strategy deep-dive | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/architecture/patterns.md` | ARCH-001..004 anti-patterns |
| Debt items CD/AD/TD/DD/IF, debt heatmap | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/tech-debt/debt-inventory.md` | CD-001..008, AD-001..005 |
| 12 modernization-decision findings (R-disposition, sequencing) | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/assessment.md` | DEBT-001..003, ARCH-001..005, DEP-001..003, OPS-001 |
| Target architecture, ADR-001..008, scheduling contract mapping | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/target-state.md` | Architecture decisions |
| 18 gap findings, dependency matrix, WSJF priority order | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/gaps.md` | TECH-001..007, STRUCT-001..005, CAP-001..003, OPS-001..003 |
| 8 strangler candidates, suitability scores | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/candidates.md` | CAND-001..008 |
| MU-001..MU-011 scope, dependency DAG, file allocations | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md` | Migration units |
| WSJF scores, additive-formula cross-check, topological ordering | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/prioritized-backlog.md` | Prioritization |
| Rewrite vs. refactor analysis, disposition per component | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/approach.md` | Disposition decisions |
| Phased roadmap (Foundation/Quick Wins/Core/Optimization) | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/roadmap.md` | Timeline |
| UI requirements (SCR-MAIN-001, SCR-SP-001, SCR-ADV-001, SCR-DON-001) | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/ui-requirements/` | Phase 2.5 UI |
| Non-visual specs (services, receivers, preferences, notifications) | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/non-visual-specs/non-visual-specs.md` | Phase 2.5 non-visual |
| Code classification (Retain/Refactor/Replace/Retire) | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/code-classification.md` | Phase 2.5 classification |
| Dead-code candidates | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/dead-code-candidates.md` | Phase 2.5 dead code |
| Tribal knowledge gaps | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/tribal-knowledge-gaps.md` | Phase 2.5 knowledge |

---

## Coverage Statement

### Assessment Phases and Steps Executed

| Phase | Status | Step Count | Notes |
|---|---|:---:|---|
| Phase 1: Current State Assessment | Completed | 6 steps (1.1–1.6) | project-overview.md, file-inventory.md, dependencies.md, ilities-assessment.md, patterns.md, debt-inventory.md |
| Phase 2: Future State Definition | Completed | 3 steps (2.1–2.3) | assessment.md (12 findings), target-state.md (ADR-001..008), gaps.md (18 findings) |
| Phase 2.5: Requirements Extraction (conditional) | Completed | 7 steps (2.5.1–2.5.7) | code-classification.md, 4 UI requirement specs, non-visual-specs.md, dead-code-candidates.md, tribal-knowledge-gaps.md, migration-units.md, prioritized-backlog.md |
| Phase 3: Strategy Development | Completed | 2 steps (3.1–3.2) | candidates.md (8 strangler candidates, STR-001..STR-008), approach.md |
| Phase 4: Roadmap Creation | Completed | 2 steps (4.1–4.2) | roadmap.md, modernization-report.md (comprehensive), this synthesize report |
| **Total** | **All phases complete** | **20 steps** | Single repository assessed: sms-backup-plus |

**Single-repo scope:** This assessment covers one repository (`sms-backup-plus`), one Android module (`app`, `com.zegoggles.smssync`), 107 production Java files, approximately 10,635 production LOC, and 36 test files. No cross-repo synthesis was required.

**Phase 2.5 inclusion:** Requirements extraction (Phase 2.5) was conditionally included based on the presence of components warranting rewrite-scope treatment (the AsyncTask workers, the donation subsystem, and several activity classes). The 7 Phase 2.5 steps produced the full requirements corpus that supports any future rebuild decision on scoped components.

---

## Appendix: Key Files Referenced

| File | Description |
|---|---|
| `app/build.gradle` | Primary target for TECH-001 (SDK/AGP uplift), TECH-007 (jcenter removal), CAP-002 (FLAG_IMMUTABLE), OPS-001 (minifyEnabled) |
| `build.gradle` | Root build file; repository declarations including scijava mirror (line 27) and JitPack (line 24) |
| `app/src/main/java/com/zegoggles/smssync/mail/AllTrustedSocketFactory.java` | SEC-001: trust-all TLS no-op trust manager — delete entirely |
| `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` | SEC-001 (migrate()), SEC-002 (getCredentials()), CAP-003 (EncryptedSharedPreferences target) |
| `app/src/main/java/com/zegoggles/smssync/service/BackupJobs.java` | CAND-001: Driver Strategy seam — extend into BackupScheduler port |
| `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` | TECH-002: extends AsyncTask — rewrite as CoroutineWorker |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java` | TECH-002 + CAP-001: AsyncTask rewrite + durable restore checkpoint |
| `app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java:82-84` | STRUCT-003: service-as-helper lifecycle bypass — removed with WorkManager |
| `app/src/main/java/com/zegoggles/smssync/service/state/State.java:32` | STRUCT-001: magic-string k-9 match — sole permitted touch to the preserved core |
| `app/src/main/java/com/zegoggles/smssync/App.java:59,116-134` | STRUCT-002: static Otto bus singleton — wrap behind SyncStateRepository |
| `app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java:145` | OPS-003: account-email PII in ungated debug log (token masked) — gate behind BuildConfig.DEBUG + centralized redaction |
| `app/src/main/AndroidManifest.xml` | android:exported, FGS type, POST_NOTIFICATIONS — required by TECH-001 |

---

## Phase Completion Report
---
artifact_id: 20260529-modernization
phase: synthesize
verdict: PASS
artifact_path: sdlc/analysis/20260529-modernization/reports/modernization-plan.md
assessment_status: in-progress
---
