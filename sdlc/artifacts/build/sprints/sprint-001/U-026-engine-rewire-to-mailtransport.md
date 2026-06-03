---
type: story
status: planned
sprint: "000001"
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
dependencies:
  - U-025
  - U-022
change_records: []
platforms: []
tags:
  - acl
  - k9-unpin
  - mail-transport
  - engine-rewire
  - anti-corruption-layer
gate_additions: []
id: U-026
title: 'Engine Rewire to MailTransport: wire BackupTask/RestoreTask/ServiceBase to the MailTransport port, remove all com.fsck.k9.* from service.* and State, pull BinaryTempFileBody behind the adapter, delete State.java:32-34 magic-string block'
pipeline: ''
domain: modernization
requirement_source: authored
---

# U-026: Engine Rewire to MailTransport — wire BackupTask/RestoreTask/ServiceBase to the MailTransport port, remove all com.fsck.k9.* from service.* and State, pull BinaryTempFileBody behind the adapter, delete State.java:32-34 magic-string block

## Story

As a maintainer of the SMS Backup+ codebase,
I want to rewire `BackupTask`, `RestoreTask`, and `ServiceBase` to receive and consume an injected `MailTransport` instead of constructing a `BackupImapStore` directly, replace all `com.fsck.k9.*` exception catches in the engine with app-owned types from the ACL hierarchy, move the `BinaryTempFileBody` body-staging call from `SmsRestoreService` behind the `K9MailTransport` adapter, and delete the `State.java:32-34` k-9 magic-string branch together with its k-9 imports — updating `State.isAuthException()` to branch on app-owned types and updating `StateTest` to construct `TemporaryImapException` instead of a k-9 `MessagingException`,
so that `grep -r "com.fsck.k9" app/src/main/java/com/zegoggles/smssync/service/` returns zero matches, the domain state machine no longer depends on a vendor library's internal exception text, and the k-9 dependency is fully confined to the `mail.transport.K9MailTransport` adapter and the Preserved-Core MIME converters.

## Acceptance Criteria

- [ ] AC-1: `ServiceBase.getBackupImapStore()` (method declaration at `service/ServiceBase.java:99`, construction at `:104`) is replaced by a `getMailTransport()` method (or equivalent injection seam) that returns a `MailTransport` (app-owned) and throws `MailException` instead of `MessagingException`. The `com.fsck.k9.mail.MessagingException` import at `ServiceBase.java:36` and the `com.zegoggles.smssync.mail.BackupImapStore` import at `:40` are both removed. The new seam constructs (or receives) a `K9MailTransport` configured via a `MailTransportConfig` built from `AuthPreferences.getStoreUri()` and a `TlsTrustPolicy` value — the `isTrustAllCertificates()` boolean is not passed; the trust-all path was removed by DES-MODERNIZATION-002 (U-025 dependency). If MU-007 (Hilt) has not yet landed, the seam may be a hand-wired provider method; the port and adapter do not require Hilt to exist (DES-MODERNIZATION-009 §Hilt binding caveat).

- [ ] AC-2: `BackupTask.java` is rewired: (a) the `config.imapStore` field reference at `:155` is replaced by a `MailTransport` obtained from `service.getMailTransport()` (or the equivalent injection point); (b) `getFolder(...)` calls at `:67` and `:70` are replaced by `transport.openFolder(type, prefs)` returning a `BackupFolderHandle`; (c) the `appendMessages(...)` call at `:135` is replaced by `transport.appendMessages(folder, result)` where `result` is the existing `ConversionResult`; (d) `store.closeFolders()` at `:99` (approximately, within the `finally` block) is replaced by `transport.closeFolders()`; (e) the `retryWithStore(service.getBackupImapStore())` call inside `handleAuthError` is replaced by `retryWithTransport(service.getMailTransport())` (or equivalent); (f) the four k-9 imports — `com.fsck.k9.mail.AuthenticationFailedException`, `com.fsck.k9.mail.Message`, `com.fsck.k9.mail.MessagingException`, `com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException` — and the `com.zegoggles.smssync.mail.BackupImapStore` import are all removed from `BackupTask.java`.

