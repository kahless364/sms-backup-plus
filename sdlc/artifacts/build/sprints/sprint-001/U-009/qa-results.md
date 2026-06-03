---
artifact_type: qa-results
story_id: U-009
verdict: PASS
agent: Developer
timestamp: "2026-06-03T00:00:00Z"
---

# QA Results: U-009

## Verification Summary

| Gate | Command | Result |
|------|---------|--------|
| Unit tests | `./gradlew :app:testDebugUnitTest` | PASS — 539 tests, 0 failures, 1 skipped |
| Coverage gate | `./gradlew :app:jacocoTestCoverageVerification` | PASS — >=70% on all gated packages |
| Debug build | `./gradlew :app:assembleDebug` | PASS — APK built successfully |

## Test Distribution

| Test Class | Tests Added | Tests |
|------------|-------------|-------|
| `AdvancedSettingsServerTest` (new) | 20 | AC-5, AC-6, AC-7, AC-8, AC-9, helper methods |
| `AuthPreferencesTest` (extended) | 9 | AC-7 migrate-path, AC-8/AC-9 end-to-end |

## AC Verification

| AC | Status | Evidence |
|----|--------|---------|
| AC-1: checkbox removed from preferences.xml | PASS | `grep -rn "server_trust_all_certificates" app/src/main/res/xml/` → 0 matches |
| AC-2: trust-all strings removed from all 13 locales | PASS | `grep -rn "ui_protocol_trust_all_certificates" app/src/main/res/` → 0 matches |
| AC-3: new `pin_server_certificate` preference present | PASS | `preferences.xml` contains `<Preference android:key="pin_server_certificate">` |
| AC-4: enrollment dialog shows 4 mandatory fields | PASS | `PinCertificateEnrollmentFlow.showEnrollmentDialog()` builds message with subject, issuer, SHA-256, expiry |
| AC-5: cancel leaves store unwritten | PASS | `ac5_dialogShownButNotConfirmed_storeUnwritten`, `ac5_cancelListener_storeRemainsEmpty` |
| AC-6: confirmation stores and indicator updates | PASS | `ac6_putCertificate_storageAndRetrieval`, `ac6_removePin_revertsToSystemValidated`, `ac6_pinScopedToHost_doesNotAffectOtherHosts` |
| AC-7: no automated write to PinnedCertStore | PASS | `ac7_freshStore_allHostsReturnSystemValidated`, `ac7_noPrefsChangeWritesPinnedCertStore`, `u009_ac7_*` (3 tests in AuthPreferencesTest) |
| AC-8: notice shown once for affected cohort | PASS | `ac8_pendingFlagTrue_noticeConsumedAndFlagCleared`, `ac8_alreadyConsumed_secondCallReturnsFalse`, `ac8_consumption_onlyFlipsFlagFromTrueToFalse`, `u009_ac8_legacySslMigrateSetsFlagThenNoticeConsumed` |
| AC-9: notice not shown to unaffected users | PASS | `ac9_flagAbsent_noticeNotShown`, `ac9_flagFalse_noticeNotShown`, `u009_ac9_normalizedProtocol_*`, `u009_ac9_xoauthUser_*` |
| AC-10: new pin-cert strings present in default locale | PASS | All 4 required strings present with exact values specified in AC-10 |
| IC-1: listener set in Server fragment body | PASS | Code review confirms `onResume()` in `AdvancedSettings.Server` |
| IC-2: notice flag consumed by exactly one site | PASS | Only `TransportSecurityNoticeHelper.checkAndClearNoticePending()` reads/writes false |
| IC-3: `remove()` call site is Remove action only | PASS | Single `pinnedCertStore.remove()` call in Remove action listener |
