---
artifact_type: plan
story_id: U-030
verdict: PASS
agent: Developer
timestamp: "2026-06-04"
---

# Implementation Plan: U-030 — Seal MailTransport ACL Exception Boundary

## Objective

Remove all `com.fsck.k9.*` references from `service/` (code AND comments) by:
1. Narrowing `K9MailTransport` public constructor throws clause from `throws MailException, MessagingException` to `throws MailException` only (CNTR-007 C-1)
2. Narrowing `MessageConverter.convertMessages` to throw `MailException` instead of `MessagingException` (CNTR-007 C-2)
3. Collapsing consumer-side FQN catches in `ServiceBase.java` and `BackupTask.java`
4. Scrubbing 12 comment-residual `com.fsck.k9` occurrences from 5 service files
5. Adding AC-8 cause-chain unit tests at both translation sites

## Changes Planned

### Production Code

| File | Change |
|------|--------|
| `mail/transport/K9MailTransport.java` | Narrow public ctor throws to `MailException`; wrap `new BackupImapStoreDelegate(...)` in try/catch (CNTR-007 C-1) |
| `mail/MessageConverter.java` | Change `convertMessages` throws to `MailException`; add internal try/catch; add MailException import; add pkg-private test ctor |
| `service/ServiceBase.java` | Remove FQN try/catch around K9MailTransport constructor call |
| `service/BackupTask.java` | Remove inner try/catch for FQN MessagingException around convertMessages; simplify to direct call |
| `service/BackupWorker.kt` | Reword line 55 comment — remove `com.fsck.k9.*` literal |
| `service/RestoreWorker.kt` | Reword lines 30, 66 comments — remove `com.fsck.k9.*` literals |
| `service/SmsBackupService.java` | Reword lines 27, 78 comments — remove `com.fsck.k9.mail.MessagingException` literals |
| `service/SmsRestoreService.java` | Reword lines 11, 12, 41, 42 comments — remove k-9 FQN literals |
| `service/state/State.java` | Reword lines 6, 7, 8 comments — remove k-9 FQN literals |

### Test Code

| File | Change |
|------|--------|
| `test/.../transport/K9MailTransportTest.java` | Update 2 tests that expected `MessagingException` from ctor to expect `MailException`; add AC-8 C-1 cause-chain test |
| `test/.../mail/MessageConverterTest.java` | Add AC-8 C-2 cause-chain test (`convertMessages_messagingExceptionWrappedAsMailException_causeChainPreserved`) |

## Scope Boundary

- `MailTransport.java` (interface): NOT touched
- `BackupTask.java` / `RestoreTask.java`: deleted by U-031; comment refs there are out of scope
- `MailModule.kt` redundant catch: optional cleanup, NOT required by this story
- Package-private test ctor `K9MailTransport(BackupImapStoreDelegate, TrustedSocketFactory)`: NOT touched

## Contract Adherence

- CNTR-007 C-1: K9MailTransport ctor throws narrowed; MessagingException translated with cause preserved
- CNTR-007 C-2: MessageConverter.convertMessages throws narrowed; MessagingException wrapped with cause preserved
- CNTR-007 Validation Rule 1: grep `com.fsck.k9` in `service/` -> 0 (excluding BackupTask/RestoreTask)
- `MailTransport.java` interface byte-unchanged (git diff shows zero changes)
