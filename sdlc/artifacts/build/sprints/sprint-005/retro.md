---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-005
started: 2026-06-05
completed: 2026-06-05
---

# Retro: sprint-005 — BUG-003 fix (credential migration off the main thread)

## Business Summary

**Objective.** Fix BUG-003 (low): the one-time plaintext→encrypted credential migration ran synchronously in `App.onCreate` on the main thread, triggering StrictMode disk-I/O violations and ANR risk (logged-only, not a crash).

**Outcome.** Done. A `CredentialMigrationGate` (CountDownLatch + volatile completion flag) now runs the migration on a background executor; all disk/crypto I/O — including both `commit()` calls — executes off the main thread. First credential access awaits the gate (with a main-thread guard that skips blocking to avoid ANR). U-012 invariants preserved: exactly-once (MIGRATION_COMPLETE), rollback-safe ordering (buffer→encrypt→verify→clear), OAuth2 users not logged out, `commit()` kept (not `apply()`). 606 tests green (12 new), per-package LINE ≥70%, assembleDebug green.

**Key Deliverables.**
- `CredentialMigrationGate.java` (new) — the off-thread dispatch + completion gate.
- `App.java` — migration dispatched via the gate in onCreate (no longer synchronous on the main thread).
- `AuthPreferences.java` — credential access awaits the gate.
- Tests: 7 gate tests + 5 AuthPreferences U-035 tests.
- U-035 workspace artifacts (5, all PASS).

**Decisions Made.**
- SA option A: background executor + CountDownLatch completion gate, with a documented happens-before race-safety argument (volatile write in signalComplete happens-after all migration writes; consumers either observe the volatile flag or block on the latch).
- Main-thread `awaitIfNeeded()` skips blocking (fast-path volatile read) to avoid trading a StrictMode warning for an ANR.

## Process Observations

**Process Compliance.** Bug → repair-story → sprint → execution chain (BUG-003 → U-035 → sprint-005). Wave-1 gate passed (U-035 done, 5/5 PASS).

**Deviations.**
- **Ran the Developer agent WITHOUT worktree isolation** (directly on the main tree), the now-standard workaround for the systematic worktree-base defect. No issues; HEAD verified before edits.

**Gate Results.** U-035: plan/implementation-log/review-code/review-security/qa-results all PASS. Wave-1 gate PASS.

**Issues Encountered.**
1. **Closing-bookend status mis-derivation (now 5×).** `bookend close` again derived `completed-with-blockers` with the only story `done` and zero blocked. Corrected to `completed`. This is a deterministic, reproducible engine bug across sprints 002–005.
2. **On-device/Robolectric StrictMode-absence not run.** Device unavailable; the threading change + 12 unit tests are the device-free evidence. A Robolectric/on-device StrictMode check is a recommended follow-up confirmation (AC-7).

**Recommendations.**
- **Fix the closing-bookend terminal-status heuristic** — five consecutive sprints corrected by hand; an all-`done`/zero-`blocked` story set must yield `completed`.
- **Fix the worktree-base tooling defect** (carried from sprint-003/004); non-isolated execution remains the safe default meanwhile.
- When the device is next available, run the deferred confirmations together: U-034 enrollment-dialog (no crash) and U-035 (no StrictMode migration violation), plus the originally-planned backup/restore round-trip.
