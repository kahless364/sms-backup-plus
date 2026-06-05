---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-010
started: 2026-06-05
completed: 2026-06-05
---

# Retro: sprint-010 — SNI log noise + edge-to-edge insets (BUG-012, BUG-014)

## Business Summary

**Objective.** Two API-35-era fixes surfaced in exploratory testing: BUG-012 (low — conscrypt `setHostname` SNI `NoSuchMethodException` log spam) and BUG-014 (medium — app content drawing under the system bars on Android 15 forced edge-to-edge; toolbar title overlapped the status-bar clock, nav bar area unusable).

**Outcome.** Both fixed and **confirmed on-device** (emulator-5554, API 37). 666 tests green, per-package LINE ≥70%, assembleDebug green. Code/security/QA reviews all PASS. Executed with worktree isolation.

- **U-045 / BUG-012:** `DefaultTrustedSocketFactory` now sets TLS SNI via the public `SSLParameters.setServerNames(new SNIHostName(host))` API on API 24+, keeping the reflective `setHostname` path only as a pre-24 fallback and demoting its failure log from ERROR+stack-trace to a single DEBUG line. **On-device:** a real backup ran (`backedUp=1`) and the logcat `NoSuchMethodException`/`setHostname` count was **0**. TLS/cert-validation/pinned-cert path untouched.
- **U-046 / BUG-014:** new shared `WindowInsetsUtil` helper (`WindowCompat.setDecorFitsSystemWindows(window,false)` + `ViewCompat.setOnApplyWindowInsetsListener` consuming `systemBars()|displayCutout()`), applied from `MainActivity` (the only activity hosting a toolbar/content layout — audited). **On-device:** status-bar clock/icons fully visible, "SMS Backup+" title + overflow menu below the status bar, green toolbar still edge-to-edge.

**Key Deliverables.** `DefaultTrustedSocketFactory.java` (vendored), `WindowInsetsUtil.java` (new), `MainActivity.java`, `main.xml`; tests `DefaultTrustedSocketFactorySniTest` (+4), `WindowInsetsUtilTest` (+3); 10 workspace artifacts (2×5, all PASS).

**Decisions Made.**
- U-045: prefer the public SNI API over reflection (eliminates the error at the source) rather than merely silencing the log.
- U-046: a single shared insets helper invoked per top-level activity; an audit confirmed only MainActivity needs it (others are dialog/transparent).

## Process Observations

**Process Compliance.** BUG-012 (carried from sprint-008) + BUG-014 (logged this session from the user's report) → create-stories (U-045, U-046) → create-sprint (sprint-010) → execute-sprint (worktrees) → wave gate → post-merge on-device validation → close. Worktree-base local-ref workaround used; both branches verified `--is-ancestor` before merge; refs restored at close.

**On-device validation caught the real finish line again.** Both stories passed unit tests, code/security/QA review, and the integrated build — yet U-046's on-device screenshot revealed the code-review **W-1 warning had materialized**: top-inset padding on a fixed `actionBarSize` Toolbar clipped the title (status bar fixed, but title gone). The W-1 fix (`wrap_content` + `minHeight`) was applied post-merge and re-verified on-device. The reviewer predicted it; the device confirmed it. Same lesson as sprint-009: for visual/integration behavior, the on-device check is the true gate.

**Tooling gotcha — `MSYS_NO_PATHCONV` breaks gradlew.** Setting `export MSYS_NO_PATHCONV=1` (needed for adb `/sdcard/...` paths) in the same shell as `./gradlew` caused `Could not find or load main class org.gradle.wrapper.GradleWrapperMain` (it disables the Git-Bash path conversion gradlew relies on for its `-classpath`). Cost two false "transient" build failures before diagnosis. Fix: never set `MSYS_NO_PATHCONV` in a shell that runs gradlew; scope it to adb-only commands. Worth a CLAUDE.md note.

**Gate Results.** U-045: code PASS (2 non-blocking warnings — SNIHostName lacks an IP-literal guard; minor test aliasing), security PASS (TLS posture preserved), QA PASS (4/4). U-046: code PASS (W-1 medium → remediated; W-2/W-3 low), security PASS (security-neutral), QA PASS (4/4, visual ACs deferred then confirmed on-device). Wave-1 gate PASS.

**Issues Encountered.**
1. **Closing-bookend status mis-derivation (now 10×).** `completed-with-blockers` with 2 done / 0 blocked → corrected to `completed`.
2. **U-046 plan.md not committed from worktree.** The worktree agent wrote plan.md but it wasn't in the merge (only implementation-log.md was); authored post-merge to complete the artifact set. Minor — watch worktree artifact propagation.
3. **W-1 toolbar clipping** (above) — remediated post-merge.
4. **Incoming-SMS notification stealing focus** repeatedly pulled Google Messages to the foreground, causing missed backup taps; mitigated by re-foregrounding the app and re-locating the (shifted) BACKUP button.

**Recommendations.**
- Add a CLAUDE.md note: do not run `./gradlew` with `MSYS_NO_PATHCONV=1` set.
- U-045 follow-up (optional, low): guard `new SNIHostName(host)` against IP-literal/empty hosts to avoid `IllegalArgumentException` on non-DNS IMAP hosts (code-review W-1).
- Two standing framework fixes persist (worktree base; bookend terminal-status heuristic).
- Backlog is now clear of known bugs: BUG-001..014 all resolved.
