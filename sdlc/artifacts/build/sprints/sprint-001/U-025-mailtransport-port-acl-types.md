---
type: story
status: done
sprint: '000001'
artifact_type: user-story
priority: medium
complexity: high
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-009
design_docs:
  - DES-MODERNIZATION-009
integration_contracts:
  - CNTR-MODERNIZATION-007
  - CNTR-MODERNIZATION-001
dependencies:
  - U-007
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-025
title: Define MailTransport port, app-owned ACL types, and reshape BackupImapStore into K9MailTransport adapter
pipeline: ''
domain: modernization
requirement_source: authored
updated_at: '2026-06-03T07:29:48.214Z'
resolution: done
---

# U-025: Define MailTransport Port, App-Owned ACL Types, and Reshape BackupImapStore into K9MailTransport Adapter

## Story

As a developer maintaining the SMS Backup+ codebase,
I want `MailTransport` (an app-owned interface), its supporting value types (`MailTransportConfig`, `BackupFolderHandle`, `MailMessageHandle`, `FetchSpec`), and its exception hierarchy (`MailException`, `TemporaryImapException`, `XOAuth2FailedException`) defined under `mail.transport`, and `BackupImapStore` reshaped — verbatim, without IMAP logic changes — into `K9MailTransport` as the sole class permitted to import `com.fsck.k9.mail.*` transport types, with `MailTransportConfig.tlsPolicy` (`TlsTrustPolicy`) mapped to the validated `TrustedSocketFactory` from CNTR-MODERNIZATION-001 inside the adapter only,
so that the engine (`service.*`) can subsequently be wired to the app-owned port (U-026) without importing any `com.fsck.k9.*` type, closing the vendor-type leakage that lets a third-party library's internal exception text shape the domain.

## Acceptance Criteria

- [ ] **AC-1 — `MailTransport` interface exists with all six operations, app-owned types only**

  Given the new file `app/src/main/java/com/zegoggles/smssync/mail/transport/MailTransport.java`,
  when a developer opens it,
  then it declares exactly the following six methods in the `com.zegoggles.smssync.mail.transport` package, and no method signature or thrown exception names any `com.fsck.k9.*` type:
  `void checkSettings() throws MailException`;
  `BackupFolderHandle openFolder(DataType type, DataTypePreferences prefs) throws MailException`;
  `void appendMessages(BackupFolderHandle folder, ConversionResult result) throws MailException`;
  `List<MailMessageHandle> getMessages(BackupFolderHandle folder, int max, boolean flagged, Date since) throws MailException`;
  `void fetch(BackupFolderHandle folder, List<MailMessageHandle> handles, FetchSpec profile) throws MailException`;
  `void closeFolders()`.
  A compile-time check (`./gradlew :app:compileDebugJavaWithJavac`) passes with zero errors referencing this interface.

- [ ] **AC-2 — App-owned value types are defined with no k-9 type in any public signature**

  Given the four new files under `com.zegoggles.smssync.mail.transport`:
  `MailTransportConfig.java` (fields `String storeUri`, `TlsTrustPolicy tlsPolicy`, `X509Certificate pinnedCert` — nullable unless `PINNED_CERTIFICATE`),
  `BackupFolderHandle.java` (opaque; no public accessor exposes a k-9 type),
  `MailMessageHandle.java` (opaque; no public accessor exposes a k-9 type; carries at minimum a `String uid` readable by the adapter),
  `FetchSpec.java` (enum; values `ENVELOPE_DATE`, `BODY`),
  when a developer reads the public API of each class,
  then no field, constructor parameter, method parameter, or method return type in any public member names a `com.fsck.k9.*` type; the k-9-typed backing reference inside `BackupFolderHandle` and `MailMessageHandle` is `package-private` to `mail.transport` (not exposed via any getter); and `./gradlew :app:compileDebugJavaWithJavac` passes with zero errors.

