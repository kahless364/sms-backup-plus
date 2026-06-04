---
status: approved
type: nfr
artifact_type: requirement
related_requirements:
  - REQ-MODERNIZATION-009
related_stories:
  - U-025
  - U-026
related_design_docs:
  - DES-MODERNIZATION-009
traces_to:
  - EPIC-MODERNIZATION-005
  - CNTR-MODERNIZATION-007
change_records: []
id: REQ-MODERNIZATION-012
title: Seal the MailTransport ACL exception boundary (zero k-9 types in the engine)
domain: modernization
epic: EPIC-MODERNIZATION-005
priority: medium
---

# REQ-MODERNIZATION-012: Seal the MailTransport ACL exception boundary (zero k-9 types in the engine)

## Description

Every `com.fsck.k9.*` type reference — including fully-qualified name (FQN) references used in catch clauses without an import statement — must be eliminated from all classes in the `service.*` package. The two residual catch-and-translate sites introduced in U-026 as a bounded residual (`BackupTask.java:308` catching `com.fsck.k9.mail.MessagingException` from `MessageConverter.convertMessages()`, and `ServiceBase.java:173` catching `com.fsck.k9.mail.MessagingException` from the `K9MailTransport` constructor) must be relocated to the adapter/converter boundary inside `mail.transport`, so that the engine has zero compile-time coupling to k-9 under any reference form. The `MailTransport` port contract must remain unchanged.

## Context

U-025 defined the `MailTransport` ACL port with an invariant that no `com.fsck.k9.*` type appears in any method signature or thrown-exception declaration in `service.*`. U-026 wired the engine onto that port and eliminated all k-9 `import` statements from `service.*` files, satisfying the letter of the AC-10 grep (`grep -rn "^import com.fsck.k9"`). However, two catch clauses still reference `com.fsck.k9.mail.MessagingException` by FQN, meaning the engine still has compile-time coupling to a k-9 type — the compiler would reject these files the moment `MessagingException` was no longer on the classpath. The coupling persists because `MessageConverter.convertMessages()` declares `throws MessagingException` (a k-9 checked exception), and because `K9MailTransport`'s public constructor declares `throws MessagingException` alongside `throws MailException`. Both throw-sites sit inside `mail.*` / `mail.transport.*`, which are the permitted k-9 zones; the problem is that these checked exceptions escape across the package boundary into `service.*` callers. The fix is to absorb the translation entirely within `mail.transport` before the checked exception can propagate: `K9MailTransport`'s constructor should wrap `MessagingException` internally so its public signature declares only `throws MailException`, and `MessageConverter.convertMessages()` (or a `mail.transport`-owned wrapper/overload) should similarly translate before the result reaches `service.*`.

The `MailTransport` abstraction is only as strong as its leakiest boundary. An ACL port whose adapter constructor can throw a vendor checked exception, or whose converter collaborator can throw one into engine callers, is not a closed boundary — it is a boundary with a hole. Closing this hole completes the ACL invariant originally stated in CNTR-MODERNIZATION-007 and ensures that removing or replacing k-9 requires changes only inside `mail.transport`, not in any `service.*` class.

## Requirements

### Zero k-9 type references in service.* engine classes

No class in `com.zegoggles.smssync.service.*` (including `BackupTask`, `RestoreTask`, `ServiceBase`, `BackupWorker`, `RestoreWorker`, `SmsBackupService`, `SmsRestoreService`, and all subpackages such as `service.state.*`) may reference any `com.fsck.k9.*` type in any form: not as an `import` statement, not as a fully-qualified name in a catch clause, not as a FQN in a `throws` declaration, not as a FQN in a local variable type, and not as a string literal used as a class name for reflective lookup. A search over the compiled sources and the source files both must yield zero matches.

#### Acceptance Criteria

1. Running `grep -rn "com\.fsck\.k9" app/src/main/java/com/zegoggles/smssync/service/` produces zero output lines for the following files: `BackupTask.java`, `RestoreTask.java`, `ServiceBase.java`, `BackupWorker.kt`, `RestoreWorker.kt`. The grep must cover all reference forms (FQN in catch, import, local type, javadoc `{@code}` blocks that would couple at compile time); the command must exit with a non-zero status (no matches) when run against the final committed source tree.

