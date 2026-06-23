---
type: story
status: done
artifact_type: user-story
priority: high
complexity: high
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-052
title: Add IMAP integration test coverage and remove worker/transport coverage exclusions
pipeline: ''
domain: modernization
requirement_source: assessment:20260623-post-migration-assessment#TE-002
sprint: '000013'
updated_at: '2026-06-23T22:24:20.922Z'
resolution: done
---

# U-052: Add IMAP integration test coverage and remove worker/transport coverage exclusions

## Story
As a maintainer of the SMS Backup+ codebase, I want JVM-based IMAP integration tests (using GreenMail or an equivalent embedded IMAP server running under Robolectric) to cover `K9MailTransport$BackupImapStoreDelegate`, `BackupWorker`, and `RestoreWorker`, so that the `jacocoFileFilter` exclusions for those classes can be removed and the highest-risk IMAP protocol and backup/restore execution code is subject to the coverage gate.

## Source
Derived from assessment 20260623-post-migration-assessment, finding TE-002. See `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/testing.md`.

## Acceptance Criteria
- [ ] AC-1: At least one JVM/Robolectric integration test exercises `K9MailTransport$BackupImapStoreDelegate` against an embedded IMAP server (GreenMail or equivalent); the test covers the IMAP append flow (folder open → message append → UID retrieval) and the folder-not-found error path; both run inside `testDebugUnitTest` without requiring a device or network.
- [ ] AC-2: At least one JVM/Robolectric integration test exercises the real `BackupWorker.doWork()` path (not a copy of the logic): a worker is constructed, run against a stubbed `MailTransportFactory` and embedded IMAP, and the test asserts that successfully appended messages advance the watermark and that a failed append leaves the watermark unchanged (BUG-010 invariant via the production code path).
- [ ] AC-3: At least one JVM/Robolectric integration test exercises `RestoreWorker.doWork()` against the embedded IMAP server: messages fetched and inserted into the device store (mocked or Robolectric content resolver), including the durable checkpoint write-ordering (`RestoreWorker.kt:455-459`).
- [ ] AC-4: The `jacocoFileFilter` exclusions for `K9MailTransport$BackupImapStoreDelegate*`, `BackupWorker*`, and `RestoreWorker*` are removed from `app/build.gradle`; after removal `./gradlew :app:jacocoTestCoverageVerification` passes with the newly-covered classes contributing to (and not diluting below threshold) the `mail*`/`worker*` package LINE gates.
- [ ] AC-5: Build green: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` — the full gate (including the widened gate from U-051) passes with no excluded packages and no excluded critical classes.

## Affected Code
| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/build.gradle` (`jacocoFileFilter` exclusions block) | Excludes `K9MailTransport$BackupImapStoreDelegate*`, `BackupWorker*`, `RestoreWorker*` from coverage measurement | Remove those three exclusions after integration tests achieve coverage |
| `app/src/test/java/.../mail/` (new test files) | No embedded-IMAP integration tests exist | Add `BackupImapStoreDelegateIntegrationTest` (or similar) using GreenMail |
| `app/src/test/java/.../service/` (new test files) | `BackupWorkerWatermarkTest` uses a local re-implementation of `processWatermarkLoop` | Add worker integration tests that invoke the real `doWork()` against embedded IMAP |
| `app/build.gradle` (test dependencies) | GreenMail or equivalent not declared | Add `testImplementation 'com.icegreen:greenmail:...'` (or `greenmail-junit5`); pin in `gradle/verification-metadata.xml` |

## Existing Behavior to Preserve
- The ACL boundary is airtight: zero `com.fsck.k9.*` imports outside `K9MailTransport.java`. Integration tests must not import k-9 internals directly; they must use `K9MailTransport` and `BackupImapStoreDelegate` through the published surface.
- `MailTransportFactory` test-seam must remain usable for fault injection in unit tests; integration tests add a layer, they do not replace the unit tests.
- The existing `K9MailTransportCreateOpenTest` and `DefaultTrustedSocketFactorySniTest` must continue to pass.
- BUG-010 watermark invariant: the new `BackupWorker` integration test must confirm (not just assert in a local helper) that the real `appendMessages` return value is the only thing that feeds `setMaxSyncedDate`.

## Verification Steps
1. Run `./gradlew :app:testDebugUnitTest` — all existing 672+ tests pass; new integration tests appear in the test report under their new class names.
2. Run `./gradlew :app:jacocoTestReport` — open the HTML report; confirm `BackupImapStoreDelegate`, `BackupWorker`, `RestoreWorker` appear in the report with non-zero LINE coverage (they are no longer excluded).
3. Run `./gradlew :app:jacocoTestCoverageVerification` — gate passes; `worker*` package is now covered and above its threshold (set in U-051).
4. Remove one of the new integration test methods temporarily and re-run the gate — confirm it fails on the `mail*` or `worker*` threshold; restore the test.
5. Confirm GreenMail dependency is pinned in `gradle/verification-metadata.xml` (SHA-256 present) so supply-chain verification passes.

## Technical Context
- TE-002 root cause: `BackupImapStoreDelegate` performs the real IMAP APPEND/UID-SEARCH protocol; `BackupWorker`/`RestoreWorker` are the actual execution bodies. Both were excluded from the gate because there were no JVM-runnable tests for them — on-device validation was the only coverage.
- GreenMail provides an in-JVM IMAP server that accepts real IMAP commands; it is the standard Android/JVM test approach for mail protocol code. Alternatives: `mock-javaMail` (less realistic), `fake-smtp-server` (SMTP only — not suitable).
- WorkManager test support: `androidx.work:work-testing` provides `TestWorkerBuilder` for constructing workers in a JVM test context; combine with a GreenMail instance and a Robolectric content resolver for a fully in-JVM integration test.
- Do NOT simply remove the `jacocoFileFilter` exclusions without adding coverage first — the gate will fail on zero-covered critical code. Coverage must come before exclusion removal (this is the sequencing noted in the health report's "What NOT to Do" section).
- This story depends conceptually on U-051 because U-051 sets the `worker*` package threshold that the newly-uncovered worker classes will count against.

## Notes
- Sprint B (Test Visibility & Coverage). Sequence after U-051.
- This is the highest-effort story in the testing sprint; plan for embedded-IMAP setup complexity and WorkManager test-builder wiring.
- U-053 (real BackupWorker seam) is complementary: U-052 adds integration tests that invoke `doWork()`, while U-053 extracts an internal seam that makes the watermark-gating loop directly testable without requiring a full worker run.
