---
artifact_type: plan
story_id: "U-007"
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# Plan: U-007 — Rewrite AuthPreferences.migrate() to Eliminate Silent Trust-All

## Scope

U-007 is strictly scoped to `AuthPreferences.migrate()` rewrite (lines 276-288) and
its corresponding unit tests. No other production file is touched.

## Files to Modify

| File | Change |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` | Rewrite `migrate()` method body; add `TRANSPORT_SECURITY_NOTICE_PENDING` constant; add `markTransportSecurityNoticePending()` helper |
| `app/src/test/java/com/zegoggles/smssync/preferences/AuthPreferencesTest.java` | Remove `@Ignore` from `migrate_legacySslTls_doesNotEnableTrustAll`; update `migrate_explicitTrustAll_isPreserved` to reflect new clearing behavior; add 14 new tests covering all ACs |

## Implementation Approach

### migrate() Rewrite (DES-MODERNIZATION-002 Decision 6)

The rewrite implements the exact method body specified in DES-MODERNIZATION-002 Decision 6:

1. `useXOAuth()` early-return guard (unchanged — first statement)
2. Read `SERVER_PROTOCOL` into a local variable via `getServerProtocol()`
3. Compute `wasLegacyDowngradeProtocol` flag (`+ssl` or `+tls`)
4. Open `SharedPreferences.Editor`
5. If `SERVER_TRUST_ALL_CERTIFICATES` is currently `true`: write `false` and call `markTransportSecurityNoticePending(edit)` — clears stale downgrade, restores validated TLS
6. If `wasLegacyDowngradeProtocol`: write normalized protocol and call `markTransportSecurityNoticePending(edit)` — preserves normalization without trust-all
7. Call `edit.apply()` — NEVER `commit()` (ARCH-017)

### New constant: TRANSPORT_SECURITY_NOTICE_PENDING

Package-visible string constant `"transport_security_notice_pending"` added to `AuthPreferences`
as a `static final`. Package-visible (not private) so tests can reference it without string literals,
preventing typo-induced silent failures.

### Test Updates

- `migrate_legacySslTls_doesNotEnableTrustAll`: Remove `@Ignore` — now passes with the rewrite
- `migrate_explicitTrustAll_isPreserved`: Updated to assert `isFalse()` (stale cleared) and notice pending `isTrue()` — reflects correct new behavior
- `migrate_cleanState_doesNotEnableTrustAll`: Augmented to also assert notice NOT set
- 14 new test methods covering AC-1 through AC-9 comprehensively

## Verification Gates

1. `./gradlew :app:testDebugUnitTest` — all tests green including un-@Ignore'd AC-1 test
2. `./gradlew :app:jacocoTestCoverageVerification` — 70% gate holds
3. `./gradlew :app:assembleDebug` — compiles without error
4. No `.commit()` in rewritten `migrate()` method — verified by grep

## Out of Scope

- `BackupImapStore.java` factory selection (U-008/U-010)
- `AllTrustedSocketFactory.java` deletion (U-010)
- Enrollment UI / notice presentation (U-009)
- `isTrustAllCertificates()` method body (U-008)
- `ServiceBase.java` (U-008)
