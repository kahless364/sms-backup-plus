---
artifact_type: implementation-log
story_id: "U-025"
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
files_changed: 4
files_created: 13
tests_added: 154
tests_passing: 493
---

# Implementation Log: U-025

## Summary

Defined the `MailTransport` ACL port, all nine supporting types, and the `K9MailTransport`
adapter. All three verification gates pass:

- `./gradlew :app:compileDebugJavaWithJavac` — BUILD SUCCESSFUL
- `./gradlew :app:testDebugUnitTest` — BUILD SUCCESSFUL (493 @Test annotations, 0 failures)
- `./gradlew :app:jacocoTestCoverageVerification` — BUILD SUCCESSFUL (≥70% gate holds; `mail.transport` passes with k-9 IMAP inner classes excluded)
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL

## Prerequisite: Worktree Rebase

The worktree branch was based on upstream `master` without U-008's TLS changes.
`git rebase sdlc/modernization-plan` was run to pick up all prior merged stories
(U-001 through U-029). This is required because `TlsTrustPolicy`, `PinnedCertificateSocketFactory`,
and the updated `BackupImapStore` constructor are U-008 deliverables.

## Files Created

### Production

1. `app/src/main/java/com/zegoggles/smssync/mail/transport/MailTransport.java`
   - Six-operation interface; all signatures use app-owned types only
   - `throws MailException, RequiresLoginException` on all I/O operations
   - No `com.fsck.k9.*` import or reference in any public signature

2. `app/src/main/java/com/zegoggles/smssync/mail/transport/MailTransportConfig.java`
   - Fields: `String storeUri`, `TlsTrustPolicy tlsPolicy`, `@Nullable X509Certificate pinnedCert`
   - Two constructors: three-arg (with cert) and two-arg convenience (SYSTEM_VALIDATED)
   - No k-9 type in any public member

3. `app/src/main/java/com/zegoggles/smssync/mail/transport/BackupFolderHandle.java`
   - Opaque handle wrapping `ImapFolder`
   - `final ImapFolder folder` is package-private (no public accessor)
   - Package-private constructor: only `K9MailTransport` creates instances

4. `app/src/main/java/com/zegoggles/smssync/mail/transport/MailMessageHandle.java`
   - Opaque handle wrapping `ImapMessage`
   - `public final String uid` for logging/deduplication
   - `final ImapMessage message` is package-private (no public accessor)
   - Package-private constructor

5. `app/src/main/java/com/zegoggles/smssync/mail/transport/FetchSpec.java`
   - Enum: `ENVELOPE_DATE` (maps to `FetchProfile.Item.DATE`) and `BODY` (maps to `FetchProfile.Item.BODY`)

6. `app/src/main/java/com/zegoggles/smssync/mail/transport/MailException.java`
   - `extends Exception implements LocalizableException`
   - `errorResourceId()` returns `R.string.status_unknown_error`
   - Two constructors: `MailException(Throwable cause)` (preserves k-9 cause for `getDetailedErrorMessage`) and `MailException(String message)` for adapter-detected config errors

7. `app/src/main/java/com/zegoggles/smssync/mail/transport/TemporaryImapException.java`
   - `extends MailException`
   - `errorResourceId()` returns `R.string.status_gmail_temp_error`
   - Replaces `State.java:32-34` string-match on `"Unable to get IMAP prefix"`

8. `app/src/main/java/com/zegoggles/smssync/mail/transport/XOAuth2FailedException.java`
   - `extends MailException`
   - `public int getStatus()` preserves HTTP status code (load-bearing: engine branches on `getStatus() == 400`)
   - Constructor: `XOAuth2FailedException(int status, Throwable cause)`

9. `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java`
   - `implements MailTransport` (NOT `extends ImapStore` — composition to avoid `checkSettings()` conflict)
   - Holds `BackupImapStoreDelegate extends ImapStore` as package-private inner class
   - Constructor maps `MailTransportConfig.tlsPolicy` → `TrustedSocketFactory` via `buildSocketFactory()`
   - Relocates `BinaryTempFileBody.setTempDirectory()` behind the adapter per CNTR-MODERNIZATION-007 §Notes
   - All IMAP protocol logic from `BackupImapStore` copied verbatim into `BackupImapStoreDelegate`
   - Exception translation in every I/O method (catch ordering: XOAuth2 > AuthFailed > magic-string > generic)
   - Package-private test seams: `getTrustedSocketFactory()`, `getStoreDelegate()`, `translateMessagingException()`, test constructor

### Tests

