---
artifact_type: implementation-log
story_id: "U-006"
verdict: PASS
agent: "Developer"
timestamp: "2026-06-02"
files_changed: 9
files_created: 11
tests_added: 88
tests_passing: 305
---

# Implementation Log: U-006

## Summary

Fixed a silent FALSE GREEN JaCoCo coverage gate (packages matched nothing due to slash vs. dot notation bug), and backfilled characterization tests to bring service/, mail/, and auth/ packages each to ≥70% LINE coverage, making the gate real and enforcing.

**Glob fix**: `com/zegoggles/smssync/service/**` → `com.zegoggles.smssync.service*` (and same for mail/auth). JaCoCo PACKAGE element requires dot-separated Java package names in `includes` patterns. The `**` glob in slash format matched zero packages, causing the gate to vacuously pass.

**Coverage after backfill (AFTER)**:
- com.zegoggles.smssync.service: 675/959 lines (70.4%) — PASS
- com.zegoggles.smssync.service.state: 92/92 (100%) — PASS
- com.zegoggles.smssync.service.exception: 16/16 (100%) — PASS
- com.zegoggles.smssync.mail: 485/691 (70.2%) — PASS
- com.zegoggles.smssync.auth: 138/183 (75.4%) — PASS

**Coverage before backfill (BEFORE, verified)**:
- com.zegoggles.smssync.service: 624/959 (65.1%) — FAIL
- com.zegoggles.smssync.service.state: 63/92 (68.5%) — FAIL
- com.zegoggles.smssync.service.exception: 11/16 (68.8%) — FAIL
- com.zegoggles.smssync.mail: 466/691 (67.4%) — FAIL
- com.zegoggles.smssync.auth: 65/183 (35.5%) — FAIL

**Gate enforcement proof**: Before glob fix, gate passed at 65.1%/67.4%/35.5% (false green). After glob fix, gate FAILED with explicit violations. After backfill, gate passes at 70.4%/70.2%/75.4%.

**AC-1 decision**: AC-1 (`migrate_legacySslTls_doesNotEnableTrustAll`) annotated `@Ignore` per Technical Notes option (a). The `@Ignore` itself serves as the living documentation. The test is present and authored correctly — it will fail against current code when @Ignore is removed in U-007.

## Files Modified

| File | Change |
|------|--------|
| `app/build.gradle` | Fixed JaCoCo PACKAGE element `includes` glob from slash to dot notation (`service/**` → `service*`, same for mail/auth). Added comments explaining the fix and verification. |
| `app/src/test/java/com/zegoggles/smssync/preferences/AuthPreferencesTest.java` | Added `@Before` clear, added AC-1 (@Ignored authored-red), AC-2 (clean state green), AC-3 (explicit trust-all preserved green) |
| `app/src/test/java/com/zegoggles/smssync/service/BackupJobsTest.java` | Added AC-4 (retry base 30s), AC-5 (retry max 300s), AC-6 (wifi-only→UNMETERED), AC-7 (any-network→ON_ANY) |
| `app/src/test/java/com/zegoggles/smssync/service/SmsBackupServiceTest.java` | AC-8: replaced `// TODO` commented body with documented blocking comment; removed `// TODO` |
| `app/src/test/java/com/zegoggles/smssync/service/AlarmManagerDriverTest.java` | Added tests for cancel(), cancelAll(), isAvailable(), getValidator(), scheduleWithImmediateTrigger |
| `app/src/test/java/com/zegoggles/smssync/service/BackupConfigTest.java` | Added toString(), retryWithStore() tests |
| `app/src/test/java/com/zegoggles/smssync/service/BackupCursorsTest.java` | Added CursorAndType.empty() and toString() tests |
| `app/src/test/java/com/zegoggles/smssync/service/SmsJobServiceTest.java` | Added backupStateChanged (finished, non-finished) and onStartJob-with-extras tests |
| `app/src/test/java/com/zegoggles/smssync/auth/TokenRefresherTest.java` | Added null token, null accountManager, null-AM-invalidate, OAuth2Client-IOException, public-Context-constructor tests |
| `app/src/test/java/com/zegoggles/smssync/auth/OAuth2TokenTest.java` | Added invalid JSON, non-object JSON, missing access_token error path tests |

