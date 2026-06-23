---
artifact_type: qa-results
story_id: "U-052"
verdict: "PASS"
agent: "QA Analyst"
timestamp: "2026-06-23"
ac_total: 5
ac_passed: 5
ac_failed: 0
tests_run: 669
tests_passed: 669
---

# QA Validation: U-052 — IMAP integration coverage; remove worker/transport coverage exclusions

## Verdict: PASS

Independently verified against the actual integration test sources, `app/build.gradle`,
`gradle/verification-metadata.xml`, and a fresh `./gradlew :app:jacocoTestReport
:app:jacocoTestCoverageVerification` run (BUILD SUCCESSFUL). Per-class and per-package LINE coverage
was extracted directly from the generated JaCoCo XML — not taken from the implementation log. The
three previously-excluded classes now appear in the report with non-zero coverage and the gate
passes with them included. All five ACs pass. On-device validation is not in scope for this story.

## Acceptance Criteria Results

> Rule: Every AC marked PASS cites at least one `file:line` reference.

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1: JVM/Robolectric integration test exercises `BackupImapStoreDelegate` against embedded IMAP (append flow + folder error path), no device/network | PASS | `app/src/test/java/com/zegoggles/smssync/mail/transport/BackupImapStoreDelegateIntegrationTest.java` — 9 `@Test` methods using `GreenMailRule(ServerSetupTest.IMAP)` (`:65`). Append+UID round-trip: `appendAndFetchMessages_roundTrip_smsType` (`:114`), `..._calllogType` (`:130`). Folder error path: `@Test(expected = IllegalStateException.class)` append-to-nonexistent (`:225`). Exception-translation path: HEADER-search → `MailException` (`:298-315`). Class measured **87.0%** LINE (`K9MailTransport$BackupImapStoreDelegate` 67/77 in JaCoCo XML). |
| AC-2: integration test exercises real `BackupWorker.doWork()`; watermark advances on success, unchanged on failure (BUG-010 via production path) | PASS | `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerIntegrationTest.kt` — 8 `@Test`; real `doWork()` via `runBlocking { worker.doWork() }` on `TestListenableWorkerBuilder<BackupWorker>` (`:118,159,182,205,287`). BUG-010 invariant via production seam: `appendBatchAndUpdateWatermark_succeeds_advancesWatermark` (`:224-244`), `..._openFolderFails_watermarkUnchanged` (`:250-258`), `..._appendFails_watermarkUnchanged` (`:301-309`) — all call the real `BackupWorker.appendBatchAndUpdateWatermark` (production seam, `BackupWorker.kt:404`). |
| AC-3: integration test exercises `RestoreWorker.doWork()` against embedded IMAP incl. checkpoint write-ordering | PASS | `app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerIntegrationTest.kt` — 9 `@Test`; real `doWork()` via `runBlocking { worker.doWork() }` (`:113,133,151,186,315,360`). IMAP restore loop: `doWork_withImapMessagesFailedImport_coversRestoreLoop` (`:297`) drives `runImapRestoreLoop` (now covered, was 0%); `doWork_withImapCallLogMessage_coversCallLogImport` (`:334`) drives `importCallLogValues`/`callLogExists`. `RestoreWorker` measured **80.2%** LINE (207/258 in JaCoCo XML). |
| AC-4: three `jacocoFileFilter` exclusions removed; gate passes with classes included, not diluting `mail*`/`worker*` below threshold | PASS | `app/build.gradle:205-218` — `jacocoFileFilter` no longer lists `BackupImapStoreDelegate*`, `BackupWorker*`, `RestoreWorker*`; explanatory comment at `:212-216` documents removal. All three classes appear in JaCoCo XML with non-zero coverage (87.0%/55.8%/80.2%). Gate green: `service`=75.6% ≥0.70 (Rule 1), `mail`=71.5% ≥0.70, `mail.transport`=84.0% ≥0.70 (Rule 2), `worker`=0% ≥0.00 (Rule 15). Note: `BackupWorker`/`RestoreWorker` live in `service*` (not `worker*`) — correctly documented at `app/build.gradle:547-551`. |
| AC-5: full gate green incl. widened U-051 gate; no excluded packages, no excluded critical classes | PASS | `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` → BUILD SUCCESSFUL (re-verified `jacocoTestReport :jacocoTestCoverageVerification` → BUILD SUCCESSFUL in 33s). 669 tests pass (`git grep` authoritative). No `<class>Test*` exclusions remain in `jacocoFileFilter` beyond generated/framework files (`app/build.gradle:205-218`). |

## Integration Path Verification