- [ ] AC-3: `RestoreTask.java` is rewired: (a) the `config.imapStore` (`BackupImapStore`) reference at `:98` is replaced by a `MailTransport` obtained from `service.getMailTransport()`; (b) `imapStore.getFolder(SMS, ...)` and `imapStore.getFolder(CALLLOG, ...)` calls at `:110` and `:113` are replaced by `transport.openFolder(SMS, prefs)` and `transport.openFolder(CALLLOG, prefs)` respectively, returning `BackupFolderHandle`s; (c) `folder.getMessages(max, flagged, null)` calls are replaced by `transport.getMessages(folder, max, flagged, null)` returning `List<MailMessageHandle>`; (d) per-message body fetch (`message.getFolder().fetch(...)` pattern at `:225`, using `FetchProfile` from `:14`) is replaced by `transport.fetch(folder, Collections.singletonList(handle), FetchSpec.BODY)`; (e) `imapStore.closeFolders()` at `:153` is replaced by `transport.closeFolders()`; (f) all k-9 imports — `com.fsck.k9.mail.AuthenticationFailedException`, `com.fsck.k9.mail.FetchProfile`, `com.fsck.k9.mail.Message`, `com.fsck.k9.mail.MessagingException`, `com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException` — and the `BackupImapStore` import are removed from `RestoreTask.java`.

- [ ] AC-4: Engine `catch` blocks are rewritten to app-owned types throughout `BackupTask` and `RestoreTask`: `catch (XOAuth2AuthenticationFailedException e)` becomes `catch (XOAuth2FailedException e)` (with `e.getStatus() == 400` token-refresh logic preserved identically); `catch (AuthenticationFailedException e)` is removed (the ACL maps this to `RequiresLoginException`, which is already covered by the `MailException` base or a dedicated catch — per CNTR-MODERNIZATION-007 exception mapping); `catch (MessagingException e)` in both tasks and inside `handleAuthError` (the `catch (MessagingException ignored)` swallow at `BackupTask.java:193`) becomes `catch (MailException e)` or `catch (MailException ignored)` respectively. No `com.fsck.k9.*` exception type appears in any `catch` clause in `service.*`.

- [ ] AC-5: `SmsBackupService.java:28` (`com.fsck.k9.mail.MessagingException` import) is removed. Every use of `MessagingException` in `SmsBackupService.java` is replaced with `MailException` (app-owned). The file compiles with zero `com.fsck.k9.*` imports.

- [ ] AC-6: `SmsRestoreService.java:9` (`com.fsck.k9.mail.MessagingException` import) and `:10` (`com.fsck.k9.mail.internet.BinaryTempFileBody` import) are both removed. The `BinaryTempFileBody.setTempDirectory(getCacheDir())` call in `SmsRestoreService.onCreate()` (at approximately `:51`) is moved behind the `K9MailTransport` adapter: the adapter's construction or initialization receives the cache directory (e.g. via a `File cacheDir` parameter on `MailTransportConfig` or a dedicated initializer, consistent with CNTR-MODERNIZATION-007 §Notes on the `BinaryTempFileBody` residual) and calls `BinaryTempFileBody.setTempDirectory(...)` internally — the `BinaryTempFileBody` type never appears in `service.*` source. Every use of `MessagingException` in `SmsRestoreService.java` is replaced with `MailException`. The file compiles with zero `com.fsck.k9.*` imports.

- [ ] AC-7: `service/state/State.java` lines 32–34 — the entire `if (exception instanceof MessagingException && "Unable to get IMAP prefix".equals(exception.getMessage()))` block including its return statement — are deleted. The three k-9 imports at `State.java:6,7,8` (`com.fsck.k9.mail.AuthenticationFailedException`, `com.fsck.k9.mail.MessagingException`, `com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException`) are removed. The `getErrorMessage` method retains the `else if (exception instanceof LocalizableException)` branch at lines 35–36 unchanged; `TemporaryImapException` (from U-025, `extends MailException implements LocalizableException`) feeds that branch type-safely with `errorResourceId()` returning `R.string.status_gmail_temp_error` — same localized string as before. No behavior change on the happy path or any error path other than the deletion of the dead string-match branch.

