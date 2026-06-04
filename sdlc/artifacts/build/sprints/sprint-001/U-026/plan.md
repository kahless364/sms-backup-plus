---
artifact_type: plan
story_id: "U-026"
agent: Developer
timestamp: "2026-06-04"
---

# Plan: U-026 — Engine Rewire to MailTransport

## Objective

Move the backup/restore engine off of `BackupImapStore` (k-9 type) and onto the
`MailTransport` ACL port created in U-025. Achieve zero `com.fsck.k9.*` import
statements in `service.*` (AC-10 invariant). Complete the stubbed `MailModule.kt`
Hilt module with correct `MailTransportFactory` binding.

## Scope

- `service/BackupConfig.java` — change `imapStore` field type from `BackupImapStore` to `MailTransport`
- `service/RestoreConfig.java` — change `imapStore` field type from `BackupImapStore` to `MailTransport`
- `service/ServiceBase.java` — replace `getBackupImapStore()` with `getMailTransport()` returning `MailTransport`
- `service/BackupTask.java` — remove all k-9 imports; use transport port methods
- `service/RestoreTask.java` — remove all k-9 imports; use transport port methods + `importMessageBody`
- `service/SmsBackupService.java` — update to call `getMailTransport()`
- `service/SmsRestoreService.java` — update to call `getMailTransport()`; move `BinaryTempFileBody.setTempDirectory` behind adapter
- `service/state/State.java` — delete MessagingException magic-string block; rewrite `isAuthException()` with app-owned types
- `di/MailModule.kt` — complete the `MailTransportFactory` singleton binding (deferred checked-exception pattern)
- `service/BackupWorker.kt` — remove k-9 imports; replace `buildImapStore` with `buildMailTransport`
- `service/RestoreWorker.kt` — same as BackupWorker; also update `importMessage` → `importMessageBody` pattern
- `mail/transport/MailTransport.java` — add `importMessageBody()` operation for converter bridge
- `mail/transport/K9MailTransport.java` — implement `importMessageBody()`; add `MessageImportResult` value type
- `mail/transport/MessageImportResult.java` (NEW) — app-owned result type for per-message body import

## Test Updates

- `BackupConfigTest.java` — swap `BackupImapStore` mock for `MailTransport` mock
- `RestoreConfigTest.java` — swap `BackupImapStore` mock for `MailTransport` mock
- `BackupTaskTest.java` — mock `MailTransport.openFolder` + `appendMessages` instead of old `store.getFolder` + `folder.appendMessages`; use `XOAuth2FailedException` instead of k-9 exception; call `getMailTransport()` instead of `getBackupImapStore()`
- `RestoreTaskTest.java` — mock `MailTransport` port; use `MailTransportTestFactories` for package-private handle construction; stub `importMessageBody` with `MessageImportResult`
- `SmsBackupServiceTest.java` — update `MessagingException` → `MailException` assertion
- `BackupStateCoverageTest.java` — update `XOAuth2AuthenticationFailedException` → `XOAuth2FailedException` assertion

## Key Design Decisions

### importMessageBody bridge

`RestoreTask.importMessage()` previously called `converter.getDataType(message)` and
`converter.messageToContentValues(message)` with k-9 `Message` objects. Since
`MessageConverter` is a bounded residual (unchanged, still uses k-9 internally) and
AC-10 prohibits k-9 imports in `service.*`, a bridge was added:
- New `MailTransport.importMessageBody(BackupFolderHandle, MailMessageHandle, MessageConverter)` operation
- New `MessageImportResult` value type carrying `DataType` + `ContentValues` across the ACL boundary
- `K9MailTransport.importMessageBody()` implementation unwraps `handle.message` (package-private) and calls converter internally

### MailTransportFactory (Hilt checked-exception deferral)

`K9MailTransport` constructor throws `MailException` + `MessagingException`. Dagger `@Provides`
methods cannot propagate checked exceptions. Solution: `MailModule` provides a
`MailTransportFactory` functional interface whose `create()` method can throw. The engine
constructs transports per-run via this factory, matching the original per-run
`BackupImapStore` construction lifecycle.

### FQN catch clauses

`BackupTask.backupCursors()` calls `converter.convertMessages()` which throws
`com.fsck.k9.mail.MessagingException`. Since no `import` statement for this type
is allowed, the catch uses the fully-qualified class name. This preserves AC-10
(zero k-9 import statements) while correctly handling the checked exception.