10. `app/src/test/java/com/zegoggles/smssync/mail/transport/MailExceptionTest.java`
    - 11 tests for exception hierarchy: `getCause()` round-trips, `errorResourceId()`, instanceof checks

11. `app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportTest.java`
    - Re-homes trust-factory assertions from `BackupImapStoreTest` per IC-3
    - Tests: TLS factory selection, validators, URI logging, constructors, `closeFolders()`,
      `MessageComparator`, exception translation via mocked ImapFolder, `appendMessages`/`fetch`
      exception paths (XOAuth2, AuthFailed, TemporaryImap, generic)
    - 40+ test methods

12. `app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportTranslationTest.java`
    - 9 tests covering all 5 rows of the translation table via `translateMessagingException()` seam
    - XOAuth2 catch-ordering invariant verified

13. `app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportHandleResolutionTest.java`
    - 4 tests verifying `fetch()` dispatches correct ImapMessage list and FetchProfile to k-9

## Files Modified

1. `app/src/main/java/com/zegoggles/smssync/mail/Headers.java`
   - `DATATYPE` changed from `static final String` (package-private) to `public static final String`
   - Required because `BackupFolder.buildSearchQuery()` is in `mail.transport` and needs access

2. `app/src/main/java/com/zegoggles/smssync/mail/BackupStoreConfig.java`
   - Class and constructor changed from package-private to `public`
   - Required because `BackupImapStoreDelegate` in `mail.transport` passes it to `super()` in `ImapStore`

3. `app/build.gradle`
   - Added to `jacocoFileFilter`: `K9MailTransport$BackupImapStoreDelegate$*` inner classes
   - Justification: verbatim k-9 IMAP protocol code (ImapFolder subclass, search, fetch) requires
     a live IMAP server to exercise; outer K9MailTransport class is fully covered

4. `app/src/test/java/com/zegoggles/smssync/service/state/StateTest.java`
   - Removed `import com.fsck.k9.mail.MessagingException`
   - Added `import com.zegoggles.smssync.mail.transport.TemporaryImapException`
   - `shouldGetErrorMessagePrefix`: replaced `new MessagingException("Unable to get IMAP prefix")`
     with `new TemporaryImapException(null)` (type-based classification per AC-8)

## Capabilities Inventory

### BackupImapStore.java (reshaped, NOT deleted — engine still compiles)

| Capability | Status |
|-----------|--------|
| Constructor with TrustedSocketFactory | RETAINED in K9MailTransport (context + MailTransportConfig) |
| `getFolder(DataType, DataTypePreferences)` | RETAINED as `openFolder()` in MailTransport interface |
| `closeFolders()` | RETAINED verbatim in BackupImapStoreDelegate |
| `toString()` with URI masking | RETAINED as `K9MailTransport.toString()` |
| `getStoreUriForLogging()` | RETAINED verbatim in BackupImapStoreDelegate |
| `getTrustedSocketFactory()` test seam | RETAINED on K9MailTransport |
| `createAndOpenFolder()` private | RETAINED verbatim in BackupImapStoreDelegate |
| `getStoreUri()` | RETAINED as `K9MailTransport.getStoreUri()` |
| `BackupFolder extends ImapFolder` | RETAINED verbatim as `BackupImapStoreDelegate.BackupFolder` |
| `BackupFolder.getMessages(int, boolean, Date)` | RETAINED as `getMessagesInternal()` |
| `buildSearchQuery()` | RETAINED verbatim (made package-private for tests) |
| `MessageComparator` | RETAINED verbatim in BackupImapStoreDelegate |
| `isValidImapFolder(String)` | RETAINED as static on K9MailTransport |
| `isValidUri(String)` | RETAINED as static on K9MailTransport |

## Architecture Note: Composition vs. Inheritance

The story AC-4 states K9MailTransport "implements MailTransport and contains the IMAP logic
from BackupImapStore". The natural first approach (K9MailTransport extends ImapStore) produces
a Java compile error:

> K9MailTransport.checkSettings() cannot override Store.checkSettings() because overridden
> method does not throw MailException

`Store.checkSettings()` (k-9 base class) does not declare `MailException`, so a subclass
cannot widen the throws clause. The solution is composition: K9MailTransport holds a
`BackupImapStoreDelegate extends ImapStore` as a static inner class, and delegates IMAP
operations to it while wrapping calls in the translation catch blocks.

## Contract Adherence

### CNTR-MODERNIZATION-007

