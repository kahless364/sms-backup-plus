---
artifact_type: qa-results
story_id: "U-007"
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# QA Results: U-007

## Test Execution Summary

| Gate | Command | Result |
|------|---------|--------|
| Unit tests | `./gradlew clean :app:testDebugUnitTest` | BUILD SUCCESSFUL |
| Coverage gate | `./gradlew :app:jacocoTestCoverageVerification` | BUILD SUCCESSFUL |
| Build | `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |

## Test Count

| Metric | Count |
|--------|-------|
| Total @Test annotations in test suite | 321 |
| @Ignore annotations (pre-existing AppTest) | 1 |
| Active (non-ignored) tests | 320 |
| Tests added by U-007 | 14 new + 1 de-@Ignore'd = 15 affected |
| AuthPreferencesTest total @Test methods | 17 |
| AuthPreferencesTest previously active | 2 (of 3; 1 was @Ignore'd) |
| AuthPreferencesTest now active | 17 |
| Failures | 0 |

## Acceptance Criterion Test Matrix

| AC | Test(s) | Pass? |
|----|---------|-------|
| AC-1: no trust_all=true in any branch | `migrate_legacySslTls_doesNotEnableTrustAll` (un-@Ignore'd), `migrate_legacyTls_doesNotEnableTrustAll`, `migrate_nonLegacyProtocols_neverEnableTrustAll` | PASS |
| AC-2: stale true cleared to false | `migrate_staleTrustAll_isClearedToFalse`, `migrate_explicitTrustAll_isPreserved` (updated) | PASS |
| AC-3: notice set for affected cohort | `migrate_legacySsl_setsNoticePending`, `migrate_legacyTls_setsNoticePending`, `migrate_staleTrustAllOnly_setsNoticePending`, `migrate_legacySslAndStaleTrustAll_setsNoticePending` | PASS |
| AC-4: notice NOT set for unaffected | `migrate_unaffectedUsers_doesNotSetNoticePending`, `migrate_cleanState_doesNotEnableTrustAll` | PASS |
| AC-5: protocol normalization preserved | `migrate_legacySsl_normalizesProtocolWithoutTrustAll`, `migrate_legacyTls_normalizesProtocolWithoutTrustAll` | PASS |
| AC-6: OAuth2 early return | `migrate_xoauth_returnsEarlyWithNoWrites` | PASS |
| AC-7: apply() not commit() | Code inspection + grep — zero `.commit()` in migrate() | PASS |
| AC-8: U-006 characterization tests green | `migrate_cleanState_doesNotEnableTrustAll`, `migrate_explicitTrustAll_isPreserved` (updated), `migrate_legacySslTls_doesNotEnableTrustAll` (un-@Ignore'd) | PASS |
| AC-9: unaffected users unchanged | `migrate_alreadyNormalizedProtocol_noChanges`, `migrate_unaffectedUsers_doesNotSetNoticePending`, `migrate_cleanState_doesNotEnableTrustAll` | PASS |

## Regression Check

- All 303 pre-U-007 passing tests still pass (no regressions)
- `BackupImapStoreTest` suite: unchanged, all passing
- No production files modified except `AuthPreferences.java` (migrate() method only)

## Coverage Gate

`jacocoTestCoverageVerification` confirms the 70% instruction-coverage gate on
`service/`, `mail/`, and `auth/` packages is still satisfied after the test additions.

## Static Checks

- `grep ".commit()" AuthPreferences.java` — 0 matches within `migrate()` method body
- `grep "TRUST_ALL_CERTIFICATES, true" AuthPreferences.java` — 0 matches in migrate() body
- `grep "TRANSPORT_SECURITY_NOTICE_PENDING" AuthPreferencesTest.java` — constant reference, not string literal
