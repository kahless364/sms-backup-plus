---
artifact_type: plan
story_id: "U-019"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-02T00:00:00Z"
---

# Plan: U-019 — SyncStateRepository facade

> Backfilled by the orchestrator during the Wave 5 gate. The implementing agent
> produced implementation-log/qa-results/review-code/review-security but omitted this
> plan.md and used non-canonical review filenames (now normalized). Content reflects the
> approach actually taken (see implementation-log.md for the authoritative record).

## Objective

Introduce a `SyncStateRepository` abstraction (CNTR-MODERNIZATION-006) as a
branch-by-abstraction facade — the seam step for DES-MODERNIZATION-007. NOT the Otto
removal (that is U-020) and NOT the MainViewModel decomposition (U-021).

## Approach

1. Enable Kotlin in the project (kotlin-android plugin 1.9.25, stdlib, kotlinx-coroutines
   1.7.3) — required because the contract specifies `StateFlow<SyncState>` /
   `SharedFlow<SyncEvent>` which are Kotlin types. Minimal enablement; no existing Java
   converted.
2. Define `SyncStateRepository` (interface), `SyncState` (typealias to existing `State`),
   `SyncEvent` (sealed class mirroring existing Otto event POJOs), and
   `DefaultSyncStateRepository` (Otto-delegating implementation — `emitState`/`emitEvent`
   forward to `App.bus.post`; consumers still register with Otto under the hood).
3. Wire a single proof-of-life call site (`SmsBackupService` state emission) through the
   facade to prove the delegation path works with zero runtime behavior change.
4. Keep Otto on the classpath; full Flow swap + Otto deletion deferred to U-020.

## Acceptance / Verification

- `SyncStateRepository` interface + `DefaultSyncStateRepository` Otto-delegating impl exist.
- New unit tests for the facade delegation and `SyncEvent` variants.
- Full suite green; JaCoCo 70% gate holds; assembleDebug succeeds.

## Deviations

- The design specifies Kotlin `StateFlow`/`SharedFlow`; the project had no Kotlin. Kotlin
  was added within this story (minimal) rather than blocked — documented in
  implementation-log.md. `ThemeChanged` implemented as a payload-less `object` to match
  the source `ThemeChangedEvent` (no fields).
