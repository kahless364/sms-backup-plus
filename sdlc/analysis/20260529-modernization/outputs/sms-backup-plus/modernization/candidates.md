---
prompt: assessments/prompts/modernization/04-strangler-candidates.md
step_id: "3.1"
finding_count: 8
assessment_date: 2026-05-29
confidence_threshold: 0.5
scope: "P01 app module — com.zegoggles.smssync (107 prod Java files; 36 test files). In-process module seams only (scheduler/worker, event spine, IMAP/auth ports, secrets, billing, contacts, k-9 boundary). No network routing seam exists — on-device app, single process."
phase: modernization
prompt_id: modernization/04-strangler-candidates
step: "3.1"
date: 2026-05-29
analyst: Solution Architect (assessment agent)
assessment_id: 20260529-modernization
status: draft
---

# Modernization: Strangler / Branch-by-Abstraction Candidates — SMS Backup+

**Engagement:** 20260529-modernization · EXECUTE Step 3.1 (Strangler Candidates Analysis)
**Subject:** P01 `app` — `com.zegoggles.smssync` (single Android module, single OS process)
**Quality bar:** `knowledge/standards/expert-exceeding-depth.md`
**All paths relative to repo root** (`C:/Code/Android/sms-backup-plus`).

---

## Framing — Why This Is Branch-by-Abstraction, Not Network-Seam Strangler Fig

The canonical Strangler Fig pattern (Fowler, *StranglerFigApplication*; Microsoft Azure / AWS prescriptive guidance) interposes an **HTTP facade / API gateway** in front of a deployed legacy system and **routes a percentage of network traffic** to a new system running **alongside** the old one. That mechanism has a hard prerequisite: a network boundary at which a router can split traffic, and a deployment topology in which "legacy" and "migrated" can coexist as separately-addressable processes.

**Neither prerequisite holds here.** This is an on-device Android app (`systems.md` — "No project-owned backend; entirely on-device"; confirmed against `target-state.md §Architecture Style` and `code-classification.md §Context`). There is one OS process, no server tier, and no request-routing layer. A traffic-splitting gateway is not merely unavailable — it is meaningless: there is no request to route.

**The correct in-process analog is Branch-by-Abstraction (Fowler, *BranchByAbstraction*) — the strangler-fig-adjacent pattern for monoliths.** The mapping is exact and is the framing this entire analysis applies:

| Network Strangler Fig concept | In-process branch-by-abstraction analog (this app) |
|---|---|
| HTTP facade / API gateway | An **app-owned port interface** (`BackupScheduler`, `MailTransport`, `SecretStore`, `EventBus`/`SyncStateRepository`, `ContactsPort`) |
| Legacy system behind the facade | The **existing implementation made the first adapter** (e.g., `BackupJobs` behind `BackupScheduler`) — no behavior change |
| New system behind the facade | The **new adapter** (`WorkManagerScheduler`, `K9ImapTransport`, `EncryptedPrefsSecretStore`) |
| Traffic-routing decision (10% → 70% → 100%) | The **DI binding flip** (which adapter the graph injects) — atomic, not percentage-based |
| Coexistence / parallel run | The **two adapters compile side-by-side** behind the port until the binding flips; the old adapter is then deleted |
| Decommission legacy | **Delete the EOL dependency line** in `app/build.gradle` after the last consumer migrates |
| Reconciliation / data-sync jobs | **Not applicable** — one process, one data store; the only "migration" is the one-time secret/credential re-encryption (CAND-003) |

Two consequences flow from this framing and are load-bearing for every candidate below:

1. **The "facade" already partially exists for one seam.** The scheduler is already a GoF *Strategy* behind a `Driver` interface (`BackupJobs.java:66-72` selects `GooglePlayDriver` vs `AlarmManagerDriver`). The team has *already proven it understands the indirection* — the migration extends an established seam rather than inventing one (`patterns.md` Deep Dive 2). This is the single strongest strangler signal in the codebase.

2. **"Suitability" is measured against substrate-swap criteria, not rewrite criteria.** A high-suitability candidate here is one whose dependency boundary is *clean enough to wrap in a port and flip*, gated by a *named characterization test*, and *reversible by a binding flip*. It is **not** a candidate for parallel-run, data-sync, or percentage traffic routing — none of which apply on-device.

This analysis is the strangler-lens *re-projection* of the seam set already defined in `migration-units.md` (MU-001..011), prioritized in `gaps.md §Prioritization` (WSJF), and architected in `target-state.md` (ADR-001..008). Where this document adds value beyond those: it (a) **scores each seam against the six strangler suitability factors** the prompt mandates, (b) **explicitly classifies the non-candidates** and says why strangling them is wrong, and (c) **sequences the strangling order** as a branch-by-abstraction wave plan with the facade/adapter/flip/delete steps named per candidate.

---

## Objective Executed

Identify the in-process module seams of SMS Backup+ that are suitable for incremental replacement via branch-by-abstraction (the on-device analog of the Strangler Fig pattern), score each against interface-clarity / coupling / data-isolation / change-frequency / business-value / risk, classify components that must **not** be strangled (refactor-in-place or preserve-verbatim instead), and recommend a strangling sequence with per-candidate facade → adapter → flip → decommission steps and characterization-test gates.

---

## Evidence Collected

