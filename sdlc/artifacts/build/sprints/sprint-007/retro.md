---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-007
started: 2026-06-05
completed: 2026-06-05
---

# Retro: sprint-007 — Security-review mediums (BUG-007/008)

## Business Summary

**Objective.** Fix the two security mediums from the consolidated review: BUG-007 (OAuth2 token in Intent extra) and BUG-008 (silent plaintext credentials on Keystore failure).

**Outcome.** Both done. 623 tests green, per-package LINE ≥70%, assembleDebug green.
- **U-039 / BUG-007:** `AccountManagerAuthActivity.useToken()` now persists the token via `AuthPreferences.setOauth2Token(...)` before `setResult` and no longer puts the raw token in an Intent extra; `MainActivity` reads from `AuthPreferences` (checks `hasOAuth2Tokens()`) instead of `EXTRA_TOKEN`. Webflow path untouched. `grep "putExtra(EXTRA_TOKEN"` → none. +4 tests.
- **U-040 / BUG-008:** On a Keystore failure during migration, `EncryptedPrefsSecretStore` sets a persisted `ENCRYPTION_DEGRADED` flag (separate plaintext `credentials_meta` prefs, readable via `isEncryptionDegraded()`), does NOT write `MIGRATION_COMPLETE` (so a later launch retries), and clears the flag on successful migration. Off-main-thread execution (U-035) preserved. +4 tests (failure sets flag; not marked complete; subsequent success completes + clears; no-regression on first-attempt success).

**Key Deliverables.** `AccountManagerAuthActivity.java`, `MainActivity.java`, `EncryptedPrefsSecretStore.java`; new `EncryptedPrefsSecretStoreDegradedTest.java`; 10 workspace artifacts (2 stories × 5, all PASS).

**Decisions Made.**
- U-040 degraded-security mechanism: a persisted boolean flag + gettable state (for a future UI/notification) rather than a full user-facing notification now — documented as the scoped boundary; the important invariant (don't mark migration complete on failure; retry on recovery) is enforced.

## Process Observations

**Process Compliance.** Consolidated-review backlog → BUG-007/008 → U-039/U-040 → sprint-007 → executed → wave-1 gate PASS (2 done, 10/10 PASS artifacts).

**Deviations.** One non-isolated Developer agent for both stories (worktree isolation still broken; disjoint files, so no intra-tree conflict). Bug/story content authored directly by the orchestrator (citing the review reports), full scaffold/review/finalize lifecycle preserved.

**Gate Results.** U-039/U-040: 5/5 PASS each. Wave-1 gate PASS.

**Issues Encountered.**
1. **Agent test-count misreport** — the implementation agent reported 523 tests; authoritative `git grep @Test` on HEAD = 623 (615 + 8 new, zero deletions). Caught by orchestrator verification before closing; no real test loss. (Recurring agent-side miscount pattern across this engagement — orchestrator always verifies the authoritative count.)
2. **Closing-bookend status mis-derivation (now 7×).** `completed-with-blockers` with 2 done / 0 blocked; corrected to `completed`.

**Recommendations.**
- Same two standing framework fixes (worktree base; bookend terminal-status heuristic).
- All consolidated-review findings are now resolved (B-1/B-2/B-3 in sprint-006; M-001/M-002 here). A re-review or the deferred on-device confirmations are the remaining confidence steps.
- On-device confirmations still pending together: U-034 (enrollment dialog), U-035 (no migration StrictMode), U-036 (engine→UI state via a restore), and the backup/restore round-trip. U-040's degraded-flag path can also be exercised by inducing a Keystore failure if desired.
