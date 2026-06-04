---
status: approved
severity: medium
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-013
design_docs:
  - DES-MODERNIZATION-012
related_stories: []
tags:
  - cntr-004
  - backuptype
  - schedulemanual
  - retroactive
id: CR-001
title: 'CNTR-MODERNIZATION-004 v2: add scheduleManual(BackupType) and correct single-enum BackupType model'
type: DCR
domain: change-records
summary: 'Retroactive CR for the in-place v1->v2 amendment of CNTR-MODERNIZATION-004 applied 2026-06-04: (a) adds the scheduleManual(BackupType) port operation and Validation Rule 8 required by REQ-MODERNIZATION-013 to preserve MANUAL/SKIP engine semantics; (b) corrects the BackupType documentation from a three-member abstraction to the true six-member single enum.'
---

# CR-001: CNTR-MODERNIZATION-004 v2 — scheduleManual(BackupType) addition and single-enum BackupType correction

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-001 |
| **Type** | DCR — Design Correction |
| **Severity** | Medium |
| **Status** | Open |
| **Reported By** | Requirements Analyst (automated cross-layer consistency check during EPIC-MODERNIZATION-005 design phase) |
| **Date Reported** | 2026-06-04 |
| **Source** | Sprint work — discovered during DES-MODERNIZATION-012 design session while modeling the REQ-MODERNIZATION-013 manual dispatch migration |

---

## Finding

### Description

During the DES-MODERNIZATION-012 design session for EPIC-MODERNIZATION-005, two related defects were found in CNTR-MODERNIZATION-004 v1 (the approved `BackupScheduler` port contract):

**Defect 1 — Missing port operation: `scheduleManual(BackupType)`.** REQ-MODERNIZATION-013 requires migrating `SmsBackupService.backup()` off the legacy `startService(...)` → `AsyncTask` dispatch path and onto the `BackupScheduler` port. The existing `scheduleImmediate()` operation cannot be used for this migration: it hard-codes `BackupType.BROADCAST_INTENT` as the tag carried to the worker (verified: `WorkManagerScheduler.kt:274,278`). If manual backup and restore dispatch were routed through `scheduleImmediate()`, the `BackupWorker.inferBackupType()` resolver (`BackupWorker.kt:430-436`) would return `BROADCAST_INTENT` for every manual invocation, collapsing the `MANUAL`/`SKIP` distinction. The `SKIP` path (`BackupWorker.executeSkip()`, `BackupWorker.kt:161-162`) would become unreachable, changing notification, foreground-service, and early-return behavior. A distinct `scheduleManual(BackupType)` operation is required — one that carries the caller-supplied `BackupType` (`MANUAL` or `SKIP`) to the worker via `addTag(backupType.name())` while remaining constraint-identical to `scheduleImmediate()` (`Constraints.NONE`, REPLACE, EXPONENTIAL/30s) and using its own unique-work name (`backupType.name()`), so a manual run and an automation-broadcast run do not replace each other in the WorkManager queue.

**Defect 2 — Incorrect BackupType model: three-member abstraction vs. the actual six-member shared enum.** CNTR-MODERNIZATION-004 v1 enumerated only the three `BackupType` values the scheduler discriminator used at the time: `REGULAR`, `INCOMING`, and `BROADCAST_INTENT`. The actual source enum `com.zegoggles.smssync.service.BackupType` (`BackupType.java:6-12`) has **six** members: `BROADCAST_INTENT`, `INCOMING`, `REGULAR`, `UNKNOWN`, `MANUAL`, `SKIP`. There is exactly one `BackupType` enum in the codebase — it is shared by the scheduler (`WorkManagerScheduler.kt:26` imports `com.zegoggles.smssync.service.BackupType`) and the engine/service layer (`BackupWorker`, `SmsBackupService`). The v1 contract's three-member enumeration implied a false partition: that there was a "scheduler-level" subset distinct from an "engine-level" set. That partition does not exist. The `MANUAL` and `SKIP` members the manual dispatch migration depends on are additional members of this same shared enum. Documenting only three members made the contract an inaccurate specification and would have caused any implementor reading v1 in isolation to produce a `scheduleManual` that operated on a non-existent "engine-only enum" rather than on the real `service.BackupType`.