- [ ] **AC-3 — Exception hierarchy is defined correctly: `MailException` extends Exception and implements `LocalizableException`; `TemporaryImapException` and `XOAuth2FailedException` extend `MailException`; `XOAuth2FailedException` exposes `getStatus()`**

  Given the three new exception files:
  `MailException.java`: `public class MailException extends Exception implements LocalizableException` with `@Override public int errorResourceId()` returning `R.string.err_communication_error` (or the nearest existing generic-IMAP-failure string — confirm at implementation; document the resource id chosen); constructor `MailException(Throwable cause)` so that the original k-9 throwable is preserved as `getCause()`.
  `TemporaryImapException.java`: `public class TemporaryImapException extends MailException` with `@Override public int errorResourceId()` returning `R.string.status_gmail_temp_error`; constructor `TemporaryImapException(Throwable cause)`.
  `XOAuth2FailedException.java`: `public class XOAuth2FailedException extends MailException` with `public int getStatus()` returning the HTTP status code preserved from the underlying k-9 `XOAuth2AuthenticationFailedException`; constructor `XOAuth2FailedException(int status, Throwable cause)` storing `status` and passing `cause` to `super`.
  when a developer reads each file,
  then the pattern `extends Exception implements LocalizableException` is used for `MailException` exactly as `RequiresLoginException` uses it at `service/exception/RequiresLoginException.java:5`; no exception class extends a k-9 type; and a unit test `MailExceptionTest` asserts: (a) `new MailException(cause).getCause() == cause`; (b) `new TemporaryImapException(cause).errorResourceId() == R.string.status_gmail_temp_error`; (c) `new XOAuth2FailedException(400, cause).getStatus() == 400` and `getCause() == cause`.

- [ ] **AC-4 — `K9MailTransport` is the sole class that imports `com.fsck.k9.mail.*` transport types; it implements `MailTransport` and contains the IMAP logic from `BackupImapStore` verbatim**

  Given the new file `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java`,
  when a developer reads its import list,
  then it imports `com.fsck.k9.mail.*` types (specifically at minimum `ImapStore`, `ImapFolder`, `ImapMessage`, `FetchProfile`, `ImapSearcher`, `MessagingException`, `AuthenticationFailedException`, `XOAuth2AuthenticationFailedException`, `DefaultTrustedSocketFactory`, `TrustedSocketFactory`) and `K9MailTransport implements MailTransport` is declared.
  When a developer compares `K9MailTransport` against the original `BackupImapStore.java` logic:
  (a) the `BackupFolder extends ImapFolder` inner class (currently nested in `BackupImapStore`, lines 140–197) is preserved inside `K9MailTransport` with its `getMessages(int max, boolean flagged, Date since)` implementation, `buildSearchQuery(...)`, `ImapSearcher` anonymous class, `FetchProfile.Item.DATE` envelope-fetch, `MessageComparator`-based sort, `Collections.reverse(messages)`, and the `max > 0 && msgs.size() > max` capping logic — all byte-for-byte identical in behavior to the current source (no IMAP logic changes);
  (b) the `createAndOpenFolder(...)` private method including its `IllegalArgumentException → MessagingException` re-wrap (`BackupImapStore.java:129-133`) is preserved;
  (c) `closeFolders()` including the per-folder exception-swallowing loop (`BackupImapStore.java:80-84`) is preserved;
  (d) `getStoreUriForLogging()` credential-masking logic (`BackupImapStore.java:98-113`) is preserved;
  (e) `isValidUri(String)` and `isValidImapFolder(String)` static helpers (`BackupImapStore.java:210-227`) are preserved.
  Running `./gradlew :app:testDebugUnitTest --tests "*BackupImapStore*"` passes (all test cases assert unchanged behavior).

- [ ] **AC-5 — `K9MailTransport` maps `MailTransportConfig.tlsPolicy` to a `TrustedSocketFactory` inside the adapter and never exposes the k-9 factory type across the port**

  Given `K9MailTransport`'s constructor (or factory method) receiving a `MailTransportConfig`,
  when `MailTransportConfig.tlsPolicy` is `TlsTrustPolicy.SYSTEM_VALIDATED`,
  then the adapter constructs `new DefaultTrustedSocketFactory(context)` and passes it to the k-9 `ImapStore` super-constructor slot `super(new BackupStoreConfig(config.storeUri), socketFactory, connectivityManager)` — matching the non-trust-all arm of the deleted `BackupImapStore.java:61` ternary.
  When `MailTransportConfig.tlsPolicy` is `TlsTrustPolicy.PINNED_CERTIFICATE` and `config.pinnedCert` is non-null,
  then the adapter constructs `new PinnedCertificateSocketFactory(context, host, config.pinnedCert)` and passes it to the same super-constructor slot.
  In both cases, the `TrustedSocketFactory` type never appears in any `MailTransport` method signature, any `MailTransportConfig` field, or any other `mail.transport` public API.
  A unit test (re-homed from `BackupImapStoreTest`) asserts that for a `MailTransportConfig` with `tlsPolicy = SYSTEM_VALIDATED` the adapter's `getTrustedSocketFactory()` package-private test accessor returns an instance of `DefaultTrustedSocketFactory`; and for `tlsPolicy = PINNED_CERTIFICATE` it returns an instance of `PinnedCertificateSocketFactory`. `AllTrustedSocketFactory` is never instantiated (grep `AllTrustedSocketFactory` within `mail/transport/` returns zero matches).

