---
type: story
status: done
artifact_type: user-story
priority: medium
complexity: medium
parallel_eligible: true
iteration: 1
requirements:
  - REQ-MODERNIZATION-012
design_docs:
  - DES-MODERNIZATION-012
integration_contracts:
  - CNTR-MODERNIZATION-007
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-030
title: Seal MailTransport ACL exception boundary — zero k-9 types in service.*
pipeline: ''
domain: modernization
alignment_audit: passed
sprint: '000002'
updated_at: '2026-06-04T19:52:48.527Z'
resolution: done
---

# U-030: Seal MailTransport ACL exception boundary — zero k-9 types in service.*

## Story

As a maintainer of SMS Backup+,
I want the backup/restore engine to reference zero `com.fsck.k9` types,
So that the MailTransport ACL is leak-proof and future k-9 coupling cannot creep back in.

## Acceptance Criteria

- [x] **AC-1 — grep-zero (code):** Running
  ```
  grep -rn "com\.fsck\.k9" app/src/main/java/com/zegoggles/smssync/service/
  ```
  against the committed source tree returns **zero output lines** attributable to executable code
  (FQN catch clauses, FQN `throws` declarations, FQN local variable types, import statements).
  Specifically, the two surviving code catches — `BackupTask.java:308`
  (`catch (com.fsck.k9.mail.MessagingException e)` wrapping `converter.convertMessages(...)`) and
  `ServiceBase.java:173` (`catch (com.fsck.k9.mail.MessagingException e)` wrapping
  `new K9MailTransport(getApplicationContext(), config)`) — are gone.

- [x] **AC-2 — grep-zero (comments and javadoc, surviving files):** The same grep command
  returns **zero output lines** across the entire `service/` directory tree — including
  `service/state/`, `service/exception/`, and every subdirectory — for all reference forms
  including `{@code com.fsck.k9.*}` javadoc blocks and `//` or `/* */` comments. The 12
  surviving comment/javadoc occurrences of the literal string `com.fsck.k9` in files that
  are **not** deleted by U-031 must be reworded or removed:

  | File | Lines | Current text (exemplar) | Required action |
  |------|-------|------------------------|-----------------|
  | `BackupWorker.kt` | 55 | `No {@code com.fsck.k9.*} import remains in this...` | Reword — drop the FQN literal |
  | `RestoreWorker.kt` | 30 | `all com.fsck.k9.* imports removed` | Reword — drop the FQN literal |
  | `RestoreWorker.kt` | 66 | `No {@code com.fsck.k9.*} import remains in this...` | Reword — drop the FQN literal |
  | `SmsBackupService.java` | 27 | `import com.fsck.k9.mail.MessagingException removed` | Reword — drop the FQN literal |
  | `SmsBackupService.java` | 78 | `{@code com.fsck.k9.mail.MessagingException} import removed` | Reword — drop the FQN literal |
  | `SmsRestoreService.java` | 11 | `com.fsck.k9.mail.MessagingException import removed` | Reword — drop the FQN literal |
  | `SmsRestoreService.java` | 12 | `com.fsck.k9.mail.internet.BinaryTempFileBody import removed` | Reword — drop the FQN literal |
  | `SmsRestoreService.java` | 41 | `{@code com.fsck.k9.mail.MessagingException} and` | Reword — drop the FQN literal |
  | `SmsRestoreService.java` | 42 | `{@code com.fsck.k9.mail.internet.BinaryTempFileBody} imports removed` | Reword — drop the FQN literal |
  | `State.java` | 6 | `com.fsck.k9.mail.AuthenticationFailedException import removed` | Reword — drop the FQN literal |
  | `State.java` | 7 | `com.fsck.k9.mail.MessagingException import removed` | Reword — drop the FQN literal |
  | `State.java` | 8 | `com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException import removed` | Reword — drop the FQN literal |

  Note: the 6 comment occurrences in `BackupTask.java` (lines 8, 51, 286) and `RestoreTask.java`
  (lines 14, 55, 134) are moot — those files are deleted wholesale by U-031 (REQ-013).
  They do not need to be reworded here; they must not be pre-emptively deleted by this story.

