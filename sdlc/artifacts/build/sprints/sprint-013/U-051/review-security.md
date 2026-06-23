---
artifact_type: review-security
story_id: "U-051"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-06-23"
blockers: 0
warnings: 0
---

# Security Review: U-051 — Expand JaCoCo Coverage Gate to All Packages

## Review Summary

U-051 is a pure build-configuration story: it adds `rule {}` blocks to `jacocoTestCoverageVerification` in `app/build.gradle` and adds an explicit CI step in `.github/workflows/ci.yml`. No production Java or Kotlin source code was modified, no new dependencies were introduced, and no credential, TLS, permission, or data-handling path was altered. The change is security-neutral.

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

None.

### Informational

**Coverage floors below 50% are correctly documented, not a security bypass.** Several newly-gated packages (`activity.auth`, `activity.events`, `di`, `tasks`) received floors of 0% because they genuinely have 0% measured coverage today. The floor assignment is honest — the comment in each rule records the measured value and a `// TODO: raise to 70%` annotation. Setting a floor at 0% for a package that has 0% coverage is not a weakening of any prior gate; these packages were previously ungated entirely.

**`activity.auth` at 0% is a debt observation, not a security finding.** The `activity.auth` package contains 139 lines of OAuth/XOAuth2 flow code with no unit tests. This is the pre-existing test debt acknowledged in U-051 itself and tracked by the `// TODO` annotation. The security implication is that the OAuth activity is not regression-tested, but this is identical to the state before this sprint and is not introduced by U-051. It is a known debt item addressed in later stories.

**`preferences` floor at 65% (measured 69.4%).** The 65% floor gives a small headroom below the 70% standard. This is a build-configuration trade-off to prevent minor refactors from accidentally breaking the gate. No security-sensitive preference code (credential storage, trust-all flag) is excluded; the `auth` package containing `AuthPreferences` remains at the 70% standard gate (Rule 3) unchanged.

**CI gate addition strengthens posture.** Adding `jacocoTestCoverageVerification` as an explicit step in `.github/workflows/ci.yml` means all new packages now have a build-breaking floor that previously did not exist. This is a net positive for security governance alignment with REQ-MODERNIZATION-003 AC-1 and AC-3.

### Security Checklist

- [x] No hardcoded credentials or secrets — build config only; no credentials possible
- [x] Input validation — not applicable (build configuration)
- [x] Output encoding prevents injection attacks — not applicable
- [x] Authentication tokens handled securely — no auth code changed
- [x] Authorization checks on all protected resources — not applicable
- [x] Sensitive data encrypted at rest and in transit — no data-path code changed
- [x] Error messages don't leak internal details — no runtime code changed
- [x] Dependencies have no known critical vulnerabilities — no new dependencies added

## Files Reviewed

- `app/build.gradle` (violationRules block, jacocoFileFilter — unchanged; only rule additions)
- `.github/workflows/ci.yml` (new explicit coverage gate step)
- Implementation log: `sdlc/artifacts/build/sprints/sprint-013/U-051/implementation-log.md`

## REQ Traceability

This story implements REQ-MODERNIZATION-003 AC-1 (CI pipeline) and AC-3 (coverage gate) by widening the gate to all 22 packages. No approved security REQ artifact is violated. The three original gated packages (`service*`, `mail*`, `auth*`) remain at minimum = 0.70, unchanged.
