---
artifact_type: security-review
story_id: "U-004"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-02"
blockers: 0
warnings: 1
---

# Security Review: U-004

## Summary

No security regressions introduced. One existing security concern is preserved as
documented (the R8 keep rule for `AuthMode`/`DataType` enum paths) and one supply-chain
gap is flagged as a known open item.

## Findings

### WARNING 1: Supply-chain integrity not yet enforced (AC-8 gap)

**Severity:** Warning
**Finding:** The CI invocation does not include `--verify` because
`gradle/verification-metadata.xml` does not exist. The dependency graph includes
`maven.scijava.org` (non-Google mirror serving `firebase-jobdispatcher:0.8.6`) and a
raw JitPack SHA pin for k-9 (`eaf689025e`). Without checksum verification, supply-chain
tampering against either host is undetectable.
**Impact:** The integrity gap was pre-existing (not introduced by this story). However,
REQ-MODERNIZATION-003 AC-8 requires it to be closed as part of Gate G2.
**Resolution:** Generate `gradle/verification-metadata.xml` and add `--verify` to the CI
invocation before Gate G2 is declared. This is tracked in implementation-log Known Gaps.

## Positive Security Observations

- **R8 keep rules are minimal and scoped**: No blanket `-keep class * { *; }` rules.
  Each rule targets a specific reflection surface with the narrowest possible scope:
  - Otto: only `@Subscribe`/`@Produce`-annotated methods, not entire classes
  - `SmsJobService`: single class by fully-qualified name
  - Enums: only `values()` and `valueOf()` methods, not all members

- **`minifyEnabled true` improves security posture**: Enabling R8 reduces the attack
  surface of the release APK by stripping unused code paths. This is a security
  improvement over the previous `false` state.

- **No credentials or secrets in workflow**: The `ci.yml` workflow does not hardcode
  any secrets, API keys, or credentials. The signing config in `build.gradle` uses
  `keystoreProperties.isEmpty() ? null : signingConfigs.release`, which gracefully
  handles missing keystore in CI.

- **No `continue-on-error: true`**: The CI workflow cannot silently suppress failures.
  Every step failure causes the job to fail, preserving the integrity of the gate.

- **R8 keep rules marked INTERIM**: Each keep rule carries a comment noting which
  migration unit removes it (MU-005 for SmsJobService, MU-006 for Otto). This
  reduces the risk of keep rules accumulating indefinitely and widening the attack
  surface beyond the intended interim period.

- **Runner is pinned**: `ubuntu-22.04` (not `ubuntu-latest`) prevents silent runner
  environment drift that could affect build reproducibility and security scanning.
