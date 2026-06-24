---
artifact_type: review-code
story_id: "U-055"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-06-24"
blockers: 0
warnings: 1
---

# Code Review: U-055

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PASS — version bump is correct; verification-metadata updated; no code regressions |
| Test coverage | PASS — 5 tests; constant-pinning and builder-capture strategies together are well-reasoned |
| Code quality | PASS — minimal footprint, clear comment explaining the version choice |

## Verdict: PASS

The upgrade from `security-crypto:1.1.0-alpha06` to `1.1.0` stable is implemented cleanly. No production code changes were required. The scheme-pinning tests are a meaningful guard. One warning: the builder-capture test (`SchemeCapturingStore.getEncrypted()`) duplicates the scheme constants from production code rather than reading them from the production method, which means a future refactor that changes the production constants would not be detected unless the test constants are also updated.

---

## Findings

### Blockers

None.

---

### Warnings

**WARNING-1: `SchemeCapturingStore.getEncrypted()` is a manual mirror of production constants, not a true interception**

File: `EncryptedPrefsSchemeRegressionTest.java:209-223`

The Javadoc comment at line 207 says the subclass "intercepts the scheme constants the production code would have passed." In practice, `SchemeCapturingStore.getEncrypted()` hardcodes the constants itself (`PrefKeyEncryptionScheme.AES256_SIV` and `PrefValueEncryptionScheme.AES256_GCM` at lines 213-216) and records them. It does not call `super.getEncrypted()` or read the constants from the parent class's source.

This means the test currently does what the constant-pinning tests (tests 1–3) already do: verify the enum constants exist with the expected names. If a developer refactors `EncryptedPrefsSecretStore.getEncrypted()` to swap in `AES256_GCM` for the key scheme but forgets to update `SchemeCapturingStore.getEncrypted()`, the test would still pass with the old (now-incorrect) values.

A true production-code capture would require either:
- A `@VisibleForTesting` accessor that exposes the scheme arguments the real `getEncrypted()` would use, or
- A spy/wrapper that intercepts the actual call to `EncryptedSharedPreferences.create()` (not feasible in Robolectric without Keystore)

The current approach still catches library-level regressions (the constant-pinning tests do this) and confirms the store wires correctly end-to-end (the round-trip test). The limitation is that it may not catch internal production-code refactors that change which constants are passed. Severity: Warning, not Blocker, because the constant-pinning tests provide the primary library-upgrade guard.

---

### Observations

**OBS-1: `alpha06` entries retained in `verification-metadata.xml`**

File: `gradle/verification-metadata.xml:694-700`

The old `security-crypto:1.1.0-alpha06` SHA-256 entries remain after regeneration. The implementation log notes this is correct Gradle behavior (Gradle retains all previously-resolved versions after `--write-verification-metadata`). No action needed, but if an explicit pruning policy is ever adopted, these are candidates for removal.

**OBS-2: `jspecify:0.3.0` and `jspecify:1.0.0` both present in verification-metadata**

File: `gradle/verification-metadata.xml:3375-3387`

Two versions of `jspecify` are pinned (`0.3.0` and `1.0.0`). The `1.0.0` entry is the new transitive introduced by `security-crypto:1.1.0`. The `0.3.0` entry may be a residual from a prior resolution. Not a defect; Gradle retains all resolved versions. Informational only.

**OBS-3: Version choice justification is thorough and correct**

The implementation log's version justification at lines 28-44 correctly explains why `1.0.0` stable could not be used (deprecated `MasterKeys.getOrCreate()` lacking `setRequestStrongBoxBacked()`), why `1.1.0` stable is the right target, and that Tink was updated to `1.8.0`. This is well-documented reasoning.

---

## Patterns Verified

- [x] Follows existing code patterns — `implementation` line in `build.gradle:143` follows the same format as neighboring declarations; ADR comment updated in place
- [x] Error handling is appropriate — no new error handling surface; existing `RuntimeException` path in `getEncrypted()` is unchanged
- [x] Tests cover new functionality — 5 tests covering constant names and builder path; round-trip test confirms end-to-end wiring
- [x] No hardcoded values that should be configurable — version declared once in `build.gradle`; no duplication
- [x] No unnecessary complexity — comment-only change to `EncryptedPrefsSecretStore.java`; test file is self-contained

## Integration Verified

- [x] New code is reachable from production entry points — `EncryptedPrefsSchemeRegressionTest` is in `app/src/test/` under the correct package; picked up automatically by `testDebugUnitTest`
- [x] Registries/dispatch maps updated for new implementations — N/A (dependency bump only)
- [x] Function signatures match at all call sites — `MasterKey.Builder` and `EncryptedSharedPreferences.create()` API surface confirmed identical between `alpha06` and `1.1.0` stable
- [x] No dead code introduced — no unreachable code added
- [x] Integration path documented in implementation-log.md — yes, implementation-log covers version choice, metadata update method, and test strategy

## Regression Check

Not applicable — no files deleted or replaced. `EncryptedPrefsSecretStore.java` received only a Javadoc comment update.

## Contract Verification

- [x] All consumers of modified interfaces identified — `EncryptedPrefsSecretStore` is the sole production consumer of `security-crypto`; its API surface is unchanged
- [x] Field names, event names, data shapes agree between producer and consumer — no interface changes; data-at-rest compatibility maintained (Tink AES-256-GCM/SIV scheme is identical between `alpha06` and `1.1.0`)
- [x] No contract mismatches between backend and frontend — N/A
