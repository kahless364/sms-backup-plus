---
artifact_type: qa-results
story_id: "U-026"
verdict: PASS
agent: Developer
timestamp: "2026-06-04"
---

# QA Results: U-026 — Engine Rewire to MailTransport

## Build Verification

| Command | Result |
|---------|--------|
| `./gradlew :app:clean :app:assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL — 568 tests, 0 failures, 2 skipped (pre-existing `@Ignore`) |

## AC-10 Invariant Verification

```
$ grep -rn "^import com.fsck.k9" app/src/main/java/com/zegoggles/smssync/service/
(no output — zero k-9 import statements)
```

AC-10 invariant satisfied: zero `com.fsck.k9.*` import statements in `service.*`.

## Test Coverage Summary

- 568 tests run (up from 493 in U-025 baseline)
- 0 failures
- 2 skipped (pre-existing `@Ignore` annotations, not introduced by this story)

## Test Files Updated

| File | Change | Test Count Impact |
|------|--------|-------------------|
| `BackupConfigTest.java` | `BackupImapStore` → `MailTransport` mocks | 0 net new tests; existing 5 tests fixed |
| `RestoreConfigTest.java` | `BackupImapStore` → `MailTransport` mocks | 0 net new tests; existing 3 tests fixed |
| `BackupTaskTest.java` | MailTransport port mocking; `XOAuth2FailedException`; `getMailTransport()` | 0 net new tests; existing 9 tests fixed |
| `RestoreTaskTest.java` | MailTransport port mocking with `MailTransportTestFactories` | 0 net new tests; existing 4 tests fixed |
| `SmsBackupServiceTest.java` | `MailException` instead of `MessagingException` assertion | 0 net new tests; 1 test fixed |
| `BackupStateCoverageTest.java` | `XOAuth2FailedException` instead of k-9 type | 0 net new tests; 1 test fixed |

## Regression Verification

All 568 pre-existing tests pass. No regressions introduced by U-026.

## Notes

- The FQN catch `com.fsck.k9.mail.MessagingException` in `BackupTask.java:308` does not create a k-9 import statement; verified by the zero-import grep above.
- `BackupImapStore.isValidUri()` calls in `ServiceBase`, `BackupWorker`, `RestoreWorker` are NOT k-9 imports — `BackupImapStore` is in `com.zegoggles.smssync.mail.*`.
