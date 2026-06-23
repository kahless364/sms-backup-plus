---
artifact_type: implementation-log
story_id: "U-052"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-23"
files_changed: 4
files_created: 3
tests_added: 26
tests_passing: 669
---

# Implementation Log: U-052 — GreenMail IMAP integration tests; remove worker/transport coverage exclusions (TE-002)

## Summary

U-052 adds GreenMail-backed IMAP integration tests for `K9MailTransport$BackupImapStoreDelegate`,
`BackupWorker`, and `RestoreWorker`, then removes those three classes from the `jacocoFileFilter`
exclusion list. The full gate (`assembleDebug + testDebugUnitTest + jacocoTestCoverageVerification`)
passes: `service*` LINE coverage is 75.6% (vs the 70% minimum), `mail*` is 71.5–84%, and no rules
are violated.

**Pre-requisites confirmed:** U-051 (gate widened to all packages) and U-053 (BackupWorker watermark
seam) were already merged. `git merge-base --is-ancestor 36062b3e HEAD` returned 0 (BASE_OK).

## Files Modified

### `app/build.gradle`
- **Removed** 6 exclusion lines from `jacocoFileFilter` (BackupImapStoreDelegate*, BackupWorker*,
  RestoreWorker*) — replaced with explanatory comment (U-052 rationale).
- **Added** `testImplementation 'com.icegreen:greenmail:2.1.3'` and the 5 GreenMail transitive
  dependencies (GreenMail uses Jakarta EE 9 / Angus Mail).
- **Updated** `gradle/verification-metadata.xml` with SHA-256 checksums for GreenMail 2.1.3 and
  its transitive JARs (supply-chain verification not disabled).
- **Updated** Rule 15 comment (worker* package) to clarify BackupWorker/RestoreWorker live in
  `service*` (not `worker*`); minimum stays 0.00 since BackupTriggerWorker has no tests.
- **Updated** Rule 1 comment with measured U-052 values (service* = 75.6%).

### `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerIntegrationTest.kt`
- **Added test** `doWork_mailExceptionFromNoEnabledTypes_returnsRetry`: disables all backup types
  → `getEnabledBackupTypes` throws `MailException` → doWork returns `Retry`.
- **Added test** `doWork_mailExceptionFromTransportFactory_returnsRetry`: factory throws `MailException`
  from `create()` → doWork outer catch → `Retry`.
- **Replaced** `doWork_noEnabledTypes_returnsRetry` with `doWork_noData_returnsSuccess`: SMS+MMS
  enabled by default; empty Robolectric providers → `fetchAndBackupItems` "nothing to backup" path
  → `Success`.
- **Added test** `doWork_skipTag_executesSkipWithoutConnectingToImap`: worker tagged "SKIP" →
  `inferBackupType()` returns `BackupType.SKIP` → `executeSkip()` runs (15 lines, was 0%).
- **Added** `AuthPreferences(context).setImapUser("backup@test.local")` in `setUp()` to prevent
  `MessageConverter` NPE from null email (RootCause: `Address(null)` → `Rfc822Tokenizer.tokenize(null)`).
- Replaced `doWork_mailExceptionOnCheckSettings_returnsRetry` with two correct tests reflecting
  actual production code flow (checkSettings is never reached without SMS data in Robolectric).

### `app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerIntegrationTest.kt`
- **Added** `FailingRestoreTransport` as `open class` (was `class`) to allow anonymous override.
- **Added** `ImapRestoringTransport`: configurable transport that accepts lambdas for `getMessages`
  and `importMessageBody`, used to drive the IMAP restore path without a live server.
- **Added test** `doWork_withImapMessagesFailedImport_coversRestoreLoop`: transport returns 1
  `MailMessageHandle` from SMS folder, `importMessageBody` returns failed result → exercises
  `runImapRestoreLoop` (33 lines, was 0%), `importMessage` failure path (15 lines, was 0%).
- **Added test** `doWork_withImapCallLogMessage_coversCallLogImport`: transport returns 1 calllog
  message, `importMessageBody` returns successful CALLLOG `MessageImportResult` → exercises
  `importCallLogValues` (9 lines, was 0%), `callLogExists` (14 lines, was 0%).
- **Added test** `doWork_restoreDisabled_returnsSuccessImmediately`: uses non-throwing transport
  (via `buildWorkerWithTransport`) AND explicitly disables both SMS+calllog restore prefs →
  exercises early-exit path in `doWork` (lines 171-173).
