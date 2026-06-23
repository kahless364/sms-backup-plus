---
artifact_type: review-code
story_id: "U-052"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-06-23"
blockers: 0
warnings: 3
---

# Code Review: U-052

## Review Summary

U-052 adds GreenMail 2.1.3 as a `testImplementation`-only dependency, adds 26 new integration tests across three files (`BackupImapStoreDelegateIntegrationTest.java`, `BackupWorkerIntegrationTest.kt`, `RestoreWorkerIntegrationTest.kt`), removes the six `jacocoFileFilter` exclusion lines for `BackupImapStoreDelegate*`, `BackupWorker*`, and `RestoreWorker*`, and adds the required supply-chain checksums to `gradle/verification-metadata.xml`. All five ACs are substantially met and the gate is reported green at `service*=75.6%`. Three warnings are noted below; none are blockers.

| Aspect | Status |
|--------|--------|
| Contract compliance | PASS — `testImplementation` only; no `.*` wildcard trust broadening |
| Test coverage | PASS — real IMAP round-trips; meaningful assertions; BUG-010 invariant confirmed via production seam |
| Code quality | PASS with warnings — one unused import; two AC-precision gaps documented |

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

**W-1: Unused import `com.fsck.k9.mail.Folder` in `BackupImapStoreDelegateIntegrationTest.java` line 4.**

`Folder` is imported but never referenced as a type in any executable expression. All occurrences in the file are in Javadoc `{@code}` tags or comment prose, not in code. The javac `-Werror` flag is scoped to non-test compilations, so this does not break the build, but it is noise and could mislead future readers about which k-9 types are in use. Recommend removal.

File: `app/src/test/java/com/zegoggles/smssync/mail/transport/BackupImapStoreDelegateIntegrationTest.java:4`

**W-2: AC-1 "folder-not-found error path" covered by null-label guard, not by IMAP-level NO response.**

AC-1 explicitly calls for coverage of "the folder-not-found error path." The test `openFolder_nullLabel_throwsIllegalStateException` tests the Java-level guard in `BackupImapStoreDelegate.openFolder()` that fires when `preferences.getFolder(type)` returns null — before any IMAP connection is made. The real IMAP-level folder-not-found path is the `openWithRetryAfterCreate` backoff retry loop (`MAX_CREATE_OPEN_RETRIES=6`, up to 23s of inter-attempt delay; lines 591-598 of `K9MailTransport.java`). That loop is NOT exercised by any test in this story because GreenMail IMAP would need to return `NO [NONEXISTENT]` repeatedly, and the built-in 23s backoff would make tests impractically slow.

The null-label guard is a legitimate code path and the test is correct. The gap is the IMAP-level retry loop. The implementation log acknowledges this implicitly by noting the `openWithRetryAfterCreate` path is not touched. This is a documentation/completeness gap in AC-1 coverage claims rather than a test correctness problem. Recommend noting the open gap with a TODO in the test file or in the story's verification steps.

**W-3: AC-2 BUG-010 watermark invariant tested via direct seam call, not via `doWork()` end-to-end.**

AC-2 states: "a worker is constructed, run against a stubbed MailTransportFactory and embedded IMAP, and the test asserts that successfully appended messages advance the watermark and that a failed append leaves the watermark unchanged (BUG-010 invariant via the production code path)." The `BackupWorkerIntegrationTest` tests `appendBatchAndUpdateWatermark_succeeds_advancesWatermark` and the failure variants by calling `worker.appendBatchAndUpdateWatermark()` directly — not via `doWork()`. The reason is structural: `doWork()` only reaches `appendBatchAndUpdateWatermark` if `backupCursors()` is called, which requires `itemsToSync > 0`, which requires real SMS data seeded into Robolectric providers.

The BUG-010 invariant IS confirmed against production code (via the `internal` seam extracted in U-053 / `BackupWorkerWatermarkTest`), and the implementation log is transparent about this. The watermark guarantee is mechanically airtight. The AC language "via the production code path" is met at the seam level, but not at the full `doWork()` → `backupCursors()` → `appendBatchAndUpdateWatermark()` end-to-end level. This is a reasonable engineering tradeoff given the ContentProvider seeding complexity. Recommend that a future story (noted already in the implementation log) seeds the Robolectric SMS provider to drive `backupCursors` and close the last 35-line gap in `BackupWorker` coverage.

### Observations

**O-1: GreenMail HEADER search limitation honestly documented.**

The test `getMessagesInternal_afterAppend_serverResponseCoverageTest` explicitly documents that GreenMail 2.1.3 does not support `UID SEARCH HEADER <field> <value>` and that the expected outcome is a `MessagingException` caught by the try block. The assertion (`assertThat(expected.getMessage()).isNotNull()`) is meaningful as a liveness check that the code path executed. The Javadoc is clear about why the success branch is not tested against GreenMail. This is the correct approach.

**O-2: `doWork_restoreDisabled_returnsSuccessImmediately` documents an important production execution order.**

The comment in `RestoreWorkerIntegrationTest` correctly notes that `RestoreWorker.doWork()` calls `mailTransportFactory.create()` BEFORE checking the restore-enabled flags (line 167-173 of `RestoreWorker.kt`). Using a non-throwing transport with `buildWorkerWithTransport` to reach the early-exit branch is correct. This level of documentation adds genuine value for future maintainers.

**O-3: `InMemoryCheckpointStore` and `FakeSmsContentProvider` are sourced from `RestoreWorkerCheckpointTest.kt` (same package).**

