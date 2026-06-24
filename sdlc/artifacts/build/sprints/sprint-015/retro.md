---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-015
started: 2026-06-24
completed: 2026-06-24
---
# Retro: sprint-015 — Toolchain Currency (Batch D)

## Business Summary
**Objective.** Pay down forward-compat debt (assessment BT-001/002/003/005/006): Kotlin 2.x + KSP + JVM 17, refresh the vendored k-9 deps, flip AGP-8 defaults + repo hygiene.
**Outcome.** All 3 stories done at **Tier A** (full scope, no deferrals); 697 tests green on the new toolchain; clean builds throughout. Each story sequenced (shared build files).
- **U-057 (BT-001):** Kotlin 1.9.25 → **2.0.21**, kapt → **KSP 2.0.21-1.0.28**, JVM/source target 8 → **17**. The U-022 "kapt over KSP for mixed Java+Kotlin" concern was re-evaluated and resolved — Hilt 2.51.1 supports KSP for mixed modules; migration succeeded with no source changes. Obsolete `-Xlint:-options` suppression removed. verification-metadata pinned for ~37 new Kotlin/KSP artifacts (no wildcard).
- **U-058 (BT-003):** commons-io 2.4 → **2.16.1**, apache-mime4j 0.7.2 → **0.8.11** (5 vendored files adapted for the 0.8 API), dropped **org.apache.http.legacy** (dead webdav package excluded; `StrictHostnameVerifier` → `HttpsURLConnection.getDefaultHostnameVerifier()` + shim — confirmed equivalent in security review). ACL boundary intact.
- **U-059 (BT-002/005/006):** `nonTransitiveRClass=true`, `nonFinalResIds=true` (MainActivity switch→if/else fix), dropped enableJetifier + dead jitpack repo. BT-006 (CI coverage-verify step) already satisfied by U-051 — confirmed, no change.

## Process Observations
- **Risk-managed sequencing paid off.** The 3 stories share build files (gradle.properties, build.gradle, verification-metadata) so they were executed strictly sequentially (U-057 → U-058 → U-059), each off the prior merge — zero conflicts.
- **Two flagged risks both came up clean.** (a) The documented "kapt-over-KSP" decision (U-022) — the agent re-evaluated and KSP worked cleanly on Kotlin 2.x. (b) U-058's TLS hostname-verifier replacement — the security review confirmed `OkHostnameVerifier` does real RFC 2818/6125 matching against the supplied peer certs (no weakening; REQ-MODERNIZATION-002 intact). Briefing the agents on these specific risks up front was effective.
- **All reviews PASS** (code 3/3, security 3/3, QA 15/15) — zero blockers; minor advisories only (prune a dead kapt verification-metadata entry; confirm the fall-through menu semantics).
- **Worktree artifact gap recurred and worsened** — U-058 lost BOTH plan.md + implementation-log.md and U-059 lost plan.md from the merge; backfilled from the agent reports. This framework propagation gap has now hit 4 sprints; worth a fix.
- **On-device DEFERRED** per user.

## Gate Results
U-057/058/059 each: code PASS, security PASS, QA PASS. Sequential waves all PASS.

## Recommendations
1. Bookend status mis-derivation (now 15×) — corrected to completed.
2. Prune the dead `kotlin-annotation-processing-gradle:1.9.25` entry from verification-metadata (kapt-era leftover).
3. **Framework:** fix the recurring worktree workspace-artifact (plan.md/impl-log) propagation gap.
4. On-device (when all-clear): a smoke + backup/restore round-trip on the new toolchain to confirm nothing regressed.