- **Added** `authPreferences.setImapUser("test@test.local")` in `setUp()` for same reason as above.
- **Fixed** 5 previously failing tests by understanding defaults (`SMS_RESTORE_ENABLED=true`,
  `CALLLOG_RESTORE_ENABLED=true`) and the ordering (`mailTransportFactory.create()` before restore
  flag checks).
- **Added** `android.provider.CallLog` import for call log ContentValues.

## Files Created

### `app/src/test/java/com/zegoggles/smssync/mail/transport/BackupImapStoreDelegateIntegrationTest.java`
9 GreenMail-backed tests for `K9MailTransport$BackupImapStoreDelegate`:
- `appendAndFetchMessages_roundTrip_smsType`: append + UID fetch with SMS folder
- `appendAndFetchMessages_roundTrip_calllogType`: append + UID fetch with CALLLOG folder
- `appendMessages_emptyResult_returnsZero`: append with no messages
- `fetchMessages_emptyFolder_returnsEmptyList`: fetch from empty folder
- `importMessageBody_failure_returnsFailedResult`: import with failed conversion
- `searchByHeader_notSupportedByGreenMail_throwsMailException`: GreenMail HEADER search limitation
- `appendThenFetchBody_bodyContentPreserved`: append + body fetch verifies content preservation
- `openFolderCreatesFolder_idempotent`: repeated openFolder is idempotent
- `closeFolders_noThrow`: closeFolders does not throw

Key: uses `MimeMessageHelper.setBody(message, new TextBody(body))` (not `message.setBody()`);
GreenMail does NOT support UID SEARCH HEADER extension — that path tests the exception-translation path.

### `sdlc/artifacts/build/sprints/sprint-013/U-052/plan.md`
(Created by Solution Architect in the prior sprint planning phase; `verdict: PASS`.)

### `sdlc/artifacts/build/sprints/sprint-013/U-052/implementation-log.md`
This file.

## Test Results

**Authoritative @Test count** (`grep -rh "@Test" app/src/test/ --include="*.java" --include="*.kt" | grep -c "@Test"`):
- **Before U-052**: 643 (at HEAD before this work)
- **After U-052**: **669** (+26 new tests)

**Full gate result**:
```
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification
BUILD SUCCESSFUL
```

**Coverage (post-exclusion-removal)**:
| Class / Package | Before | After |
|---|---|---|
| `K9MailTransport$BackupImapStoreDelegate` | excluded (0% counted) | **87.0%** LINE |
| `BackupWorker` | excluded (0% counted) | **55.8%** LINE |
| `RestoreWorker` | excluded (0% counted) | **80.2%** LINE |
| `service*` package (Rule 1, min=70%) | 83.6% (workers excluded) | **75.6%** ✓ |
| `mail.transport*` package (Rule 2, min=70%) | 88.8% | **84.0%** ✓ |
| `mail*` package (Rule 2, min=70%) | 71.5% | **71.5%** ✓ |

## Regression Results

**Capabilities Inventory** (replacement / exclusion-removal scope):

The three previously excluded classes have the following method-level coverage retained:

**K9MailTransport$BackupImapStoreDelegate** (was fully excluded, now at 87%):
- RETAINED: `openFolder` (SMS, CALLLOG, MMS folders) — GreenMail tests cover folder creation
- RETAINED: `appendMessages` — covered via GreenMail round-trip
- RETAINED: `getMessages` (fetch with starred/since filters) — covered via GreenMail
- RETAINED: `importMessageBody` — covered via failed-import path test
- RETAINED: `closeFolders` — covered
- NOTE: BackupFolder.search() HEADER path → exception-translation covered; GreenMail limitation documented

**BackupWorker** (was excluded, now at 55.8%):
- RETAINED: `doWork` entrypoint — 82% covered (setForeground, backoff cap, factory, backupType)
- RETAINED: `inferBackupType` — 100% covered
- RETAINED: `getEnabledBackupTypes` — 100% covered
- RETAINED: `executeBackup` — 66% covered
- RETAINED: `executeSkip` — now covered (was 0%; U-052 doWork_skipTag test)
- RETAINED: `appendBatchAndUpdateWatermark` — 100% covered (BUG-010 watermark seam)
- RETAINED: `createBackupForegroundInfo` — 92% covered
- NOT COVERED: `backupCursors` (35 lines, 0%) — requires real SMS data in providers; not reachable without seeding ContentProvider
- NOT COVERED: `handleAuthError` (26 lines, 0%) — requires XOAuth2FailedException thrown from backupCursors; same blocker

