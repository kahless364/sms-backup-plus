---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-006
started: 2026-06-05
completed: 2026-06-05
---

# Retro: sprint-006 — Consolidated-review code fixes (BUG-004/005/006)

## Business Summary

**Objective.** Fix the three code findings from the consolidated review: B-1 (BUG-004, critical), B-2 (BUG-005, high), B-3 (BUG-006, medium).

**Outcome.** All three done, 615 tests green, per-package LINE ≥70%, assembleDebug green.
- **U-036 / BUG-004 (critical):** Unified `SyncStateRepository` to a single instance — `EventModule.provideSyncStateRepository()` now delegates to `App.syncStateRepository()` instead of constructing a second `FlowSyncStateRepository`. Engine (services/workers) and Hilt consumers (`MainViewModel`) now share one `@Singleton`, restoring engine→UI state flow (post-restore default-SMS restoration, progress/event/permission routing). Race-safe: App declares no `@Inject SyncStateRepository`, so the provider is only invoked when `MainViewModel` is created — after `App.onCreate` has set the static. 3 identity/emission tests added.
- **U-037 / BUG-005 (high):** The worker→foreground `WorkInfo` observer is now removed via `removeObserver()` on every teardown/`onDestroy` path (LiveData handle cached); no leak, no duplicate callbacks across restarts. 6 tests added (both services).
- **U-038 / BUG-006 (medium):** Removed 5 dead mail-transport/adapter imports from `RestoreWorker.kt` (ACL purity).

**Key Deliverables.** `App.java`, `di/EventModule.kt`, `service/SmsBackupService.java`, `service/SmsRestoreService.java`, `service/RestoreWorker.kt`; new `EventModuleSingleInstanceTest.kt`; 15 workspace artifacts (3 stories × 5, all PASS).

**Decisions Made.**
- U-036: chose `EventModule → App.syncStateRepository()` delegation (over `@Inject` into App) because it sidesteps Application-field-injection ordering entirely and yields a provably single instance.

## Process Observations

**Process Compliance.** Consolidated review → 5 bugs filed (BUG-004..008) → 3 repair stories (U-036/037/038) → sprint-006 → executed → wave-1 gate PASS (3 done, 15/15 PASS artifacts). BUG-007/008 (security mediums) filed and queued as backlog per plan.

**Deviations.**
- **One Developer agent for all three stories, non-isolated** (worktree isolation remains broken). The three touch disjoint files (App/EventModule vs the two services vs RestoreWorker), so a single non-isolated pass had no intra-tree conflict. Parallel non-isolated agents were deliberately avoided (they'd collide in the shared tree).
- Bug artifacts and repair-story content were authored directly by the orchestrator (citing the committed review reports) rather than via per-artifact RA/SA agent rounds — pragmatic given the findings were already expertly documented; full lifecycle (scaffold/review/finalize) preserved.

**Gate Results.** U-036/037/038: all 5 artifacts PASS each. Wave-1 gate PASS.

**Issues Encountered.**
1. **Closing-bookend status mis-derivation (now 6×).** `completed-with-blockers` with 3 done / 0 blocked; corrected to `completed`.
2. **Worktree-base defect** — unchanged; non-isolated execution used as the standard workaround.
3. The consolidated review itself was the high-value event: it caught **B-1, a critical functional regression** (dual repository breaking engine→UI state) that build + unit tests + launch smoke all missed — only a code-reading review or live restore would surface it.

**Recommendations.**
- Fix the two recurring framework defects (worktree base; bookend terminal-status heuristic) — 6 sprints of manual correction.
- **Run a consolidated review at the end of each modernization phase**, not just at the end — B-1 shipped through five sprints undetected; an earlier review would have caught it sooner.
- On-device confirmations now pending together: U-034 (enrollment dialog), U-035 (no migration StrictMode), **U-036 (engine→UI state: run a restore and confirm `restoreDefaultSmsProvider` + UI updates)**, and the backup/restore round-trip.
- Schedule BUG-007 (token-in-intent) and BUG-008 (silent plaintext on Keystore failure) when convenient.
