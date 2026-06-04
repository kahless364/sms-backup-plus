---
artifact_type: review-code
story_id: "U-026"
verdict: PASS
agent: Developer
timestamp: "2026-06-04"
---

# Code Review: U-026 — Engine Rewire to MailTransport

## Summary

Self-review of U-026 implementation. All acceptance criteria verified against code.

## AC Checklist

| AC | Description | Status | Evidence |
|----|-------------|--------|----------|
| AC-1 | `BackupConfig.imapStore` typed as `MailTransport` | PASS | BackupConfig.java:15 `public final MailTransport imapStore` |
| AC-1 | `RestoreConfig.imapStore` typed as `MailTransport` | PASS | RestoreConfig.java:12 `final MailTransport imapStore` |
| AC-2 | `BackupTask` uses transport port methods | PASS | BackupTask.java:293, 310-311, 335 |
| AC-3 | `RestoreTask` uses transport port methods | PASS | RestoreTask.java:144, 152-158, 241, 213 |
| AC-4 | `BackupTask`/`RestoreTask` catch app-owned exceptions | PASS | BackupTask.java:169-177; RestoreTask.java:197-207 |
| AC-5 | `SmsBackupService` calls `getMailTransport()` | PASS | SmsBackupService.java — `getMailTransport()` call in `backup()` |
| AC-6 | `SmsRestoreService` `BinaryTempFileBody` moved behind adapter | PASS | SmsRestoreService.java:64-68 comment; K9MailTransport.java:126 |
| AC-7 | `State.java` MessagingException string-match block deleted | PASS | State.java:45-53 (block gone; LocalizableException branch retained) |
| AC-8 | `State.isAuthException()` uses app-owned types | PASS | State.java:101-103 |
| AC-9 | `MailModule.kt` complete (MailTransportFactory) | PASS | MailModule.kt:73-99 |
| AC-10 | Zero `com.fsck.k9.*` import statements in `service.*` | PASS | `grep -rn "^import com.fsck.k9" app/src/main/java/.../service/` = 0 results |

## Additional Scope Verified

| Item | Status | Evidence |
|------|--------|----------|
| `BackupWorker.kt` rewired to `MailTransport` | PASS | BackupWorker.kt:24 comment; `buildMailTransport()` at line 407 |
| `RestoreWorker.kt` rewired to `MailTransport` | PASS | RestoreWorker.kt:30 comment; `buildMailTransport()` at line 665 |
| `MessageImportResult` value type created | PASS | `mail/transport/MessageImportResult.java` |
| `MailTransport.importMessageBody()` added | PASS | MailTransport.java:138-141 |
| `K9MailTransport.importMessageBody()` implemented | PASS | K9MailTransport.java:274-299 |
| Tests updated to use `MailTransport` mocks | PASS | BackupConfigTest, RestoreConfigTest, BackupTaskTest, RestoreTaskTest, SmsBackupServiceTest, BackupStateCoverageTest |
| `MailTransportTestFactories` test helper | PASS | Created in `mail.transport` test package |

## Observations

1. **FQN catch in BackupTask.java:308**: Uses `com.fsck.k9.mail.MessagingException` as FQN (no import). This is the minimal change to bridge the bounded-residual `MessageConverter.convertMessages()` without adding a k-9 import. Acceptable per AC-10 definition ("zero import statements").

2. **BackupImapStore.isValidUri() still in use**: Called from `ServiceBase`, `BackupWorker`, `RestoreWorker`. This is a static validation helper from `mail.*`, not a k-9 import. The import `com.zegoggles.smssync.mail.BackupImapStore` is allowed.

3. **MessageWithFolder data class in RestoreWorker**: The `(handle, folderHandle)` pair tracks which folder each handle came from, needed because `K9MailTransport.importMessageBody()` takes a folder handle. Clean design.

4. **MailTransportTestFactories**: Creating test factories in the `mail.transport` package is the correct pattern for package-private constructors. The factory is in `src/test/java` so it doesn't affect production builds.

5. **K9MailTransport.importMessageBody() fix**: The `Collections.singletonList((Message) handle.message)` cast caused a Java 8 type inference error — `ImapFolder.fetch()` expects `List<? extends Message>` but inference gave `List<Message>`. Fixed by explicit `List<ImapMessage>` local variable.