- [x] **AC-3 — CNTR-007 Clause C-1 — K9MailTransport constructor narrowed:**
  `K9MailTransport`'s public constructor signature at `K9MailTransport.java:117-118`
  changes from
  `public K9MailTransport(Context context, MailTransportConfig config) throws MailException, MessagingException`
  to
  `public K9MailTransport(Context context, MailTransportConfig config) throws MailException`
  (i.e., `MessagingException` is removed from the public `throws` clause). The constructor body
  wraps the `new BackupImapStoreDelegate(context, config.storeUri, this.resolvedSocketFactory)`
  call at line 120 in `try { ... } catch (com.fsck.k9.mail.MessagingException e) { throw new MailException(e); }`.
  The `com.fsck.k9.mail.MessagingException` **import remains** in `K9MailTransport.java`
  (that file is in the permitted adapter zone `mail.transport.*`). The
  `BinaryTempFileBody.setTempDirectory(context.getCacheDir())` call at line 125 must remain
  inside the constructor, positioned after the delegate is built.

- [x] **AC-4 — CNTR-007 Clause C-2 — MessageConverter.convertMessages thrown type narrowed:**
  `MessageConverter.convertMessages(Cursor, DataType)` at `MessageConverter.java:120-121`
  changes its declared `throws` from `com.fsck.k9.mail.MessagingException` to
  `com.zegoggles.smssync.mail.transport.MailException`. Internally the method wraps any
  `MessagingException` it catches as `new MailException(originalMessagingException)` —
  the original `MessagingException` instance is the cause (`getCause()` returns it). The import
  `import com.zegoggles.smssync.mail.transport.MailException;` is added to `MessageConverter.java`.
  The six existing test methods in `MessageConverterTest` that declare `throws MessagingException`
  on their own method signatures are updated to `throws MailException` (or `throws Exception`);
  no result-assertion change is required for those six existing tests.

- [x] **AC-5 — ServiceBase FQN catch collapses (Clause C-1 consumer-side effect):**
  `ServiceBase.java:171-175` — the FQN catch
  `} catch (com.fsck.k9.mail.MessagingException e) { throw new MailException(e); }`
  surrounding `return new K9MailTransport(getApplicationContext(), config)` — is removed.
  The call site compiles cleanly with only `throws MailException` on `getMailTransport()`.
  `./gradlew :app:compileDebugJavaWithJavac` completes with zero errors and zero k-9-type
  warnings in any `service.*` source file.

- [x] **AC-6 — BackupTask FQN catch collapses (Clause C-2 consumer-side effect):**
  `BackupTask.java:305-310` — the inner `try { result = converter.convertMessages(...); } catch (com.fsck.k9.mail.MessagingException e) { throw new MailException(e); }` block — is simplified to a
  direct `result = converter.convertMessages(cursor.cursor, cursor.type);`. The outer
  `catch (MailException e)` at the enclosing try already handles it. The comment above the
  block (lines 302-304) that references the U-026 bounded residual is removed or reworded so
  that no FQN literal `com.fsck.k9` survives. Both `BackupTask.java` and `BackupWorker.kt:310`
  compile under the new `convertMessages` signature.

- [x] **AC-7 — MailTransport interface byte-unchanged (REQ-012 AC-8, CNTR-007 v2):**
  The file `app/src/main/java/com/zegoggles/smssync/mail/transport/MailTransport.java` is
  not edited. A diff of `MailTransport.java` against the version committed at the close of
  U-025 shows zero changes to method signatures, `throws` declarations, or method count.

- [x] **AC-8 — Cause-chain preserved — unit test:**
  A new unit test (in an appropriate test class under `app/src/test/`) verifies the
  `MessagingException → MailException` cause chain at **both** translation sites:
  - **C-1 path (constructor):** given a `BackupImapStoreDelegate` stub / mock whose constructor
    throws `new MessagingException("password not set")`, the `MailException` caught by the
    caller must satisfy:
    `assertThat(caught.getCause()).isInstanceOf(MessagingException.class)` and
    `assertThat(caught.getCause().getMessage()).isEqualTo("password not set")`.
  - **C-2 path (converter):** given a `MessageConverter` (or a subclass / mock) configured
    so that `convertMessages` triggers its internal catch of a
    `MessagingException("conversion failed")`, the `MailException` propagated to the caller
    must satisfy the same assertions with message `"conversion failed"`.
  Both sub-cases must pass; the test may live in one test method or two.

