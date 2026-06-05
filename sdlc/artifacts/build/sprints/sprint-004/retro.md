---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-004
started: 2026-06-04
completed: 2026-06-05
---

# Retro: sprint-004 — BUG-002 fix (TLS enrollment dialog AppCompat-theme crash)

## Business Summary

**Objective.** Fix BUG-002: a FATAL `IllegalStateException: You need to use a Theme.AppCompat theme` when the TLS pin-certificate enrollment dialog tried to show — the `androidx.appcompat.app.AlertDialog` was built with `getApplicationContext()` (no AppCompat theme). Surfaced during live on-device testing of the U-009 feature.

**Outcome.** Done. The `DialogShower` contract was changed to pass an `EnrollmentDialogData` value object; `AdvancedSettings` now builds AND shows the dialog with `getActivity()` (the AppCompat-themed Activity context). The flow retains no Activity reference (anti-leak intent preserved); the app context remains only for `getString` lookups. U-009 behavior preserved (Trust stores via PinnedCertStore; Cancel writes nothing; Cancel is the safe default; cert subject/issuer/SHA-256 fingerprint/expiry shown). Build-verified (595 tests green, per-package LINE ≥70%) and hardened with a Robolectric regression guard.

**Key Deliverables.**
- `PinCertificateEnrollmentFlow.java` — `DialogShower` contract changed to `show(EnrollmentDialogData)`; new `EnrollmentDialogData` value object; `showEnrollmentDialog` produces data, not a dialog.
- `AdvancedSettings.java` — `DialogShower` impl builds `AlertDialog.Builder(getActivity())` + Trust/Cancel wiring (themed context).
- `EnrollmentDialogThemeTest.java` (new) — 3 Robolectric tests guarding the BUG-002 class.
- U-034 workspace artifacts (5, all PASS).

**Decisions Made.**
- SA option A (relocate dialog construction into the themed Activity via a data-carrying `DialogShower`) over passing a themed context into the flow — cleanest separation (flow = fetch/logic, Activity = UI) and preserves the anti-leak boundary.
- Hardened the fix with a JVM/Robolectric contract guard rather than relying solely on a future on-device check.

## Process Observations

**Process Compliance.** Bug → repair-story → sprint → execution chain (BUG-002 → U-034 → sprint-004). Wave-1 gate passed (U-034 done, 5/5 PASS).

**Deviations.**
- **`isolation: worktree` is broken for this repo — abandoned for this sprint.** The first U-034 dispatch (with worktree isolation) branched from upstream `master` (`ef97bfa9`, pre-modernization) instead of `sdlc/modernization-plan` HEAD — the now-confirmed systematic worktree-base defect (also hit in sprint-003). The agent's pre-edit base-check guardrail caught it and stopped cold (touched nothing). U-034 was then re-dispatched to a Developer agent **without worktree isolation**, working directly on the main tree at the correct HEAD. The hardening test was added the same way.
- **On-device AC pending.** U-034 AC-1 (confirm no crash on the device) could not be run (device unavailable). Mitigated by the Robolectric contract guard; on-device confirmation remains a documented residual.

**Gate Results.** U-034: plan/implementation-log/review-code/review-security/qa-results all PASS. Wave-1 gate PASS.

**Issues Encountered.**
1. **Worktree-base defect (now 3×: sprint-003, sprint-004 ×1 attempt).** `isolation: worktree` branches from the repo default branch (`master`), not the session HEAD. Mitigations that worked: (a) a mandatory agent-side base-check that aborts on wrong base; (b) the orchestrator's `git merge-base --is-ancestor HEAD <branch>` pre-merge check; (c) for single-threaded work, running the Developer agent WITHOUT worktree isolation directly on the main tree. Recommend the latter as the default until the tooling is fixed.
2. **Closing-bookend status mis-derivation (now 4×).** `bookend close` again derived `completed-with-blockers` despite the only story `done` and zero blocked. Corrected to `completed`.
3. **Robolectric can't reproduce the exact theme crash.** Robolectric 4.12.2 shadows `AlertDialog.Builder.create()` before `AppCompatDelegateImpl.createSubDecor()` runs, and `ShadowAlertDialog` tracks the framework dialog, not the AppCompat one. So the regression test is a **contract guard** (asserts the dialog context IS-A `Activity`, and verified the guard fails if app context is used) rather than an exact-crash reproduction. Honest residual: on-device is still the definitive confirmation for this defect class.

**Recommendations.**
- **Default to non-isolated Developer agents (or manually-created correct worktrees) until the worktree-base bug is fixed.** This is the third occurrence and nearly caused a full-engagement revert in sprint-003; the workaround is proven.
- **Fix the closing-bookend terminal-status heuristic** (all-`done`/zero-`blocked` ⇒ `completed`) — four consecutive sprints corrected by hand.
- **UI smoke coverage in story DoD.** BUG-002 shipped from sprint-001 because U-009's smoke only checked app launch, not the enrollment dialog path. For UI-bearing stories, the definition of done should include exercising the specific UI path (or a Robolectric inflation guard), not just launch.
- **Run the on-device confirmation for U-034** when the device is next available (drive Advanced Settings → Server → enroll on emulator-5554; confirm no `IllegalStateException` and the dialog renders with Trust/Cancel).
