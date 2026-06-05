---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-011
started: 2026-06-05
completed: 2026-06-05
---

# Retro: sprint-011 — SNIHostName IP-literal guard (BUG-015)

## Business Summary

**Objective.** Fix BUG-015 (low) — the U-045 SNI change constructed `new SNIHostName(host)` unguarded, which throws `IllegalArgumentException` for an IP-literal or empty IMAP host (API 24+), breaking the connect for users with an IP-addressed server. Flagged as code-review W-1 during U-045.

**Outcome.** Fixed. 672 tests green, per-package LINE ≥70%, assembleDebug green. Code/security/QA reviews all PASS. Executed in a worktree.

- **U-047 / BUG-015:** added `isIpOrBlankHostname()` (null/blank trim check, IPv6 via `indexOf(':')`, IPv4 via 4-octet regex) plus a `try/catch IllegalArgumentException` safety net in `DefaultTrustedSocketFactory.setSniViaSSLParameters` — SNI is now skipped (RFC 6066) for non-DNS hosts and set only for valid DNS names. The Gmail/DNS path is byte-for-byte unchanged; logging stays DEBUG-only (BUG-012 preserved). +6 unit tests (DNS, IPv4, IPv6 compressed + full, empty, blank).

**Decisions Made.**
- Explicit IP detection was necessary in addition to the try/catch: the JVM's `SNIHostName` accepts dotted-decimal IPv4 without throwing (unlike Android's runtime), so a pure try/catch would have left the IPv4 case unguarded in unit tests / on JVM. The two-layer approach (explicit check + catch safety net) is correct across both environments.

## Process Observations

**Process Compliance.** BUG-015 (from the U-045 code-review W-1) → create-stories (U-047) → create-sprint (sprint-011) → execute-sprint (worktree) → wave gate → reviews → close. Worktree-base local-ref workaround used; branch verified `--is-ancestor` before merge; refs restored at close.

**Validation scope.** Unit-test-gated by design — no IP-literal IMAP server is available for an on-device test, and the DNS (Gmail) path was already on-device-validated in sprint-010 (U-045). The story documented this explicitly; QA confirmed it as expected rather than a gap.

**Gate Results.** U-047: code PASS (2 low warnings — IPv4 regex over-matches out-of-range octets like `999.x`, safe; no direct null-arg test, behavior correct), security PASS (TLS trust anchors unchanged; SNI is a routing hint not a trust control; REQ-MODERNIZATION-002 preserved), QA PASS (4/4). Wave-1 gate PASS.

**Issues Encountered.**
1. **Closing-bookend status mis-derivation (now 11×).** `completed-with-blockers` with 1 done / 0 blocked → corrected to `completed`.

**Recommendations.**
- Optional nit (code-review W-1): tighten the IPv4 octet regex or add a javadoc note that out-of-range octets are intentionally treated as "skip SNI". No functional impact (try/catch + hostname verification cover it).
- Two standing framework fixes persist (worktree base; bookend terminal-status heuristic).
- **Backlog is fully clear: BUG-001 through BUG-015 all resolved.**