- [x] **AC-9 — State.getDetailedErrorMessage "underlying=" suffix preserved:**
  After the changes, `State.getDetailedErrorMessage(resources)` continues to produce a
  string containing `"underlying="` when called with a `MailException` that wraps a
  `MessagingException`. This is verified by reading `State.java:59`
  (`exception.getCause() != null ? exception.getCause().toString() : null`) and confirming
  the `MailException` cause chain populates that branch: the original `MessagingException`
  remains reachable via `getCause()` and its `toString()` is non-empty. An existing or new
  unit test on `State` (or `BackupState` / `RestoreState`) asserts this behavior.

- [x] **AC-10 — Auth-escalation paths unchanged:**
  The existing unit tests covering the `XOAuth2FailedException` and `RequiresLoginException`
  propagation paths (verifying that an `XOAuth2AuthenticationFailedException` or
  `AuthenticationFailedException` from the k-9 layer surfaces to `service.*` as the correct
  app-owned type, not as a generic `MailException`) pass without modification to their
  assertion logic. The `checkSettings()` and adapter methods in `K9MailTransport`
  (lines 152-247) are not changed by this story; their subtype-first catch ordering
  (`XOAuth2AuthenticationFailedException` → `XOAuth2FailedException`, then
  `AuthenticationFailedException` → `RequiresLoginException`) is preserved byte-for-byte.

- [x] **AC-11 — Package-private test constructor unchanged:**
  The package-private constructor `K9MailTransport(BackupImapStoreDelegate, TrustedSocketFactory)`
  at `K9MailTransport.java:93-96` is not modified. It declares no `throws` clause and is used
  by `MailTransportTestFactories`; `MailTransportTestFactories`-dependent tests must continue
  to compile and pass.

- [x] **AC-12 — Full test suite green, regression floor holds:**
  `./gradlew :app:testDebugUnitTest` completes with **zero failures**. The passing test count
  is equal to or greater than 568 (the regression floor at the close of U-026, REQ-012 AC-9).

- [x] **AC-13 — JaCoCo 70% line-coverage gate holds:**
  `./gradlew :app:jacocoTestReportDebug` (or equivalent coverage task) passes the 70%
  line-coverage gate for the `:app` module (REQ-012 AC-10). The new translation code added
  inside `K9MailTransport.java` and `MessageConverter.java` is covered by the unit test from
  AC-8.

### Integration Criteria

- [x] **IC-1:** After C-1 lands, `ServiceBase.getMailTransport()` compiles with only
  `throws MailException` on its declaration — confirmed by `./gradlew :app:compileDebugJavaWithJavac`
  with zero errors.
- [x] **IC-2:** After C-2 lands, `BackupWorker.kt:310` (`val result = converter.convertMessages(...)`)
  continues to compile without modification — the `MailException` it can now receive is already
  covered by the enclosing `catch (e: MailException)` at `BackupWorker.kt:138` and
  `MailException` is already imported at line 34.
