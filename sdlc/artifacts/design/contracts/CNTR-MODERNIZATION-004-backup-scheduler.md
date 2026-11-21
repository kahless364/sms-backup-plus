---
status: approved
artifact_type: interface-contract
consumers: []
related_requirements:
  - REQ-MODERNIZATION-013
  - REQ-MODERNIZATION-015
related_design_docs:
  - DES-MODERNIZATION-012
  - DES-MODERNIZATION-014
related_stories: []
change_records:
  - CR-001
  - CR-015
  - CR-017
id: CNTR-MODERNIZATION-004
title: ''
domain: modernization
contract_type: ''
producer: ''
---

# CNTR-MODERNIZATION-004: BackupScheduler Port

> One-line note: defines the `BackupScheduler` port — the single core↔app scheduling boundary that retires the four legacy background-execution mechanisms behind one binding flip, preserving four behavioral invariants verified line-for-line against `BackupJobs.java`. Draft; not finalized.

## Overview

This contract specifies the **`BackupScheduler` port** — the app-owned hexagonal port through which all background backup/restore scheduling flows in SMS Backup+. It is the central abstraction of migration unit MU-005 ("THE SPINE") and design DES-MODERNIZATION-005: a single port behind which four overlapping legacy mechanisms (Firebase JobDispatcher, the `SmsJobService` lifecycle bridge, `AlarmManagerDriver`, and `AsyncTask`-based execution) collapse into one WorkManager-backed implementation, swapped by one DI binding change.

The port lives in the **core module** and carries **no Android scheduler types in its signature** (no `com.firebase.*`, no `androidx.work.*`, no `Constraints`/`WorkRequest`/`BackoffPolicy`). All Android-specific construction lives in the adapter. This keeps the "when and how to schedule" decision logic unit-testable off-device and makes the legacy→WorkManager cutover a binding flip plus a worker implementation rather than a call-site rewrite.

This contract is the load-bearing artifact for the four MU-005 invariants. It encodes the **verified** source values (corrected from plausible misreadings) so that the WorkManager adapter is a mechanical translation of the current `BackupJobs.java` behavior, not a reinterpretation of it.

## Contract Boundary

This is a **service / module-interface contract** across one internal boundary (core ↔ app).

