---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-005
related_stories: []
related_design_docs: []
integration_contracts:
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-005
change_records: []
type: ''
id: DES-MODERNIZATION-005
title: WorkManager Scheduler Spine Design
domain: modernization
---

# DES-MODERNIZATION-005: WorkManager Scheduler Spine Design

## Overview

This design replaces the four overlapping background-execution mechanisms in SMS Backup+
with a single WorkManager spine driven by `CoroutineWorker`, behind an app-owned
`BackupScheduler` port. It realizes REQ-MODERNIZATION-005 (migration unit MU-005, "THE
SPINE") — the central, highest-risk migration of the engagement, in which one binding
flip retires four dead mechanisms at once while preserving every current scheduling
behavior and the public `com.zegoggles.smssync.BACKUP` broadcast contract.

The four mechanisms, **verified against current source**, are:

| Mechanism | Source (verified) | Role today |
|-----------|-------------------|------------|
| Firebase JobDispatcher | `app/src/main/java/com/zegoggles/smssync/service/BackupJobs.java` (`FirebaseJobDispatcher` + `GooglePlayDriver`) | Primary scheduler when Play Services present |
| `JobService` lifecycle bridge | `app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java` (extends `com.firebase.jobdispatcher.JobService`) | `onStartJob` manually instantiates `SmsBackupService` to dodge the API-26 background-start ban — so `onCreate()` never runs |
| AlarmManager fallback driver | `app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java` (implements `com.firebase.jobdispatcher.Driver`) | Selected when `Preferences.isUseOldScheduler()` is true |
| `AsyncTask` execution | `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java`, `.../RestoreTask.java` (extend `android.os.AsyncTask`) | Off-main-thread backup/restore loop; progress via the Otto event bus (`App.post` / `@Subscribe`) |

## Context

Why this design is needed (per REQ-MODERNIZATION-005 and the source assessment):
`firebase-jobdispatcher` is deprecated and is served **only** from the non-Google
`maven.scijava.org` mirror — a supply-chain risk, verified at
`build.gradle:26-27`. `AsyncTask` is removed in API 33 — verified at
`BackupTask.java:50` and `RestoreTask.java:48` (both `extends AsyncTask`). The
`SmsJobService` lifecycle bypass is a structural hazard — verified at
`SmsJobService.java:82-84`, where it does `new SmsBackupService(); attachBaseContext(this);
handleIntent(...)`. `AlarmManagerDriver` is a third parallel scheduling path — verified
at `AlarmManagerDriver.java:44`. WorkManager natively subsumes `JobScheduler` (API 23+)
and `AlarmManager` (below 23), owns worker lifecycle plus foreground promotion, and
provides durable work state that is the prerequisite for a resumable restore checkpoint.

The existing `Driver` Strategy at `BackupJobs.java:66-72` (the `isUseOldScheduler()`
ternary selecting `AlarmManagerDriver` vs `GooglePlayDriver`) is already half of the
`BackupScheduler` port this design requires — confirming candidates.md's CAND-001
assessment.

This design depends on and conforms to: target-state.md (BackupScheduler port, the
program-level WorkManager ADR, the JobDispatcher→WorkManager contract-mapping table),
migration-units.md (MU-005, the four invariants, broadcast-contract detail, rollback
story), approach.md (strangler-fig + branch-by-abstraction, AsyncTask→CoroutineWorker
concern split, debug parallel-run), candidates.md (CAND-001 Driver Strategy), DES-001
(shared library that hosts the scheduling-independent backup/restore loop), DES-003
(Gate G2 tests green before cutover), and DES-008 (Hilt `@HiltWorker` wiring, traces to
REQ-MODERNIZATION-008). Verified build facts: `firebase-jobdispatcher` + scijava mirror in
`build.gradle`; AGP `4.1.3` (REQ-001 raises this for WorkManager 2.9.x+).

## Design

## System Architecture

### BackupScheduler port (CAND-001 realized)

A single hexagonal port owns all scheduling. Per target-state.md's module topology it
lives in the shared core module (DES-001) with no `com.firebase.*` or WorkManager types in
its signature, so the program logic that decides *when and how* to schedule is unit-testable
off-device. Android-specific construction (`Constraints`, `WorkRequest`, `BackoffPolicy`)
lives entirely in the adapter.

```
core (no Android scheduler types in signature)
  port: BackupScheduler
    scheduleIncoming()                  // one-off, delay = incomingTimeoutSecs
    scheduleRegular()                   // periodic / recurring, constraint-bound
    scheduleContentTrigger()            // recurring content-URI observer (API 24+)
    scheduleBootup()                    // honors isAutoBackupEnabled(); cancelAll() if off
    scheduleImmediate()                 // one-off NOW, BROADCAST_INTENT, no constraint
    scheduleRestore(config)             // one-off restore (NEW — no scheduler today)
    cancelAll()                         // cancelRegular() + cancelContentUriTrigger()
    cancelRegular()
    cancel(jobKind)                     // single-flight cancellation (AC-2/AC-3)
    observe(jobKind): Flow<SchedulerState>   // observability
```

The surface is derived **directly** from the public methods verified in `BackupJobs.java`:
`scheduleIncoming` (74), `scheduleRegular` (78), `scheduleContentTriggerJob` (82),
`scheduleBootup` (86), `scheduleImmediate` (99), `cancelAll` (103), `cancelRegular` (108) —
plus restore scheduling (AC-6, new), cancellation, and observation. Every caller depends on
the port, never on a concrete mechanism (AC-1). This is CAND-001's driver strategy: once all
call sites use the port, the JobDispatcher→WorkManager swap is a binding change plus a worker
implementation, not a call-site rewrite.

> Verified caller note: the only constructor call sites of `BackupJobs` are
> `new BackupJobs(this)` inside `SmsJobService.getBackupJobs()` (`SmsJobService.java:147`)
> and the public `BackupJobs(Context)` constructor used by external entry points (boot
> receiver, broadcast receiver, settings). Each must be migrated to depend on the injected
> port. Scope assumed for the broadcast/boot receivers — not yet grep-verified across the
> full tree; to be confirmed at implementation planning time.

### WorkManager + CoroutineWorker: four mechanisms collapse to one, flipped once

`WorkManagerScheduler` (in the app module) is the sole production implementation of the
port. It collapses all four legacy mechanisms behind one enqueue path:

| Legacy mechanism | Subsumed by | How |
|------------------|-------------|-----|
| Firebase JobDispatcher (scheduling) | `WorkManager.enqueue*` | `PeriodicWorkRequest` (regular, content-trigger) / `OneTimeWorkRequest` (incoming, bootup, immediate, restore) |
| AlarmManager fallback driver | WorkManager internal scheduler | WorkManager selects `JobScheduler` (API 23+) or its own `AlarmManager`+`BroadcastReceiver` implementation (API 14–22); the `isUseOldScheduler()` driver-selection ternary (`BackupJobs.java:68-71`) is **deleted, not ported** |
| `SmsJobService` lifecycle bridge | `CoroutineWorker.doWork()` | The worker *is* the execution context; there is no separate `JobService` that hand-instantiates `SmsBackupService` (eliminating the `SmsJobService.java:82-84` bypass) |
| `BackupTask`/`RestoreTask` (`AsyncTask`) | `BackupWorker`/`RestoreWorker` (`CoroutineWorker`) | The backup/restore loop moves to core (DES-001) as suspending functions; the worker is a thin adapter that calls core logic and reports progress through WorkManager state |

This is the "four mechanisms, one flip" property (AC-1, AC-2): because every call site already
routes through the port, cutover is a single change to the bound implementation
(`LegacyScheduler` → `WorkManagerScheduler`). The `SmsJobService` lifecycle bypass disappears
entirely — there is no longer an `onStartJob` that starts a foreground `Service`; the
`CoroutineWorker` runs the work directly, and where a user-visible notification is required it
uses `setForeground` / `getForegroundInfo`.

> Behavioral preservation note — the content-trigger follow-up. Today
> `SmsJobService.onStartJob` (`SmsJobService.java:72-77`), when triggered by the
> `contentTrigger` job, does NOT back up directly — it calls `scheduleIncoming()` to enqueue
> a delayed follow-up. WorkManager content-URI triggers reproduce this two-stage behavior:
> the content-trigger work enqueues the delayed incoming-backup work; the design must keep the
> debounce (delay = `getIncomingTimeoutSecs()`), not back up on the raw content change.

### Branch-by-abstraction parallel-run (debug only)

Per approach.md and the MU-005 rollback story, a debug-only composite binding wraps both
drivers:

```
CompositeScheduler (debug builds only)
  delegates to LegacyScheduler  (wraps today's BackupJobs machinery, unchanged)
  delegates to WorkManagerScheduler  (the target)
  logs divergence in enqueued constraints, backoff, and trigger timing
```

Never present in release builds. It validates that the WorkManager adapter produces
behaviorally-equivalent scheduling before the binding flip. Rollback is the reverse flip of
the bound implementation; because the port surface is identical, rollback requires no
call-site change.

### Durable restore checkpoint via WorkManager state

`RestoreTask` today is an `AsyncTask` with no durability: process death loses all progress.
Notably, `RestoreConfig` already carries `currentRestoredItem` (read at `RestoreTask.java:100`,
advanced in the loop at `:119`) and `retryWithStore(currentRestoredItem, ...)` is used for the
in-process OAuth retry path (`:165`) — so the restore loop is already cursor-shaped; it is
simply not **durable** across process death. The `RestoreWorker` design makes restore resumable
(AC-6) by combining two WorkManager guarantees:

1. **Guaranteed re-execution.** A `OneTimeWorkRequest` that has not reached a terminal state is
   re-run by WorkManager after process death/reboot. The worker needs no restart machinery of
   its own.
2. **Durable checkpoint.** The worker persists the restore cursor (the `currentRestoredItem`
   index already tracked in `RestoreConfig`, promoted to durable storage) via `setProgressAsync`
   (observable, for UI) **and** to a durable store in core, written *after* each successful
   provider insert and *before* advancing. On re-execution the worker reads the checkpoint and
   resumes from the next uncommitted item.

Idempotency (no duplicate rows) is enforced at the write seam, not by the scheduler. The
existing dedup guards are **verified present**: `smsExists(values)` keys on `date + address +
type` (`RestoreTask.java:308-327`) and `callLogExists(values)` keys on `date + number +
duration + type` (`:288-306`); inserts are gated on `!smsExists(...)` (`:259`) and
`!callLogExists(...)` (`:280`). The checkpoint guarantees committed items are never
re-attempted; the existing dedup guard guarantees that even an item committed-but-not-yet-
checkpointed (the crash window between provider insert and cursor advance) cannot duplicate.
Both are required because that window is non-zero.

### ADR-005-A — WorkManager subsumes JobScheduler **and** AlarmManager

**Status.** Accepted (realizes the program-level WorkManager ADR in target-state.md).

**Decision.** Adopt AndroidX WorkManager as the single background-execution API. Do not port
the `GooglePlayDriver` / `AlarmManagerDriver` selection logic (`BackupJobs.java:68-71`).
WorkManager internally chooses `JobScheduler` on API 23+ and its own
`AlarmManager`+`BroadcastReceiver` implementation on API 14–22 — covering the project's API-14
floor (`app/build.gradle:14`; raised to 21 later by MU-011/REQ-MODERNIZATION-006, a separate
not-yet-landed change) without a hand-written fallback driver. WorkManager's own minimum is also
API 14, so the entire current floor is covered.

**Consequences.** `AlarmManagerDriver.java`, `GooglePlayDriver` usage, the
`com.firebase:firebase-jobdispatcher` dependency, and the `maven.scijava.org` repository entry
(`build.gradle:27`) are all removed (AC-5). Dependence on Google Play Services for scheduling
is eliminated. Add `androidx.work:work-runtime-ktx` (2.9.x+, gated on the AGP/SDK bump in
REQ-001).

**Alternatives rejected.** (a) Keep `AlarmManager` directly for pre-23 and `JobScheduler` for
23+ — reintroduces the exact dual-mechanism fragmentation MU-005 exists to remove.
(b) `androidx.work:work-multiprocess` — no multi-process requirement; rejected as unnecessary
surface.

### ADR-005-B — CoroutineWorker rewrite of the execution model

**Status.** Accepted (realizes approach.md "AsyncTask → CoroutineWorker").

**Decision.** Replace `BackupTask` / `RestoreTask` (`AsyncTask`) with `BackupWorker` /
`RestoreWorker` extending `CoroutineWorker`. Per approach.md's concern split, the backup/restore
*loop* becomes plain suspending functions in core (DES-001); the worker owns only threading,
lifecycle, constraint/result mapping, and progress emission.

**Consequences.** Structured cancellation replaces the `AsyncTask.cancel(true)` + `isCancelled()`
polling verified in both tasks (`BackupTask.java:117,234,267`; `RestoreTask.java:78,119,206`):
cancellation cooperatively cancels the coroutine, which unwinds the suspending loop at the next
suspension point. Progress reporting moves off the **Otto** event bus (verified
`com.squareup.otto` imports and `App.register`/`App.post`/`@Subscribe` in `BackupTask.java:12,110,
113,255` and `RestoreTask.java:18,74,77,213`) to WorkManager `setProgress` + an observable
`SchedulerState`/`BackupState` flow. This intersects DES-007 (Otto→StateFlow) — the progress
channel is migrated as part of that work; this design only requires that the worker emit through
the new observable channel, not Otto. The deprecated `AsyncTask` is removed (AC-5).

**Alternatives rejected.** Plain `Worker` (blocking `doWork`) — loses structured cancellation and
forces manual thread management. Hand-rolled `ListenableWorker` — more boilerplate, no benefit
over `CoroutineWorker`.

### Traceability to REQ-MODERNIZATION-005

| REQ element | Design realization |
|-------------|--------------------|
| AC-1 single port, WM only adapter | `BackupScheduler` port; all call sites depend on it; one binding flip |
| AC-2 CoroutineWorker, no lifecycle bypass | `BackupWorker`/`RestoreWorker`; `SmsJobService` bypass deleted |
| AC-3 (a) REPLACE semantics | `ExistingWorkPolicy.REPLACE` / unique periodic work (Integration Design) |
| AC-3 (b) 30s/300s exponential backoff | `BackoffPolicy.EXPONENTIAL`, 30s initial / 300s cap (Integration Design) |
| AC-3 (c) UNMETERED/ANY_NETWORK per type | network-constraint mapping incl. empty-constraint for immediate (Integration Design) |
| AC-3 (d) content-URI trigger + pre-24 fallback | `addContentUriTrigger` (24+) / delayed one-off (14–23) |
| AC-4 public broadcast preserved | `com.zegoggles.smssync.BACKUP` → `scheduler.scheduleImmediate()`; ADB test |
| AC-5 removals | ADR-005-A/B; build + source removal; grep gate |
| AC-6 durable resumable restore | checkpoint + guaranteed re-execution + existing dedup guard |

## Architecture

See **System Architecture** above for the component decomposition (port, adapter, workers,
composite debug binding). The runtime topology after cutover: external triggers (broadcast
receiver, boot receiver, settings UI, content-URI observer) → `BackupScheduler` port →
`WorkManagerScheduler` adapter → `WorkManager` → `BackupWorker` / `RestoreWorker`
(`CoroutineWorker`) → core suspending backup/restore use-cases → IMAP store + SMS/call-log
content providers, with progress surfaced via WorkManager `WorkInfo` → `SchedulerState` flow.

## Components

| Component | Module | Responsibility |
|-----------|--------|----------------|
| `BackupScheduler` (port) | core | Scheduling contract; no Android scheduler types in signature |
| `WorkManagerScheduler` (adapter) | app | Sole production impl; maps port calls to `WorkRequest`s; version-branches content-URI trigger |
| `LegacyScheduler` (adapter) | app (debug) | Wraps today's `BackupJobs` machinery for parallel-run only |
| `CompositeScheduler` | app (debug) | Delegates to both adapters; logs divergence |
| `BackupWorker` | app | `@HiltWorker` `CoroutineWorker`; calls core backup use-case; emits progress |
| `RestoreWorker` | app | `@HiltWorker` `CoroutineWorker`; durable checkpoint; calls core restore use-case |
| backup/restore use-cases | core | Suspending loop extracted from `BackupTask`/`RestoreTask` |
| `SchedulerConfig` (value object) | core | Constraint + backoff config (see Interfaces) |

## Interfaces

## Integration Design

### JobDispatcher → WorkManager contract mapping (the load-bearing detail)

This extends target-state.md's program-level mapping with the **verified** source values, so
implementation is a mechanical translation rather than a reinterpretation. Left column = exact
construct in current source (line cited).

| Current source (verified) | WorkManager equivalent | Invariant |
|---------------------------|------------------------|-----------|
| `setReplaceCurrent(true)` on **every** job (`createBuilder`, `BackupJobs.java:188`) | one-off: `ExistingWorkPolicy.REPLACE`; periodic: `enqueueUniquePeriodicWork(name, REPLACE/UPDATE, …)` with a stable unique name per `BackupType` | INV-1 single-flight |
| `setRecurring(backupType.isRecurring())` (`:163`); content-trigger `setRecurring(true)` (`:171`) | `PeriodicWorkRequest` for recurring types; `OneTimeWorkRequest` otherwise | timing |
| `Trigger.NOW` when `inSeconds <= 0` (`:162`) | `OneTimeWorkRequest` with no initial delay | — |
| `Trigger.executionWindow(inSeconds, inSeconds)` — **degenerate window, start == end** (`:162`) | `setInitialDelay(inSeconds, SECONDS)` (one-off) / `PeriodicWorkRequest(interval)` (recurring) | timing |
| `Trigger.contentUriTrigger(observedUris())` over SMS + (optional) call-log providers (`:170,177-184`) | `Constraints.addContentUriTrigger(uri, triggerForDescendants=true)` per observed URI (API 24+); see fallback below | INV-4(d) |
| `newRetryStrategy(RETRY_POLICY_EXPONENTIAL, 30, 300)` — **30 = initial backoff, 300 = maximum/cap** (`:205`) | `setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, SECONDS)`; WorkManager caps backoff at `MAX_BACKOFF_MILLIS` (≈5 h), so the 300s ceiling is enforced at the worker by capping retry scheduling at 300s | INV-3 backoff fidelity |
| `ON_UNMETERED_NETWORK` when `isWifiOnly()` (`:199`) | `Constraints.setRequiredNetworkType(NetworkType.UNMETERED)` | INV-2 |
| `ON_ANY_NETWORK` otherwise (`:199`) | `Constraints.setRequiredNetworkType(NetworkType.CONNECTED)` | INV-2 |
| `BROADCAST_INTENT` → `new int[0]` — **no network constraint for immediate backup** (`jobConstraints`, `:197`) | `Constraints.NONE` (no network requirement) for `scheduleImmediate` | INV-2 |
| `Lifetime.FOREVER` (recurring) / `UNTIL_NEXT_BOOT` (one-off) (`:164`) | WorkManager work survives reboot by default; one-off `UNTIL_NEXT_BOOT` semantics are emulated by enqueueing at boot, not by a lifetime flag | NFR durability |
| `GooglePlayDriver` / `AlarmManagerDriver` selection (`:68-71`) | *deleted* — WorkManager selects the platform scheduler internally | AC-5 |

**Honesty notes (verified-vs-asserted) — corrections from reading the real source:**

- **The retry strategy is a SINGLE strategy with `(initial=30, maximum=300)`** — `newRetryStrategy
  (RETRY_POLICY_EXPONENTIAL, 30, 300)` at `BackupJobs.java:205`, whose Firebase signature is
  `(policy, initialBackoffSeconds, maximumBackoffSeconds)`. AC-3(b)'s phrase "30s/300s exponential
  backoff" therefore means **initial 30s growing exponentially, capped at 300s** — it is NOT "30s
  for backup, 300s for restore." There is no restore scheduler today, so restore has no current
  backoff value at all. INV-3 must assert `EXPONENTIAL`, `backoffDelayDuration == 30s`, and that
  effective retry delay is capped at 300s. (This corrects a plausible misreading.)
- **Immediate backup has NO network constraint.** `jobConstraints` returns `new int[0]` for
  `BROADCAST_INTENT` (`:197`). The WorkManager mapping for `scheduleImmediate` must use
  `Constraints.NONE`, not `CONNECTED`. INV-2 must encode this per-type difference.
- **The execution window is degenerate** (`executionWindow(inSeconds, inSeconds)`, `:162`), not a
  10% flex window. The mapping is a single `setInitialDelay(inSeconds)`; no flex window to
  preserve. The `AlarmManagerDriver` path confirms this — it reads only `getWindowStart()`
  (`AlarmManagerDriver.java:134-135`).
- **No charging constraint exists anywhere.** `jobConstraints` (`:195-201`) emits network only.
  INV-2 must assert charging is **not** required, to avoid silently changing semantics.
- **Progress bus is Otto, not greenrobot.** Verified `com.squareup.otto` imports in both tasks.
  The migration off Otto is owned by DES-007; this design requires the worker to emit through the
  new observable channel.

### Incoming-SMS trigger (AC-3d)

Today this is two cooperating jobs: a recurring `contentTrigger` job
(`scheduleContentTriggerJob`, `BackupJobs.java:82,168-175`) observing the SMS provider (and the
call-log provider when call-log-after-call is enabled, `:180-181`), and a delayed `INCOMING`
follow-up enqueued by `SmsJobService.onStartJob` (`:76`) with delay
`getIncomingTimeoutSecs()`. The design preserves this two-stage debounce with a version split:

- **API 24+ (preferred):** `Constraints.addContentUriTrigger(SMS_PROVIDER, triggerForDescendants
  = true)` (the `FLAG_NOTIFY_FOR_DESCENDANTS` equivalent verified at `:179,181`), plus the call-log
  URI under the same `isCallLogBackupAfterCallEnabled()` condition. The triggered worker enqueues
  the delayed incoming-backup work (preserving the `getIncomingTimeoutSecs()` debounce) — it does
  not back up on the raw content change, matching today's follow-up behavior.
- **API 14–23 (the current floor up to pre-24):** content-URI triggers are unavailable. Retain the
  **existing broadcast fallback** — the SMS-received path calls `scheduler.scheduleIncoming()`,
  enqueueing a delayed `OneTimeWorkRequest(initialDelay = incomingTimeoutSecs)`. Mandated by AC-3(d)
  ("broadcast fallback below API 24") and required because the project floor is API 14
  (`app/build.gradle:14`; raised to 21 later by MU-011/REQ-MODERNIZATION-006). The fallback applies
  to the whole pre-24 range regardless of whether that floor is 14 or the later 21.

The version branch lives inside `WorkManagerScheduler.scheduleContentTrigger()` /
`scheduleIncoming()` (an `SDK_INT >= 24` check), so the port surface is version-agnostic and
callers are unaffected.

### Public broadcast contract preservation (AC-4)

The public contract `com.zegoggles.smssync.BACKUP` is unchanged. Only the receiver body changes:
where it currently constructs `new BackupJobs(context).scheduleImmediate()`, it instead resolves
the injected `BackupScheduler` and calls `scheduler.scheduleImmediate()`. Action string, receiver
class, manifest filter, and exported semantics are preserved byte-for-byte so third-party
automation (Tasker, etc.) keeps working. This is a public API contract; no drift allowed.

> Verification gap to close at planning time: confirm the exact receiver class and manifest
> `intent-filter` for `com.zegoggles.smssync.BACKUP` via grep over `app/src/main/AndroidManifest.xml`
> and the receiver source. Scope assumed from REQ-005 AC-4 — not yet grep-confirmed in this session.

### Dependency gates (sequencing)

- **DES-001 (shared core library).** The backup/restore loop must be extracted to core as
  suspending functions before `BackupWorker`/`RestoreWorker` can be thin adapters (ADR-005-B).
  MU-005 cutover is **blocked** until DES-001 lands.
- **DES-003 (test harness + CI), Gate G2.** Characterization/instrumented tests pinning retry and
  constraint behavior MUST be **green before the binding flip** (REQ-005 constraints; AC-3/AC-6).
  The four-invariant tests and the resumable-restore test run under this harness.
- **REQ-001 (SDK/AGP).** WorkManager 2.9.x+ requires the AGP/SDK bump from REQ-001; AGP is
  verified at `4.1.3` today (`build.gradle:8`), which is below the floor — so REQ-001 is a hard
  predecessor.
- **Gate G3 (grep gate).** After cutover, no references to `com.firebase.jobdispatcher`,
  `AlarmManagerDriver`, `SmsJobService`, or `extends AsyncTask` (in the service package) may
  remain (AC-5). The `firebase-jobdispatcher` dependency and the scijava repo are removed from
  `build.gradle`.

### Hilt worker construction (DES-008 / REQ-008)

`BackupWorker` and `RestoreWorker` are annotated `@HiltWorker` with `@AssistedInject` constructors
taking `@Assisted Context`, `@Assisted WorkerParameters`, plus the injected core collaborators
(backup/restore use-cases, message store / restore-checkpoint store, `Preferences`, auth/token
dependencies — replacing the hand-construction seen in `BackupTask`'s constructor at
`BackupTask.java:61-88`). The application configures a `HiltWorkerFactory` via
`Configuration.Provider` (custom WorkManager initialization, default initializer removed in the
manifest). The `BackupScheduler` binding (`WorkManagerScheduler`) is provided by a Hilt module;
the debug `CompositeScheduler` is bound only in the debug variant. Detailed DI wiring is owned by
DES-008; this design states only the integration points it requires.

## Design Validation

### The four MU-005 invariants → tests (AC-3)

All run under the WorkManager test harness (`WorkManagerTestInitHelper.initializeTestWorkManager`,
`SynchronousExecutor`, `WorkManagerTestInitHelper.getTestDriver()`).

| Invariant | Test |
|-----------|------|
| **INV-1 single-flight (AC-3a)** | Enqueue the same job kind twice via the port; query `WorkManager.getWorkInfosForUniqueWork(name)`; assert exactly one non-terminal work item — covering `ExistingWorkPolicy.REPLACE` / `enqueueUniquePeriodicWork`, the equivalent of `setReplaceCurrent(true)`. |
| **INV-2 constraint fidelity (AC-3c)** | For `isWifiOnly()=true` assert enqueued `WorkSpec.constraints.requiredNetworkType == UNMETERED`; for `false` assert `CONNECTED`. For `scheduleImmediate` (`BROADCAST_INTENT`) assert **no network constraint** (`NetworkType.NOT_REQUIRED`). Assert charging is **not** required in all cases — matching verified `jobConstraints`. |
| **INV-3 backoff fidelity (AC-3b)** | Assert `WorkSpec.backoffPolicy == EXPONENTIAL` and `backoffDelayDuration == 30_000ms`; assert effective retry delay is capped at `300s` (the maximum from `newRetryStrategy(…, 30, 300)`). |
| **INV-4 content-URI trigger (AC-3d)** | API 24+: assert `WorkSpec.constraints.contentUriTriggers` includes the SMS provider URI (and call-log URI when `isCallLogBackupAfterCallEnabled()`), `triggerForDescendants=true`. API ≤23: assert the broadcast fallback enqueues a delayed `OneTimeWorkRequest(initialDelay = incomingTimeoutSecs)`. |

### Public broadcast contract — ADB test (AC-4)

Instrumented test driving the real broadcast:
`adb shell am broadcast -a com.zegoggles.smssync.BACKUP` against the installed debug build; assert
a backup work item is enqueued (`getWorkInfosForUniqueWork` for the immediate/BROADCAST_INTENT
unique name shows an enqueued item). Verifies the public contract survives the migration.

### Resumable restore, no duplicates (AC-6)

Instrumented test: seed a restore of N messages; inject a fault that throws after committing K
(`< N`) rows but **before** checkpoint advance; simulate process death by re-initializing the test
WorkManager; let the worker re-execute. Assert: (a) the worker resumes from the persisted cursor;
(b) final SMS-provider row count == N — no duplicates from the K-row crash window, exercising the
verified `smsExists`/`callLogExists` dedup guards (`RestoreTask.java:259,280,308`); (c) the restore
reaches a terminal `SUCCEEDED` state.

### Removal verification (AC-5, Gate G3)

CI grep gate asserting zero matches for `com.firebase.jobdispatcher`, `AlarmManagerDriver`,
`SmsJobService`, and `extends AsyncTask` (in the service package). The `firebase-jobdispatcher`
dependency and the `maven.scijava.org` repository entry are removed from `build.gradle`
(verified present at lines 26-27); `androidx.work:work-runtime-ktx` is present.

### Parallel-run divergence check (pre-cutover confidence)

In a debug build with `CompositeScheduler` bound, drive each scheduling entry point and assert the
WorkManager-produced constraints / backoff / trigger match the legacy-produced equivalents. Logged
divergences outside the documented allow-list (none expected, since the source mapping is exact)
fail the check. This is the gate that justifies the binding flip and the documented rollback.

### Observability validation

Assert `scheduler.observe(jobKind)` emits the WorkManager-backed state transitions
(`ENQUEUED → RUNNING → SUCCEEDED/FAILED`) and that the worker's `setProgress` values surface to the
flow — replacing the Otto `App.post(BackupState)` / `@Subscribe` progress path verified in the
tasks and in `SmsJobService.backupStateChanged` (`SmsJobService.java:110-126`).

## Integration Contracts

> This design introduces one new internal cross-component boundary (the `BackupScheduler` port,
> core ↔ app) and preserves one existing external boundary (the public BACKUP broadcast). The
> internal port boundary requires a CNTR artifact before stories referencing this design can be
> sprint-planned.

| Boundary | Producer | Consumer(s) | Contract Type | CNTR Artifact | Status |
|----------|----------|-------------|---------------|---------------|--------|
| `BackupScheduler` port (core) → `WorkManagerScheduler` (app) | core (port owner) | app adapter, broadcast/boot receivers, settings UI | service | CNTR-MODERNIZATION-005-scheduler (needed) | needed |
| `com.zegoggles.smssync.BACKUP` broadcast (external trigger) | third-party automation / app | broadcast receiver → `scheduler.scheduleImmediate()` | event | CNTR-MODERNIZATION-005-broadcast (needed) | needed |

### `BackupScheduler` port (operations)

| Operation | Semantics | Maps to |
|-----------|-----------|---------|
| `scheduleIncoming()` | delayed one-off, delay = `getIncomingTimeoutSecs()`, network per `isWifiOnly()` | unique `OneTimeWorkRequest` |
| `scheduleRegular()` | recurring backup, network per `isWifiOnly()`, exponential backoff (30s→300s) | `enqueueUniquePeriodicWork(REGULAR, REPLACE/UPDATE)` |
| `scheduleContentTrigger()` | recurring content-URI observer over SMS (+ call-log when enabled), API 24+ | `PeriodicWorkRequest` + content-URI triggers |
| `scheduleBootup()` | if `!isAutoBackupEnabled()` → `cancelAll()`; else schedule regular | per-branch, mirrors `BackupJobs.scheduleBootup` |
| `scheduleImmediate()` | immediate one-off, **no network constraint**, REPLACE | one-off, no delay, `Constraints.NONE` |
| `scheduleRestore(config)` | one-off restore; resumable via durable checkpoint (NEW) | unique `OneTimeWorkRequest` |
| `cancelAll()` | cancel regular + content-trigger | `cancelUniqueWork(REGULAR)` + `cancelUniqueWork(contentTrigger)` |
| `cancelRegular()` | cancel periodic backup | `cancelUniqueWork(REGULAR)` |
| `cancel(jobKind)` | cancel running/pending job; structured-cancel the coroutine; clear retry | `cancelUniqueWork(name)` |
| `observe(jobKind): Flow<SchedulerState>` | observable job state + progress | `getWorkInfosForUniqueWork*` flow |

### Constraint + backoff configuration (value object, core)

| Field | Type / values | Source of truth (verified) |
|-------|---------------|----------------------------|
| `networkType` | `UNMETERED` (wifi-only) \| `CONNECTED` (any) \| `NONE` (immediate) | `Preferences.isWifiOnly()`; `BROADCAST_INTENT → new int[0]` (`BackupJobs.java:197-199`) |
| `requiresCharging` | `Boolean`, default `false` | not enforced today (`jobConstraints` emits network only) |
| `backoffPolicy` | `EXPONENTIAL` | `RETRY_POLICY_EXPONENTIAL` (`:205`) |
| `initialBackoff` | `30s` | `newRetryStrategy(…, 30, 300)` initial (`:205`) |
| `maximumBackoff` | `300s` (cap) | `newRetryStrategy(…, 30, 300)` maximum (`:205`) |
| `existingPolicy` | `REPLACE` | `setReplaceCurrent(true)`, universal (`:188`) |
| `uniqueName` | per `BackupType` (`REGULAR`/`INCOMING`/`BROADCAST_INTENT`) + `contentTrigger` tag + restore | `setTag(backupType.name())` (`:190`); `CONTENT_TRIGGER_TAG` (`:57`) |
| `incomingDelaySecs` | `getIncomingTimeoutSecs()` | `scheduleIncoming` (`:74-75`) |
| `bootDelaySecs` | `60` (`BOOT_BACKUP_DELAY`) | `BackupJobs.java:56,92` |

**Binding invariants:** single-flight (REPLACE); constraint fidelity (network per type, immediate
unconstrained, charging never required); backoff fidelity (30s initial, 300s cap, exponential);
public broadcast contract honored. **Cancellation:** structured — cancels the coroutine and clears
the scheduled retry. **Durability:** restore survives process death via WorkManager re-execution +
durable checkpoint; the existing `smsExists`/`callLogExists` write-seam guards guarantee no
duplicate rows.

### Contracts Needed (pre-sprint gate)

These boundaries do NOT yet have an approved CNTR-* artifact. They must be resolved by running
`/amp:create-contracts` before any story referencing this design can be approved for a sprint.

- [ ] CNTR needed: `BackupScheduler` port (core → app) — service contract; operations + constraint/backoff value object above.
- [ ] CNTR needed: `com.zegoggles.smssync.BACKUP` broadcast — external event contract; action string, receiver, intent-filter (exact receiver/filter to be grep-confirmed at contract authoring).

## Trade-offs

- **Single delay vs. flex window.** WorkManager periodic/one-off scheduling does not reproduce a
  JobDispatcher flex window — but the source uses a **degenerate** window (`executionWindow(s, s)`),
  so there is no flex behavior to lose. Net: no behavioral regression.
- **Backoff cap semantics.** Firebase's `(30, 300)` is an explicit initial+max pair. WorkManager
  expresses initial backoff directly and caps internally at a much larger ceiling; the 300s cap is
  re-imposed at the worker. Slightly more code, exact behavioral parity.
- **Otto coupling deferred to DES-007.** This design routes worker progress through the new
  observable channel rather than ripping out Otto here, keeping MU-005 focused on the scheduler
  spine. Risk: temporary dual presence of observable + Otto until DES-007 lands; mitigated by the
  worker emitting only through the new channel.

## Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Public BACKUP broadcast breaks third-party automation | High | Preserve action/receiver/filter byte-for-byte; ADB broadcast test (AC-4); parallel-run before flip |
| Restore duplicates across crash window | High | Durable checkpoint + verified `smsExists`/`callLogExists` dedup guards; fault-injection test (AC-6) |
| Cutover sequencing (DES-001/003/REQ-001 not landed) | High | Hard predecessor gates; cutover blocked until all green |
| scijava removal breaks another consumer | Medium | Grep-confirm no remaining `firebase-jobdispatcher` consumer before removing the repo entry |
| Content-URI debounce regresses (backs up on every SMS) | Medium | Reproduce the two-stage trigger→delayed-follow-up; INV-4 test asserts the debounce delay |
| Misreading `(30,300)` as two strategies | Medium | Corrected in this design; INV-3 asserts initial=30s, cap=300s on one strategy |

## Notes

This design corrects three misreadings that a summary-only reading would have introduced:
the `(30, 300)` retry pair is a single initial+max strategy (not backup-30 / restore-300); the
immediate/BROADCAST_INTENT job carries **no** network constraint; and the execution window is
degenerate (no 10% flex). All scheduling values are cited to verified source lines in
`BackupJobs.java`. The Otto-vs-greenrobot progress bus and the already-cursor-shaped
`RestoreConfig.currentRestoredItem` were likewise verified directly. Cross-cutting boundaries that
must be owned before sprint planning: the `BackupScheduler` CNTR and the BACKUP broadcast CNTR
(both listed as needed). Open verification items deferred to implementation planning: exact
BACKUP receiver class + manifest intent-filter, and the full set of `BackupJobs` external call
sites — both flagged inline as "scope assumed — not verified" rather than asserted.