Both defects were corrected together in a single in-place amendment to the approved contract (v1 → v2, 2026-06-04). This CR exists to provide retroactive traceability of that change to an approved artifact.

### Source of Truth

1. **`app/src/main/java/com/zegoggles/smssync/service/BackupType.java:6-12`** — the single authoritative `BackupType` enum; verified to contain six members: `BROADCAST_INTENT(R.string.source_3rd_party)`, `INCOMING(R.string.source_incoming)`, `REGULAR(R.string.source_regular)`, `UNKNOWN(R.string.source_unknown)`, `MANUAL(R.string.source_manual)`, `SKIP(R.string.source_manual)`.
2. **`app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt:26`** — confirms the scheduler imports `com.zegoggles.smssync.service.BackupType`; the same type is used at the engine layer. No second `BackupType` enum exists.
3. **REQ-MODERNIZATION-013** (`sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-013-remove-legacy-asynctask-execution-path.md`, status: approved) — the driving requirement; its AC-7 requires `SmsBackupService.backup()` to delegate to the `BackupScheduler` rather than constructing an `AsyncTask`. The MANUAL/SKIP semantics carried by `SmsBackupService` through the service's `handleIntent()` intent action must be preserved at the WorkManager layer.
4. **DES-MODERNIZATION-012** (`sdlc/artifacts/design/modernization/DES-MODERNIZATION-012-modernization-debt-closure-design.md`, status: approved) — the design that uncovered both defects during modeling; references CNTR-MODERNIZATION-004 in its `integration_contracts` frontmatter.
5. **CNTR-MODERNIZATION-004 v2 Changelog** (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`, lines 249-255) — the v2 entry is the primary record of the amendment; this CR is its traceability counterpart.

### Evidence

**E1 — Six-member enum confirmed at `BackupType.java:6-12`:**

```java
// BackupType.java:6-12 (verified)
public enum BackupType {
    BROADCAST_INTENT(R.string.source_3rd_party),
    INCOMING(R.string.source_incoming),
    REGULAR(R.string.source_regular),
    UNKNOWN(R.string.source_unknown),
    MANUAL(R.string.source_manual),
    SKIP(R.string.source_manual);
```

v1 documented only `REGULAR`, `INCOMING`, `BROADCAST_INTENT`. The three undocumented members (`UNKNOWN`, `MANUAL`, `SKIP`) were present in source throughout; their omission was an accuracy defect in the contract.

**E2 — `scheduleImmediate()` hard-codes `BROADCAST_INTENT`, confirming the tag-collision risk (`WorkManagerScheduler.kt:270-282`):**

```kotlin
// WorkManagerScheduler.kt:270-282 (verified)
override fun scheduleImmediate(): ScheduledJob? {
    val request = OneTimeWorkRequest.Builder(BackupWorker::class.java)
        .setConstraints(Constraints.NONE)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_INITIAL_SECS, TimeUnit.SECONDS)
        .addTag(BackupType.BROADCAST_INTENT.name)          // hard-coded: always "BROADCAST_INTENT"
        .build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork(BackupType.BROADCAST_INTENT.name, ExistingWorkPolicy.REPLACE, request)
    ...
}
```

If `SmsBackupService.backup(SKIP)` called `scheduleImmediate()`, the worker tag would be `"BROADCAST_INTENT"` and `inferBackupType()` would resolve to `BackupType.BROADCAST_INTENT`, never reaching `executeSkip()`.

**E3 — `BackupWorker.inferBackupType()` tag-resolution mechanism (`BackupWorker.kt:430-436`, verified):**

```kotlin
// BackupWorker.kt:430-436
private fun inferBackupType(): BackupType {
    for (tag in tags) {
        val type = BackupType.fromName(tag)
        if (type != BackupType.UNKNOWN) return type
    }
    return BackupType.BROADCAST_INTENT   // fallback — no tag matched
}
```

The method depends entirely on the worker tag to distinguish `MANUAL` from `SKIP` from `BROADCAST_INTENT`. Without `addTag(backupType.name())` on the enqueue, all three collapse to `BROADCAST_INTENT`.

**E4 — `BackupWorker.executeSkip()` routing (`BackupWorker.kt:161-162`, verified):**

```kotlin
// BackupWorker.kt:155-166
private suspend fun executeBackup(...): Result {
    return if (config.backupType == BackupType.SKIP) {
        executeSkip(config, preferences)         // no IMAP; marks max synced date only
    } else {
        fetchAndBackupItems(config, preferences, authPreferences, ctx)
    }
}
```

`SKIP` is a distinct execution path. Collapsing it onto `BROADCAST_INTENT` silently converts a "mark as backed up without connecting to IMAP" operation into a full IMAP backup run.

**E5 — CNTR-004 v2 Changelog entry (contract lines 249-255, verified):** The v2 changelog entry explicitly records all four changes: addition of `scheduleManual(backupType: BackupType): Unit`, the single-enum reconciliation, Validation Rule 8, and the example payload. Net operation count noted as "nine → ten."

**E6 — `BackupScheduler.java` javadoc still reads "nine operations" (line 37, verified):** The interface source file (`app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java:37`) reads `"all nine operations below"`. `scheduleManual` has not yet been added to the interface (the contract was amended in the specification; implementation is pending the REQ-MODERNIZATION-013 story). This is the expected state for an in-place contract amendment before implementation — the source change is owned by the implementing story, not by the contract edit itself.

---

## Triage

### Classification

- **Type:** DCR (Design Correction)
- **Root:** Design layer — the defect is in the specification artifact (CNTR-MODERNIZATION-004), not in any implementation. No production code is incorrect at the time this CR is filed; `scheduleManual` has not yet been implemented against either the v1 or v2 surface.
- **Layers Affected:** 1 (specification layer: the contract itself). The amendment is already applied in-place; downstream layers (implementation, stories) are not yet affected because no story has been authored against the v1 surface for the `scheduleManual` capability.
- **Severity Justification:** Medium. The amendment corrects a specification that has not yet been implemented against — no running code is wrong, no active sprint story is blocked, and the fix (in-place contract edit) is already done. Medium is appropriate because: (a) the incorrect v1 model would have caused a behavioral regression at implementation time (SKIP semantics silently dropped) had a developer implemented the REQ-013 dispatch migration from the v1 contract; (b) the scope is bounded to one contract plus one upcoming requirement's stories; (c) no approved story has been authored against the incorrect surface; and (d) the contract retains `approved` status without requiring re-approval because the amendment is additive and corrective — no invariant (INV-1..4) is changed and no existing consumer API surface is altered.

**Retroactive status.** This CR is filed after-the-fact. The amendment was applied in-place to CNTR-MODERNIZATION-004 on 2026-06-04 as part of the DES-MODERNIZATION-012 design session. The contract's own v2 Changelog entry (lines 249-255) is the primary record of the change. This CR exists solely to satisfy the traceability requirement that changes to approved artifacts be recorded in the change-record log.

---

## Impact Analysis

Since `change_records.layers` is not configured in `sdlc/config.yaml`, the following cascade analysis is performed manually against the known artifact graph.

### Layer Impact Matrix

| # | Layer | Artifact | Affected? | Action Needed |
|---|-------|----------|-----------|---------------|
| 1 | Specification (contract) | CNTR-MODERNIZATION-004 | **YES — root; already fixed** | Amendment applied in-place v1→v2 on 2026-06-04. No further action on the contract itself. |
| 2 | Requirement | REQ-MODERNIZATION-013 | **YES — inform** | REQ-013 text correctly requires migration of `SmsBackupService.backup()` onto the scheduler and references CNTR-004. The requirement's own narrative does not name `scheduleManual` explicitly (it predates the v2 amendment). Story authoring for REQ-013 must honor the v2 contract surface. No text change to REQ-013 is required — the requirement is stated at the behavioral level and the v2 contract is its implementation specification. |
| 3 | Design | DES-MODERNIZATION-012 | **YES — inform** | DES-012 references CNTR-MODERNIZATION-004 in its `integration_contracts` frontmatter and its integration design section. The design was authored concurrently with the v2 amendment and is consistent with v2. No change to DES-012 is required. |
| 4 | Stories for REQ-MODERNIZATION-013 | Not yet authored | **YES — authoring constraint** | When stories are authored for REQ-013, they must: (a) describe MANUAL and SKIP as members of the single shared `com.zegoggles.smssync.service.BackupType` enum, NOT a separate engine-level type; (b) require the implementing developer to call `scheduleManual(backupType)` (not `scheduleImmediate()`) for manual dispatch; (c) require `addTag(backupType.name())` on the WorkManager enqueue so `BackupWorker.inferBackupType()` resolves the correct type. See "Next Steps" below. |
| 5 | Implementation — `BackupScheduler.java` | `app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java` | **YES — pending** | The interface javadoc at line 37 reads "all **nine** operations below." The implementing story for REQ-013 must add `scheduleManual(BackupType backupType)` to this interface AND update the javadoc count from **nine** to **ten**. This is not a CR repair action — it is a normal story implementation task. Noted here so the story author does not miss it. |
| 6 | Implementation — `WorkManagerScheduler.kt` | `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` | **YES — pending** | `scheduleManual(BackupType)` must be added to the `WorkManagerScheduler` adapter. The implementation is owned by the REQ-013 story. |
| 7 | Implementation — debug adapters (`LegacyScheduler`, `CompositeScheduler`) | Pending; may or may not exist at story time | **YES — pending (conditional)** | If `LegacyScheduler` and/or `CompositeScheduler` are still present when the REQ-013 story is implemented, they must also implement `scheduleManual`. The CNTR-004 contract notes these are debug-only and may be deleted before REQ-013 lands. The implementing developer must confirm their presence at story time. |
| 8 | Javadoc / internal notes referencing CNTR-004 operation count | Various (none identified as of CR filing) | No additional instances identified | If any artifact references "nine BackupScheduler operations" by count, it must be updated to "ten." Grep over `sdlc/` for `nine operations` is advisory at story time. |

### Cascade Direction

Isolated downstream propagation: the defect originated and was corrected in the specification layer before reaching any implementation. The cascade is forward-only (contract → stories → implementation) and affects only the REQ-013 work stream. No existing approved story, no merged implementation, and no test suite is affected.

---

## Suggested Approach

Advisory only — refined during story planning.

### This CR carries no contract-repair work

The CNTR-MODERNIZATION-004 v1→v2 amendment (the `scheduleManual(BackupType)` operation, Validation Rule 8, the single-enum reconciliation, and the v2 example payload) is **already applied** to the approved contract. The specification is now correct. There is therefore **no contract-repair code story** to schedule: nothing in source needs to be changed to make the *contract* right, because the artifact that was wrong has already been fixed in place. The value this CR delivers is forward-looking — it ensures the still-unauthored REQ-MODERNIZATION-013 stories are written against the v2 surface rather than the obsolete v1 model. Treat the approach below as guidance for that authoring step, not as a work item in its own right.

### One story, not a separate one — the migration owns the call site

The `scheduleManual` call-site migration is **not** a standalone story. The reason `scheduleManual(BackupType)` exists at all is to give REQ-MODERNIZATION-013 a port operation that carries `MANUAL`/`SKIP` to the worker; the operation has no independent consumer. Splitting "add `scheduleManual` to the port" from "migrate `SmsBackupService.backup()` off the `AsyncTask` path" would create a component (a new interface method plus its `WorkManagerScheduler` adapter body) with no story owning its integration — precisely the orphaned-integration shape that planning is meant to prevent. The cleaner decomposition is to fold the interface addition, the adapter implementation, and the `SmsBackupService.backup()` re-point into the single REQ-013 dispatch-migration story that already owns the end-to-end `startService(...)`→`AsyncTask`→scheduler-port wiring. That story is the natural owner of the new operation's first and only call site.

### Constraints the REQ-013 story authoring must carry forward

These are correctness constraints the v2 contract already specifies; they are surfaced here so the story author does not have to reverse-engineer them from the contract changelog:

- **Call `scheduleManual(BackupType)`, never `scheduleImmediate()`, for manual/skip dispatch.** `scheduleImmediate()` hard-codes the tag and the unique-work name to `BackupType.BROADCAST_INTENT.name` (verified this session at `WorkManagerScheduler.kt:274` and `:278`). Routing a manual run through it would tag the work `"BROADCAST_INTENT"`, and `BackupWorker.inferBackupType()` would resolve every manual invocation to `BROADCAST_INTENT` — silently dropping the `SKIP` execution path.
- **Model `MANUAL` and `SKIP` as members of the one shared `com.zegoggles.smssync.service.BackupType` enum.** There is a single `BackupType` enum (six members), imported by both the scheduler and the engine. Story text must not describe a separate "engine-level" or "scheduler-level" enum — that partition does not exist, and authoring as if it did would mislead the implementer into building `scheduleManual` against a phantom type.
- **Bump the `BackupScheduler.java` javadoc operation count from nine to ten.** The "all nine operations below" token is at `BackupScheduler.java:36` (verified this session; the CR's "line 37" reference points at the same contract-clause javadoc block). When the interface gains `scheduleManual(BackupType)`, this count must move to ten in the same change. This is ordinary story implementation work, not a CR action — noted so it is not missed.

### Risk to keep visible during planning

**Unique-work-name collision.** `scheduleImmediate()` enqueues under the unique-work name `BackupType.BROADCAST_INTENT.name` with `ExistingWorkPolicy.REPLACE` (`WorkManagerScheduler.kt:278`). `scheduleManual` must enqueue under `backupType.name()` (i.e. `"MANUAL"` or `"SKIP"`) — a name that is inherently distinct from `"BROADCAST_INTENT"`. Keeping the names distinct is what prevents a third-party automation broadcast (BROADCAST_INTENT) and a user-initiated manual run from `REPLACE`-evicting each other in the WorkManager queue. The story should treat "manual and broadcast runs use distinct unique-work names so neither replaces the other" as an explicit correctness property to confirm, since both paths share the `REPLACE` policy and an accidental name reuse would re-introduce the collision the new operation was created to avoid.

---

## Cascade Analysis

**Root Layer:** Specification (CNTR-MODERNIZATION-004)
**Root Defect:** v1 documented only three of the six `BackupType` members and omitted the `scheduleManual` operation needed to carry `MANUAL`/`SKIP` to the worker without collapsing onto `BROADCAST_INTENT`.
**Cascade Direction:** Forward-only downstream; isolated to the REQ-013 work stream. The amendment was applied before any story was authored against the incorrect surface.

### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Specification (CNTR-004 v1) | Requirement (REQ-013) | Partial — behavioral intent correct; `scheduleManual` naming absent | REQ-013 requires scheduler-path migration but predates the `scheduleManual` naming; the v2 contract is now the authoritative specification for implementation |
| 2 | Specification (CNTR-004 v1) | Stories for REQ-013 | Not yet propagated — no stories authored | Stories not yet authored; this CR is the gate that ensures authoring uses v2 |
| 3 | Specification (CNTR-004 v1) | Implementation (`BackupScheduler.java`, `WorkManagerScheduler.kt`) | Not yet propagated — `scheduleManual` not yet implemented | Implementation pending REQ-013 stories; no incorrect implementation exists |

### Fix Order

1. [x] Specification layer (CNTR-MODERNIZATION-004): in-place v1→v2 amendment — ROOT FIX (already done, 2026-06-04)
2. [ ] Story authoring (REQ-MODERNIZATION-013 stories): author stories against v2 surface — CASCADE GATE (this CR is the traceability record ensuring story authors read v2)
3. [ ] Implementation (`BackupScheduler.java`): add `scheduleManual(BackupType)` and update javadoc count nine→ten — IMPLEMENTATION (owned by REQ-013 story)
4. [ ] Implementation (`WorkManagerScheduler.kt`): implement `scheduleManual(BackupType)` per Validation Rule 8 — IMPLEMENTATION (owned by REQ-013 story)

### Affected In-Progress Work

No stories for REQ-MODERNIZATION-013 are currently in-progress or drafted against the incorrect v1 surface. The amendment was applied before story authoring began.

---

## Plan

- **Execution Window:** Current sprint planning — CR filed and queued; the amendment is already reflected in the contract. No separate repair story is needed.
- **Assigned To:** Story author for REQ-MODERNIZATION-013 stories (must read v2 contract before authoring)
- **Estimated Effort:** Zero additional effort for the contract repair (already done). Story-authoring effort for REQ-013 is unchanged — the v2 contract provides the complete specification.
- **Sprint:** Next sprint that schedules REQ-013 story authoring

### Fix Checklist

- [x] Specification: CNTR-MODERNIZATION-004 amended in-place to v2 — DONE
- [ ] Story authoring: REQ-013 stories use `scheduleManual(BackupType)` and the six-member `service.BackupType` enum model
- [ ] Implementation: `BackupScheduler.java` adds `scheduleManual(BackupType)` and updates javadoc count to ten
- [ ] Implementation: `WorkManagerScheduler.kt` implements `scheduleManual(BackupType)` per Validation Rule 8
- [ ] Verification: `BackupWorker.inferBackupType()` returns `MANUAL` or `SKIP` for manually dispatched jobs (not `BROADCAST_INTENT`)

---

## Execution

### Changes Made

| File | Change | Layer |
|------|--------|-------|
| `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` | v1→v2 in-place amendment: added `scheduleManual(BackupType)` operation, Validation Rule 8, single-enum reconciliation, v2 example payload, and v2 Changelog entry | Specification |

### Verification Evidence

The v2 Changelog entry at CNTR-MODERNIZATION-004 lines 249-255 is the primary verification that the amendment was applied. The six-member enum is confirmed at `BackupType.java:6-12` (read this session). The `scheduleImmediate()` BROADCAST_INTENT hard-coding is confirmed at `WorkManagerScheduler.kt:274,278` (read this session). No implementation of `scheduleManual` exists yet — correct, expected state.

### Commit Traceability

| Commit | Date | Branch | Scope | Author |
|--------|------|--------|-------|--------|
| (amendment applied in-place during DES-MODERNIZATION-012 design session) | 2026-06-04 | sdlc/modernization-plan | CNTR-MODERNIZATION-004 v2 | Michael Horsley |

---

## Closure

- **Root Cause Category:** Incomplete specification — the initial contract documented a partial view of an existing enum (three of six members) and omitted an operation that was only identified as necessary during the downstream requirement's design phase.
- **Prevention Recommendation:** When authoring a contract that references a source-code enum, read the actual enum definition in full before documenting its values. A contract that lists a subset of an existing enum's members is inaccurate from the moment it is written. For future contracts in this domain: grep-verify any enum referenced in a contract during the contract-authoring session (as was done during this DES-012 amendment session) and enumerate all members explicitly, marking any members not used by the current contract scope as "present in source; out of scope for this contract."
- **Closed By:** (pending — remains open until REQ-013 stories are authored and implementation is complete)
- **Date Closed:** (pending)
