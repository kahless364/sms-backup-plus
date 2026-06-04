---
artifact_type: implementation-log
story_id: U-030
verdict: PASS
agent: Developer
timestamp: "2026-06-04"
files_changed: 11
files_created: 0
tests_added: 3
tests_passing: 594
---

# Implementation Log: U-030

## Summary

Sealed the MailTransport ACL exception boundary. Two k-9 exception leaks in `service.*` are closed:
- Clause C-1: `K9MailTransport` public constructor throws narrowed from `throws MailException, MessagingException` to `throws MailException`; `MessagingException` from `BackupImapStoreDelegate` construction now translated inside the constructor body.
- Clause C-2: `MessageConverter.convertMessages` throws narrowed from `MessagingException` to `MailException`; internal `MessagingException` wrapped with cause preserved.

Consumer-side FQN catches in `ServiceBase.java` and `BackupTask.java` eliminated (simplified to direct calls).

Twelve comment-residual `com.fsck.k9` occurrences in 5 service files reworded to remove FQN literals.

Grep-zero gate: `grep -rn "com\.fsck\.k9" service/` (excluding BackupTask.java + RestoreTask.java which are deleted by U-031) returns ZERO lines.

## Files Modified

### Production Code

1. **`app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java`**
   - Public constructor at lines 117-126: removed `MessagingException` from `throws` clause
   - Wrapped `new BackupImapStoreDelegate(...)` in `try { ... } catch (com.fsck.k9.mail.MessagingException e) { throw new MailException(e); }`
   - `BinaryTempFileBody.setTempDirectory(context.getCacheDir())` preserved after the delegate construction
   - Javadoc updated to remove `@throws MessagingException` and document MailException wrapping
   - CNTR-007 C-1 implemented

2. **`app/src/main/java/com/zegoggles/smssync/mail/MessageConverter.java`**
   - Added import `com.zegoggles.smssync.mail.transport.MailException`
   - `convertMessages(Cursor, DataType)` method: changed `throws MessagingException` to `throws MailException`
   - Wrapped method body in `try { ... } catch (MessagingException e) { throw new MailException(e); }`
   - Added package-private test constructor accepting `MessageGenerator` (for AC-8 C-2 test seam)
   - CNTR-007 C-2 implemented

3. **`app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java`**
   - Lines 170-175: removed `try { ... } catch (com.fsck.k9.mail.MessagingException e) { throw new MailException(e); }` around `return new K9MailTransport(getApplicationContext(), config)`
   - Simplified to direct `return new K9MailTransport(getApplicationContext(), config);`
   - AC-5 consumer-side collapse complete

4. **`app/src/main/java/com/zegoggles/smssync/service/BackupTask.java`**
   - Lines 302-310: removed inner `try/catch(com.fsck.k9.mail.MessagingException e)` around `converter.convertMessages(...)`
   - Simplified to direct `final ConversionResult result = converter.convertMessages(cursor.cursor, cursor.type);`
   - Updated comment to reference U-030
   - AC-6 consumer-side collapse complete

5. **`app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt`**
   - Line 55: reworded class-level javadoc to remove `com.fsck.k9.*` FQN literal

6. **`app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt`**
   - Line 30: reworded comment to remove `com.fsck.k9.*` FQN literal
   - Line 66: reworded class-level javadoc to remove `com.fsck.k9.*` FQN literal

7. **`app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java`**
   - Line 27: reworded comment to remove `com.fsck.k9.mail.MessagingException` literal
   - Line 78: reworded javadoc to remove `com.fsck.k9.mail.MessagingException` literal

8. **`app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java`**
   - Lines 11-12: reworded comments to remove k-9 FQN literals
   - Lines 41-42: reworded javadoc to remove k-9 FQN literals

9. **`app/src/main/java/com/zegoggles/smssync/service/state/State.java`**
   - Lines 6-8: consolidated 3 comment lines into 3-line comment block without k-9 FQN literals

### Test Code

10. **`app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportTest.java`**
    - `shouldThrowExceptionIfUsernameIsMissing` (was `expected = MessagingException.class`): updated to catch `MailException` and verify `getCause() instanceof MessagingException`
    - `shouldThrowExceptionIfPasswordIsMissing` (was `expected = MessagingException.class`): same update
    - Added `causeChain_C1_constructor_messagingExceptionPreservedAsCause`: AC-8 C-1 path test