- [x] **IC-3:** After C-2 lands, `BackupTask.java` compiles (or is shown to compile after the
  inner try/catch at lines 305-310 is simplified to a direct call) — required because U-031
  has not yet deleted that file; both call sites must be valid under the new signature.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java` | Public constructor at lines 117-118 declares `throws MailException, MessagingException`; `new BackupImapStoreDelegate(...)` at line 120 is unwrapped | Narrow constructor `throws` to `throws MailException` only; wrap delegate construction in `try/catch(MessagingException e){throw new MailException(e);}` (CNTR-007 Clause C-1); preserve `BinaryTempFileBody.setTempDirectory(context.getCacheDir())` at line 125 |
| `app/src/main/java/com/zegoggles/smssync/mail/MessageConverter.java` | `convertMessages(Cursor, DataType)` at lines 120-121 declares `throws com.fsck.k9.mail.MessagingException` | Change declared throws to `throws MailException`; wrap internal `MessagingException` as `new MailException(e)` with cause preserved; add `import com.zegoggles.smssync.mail.transport.MailException` (CNTR-007 Clause C-2) |
| `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` | Lines 305-310: inner `try/catch(com.fsck.k9.mail.MessagingException e)` around `converter.convertMessages(...)` call | Remove the inner try/catch wrapper; direct `result = converter.convertMessages(...)` falls under the existing outer `catch (MailException e)` |
| `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` | Lines 171-175: FQN catch `catch (com.fsck.k9.mail.MessagingException e) { throw new MailException(e); }` around `return new K9MailTransport(getApplicationContext(), config)` | Remove the FQN catch wrapper; plain `return new K9MailTransport(getApplicationContext(), config);` compiles under the narrowed constructor signature |
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` | Line 55: javadoc comment contains literal `com.fsck.k9.*` | Reword the comment to remove the FQN literal (e.g., "No k-9 transport/MIME import remains…"); no code change |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` | Lines 30, 66: comments contain literal `com.fsck.k9.*` | Reword both comments to remove FQN literals |
| `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` | Lines 27, 78: comments contain literal `com.fsck.k9.mail.MessagingException` | Reword both to remove FQN literals |
| `app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` | Lines 11, 12, 41, 42: comments contain FQN literals `com.fsck.k9.mail.MessagingException` and `com.fsck.k9.mail.internet.BinaryTempFileBody` | Reword all four to remove FQN literals |
| `app/src/main/java/com/zegoggles/smssync/service/state/State.java` | Lines 6, 7, 8: `//` comments name three k-9 FQN types (`AuthenticationFailedException`, `MessagingException`, `XOAuth2AuthenticationFailedException`) | Reword all three to remove FQN literals (e.g., "k-9 auth/messaging exception imports removed (U-026)") |
| Test class (new or existing): `K9MailTransportTest` or `MessageConverterTest` | No test for cause-chain on the new translation paths | Add AC-8 cause-chain unit test(s) |
| `app/src/test/java/.../mail/MessageConverterTest.java` | Six test methods declare `throws MessagingException` | Update those six `throws` clauses to `throws MailException` (or `throws Exception`); no assertion changes |

## Existing Behavior to Preserve