2. Running `grep -rn "com\.fsck\.k9" app/src/main/java/com/zegoggles/smssync/service/` produces zero output lines across the entire `service/` directory tree, including `service/state/`, `service/exception/`, and any other subdirectory. Residuals already documented in approved artifacts as explicitly out-of-scope for the engine ACL (e.g., `App.java` in `com.zegoggles.smssync` root, `preferences/AuthPreferences.java`) are not in `service/` and therefore do not affect this criterion.

3. `./gradlew :app:compileDebugJavaWithJavac` completes with zero errors and zero warnings referencing any k-9 type in a `service.*` source file after the fix. (A compile failure here proves that a FQN catch clause was load-bearing and was not properly relocated to the adapter boundary.)

4. Exception-translation behavior is preserved end-to-end: a `com.fsck.k9.mail.MessagingException` thrown by the k-9 IMAP layer (e.g., password not set, connection refused, folder not found) must surface to all `service.*` callers as `com.zegoggles.smssync.mail.transport.MailException`, with `MailException.getCause()` returning the original `MessagingException` instance. No diagnostic information may be lost. A unit test must verify this causal chain: given a mock/stub that throws `MessagingException("password not set")`, the `MailException` caught by the engine must satisfy `assertThat(caught.getCause()).isInstanceOf(MessagingException.class)` and `assertThat(caught.getCause().getMessage()).isEqualTo("password not set")`.

5. The authentication-failure escalation path is preserved: when the underlying k-9 layer throws `XOAuth2AuthenticationFailedException` or `AuthenticationFailedException`, those exceptions must continue to surface to `service.*` as the existing app-owned types (`XOAuth2FailedException` or `RequiresLoginException` respectively), not as `MailException`. The retry and backoff semantics in `BackupTask.fetchAndBackupItems()`, `BackupWorker.fetchAndBackupItems()`, `RestoreTask.restore()`, and `RestoreWorker.executeRestore()` must be unchanged. Existing unit tests covering these auth-error paths must continue to pass without modification to their assertion logic.

6. `MessageConverter.convertMessages()` must either (a) be changed to declare `throws` an app-owned exception (e.g., `MailException` or a new `MessageConversionException extends MailException`) rather than `throws MessagingException`, or (b) the call site in `BackupTask` must be moved behind a `mail.transport`-owned wrapper method so that the checked `MessagingException` is translated before crossing the package boundary. Either way, the `service.*` call site must not catch any `com.fsck.k9.*` type in any form. The chosen approach must not break the compile contract for any existing caller of `MessageConverter` inside `mail.*`.

7. `K9MailTransport`'s public constructor signature must declare only `throws MailException` (no `throws MessagingException`). The `MessagingException` that `ImapStore`'s constructor can throw must be caught and wrapped inside the `K9MailTransport` constructor body, keeping the translation inside `mail.transport` where k-9 types are permitted. `ServiceBase.getMailTransport()` must compile without any reference to `MessagingException`.

8. The `MailTransport` interface (`com.zegoggles.smssync.mail.transport.MailTransport`) must not be modified: no new method signature, no changed `throws` declaration, no removed method. The port contract established in U-025 and governed by CNTR-MODERNIZATION-007 is immutable for this requirement.

9. All existing unit tests pass: `./gradlew :app:testDebugUnitTest` completes with zero failures. The minimum test count must be equal to or greater than the 568 passing at the close of U-026 (regression floor).

10. The JaCoCo line-coverage gate of 70% for the `:app` module must remain satisfied after any new or modified production code introduced by this requirement. If new translation code is added inside `mail.transport` or `mail/`, corresponding unit tests covering both the happy path (translation occurs, cause chain intact) and the error path (correct app-owned exception type is thrown) must be added to keep coverage above the gate.

## Rationale

The MailTransport ACL boundary was designed so that a future replacement or upgrade of the k-9 library requires changes only inside `com.zegoggles.smssync.mail.transport`. With FQN catch clauses present in `BackupTask` and `ServiceBase`, that promise is broken: removing `MessagingException` from the classpath would cause compile failures in `service.*`. More concretely, the on-device log entry `com.zegoggles.smssync.mail.transport.MailException: com.fsck.k9.mail.MessagingException: password not set` shows the translation working at runtime, but the engine class that produces it still names the k-9 type at compile time. The ACL purity argument is architectural, not behavioral: behavior is already correct, but the structural dependency that ACL was built to sever remains open. This requirement closes that last-mile gap, making CNTR-MODERNIZATION-007's "zero k-9 types in `service.*`" invariant unconditionally true — not just true under the narrow definition of import statements.

