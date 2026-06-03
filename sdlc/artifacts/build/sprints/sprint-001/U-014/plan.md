---
artifact_type: plan
story_id: "U-014"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-03"
---

# Plan: U-014 — WorkManagerScheduler Adapter

## Objective

Introduce `WorkManagerScheduler` as a second `BackupScheduler` adapter alongside `LegacyScheduler`.
The four invariants (INV-1 REPLACE, INV-2 constraints, INV-3 backoff, INV-4 content-URI) must be
real and test-verified. Legacy stays as the default binding. No production cutover (U-017).

## Dependencies Verified

- U-013: `BackupScheduler` port + `LegacyScheduler` present at
  `app/src/main/java/com/zegoggles/smssync/scheduler/` — confirmed
- U-003: compileSdk=35, minSdk=21, AGP 8.7.3 — confirmed in `app/build.gradle`
- Kotlin + coroutines enabled (U-019) — confirmed

## Approach

### Step 1 — WorkManager dependency

Add `androidx.work:work-runtime-ktx:2.9.1` and `androidx.work:work-testing:2.9.1` to
`app/build.gradle`. These versions are compatible with compileSdk 35 and work-runtime-ktx 2.9.x
requires minSdk 14+ (our floor is 21).

### Step 2 — Stub workers

Create thin stub workers that WorkManagerScheduler can reference. These are stubs only — actual
execution logic is U-015.

- `app/src/main/java/com/zegoggles/smssync/worker/BackupWorker.kt` — stub `ListenableWorker`
  (not CoroutineWorker yet — U-015) that does nothing and returns success; referenced by
  WorkManagerScheduler for regular/incoming/immediate jobs.
- `app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt` — stub that enqueues
  a delayed incoming backup (implements the INV-4 two-stage debounce); referenced by the
  content-trigger periodic work.

### Step 3 — WorkManagerScheduler

Create `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` implementing
`BackupScheduler`.

Key design decisions from the contract and design documents:
- INV-1: `ExistingWorkPolicy.REPLACE` for one-off, `UPDATE` for periodic
- INV-2: `NetworkType.UNMETERED` when `isWifiOnly()`, `CONNECTED` otherwise; `Constraints.NONE`
  for BROADCAST_INTENT
- INV-3: `BackoffPolicy.EXPONENTIAL`, initial=30s; NOTE: 300s cap is re-imposed at worker
  layer (U-015 responsibility); documented explicitly
- INV-4: `addContentUriTrigger` on API 24+, broadcast fallback on < 24
- Unique names: `BackupType.name()` (REGULAR, INCOMING, BROADCAST_INTENT), "CONTENT_TRIGGER"
- `scheduleBootup`: cancelAll() if !isAutoBackupEnabled(), else schedule regular with 60s delay
- `scheduleRestore`: basic stub returning a ScheduledJob (U-016 owns full implementation)

### Step 4 — CompositeScheduler (debug-only)

Create `app/src/debug/java/com/zegoggles/smssync/scheduler/CompositeScheduler.kt` delegating to
both LegacyScheduler and WorkManagerScheduler, logging divergences at DEBUG level.

### Step 5 — Tests

Create `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt` using
`WorkManagerTestInitHelper` with `SynchronousExecutor` to verify all four invariants:
- `inv1_replaceSemanticsEnqueueSameTwiceYieldsOne` (REPLACE)
- `inv2_networkConstraints_wifiOnly_unmetered` / `inv2_networkConstraints_any_connected` /
  `inv2_immediate_noConstraint` / `inv2_noChargingConstraint`
- `inv3_backoffPolicy_exponential_30s`
- `inv4_contentTrigger_api24_smsUri` / `inv4_contentTrigger_pre24_fallback`

Additionally AC-5 (initialDelay), AC-6 (broadcast contract via annotation check).

### Step 6 — Build verification

Run `./gradlew :app:testDebugUnitTest`, `jacocoTestCoverageVerification`, `assembleDebug`.

## Invariants and test verifiability

| Invariant | Test method | Testable? |
|-----------|-------------|-----------|
| INV-1 REPLACE | `inv1_replaceSemanticsEnqueueSameTwiceYieldsOne` | YES — WorkManager test harness |
| INV-2 Constraints | `inv2_*` tests | YES — inspect WorkSpec |
| INV-3 Backoff 30s | `inv3_backoffPolicy_exponential_30s` | YES — inspect WorkSpec |
| INV-3 300s cap | Documented only (worker-layer, U-015) | DEFERRED to U-015 |
| INV-4 content URI | `inv4_contentTrigger_api24_smsUri` | YES — Robolectric @Config(sdk=28) |
| INV-4 pre-24 fallback | Documented (broadcast path unchanged) | DEFERRED — broadcast receiver unchanged |

## Files to Create/Modify

| File | Action |
|------|--------|
| `app/build.gradle` | Add work-runtime-ktx:2.9.1 + work-testing:2.9.1 |
| `app/src/main/java/com/zegoggles/smssync/worker/BackupWorker.kt` | Create stub worker |
| `app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt` | Create stub trigger worker |
| `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` | Create WM adapter |
| `app/src/debug/java/com/zegoggles/smssync/scheduler/CompositeScheduler.kt` | Create debug composite |
| `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt` | Create INV tests |
| `sdlc/artifacts/build/sprints/sprint-001/U-014/plan.md` | This file |
| `sdlc/artifacts/build/sprints/sprint-001/U-014/implementation-log.md` | Log |
| `sdlc/artifacts/build/sprints/sprint-001/U-014/review-code.md` | Review |
| `sdlc/artifacts/build/sprints/sprint-001/U-014/review-security.md` | Security |
| `sdlc/artifacts/build/sprints/sprint-001/U-014/qa-results.md` | QA |