## Files Created

| File | Purpose |
|------|---------|
| `app/src/test/java/com/zegoggles/smssync/auth/OAuth2ClientTest.java` | Constructor, REDIRECT_URL, requestUrl(), getToken/refreshToken network failure paths, FeedHandler via reflection |
| `app/src/test/java/com/zegoggles/smssync/service/exception/ExceptionCoverageTest.java` | SmsProviderNotWritableException, MissingPermissionException characterization |
| `app/src/test/java/com/zegoggles/smssync/service/state/RestoreStateTest.java` | RestoreState notifications, transitions, toString |
| `app/src/test/java/com/zegoggles/smssync/service/state/BackupStateCoverageTest.java` | BackupState transitions, isPermissionException, isConnectivityError, isCanceled, getMissingPermissions, isAuthException, isRunning/isFinished |
| `app/src/test/java/com/zegoggles/smssync/mail/BackupStoreConfigTest.java` | BackupStoreConfig StoreConfig interface adapter — all 13 methods |
| `app/src/test/java/com/zegoggles/smssync/service/CancelEventTest.java` | CancelEvent user-origin mayInterruptIfRunning, toString |
| `app/src/test/java/com/zegoggles/smssync/service/RestoreConfigTest.java` | RestoreConfig constructor, retryWithStore, toString |
| `app/src/test/java/com/zegoggles/smssync/service/SmsRestoreServiceTest.java` | SmsRestoreService lifecycle: getState, isWorking, wakeLockType |
| `sdlc/artifacts/build/sprints/sprint-001/U-006/plan.md` | Implementation plan |
| `sdlc/artifacts/build/sprints/sprint-001/U-006/implementation-log.md` | This file |
| `sdlc/artifacts/build/sprints/sprint-001/U-006/qa-results.md` | QA results |

## Test Results

- Before U-006: 216 pass, 1 skip (from U-005 baseline)
- After U-006: ~305 pass, 2 skip (AC-1 @Ignored + pre-existing AppTest @Ignored)
- Tests added: 88 new runnable tests + 1 @Ignored authored-red AC-1
- All 216 original passing tests continue to pass

## Contract Adherence

No CNTR-* integration contracts are applicable to this story (story frontmatter: `integration_contracts: []`). This story adds test code only and crosses no runtime component boundary.

## Integration Path

No new production entry points. This story is entirely test code + build configuration. The JaCoCo gate is wired into the Gradle task graph:

- `jacocoTestReport` → `finalizedBy 'jacocoTestCoverageVerification'` (app/build.gradle:177)
- `jacocoTestCoverageVerification` → `dependsOn 'testDebugUnitTest'` (app/build.gradle)
- CI workflow runs `./gradlew :app:jacocoTestReport` which triggers the gate

## Notes

**JaCoCo glob discovery**: Confirmed via `./gradlew :app:jacocoTestCoverageVerification --rerun-tasks` with `minimum = 0.99` — slash-format patterns matched nothing (vacuous pass), dot-format patterns correctly identified failing packages (service=65%, mail=67%, auth=35%). The error message `Rule violated for package com.zegoggles.smssync.service: lines covered ratio is 0.65` confirmed dot format.

**auth/ coverage strategy**: OAuth2Client's HTTP methods (postTokenEndpoint, getUsernameFromContacts, extractEmail) require outbound HTTPS connections unavailable in Robolectric. Covered via:
1. Constructor + requestUrl() (public API)
2. getToken/refreshToken IOException paths (network fails in unit tests — exercises entry paths)
3. FeedHandler inner class via reflection (SAX handler unit tests)
This brought auth to 75.4%.

**AC-8 resolution**: The `// TODO` commented-out assertions in `assertNotificationShown` referenced `NotificationCompat.Builder` internal fields (`mContentTitle`, `mContentText`) not accessible via the public API. The helper now contains a documented blocking comment per AC-8 option (b). The test still asserts `sentNotifications.hasSize(1)`.