| Side | Component | Module | Role |
|------|-----------|--------|------|
| **Producer (port owner)** | `BackupScheduler` interface | core | Declares the scheduling contract; owns the value objects (`BackupType`, `SchedulerConfig`, `SchedulerState`). No Android scheduler type in any signature. |
| **Producer (sole production impl)** | `WorkManagerScheduler` adapter | app | Implements the port by mapping each call to `WorkRequest`s; the content-URI trigger needs no version branch at the minSdk 24 floor (REQ-MODERNIZATION-015; the code branch that served the lower levels is removed by story S1 of DES-MODERNIZATION-014); re-imposes the 300s backoff cap at the worker (DES-MODERNIZATION-005 §System Architecture, ADR-005-A/B). |
| **Producer (debug-only impls)** | `LegacyScheduler` (wraps today's `BackupJobs`) and `CompositeScheduler` (delegates to both, logs divergence) | app (debug) | Parallel-run / branch-by-abstraction validation only; never in release builds. |
| **Consumers** | The `com.zegoggles.smssync.BACKUP` broadcast receiver (CNTR-MODERNIZATION-005), boot receiver, package-replaced receiver, content-URI observer path (the content-trigger worker), settings UI. From v4 the SMS-received receiver is not a consumer (CR-017) | app | Resolve the **injected** `BackupScheduler` (Hilt; DES-008) and call its operations. They depend on the port, never on a concrete mechanism (AC-1). |

**Producer:** `WorkManagerScheduler` adapter (DES-MODERNIZATION-005).
**Consumers:** the services/receivers/UI that currently construct and call `BackupJobs` directly; rewired to depend on the injected `BackupScheduler` (Hilt-provided per DES-008).

> Verified caller note (DES-MODERNIZATION-005 §System Architecture): the only `BackupJobs` constructor call sites confirmed are `new BackupJobs(this)` in `SmsJobService.getBackupJobs()` (`SmsJobService.java:147`) and the public `BackupJobs(Context)` constructor used by external entry points. The full set of external `BackupJobs` call sites across receivers/settings is **scope-assumed, not yet grep-verified** in the design session — it must be grep-confirmed at implementation planning time before this contract's consumer list is treated as exhaustive.

## Contract Definition

### Interface: `BackupScheduler` (core port)

```
Interface: BackupScheduler   // module: core; no Android scheduler types in any signature

Methods:
  // --- scheduling (surface derived directly from BackupJobs.java public methods) ---
  scheduleIncoming(): Unit
      // delayed one-off; delay = Preferences.getIncomingTimeoutSecs();
      // network per isWifiOnly(); REPLACE. (BackupJobs.java:74)
  scheduleRegular(): Unit
      // recurring backup; network per isWifiOnly(); EXPONENTIAL 30s→300s backoff;
      // UPDATE on the stable unique name REGULAR; if UPDATE fails (a one-time entry
      // under REGULAR left by an earlier build), it re-registers with
      // CANCEL_AND_REENQUEUE (the periodic REPLACE). (BackupJobs.java:78)
      // interval = getRegularTimeoutSecs(); a positive value below 15 min is raised to
      // WorkManager's 15-min minimum. A non-positive value ("Never" is -1; 0 too)
      // registers nothing, cancels an existing REGULAR and returns no job: the legacy
      // `inSeconds > 0` rule (BackupJobs.java:132). (v4, CR-017; Rule 4a)
  scheduleContentTrigger(): Unit
      // recurring content-URI observer over SMS provider (+ call-log when
      // call-log backup is enabled and isCallLogBackupAfterCallEnabled());
      // available on every supported level (minSdk 24). (BackupJobs.java:82)
  scheduleBootup(): Unit
      // if !isAutoBackupEnabled() -> cancelAll(); else registers the regular periodic
      // backup exactly as scheduleRegular() does (same REGULAR request, UPDATE, the
      // CANCEL_AND_REENQUEUE fallback, the same "Never" rule), with
      // BOOT_BACKUP_DELAY = 60s as the first-run delay of a new registration. UPDATE
      // keeps an existing registration (its id and schedule; a running regular backup
      // is not cancelled). Never enqueues a one-time REGULAR work and starts no separate
      // boot backup. (BackupJobs.java:86-97, :56; v4, CR-017; Rule 4)
  scheduleImmediate(): Unit
      // immediate one-off, BackupType.BROADCAST_INTENT; NO NETWORK CONSTRAINT
      // (Constraints.NONE); REPLACE. (BackupJobs.java:99, jobConstraints :197)
      // UNCHANGED by the v2 amendment — third-party/automation BACKUP-broadcast
      // semantics preserved byte-for-byte (CNTR-MODERNIZATION-005 feed).
  scheduleManual(backupType: BackupType): Unit          // NEW (v2; DES-MODERNIZATION-012 / REQ-MODERNIZATION-013)
      // immediate one-off carrying the ENGINE backup type so the manual
      // MainActivity→SmsBackupService dispatch (migrated off startService→AsyncTask
      // onto the scheduler) preserves the MANUAL-vs-SKIP distinction at the worker.
      // Accepts ONLY BackupType.MANUAL or BackupType.SKIP (see Validation Rule 8).
      // NO NETWORK CONSTRAINT (Constraints.NONE, identical to scheduleImmediate);
      // REPLACE; no initial delay; unique-work name = backupType.name()
      // ("MANUAL" / "SKIP"); the type is carried to the worker via the worker TAG
      // (addTag(backupType.name())), which BackupWorker.inferBackupType() reads via
      // BackupType.fromName(tag). SKIP therefore reaches BackupWorker.executeSkip()
      // (BackupWorker.kt:161-162); MANUAL reaches the normal IMAP path. Does NOT
      // replace scheduleImmediate() — BROADCAST_INTENT keeps its own unique name.
  scheduleRestore(config: RestoreSchedulerConfig): Unit
      // one-off restore; resumable via durable WorkManager checkpoint (NEW —
      // there is no restore scheduler in current source). (DES-005 AC-6)

  // --- cancellation ---
  cancelAll(): Unit
      // cancelRegular() + cancel content-trigger. (BackupJobs.java:103)
  cancelRegular(): Unit
      // cancel the periodic backup. (BackupJobs.java:108)
  cancel(jobKind: BackupType): Unit
      // single-flight cancellation; structured-cancels the running coroutine
      // and clears any scheduled retry. (AC-2/AC-3)

  // --- observability ---
  observe(jobKind: BackupType): Flow<SchedulerState>
      // observable job state + progress; replaces the Otto App.post(BackupState)
      // path. Backed by WorkManager WorkInfo. (DES-005 §Observability validation)
```

**Surface provenance (verified).** Every scheduling method maps directly to a verified public method of `BackupJobs.java`: `scheduleIncoming` (:74), `scheduleRegular` (:78), `scheduleContentTriggerJob` (:82), `scheduleBootup` (:86), `scheduleImmediate` (:99), `cancelAll` (:103), `cancelRegular` (:108). `scheduleRestore`, `cancel(jobKind)`, and `observe` are net-new (AC-6, AC-2/AC-3, observability) — there is no restore scheduler in current source, so restore has **no** current backoff/constraint value to preserve.

### Enum: `BackupType` (core)

The job-kind discriminator. Source of truth: the existing `BackupType` enum read via `setTag(backupType.name())` (`BackupJobs.java:190`) and the `isRecurring()` / `Trigger.NOW` branching (`:162-163`). Carried verbatim so unique-work names and recurrence remain byte-stable.

> **Single-enum reconciliation (v2 correction; verified against source `BackupType.java`).** There is exactly **one** `BackupType` enum in the codebase — `com.zegoggles.smssync.service.BackupType` (`app/src/main/java/com/zegoggles/smssync/service/BackupType.java:6-12`, verified this session). It is shared by the scheduler (`WorkManagerScheduler.kt`, which imports `com.zegoggles.smssync.service.BackupType` at `:26`) AND the engine/service layer (`BackupWorker.kt`, `BackupTask`, `SmsBackupService`). v1 of this contract enumerated only the **three values the scheduler discriminator used at the time** (`REGULAR`/`INCOMING`/`BROADCAST_INTENT`); the actual enum has **six** members. There is **no** separate "engine-level backup type" enum — the MANUAL/SKIP distinction the manual-dispatch migration (REQ-MODERNIZATION-013) must preserve is carried by **additional members of this same enum**. v2 enumerates them and adds the `scheduleManual(BackupType)` operation that carries `MANUAL`/`SKIP` to the worker. The recurrence/network model of the original three members is **unchanged**.

```
Enum: BackupType        // com.zegoggles.smssync.service.BackupType (verified BackupType.java:6-12)
Values:
  // --- the three scheduler-discriminator members (v1; unchanged) ---
  REGULAR           // recurring=true  — periodic scheduled backup
  INCOMING          // recurring=false — delayed follow-up after an SMS arrives
  BROADCAST_INTENT  // recurring=false — immediate, user/automation-triggered;
                    //                   NO network constraint (Constraints.NONE)
  // --- the remaining members (always present in source; enumerated in v2) ---
  UNKNOWN           // fromName()/fromIntent() fallback (BackupType.java:10,24,34);
                    //                   inferBackupType() treats it as "no tag matched"
  MANUAL            // user-initiated backup from MainActivity (engine type);
                    //                   isBackground()==false (BackupType.java:38)
  SKIP              // user-initiated "mark-as-backed-up without IMAP"; routes to
                    //                   BackupWorker.executeSkip() (BackupType.java:12;
                    //                   BackupWorker.kt:161-162); isBackground()==false
Derived:
  isRecurring(): Boolean   // REGULAR -> true; all others -> false
                           // (verified BackupType.java:41 — `this == REGULAR`)
  isBackground(): Boolean  // false for MANUAL and SKIP; true for all others
                           // (verified BackupType.java:37-39) — the property that
                           // distinguishes the manual path from the scheduled path.
Notes:
  - The content-trigger job is recurring (setRecurring(true), BackupJobs.java:171)
    and is identified by the stable unique name and tag CONTENT_TRIGGER
    (WorkManagerScheduler.CONTENT_TRIGGER_UNIQUE_NAME; the legacy job's tag was
    CONTENT_TRIGGER_TAG = "contentTrigger", BackupJobs.java:57),
    distinct from the BackupType values above.
  - RESTORE is NOT a BackupType value — restore is a separate one-off via
    scheduleRestore(config); it is excluded from the backup recurrence model.
  - uniqueName per kind: REGULAR / INCOMING / BROADCAST_INTENT / MANUAL / SKIP +
    the CONTENT_TRIGGER unique name (also its tag) + a restore unique name. MANUAL and SKIP get their
    OWN unique-work names (backupType.name()), distinct from BROADCAST_INTENT, so a
    manual run and an automation-broadcast run do not REPLACE each other.
  - The boot path has no unique name of its own: it registers the REGULAR periodic
    work, because it is the regular backup (v4, CR-017). No BOOT unique name is used.
  - The worker resolves the carried type from its tag: BackupWorker.inferBackupType()
    (BackupWorker.kt:430-436) iterates tags and returns BackupType.fromName(tag);
    an unrecognized/absent tag falls back to BROADCAST_INTENT. This is the exact
    mechanism scheduleManual(backupType) relies on — it MUST addTag(backupType.name()).
```

### Unique work names and policies (v4)

| Unique name | Work | Request | Policy | Enqueued by |
|-------------|------|---------|--------|-------------|
| `REGULAR` | `BackupWorker`, tag `REGULAR` | periodic, interval `max(getRegularTimeoutSecs(), 15 min)`; network per `isWifiOnly()`; `EXPONENTIAL` 30s; first-run delay 60s when the boot path makes a new registration | `UPDATE`; `CANCEL_AND_REENQUEUE` only when `UPDATE` fails | `scheduleRegular()`, `scheduleBootup()`; nothing is registered, and an existing entry is cancelled, when `getRegularTimeoutSecs() <= 0` (Rule 4a) |
| `INCOMING` | `BackupWorker`, tag `INCOMING` | one-off, initial delay `getIncomingTimeoutSecs()`; network per `isWifiOnly()`; `EXPONENTIAL` 30s | `REPLACE` | `scheduleIncoming()`, called by the content-trigger worker |
| `CONTENT_TRIGGER` | `BackupTriggerWorker`, tag `CONTENT_TRIGGER` | periodic, 15 min, content-URI triggers (SMS; call log when enabled); no network constraint; no backoff set | `UPDATE` | `scheduleContentTrigger()` |
| `BROADCAST_INTENT` | `BackupWorker` | one-off, `Constraints.NONE`, `EXPONENTIAL` 30s, no delay | `REPLACE` | `scheduleImmediate()` |
| `MANUAL`, `SKIP` | `BackupWorker`, tag = name | one-off, `Constraints.NONE`, `EXPONENTIAL` 30s, no delay | `REPLACE` | `scheduleManual(type)` |
| restore name | `RestoreWorker` | one-off, `Constraints.NONE`, `EXPONENTIAL` 30s | `REPLACE` | `scheduleRestore(config)` |

No `BOOT` unique name is used. See "Code not yet at v4" for the rows the code reaches only with a later story.

### Value object: `SchedulerConfig` (core) — constraint + backoff configuration

The Android-free description of *how* a job must be enqueued. The adapter translates this to `androidx.work.Constraints` + `BackoffPolicy`; core never names a WorkManager type. **All values verified against `BackupJobs.java` and carried exactly.**

```
Type: SchedulerConfig   // module: core; immutable value object

Fields:
  networkType: NetworkRequirement        // enum below — per-type, NOT uniform
  requiresCharging: Boolean = false       // VERIFIED never required (jobConstraints
                                           // emits network only, BackupJobs.java:195-201)
  backoff: BackoffConfig?                  // see below; null = none set: the content-trigger
                                           // observer only, which never retries (v4)
  existingPolicy: ExistingPolicy = REPLACE // one-off work; periodic work (REGULAR,
                                           // CONTENT_TRIGGER) uses UPDATE (v4; enum below).
                                           // Legacy: setReplaceCurrent(true) on every job (:188)
  uniqueName: String                       // per BackupType / content-trigger / restore
  initialDelaySecs: Long?                  // null = no delay (Trigger.NOW, :162);
                                           // INCOMING = getIncomingTimeoutSecs();
                                           // boot path: BOOT_BACKUP_DELAY = 60 (:56) as the
                                           // first-run delay of a new REGULAR registration (v4)
  recurring: Boolean                       // BackupType.isRecurring() / content-trigger=true
  contentUriTriggers: List<ContentUriTrigger>?  // content-trigger job only

Enum: NetworkRequirement
  UNMETERED   // adapter -> NetworkType.UNMETERED  ; when isWifiOnly()      (BackupJobs.java:199)
  CONNECTED   // adapter -> NetworkType.CONNECTED  ; when !isWifiOnly()     (BackupJobs.java:199)
  NONE        // adapter -> Constraints.NONE / NOT_REQUIRED ; for backup work:
              //   BROADCAST_INTENT (jobConstraints returns new int[0], BackupJobs.java:197)
              //   and MANUAL/SKIP (Rule 8). The content-trigger observer also has NONE: it
              //   is not backup work; INV-2 applies to the INCOMING backup it enqueues (v4)

Type: BackoffConfig   // ONE strategy — initial + cap, NOT two separate values
  policy: BackoffPolicy = EXPONENTIAL      // RETRY_POLICY_EXPONENTIAL (BackupJobs.java:205)
  initialBackoffSecs: Long = 30            // newRetryStrategy(EXPONENTIAL, 30, 300) initial
  maximumBackoffSecs: Long = 300           // newRetryStrategy(EXPONENTIAL, 30, 300) cap

Enum: BackoffPolicy { EXPONENTIAL }
Enum: ExistingPolicy { REPLACE, UPDATE }   // one-off: REPLACE; periodic: UPDATE (REGULAR,
                                           // CONTENT_TRIGGER); REGULAR falls back to
                                           // CANCEL_AND_REENQUEUE (the periodic REPLACE)
                                           // when UPDATE fails (v4)

Type: ContentUriTrigger
  uri: String                              // SMS provider URI; + call-log URI when
                                           // call-log backup is enabled and
                                           // isCallLogBackupAfterCallEnabled()
  triggerForDescendants: Boolean = true    // FLAG_NOTIFY_FOR_DESCENDANTS equivalent
                                           // (BackupJobs.java:179,181)
```

> **Backoff is ONE strategy, not two values.** `newRetryStrategy(RETRY_POLICY_EXPONENTIAL, 30, 300)` at `BackupJobs.java:205` has the Firebase signature `(policy, initialBackoffSeconds, maximumBackoffSeconds)`. It means **30s initial backoff growing exponentially, capped at 300s** — it is NOT "30s for backup, 300s for restore." There is no restore scheduler today, so restore has no current backoff at all. A consumer or adapter that treats `30` and `300` as two separate strategies violates this contract.

### Value object: `RestoreSchedulerConfig` (core) — for `scheduleRestore`

```
Type: RestoreSchedulerConfig   // module: core
Fields:
  // the restore selection/params already shaped by the preserved RestoreConfig core type;
  // the scheduler only needs the unique-work identity + checkpoint key.
  uniqueName: String                       // restore unique work name
  checkpointKey: String                    // durable cursor key (currentRestoredItem,
                                           // promoted to durable store; RestoreConfig
                                           // already tracks it — RestoreTask.java:100,:119)
Semantics:
  - one-off OneTimeWorkRequest; resumable across process death via WorkManager
    guaranteed re-execution + a durable checkpoint written AFTER each successful
    provider insert and BEFORE advancing the cursor.
  - idempotency is enforced at the write seam by the preserved smsExists/callLogExists
    guards (RestoreTask.java:259,280,288-327) — NOT by this scheduler.
```

### Value object: `SchedulerState` (core) — for `observe`

```
Type: SchedulerState   // module: core; adapter maps from WorkManager WorkInfo.State
Variants (sealed):
  Enqueued
  Running(progress: BackupProgress?)   // progress surfaced via WorkManager setProgress
  Succeeded
  Failed(cause: SchedulerFailure)
  Cancelled
Notes:
  - Maps the WorkInfo lifecycle ENQUEUED -> RUNNING -> SUCCEEDED/FAILED/CANCELLED.
  - Replaces the Otto App.post(BackupState)/@Subscribe path (DES-005 ADR-005-B).
  - The concrete progress payload type is owned by DES-007 (Otto->StateFlow); this
    contract requires only that progress surface through observe(), not through Otto.
```

## The four preserved invariants (contract clauses)

These four invariants are **binding clauses**. The producer (`WorkManagerScheduler`) MUST satisfy all four; the parallel-run `CompositeScheduler` exists to prove it before the binding flip. Each maps line-for-line to current `BackupJobs.java` source (DES-MODERNIZATION-005 §Integration Design contract-mapping table).

| Invariant | Clause | Verified source | WorkManager realization |
|-----------|--------|-----------------|-------------------------|
| **INV-1 — single-flight (REPLACE enqueue)** | Enqueuing a job kind that is already pending MUST replace it; at most one non-terminal work item per unique name. | `setReplaceCurrent(true)` on **every** job (`BackupJobs.java:188`) | one-off → `ExistingWorkPolicy.REPLACE`; periodic → `enqueueUniquePeriodicWork(name, REPLACE/UPDATE, …)` with a stable unique name per `BackupType`. `REGULAR` and `CONTENT_TRIGGER` use `UPDATE`; `REGULAR` falls back to `CANCEL_AND_REENQUEUE` (the periodic `REPLACE`) only when `UPDATE` fails. One work kind per unique name: the boot path registers the `REGULAR` periodic work and never enqueues a one-off under `REGULAR` (v4, CR-017). |
| **INV-2 — network constraints (per type)** | Network requirement is **per job type**, not uniform: `UNMETERED` when `isWifiOnly()`, `CONNECTED` otherwise, and **NONE** for the immediate `BROADCAST_INTENT` backup. Charging is **never** required. | `ON_UNMETERED_NETWORK`/`ON_ANY_NETWORK` (`:199`); `BROADCAST_INTENT → new int[0]` (`jobConstraints`, `:197`); no charging anywhere (`:195-201`) | `NetworkType.UNMETERED` / `NetworkType.CONNECTED` / `Constraints.NONE` for immediate; `setRequiresCharging` never set. |
| **INV-3 — backoff fidelity** | Retry MUST be **exponential, 30s initial, capped at 300s** — one strategy. The effective retry delay MUST never exceed 300s. | `newRetryStrategy(RETRY_POLICY_EXPONENTIAL, 30, 300)` (`:205`) | `setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, SECONDS)`; because WorkManager's internal ceiling is `MAX_BACKOFF_MILLIS` (~5h), the **300s cap is re-imposed at the worker** by capping retry scheduling. |
| **INV-4 — content-URI trigger** | An SMS (and, when call-log-after-call is enabled, call-log) content change MUST trigger backup, on every supported level (minSdk 24 and above), via content-URI triggers. The trigger MUST preserve the **two-stage debounce**: the content change enqueues a *delayed* incoming-backup (delay = `getIncomingTimeoutSecs()`), it does NOT back up on the raw change. | `Trigger.contentUriTrigger(observedUris())` over SMS + optional call-log (`:170,177-184`); follow-up `scheduleIncoming()` in `SmsJobService.onStartJob` (`:76`); `FLAG_NOTIFY_FOR_DESCENDANTS` (`:179,181`) | `Constraints.addContentUriTrigger(uri, triggerForDescendants=true)` per observed URI; the triggered worker enqueues the delayed `OneTimeWorkRequest(initialDelay = incomingTimeoutSecs)` as unique work `INCOMING` (REPLACE). The port surface is version-agnostic. The content trigger is the one incoming path (v4); see the note below the table. |

> **INV-4 at the minSdk 24 floor (v3, CR-015).** Every supported level (minSdk 24, REQ-MODERNIZATION-015) has content-URI triggers, so INV-4 is one mechanism with no version split. The earlier clause "on API ≤23 via the broadcast fallback" is retired as obsolete scope, not as a lost capability: no supported level can run it, and this is not a behavioural break (see Breaking change policy). Below API 24 that clause was met at most by the SMS-received receiver, for incoming SMS only, and its API ≤23 assertion was never implemented. **The content trigger is the one incoming path (v4, CR-017).** The SMS-received receiver (`SmsBroadcastReceiver`, a manifest receiver for `SMS_RECEIVED`) is not part of this contract from v4. It has not been delivered the broadcast since 2026-06-02, when U-001 declared it `android:exported="false"`, and `RECEIVE_SMS` is never requested at run time (BUG-023). The product owner decided on 2026-11-21 to remove it (decision appendix, Record 20, ruling 56). INV-4 never depended on it: the content trigger sees incoming and sent messages and, when enabled, call-log changes. The story that removes the receiver deletes the class, its manifest entry and its enable/disable in `App`; until then the class is in the code but is not an incoming path in fact (see "Code not yet at v4"). `scheduleIncoming()` stays a port operation; its production caller is the content-trigger worker. `RECEIVE_SMS` (`AndroidManifest.xml:17`) stays: the default-SMS `SMS_DELIVER` receiver (`compat/SmsReceiver`) needs it while the app holds the SMS role during a restore (CR-017, ruling 3); the story that removes the SMS-received receiver removes no permission. Test gate: INV-4's version-boundary assertion is made at the lowest supported level, API 24 (DES-MODERNIZATION-005 §Design Validation). The `SDK_INT < VERSION_CODES.N` branch of `WorkManagerScheduler.scheduleContentTrigger` and the test `inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast` are removed and retargeted by story S1 of DES-MODERNIZATION-014 (D4, row S-7 and table T). This contract states the target the code reaches with S1.

> **Degenerate execution window (not a flex window).** `Trigger.executionWindow(inSeconds, inSeconds)` (`:162`) has start == end — there is no JobDispatcher flex window to reproduce. The mapping is a single `setInitialDelay(inSeconds)`. Confirmed by `AlarmManagerDriver` reading only `getWindowStart()` (`AlarmManagerDriver.java:134-135`). A consumer expecting flex-window timing semantics is mistaken.

## Versioning

- **Current version:** v4 (amended in place by CR-017, 2026-11-21; v3 amended in place by CR-015, 2026-11-21; v2 amended in place by DES-MODERNIZATION-012 / REQ-MODERNIZATION-013; v1 introduced by MU-005 / DES-MODERNIZATION-005). v4 is non-breaking for consumers: it changes no port signature, no `BackupType` value and no `SchedulerState` variant, so no consumer migration is required. Its boot-path part is a behavioural break of the boot path, gated by tests (Changelog, v4, item 2); no invariant's clause changes. `status` remains `approved`.

> **Code not yet at v4 (CR-017).** v4 states the target. Until the story named in an item is done, the code differs as the item says; the orchestrator deletes the item when that story closes.
> - **K-1, until the story that fixes BUG-026 and BUG-027:** `scheduleRegular()` raises a non-positive interval to 15 minutes and registers a periodic backup (BUG-026). `scheduleBootup()` enqueues a one-time `BackupWorker` under `REGULAR` with `REPLACE`, which deletes the periodic regular backup at every restart, and the next `scheduleRegular()` then fails silently on that entry (BUG-027). Affected here: the `scheduleRegular()` and `scheduleBootup()` blocks, the boot-path bullet of the `BackupType` notes, the boot comment of `initialDelaySecs`, the `REGULAR` fallback in the `ExistingPolicy` note, the `REGULAR` row of the unique-name table, the INV-1 realization, Rules 4 and 4a, and the regular example.
> - **K-2, until the story that removes the SMS-received receiver (ruling 56):** `SmsBroadcastReceiver`, its manifest entry and its enable/disable in `App` are still in the code. The receiver is not delivered `SMS_RECEIVED` and is not an incoming path in fact (BUG-023).

### Changelog

- **v4 — 2026-11-21 (CR-017; BUG-026, BUG-027, BUG-023; product owner's decision of 2026-11-21, decision appendix Record 20, rulings 55 and 56).** Non-breaking for consumers: no port signature, `BackupType` value or `SchedulerState` variant changes. 1. **"Never" schedules nothing (BUG-026).** A non-positive regular interval registers no regular backup and cancels an existing one; a positive interval below 15 minutes is raised to WorkManager's minimum (new Rule 4a; `scheduleRegular()` text). This states the legacy rule (`BackupJobs.java:132`) the contract always claimed to translate; not a break. 2. **The boot path keeps the regular backup (BUG-027).** `scheduleBootup()` registers the regular periodic backup through the same request and policy as `scheduleRegular()` (`REGULAR`, `UPDATE`, `CANCEL_AND_REENQUEUE` only when `UPDATE` fails), with the 60s boot delay as the first-run delay of a new registration. It no longer enqueues a one-time `REGULAR` backup with `REPLACE`, which deleted the periodic work at every restart. Rule 4 and the INV-1 realization say so. Ruled a behavioural break of the boot path (its policy changes from one-off `REPLACE` to periodic `UPDATE`; with a registered regular backup, a restart no longer starts an extra backup a minute later); INV-1's clause is unchanged; gated by the boot tests in DES-MODERNIZATION-005 §Design Validation. The product owner's questions PO-1 (a backup after a restart when the schedule is "Never") and PO-2 (a backup about a minute after every restart) are open; v4 states the answer "no" to both, which the fix uses until the owner decides (Record 20). 3. **Content-trigger example corrected (U-069; BUG-023, Suggested Approach §2).** The registration carries no network constraint and no backoff, and uses `UPDATE`; INV-2 and INV-3 apply to the `INCOMING` backup it enqueues (Rule 2, one scoping sentence). The `NetworkRequirement.NONE` and `ExistingPolicy` notes and the `backoff` field follow. The code has this shape (`WorkManagerScheduler.kt:218-250`); not a break. 4. **The SMS-received receiver is no longer part of the contract (BUG-023; ruling 56).** INV-4 is met by the content trigger alone; the receiver has not been delivered `SMS_RECEIVED` since U-001; it is removed by its own story. Not a break: nothing observable changes, and no consumer signature changes. 5. **Unique work names and policies** are stated in one table. 6. Frontmatter: `CR-017` in `change_records`. 7. The code reaches items 1 and 2 with the story that fixes BUG-026 and BUG-027, and item 4 with the story that removes the receiver; until then see "Code not yet at v4".
- **v3 — 2026-11-21 (CR-015; REQ-MODERNIZATION-015 / CR-011; DES-MODERNIZATION-014 story S1).** Non-breaking amendment: the minSdk 24 floor retires the pre-API-24 clauses. 1. **INV-4 is one mechanism on every supported level.** Removed "on API ≤23 via the broadcast fallback" from the INV-4 clause and realization, and "(+ pre-24 fallback)" from its title. Kept: an SMS (and, when enabled, call-log) content change MUST trigger backup via content-URI triggers, with the two-stage debounce. 2. **Ruling: not a behavioural break.** No supported level can run the removed branch, which only returned `null`; the SMS-received broadcast path is unchanged; the debounce is intact. 3. Removed the API 24+ / pre-24 wording from `scheduleContentTrigger()`, `contentUriTriggers`, the producer row and the content-trigger example; added a note stating what the broadcast path covers (incoming SMS only). 4. **Test gate retargeted, not dropped:** the INV-4 version-boundary assertion moves to the lowest supported level, API 24. The API ≤23 assertion that DES-MODERNIZATION-005 required was never implemented (the only test asserted a `null` return). 5. No change to any port signature, `BackupType` value, `SchedulerState` variant, other invariant or Validation Rule. 6. Frontmatter: `CR-015` in `change_records`, `REQ-MODERNIZATION-015` in `related_requirements`, `DES-MODERNIZATION-014` in `related_design_docs`. 7. The branch and its test are deleted and retargeted by story S1 of DES-MODERNIZATION-014 (D4); this entry states the target.
- **v2 — 2026-06-04 (DES-MODERNIZATION-012 / REQ-MODERNIZATION-013).** Additive amendment for the manual backup/restore dispatch migration off `startService(...)`→`AsyncTask` onto the scheduler.
  1. **Added port operation `scheduleManual(backupType: BackupType): Unit`** — an immediate one-off that carries the engine `BackupType` (`MANUAL` or `SKIP`) to the worker via the work tag (`addTag(backupType.name())`), so `BackupWorker.inferBackupType()` resolves it and `SKIP` reaches `executeSkip()`. Constraint-identical to `scheduleImmediate` (`Constraints.NONE`, REPLACE, EXPONENTIAL/30s, no delay) but with its own unique-work name (`backupType.name()`), so a manual run and a `BROADCAST_INTENT` automation run do not REPLACE each other. `scheduleImmediate()` is **unchanged** — existing callers and the `BROADCAST_INTENT` automation path are not broken. Net operation count: **nine → ten**.
  2. **Single-enum reconciliation.** Corrected the `BackupType` enum documentation: there is exactly **one** `BackupType` enum (`com.zegoggles.smssync.service.BackupType`, verified `BackupType.java:6-12`) shared by the scheduler and the engine; v1 documented only the three discriminator members (`REGULAR`/`INCOMING`/`BROADCAST_INTENT`). v2 enumerates the full member set (`+ UNKNOWN, MANUAL, SKIP`) and the `isBackground()` derived property. There is **no** separate engine-level backup-type enum; `MANUAL`/`SKIP` are members of this same enum.
  3. **Added Validation Rule 8** governing `scheduleManual` (type-carrying, accepts only `MANUAL`/`SKIP`, constraint-identical to `scheduleImmediate`, must not collapse onto `BROADCAST_INTENT`).
  4. **Added an Example Payload** for `scheduleManual(SKIP)`/`scheduleManual(MANUAL)`.
  5. **Frontmatter:** added `REQ-MODERNIZATION-013` to `related_requirements` and `DES-MODERNIZATION-012` to `related_design_docs`.
- **v1 — introduced by MU-005 / DES-MODERNIZATION-005.** Initial `BackupScheduler` port (nine operations, four invariants).

### Breaking change policy

- **Breaking change policy.** Breaking for **consumers** = any change to a method signature, the `BackupType` enum values, or the `SchedulerState` variant set. Breaking for the **behavioral contract** = any change to the four invariants (different `ExistingPolicy`, a network constraint on `BROADCAST_INTENT`, a backoff value other than `EXPONENTIAL`/30s/300s, or losing the content-trigger or its debounce on any supported level). Behavioral breaks are the higher-severity class: they can silently change scheduling semantics without a compile error and MUST be gated by the INV-1..4 tests (DES-005 §Design Validation); where an invariant's realization depends on the API level, it is asserted at the lowest supported level. From v3 the pre-24 fallback is not on this list: the minSdk 24 floor (REQ-MODERNIZATION-015) retired the pre-24 levels, so removing the branch that served them loses no behaviour on any supported level (CR-015).
- **Backward compatibility (non-breaking).** Swapping the bound implementation (`LegacyScheduler` ↔ `WorkManagerScheduler` ↔ `CompositeScheduler`) is non-breaking by construction — the port surface is identical, which is exactly what makes the cutover and rollback a binding flip with **no** call-site change. Adapter-internal changes (the worker-side 300s cap mechanism; an API-level branch inside the adapter, such as the `SDK_INT >= 24` branch of the content trigger that story S1 of DES-MODERNIZATION-014 removes, provided every supported level still meets INV-1..4) are non-breaking. Adding a new `observe`-only state variant is a breaking change for exhaustive consumers and must be versioned.

## Validation Rules

Both sides MUST honor:

1. **No Android scheduler type crosses the port.** No `com.firebase.*`, `androidx.work.*`, `Constraints`, `WorkRequest`, `BackoffPolicy`, or `Context`-as-scheduler in any port signature. `Constraints`/`WorkRequest`/`BackoffPolicy` construction is adapter-only (DES-005 §System Architecture).
2. **The four invariants (INV-1..INV-4) hold** for every enqueue path — asserted by the harness in DES-005 §Design Validation (`WorkManagerTestInitHelper`, `getWorkInfosForUniqueWork`, `WorkSpec.constraints`/`backoffPolicy`/`backoffDelayDuration`/`contentUriTriggers`). INV-2 and INV-3 govern backup work: the content-trigger observer, which does no network I/O and never retries, carries no network constraint and no backoff of its own, and the `INCOMING` backup it enqueues through `scheduleIncoming()` carries both (v4, CR-017).
3. **`scheduleImmediate` is unconstrained.** It MUST enqueue with `Constraints.NONE` (no network), `BackupType.BROADCAST_INTENT`, REPLACE, no delay. Any network constraint here is a contract violation (INV-2).
4. **`scheduleBootup` honors `isAutoBackupEnabled()` and keeps the regular backup.** If auto-backup is off, it MUST `cancelAll()` (not schedule). Else it MUST register the regular periodic backup through the same request and policy as `scheduleRegular()` (`REGULAR`, `UPDATE`, `CANCEL_AND_REENQUEUE` only when `UPDATE` fails), with the 60s boot delay as the first-run delay of a new registration. It MUST NOT enqueue a one-time work under `REGULAR` and starts no separate boot backup. With a positive regular interval it MUST NOT cancel or reset a registered or running regular backup. With a non-positive regular interval ("Never") it registers nothing and cancels an existing `REGULAR` work, including a periodic one that an earlier build registered under "Never"; there Rule 4a governs. The encryption-degraded check comes first and changes nothing (U-054). (v4, CR-017)
4a. **A non-positive regular interval means "Never".** `scheduleRegular()` (and the boot path) MUST register no regular backup when `getRegularTimeoutSecs() <= 0`, MUST cancel an existing `REGULAR` work, and returns no job. A positive interval below 15 minutes is raised to WorkManager's 15-minute minimum; every other positive value is used as given. The encryption-degraded check comes first and changes nothing (U-054). (v4, CR-017; the legacy rule, `BackupJobs.java:132`. Numbered 4a so that Rules 5 to 8 keep their numbers.)
5. **Content-trigger debounce preserved.** The content-URI-triggered worker MUST NOT back up on the raw content change; it MUST enqueue the delayed incoming-backup (delay = `getIncomingTimeoutSecs()`). (INV-4, two-stage.)
6. **Restore is resumable and idempotent.** `scheduleRestore` MUST persist the checkpoint after each successful insert and before cursor advance; the preserved `smsExists`/`callLogExists` write-seam guards enforce no-duplicate-rows across the crash window. Both are required — the window is non-zero (DES-005 §Durable restore checkpoint).
7. **`cancel` is structured.** It MUST cooperatively cancel the running coroutine (unwinding at the next suspension point) and clear any scheduled retry — not merely poll an `isCancelled()` flag.
8. **`scheduleManual` carries the engine type and is otherwise constraint-identical to `scheduleImmediate`.** (v2; DES-MODERNIZATION-012 / REQ-MODERNIZATION-013.) It MUST enqueue a `BackupWorker` with `Constraints.NONE` (no network — identical to `scheduleImmediate`, INV-2), `ExistingWorkPolicy.REPLACE` (INV-1), `EXPONENTIAL`/30s backoff (INV-3), **no** initial delay, a unique-work name of `backupType.name()`, and **`addTag(backupType.name())`** so `BackupWorker.inferBackupType()` (`BackupWorker.kt:430-436`, reads `BackupType.fromName(tag)`) resolves the carried type — `SKIP` then reaches `BackupWorker.executeSkip()` (`BackupWorker.kt:161-162`) and `MANUAL` reaches the normal IMAP path. It MUST accept **only** `BackupType.MANUAL` or `BackupType.SKIP`; passing `REGULAR`/`INCOMING`/`BROADCAST_INTENT`/`UNKNOWN` is a contract violation (those have their own dedicated operations or are not a manual trigger). `scheduleManual` MUST NOT change, replace, or remove `scheduleImmediate()` — `BROADCAST_INTENT` keeps its own unique-work name and its byte-for-byte automation semantics (Rule 3 is untouched). Collapsing MANUAL/SKIP onto `scheduleImmediate()`'s `BROADCAST_INTENT` (losing the SKIP early-return and the manual notification/foreground semantics) is the prohibited behavior this amendment exists to prevent (DES-MODERNIZATION-012 §Integration Design; EPIC-MODERNIZATION-005 SC-10).

## Error Handling

- **Worker failure → retry under INV-3.** A failed `doWork()` returns `Result.retry()`; WorkManager re-enqueues under the EXPONENTIAL/30s policy, with the effective delay capped at 300s at the worker. Non-retryable failures return `Result.failure()` and surface as `SchedulerState.Failed(cause)`.
- **No silent swallowing across the port.** Failures surface to consumers via `observe(jobKind)` emitting `SchedulerState.Failed`, replacing the Otto error-event path. The `SchedulerFailure` cause carries the app-owned reason (consistent with the preserved `LocalizableException` hierarchy in MU-000); no `com.fsck.k9.*` or raw provider exception type crosses this port (those are translated at the MailTransport ACL, MU-008 / a separate boundary).
- **Process death is not an error.** A non-terminal `OneTimeWorkRequest`/`PeriodicWorkRequest` is re-executed by WorkManager after process death/reboot; restore resumes from the durable checkpoint. No restart machinery in the worker.
- **Cancellation is terminal, not failure.** A structured-cancelled job emits `SchedulerState.Cancelled`, distinct from `Failed`.

## Example Payloads

**Example: the public BACKUP broadcast triggers an immediate, unconstrained backup**

```kotlin
// Consumer: the broadcast receiver for com.zegoggles.smssync.BACKUP
// (formerly: new BackupJobs(context).scheduleImmediate())
class BackupBroadcastReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: BackupScheduler   // Hilt-injected port (DES-008)

    override fun onReceive(context: Context, intent: Intent) {
        // Action/receiver/intent-filter preserved byte-for-byte (CNTR-MODERNIZATION-005).
        scheduler.scheduleImmediate()
    }
}
```

The resulting enqueue, expressed as the port's `SchedulerConfig` (what the adapter must produce):

```
SchedulerConfig(
  networkType      = NONE,                  // INV-2: BROADCAST_INTENT has NO network constraint
  requiresCharging = false,                 // INV-2: charging never required
  backoff          = BackoffConfig(EXPONENTIAL, initial=30s, maximum=300s),  // INV-3
  existingPolicy   = REPLACE,               // INV-1
  uniqueName       = "BROADCAST_INTENT",
  initialDelaySecs = null,                  // immediate (Trigger.NOW)
  recurring        = false,
  contentUriTriggers = null
)
```

**Example: manual backup/restore dispatch carrying the engine `BackupType` (v2)**

```kotlin
// Consumer: SmsBackupService.backup(BackupType), after the REQ-013 dispatch migration
// (formerly: getBackupTask().execute(getBackupConfig(..., getMailTransport()))).
// MainActivity.startBackup(MANUAL|SKIP) → startService(action = type) is UNCHANGED.
override fun backup(type: BackupType) {       // type is MANUAL or SKIP from the intent action
    // ... pre-flight checks (permissions, credentials, enabled-types) preserved verbatim ...
    scheduler.scheduleManual(type)            // was scheduleImmediate(); carries MANUAL vs SKIP
}
```

The resulting enqueue, expressed as the port's `SchedulerConfig` (what the adapter must produce):

```
// scheduleManual(BackupType.SKIP):
SchedulerConfig(
  networkType      = NONE,                  // identical to scheduleImmediate (INV-2)
  requiresCharging = false,                 // INV-2
  backoff          = BackoffConfig(EXPONENTIAL, initial=30s, maximum=300s),  // INV-3
  existingPolicy   = REPLACE,               // INV-1
  uniqueName       = "SKIP",                // backupType.name(); distinct from "BROADCAST_INTENT"
  initialDelaySecs = null,                  // immediate
  recurring        = false,                 // SKIP.isRecurring() == false
  contentUriTriggers = null
)
// WorkManager realization adds: addTag("SKIP").
// BackupWorker.inferBackupType() → BackupType.fromName("SKIP") → SKIP →
//   executeBackup() routes to executeSkip() (no IMAP). For MANUAL, uniqueName/tag = "MANUAL"
//   and the worker takes the normal IMAP path.
```

**Example: a regular Wi-Fi-only scheduled backup**

```
// isWifiOnly() == true; getRegularTimeoutSecs() == 7200 (a value <= 0 registers nothing, Rule 4a)
SchedulerConfig(
  networkType      = UNMETERED,             // INV-2
  requiresCharging = false,
  backoff          = BackoffConfig(EXPONENTIAL, 30s, 300s),  // INV-3
  existingPolicy   = UPDATE,                // INV-1 (periodic); CANCEL_AND_REENQUEUE only if UPDATE fails
  uniqueName       = "REGULAR",
  initialDelaySecs = null,                  // periodic interval, not a delay; 60 when the boot path makes a new registration
  recurring        = true,
  contentUriTriggers = null
)
```

**Example: content-URI trigger and its debounce follow-up**

```
// scheduleContentTrigger() — the recurring observer (BackupTriggerWorker) over the SMS
// provider, + the call-log provider when call-log backup is enabled and
// isCallLogBackupAfterCallEnabled()
SchedulerConfig(
  networkType        = NONE,                // the observer does no network I/O; INV-2 applies
                                            //   to the INCOMING backup it enqueues (Rule 2)
  requiresCharging   = false,
  backoff            = null,                // none set: the worker always returns success, so it
                                            //   never retries; INV-3 applies to the INCOMING backup
  existingPolicy     = UPDATE,              // INV-1 (periodic)
  uniqueName         = "CONTENT_TRIGGER",   // also its tag (WorkManagerScheduler.CONTENT_TRIGGER_UNIQUE_NAME; the legacy job's tag was CONTENT_TRIGGER_TAG = "contentTrigger")
  initialDelaySecs   = null,
  recurring          = true,                // PeriodicWorkRequest, every 15 min (WorkManager's minimum)
  contentUriTriggers = [
    ContentUriTrigger(uri = "<sms provider uri>",     triggerForDescendants = true),
    ContentUriTrigger(uri = "<call-log provider uri>", triggerForDescendants = true)  // only when call-log backup is enabled and isCallLogBackupAfterCallEnabled()
  ]
)
// On trigger, the worker does NOT back up directly — it calls scheduler.scheduleIncoming(),
// enqueuing a delayed one-off (delay = getIncomingTimeoutSecs()). INV-4 two-stage debounce.
// That INCOMING one-off carries INV-1 (REPLACE), INV-2 (network per isWifiOnly()) and
// INV-3 (EXPONENTIAL 30s); scheduleIncoming() is its only builder. There is no second
// incoming path (v4).
```

## Dependencies

| Dependency | Relationship |
|------------|--------------|
| **CNTR-MODERNIZATION-005** (`com.zegoggles.smssync.BACKUP` broadcast) | The external event contract that **feeds** this port. The broadcast receiver, on receiving `com.zegoggles.smssync.BACKUP`, resolves the injected `BackupScheduler` and calls `scheduleImmediate()` (the unconstrained `BROADCAST_INTENT` enqueue above). The action string, receiver class, manifest `intent-filter`, and exported semantics are preserved byte-for-byte and are specified there; this contract owns only the scheduler-side reaction. (DES-005 §Public broadcast contract preservation, AC-4.) |
| **DES-MODERNIZATION-005** (WorkManager Scheduler Spine Design) | Parent design; this contract realizes its `BackupScheduler` port, the JobDispatcher→WorkManager contract-mapping table, and the four invariants. |
| **REQ-MODERNIZATION-005** / **migration-units.md MU-005** | Source requirement and migration unit ("THE SPINE"); AC-1..AC-6 trace here. |
| **DES-001** (shared core library) | Hard predecessor — the backup/restore loop must be extracted to core as suspending functions before the workers can be thin adapters (ADR-005-B). |
| **DES-008** (Hilt DI) | Owns the `@HiltWorker`/`HiltWorkerFactory` wiring and the `BackupScheduler` binding (`WorkManagerScheduler` in release; `CompositeScheduler` in debug). This contract states only the injection points it requires. |
| **DES-003** (test harness, Gate G2) | The INV-1..INV-4 + resumable-restore tests run under this harness and MUST be green before the binding flip. |
| **DES-007** (Otto→StateFlow) | Owns the progress-payload migration that `observe()`/`SchedulerState.Running(progress)` surfaces. This contract requires only that progress flow through the port, not Otto. |
| **REQ-MODERNIZATION-001** (SDK/AGP uplift) | Hard predecessor — WorkManager 2.9.x+ requires the AGP/SDK bump (AGP is `4.1.3` today, below floor). |

## Notes

- **Three corrected misreadings carried from DES-MODERNIZATION-005 (verified against `BackupJobs.java`):** (1) `(30, 300)` is **one** exponential strategy — 30s initial, 300s cap — not two separate values and not backup-vs-restore; (2) the immediate `BROADCAST_INTENT` backup has **no** network constraint (`Constraints.NONE`); (3) the execution window is **degenerate** (`executionWindow(s, s)`), so there is no flex window to preserve. A consumer or adapter implemented from a summary rather than this contract is at risk of all three errors.
- **Open verification items deferred to implementation planning** (flagged in DES-005 as "scope assumed — not verified"): the exact `com.zegoggles.smssync.BACKUP` receiver class + manifest `intent-filter` (owned by CNTR-MODERNIZATION-005), and the full set of external `BackupJobs` call sites that become port consumers. Both MUST be grep-confirmed before this contract's consumer list and the broadcast cross-reference are treated as exhaustive.
- **Status:** approved (v4 amended in place by CR-017; v3 amended in place by CR-015; v2 amended in place per DES-MODERNIZATION-012 / REQ-MODERNIZATION-013). All paths are relative to the repository root.
- **v2 implementation note (BackupScheduler.java javadoc).** The port interface javadoc (`BackupScheduler.java:36`) currently reads "all **nine** operations below … are binding clauses." When the implementing story adds `scheduleManual(BackupType)` to `BackupScheduler.java` + `WorkManagerScheduler.kt` (+ the debug `LegacyScheduler`/`CompositeScheduler` impls, if still present), it MUST update that javadoc count to **ten** and add the new method's javadoc. This is a source change owned by the REQ-MODERNIZATION-013 story, not by this contract amendment.
- **Amended 2026-11-21 by CR-015 (v3):** the pre-API-24 clauses are removed because the minSdk 24 floor (REQ-MODERNIZATION-015, CR-011) leaves no supported level below API 24; ruled not a behavioural break. INV-4's version-boundary assertion moves to API 24. The code branch and the test follow in story S1 of DES-MODERNIZATION-014. See the Changelog, v3.
- **Amended 2026-11-21 by CR-017 (v4):** a regular schedule of "Never" schedules nothing (Rule 4a); the boot path registers the regular periodic backup and never a one-time `REGULAR` work (Rule 4; a behavioural break of the boot path, gated by tests); the content-trigger example matches the code (no network constraint, no backoff, `UPDATE`); the SMS-received receiver is no longer part of the contract, by the product owner's decision of 2026-11-21. The code follows in two stories; see "Code not yet at v4" and the Changelog, v4.
