---
artifact_type: plan
story_id: "U-042"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-05"
contracts_verified:
  - CNTR-MODERNIZATION-007
risks_identified: 1
---

# Implementation Plan: U-042

## Story Overview

Fix BUG-010: `max_synced_date` watermark advances even when no IMAP append was confirmed,
causing subsequent backup runs to silently skip messages forever ("nothing to backup").

**Root cause (file:line):** `BackupWorker.kt:344` — `preferences.dataTypePreferences.setMaxSyncedDate(cursor.type, result.maxDate)` was called after `transport.appendMessages(folder, result)` regardless of whether the append succeeded. The old `appendMessages` was `void`; silent k-9 failures (e.g. folder NONEXISTENT) returned normally without appending but also without throwing, so the watermark advanced on the pre-computed `result.maxDate` even though nothing was appended.

**Fix strategy:** Change `MailTransport.appendMessages()` return type from `void` to `long` (the confirmed max-date). Callers now receive the confirmed date as a compiler-enforced return value and use it as the watermark. If `appendMessages` throws (any failure), the watermark write is unreachable. This makes "watermark only advances on confirmed append" a language-enforced invariant.

## Acceptance Criteria Mapping

| AC | Implementation Step | Notes |
|----|---------------------|-------|
| AC-1: failure never advances watermark | `MailTransport.appendMessages` changed to `long`; `setMaxSyncedDate` is only reachable after the confirmed return | Compiler-enforced: if `appendMessages` throws, control never reaches `setMaxSyncedDate` |
| AC-2: watermark = max confirmed message date | `K9MailTransport.appendMessages` returns `result.getMaxDate()` after k-9 append returns; callers use this value verbatim | Never uses wall-clock; value comes from message DATE headers |
| AC-3: regression tests for 3 scenarios | `BackupWorkerWatermarkTest.kt` (6 tests) covers (a) failure→unchanged, (b) success→confirmed date, (c) partial success→batch-1 date | `K9MailTransportTest` (2 tests) covers adapter layer |
| AC-4: build green | `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` passes | Jacoco per-package LINE ≥70% maintained |

## Implementation Steps

1. **`MailTransport.java`** — Change `appendMessages` signature from `void` to `long`. Add Javadoc describing the confirmed-append contract.

2. **`K9MailTransport.java`** — Change `appendMessages` implementation to return `result.getMaxDate()` after `folder.folder.appendMessages(messages)` completes normally. Exception translation preserved.

3. **`BackupWorker.kt`** — Change call site in `backupCursors()` to capture the return value: `val confirmedMaxDate: Long = transport.appendMessages(folder, result)`. Pass `confirmedMaxDate` to `setMaxSyncedDate` instead of `result.maxDate`. Update KDoc.

4. **`K9MailTransportTest.java`** — Add two tests:
   - `appendMessages_success_returnsConfirmedMaxDate`: confirms return value = message DATE header epoch ms
   - `appendMessages_emptyResult_returnsDefaultMaxDate`: confirms empty result returns -1

5. **`BackupWorkerWatermarkTest.kt`** — New test class with 6 AC-3 regression tests using `StubMailTransport` and `processWatermarkLoop` helper. Tests cover AC-3(a) failure→unchanged (3 variants), AC-3(b) success→confirmed date (2 variants), AC-3(c) partial success→batch-1 date.

6. **`SmsBackupServiceTest.java`** — Add `WorkManagerTestInitHelper.initializeTestWorkManager` to `@Before` to fix a pre-existing flaky test (test-order-dependent `IllegalStateException` when WorkManager is not initialized in the test JVM worker; unmasked by the new test class changing JVM load distribution).

## Files Modified/Created

**Modified:**
- `app/src/main/java/com/zegoggles/smssync/mail/transport/MailTransport.java`
- `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java`
- `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt`
- `app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportTest.java`
- `app/src/test/java/com/zegoggles/smssync/service/SmsBackupServiceTest.java`

**Created:**
- `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerWatermarkTest.kt`

## Contracts Verification

**CNTR-MODERNIZATION-007 (ACL port invariant):**
- `MailTransport.java` uses only app-owned types in all method signatures. The `long` return type of `appendMessages` is a Java primitive — no k-9 type.
- `BackupWorker.kt` imports only `MailTransport`, `BackupFolderHandle`, and `MailException` from `mail.transport` — no k-9 imports remain.
- `K9MailTransport.java` remains the sole class importing k-9 types.

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| k-9 `ImapFolder.appendMessages()` is not void — Mockito `doNothing()` fails | Use `when(mockFolder.appendMessages(any())).thenReturn(Collections.emptyMap())` in tests |

## Test Strategy

- AC-3(a): `StubMailTransport` configured to throw `MailException` from `openFolder` or `appendMessages`; assert watermark remains -1
- AC-3(b): `StubMailTransport` returns `SMS_DATE_1` (1_700_000_001_000L) from `appendMessages`; assert watermark = `SMS_DATE_1`
- AC-3(c): `StubMailTransport` returns `SMS_DATE_1` on call 1, throws `MailException` on call 2; assert watermark = `SMS_DATE_1` (batch 1 committed, batch 2 not)
- Adapter: `K9MailTransportTest` uses real `ImapFolder` mock with DATE header; asserts return value matches

## Phase Completion Report
---
story_id: "U-042"
phase: "planning"
verdict: "PASS"
artifact_path: "C:/Code/Android/sms-backup-plus/.claude/worktrees/agent-a0998b912c8fe60be/sdlc/artifacts/build/sprints/sprint-008/U-042/plan.md"
story_status: "in-progress"
current_build_phase: "implementation"
errors: []
---