- [ ] **AC-6 — The complete k-9-throwable-to-app-exception translation mapping is implemented at every `MailTransport` operation boundary**

  Given each overridden method in `K9MailTransport` that calls k-9 APIs (`checkSettings`, `openFolder`, `appendMessages`, `getMessages`, `fetch`),
  when a k-9 `XOAuth2AuthenticationFailedException` is thrown by the k-9 call,
  then the adapter catches it and throws `new XOAuth2FailedException(e.getStatus(), e)` before returning to any caller.
  When a k-9 `AuthenticationFailedException` is thrown,
  then the adapter catches it and throws `new RequiresLoginException()` (using the existing Preserved-Core type at `service/exception/RequiresLoginException.java`).
  When a k-9 `MessagingException` whose `getMessage()` equals `"Unable to get IMAP prefix"` is thrown,
  then the adapter catches it and throws `new TemporaryImapException(e)`.
  When any other `MessagingException` or `com.fsck.k9.mail.*` throwable is thrown (including the `IllegalArgumentException` re-wrapped as `MessagingException` at `BackupImapStore.java:129-133`),
  then the adapter catches it and throws `new MailException(e)` (the backstop — no k-9 type escapes).
  The catch ordering within each method places `XOAuth2AuthenticationFailedException` before `AuthenticationFailedException` (subtype specificity) and `MessagingException` last in the hierarchy.
  A unit test `K9MailTransportTranslationTest` uses a fake k-9-layer stub (or constructor injection) to exercise each row of the translation table in isolation, asserting the correct app-owned exception type and that `getCause()` returns the original k-9 throwable in every case.

- [ ] **AC-7 — `MailMessageHandle` internal k-9 reference resolution works correctly for the Preserved-Core converter call path**

  Given a `MailMessageHandle` returned by `K9MailTransport.getMessages(...)`,
  when the adapter's own `fetch(folder, handles, profile)` method resolves each handle back to a k-9 `ImapMessage` for the actual IMAP `fetch(msgs, fp, null)` call (the `ImapFolder.fetch` invocation at `BackupImapStore.java:169`),
  then the resolution produces the correct k-9 `Message` object that was wrapped when `getMessages` was called; the k-9 `Message` reference is never exposed outside `mail.transport`; and `fetch` completes without `NullPointerException` or `ClassCastException` for a list of handles produced by a prior `getMessages` call on the same adapter instance.
  A unit test `K9MailTransportHandleResolutionTest` creates a `K9MailTransport` with a stubbed k-9 `ImapFolder`, calls `getMessages` to obtain handles, then calls `fetch` with those handles under `FetchSpec.BODY`, and asserts that the k-9 `ImapFolder.fetch(...)` was called with the correct `ImapMessage` list and `FetchProfile.Item.BODY`.

- [ ] **AC-8 — `StateTest.shouldGetErrorMessagePrefix` is updated to use `TemporaryImapException` and still passes**

  Given `app/src/test/java/com/zegoggles/smssync/service/state/StateTest.java` method `shouldGetErrorMessagePrefix` (currently at lines 62–67, which constructs `new MessagingException("Unable to get IMAP prefix")`),
  when the developer replaces that construction with `new TemporaryImapException(null)` (or `new TemporaryImapException(new MessagingException("Unable to get IMAP prefix"))` — either is correct as long as `TemporaryImapException.errorResourceId()` returns `R.string.status_gmail_temp_error`),
  then the test still asserts `state.getErrorMessage(resources).equals("Temporary IMAP error, try again later.")` (the same string resource) and passes under `./gradlew :app:testDebugUnitTest --tests "*StateTest*"`.
  This verifies that `State.getErrorMessage(Resources)` now classifies the temporary-error condition by **type** (via the existing `LocalizableException` branch at `State.java:35-36`) rather than by the k-9 magic string.