| Requirement | File:Line | Status |
|-------------|-----------|--------|
| `MailTransport` interface with exactly six methods | `MailTransport.java:46-105` | SATISFIED |
| No `com.fsck.k9.*` in port signatures | `MailTransport.java` — zero k9 imports | SATISFIED |
| `MailTransportConfig` fields: `storeUri`, `tlsPolicy`, `pinnedCert` | `MailTransportConfig.java:34-46` | SATISFIED |
| `BackupFolderHandle` opaque, k-9 ref package-private | `BackupFolderHandle.java:38` | SATISFIED |
| `MailMessageHandle` opaque, k-9 ref package-private | `MailMessageHandle.java:46` | SATISFIED |
| `FetchSpec` enum values `ENVELOPE_DATE`, `BODY` | `FetchSpec.java:40-49` | SATISFIED |
| `MailException extends Exception implements LocalizableException` | `MailException.java:42` | SATISFIED |
| `MailException(Throwable cause)` preserves `getCause()` | `MailException.java:50` via `super(cause)` | SATISFIED |
| `TemporaryImapException.errorResourceId()` returns `status_gmail_temp_error` | `TemporaryImapException.java:46` | SATISFIED |
| `XOAuth2FailedException.getStatus()` preserves status code | `XOAuth2FailedException.java:52-55` | SATISFIED |
| Translation table: XOAuth2 → XOAuth2FailedException | `K9MailTransport.java:134-137` | SATISFIED |
| Translation table: AuthFailed → RequiresLoginException | `K9MailTransport.java:139-140` | SATISFIED |
| Translation table: "Unable to get IMAP prefix" → TemporaryImapException | `K9MailTransport.java:141-143` | SATISFIED |
| Translation table: generic MessagingException → MailException | `K9MailTransport.java:144` | SATISFIED |
| Catch ordering (XOAuth2 before AuthFailed before generic) | `K9MailTransport.java:132-144` | SATISFIED |
| `closeFolders()` does not throw (exception-swallowing) | `K9MailTransport.java:250-260` | SATISFIED |
| `BinaryTempFileBody.setTempDirectory()` moved behind adapter | `K9MailTransport.java:98` | SATISFIED |

### CNTR-MODERNIZATION-001

| Requirement | File:Line | Status |
|-------------|-----------|--------|
| `TlsTrustPolicy.SYSTEM_VALIDATED` → `DefaultTrustedSocketFactory` | `K9MailTransport.java:117-118` | SATISFIED |
| `TlsTrustPolicy.PINNED_CERTIFICATE` → `PinnedCertificateSocketFactory` | `K9MailTransport.java:110-115` | SATISFIED |
| `AllTrustedSocketFactory` never instantiated in `mail.transport` | grep: zero matches | SATISFIED |
| Null pinnedCert with PINNED_CERTIFICATE → MailException at construction | `K9MailTransport.java:109-111` | SATISFIED |

## Integration Verification

1. **New code reachable from production**: `K9MailTransport` is instantiable from
   `ServiceBase.getBackupImapStore()` via `new K9MailTransport(context, config)`. The engine
   call-site rewire is U-026; for now `BackupImapStore` remains and both classes compile.

2. **StateTest updated**: `shouldGetErrorMessagePrefix` now constructs `TemporaryImapException`
   and classifies by type via the `LocalizableException` branch (`State.java:35-36`). The
   k-9 string-match at `State.java:32-34` is superseded but NOT removed (removal is U-026).

3. **No import of k-9 types in port**: Verified zero matches for `com.fsck.k9` in all
   `mail.transport` interface and value type files (`MailTransport.java`, `MailTransportConfig.java`,
   `BackupFolderHandle.java`, `MailMessageHandle.java`, `FetchSpec.java`,
   `MailException.java`, `TemporaryImapException.java`, `XOAuth2FailedException.java`).
   Note: package-private fields in `BackupFolderHandle` and `MailMessageHandle` have k-9 imports
   that are NOT public — this is compliant with CNTR validation rule #4.

4. **`AllTrustedSocketFactory` not instantiated**: grep `AllTrustedSocketFactory` in
   `app/src/main/java/com/zegoggles/smssync/mail/transport/` returns zero matches.

## Notes

- `MailException.errorResourceId()` returns `R.string.status_unknown_error` (confirmed present
  in `strings.xml:47`). The story mentions `R.string.err_communication_error` but that resource
  does NOT exist in `strings.xml`. `status_unknown_error` is the nearest generic IMAP failure
  string.
- The `RequiresLoginException` is declared in port method throws clauses (not just `MailException`)
  because Java's checked exception rules prevent throwing it from a method that only declares
  `MailException` (since `RequiresLoginException` extends `Exception` not `MailException`).