- [ ] AC-8: `State.isAuthException()` at `State.java:78-81` is rewritten: `exception instanceof XOAuth2AuthenticationFailedException` is replaced by `exception instanceof XOAuth2FailedException` (app-owned, from U-025); `exception instanceof AuthenticationFailedException` is replaced by `exception instanceof RequiresLoginException` (already present in the `||` chain; the k-9 `AuthenticationFailedException` clause is removed). The `RequiresLoginException` import already present at `State.java:14` is retained. The `com.fsck.k9.*` imports at lines 6–8 are absent (covered by AC-7). `State.isAuthException()` returns `true` for the same runtime exception instances as before — only the type names change.

- [ ] AC-9: `StateTest.shouldGetErrorMessagePrefix()` (`StateTest.java:62-67`) is updated: the constructor call `new MessagingException("Unable to get IMAP prefix")` is replaced by `new TemporaryImapException(/* cause= */ null)` (or equivalent constructor consistent with U-025's `TemporaryImapException` definition); the assertion `assertThat(state.getErrorMessage(resources)).isEqualTo("Temporary IMAP error, try again later.")` remains identical. The `com.fsck.k9.mail.MessagingException` import in `StateTest.java` is removed. All other `StateTest` test methods pass unmodified.

- [ ] AC-10: After all changes, `grep -r "com.fsck.k9" app/src/main/java/com/zegoggles/smssync/service/` returns zero matches. This grep scope covers `BackupTask.java`, `RestoreTask.java`, `ServiceBase.java`, `SmsBackupService.java`, `SmsRestoreService.java`, and `service/state/State.java` — the complete verified in-scope surface from DES-MODERNIZATION-009 Context bucket 1 and bucket 4. The two explicitly carved-out files (`App.java` with `K9MailLib` bootstrap and `preferences/AuthPreferences.java` with `AuthType`) are outside `service/` and are not asserted by this grep.

- [ ] AC-11: `./gradlew test` exits with code 0. Specifically: `StateTest.shouldGetErrorMessagePrefix` passes with the updated `TemporaryImapException` constructor; `BackupImapStoreTest` (re-homed through the adapter per DES-009 §Test integration) and all `MessageConverterTest`/`MessageGeneratorTest` converter tests pass unmodified (converter source not touched — bounded residual per ADR-009-B); no previously-passing test is broken.

### Integration Criteria

- [ ] IC-1: `ServiceBase.getMailTransport()` (or equivalent seam) is the sole construction point for `MailTransport` in the engine; `BackupTask.fetchAndBackupItems(BackupConfig)` and `RestoreTask.restore(RestoreConfig)` obtain their `MailTransport` through this seam — never by constructing a `BackupImapStore` or `K9MailTransport` directly.
- [ ] IC-2: `K9MailTransport` (the adapter, introduced by U-025) is the concrete class returned by the `getMailTransport()` seam and is the only class in the production codebase (outside `mail.transport.*`) that imports `com.fsck.k9.mail.store.imap.*` or `com.fsck.k9.mail.MessagingException`.
- [ ] IC-3: Every `BackupConfig` and `RestoreConfig` field or parameter that previously carried a `BackupImapStore` is updated to carry either a `MailTransport` or nothing (if the transport is obtained from the service directly). No public method signature outside `service.*` is broken.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` | Calls `config.imapStore` / `getFolder` / `appendMessages` / `closeFolders` on a `BackupImapStore`; catches `XOAuth2AuthenticationFailedException`, `AuthenticationFailedException`, `MessagingException`; imports four k-9 types | Rewire all calls to injected `MailTransport`; replace all k-9 catch clauses with app-owned types; remove four k-9 imports and the `BackupImapStore` import |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java` | Calls `config.imapStore.getFolder(...)`, `folder.getMessages(...)`, `message.getFolder().fetch(...)`, `imapStore.closeFolders()`; catches three k-9 exceptions; uses `FetchProfile` directly | Rewire all calls to injected `MailTransport`; replace k-9 `catch` clauses; replace `FetchProfile.Item` with `FetchSpec`; remove all k-9 imports |
| `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` | `getBackupImapStore()` at `:99` declares `throws MessagingException` and constructs `new BackupImapStore(ctx, uri, trustAll)` at `:104`; imports `MessagingException` at `:36` and `BackupImapStore` at `:40` | Replace with `getMailTransport()` returning `MailTransport`, throws `MailException`; assemble `MailTransportConfig`; remove k-9 imports |
| `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` | Imports `com.fsck.k9.mail.MessagingException` at `:28`; uses it in catch/throws declarations | Replace `MessagingException` references with `MailException`; remove import |
| `app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` | Imports `com.fsck.k9.mail.MessagingException` at `:9` and `com.fsck.k9.mail.internet.BinaryTempFileBody` at `:10`; calls `BinaryTempFileBody.setTempDirectory(getCacheDir())` in `onCreate()` at ~`:51` | Remove both k-9 imports; move `BinaryTempFileBody` initialization behind the `K9MailTransport` adapter; replace `MessagingException` with `MailException` |
| `app/src/main/java/com/zegoggles/smssync/service/state/State.java` | Lines 32–34: `if (exception instanceof MessagingException && "Unable to get IMAP prefix".equals(...))` branch; imports k-9 `AuthenticationFailedException`, `MessagingException`, `XOAuth2AuthenticationFailedException` at lines 6–8; `isAuthException()` at `:78-81` names k-9 types | Delete lines 32–34 magic-string block; remove lines 6–8 k-9 imports; rewrite `isAuthException()` to use app-owned `XOAuth2FailedException` and keep `RequiresLoginException` |
| `app/src/test/java/com/zegoggles/smssync/service/state/StateTest.java` | `shouldGetErrorMessagePrefix()` at `:62-67` constructs `new MessagingException("Unable to get IMAP prefix")`; imports `com.fsck.k9.mail.MessagingException` | Replace with `new TemporaryImapException(null)`; remove k-9 import; assertion text unchanged |

## Existing Behavior to Preserve

- Backup writes the same IMAP messages in the same sequence; the `ConversionResult` passed to `appendMessages` is unchanged — the adapter unwraps the k-9 `Message` list from it inside `mail.*`, invisible to the engine.
- Restore fetches messages from the same IMAP folders in the same sequence; the per-message body fetch (`FetchSpec.BODY`) produces the same k-9 `Message` bodies inside the adapter for the Preserved-Core converter call — `MessageConverter.importMessage(...)` behavior is unchanged.
- `BackupTask.handleAuthError` token-refresh retry loop (status `== 400`) fires under the same conditions: `XOAuth2FailedException.getStatus()` returns the same HTTP status code as the former k-9 `XOAuth2AuthenticationFailedException.getStatus()` per CNTR-MODERNIZATION-007 §exception hierarchy.
- `State.getErrorMessage(resources)` returns `"Temporary IMAP error, try again later."` for the same runtime condition (IMAP prefix failure) — now via the `LocalizableException` branch fed by `TemporaryImapException` rather than via the deleted string-match branch.
- `State.isAuthException()` returns `true` for OAuth2 failures and login failures — same runtime behavior, new type names.
- `State.getDetailedErrorMessage(resources)` continues to append `", underlying=..."` by reading `exception.getCause().toString()` — all app-owned exception types in the ACL hierarchy carry the original k-9 throwable as `getCause()` per CNTR-MODERNIZATION-007 §Error Handling.
- `closeFolders()` in both tasks' `finally` blocks continues to swallow per-folder close errors — `MailTransport.closeFolders()` does not throw (mirroring the existing `BackupImapStore.closeFolders()` behavior at `BackupImapStore.java:80-84`).
- The `BinaryTempFileBody.setTempDirectory(getCacheDir())` initialization continues to happen before the first restore body fetch — it is moved to the adapter's construction or an explicit `initialize(cacheDir)` call made from `SmsRestoreService.onCreate()` on the adapter object, not omitted.
- `BackupConfig` and `RestoreConfig` data classes are modified minimally: only the field type carrying the store reference changes (from `BackupImapStore` to `MailTransport`). All other fields, constructors, and the `retryWithStore(...)` / `retryWithTransport(...)` builder pattern preserve their semantics.
- `BackupImapStoreTest` trust-factory assertions (re-homed by this story onto `K9MailTransport`'s `MailTransportConfig`-to-factory mapping) assert the same validated/pinned factory selection as before DES-MODERNIZATION-002 removed the trust-all arm — they must not regress.
- No `MessageConverter`, `MessageGenerator`, `MmsSupport`, `Attachment`, `HeaderGenerator`, `Headers`, `ConversionResult`, or `PersonRecord` source is touched. The converter residual in `mail.*` is a documented bounded residual (ADR-009-B) and is out of scope.

## Verification Steps

1. **AC-10 — engine grep returns zero:** From the repo root run `grep -r "com.fsck.k9" app/src/main/java/com/zegoggles/smssync/service/`. Confirm the command produces no output (exit code 1 or empty stdout). This is the definitive fitness function for AC-2 / REQ-MODERNIZATION-009 scoped per ADR-009-B.

2. **AC-7 — State.java magic-string gone:** Run `grep -rn "Unable to get IMAP prefix" app/src/main`. Confirm zero matches. Open `service/state/State.java` and confirm lines 32–34 contain the `else if (exception instanceof LocalizableException)` branch (formerly at lines 35–36, now shifted up), not a `MessagingException instanceof` test. Confirm imports at lines 6–8 no longer name any `com.fsck.k9.*` type.

3. **AC-8 — State.isAuthException() uses app-owned types:** Open `service/state/State.java` and read `isAuthException()`. Confirm it references `XOAuth2FailedException` and `RequiresLoginException`, not `XOAuth2AuthenticationFailedException` or `AuthenticationFailedException`. Confirm no `com.fsck.k9` string appears anywhere in the file.

4. **AC-9 — StateTest updated:** Open `app/src/test/java/com/zegoggles/smssync/service/state/StateTest.java`. Confirm `shouldGetErrorMessagePrefix()` constructs `new TemporaryImapException(...)` (not `new MessagingException(...)`). Confirm no `com.fsck.k9` import is present. Run `./gradlew test --tests "*.StateTest"` and confirm exit code 0 with all test methods passing.

5. **AC-5 / AC-6 — SmsBackupService and SmsRestoreService zero k-9:** Open each file and confirm the import list contains no `com.fsck.k9.*` line. For `SmsRestoreService.java`, additionally confirm the `BinaryTempFileBody` class name does not appear anywhere in the file source. Open `mail/transport/K9MailTransport.java` (or the adapter class from U-025) and confirm it contains the `BinaryTempFileBody.setTempDirectory(...)` call — proving the body-staging configuration is now confined to the adapter.

6. **AC-1 — ServiceBase seam:** Open `service/ServiceBase.java` and confirm: (a) no `com.fsck.k9` import appears; (b) a `getMailTransport()` method (or equivalent) exists that returns `MailTransport` and throws `MailException`; (c) the method builds a `MailTransportConfig` from `AuthPreferences.getStoreUri()` and a `TlsTrustPolicy` value — not a raw boolean `trustAll` argument.

7. **AC-2 / AC-3 — BackupTask and RestoreTask rewired:** Open `BackupTask.java` and confirm: no `com.fsck.k9` import; `catch (XOAuth2FailedException e)` present; `catch (MailException e)` present; `transport.openFolder(...)` present; `transport.appendMessages(...)` present; `transport.closeFolders()` present. Open `RestoreTask.java` and confirm: no `com.fsck.k9` import; `catch (XOAuth2FailedException e)` present; `transport.openFolder(...)`, `transport.getMessages(...)`, `transport.fetch(...)`, `transport.closeFolders()` all present; no reference to `FetchProfile`.

8. **AC-4 — No k-9 exception in any catch clause:** Run `grep -rn "MessagingException\|AuthenticationFailedException\|XOAuth2AuthenticationFailedException" app/src/main/java/com/zegoggles/smssync/service/`. Confirm zero matches.

9. **IC-2 — K9MailTransport is the sole adapter:** Run `grep -rn "com.fsck.k9.mail.store.imap" app/src/main/java/`. Confirm results are limited to `mail/transport/K9MailTransport.java` (or the adapter file from U-025) and `mail/BackupImapStore.java` (if that file is being retained as a shell during transition) — no `service/` file appears.

10. **AC-11 — Full test suite green:** Run `./gradlew test` from the repo root. Confirm exit code 0. In the test report under `app/build/reports/tests/`, confirm `StateTest.shouldGetErrorMessagePrefix`, `BackupImapStoreTest` (all methods), `MessageConverterTest` (all methods), and `MessageGeneratorTest` (all methods) all show PASSED. Confirm total failure count is 0.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android / Java | All changes: `ServiceBase` seam replacement; `BackupTask` and `RestoreTask` call-site rewire; `SmsBackupService` and `SmsRestoreService` k-9 import removal; `BinaryTempFileBody` migration behind adapter; `State.java:32-34` deletion and `isAuthException()` rewrite; `StateTest` update; `BackupConfig`/`RestoreConfig` field type update | Developer |

## Technical Context

**Sequencing is a hard constraint.** This story must be implemented after U-025 (which introduces the `MailTransport` port, `K9MailTransport` adapter, and the `MailException`/`TemporaryImapException`/`XOAuth2FailedException` types). It must also follow the DES-MODERNIZATION-002 trust-security changes (which removed `AllTrustedSocketFactory` and the `trustAll` boolean path from `BackupImapStore` construction). Reversing either order would either leave the engine rewired to a port that does not yet exist, or wrap the ACL around a transport that still permits trust-all TLS.

**The Hilt binding is deferred.** MU-007 (Hilt, U-022) is listed as a dependency because the injection graph must be established before the `MailTransport` binding can be formal. However, DES-MODERNIZATION-009 §Hilt binding caveat explicitly permits a hand-wired provider method on `ServiceBase.getMailTransport()` if MU-007 has not yet fully landed. The engine and adapter do not depend on Hilt; Hilt only formalizes the binding. The developer should confirm the Hilt `MailTransportModule` binding (introduced by U-022) is in place before using constructor injection — if not, a factory method is the correct interim seam.

**`BinaryTempFileBody.setTempDirectory` migration detail.** The call currently lives in `SmsRestoreService.onCreate()`. Its purpose is to configure k-9's temporary MIME body storage before any restore fetch occurs. Moving it behind the adapter means `K9MailTransport` (or its constructor) must receive the `cacheDir` value — either as a parameter on `MailTransportConfig` (add a nullable `File cacheDir` field) or via a separate `initBodyStaging(File cacheDir)` method called from within the adapter's construction path. The adapter then calls `BinaryTempFileBody.setTempDirectory(cacheDir)` at the right moment (before the first `fetch()` call resolves a body). The initialization must still occur before the first restore body fetch — the observable behavior contract does not change.

**`BackupConfig` and `RestoreConfig` carry the store reference.** Today `BackupConfig` holds a `BackupImapStore imapStore` field (used in `fetchAndBackupItems` at `BackupTask.java:155` via `config.imapStore`), and `RestoreConfig` similarly holds a `BackupImapStore imapStore`. The field type in both must change from `BackupImapStore` to `MailTransport`. The `BackupConfig.retryWithStore(BackupImapStore)` builder method should be renamed `retryWithTransport(MailTransport)`. All constructors and test factories that supply a `BackupImapStore` must be updated to supply a `MailTransport` (the test fake / mock from U-025's `FakeMailTransport` or equivalent).

**`State.java` is a Preserved-Core file.** This is the single permitted Preserved-Core edit in the entire engagement (MU-000 invariant, DES-MODERNIZATION-009 §Context). The edit is surgically minimal: delete lines 32–34, remove lines 6–8 k-9 imports, rewrite the two-line `isAuthException()` body. No other logic in `State.java` is touched. The characterization gate is `StateTest.shouldGetErrorMessagePrefix` — it must pass before and after the edit (with the updated `TemporaryImapException` constructor on the after side).

**Exception-mapping completeness.** The `MailException` base type in the ACL hierarchy is the mandatory backstop: any k-9 `MessagingException` subtype not matched by a specific `catch` in the adapter maps to `MailException`. The engine's `catch (MailException e)` block handles it as a generic IMAP failure. This prevents any unmapped k-9 throwable from leaking across the port. The developer must verify the mapping table in CNTR-MODERNIZATION-007 §Exception-translation mapping is exhaustive for every `MessagingException` subtype reachable from `checkSettings`, `openFolder`, `appendMessages`, `getMessages`, and `fetch` — if a gap is found, the fix belongs in the adapter (U-025 scope), not in the engine.

**Key line references (all relative to repo root, verified against source this session):**

| File | Lines of interest |
|------|------------------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` | `:8-11` k-9 imports to remove; `:21` `BackupImapStore` import to remove; `:155` `config.imapStore` reference; `:67,70` `getFolder(...)` calls; `:135` `appendMessages(...)` call; `:168-173` `catch (XOAuth2AuthenticationFailedException/AuthenticationFailedException/MessagingException)` block; `:183-204` `handleAuthError` with `e.getStatus()==400` token-refresh and `catch (MessagingException ignored)` at `:193` |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java` | `:13-17` k-9 imports to remove; `:98` `config.imapStore` reference; `:103` `imapStore.checkSettings()`; `:110,113` `getFolder(...).getMessages(...)` calls; `:141-149` catch block; `:153` `imapStore.closeFolders()`; `:157-170` `handleAuthError` with status-400 branch |
| `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` | `:36` `MessagingException` import; `:40` `BackupImapStore` import; `:99` method declaration; `:104` `new BackupImapStore(ctx, uri, trustAll)` construction |
| `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` | `:28` `com.fsck.k9.mail.MessagingException` import |
| `app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` | `:9` `MessagingException` import; `:10` `BinaryTempFileBody` import; ~`:51` `BinaryTempFileBody.setTempDirectory(getCacheDir())` call |
| `app/src/main/java/com/zegoggles/smssync/service/state/State.java` | `:6-8` k-9 imports to remove; `:32-34` magic-string block to delete; `:35-36` `LocalizableException` branch to retain; `:78-81` `isAuthException()` to rewrite |
| `app/src/test/java/com/zegoggles/smssync/service/state/StateTest.java` | `:62-67` `shouldGetErrorMessagePrefix()` — update `new MessagingException(...)` to `new TemporaryImapException(...)` |

## Supporting Documentation

- `REQ-MODERNIZATION-009` §Acceptance Criteria (AC-1, AC-2, AC-3, AC-5), §Constraints, §Notes
- `DES-MODERNIZATION-009` §Context (four-bucket model; bucket 1 transport coupling; bucket 4 service/bootstrap residuals); §Design (MailTransport port operation table; exception translation mapping; State.java edit target); §Components table; §Integration Design (sequencing; Hilt binding caveat; test integration); §Design Validation (AC-2 fitness function, ADR-009-B scope)
- `CNTR-MODERNIZATION-007` §Contract Definition (MailTransport interface); §App-owned value types; §Exception hierarchy and translation mapping; §Validation Rules (rule 1 engine grep, rule 2 classify-by-type, rule 5 MIME residual); §Error Handling (`getCause()` preservation); §Notes (`BinaryTempFileBody` residual disposition); §Example Payloads (backup write path and restore read path pseudocode)

## Integration Contract References

- **CNTR-MODERNIZATION-007** §Contract Definition — `MailTransport` interface operations (`checkSettings`, `openFolder`, `appendMessages`, `getMessages`, `fetch`, `closeFolders`) and their app-owned type signatures; the exception-translation mapping table (exhaustive k-9-throwable-to-app-owned mapping); `BackupFolderHandle`, `MailMessageHandle`, `FetchSpec`, `MailTransportConfig` value types; validation rule 1 (engine grep = 0) and rule 2 (classify by type, not text)
- **CNTR-MODERNIZATION-007** §Notes — `BinaryTempFileBody` residual disposition: the temp-directory configuration moves behind the adapter; `SmsRestoreService.java:10` must reach 0 under the engine grep
- **CNTR-MODERNIZATION-001** — consumed indirectly: `MailTransportConfig.tlsPolicy` (app-owned `TlsTrustPolicy`) is the engine-side handle; the adapter maps it to the resolved `TrustedSocketFactory` produced by DES-MODERNIZATION-002. This story does not touch the TLS factory selection — it uses whatever `TlsTrustPolicy` shape U-025 established

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Preconditions: U-025 must be complete (port types, adapter skeleton, and ACL exception hierarchy in place) and U-022 must be complete (Hilt singleton modules bootstrapped, so the `MailTransportModule` binding is available). Do not attempt this story until both preconditions are satisfied. The Hilt binding may be hand-wired if MU-007 has not yet fully propagated to the `ServiceBase` seam — see Technical Context above.