- [ ] **AC-9 — `BackupImapStore.java` is deleted; no compile-time reference to it remains in the codebase after U-025 (adapter scope only — engine rewire is U-026)**

  Given that `K9MailTransport` contains all logic from `BackupImapStore` and that U-025 scope is the adapter definition (not the engine call-site rewire),
  when the developer deletes `app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java`,
  then any pre-existing reference to `BackupImapStore` in `mail.*` test files that were testing the adapter logic (e.g., `BackupImapStoreTest`) is updated to reference `K9MailTransport` and its test seam `getTrustedSocketFactory()` instead; the engine files (`BackupTask.java`, `RestoreTask.java`, `ServiceBase.java`, `SmsBackupService.java`, `SmsRestoreService.java`) that still reference `BackupImapStore` will fail to compile — this is the **expected, accepted state** at the end of U-025, because U-026 completes the engine-side rewire; the U-025 PR is merged with the understanding that U-026 must follow without gap before CI is considered green for the combined changeset.
  Alternatively, if project policy requires each story to leave `master` green independently: the developer keeps a temporary `BackupImapStore` stub (a class that delegates entirely to `K9MailTransport`) only for the duration of this story, with a `@Deprecated` annotation and an `// TODO: remove in U-026` comment, and removes it in U-026.

- [ ] **AC-10 — `./gradlew :app:testDebugUnitTest` is green for all pre-existing test classes that are in scope for this story**

  Given the test classes that test adapter behavior moved to `K9MailTransport`:
  `BackupImapStoreTest.java` (re-homed to test `K9MailTransport`; all existing test methods pass with updated references),
  `StateTest.java` (updated per AC-8; all test methods including the existing `shouldGetErrorMessage`, `shouldGetErrorMessageRootCause`, `shouldGetErrorMessageRequiresWifi`, and `shouldGetErrorMessagePrefix` pass),
  `MailExceptionTest.java` (new, created per AC-3),
  `K9MailTransportTranslationTest.java` (new, created per AC-6),
  `K9MailTransportHandleResolutionTest.java` (new, created per AC-7),
  when `./gradlew :app:testDebugUnitTest` is executed,
  then all of the above test classes compile and pass, and no test class in `MessageConverterTest`, `MessageGeneratorTest`, `MmsSupportTest`, or other Preserved-Core converter test suites regresses (those files are untouched by this story).

### Integration Criteria

- [ ] **IC-1 — `MailTransport` interface is in `com.zegoggles.smssync.mail.transport` package**

  Given the package structure under `app/src/main/java/com/zegoggles/smssync/mail/transport/`,
  when a developer lists the files,
  then `MailTransport.java`, `K9MailTransport.java`, `MailTransportConfig.java`, `BackupFolderHandle.java`, `MailMessageHandle.java`, `FetchSpec.java`, `MailException.java`, `TemporaryImapException.java`, and `XOAuth2FailedException.java` all exist in that directory.

- [ ] **IC-2 — `K9MailTransport` is instantiable from the existing `ServiceBase.getBackupImapStore()` construction seam without Hilt**

  Given that Hilt (MU-007) has not yet landed when this story ships,
  when `ServiceBase.getBackupImapStore()` (currently at `ServiceBase.java:99` method / `:104` construction) needs to construct the transport,
  then either: (a) it can directly construct a `K9MailTransport` using `new K9MailTransport(context, mailTransportConfig)` (hand-wired provider pattern), or (b) a temporary `BackupImapStore` stub (see AC-9 alternative) is in place so the existing `ServiceBase` code compiles unchanged.
  The port and adapter compile and are fully functional regardless of whether Hilt is present (per DES-009 §Hilt: "the port and adapter do not depend on Hilt existing").