- The `BinaryTempFileBody.setTempDirectory(context.getCacheDir())` call at `K9MailTransport.java:125` must remain in the public constructor body, executing after the delegate is built. It was relocated behind the adapter in U-026 and must not be displaced.
- `State.getDetailedErrorMessage(resources)` reads `exception.getCause().toString()` to produce the `"underlying="` suffix (State.java:59). The `MailException.getCause()` must return the original `MessagingException` instance at both new translation sites so this diagnostic path is unaffected.
- The auth-escalation translation in `K9MailTransport` adapter methods (lines 152-247) — `XOAuth2AuthenticationFailedException` → `XOAuth2FailedException`, `AuthenticationFailedException` → `RequiresLoginException` — is byte-unchanged. These catches precede the generic `MessagingException` backstop in subtype-first order; this story does not touch them.
- The `XOAuth2FailedException.getStatus()` value is preserved through the auth path (the engine branches on `getStatus() == 400` for token-refresh retry).
- `BackupWorker.kt:310` (`val result = converter.convertMessages(...)`) requires no code change — the C-2 narrowing is a compile-safe narrowing for this call site; it already imports and catches `MailException`.
- The package-private test constructor `K9MailTransport(BackupImapStoreDelegate, TrustedSocketFactory)` at lines 93-96 (no `throws`, used by `MailTransportTestFactories`) is not modified.
- `BackupTask.java` and `RestoreTask.java` are **not** deleted by this story. Their deletion belongs to U-031 (REQ-013). The 6 FQN-literal comment lines in those two files (BackupTask:8,51,286 and RestoreTask:14,55,134) are removed from the grep-zero scope for this story because those files will be deleted wholesale by U-031.
- `MailTransport.java` (the interface) is not edited — zero changes to method signatures, `throws` declarations, or method count.
- `MailModule.kt:93-97` contains a redundant `MessagingException` catch wrapping the `K9MailTransport` constructor (the workers' factory lambda). Simplifying it to a plain constructor call is permitted but not required by this story; it must not be broken.

## Verification Steps

1. **Run the grep-zero gate:** From the project root, run
   ```
   grep -rn "com\.fsck\.k9" app/src/main/java/com/zegoggles/smssync/service/
   ```
   Expected: zero output lines. Any output is a failure — identify the file and line and fix
   before declaring done.

2. **Compile check:** Run `./gradlew :app:compileDebugJavaWithJavac` and confirm zero errors
   and zero k-9-type warnings in `service.*` source files. A compile failure here proves that
   a FQN catch clause was load-bearing and was not properly relocated to the adapter boundary
   (REQ-012 AC-3).

3. **Verify constructor `throws` clause:** Open `K9MailTransport.java` and confirm the public
   constructor at line 117 declares `throws MailException` (only). Confirm that inside the
   constructor body, `new BackupImapStoreDelegate(...)` is wrapped in a try/catch that catches
   `com.fsck.k9.mail.MessagingException` and throws `new MailException(e)`. Confirm
   `BinaryTempFileBody.setTempDirectory(context.getCacheDir())` remains on the line immediately
   after the delegate is constructed.

4. **Verify converter `throws` clause:** Open `MessageConverter.java` and confirm
   `convertMessages(Cursor, DataType)` at line 120 declares `throws MailException` (app-owned).
   Confirm `import com.zegoggles.smssync.mail.transport.MailException` is present. Confirm
   internal wrapping as `new MailException(originalCause)`.

5. **Verify ServiceBase collapse:** Open `ServiceBase.java` around lines 171-175 and confirm
   the FQN catch is gone. The call `return new K9MailTransport(getApplicationContext(), config)`
   should be a direct return with no surrounding try/catch for `MessagingException`.

6. **Verify BackupTask collapse:** Open `BackupTask.java` around lines 305-310 and confirm the
   inner try/catch for `com.fsck.k9.mail.MessagingException` is gone. The call
   `result = converter.convertMessages(cursor.cursor, cursor.type)` should be a plain
   assignment inside the outer try block.

7. **Verify MailTransport interface unchanged:** Run
   ```
   git diff <U-025-merge-commit>..HEAD -- app/src/main/java/com/zegoggles/smssync/mail/transport/MailTransport.java
   ```
   and confirm zero changes to method signatures, throws declarations, or method count.

8. **Verify comment scrub:** Run the grep again targeting each of the 12 surviving files
   individually (BackupWorker.kt, RestoreWorker.kt, SmsBackupService.java,
   SmsRestoreService.java, State.java) and confirm zero hits in each. Do not rely solely
   on the directory-wide grep — verify file by file that the rewording is complete.

9. **Run AC-8 cause-chain unit test:** Run `./gradlew :app:testDebugUnitTest --tests "*K9MailTransportTest*"` (or the appropriate test class name). The cause-chain assertions for both C-1 and C-2 paths must pass.

10. **Run the full unit test suite:** `./gradlew :app:testDebugUnitTest`. Confirm zero failures and
    test count >= 568.

11. **Run the JaCoCo gate:** `./gradlew :app:jacocoTestReportDebug` (or the project's equivalent
    coverage verification task). Confirm line coverage >= 70% for the `:app` module.

12. **Verify auth-escalation tests pass:** Identify the existing unit tests for
    `XOAuth2FailedException` and `RequiresLoginException` propagation in
    `K9MailTransportTest` (or equivalent). Confirm all pass without modification to their
    assertion logic.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java/Kotlin) | All changes — `mail.transport`, `mail`, `service.*`, tests | Developer |

## Technical Context

**Sequencing:** This is story 1 of the EPIC-MODERNIZATION-005 debt-closure sequence.
U-030 (REQ-012, this story) must land **before** U-031 (REQ-013, `BackupTask`/`RestoreTask`
deletion) and U-032 (REQ-014, `@AndroidEntryPoint`). The ordering is a hard constraint:
U-031 deletes `BackupTask.java` (making the `BackupTask:308` collapse moot retroactively),
but since U-030 lands first, `BackupTask.java` still exists and must compile under the
narrowed `convertMessages` signature during this story's implementation and CI gate.

**Permitted-zone rule:** The `com.fsck.k9.mail.MessagingException` **import** is allowed to
remain in `K9MailTransport.java` — that file lives in `mail.transport.*`, the designated
adapter zone. The seal is about what appears in `service.*` (the engine), not about purging
k-9 from the adapter internals. Only the *public constructor throws clause* narrows; the
in-body `catch (com.fsck.k9.mail.MessagingException e)` and the corresponding import are
expected to stay inside `K9MailTransport.java`.

**Two leak sources, not one:**
- Primary leak: `MessageConverter.convertMessages()` (`mail.*`) declares
  `throws com.fsck.k9.mail.MessagingException` — a checked exception that escapes from
  `mail.*` into `service.*`, forcing `BackupTask.java:308` to name the k-9 type in a
  FQN catch clause. Fix: narrow to `throws MailException` (option (a) of REQ-012 AC-6),
  translate internally.
