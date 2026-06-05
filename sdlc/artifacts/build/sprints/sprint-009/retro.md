---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-009
started: 2026-06-05
completed: 2026-06-05
---

# Retro: sprint-009 — Fresh-label first-run fix (BUG-013)

## Business Summary

**Objective.** Fix BUG-013 (medium): the first backup to a brand-new Gmail label failed on the first run — the U-043 create→select retry window was too short for Gmail's label propagation, so success only came on the WorkManager retry (~40 s later). No data was lost (U-042), but the first manual backup visibly failed.

**Outcome.** Fixed and **confirmed on-device against live Gmail** — a backup to a brand-new label now succeeds in a single run. 659 tests green, per-package LINE ≥70%, assembleDebug green. Required **three remediations**, each driven by what on-device testing revealed:

1. **Widen retry budget** (3×1 s → 5 attempts, exponential 1/2/4/8 s ≈ 15 s). Unit-tested, reviewed PASS, build green. **On-device: still failed.**
2. **Handle the second transitional state.** On-device showed Gmail returns *two* post-CREATE transitional errors: first `NO [NONEXISTENT]`, then (a second later) `MessagingException: Did not find message count during open`. The retry guard only matched NONEXISTENT, so it gave up. Broadened the predicate to cover both. **On-device: still failed** — even ~15 s on the same connection wasn't enough.
3. **Reconnect-and-retry (the actual root cause).** `ImapFolder.open()` re-acquires a connection from the `ImapStore` pool — the *same* stale connection that can't see the new label. Only a fresh login sees it (which is why the WorkManager retry, with a new `ImapStore`, succeeded). Added `ImapStore.closePooledConnections()` (vendored) and a `forceFreshConnection()` seam that does `folder.close()` + pool-drain before each retry, so `open()` re-logs-in fresh. 6 attempts / ~23 s, bounded + cancellable. **On-device: first-run SUCCESS** — `Creating → not selectable (attempt 1/6); draining pool and retrying → watermark advanced → backedUp=1 → Worker result SUCCESS`, all in one run.

**Key Deliverables.** `K9MailTransport.java` (retry/reconnect), vendored `ImapStore.java` (`closePooledConnections()`), `K9MailTransportCreateOpenTest.java` (+13 tests across remediations); 5 workspace artifacts (all PASS, final review covers the complete fix).

**Decisions Made.**
- The fix lives at the transport call site + a minimal thread-safe vendored pool-drain method, rather than a broader k-9 rewrite — keeps the vendored change to ~10 lines, synchronized on the existing `connections` monitor.
- Budget set to 6 attempts/~23 s with per-retry reconnect: covers both "needs fresh connection" and "needs some propagation time" without an excessive worker block.

## Process Observations

**Process Compliance.** BUG-013 (logged from sprint-008 live testing) → create-stories (U-044) → create-sprint (sprint-009) → execute-sprint (worktree) → wave gate → post-merge on-device validation → 3 remediation loops → final consolidated review → retro. Worktree isolation used throughout (origin/master local-ref workaround; every branch verified `--is-ancestor` before merge; refs restored at close).

**On-device testing was decisive — three times.** Each remediation passed unit tests, code review, security review, and the integrated build, yet the first two still failed the real first-run test against live Gmail. This is the headline lesson: for an integration-level behavior like IMAP label propagation, unit tests and review cannot substitute for on-device validation. The test seam (`getRetryDelayMs`, `forceFreshConnection`) let unit tests assert the control flow without real sleeps/connections, but only the device revealed (a) the second transitional error string and (b) the stale-pooled-connection root cause.

**Root-cause depth.** The bug looked like "retry window too short" (a timing tweak). On-device evidence reframed it twice — first as a missed error condition, then as a connection-pooling issue. The user chose the "most correct" reconnect approach over a crude longer-timeout, which is what actually worked.

**Gate Results.** U-044 final: code PASS (1 non-blocking warning — shift-overflow only at hypothetical attempt≥61, impossible at MAX=6), security PASS (0 findings; TLS/cert-pinning posture preserved on the fresh connection), QA PASS (4/4 ACs, on-device cited). Wave-1 gate PASS.

**Issues Encountered.**
1. **Closing-bookend status mis-derivation (now 9×).** `completed-with-blockers` with 1 done / 0 blocked; corrected to `completed`.
2. **Two premature-done corrections.** U-044 was marked done after the unit-level wave gate, but on-device validation failed; status was reverted to `planned` and the story kept open until the device test actually passed. Good catch — the wave gate alone would have shipped an incomplete fix.
3. **Vendored-module change.** First vendored (`:k9mail-vendored`) source change since U-027; kept minimal and thread-safe; did not break other ImapStore/ImapFolder callers (full suite green).
4. **UI-automation flakiness.** adb `uiautomator`/`input tap` timing caused several missed taps; mitigated by per-step dumps + verifying the screen before tapping.

**Recommendations.**
- Same two standing framework fixes (worktree base should follow session HEAD; bookend terminal-status heuristic).
- Optional hardening follow-up: cap the backoff shift defensively (`attempt <= 13 ? 1000<<(attempt-1) : 8000`) so a future increase of `MAX_CREATE_OPEN_RETRIES` can't overflow — noted by code review, not a current risk.
- BUG-012 (conscrypt SNI log noise) remains logged + deferred (low).
- Consider making on-device validation a required gate for integration-level transport stories, not just a post-merge step — it caught what three rounds of review did not.