- [ ] **IC-3 — `BackupImapStoreTest` test methods for trust-factory assertions are re-homed to `K9MailTransport` and reference `TlsTrustPolicy` canonical constants**

  Given the trust-factory test methods in `BackupImapStoreTest` (the `testShouldCreateCorrectTrustFactoryFor*` family),
  when they are migrated to test `K9MailTransport`,
  then they reference `TlsTrustPolicy.SYSTEM_VALIDATED` and `TlsTrustPolicy.PINNED_CERTIFICATE` (the canonical constants from CNTR-MODERNIZATION-001, not any `TlsPolicy.VALIDATED` / `TlsPolicy.PINNED` alias), use `MailTransportConfig` as the input, and assert the correct `TrustedSocketFactory` subclass via the `getTrustedSocketFactory()` package-private seam re-homed onto `K9MailTransport`.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java` | `extends ImapStore`; holds the only IMAP connection logic; imports `com.fsck.k9.mail.*` throughout; directly instantiated by `ServiceBase.getBackupImapStore()` | Reshaped verbatim into `K9MailTransport`; this file is deleted (or replaced with a temporary delegation stub — see AC-9) |
| `app/src/main/java/com/zegoggles/smssync/mail/transport/` (new directory) | Does not exist | Created; receives `MailTransport.java`, `K9MailTransport.java`, `MailTransportConfig.java`, `BackupFolderHandle.java`, `MailMessageHandle.java`, `FetchSpec.java`, `MailException.java`, `TemporaryImapException.java`, `XOAuth2FailedException.java` |
| `app/src/test/java/com/zegoggles/smssync/service/state/StateTest.java` | `shouldGetErrorMessagePrefix` (lines 62-67) constructs `new MessagingException("Unable to get IMAP prefix")` | Replace with `new TemporaryImapException(...)` so the test classifies by type not by k-9 string |
| `app/src/test/java/com/zegoggles/smssync/mail/BackupImapStoreTest.java` | Tests `BackupImapStore` directly; `testShouldCreateCorrectTrustFactoryFor*` methods assert factory selection | Migrated to test `K9MailTransport` via `MailTransportConfig`; class renamed or moved to `mail/transport/K9MailTransportTest.java` |

## Existing Behavior to Preserve

- All IMAP protocol logic inside `BackupImapStore` and its nested `BackupFolder` class must be copied **verbatim** into `K9MailTransport`. This is a reshape (move + implement interface), not a rewrite. No IMAP/MIME logic changes. Specifically: the `getMessages` envelope-date sort, the `SENTSINCE` + `FLAGGED` + `DATATYPE` header search query construction, the `Collections.reverse(messages)` call, the `max > 0 && msgs.size() > max` capping with `FetchProfile.Item.DATE` sort, the `IllegalArgumentException → MessagingException` re-wrap in `createAndOpenFolder`, and the exception-swallowing `closeFolders` loop.
- `State.getDetailedErrorMessage(Resources)` reads `exception.getCause().toString()` (`State.java:45`) for the `"underlying="` suffix. Every `MailException` and subtype must preserve the original k-9 throwable as `getCause()` so this diagnostic output continues to work correctly.
- `isValidUri(String)` and `isValidImapFolder(String)` are static utility methods used by callers outside `BackupImapStore` (e.g., settings validation). They must remain accessible — either on `K9MailTransport` (as static methods) or moved to a separate `MailTransportValidator` utility class in `mail.transport` — with no behavior change.
- `getStoreUriForLogging()` credential-masking output must be identical in the re-homed implementation (the `parts[0] + ":" + parts[1].replaceAll(".", "X")` masking pattern).
- `BackupImapStoreTest` characterization tests that cover `isValidUri`, `isValidImapFolder`, and `getStoreUriForLogging` must continue to pass after re-homing.
- `StateTest` tests `shouldGetErrorMessage`, `shouldGetErrorMessageRootCause`, `shouldGetErrorMessageRequiresWifi`, and `shouldGetNotificationLabelLogin` must remain green without modification (they do not reference `MessagingException`).
- The Preserved-Core converter tests (`MessageConverterTest`, `MessageGeneratorTest`, and all tests in `mail/` that import `com.fsck.k9.mail.*` value types) must not be modified; this story does not touch the MIME converter layer.

## Verification Steps

1. **AC-1 (interface shape):** Open `app/src/main/java/com/zegoggles/smssync/mail/transport/MailTransport.java`. Confirm it declares exactly six methods. Use `grep -n "com.fsck.k9" app/src/main/java/com/zegoggles/smssync/mail/transport/MailTransport.java` and confirm zero matches.

2. **AC-2 (value types — no k-9 in public API):** For each of `MailTransportConfig`, `BackupFolderHandle`, `MailMessageHandle`, `FetchSpec`: run `grep -n "com.fsck.k9" <filepath>` and confirm zero matches in any public member declaration. Confirm `BackupFolderHandle` and `MailMessageHandle` fields holding k-9 references are `package-private` (no `public` modifier, not exposed via any getter).

3. **AC-3 (exception hierarchy):** Run `./gradlew :app:testDebugUnitTest --tests "*MailExceptionTest*"` from the repo root. Confirm three assertions pass: `getCause()` round-trip on `MailException`; `errorResourceId()` on `TemporaryImapException`; `getStatus()` and `getCause()` on `XOAuth2FailedException(400, cause)`. Confirm `grep "extends.*LocalizableException\|implements.*LocalizableException" app/src/main/java/com/zegoggles/smssync/mail/transport/MailException.java` returns the `implements LocalizableException` declaration.

4. **AC-4 (K9MailTransport reshapes BackupImapStore verbatim):** Run `./gradlew :app:testDebugUnitTest --tests "*BackupImapStore*"` (or the re-homed equivalent). All existing test methods pass. Open `K9MailTransport.java`; confirm the `BackupFolder extends ImapFolder` inner class is present with the original search query builder, envelope-date sort block, and `Collections.reverse` call. Confirm no IMAP-related line was added or removed from the original `BackupImapStore.java` logic.

5. **AC-5 (TLS factory mapping):** Run `./gradlew :app:testDebugUnitTest --tests "*K9MailTransport*"` (the re-homed trust-factory tests). Confirm: for `TlsTrustPolicy.SYSTEM_VALIDATED`, `getTrustedSocketFactory()` returns an instance of `DefaultTrustedSocketFactory`; for `TlsTrustPolicy.PINNED_CERTIFICATE`, it returns an instance of `PinnedCertificateSocketFactory`. Run `grep -rn "AllTrustedSocketFactory" app/src/main/java/com/zegoggles/smssync/mail/transport/` and confirm zero matches.

6. **AC-6 (exception translation):** Run `./gradlew :app:testDebugUnitTest --tests "*K9MailTransportTranslationTest*"`. Confirm five test cases pass: `XOAuth2AuthenticationFailedException` → `XOAuth2FailedException`; `AuthenticationFailedException` → `RequiresLoginException`; `MessagingException("Unable to get IMAP prefix")` → `TemporaryImapException`; generic `MessagingException` → `MailException`; re-wrapped `IllegalArgumentException` → `MailException`. In each case confirm `getCause()` returns the original throwable. Confirm `XOAuth2FailedException.getStatus()` equals the value from the original `XOAuth2AuthenticationFailedException`.

7. **AC-7 (handle resolution):** Run `./gradlew :app:testDebugUnitTest --tests "*K9MailTransportHandleResolutionTest*"`. Confirm the test asserts that after calling `getMessages(...)` and receiving `MailMessageHandle` instances, calling `fetch(folder, handles, FetchSpec.BODY)` invokes the k-9 `ImapFolder.fetch(messages, fp, null)` with the correct `ImapMessage` list and a `FetchProfile` containing `FetchProfile.Item.BODY`.

8. **AC-8 (StateTest updated):** Open `StateTest.java` method `shouldGetErrorMessagePrefix`. Confirm it no longer constructs `new MessagingException("Unable to get IMAP prefix")`. Run `./gradlew :app:testDebugUnitTest --tests "*StateTest.shouldGetErrorMessagePrefix*"` and confirm it passes. Run `grep -n "MessagingException" app/src/test/java/com/zegoggles/smssync/service/state/StateTest.java` and confirm zero matches (the `MessagingException` import is also removed from `StateTest.java`).

9. **AC-9 (BackupImapStore deleted or stubbed):** If the full-delete approach is taken, confirm `app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java` does not exist. If the temporary-stub approach is taken, confirm `BackupImapStore.java` contains only delegation to `K9MailTransport`, a `@Deprecated` annotation, and a `// TODO: remove in U-026` comment, with no IMAP logic of its own.