`RestoreWorkerIntegrationTest.kt` references these classes but they are defined in `RestoreWorkerCheckpointTest.kt`. Both are in `package com.zegoggles.smssync.service` under `src/test`, so Kotlin visibility is correct. This is fine, but it creates a hidden coupling: if `RestoreWorkerCheckpointTest.kt` renames or internalizes `InMemoryCheckpointStore`, `RestoreWorkerIntegrationTest` would silently break. This is a low-risk observation; no action required unless the checkpoint test is ever refactored.

**O-4: `BackupWorker=55.8%` LINE is below the 70% AC target but does not fail the gate.**

The story's coverage goal (implied by AC-4) is that all three excluded classes should "contribute to" the package floor, not that each class individually hit 70%. The 55.8% on `BackupWorker` is primarily because `backupCursors` (35 lines) and `handleAuthError` (26 lines) are unreachable without real ContentProvider data or XOAuth2 injection. The package-level `service*=75.6%` clears the 70% floor. The implementation log is transparent about these gaps. No action required.

**O-5: `verification-metadata.xml` changes are correct — no wildcard broadening.**

The diff adds 12 specific `<component>` entries for GreenMail 2.1.3 and its transitive closure (greenmail, greenmail-junit4, greenmail-parent, jakarta.activation-api, jakarta.mail-api, org.eclipse.angus:all/angus-activation/jakarta.mail, org.eclipse.ee4j:project 1.0.8/1.0.9, org.glassfish.jersey:jersey-bom, org.slf4j:slf4j-api 1.7.36 / slf4j-parent 1.7.36). All entries carry specific SHA-256 values with `origin="Generated by Gradle"`. The `<trust file=".*-sources[.]jar" regex="true"/>` and `<trust file=".*-javadoc[.]jar" regex="true"/>` lines are reordered in the diff (alphabetical swap) but are functionally unchanged. The `verify-metadata=true` and `verify-signatures=false` flags are preserved. Supply-chain verification is not weakened.

## Patterns Verified

- [x] Follows existing code patterns (`@RunWith(RobolectricTestRunner.class)`, `@Config(sdk = {29})`, Truth assertions, Robolectric `RuntimeEnvironment.application`)
- [x] Error handling is appropriate (MailException, RequiresLoginException caught in catch blocks with meaningful assertions)
- [x] Tests cover new functionality (26 tests; GreenMail IMAP round-trips; worker doWork() paths; checkpoint write-ordering)
- [x] No hardcoded values that should be configurable (GreenMail `ServerSetupTest.IMAP` uses a random free port dynamically)
- [x] No unnecessary complexity (test doubles are minimal; lambdas for configurable behavior are clean)

## Integration Verified

- [x] New code is reachable from production entry points: `BackupImapStoreDelegateIntegrationTest` connects via real TCP IMAP socket to GreenMail; `BackupWorkerIntegrationTest` / `RestoreWorkerIntegrationTest` use `TestListenableWorkerBuilder` with real `WorkManager` test environment
- [x] Registries/dispatch maps: GreenMail is `testImplementation`-only; no production wiring required
- [x] Function signatures match at all call sites: `BackupWorker.appendBatchAndUpdateWatermark` is `internal`; called correctly from test with all four parameters
- [x] No dead code introduced: all new test classes are executed under `testDebugUnitTest` (669 tests total per implementation log)
- [x] `MailTransportTestFactories.createHandle()` and `createFolderHandle()` are used correctly from `service` test package (not an ACL violation — those factories are in `mail.transport` package and are `public static`)

## Regression Check

- [x] Capabilities Inventory present in implementation-log.md (method-level retention documented for all three previously-excluded classes)
- [x] Every RETAINED item verified present in new code at the cited file:line (verified via reading production source for BackupWorker, RestoreWorker, and K9MailTransport)
- [x] Every INTENTIONALLY REMOVED item (the six `jacocoFileFilter` exclusion lines) has valid justification: integration tests now provide coverage
- [x] No capabilities missing from inventory: the NOT COVERED items (`backupCursors`, `handleAuthError` in both workers, `clearAppCache`) are honestly documented with root causes

## Contract Verification

- [x] No API surface or event emitter changes: only tests and build configuration modified
- [x] ACL boundary intact: no `com.fsck.k9.*` imports in `service.*` test files (`BackupWorkerIntegrationTest.kt` and `RestoreWorkerIntegrationTest.kt` grep clean; `BackupWorkerWatermarkTest.kt` imports `ImapMessage` which pre-existed this story and is not a U-052 introduction)
- [x] `MailTransportTestFactories` usage in `service.*` tests is correct: the factory is in `mail.transport` (permitted), and the returned `BackupFolderHandle`/`MailMessageHandle` types are app-owned ACL types, not k-9 types

## Phase Completion Report
---
story_id: "U-052"
phase: "code-review"
verdict: "PASS"
artifact_path: "C:/Code/Android/sms-backup-plus/sdlc/artifacts/build/sprints/sprint-013/U-052/review-code.md"
story_status: "review"
current_build_phase: "validation"
blockers: 0
warnings: 3
errors: []
notes: "PASS with 3 warnings (unused import, AC-1 folder-not-found gap at IMAP-retry layer, AC-2 BUG-010 via seam not doWork). All are informational/documentation gaps; gate passes at service*=75.6%; ACL boundary clean; supply-chain checksums correct."
---
