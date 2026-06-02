---
artifact_type: qa-results
story_id: "U-005"
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# QA Results: U-005

## Test Execution

**Command:** `./gradlew :app:testDebugUnitTest`
**Result:** BUILD SUCCESSFUL

```
217 tests total
216 passed
1 skipped (AppTest.shouldGetVersionName — @Ignore, pre-existing before this story)
0 failures
0 errors
```

The suite was GREEN with 0 failures.

## Dependency Verification (AC-1 through AC-4)

**Command:** `./gradlew :app:dependencies --configuration debugUnitTestRuntimeClasspath`

| AC | Dependency | Resolved Version | Status |
|----|-----------|-----------------|--------|
| AC-2 | junit:junit | 4.13.2 | PASS |
| AC-1 | org.robolectric:robolectric | 4.12.2 | PASS |
| AC-4 | com.google.truth:truth | 1.4.4 | PASS |
| AC-3 | org.mockito:mockito-core | 5.14.2 | PASS |
| AC-3b | org.mockito:mockito-all | NOT PRESENT | PASS |

## JaCoCo Coverage Report

**Command:** `./gradlew :app:jacocoTestReport`
**Result:** BUILD SUCCESSFUL

**Actual line coverage (from jacocoTestReport.xml):**

| Package | Lines Covered | Lines Total | Coverage |
|---------|--------------|-------------|----------|
| com/zegoggles/smssync/service | 624 | 959 | 65.1% |
| com/zegoggles/smssync/service/state | 63 | 92 | 68.5% |
| com/zegoggles/smssync/service/exception | 11 | 16 | 68.8% |
| com/zegoggles/smssync/mail | 466 | 691 | 67.4% |
| com/zegoggles/smssync/auth | 65 | 183 | 35.5% |

**All three gated packages are BELOW the 70% threshold.**

- service/ (combined): 698/1067 lines = 65.4% (threshold: 70%)
- mail/: 466/691 = 67.4% (threshold: 70%)
- auth/: 65/183 = 35.5% (threshold: 70%)

## Coverage Gate Execution

**Command:** `./gradlew :app:jacocoTestCoverageVerification`
**Result:** BUILD SUCCESSFUL (passes — see note)

**IMPORTANT NOTE:** The gate currently PASSES despite coverage being below 70% due to a
JaCoCo glob pattern issue inherited from U-004. The `includes = ['com/zegoggles/smssync/service/**']`
pattern in JaCoCo's `element = 'PACKAGE'` rule matches only sub-packages (service/state,
service/exception) via the `**` glob, but NOT the top-level service package itself.
The sub-packages happen to be above 60% each.

The coverage gate is NOMINALLY passing but is NOT enforcing 70% on the main packages.
This is an acknowledged U-004 design gap. Backfilling coverage to actually exceed 70%
is U-006's scope. The glob fix (using `com/zegoggles/smssync/service*` to include
the package itself) should be addressed in U-006 or a dedicated cleanup task.

## --add-opens Verification

The `tasks.withType(Test).configureEach { jvmArgs '--add-opens...' }` block was REMOVED.
Tests pass without any --add-opens flags. Robolectric 4.12.2 handles JDK 17 natively.

## AC Compliance Summary

| AC | Description | Status | Notes |
|----|-------------|--------|-------|
| AC-1 | Robolectric 4.12.x | PASS | 4.12.2 resolved |
| AC-2 | JUnit 4.13.2 | PASS | 4.13.2 resolved |
| AC-3 | mockito-core 5.x, no mockito-all | PASS | 5.14.2, no mockito-all |
| AC-4 | Truth 1.4.x | PASS | 1.4.4 resolved |
| AC-5 | All 35 test classes pass | PASS | 36 test classes, 216 pass, 1 skip (pre-existing @Ignore) |
| AC-6 | Same-PR as U-003 SDK raise | DEFERRED | U-003 not yet executed; ordering constraint noted |
| AC-7 | verification-metadata.xml | DEFERRED | Requires U-003 jcenter removal first |

## Test Class Count

36 test classes present (story mentions 35; story count is from the original pre-U-001 state):
```
AppTest, OAuth2TokenTest, TokenRefresherTest, CalendarAccessorPost40Test,
SmsReceiverTest, ContactAccessorTest, ContactGroupsTest, AttachmentTest,
BackupImapStoreTest, CallFormatterTest, ConversionResultTest, HeaderGeneratorTest,
MessageConverterTest, MessageGeneratorTest, PersonLookupTest, PersonRecordTest,
AuthPreferencesTest, PreferencesTest, BootReceiverTest, SmsBroadcastReceiverTest,
AlarmManagerDriverTest, BackupConfigTest, BackupCursorsTest, BackupItemsFetcherTest,
BackupJobsTest, BackupQueryBuilderTest, BackupTaskTest, BulkFetcherTest,
CalendarSyncerTest, RestoreTaskTest, SmsBackupServiceTest, SmsJobServiceTest,
StateTest, DonationActivityTest, SkuTest, PreferenceTitlesTest
```