Preventing future coupling creep is an equal motivation. A FQN catch in a file that is otherwise k-9-free signals to the next developer that using k-9 FQNs is an acceptable pattern in this package. Eliminating the pattern entirely makes the boundary self-enforcing: any future re-introduction of a k-9 FQN will be immediately visible as an anomaly in code review, rather than blending into an already-present precedent.

## Constraints

- Behavior preservation is mandatory. This is an architectural-quality change only. No observable runtime behavior — exception message content, error codes, retry counts, backoff intervals, UI state transitions, or WorkManager result codes — may change.
- The original `MessagingException` instance must be retained as `getCause()` on the wrapping `MailException` in all translation paths. Dropping the cause would remove diagnostic information that `State.getDetailedErrorMessage()` relies on to produce the "underlying=" suffix in error strings (see `State.java:45`).
- The `MailTransport` port interface must not be modified (see AC-8). Design must absorb the translation below the port, not above it.
- `MessageConverter` is a mail-conversion collaborator, not a transport component. If its `convertMessages()` signature is changed to throw an app-owned exception, that change must be backward-compatible with all callers inside `mail.*` and any test code that invokes `convertMessages()` directly. Adding a new checked exception to the signature is a breaking API change; the preferred approach is to wrap `MessagingException` inside `convertMessages()` and re-throw as the app-owned type, or to introduce a new package-private wrapper method in `mail.transport` that calls `convertMessages()` and performs the translation locally.
- The `K9MailTransport` package-private test constructor (`K9MailTransport(BackupImapStoreDelegate, TrustedSocketFactory)`) must not be changed; it is used by `MailTransportTestFactories` in the test suite.
- No changes to `App.java` (k-9 bootstrap) or `preferences/AuthPreferences.java` (k-9 `AuthType` enum) are in scope; those files are documented carve-outs in REQ-MODERNIZATION-009.

## Verification Method

Static analysis (grep): AC-1 and AC-2 are verified by running the specified grep command against the committed source tree and confirming zero output lines.

Compilation: AC-3 and AC-7 are verified by a clean `./gradlew :app:compileDebugJavaWithJavac` run with zero errors.

Unit tests: AC-4, AC-5, AC-6, AC-9, and AC-10 are verified by `./gradlew :app:testDebugUnitTest` with a passing test count >= 568 and zero failures. AC-4 specifically requires a named test case asserting the cause chain.

Coverage gate: AC-10 is verified by `./gradlew :app:jacocoTestReportDebug` producing a line-coverage percentage >= 70% for the `:app` module.

Port immutability: AC-8 is verified by diffing `MailTransport.java` against the version committed at the close of U-025 and confirming no changes to method signatures, throws declarations, or method count.

## Notes

**Bounded-residual origin**: Both catch sites were introduced intentionally in U-026 as the minimum change needed to keep the engine compiling while U-025's port was being wired. The U-026 implementation log (line 55) and code review (observation 1) both document this as an accepted "bounded residual" requiring a follow-on story. This requirement is that follow-on.

**Primary leak source is MessageConverter, not K9MailTransport**: `MessageConverter` is in the `mail` package (not `mail.transport`), and it uses k-9 `Message`, `MessagingException`, `Address`, `Flag`, and `MimeUtility` internally — these internal usages are permitted and out of scope. The problem is exclusively the `throws MessagingException` on the public `convertMessages()` method, which forces any `service.*` caller to name the k-9 type in a catch clause. The `K9MailTransport` constructor leak is secondary and structurally simpler: it is already inside `mail.transport` and needs only to move its catch inward.

**FQN-catch is not a safe workaround for an import-based AC**: The AC-10 criterion in U-026 was defined as "zero `^import com.fsck.k9` lines in `service.*`". FQN catches technically satisfy that grep while preserving compile-time coupling. This requirement supersedes that interpretation: the correct completeness criterion is zero occurrences of `com.fsck.k9` in any syntactic position in `service.*` source files.

**Related approved requirement**: REQ-MODERNIZATION-009 AC-2 establishes the broad ACL goal; this requirement narrows it to the specific residuals that survived U-025/U-026 and tightens the verification criterion to cover all reference forms, not just import statements.