11. **`app/src/test/java/com/zegoggles/smssync/mail/MessageConverterTest.java`**
    - Added import `android.database.Cursor` and `com.zegoggles.smssync.mail.transport.MailException`
    - Added `convertMessages_messagingExceptionWrappedAsMailException_causeChainPreserved`: AC-8 C-2 path test

## Contract Adherence

### CNTR-MODERNIZATION-007 v2

| Clause | Requirement | Implementation | File:Line |
|--------|-------------|----------------|-----------|
| C-1 | K9MailTransport public ctor throws only MailException | Constructor narrowed; BackupImapStoreDelegate construction wrapped in catch | `K9MailTransport.java:117-130` |
| C-1 | Cause chain preserved (new MailException(e)) | `catch (MessagingException e) { throw new MailException(e); }` | `K9MailTransport.java:121-123` |
| C-1 | BinaryTempFileBody.setTempDirectory preserved | Call at line 129, after delegate block | `K9MailTransport.java:129` |
| C-1 | Package-private test ctor unchanged | No modification to `K9MailTransport(BackupImapStoreDelegate, TrustedSocketFactory)` | `K9MailTransport.java:93-96` |
| C-2 | convertMessages throws MailException | `throws MailException` in method signature | `MessageConverter.java:140-141` |
| C-2 | Cause chain preserved (new MailException(e)) | `catch (MessagingException e) { throw new MailException(e); }` | `MessageConverter.java:150-152` |
| C-2 | MailException import added to MessageConverter | `import com.zegoggles.smssync.mail.transport.MailException;` | `MessageConverter.java:34` |
| Validation Rule 1 | grep com.fsck.k9 in service/ -> 0 (excl. BackupTask/RestoreTask) | ZERO lines returned | Verified below |
| Interface byte-unchanged | MailTransport.java not edited | `git diff` shows zero changes | `MailTransport.java` |

## Test Results

- `./gradlew :app:assembleDebug`: BUILD SUCCESSFUL
- `./gradlew :app:compileDebugJavaWithJavac`: BUILD SUCCESSFUL (zero errors)
- `./gradlew :app:testDebugUnitTest`: BUILD SUCCESSFUL (zero failures)
- `./gradlew :app:jacocoTestCoverageVerification`: BUILD SUCCESSFUL (coverage gate passes)
- Test count: 594 @Test annotations (above 568 floor)

## Grep-Zero Gate Result

```
grep -rn "com\.fsck\.k9" app/src/main/java/com/zegoggles/smssync/service/
```

Full result (6 lines in BackupTask.java + RestoreTask.java only):
- `BackupTask.java:8` — comment (deleted by U-031)
- `BackupTask.java:51` — comment (deleted by U-031)
- `BackupTask.java:286` — comment (deleted by U-031)
- `RestoreTask.java:14` — comment (deleted by U-031)
- `RestoreTask.java:55` — comment (deleted by U-031)
- `RestoreTask.java:134` — comment (deleted by U-031)

Excluding BackupTask.java + RestoreTask.java: **ZERO lines** (gate PASSED).

All other service files (BackupWorker.kt, RestoreWorker.kt, SmsBackupService.java, SmsRestoreService.java, State.java, ServiceBase.java, and all subdirs) are clean.

## Integration Verification

- `ServiceBase.getMailTransport()` compiles with only `throws MailException` — IC-1 satisfied
- `BackupWorker.kt:310` (`val result = converter.convertMessages(...)`) compiles unchanged — IC-2 satisfied (MailException already caught at BackupWorker.kt:138)
- `BackupTask.java` direct call compiles under new signature — IC-3 satisfied

## Notes

- `MailModule.kt:93-97` redundant `catch (MessagingException)` wrapper retained (optional cleanup, not required by any AC).
- The 6 `com.fsck.k9` comment occurrences in BackupTask.java (lines 8, 51, 286) and RestoreTask.java (lines 14, 55, 134) are out of scope for this story — those files are deleted wholesale by U-031 (REQ-013).
- AC-8 C-1 test uses a URI with missing credentials to trigger k-9's internal MessagingException and verify it is wrapped as MailException with getCause() instanceof MessagingException.
- AC-8 C-2 test uses a package-private test constructor (added to MessageConverter) that accepts a MessageGenerator mock, which is configured to throw MessagingException("conversion failed").