10. **AC-10 (full test suite green for in-scope tests):** Run `./gradlew :app:testDebugUnitTest` from the repo root. Confirm zero failures in `BackupImapStoreTest` (or its re-homed equivalent), `StateTest`, `MailExceptionTest`, `K9MailTransportTranslationTest`, `K9MailTransportHandleResolutionTest`. Confirm zero regressions in `MessageConverterTest` and `MessageGeneratorTest`.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java) | Create `mail/transport/` package with all nine new files; reshape `BackupImapStore` into `K9MailTransport`; update `StateTest`; re-home `BackupImapStoreTest`; author `MailExceptionTest`, `K9MailTransportTranslationTest`, `K9MailTransportHandleResolutionTest`; delete or stub `BackupImapStore.java` | Developer |

## Technical Context

- **This story is a reshape, not a rewrite.** All IMAP protocol logic currently in `BackupImapStore.java` and its `BackupFolder extends ImapFolder` inner class must be copied without modification into `K9MailTransport`. The only structural changes are: (1) the class now `implements MailTransport`; (2) the constructor receives `MailTransportConfig` and maps `TlsTrustPolicy` to `TrustedSocketFactory`; (3) each public method is wrapped in a translation try-catch block; (4) return types of operations that return k-9 types (`getFolder` → `ImapFolder`, `getMessages` → `List<ImapMessage>`) are replaced by the opaque handle types. The literal k-9 operation logic inside those methods is preserved verbatim.