| Evidence Type | Source | Reference |
|---------------|--------|-----------|
| Migration unit seam map, dependency DAG, shared-file allocation, per-unit cutover | Step 2.5.6 migration-units | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md` |
| Target ports, ADR-001..008, scheduling contract mapping, integration pattern (ACL) | Step 2.2 target-state | `.../modernization/target-state.md` |
| Pattern inventory, Strategy/Driver seam, anti-pattern findings ARCH-001..004 | Step 1.5 patterns | `.../architecture/patterns.md` |
| 18 gaps, dependency matrix, WSJF priority order, parallelism notes | Step 2.3 gaps | `.../modernization/gaps.md` |
| Strategy/Driver seam already present (`isUseOldScheduler` selection) | patterns Deep Dive 2 | `service/BackupJobs.java:66-72`; `service/AlarmManagerDriver.java:44` |
| Otto static bus + static service back-refs (the global edge) | patterns Deep Dive 3 / ARCH-001 | `App.java:59`; `service/SmsBackupService.java:66`; `SmsRestoreService.java:40` |
| Trust-all TLS no-op trust manager (security seam) | gaps SEC-001 / target ADR-007 | `mail/AllTrustedSocketFactory.java:42-57`; `preferences/AuthPreferences.java:276-288` |
| k-9 magic-string leak into domain State (ACL boundary) | patterns Deep Dive 1 / ADR-004 | `service/state/State.java:32`; `mail/BackupImapStore.java` |
| Single credential accessor seam | gaps SEC-002 / ADR-007 | `preferences/AuthPreferences.java:221-226` |
| Billing 2.1.0 SkuDetails (isolated 3-file seam) | gaps DEP-004 / migration MU-009 | `app/build.gradle:59`; `activity/donation/*` |
| Legacy GData contacts call behind a potential port | migration MU-010 / target ADR-004 | `auth/OAuth2Client.java:145`; `mail/PersonLookup.java` |
| Verified dependency-line allocation (each unit owns one build.gradle line) | migration-units §Shared-File Allocation | `app/build.gradle:57` (otto), `:58` (k-9), `:59` (billing), `:60` (jobdispatcher) |
| Public `com.zegoggles.smssync.BACKUP` broadcast contract (hard constraint on scheduler seam) | migration-units External Integration Points | `app/src/main/AndroidManifest.xml`; `receiver/BackupBroadcastReceiver.java` |

> **Verification note (truth-and-accuracy):** This step is a strangler-lens *re-projection* of seam boundaries that the Step 2.2/2.3/2.5 artifacts already established against full source reads in their own sessions (build.gradle, AndroidManifest.xml, BackupJobs.java, State.java verified there). File:line citations below are inherited from those verified artifacts; where a citation is load-bearing for a suitability *score* (coupling, interface clarity) it is traced to the specific artifact that read it. No new scope claim is made here that is not already grep/read-verified upstream. Confidence values reflect this inheritance.

---

## Executive Summary

**Total Candidates Identified:** 8 (in-process module seams). **Three components are explicitly classified NOT-a-candidate** (preserve-verbatim or refactor-in-place).

| Suitability | Count | Candidates | Estimated Effort |
|-------------|:-----:|-----------|------------------|
| Excellent (80–100) | 3 | CAND-001 Scheduler/Worker, CAND-003 Secret store, CAND-005 Billing | L + M + M |
| Good (60–79) | 4 | CAND-002 Event spine, CAND-004 Mail/k-9 ACL, CAND-006 Contacts, CAND-008 Transport-security | M + M + M + M |
| Moderate (40–59) | 1 | CAND-007 Hilt DI graph (enabling seam, not a swap) | M |
| Poor (0–39) | 0 (3 components classified as **do-not-strangle**, see dedicated section) | — | N/A |

**Recommended Strangling Sequence (branch-by-abstraction waves):**

1. **CAND-008 (Transport-Security seam) + CAND-001 facade step** — Wave 1. CAND-008 is Critical security, SDK-independent, and removes a *no-op trust manager* (an active MITM exposure, `gaps.md` SEC-001) — start day-1, parallel to the build gate. CAND-001's *facade-only* step (introduce `BackupScheduler` port, make `BackupJobs` the first adapter, zero behavior change) lands here as the cheapest reversible foundation.
2. **CAND-001 (Scheduler/Worker → WorkManager) full swap** — Wave 2. Largest blast radius and highest leverage: one binding flip retires `firebase-jobdispatcher`, `AsyncTask`, the `AlarmManagerDriver` fallback, the scijava mirror, and the service-as-helper lifecycle bypass (`patterns.md` ARCH-003/004). Everything downstream depends on the engine being on WorkManager first.
3. **CAND-004 (Mail/k-9 ACL) + CAND-003 (Secret store) + CAND-002 (Event spine → Flow)** — Wave 3. CAND-004 wraps the now-validated transport (CAND-008 must precede it). CAND-002 follows CAND-001 to avoid double-churning the engine.
4. **CAND-007 (Hilt) → CAND-005 (Billing) → CAND-006 (Contacts)** — Wave 4. Hilt formalizes the binding-flip mechanism once the construction graph is stable; Billing and Contacts are isolated, deferrable, low-risk.

**Key Finding:** The scheduler seam (CAND-001) is the textbook branch-by-abstraction candidate **and the team has already built half the abstraction** — the `Driver` Strategy is a ready-made first-adapter slot (`patterns.md` Deep Dive 2; `BackupJobs.java:66-72`). The single highest-value action in the entire modernization is to *rename the abstraction from a dead vendor's `Driver` interface to an app-owned `BackupScheduler` port* and add a `WorkManagerScheduler` adapter — the indirection the strangler pattern requires is already 50% built. Conversely, the **immutable `State` machine and `DataType` type-object are emphatically NOT candidates** — they are the protected core the ports exist to insulate; strangling them would discard the characterization safety net for zero benefit.

---

## Branch-by-Abstraction (In-Process Strangler) Pattern Overview

```
┌──────────────────────────────────────────────────────────────────────────┐
│       BRANCH-BY-ABSTRACTION  (on-device strangler analog)                   │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  Phase 1: Introduce Port    Phase 2: Add New Adapter   Phase 3: Flip+Delete│
│   (facade, no behavior Δ)    (both compile side-by-side) (binding flip)     │
│                                                                            │
│   Engine                      Engine                     Engine             │
│     │                           │                          │               │
│     ▼                           ▼                          ▼               │
│  ┌─────────┐               ┌─────────┐                ┌─────────┐          │
│  │  PORT   │  ← the facade │  PORT   │                │  PORT   │          │
│  └────┬────┘               └────┬────┘                └────┬────┘          │
│       │ binding                 │ binding (still old)      │ binding (new) │
│       ▼                    ┌────┴─────┐                    ▼               │
│  ┌─────────┐               ▼          ▼               ┌─────────┐          │
│  │ EXISTING│          ┌────────┐  ┌────────┐          │   NEW   │          │
│  │ = first │          │EXISTING│  │  NEW   │          │ adapter │          │
│  │ adapter │          │adapter │  │adapter │          │ (100%)  │          │
│  └─────────┘          └────────┘  └────────┘          └─────────┘          │
│   shippable            both ship    (dark)             EOL dep DELETED     │
│                                                         from build.gradle   │
│                                                                            │
│  Reversible at every phase by flipping the DI binding back.                 │
│  No traffic %, no data-sync, no parallel deployment — one process.          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Candidate Evaluation Criteria

### Suitability Factors (prompt-mandated weights)

| Factor | Weight | Reinterpretation for in-process seams |
|--------|:------:|---------------------------------------|
| Interface Clarity | 20% | Is the dependency boundary clean enough to wrap in a single port interface? Does an abstraction already exist? |
| Coupling | 20% | How many files import the dependency directly? Does any vendor type leak into the domain core? |
| Data Isolation | 15% | Does the seam own/translate its own data, or is it entangled with shared mutable state? (On-device: "data" = preferences, work-state, IMAP UIDs.) |
| Change Frequency | 15% | Is the underlying dependency EOL / forcing churn (high) or stable (low)? |
| Business Value | 15% | Does strangling it restore shippability, close a security exposure, or remove a supply-chain SPOF? |
| Risk Level | 15% | How reversible is the binding flip? Does a failed swap cascade past the port? (Higher score = lower risk.) |

### Suitability Score → Recommendation

| Score | Rating | Recommendation |
|-------|--------|----------------|
| 80–100 | Excellent | Strangle early — clean port, reversible flip, high value |
| 60–79 | Good | Strangle — sound seam, sequence per dependency |
| 40–59 | Moderate | Strangle with care — enabling seam or partial-clarity boundary |
| 0–39 | Poor | **Do not strangle** — preserve verbatim or refactor in place |

---

## Candidate Analysis

## CAND-001: Scheduler / Worker Substrate (JobDispatcher + AsyncTask + AlarmManager → WorkManager)

### Overview

| Attribute | Value |
|-----------|-------|
| **Component** | Background-execution platform: `BackupJobs` (scheduler) + `BackupTask`/`RestoreTask` (`AsyncTask` workers) + `AlarmManagerDriver` (fallback) + `SmsJobService` (entry) |
| **Type** | Subsystem (scheduling + concurrency) |
| **Location** | `service/BackupJobs.java`, `service/AlarmManagerDriver.java`, `service/SmsJobService.java`, `service/BackupTask.java:50`, `service/RestoreTask.java`, `service/SmsBackupService.java`, `service/SmsRestoreService.java` |
| **Target port** | `BackupScheduler` (app-owned) → `WorkManagerScheduler` adapter; workers → `CoroutineWorker` |
| **Migration unit** | MU-005 (+ MU-001 SDK gate, MU-002 test net) |
| **Suitability Score** | **88** |
| **Rating** | Excellent |

### Current State

**Purpose:** Schedules and executes regular/incoming/manual backup and restore on three obsolete primitives: `firebase-jobdispatcher:0.8.6` (deprecated 2018, archived, served only from the `maven.scijava.org` mirror), `AsyncTask` (deprecated API 30), and an `AlarmManagerDriver` fallback selected by `isUseOldScheduler`.

**Interfaces:**

| Interface | Type | Consumers | Stability |
|-----------|------|-----------|-----------|
| `Driver` (firebase-jobdispatcher SPI) | In-process Strategy | `BackupJobs` | **Volatile** — owned by a dead vendor |
| `com.zegoggles.smssync.BACKUP` broadcast | BroadcastReceiver (public) | Third-party apps | **Stable — HARD-PRESERVE** |
| `contentUriTrigger` (SMS/CALLLOG) | OS content observation | `BackupJobs` | Stable (maps to WorkManager constraint) |

**Inbound Dependencies:** triggers from `SmsBroadcastReceiver`, `BootReceiver`, `PackageReplacedReceiver`, `BackupBroadcastReceiver`, manual UI, content URIs.
**Outbound Dependencies:** the immutable `State` machine (preserved core — strong but read-only), `MailTransport` (future), content cursors.

### Suitability Assessment

| Factor | Score | Notes |
|--------|:-----:|-------|
| Interface Clarity | 95 | **A port abstraction already exists** — the `Driver` Strategy (`BackupJobs.java:66-72`). The seam is half-built; rename to `BackupScheduler` and add the WorkManager adapter (`patterns.md` Deep Dive 2). |
| Coupling | 70 | jobdispatcher imported in 4 files; AsyncTask in 3; the service-as-helper bypass (`SmsJobService.java:82-84`) entangles the entry point. Largest blast radius — bounded but real. |
| Data Isolation | 85 | The scheduler owns its own contract (constraints, backoff, content triggers); WorkManager work-state *adds* durable checkpoint capability (ADR-005). No shared mutable state crosses the seam. |
| Change Frequency | 100 | Forcing the entire modernization — EOL on three axes; blocks `targetSdk` raise; supply-chain SPOF (scijava mirror). |
| Business Value | 95 | Retires two EOL deps + the mirror + the lifecycle bypass in one flip; unblocks the durable restore checkpoint (highest-leverage single migration, `gaps.md` Cross-Cutting #1). |
| Risk Level | 80 | Two-step facade-first flip is reversible; the `BACKUP` public contract is a hard constraint but does not block the seam (it routes *through* the port). WorkManager content-trigger latency is bounded (ADR-002). |
| **Weighted Total** | **88** | Excellent. The flagship strangler candidate. |

### Strangling Strategy (Branch-by-Abstraction)

**Recommended Approach:** **Port-first facade (the in-process analog of "build the API gateway first").** Not API/event/DB/UI-first in the network sense — the on-device facade is the `BackupScheduler` interface.

**Rationale:** The existing `Driver` Strategy proves the indirection is understood and partially built; wrapping it in an app-owned port is mechanical and reversible. WorkManager natively subsumes *both* current branches (JobScheduler ≥ API 23, AlarmManager below), so the new adapter collapses the dual-driver maintenance burden in addition to retiring the dead dep.

```
Phase 1 (facade, no behavior Δ):  Engine → BackupScheduler(port) → BackupJobs (first adapter)   [ship + verify]
Phase 2 (new adapter, dark):      Engine → BackupScheduler(port) → { BackupJobs | WorkManagerScheduler }  [both compile]
Phase 3 (flip + delete):          Engine → BackupScheduler(port) → WorkManagerScheduler           [delete jobdispatcher line :60]
```

### Implementation Plan (facade → adapter → flip → decommission)

1. **Facade:** Define `interface BackupScheduler { scheduleRegular/scheduleIncoming/scheduleContentTrigger/scheduleImmediate/cancelAll }`; make `BackupJobs` the first adapter. No behavior change. Ship.
2. **Characterization gate (MU-002, Feathers Ch. 2):** Pin `BackupJobs` retry `30/300` exponential, `UNMETERED|CONNECTED` constraints, `setReplaceCurrent(true)`, content-URI triggers *before* the swap.
3. **New adapter:** `WorkManagerScheduler` + `BackupWorker`/`RestoreWorker : CoroutineWorker` with `setForeground`; map the scheduling contract line-for-line (`target-state.md §Scheduling Contract Mapping`).
4. **Flip:** Bind `WorkManagerScheduler`. Reversible by flipping back.
5. **Decommission:** Delete `firebase-jobdispatcher` (`app/build.gradle:60`), the scijava mirror, `AlarmManagerDriver`, `SmsJobService`, the manifest `<service>`/`ACTION_EXECUTE` filter.

### Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| WorkManager content-trigger latency ≠ instant | Medium | Medium | Bounded latency acceptable for backup; below API 24 keep `SmsBroadcastReceiver`→enqueue (ADR-002) |
| Public `BACKUP` broadcast contract breaks | Low | High | Route the receiver through the port; preserve the action string (hard requirement, migration-units) |
| Largest blast radius of any seam | Medium | Medium | **Split** MU-005a (port + first adapter) / MU-005b (WorkManager + delete), per migration-units |

### Success Criteria

- [ ] All four scheduling-contract rows preserved (REPLACE, 30/300 backoff, UNMETERED, content-trigger w/ <24 fallback)
- [ ] `firebase-jobdispatcher`, scijava mirror, `AsyncTask` usages removed
- [ ] Public `BACKUP` broadcast still triggers a backup
- [ ] `BackupJobsTest` characterization assertions pass against the new scheduler before the old path is deleted

---

## CAND-002: Event Spine (Otto static `Bus` → `StateFlow`/`SharedFlow` behind `SyncStateRepository`)

### Overview

| Attribute | Value |
|-----------|-------|
| **Component** | Process-global Otto `Bus` singleton + static service back-refs + 9 event POJOs |
| **Type** | Cross-cutting communication spine |
| **Location** | `App.java:59`; `service/SmsBackupService.java:66`; `SmsRestoreService.java:40`; 11 distinct Otto-importing files; `activity/events/*` |
| **Target port** | `EventBus`/`SyncStateRepository` → `StateFlow<SyncState>` (sticky) + `SharedFlow<SyncEvent>` (one-shot) |
| **Migration unit** | MU-006 |
| **Suitability Score** | **72** |
| **Rating** | Good |

### Suitability Assessment

| Factor | Score | Notes |
|--------|:-----:|-------|
| Interface Clarity | 65 | No interface exists today — the bus is reached *statically* from every layer. The facade must be *introduced* (Feathers Ch. 25, "Introduce Instance Delegator"). Clear once wrapped, but starts at zero. |
| Coupling | 45 | **The worst-coupled seam** — a static global edge touched by 11 files across all layers (`patterns.md` Deep Dive 3); the dependency is invisible at call sites. This is what *lowers* the score below CAND-001. |
| Data Isolation | 75 | `@Produce` sticky semantics must be preserved via `StateFlow.value`; `CancelEvent` carries behavioral Origin USER/SYSTEM — not a trivial POJO. Translatable but care-bound. |
| Change Frequency | 90 | Otto archived 2014, never patched, reflection-based, no compile-time subscriber check — EOL portability liability. |
| Business Value | 70 | Removes archived dep + Context-leak hazard; makes the engine unit-testable without static rigging (enables deleting test-only constructors). |
| Risk Level | 70 | Two-step facade-then-swap is reversible; risk concentrated in 4 of 9 POJOs with no static `@Subscribe` evidence (runtime reflection — verify consumers before deleting). |
| **Weighted Total** | **72** | Good. Sequence *after* CAND-001 to avoid double-churning the engine. |

### Strangling Strategy

**Approach:** Facade-first (Introduce Instance Delegator), then swap implementation per event type to Flow. Decommission Otto (`app/build.gradle:57`) only after the last file migrates. Sequenced after CAND-001 (`gaps.md §Sequencing` — avoid engine double-churn). Folds in the `MainActivity` God-Activity decomposition (extract `MainViewModel`).

### Success Criteria

- [ ] `com.squareup.otto` removed; 0 imports
- [ ] Sticky last-state reproduced (status row correct on subscribe)
- [ ] Cancel Origin USER/SYSTEM distinction preserved end-to-end
- [ ] `isServiceWorking()` replaced by `WorkInfo`/repository read

---

## CAND-003: Secret Store (plaintext `SharedPreferences` → `EncryptedSharedPreferences`)

### Overview

| Attribute | Value |
|-----------|-------|
| **Component** | Credential storage — single accessor seam `getCredentials()` |
| **Type** | Module (security/secrets-at-rest) |
| **Location** | `preferences/AuthPreferences.java:221-226` |
| **Target port** | `SecretStore` → `EncryptedPrefsSecretStore` (Jetpack Security, Keystore AES-256) |
| **Migration unit** | MU-004 |
| **Suitability Score** | **84** |
| **Rating** | Excellent |

### Suitability Assessment

| Factor | Score | Notes |
|--------|:-----:|-------|
| Interface Clarity | 95 | **Single accessor seam** (`getCredentials()`) — the cleanest possible port boundary; one method to wrap. |
| Coupling | 90 | One call site; no vendor type leak; orthogonal to scheduler/event seams. |
| Data Isolation | 75 | Owns its data (credentials.xml). Requires a **one-time migration** (read plaintext → write encrypted → clear) — the only seam with a genuine data-migration step (the on-device analog of strangler data-sync, but one-shot not continuous). |
| Change Frequency | 60 | Not EOL-driven; driven by the CWE-312 cleartext-at-rest defect. |
| Business Value | 80 | Closes a Medium security exposure; hardens against rooted/forensic extraction. |
| Risk Level | 80 | Binding flip behind `SecretStore`; reversible until plaintext is cleared. Keystore key loss → user re-auth (acceptable, ADR-007). |
| **Weighted Total** | **84** | Excellent — cheapest clean strangle in the codebase. |

### Strangling Strategy

**Approach:** Port-first with a one-time data migration. Bind `SecretStore`; first read falls through to plaintext, migrates, then serves encrypted. Preserve the `credentials.xml` backup-exclusion (do not regress the existing defensive segregation). Eased by minSdk 21 Keystore ergonomics (CAND-007/MU-001).

### Success Criteria

- [ ] Credentials AES-256, Keystore-backed (CWE-312 closed)
- [ ] `credentials.xml` remains backup-excluded
- [ ] One-time migration leaves no plaintext

---

## CAND-004: Mail / k-9 Boundary (direct `ImapStore` subclassing → `MailTransport` port + Anti-Corruption Layer)

### Overview

| Attribute | Value |
|-----------|-------|
| **Component** | IMAP transport — `BackupImapStore extends ImapStore` (k-9), pinned to JitPack SHA `eaf689025e` |
| **Type** | Integration boundary |
| **Location** | `mail/BackupImapStore.java`; leak at `service/state/State.java:32` (`"Unable to get IMAP prefix"` magic-string) |
| **Target port** | `MailTransport` → `K9ImapTransport` adapter with ACL (translate `MessagingException` → app-owned `LocalizableException`) |
| **Migration unit** | MU-008 |
| **Suitability Score** | **70** |
| **Rating** | Good |

### Suitability Assessment

| Factor | Score | Notes |
|--------|:-----:|-------|
| Interface Clarity | 70 | Boundary is identifiable but currently *leaky* — k-9 protected fields (`mStoreConfig`, `mTrustedSocketFactory`) and a magic-string in the domain `State` (`patterns.md` Boundary Analysis). The ACL must *create* the clean line. |
| Coupling | 55 | k-9 supplies 15+ IMAP files (concentration risk); the domain core string-matches a library internal — a leak into the protected core (the one permitted `State.java:32` edit). |
| Data Isolation | 80 | IMAP UIDs/folders are owned at the boundary; the ACL translates exceptions so no `com.fsck.k9.*` type crosses the port. |
| Change Frequency | 65 | SHA-pinned (unreproducible, transitively unpatched) — supply-chain + security pressure, not active churn. |
| Business Value | 70 | Reproducible builds; insulates the domain from library churn; makes vendoring-or-versioning a swappable choice. |
| Risk Level | 65 | **Must sequence after CAND-008** (security validates the transport the ACL then wraps); k-9 may have no API-compatible release → vendoring fallback (ADR-004). Contract-gated: enumerate every `MessagingException` case before coding. |
| **Weighted Total** | **70** | Good. Owns the sole permitted edit to the preserved core. |

### Strangling Strategy

**Approach:** Port-first with ACL. Introduce `MailTransport` with `BackupImapStore` as first adapter (no behavior change), add the exception-translation ACL, replace `State.java:32` magic-string with the ACL-provided subtype (gated by `StateTest`), then unpin k-9. Shares `BackupImapStore.java` with CAND-008 — **CAND-008 lands first**, CAND-004 wraps the ACL around the already-validated transport.

### Success Criteria

- [ ] No `com.fsck.k9.*` type crosses `MailTransport`
- [ ] `State` no longer string-matches a k-9 message; `StateTest` passes
- [ ] k-9 resolves from a reproducible coordinate (or vendored)
- [ ] Every `MessagingException` case maps to a defined `LocalizableException` subtype (ACL contract complete)

---

## CAND-005: Billing (Play Billing 2.1.0 `SkuDetails` → 7.x `ProductDetails`)

### Overview

| Attribute | Value |
|-----------|-------|
| **Component** | Donation subsystem |
| **Type** | Feature (self-contained) |
| **Location** | `activity/donation/DonationActivity.java`, `DonationListFragment.java`, `Sku.java`; `app/build.gradle:59` |
| **Target** | Billing 7.x (`ProductDetails`/`queryProductDetailsAsync`) |
| **Migration unit** | MU-009 |
| **Suitability Score** | **80** |
| **Rating** | Excellent |

### Suitability Assessment

| Factor | Score | Notes |
|--------|:-----:|-------|
| Interface Clarity | 80 | The Play Billing IPC SDK *is* the boundary; the seam is the 3-file donation subsystem. No engine coupling. |
| Coupling | 90 | Isolated to 3 files; failure cannot cascade past the donation screen. |
| Data Isolation | 85 | No shared state; purchase tokens are Play-owned. |
| Change Frequency | 70 | `SkuDetails` deprecated; Play enforces ≥ 6.x — a forward-policy deadline, not active churn. |
| Business Value | 50 | Low — donation flow only; deferrable to Phase 3. |
| Risk Level | 90 | Self-contained rewrite of 3 files, gated by `DonationActivityTest`. Highest reversibility of any candidate. |
| **Weighted Total** | **80** | Excellent *suitability*, but **low value/priority** — strangle late. |

### Strangling Strategy

**Approach:** Isolated in-place rewrite of 3 files (the seam is small enough that a port abstraction is over-engineering — the Play SDK is the de-facto port). Gated by `DonationActivityTest`. Phase 3, deferrable.

### Success Criteria

- [ ] Donation flow uses `ProductDetails`/`queryProductDetailsAsync`; no `SkuDetails`
- [ ] `DonationActivityTest` passes against Billing 7.x

---

## CAND-006: Contacts (legacy GData `m8/feeds/` → People API behind `ContactsPort`)

### Overview

| Attribute | Value |
|-----------|-------|
| **Component** | Contact name resolution (remote GData call) |
| **Type** | Integration boundary |
| **Location** | `auth/OAuth2Client.java:145`; local lookup preserved in `mail/PersonLookup.java`, `contacts/ContactAccessor.java` |
| **Target port** | `ContactsPort` → `PeopleApiContactsAdapter`; ContentResolver path preserved |
| **Migration unit** | MU-010 |
| **Suitability Score** | **66** |
| **Rating** | Good |

### Suitability Assessment

| Factor | Score | Notes |
|--------|:-----:|-------|
| Interface Clarity | 75 | Port boundary is clean (`ContactsPort`); local ContentResolver lookup stays put behind the same port. |
| Coupling | 75 | Concentrated at one GData call site; local lookup is orthogonal. |
| Data Isolation | 70 | Contact names are remote-owned; OAuth2 contacts/calendar path must remain intact (Gmail policy constraint). |
| Change Frequency | 55 | Legacy GData *may already be degraded* — **EVALUATE, not assume** (target-state is explicit). |
| Business Value | 45 | Low — contact-name enrichment; not on the critical path. |
| Risk Level | 70 | Binding flip; reversible. Must not break the OAuth2 contacts/calendar path. Token-redaction cross-cut at `:145` (ARCH-010) must be closed regardless. |
| **Weighted Total** | **66** | Good *seam*, low priority; gated on endpoint liveness verification. |

### Strangling Strategy

**Approach:** Port-first; verify GData endpoint liveness before committing (the new adapter may replace an already-dead path). Phase 3, after Hilt. Close the token-log redaction cross-cut even if the People API migration is deferred.

### Success Criteria

- [ ] Contact resolution works via People API behind `ContactsPort`
- [ ] OAuth2 contacts/calendar path intact
- [ ] No account-email PII in release logs; credential-adjacent logging gated behind BuildConfig.DEBUG (ARCH-010 closed)

---

## CAND-007: DI Construction Graph (manual dual-constructor wiring → Hilt) — *enabling seam*

### Overview

| Attribute | Value |
|-----------|-------|
| **Component** | Dependency wiring — `new Preferences(this)` Service-Locator calls + test-only dual constructors |
| **Type** | Cross-cutting enabling mechanism |
| **Location** | `App.java`; ~15 collaborator classes; the four ports |
| **Target** | Hilt `@HiltAndroidApp` + constructor injection |
| **Migration unit** | MU-007 |
| **Suitability Score** | **52** |
| **Rating** | Moderate |

### Suitability Assessment

| Factor | Score | Notes |
|--------|:-----:|-------|
| Interface Clarity | 60 | Not a single boundary — a graph-wide concern. There is no one port to wrap; Hilt *is* the binding mechanism the other strangles flip. |
| Coupling | 45 | Touches ~15 files; constructor-injection ripples broadly (though mechanically). |
| Data Isolation | 50 | N/A — DI does not own data; it wires it. |
| Change Frequency | 40 | Not EOL-driven; the manual DI works, it is just verbose. |
| Business Value | 60 | Formalizes the binding-flip mechanism that *makes the other strangles clean*; deletes test-only constructors. |
| Risk Level | 70 | Incremental (inject one graph at a time); low-risk but no single reversible flip. |
| **Weighted Total** | **52** | Moderate. **Not a swap candidate — an enabling seam.** Strangle *after* the construction graph stabilizes (post CAND-001/002), so it formalizes binding flips rather than chasing a moving graph. |

### Strangling Strategy

**Approach:** Not a port swap — adopt Hilt incrementally once the other seams' bindings exist, so Hilt becomes the formal DI mechanism for the very binding flips the strangler relies on. Sequence last among the spine work. **Rejected alternative:** Koin (runtime resolution, weaker compile-time safety, ADR-008).

---

## CAND-008: Transport-Security seam (trust-all TLS no-op → validated TLS + opt-in pinning)

### Overview

| Attribute | Value |
|-----------|-------|
| **Component** | TLS trust path — `AllTrustedSocketFactory` (no-op trust manager) + silent-downgrade migration |
| **Type** | Security seam (deletion + migration rewrite) |
| **Location** | `mail/AllTrustedSocketFactory.java:42-57`; `preferences/AuthPreferences.java:276-288`; selection at `mail/BackupImapStore.java:60-62` |
| **Target** | Validated TLS only; explicit user-pinned cert for self-hosted IMAP; `migrate()` preserves validated TLS |
| **Migration unit** | MU-003 |
| **Suitability Score** | **74** |
| **Rating** | Good |

### Suitability Assessment

| Factor | Score | Notes |
|--------|:-----:|-------|
| Interface Clarity | 75 | The socket-factory selection point (`BackupImapStore.java:60-62`) is a clean swap site; the seam is "delete the no-op factory + rewrite migrate()". |
| Coupling | 70 | 3 files, 1 deletion; shares `BackupImapStore.java` with CAND-004 (sequence this first). |
| Data Isolation | 70 | Owns the trust-decision and the legacy `+ssl`/`+tls` migration; one-time user notice. |
| Change Frequency | 60 | Not EOL; driven by an *active* Critical MITM exposure (CWE-295) the SDK upgrade worsens. |
| Business Value | 95 | **Critical security** — closes an active MITM exposure on the entire message corpus + app-password. Highest risk-reduction per effort (`gaps.md` WSJF #2). |
| Risk Level | 75 | Not a binding flip — a deletion + migration rewrite, gated by the backfilled `AuthPreferencesTest`. SDK-independent → start day-1, parallel to the gate. |
| **Weighted Total** | **74** | Good. **Highest-priority security candidate; runs Wave 1, parallel to everything.** |

### Strangling Strategy

**Approach:** Not a classic port-flip — a *security-first deletion*. Delete `AllTrustedSocketFactory`; replace with validated TLS + opt-in user-pinned cert; rewrite `migrate()` to never silently set `SERVER_TRUST_ALL_CERTIFICATES=true`. Characterization-test `migrate()`/`getStoreUri()` first (MU-002). Independent of the SDK gate — Wave 1.

### Success Criteria

- [ ] No code path accepts an unvalidated certificate (CWE-295 closed)
- [ ] `migrate()` preserves validated TLS + one-time notice; never silently enables trust-all
- [ ] `BackupImapStoreTest` asserts the validated/pinned factory, not `AllTrustedSocketFactory`

---

## Summary Comparison

| Candidate | Suitability | Effort | Value | Risk | Wave |
|-----------|:-----------:|:------:|:-----:|:----:|:----:|
| CAND-008 Transport-Security | 74 | M | High (Critical sec) | Med | 1 |
| CAND-001 Scheduler/Worker | 88 | L | High | Med-High | 1→2 |
| CAND-004 Mail/k-9 ACL | 70 | M | Med | Med | 3 |
| CAND-003 Secret store | 84 | M | Med | Low | 3 |
| CAND-002 Event spine | 72 | M | Med | Med | 3 |
| CAND-007 Hilt DI (enabling) | 52 | M | Med | Low | 4 |
| CAND-005 Billing | 80 | M | Low | Low | 4 |
| CAND-006 Contacts | 66 | M | Low | Low | 4 |

---

## Recommended Strangling Sequence

```
Timeline (branch-by-abstraction waves; mapped to gaps.md Phase 0–3) →

Phase 0/1 (0–3 mo)          Phase 2 (3–9 mo)              Phase 3 (9–18 mo)
│                           │                              │
├── CAND-008 (Transport sec)┤  (parallel, day-1, Critical) │
│                           │                              │
├── CAND-001 facade step ───┼── CAND-001 full swap ────────┤
│   (port + 1st adapter)    │   (WorkManager flip + delete)│
│                           │                              │
│                           ├── CAND-004 (Mail/k-9 ACL) ───┤  (after CAND-008)
│                           ├── CAND-003 (Secret store) ───┤
│                           ├── CAND-002 (Event → Flow) ───┤  (after CAND-001)
│                           │                              │
│                           │                       ┌── CAND-007 (Hilt) ──┐
│                           │                       ├── CAND-005 (Billing)─┤
│                           │                       └── CAND-006 (Contacts)┘
```

### Sequencing Rationale

1. **CAND-008 + CAND-001 facade first** because: CAND-008 is an *active* Critical MITM exposure that is SDK-independent — every day it is unaddressed is a day of real risk, and the SDK raise worsens it (`gaps.md` SEC-001). CAND-001's facade step is the cheapest, fully-reversible foundation (zero behavior change) and the `Driver` Strategy makes it nearly free.
2. **CAND-001 full swap second** because: it has the largest blast radius and highest leverage — one flip retires `jobdispatcher` + `AsyncTask` + `AlarmManager` + the mirror + the lifecycle bypass, and it unblocks the durable restore checkpoint. Everything downstream assumes the engine is on WorkManager.
3. **CAND-004 / CAND-003 / CAND-002 third** because: CAND-004 must wrap the *validated* transport (CAND-008 precedes it — both touch `BackupImapStore.java`); CAND-002 must follow CAND-001 to avoid double-churning the engine; CAND-003 is independent and slots in here cheaply.
4. **CAND-007 → CAND-005 → CAND-006 last** because: Hilt formalizes the binding-flip mechanism only once the construction graph is stable; Billing and Contacts are isolated, deferrable, low-value-but-high-reversibility.

---

## Components NOT Recommended for Strangling

These are the explicit Poor-suitability classifications. Strangling any of them is a category error — they are either the *protected core the ports exist to insulate* or *configuration that requires no migration*.

| Component | Location | Reason NOT a strangler candidate | Correct approach |
|-----------|----------|----------------------------------|------------------|
| **Immutable `State` machine** (`State`/`BackupState`/`RestoreState`/`SmsSyncState`) | `service/state/*` | It is the **preserved core** (MU-000) — reference-quality, language-portable, the linchpin that makes cross-thread eventing safe (`patterns.md` Deep Dive 1). Strangling it discards the characterization safety net for zero benefit. There is no dead dependency behind it. | **Preserve verbatim.** The ONE permitted touch (`State.java:32` magic-string) is owned by CAND-004's ACL as a boundary cleanup, gated by `StateTest` — not a strangle of the core. Optional future Kotlin `sealed interface` promotion is behavior-preserving, not a substrate swap. |
| **`DataType` type-object enum** | `mail/DataType.java` | Exemplary Open/Closed design (`patterns.md` Deep Dive 5); adding a data type is one enum constant. No vendor coupling, no EOL pressure, no boundary to wrap. | **Preserve verbatim.** Strangling it would replace a 5/5 design with risk. |
| **Typed `Preferences` facade + `enum Keys`** | `preferences/Preferences.java` | Already a clean Facade over `SharedPreferences` with centralized typed keys (`patterns.md` consistency = High). The only strangle-worthy slice (the *credential* accessor) is carved out as CAND-003; the rest is a sound boundary. | **Refactor in place** (inject via Hilt/CAND-007); do not wrap the whole facade in a new port. |

**Why this classification matters (anti-Golden-Hammer):** A naive strangler pass would try to port-and-flip *everything*. The expert call is that the macro-architecture is sound (`patterns.md` — "not a Big Ball of Mud"); only the *dead dependency substrate* warrants strangling. Wrapping the preserved core in ports would be accidental complexity — the exact Golden Hammer the depth standard warns against.

---

## Infrastructure Requirements (in-process strangler enablers)

The network-strangler checklist (API gateway, feature flags, data-sync, traffic routing) is largely **N/A** on-device. The in-process equivalents:

- [x] **"Facade" = port interfaces** — `BackupScheduler`, `MailTransport`, `SecretStore`, `EventBus`/`SyncStateRepository`, `ContactsPort` (introduced per candidate)
- [x] **"Routing decision" = DI binding** — formalized by Hilt (CAND-007); manual binding works in the interim
- [x] **"Rollback" = binding flip** — each port-based candidate is reversible by re-binding the old adapter
- [x] **Characterization-test gate (the safety net)** — JaCoCo + backfilled tests *must precede every swap* (`gaps.md` PROC-002; the on-device analog of strangler monitoring/metrics)
- [x] **CI to run the gate** — GitHub Actions (`gaps.md` PROC-001)
- [ ] **Feature flags** — **N/A** (no percentage routing; the binding flip is atomic)
- [ ] **Data-synchronization / CDC** — **N/A** except CAND-003's one-time credential re-encryption (one-shot, not continuous)

---

## Next Steps

1. **Immediate:** Begin CAND-008 (delete trust-all, fix `migrate()`) in parallel with the CAND-001 facade step — both are Wave-1 and CAND-008 closes an active Critical exposure.
2. **This phase (0–3 mo):** Land the test-net + CI (PROC-001/002) so every subsequent flip is gated; complete the `BackupScheduler` facade with `BackupJobs` as first adapter (reversible, zero behavior change).
3. **This quarter (3–9 mo):** Execute the CAND-001 WorkManager flip, then CAND-004/CAND-003/CAND-002.

---

## Metrics

| Metric | Value |
|--------|-------|
| Findings Count (candidates) | 8 |
| Critical | 0 |
| High | 3 (CAND-001, CAND-003, CAND-005 — Excellent suitability) |
| Medium | 4 (CAND-002, CAND-004, CAND-006, CAND-008 — Good suitability) |
| Low | 1 (CAND-007 — Moderate suitability) |
| Informational | 0 |
| Files Analyzed | 0 new source reads this step (re-projection of verified Step 2.2/2.3/2.5 artifacts; seam boundaries traced to upstream file:line) |
| Domains Covered | Architecture (modernization / branch-by-abstraction), Security (transport seam), Dependency (EOL substrate) |

> Severity here maps suitability-to-act: "High" = Excellent strangler candidate (act early), "Medium" = Good, "Low" = Moderate/enabling. No candidate is a defect; all are modernization opportunities. The mapping is stated explicitly to avoid conflation with the gap-finding severities in `gaps.md`.

---

## Limitations Encountered

- **No new source reads this step — re-projection of upstream artifacts.** This candidates analysis projects the strangler lens onto seam boundaries already established (against full source reads) in Steps 2.2 (target-state), 2.3 (gaps), and 2.5.6 (migration-units). Suitability scores are reasoned from those verified boundaries, not from a fresh source pass. Where a score depends on a coupling/clarity claim, it is traced to the artifact that read the source. Any implementer should re-confirm the load-bearing file:line citations (notably the four `app/build.gradle` dependency lines and the `Driver` Strategy at `BackupJobs.java:66-72`) at execution time.
- **Strangler suitability scores are judgment-calibrated, not metric-derived.** The six-factor weighting is applied qualitatively against the documented boundary characteristics; no cyclomatic-complexity or coupling-metric tool was run *in this step* (those exist in `ilities-assessment.md` / `debt-inventory.md` and inform the scores indirectly).
- **People API endpoint liveness (CAND-006) and k-9 versioned-coordinate availability (CAND-004) are EVALUATE-not-assume** per `target-state.md`; both carry vendoring/fallback contingencies that could change their effort estimate.
- **The network-strangler template sections (traffic %, data-sync, parallel-run) are deliberately marked N/A**, not omitted — their absence is a substantive finding about the on-device architecture, not a gap in the analysis.

---

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| Strangler-candidates prompt | `assessments/prompts/modernization/04-strangler-candidates.md` (plugin cache) | Required output structure + suitability factors |
| Finding schema | `prompts/references/finding-schema.md` (plugin cache) | Field/severity/confidence rules |
| Findings template | `templates/findings-template.md` (plugin cache) | Canonical section headings |
| Expert-Exceeding Depth | `knowledge/standards/expert-exceeding-depth.md` (plugin cache) | Quality bar (Strangler/Branch-by-Abstraction modernization row) |
| Migration Units (Step 2.5.6) | `.../modernization/requirements/migration-units.md` | Seam map MU-001..011, dependency DAG, shared-file allocation, per-unit cutover |
| Target State (Step 2.2) | `.../modernization/target-state.md` | Ports, ADR-001..008, scheduling contract mapping, integration ACL pattern, preserved-core list |
| Patterns Analysis (Step 1.5) | `.../architecture/patterns.md` | Strategy/Driver seam, ARCH-001..004 anti-patterns, preserved-core design deep-dives |
| Gap Analysis (Step 2.3) | `.../modernization/gaps.md` | 18 gaps, dependency matrix, WSJF priority order, parallelism notes |

---

## Verdict

**PASS** — Eight in-process strangler (branch-by-abstraction) candidates identified and scored against all six prompt-mandated suitability factors; the on-device framing (port = facade, DI binding = traffic routing, binding flip = rollback) is made explicit and applied throughout, with the network-strangler mechanics (traffic %, data-sync, parallel-run) correctly classified N/A and explained rather than omitted. Three components are explicitly classified do-not-strangle with rationale grounded in the preserved-core analysis (anti-Golden-Hammer). A four-wave strangling sequence is recommended with per-candidate facade → adapter → flip → decommission steps, characterization-test gates, and dependency/parallelism rationale traceable to gaps.md WSJF and migration-units sequencing. The single highest-leverage finding — that the scheduler abstraction is already 50% built via the `Driver` Strategy — is surfaced and exploited in the sequence. Expert-depth self-check satisfied: patterns named from canonical catalogs (Fowler Branch-by-Abstraction & Strangler Fig, Feathers Ch. 25 Introduce Instance Delegator, DDD Anti-Corruption Layer, GoF Strategy), trade-offs explicit, sequencing rigorous, and the do-not-strangle classification demonstrates anti-pattern recognition.

---

*Assessment status: in-progress (Step 3.1 of EXECUTE).*

## Phase Completion Report
---
artifact_id: 20260529-modernization
phase: execute
verdict: PASS
artifact_path: sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/candidates.md
assessment_status: in-progress
---