- Secondary leak: `K9MailTransport` public constructor (`mail.transport.*`) declares
  `throws MailException, MessagingException` — the `MessagingException` from
  `new BackupImapStoreDelegate(...)` forces `ServiceBase.java:173` to name the k-9 FQN.
  Fix: absorb the catch inside the constructor body, narrow to `throws MailException` only
  (REQ-012 AC-7, CNTR-007 Clause C-1).

**Comment scrub is mandatory to reach grep-zero:** AC-2 demands the grep produce zero lines
across the entire `service/` tree. Fixing only the two code catches leaves 12
comment/javadoc `com.fsck.k9` occurrences (in BackupWorker.kt, RestoreWorker.kt,
SmsBackupService.java, SmsRestoreService.java, and State.java) — these are stale U-026
"import removed" breadcrumbs that are no longer meaningful and must be reworded. The 6
occurrences in BackupTask.java and RestoreTask.java vanish when U-031 deletes those files
and need not be touched by this story.

**MailException(Throwable) constructor:** `MailException.java:44` provides
`public MailException(Throwable cause) { super(cause); }`. Both new translation sites must
use this constructor form — `new MailException(e)` where `e` is the original
`MessagingException` — never `new MailException(e.getMessage())` (which drops the cause
and breaks `State.getDetailedErrorMessage()`'s "underlying=" suffix).

**Redundant wrap in MailModule.kt:** `MailModule.kt:93-97` contains a `catch (MessagingException e)`
wrapper in the workers' `MailTransportFactory` lambda. After C-1 narrows the constructor,
this redundant wrap can be simplified to a plain `K9MailTransport(context, config)` call.
This cleanup is **not required** by any AC but is a permitted tidy if it aids clarity.
It must not be broken.

**Estimation:** Medium complexity. The code changes are highly localized (2 production files
in `mail.*` / `mail.transport.*`, 2 consumer collapses in `service.*`, 9 comment rewrites in
5 service files). The single atomic done-condition is the grep-zero gate. One new unit test
(AC-8 cause-chain). MessageConverterTest `throws` clause updates are mechanical.

## Supporting Documentation

- REQ-MODERNIZATION-012 — the driving requirement; all 10 ACs ground the criteria above
- DES-MODERNIZATION-012 §Component Design (REQ-012 subsections): architectural rationale,
  AC-5 auth-escalation safety analysis, comment/javadoc scrub decision, sequencing
- CNTR-MODERNIZATION-007 v2 §Adapter-construction & converter-boundary clauses (v2):
  authoritative specification for Clause C-1 and Clause C-2
- CR-002 §Impact Analysis and §Cascade Analysis: per-file change map, compile-safety
  analysis for both call sites, and fix checklist

## Integration Contract References

- CNTR-MODERNIZATION-007 v2 §Clause C-1 — `K9MailTransport` public constructor `throws` narrowing
- CNTR-MODERNIZATION-007 v2 §Clause C-2 — `MessageConverter.convertMessages` thrown-type narrowing
- CNTR-MODERNIZATION-007 §Validation Rules Rule 1 — `grep com.fsck.k9 service/` → 0 (unconditionally)
- CNTR-MODERNIZATION-007 §"Interface: MailTransport" — byte-unchanged invariant (REQ-012 AC-8)
- CNTR-MODERNIZATION-007 §Error Handling — cause-chain preservation (`getCause()` = original `MessagingException`) for `State.getDetailedErrorMessage()`

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

**Scope boundary — what is NOT in this story:**
- No changes to `MailTransport.java` (the interface) — it is byte-unchanged by design.
- No deletion of `BackupTask.java` or `RestoreTask.java` — that is U-031.
- No `@AndroidEntryPoint` annotation changes — that is U-032.
- No changes to `App.java` or `preferences/AuthPreferences.java` — documented carve-outs per REQ-MODERNIZATION-009.
- The `K9MailTransport` package-private test constructor is not touched.
- Simplifying `MailModule.kt:93-97` is optional, not required.

**FQN-catch is not a safe workaround:** The U-026 AC-10 criterion was defined as
"zero `^import com.fsck.k9` lines in `service.*`". FQN catches technically satisfy that
grep while preserving compile-time coupling. This story supersedes that interpretation —
the correct completeness criterion is zero occurrences of `com.fsck.k9` in any syntactic
position or comment in `service.*` source files.