- **`LocalizableException` is an interface, not a base class.** Verified at `service/exception/LocalizableException.java:3-5`: `public interface LocalizableException { int errorResourceId(); }`. The correct pattern — used by `RequiresLoginException.java:5` — is `extends Exception implements LocalizableException`. All three new exception classes (`MailException`, `TemporaryImapException`, `XOAuth2FailedException`) follow this pattern. `MailException` establishes the base; the subtypes use `extends MailException` (not re-implementing the interface).

- **`XOAuth2FailedException.getStatus()` is load-bearing.** `BackupTask.java:183` (and `RestoreTask.java:158`) branch on `e.getStatus() == 400` to decide whether to attempt a token refresh. If `getStatus()` is missing or returns zero, the XOAuth2 retry mechanism silently breaks. The value comes from `XOAuth2AuthenticationFailedException.getStatus()` on the k-9 side — preserve it exactly.

- **`MailException` must carry the k-9 cause for `getDetailedErrorMessage`.** `State.getDetailedErrorMessage(Resources)` at `State.java:45` reads `exception.getCause().toString()` to produce the `"underlying=..."` suffix in error messages. If `MailException(Throwable cause)` does not call `super(cause)` (i.e., does not chain the cause via `Exception(Throwable)` constructor), `getCause()` returns null and the diagnostic output truncates. Every adapter catch block must pass the original k-9 throwable: `throw new MailException(e)`, not `throw new MailException()`.

- **Exception catch ordering within each adapter method.** `XOAuth2AuthenticationFailedException` is a subtype of `AuthenticationFailedException` (in the k-9 library) — catch it first. `AuthenticationFailedException` is a subtype of `MessagingException` — catch it before the generic `MessagingException` catch. The magic-string check (`"Unable to get IMAP prefix"`) lives inside the `MessagingException` catch, checked before falling through to `MailException`. A catch block written in the wrong order silently demotes an `XOAuth2FailedException` to `RequiresLoginException`, breaking the `getStatus() == 400` retry path.

- **`TlsTrustPolicy` canonical names.** Per CNTR-MODERNIZATION-001 §Overview, the canonical enum constants are `SYSTEM_VALIDATED` and `PINNED_CERTIFICATE` — not `VALIDATED` / `PINNED`. The DES-009 document uses `TlsPolicy.VALIDATED` / `TlsPolicy.PINNED(cert)` as informal shorthand; this story must use the canonical names. Trust-factory test methods previously in `BackupImapStoreTest` that assert the now-deleted `AllTrustedSocketFactory` path are removed (that path was eliminated by U-008/DES-002, which is the U-007 dependency).

- **`MailTransportConfig.pinnedCert` is `null` for `SYSTEM_VALIDATED`.** The adapter's constructor must null-check before constructing `PinnedCertificateSocketFactory`. If `tlsPolicy == PINNED_CERTIFICATE` but `pinnedCert == null`, the adapter should throw `MailException` at construction time (not a `NullPointerException` later during TLS handshake).

- **`closeFolders()` must not throw.** The existing `BackupImapStore.closeFolders()` swallows per-folder exceptions with `Log.w(TAG, e)` (`BackupImapStore.java:80-84`). This is load-bearing: `BackupTask.java:299-301` and `RestoreTask.java:152-154` call `closeFolders()` in `finally` blocks where a thrown exception would discard the return value. Preserve the exception-swallowing exactly.

- **Handle resolution and thread safety.** `BackupFolderHandle` wraps the k-9 `BackupFolder` (the `ImapFolder` subclass). `MailMessageHandle` wraps the k-9 `ImapMessage`. The adapter maintains the handle-to-k-9-object mapping. The simplest correct implementation is a `Map<String, ImapMessage>` keyed by UID inside `K9MailTransport`, populated during `getMessages` and read during `fetch`. The adapter and its handles are per-backup-session objects (not singletons), so concurrent access from multiple sessions is not a concern at this scope.

- **`BinaryTempFileBody` MIME residual — this story's contribution.** `SmsRestoreService.java:10` imports `com.fsck.k9.mail.internet.BinaryTempFileBody` and calls `BinaryTempFileBody.setTempDirectory(getCacheDir())` in `onCreate`. Per DES-009 Context bucket 4 and CNTR-MODERNIZATION-007 §Notes, this call belongs behind the adapter (the temp-directory configuration is an adapter-internal staging concern, not a service-layer concern). In U-025 the developer moves this call into `K9MailTransport`'s initialization path (e.g., called from the constructor or a lazy-init method when `SmsRestoreService` creates/obtains the transport). The `SmsRestoreService.java:10` import is removed in U-026 when the service is rewired; for now the developer notes that the functional relocation (`BinaryTempFileBody.setTempDirectory(...)` called inside the adapter) is part of this story's scope, and the import removal from `SmsRestoreService` is gated on U-026.

