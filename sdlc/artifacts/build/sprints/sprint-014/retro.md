---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-014
started: 2026-06-24
completed: 2026-06-24
---
# Retro: sprint-014 — Security Hardening (Batch C)

## Business Summary
**Objective.** Close the cheap, high-trust security gaps from the assessment (SE-002/003/004): enforce the ENCRYPTION_DEGRADED flag, move security-crypto off alpha, add a no-cleartext network config + clearer opt-in copy.
**Outcome.** All 3 stories done; 697 tests green (was 690 → +28 incl remediation), jacoco ≥70%, dep-verification green.
- **U-054 (SE-002, completes BUG-008):** `ENCRYPTION_DEGRADED` now gates backup/restore at MainViewModel + every WorkManagerScheduler path (manual, immediate/broadcast, AND the automatic incoming/regular/content-trigger/bootup/restore paths after remediation) and renders a persistent launch warning. U-040 rollback-safe migration + BUG-008 retry preserved (flag not latched; clears on recovery).
- **U-055 (SE-003/BT-004):** `security-crypto 1.1.0-alpha06 → 1.1.0 stable` (no API change; StrongBox best-effort retained); verification-metadata SHA-256 pinned (no wildcard); AES-256-GCM/SIV scheme regression test added.
- **U-056 (SE-004 + SE-001-UX):** `network_security_config.xml` with `cleartextTrafficPermitted=false` wired to `<application>`; opt-in copy clarified to name the third-party-broadcast risk; `BackupBroadcastReceiver` untouched (CNTR-MODERNIZATION-005, no permission added).

## Process Observations
- **Multi-reviewer caught a real runtime miss (again).** U-054 passed dev self-test AND QA, but the Code Reviewer found the launch warning was silently dropped: `StatusPreference` early-returns on background-type states and `BackupType.UNKNOWN.isBackground()==true`, so the degraded ERROR never rendered (AC-2 failed at runtime). Remediated (ERROR carve-out in the guard + headline-string wiring). The Security Reviewer + QA independently flagged the ungated automatic schedule paths (defense-in-depth) — also remediated (all 5 paths now gated). Both findings → PASS after remediation.
- **Supply-chain discipline** maintained for the security-crypto bump (specific SHA-256, verify-metadata stays true).
- **Worktree artifact gap recurred** (U-054 plan.md not propagated) — backfilled.
- **Context expiry:** the execute-sprint skill context expired mid-sprint during the long agent runs (blocked the Security Reviewer); re-established and continued.
- **On-device DEFERRED** per user. The degraded-warning rendering + the no-cleartext config are good candidates for the eventual on-device confirmation pass.

## Gate Results
U-054 code FAIL→PASS (remediated) / sec PASS / QA PASS. U-055 code PASS / sec PASS / QA PASS. U-056 code PASS / sec PASS / QA PASS. Wave gate PASS.

## Recommendations
1. Bookend status mis-derivation (now 14×) — corrected to completed.
2. Deferred low: improve `SchemeCapturingStore` test fidelity (capture vs mirror the scheme constants).
3. On-device (when all-clear): confirm the degraded-warning banner renders and backup is blocked while degraded; confirm IMAP/OAuth still work with cleartext disabled.