**RestoreWorker** (was excluded, now at 80.2%):
- RETAINED: `doWork` entrypoint — 84% covered
- RETAINED: `executeRestore` — 27% covered (checkSettings, openFolder, getMessages delegation)
- RETAINED: `runImapRestoreLoop` — now covered (was 0%; U-052 IMAP test with 1 message)
- RETAINED: `importMessage` — now covered (was 0%; failure path and CALLLOG dispatch)
- RETAINED: `importCallLogValues` — now covered (was 0%)
- RETAINED: `callLogExists` — now covered (was 0%)
- RETAINED: `insertSmsValues` — 72% covered (from executeRestoreWithValues tests)
- RETAINED: `executeRestoreWithValues` — 90% covered
- RETAINED: `smsExists` — 100% covered
- RETAINED: `updateAllThreadsIfAnySmsRestored` — 100% covered
- NOT COVERED: `handleAuthError` (18 lines, 0%) — requires XOAuth2FailedException from checkSettings; tested via unit mock in RestoreWorkerTest
- NOT COVERED: `clearAppCache` (5 lines, 0%) — only triggered at i%50==0 in restore loop; requires 50+ items

## Integration Path

New code is reachable from production entry points:
- `BackupImapStoreDelegateIntegrationTest` exercises `K9MailTransport$BackupImapStoreDelegate` via
  `GreenMail.start()` → TCP IMAP → `K9MailTransport.checkSettings()` → `BackupImapStoreDelegate.openFolder()`.
- `BackupWorkerIntegrationTest` exercises `BackupWorker.doWork()` via `TestListenableWorkerBuilder`
  → real `WorkManager` test environment → actual `doWork()` coroutine invocation.
- `RestoreWorkerIntegrationTest` exercises `RestoreWorker.doWork()` and
  `RestoreWorker.executeRestoreWithValues()` via same WorkManager test harness.

## Notes

### GreenMail HEADER Search Limitation
GreenMail 2.1.3 does NOT support `UID SEARCH HEADER <field> <value>` — it throws
`BadCharsetException` when the search extension is invoked. The test `searchByHeader_notSupportedByGreenMail_throwsMailException`
explicitly verifies that `BackupImapStoreDelegate.getMessages(since=...)` translates this into a
`MailException`, covering the exception-translation path. This is documented in the test Javadoc.

### Supply-Chain Verification
GreenMail 2.1.3 and its 5 transitive dependencies were added to `gradle/verification-metadata.xml`
with individual SHA-256 checksums. No wildcard `.*` trust rules were used. The `verify-metadata=true`
flag was preserved.

### Null Email NPE Root Cause
`AuthPreferences.getImapUsername()` returns null in a fresh Robolectric context (no preferences
set). `MessageConverter` calls `new Address(null)` which delegates to
`Rfc822Tokenizer.tokenize(null)` → `NullPointerException`. Fix: call
`authPreferences.setImapUser("backup@test.local")` in `setUp()` before building any worker that
reaches `fetchAndBackupItems` or `executeRestore`.

### BackupWorker.backupCursors Coverage Gap
`backupCursors` (35 lines, 0%) and `BackupWorker.handleAuthError` (26 lines, 0%) require real
SMS/MMS ContentProvider data to be seeded in Robolectric. Without data, `itemsToSync = 0` and
`backupCursors` is never called. Seeding the Android SMS provider in Robolectric requires
`ShadowContentResolver.registerProvider` and populating a fake cursor. This is deferred as a
follow-on story; the coverage gate at 70% is met by other covered methods.

### MailTransport ACL Boundary
No `com.fsck.k9.*` types cross the `service.*` boundary. `MailMessageHandle` and
`BackupFolderHandle` are app-owned types. `MailTransportTestFactories.createHandle()` (in the
`mail.transport` package) is used to construct `MailMessageHandle` instances in tests without
violating the package-private constructor constraint. Verified via grep for k9 imports in service
test files — none found.

### BUG-010 Watermark Invariant
The `appendBatchAndUpdateWatermark` seam is tested at 100% via `BackupWorkerWatermarkTest` (U-053).
`BackupWorkerIntegrationTest` confirms the watermark advance via `appendBatchAndUpdateWatermark_succeeds_advancesWatermark` and the
no-advance-on-failure invariant via `appendBatchAndUpdateWatermark_openFolderFails_watermarkUnchanged` and
`appendBatchAndUpdateWatermark_appendFails_watermarkUnchanged`. These use production
`BackupWorker` instances constructed via `TestableBackupWorkerFactoryWithTransport`.