- **Sequencing constraint (hard).** This story depends on U-007 (which depends on U-008 via `BackupImapStore` TLS validation being already established). DES-009 §Sequencing mandates MU-003 (the trust-all removal and socket-factory validation hardening) before MU-008 (this story). Attempting to author `K9MailTransport` before `AllTrustedSocketFactory` is deleted and the validated-TLS constructor shape is in place would result in re-wrapping a transport still capable of trust-all — a correctness defect. The `depends_on: U-007` in this story's frontmatter enforces that sequencing.

- **Hilt is NOT required by this story.** The adapter is implemented as a plain Java class. The Hilt binding (`MailTransport → K9MailTransport`) is authoring work for U-022/U-023/U-024. This story merely makes the adapter instantiable. Per DES-009 §Hilt: "the first `MailTransport` binding may be hand-wired (a factory/provider method on the existing construction seam, no behavior change), then folded into the Hilt graph when MU-007 lands."

- **`BackupImapStore` is internal to `mail.*` — no public API outside that package references it by name in the current codebase, except through the service layer which calls `getBackupImapStore()` returning the concrete type.** The service-layer rewire is U-026. This story is free to delete `BackupImapStore` or replace it with a stub; either approach is acceptable per AC-9.

## Supporting Documentation

- REQ-MODERNIZATION-009 §Acceptance Criteria AC-1, AC-2, AC-3, AC-5 — the requirements this story directly satisfies (AC-4 is the k-9 unpin, which is U-027)
- DES-MODERNIZATION-009 §MailTransport port — the complete operation table derived from verified call sites
- DES-MODERNIZATION-009 §k-9 adapter — the reshape specification
- DES-MODERNIZATION-009 §Exception translation — the ACL mapping table (the complete list)
- DES-MODERNIZATION-009 §Socket-factory injection — the DES-002 → DES-009 handoff description
- DES-MODERNIZATION-009 §Integration Design §Sequencing — the MU-003 → MU-008 hard ordering
- DES-MODERNIZATION-009 §Test integration — `BackupImapStoreTest` re-homing and `StateTest` update specification
- CNTR-MODERNIZATION-007 §Contract Definition — authoritative interface specification, value type shapes, exception hierarchy, and translation mapping
- CNTR-MODERNIZATION-007 §Error Handling — `getCause()` preservation requirement for `getDetailedErrorMessage`
- CNTR-MODERNIZATION-001 §Trust-policy enum — canonical `TlsTrustPolicy` constant names
- CNTR-MODERNIZATION-001 §Canonical-naming reconciliation — reconciles DES-009's `TlsPolicy` alias to `TlsTrustPolicy`

## Integration Contract References

- **CNTR-MODERNIZATION-007 §Interface: MailTransport** — the six-operation port this story implements; every operation signature, return type, and thrown exception must match exactly
- **CNTR-MODERNIZATION-007 §App-owned value types** — `MailTransportConfig`, `BackupFolderHandle`, `MailMessageHandle`, `FetchSpec` shapes
- **CNTR-MODERNIZATION-007 §App-owned exception hierarchy** — `MailException` / `TemporaryImapException` / `XOAuth2FailedException` (including the `extends Exception implements LocalizableException` pattern and the `getStatus()` requirement)
- **CNTR-MODERNIZATION-007 §Exception-translation mapping** — the exhaustive k-9-throwable → app-exception table; this story is the producer of the mapping
- **CNTR-MODERNIZATION-007 §Validation Rules #4** — opaque handle invariant (no k-9 type in any public accessor)
- **CNTR-MODERNIZATION-001 §Trust-policy enum** — `TlsTrustPolicy.SYSTEM_VALIDATED` / `PINNED_CERTIFICATE` canonical constants; `MailTransportConfig.tlsPolicy` consumes this
- **CNTR-MODERNIZATION-001 §Invariant #2 (never trust-all)** — `AllTrustedSocketFactory` must not be instantiated in `mail.transport`

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Not finalized. U-025 defines the port, adapter, and ACL types; U-026 rewires the engine call sites; the two stories must ship in sequence without an intervening green-master gap if the full-delete approach (AC-9 first option) is taken.