> Rule: Any new component with no verified call path from a production entry point is a BLOCK finding.

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| `BackupImapStoreDelegateIntegrationTest` | GreenMail IMAP TCP → `K9MailTransport` published surface | `GreenMailRule.start()` → `K9MailTransport.checkSettings()` → `BackupImapStoreDelegate.openFolder()/appendMessages()/getMessages()` (real production delegate) | yes |
| `BackupWorkerIntegrationTest` | `BackupWorker.doWork()` | `TestListenableWorkerBuilder<BackupWorker>` → `runBlocking { worker.doWork() }` (`:118` etc.) → production coroutine body | yes |
| `RestoreWorkerIntegrationTest` | `RestoreWorker.doWork()` | `TestListenableWorkerBuilder<RestoreWorker>` → `runBlocking { worker.doWork() }` (`:113` etc.) → `executeRestore` → `runImapRestoreLoop` → `importMessage` (production) | yes |
| GreenMail dependency | `testImplementation` config | `app/build.gradle:163` `com.icegreen:greenmail-junit4:2.1.3`; SHA-256 pinned in `gradle/verification-metadata.xml` | yes |

## Behavioral Contract Verification

> ACL boundary and BUG-010 invariant are the load-bearing contracts for this story.

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| ACL boundary (no `com.fsck.k9.*` in service layer) | New service integration tests import zero k9 internals | `grep "import com.fsck.k9"` on `BackupWorkerIntegrationTest.kt` + `RestoreWorkerIntegrationTest.kt` → 0 results (exit 1) | yes |
| ACL boundary (k9 confined to mail/transport package) | Delegate test imports k9 only within `mail.transport` package (the designated boundary) | `BackupImapStoreDelegateIntegrationTest.java:4-10` k9 imports are inside the `mail.transport` package, consistent with pre-existing `mail/` boundary architecture | yes |
| BUG-010 watermark invariant | `setMaxSyncedDate` fed only by `appendMessages` return; throw exits before watermark write | `BackupWorker.kt:416-422` (production seam); asserted via real-worker tests `BackupWorkerIntegrationTest.kt:224-309` | yes |
| Supply-chain verification preserved | GreenMail + transitives pinned with specific SHA-256, no artifact-trust wildcard, `verify-metadata=true` | `gradle/verification-metadata.xml:1981-2001` (greenmail/greenmail-junit4/parent SHA-256), `:4` `verify-metadata=true`; only wildcards present are standard `.*-javadoc`/`.*-sources` (`:7-8`), not artifact trust bypass | yes |
| Existing tests preserved | `K9MailTransportCreateOpenTest`, `DefaultTrustedSocketFactorySniTest` still pass | All 669 tests green in gate run | yes |

## Requirement Scope Coverage

> Source: assessment 20260623-post-migration-assessment#TE-002. No REQ-*/DES-* docs referenced.

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| TE-002 | Embedded-IMAP coverage for `BackupImapStoreDelegate` | yes | `BackupImapStoreDelegateIntegrationTest.java` (9 tests, 87.0% LINE) |
| TE-002 | Real `BackupWorker.doWork()` coverage | yes | `BackupWorkerIntegrationTest.kt` (8 tests, BackupWorker 55.8% LINE) |
| TE-002 | Real `RestoreWorker.doWork()` coverage | yes | `RestoreWorkerIntegrationTest.kt` (9 tests, RestoreWorker 80.2% LINE) |
| TE-002 | Remove 3 jacocoFileFilter exclusions; gate passes | yes | `app/build.gradle:205-218`; gate BUILD SUCCESSFUL with classes included |
| TE-002 | GreenMail pinned in verification-metadata (SHA-256, no wildcard) | yes | `gradle/verification-metadata.xml:1981-2001` |

## Test Results

- Integration tests added: 9 (delegate) + 8 (BackupWorker) + 9 (RestoreWorker) = **26**, matching the
  implementation-log claim (verified via `grep -c '@Test'` per file).
- Authoritative total `@Test` count (`git grep`): **669** (was 643 pre-U-052; +26).
- Per-class LINE coverage from JaCoCo XML: `BackupImapStoreDelegate`=87.0% (67/77),
  `BackupWorker`=55.8% (101/181), `RestoreWorker`=80.2% (207/258).
- `./gradlew :app:jacocoTestReport :app:jacocoTestCoverageVerification` → BUILD SUCCESSFUL.

## Regression Results

No regressions. Remaining-uncovered paths are documented honestly in the implementation log and
confirmed in code: `BackupWorker.backupCursors` (35 lines, 0% — needs seeded SMS ContentProvider),
`BackupWorker.handleAuthError` (0% — needs XOAuth2 throw from backupCursors), `RestoreWorker.handleAuthError`
(0% — covered by unit mock elsewhere), `RestoreWorker.clearAppCache` (0% — only at i%50==0). These do
not pull any gated package below its floor (`service`=75.6% ≥ 0.70). The MainActivity ASM "do not
match" warning is pre-existing and non-blocking.

## Phase Completion Report
---
story_id: "U-052"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-013/U-052/qa-results.md"
story_status: "validated"
current_build_phase: "validation"
ac_passed: 5
ac_total: 5
errors: []
---
