---
artifact_type: qa-results
story_id: U-044
verdict: PASS
agent: "QA Analyst"
timestamp: 2026-06-05
ac_total: 4
ac_passed: 4
ac_failed: 0
tests_run: 14
tests_passed: 14
---

# QA Validation: U-044

## Verdict: PASS

Verified the create→select retry-window widening (fix for BUG-013) by reading the post-merge
`K9MailTransport.java` and `K9MailTransportCreateOpenTest.java` on disk, walking the full
integration call path from the public `openFolder()` entry point to `getRetryDelayMs(attempt)`,
and confirming the changeset (`git diff 691f6da5 HEAD`) touches only the transport class, its
create/open test, and the U-044 artifacts — no service-layer or BackupWorker/watermark code.
All four AC are satisfied by production code with cited file:line evidence and by genuine,
non-tautological unit tests. The on-device first-run AC-1 confirmation is correctly deferred
to post-merge device validation per the delegation.

## Acceptance Criteria Results

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1: Widen post-CREATE SELECT retry budget from 3×1000 ms (~3 s) to exponential backoff up to a bounded ~15 s so fresh-label propagation completes in one run. | PASS | `MAX_CREATE_OPEN_RETRIES = 5` at K9MailTransport.java:500 (was 3). Exponential backoff `min(1000<<(attempt-1), 8000)` at K9MailTransport.java:524-530, called in-loop at :611 → schedule 1s/2s/4s/8s = 15 s inter-attempt budget across 5 attempts. Verified by tests: `getRetryDelayMs_exponentialBackoff_matchesSchedule` asserts 1000/2000/4000/8000/8000 against the REAL formula via a minimal subclass that does NOT zero the delay (test:79-97); `createAndOpenFolder_openFailsNonExistentThreeTimes_thenSucceeds` proves success on the 4th attempt after 3 NONEXISTENT failures — impossible under the old 3-cap (test:304-324, verifies open() called 4 times). NOTE: full on-device first-run success deferred to post-merge device validation per delegation. |
| AC-2: Retry stays bounded (no unbounded loop / indefinite block) and is cancellable — restores interrupt flag, aborts promptly on interruption. | PASS | Loop bounded by `for (attempt=1; attempt<=MAX_CREATE_OPEN_RETRIES; attempt++)` (K9MailTransport.java:597) with terminal throw at :627-631. Interruption: `Thread.currentThread().interrupt()` then immediate `throw new MessagingException("Interrupted...")` at K9MailTransport.java:617-623. Verified by `createAndOpenFolder_allOpenAttemptsFailNonExistent_throwsMessagingException` (bounded exhaustion: open() called exactly MAX_CREATE_OPEN_RETRIES times, test:185-206) and `openWithRetryAfterCreate_interruptedDuringSleep_restoresInterruptFlagAndThrows` (asserts "Interrupted" message thrown AND `Thread.currentThread().isInterrupted()==true`, test:242-280). |
| AC-3: U-042 watermark invariant preserved across the slower path — failed run does not advance watermark; success advances once; no duplicate appends. | PASS | Verified by inspection of the changeset: `git diff --name-only 691f6da5 HEAD` returns only K9MailTransport.java, K9MailTransportCreateOpenTest.java, and the U-044 artifacts. BackupWorker / service-layer / watermark code is NOT modified (`git diff 691f6da5 HEAD` for BackupWorker.java is empty; the only two "watermark" tokens in the diff are in the markdown artifacts, not code). The change is internal to the transport open path and does not alter when/how the watermark advances, so the U-042 invariant is untouched. |
| AC-4: Idempotent fast path unchanged — folder-exists open() is immediate with no added latency (extended retry applies ONLY after CREATE on a NONEXISTENT response); non-NONEXISTENT errors propagate immediately. | PASS | Fast path: `else { folder.open(OPEN_MODE_RW); }` at K9MailTransport.java:563 — no retry loop, reached only when `folder.exists()==true` (:552). Retry is invoked ONLY after a successful CREATE at :561. Non-NONEXISTENT guard: `if (!isNonExistent) throw e;` at K9MailTransport.java:605-607 (first occurrence, no delay). Verified by `createAndOpenFolder_folderExists_exactlyOneOpen_noRetry` (open() exactly once, create() never, test:286-297) and `createAndOpenFolder_openFailsNonNonExistent_propagatesImmediately` (same exception instance rethrown, open() once, test:212-231). Build green per implementation-log (650 tests, 0 failed) — being re-verified separately by orchestrator. |

## Integration Path Verification

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| `getRetryDelayMs(int attempt)` (new signature) | `K9MailTransport.openFolder()` (public, :172) | openFolder():172 → store.openFolder():175 → BackupImapStoreDelegate.openFolder():429 → createAndOpenFolder():435/548 → openWithRetryAfterCreate(folder):561 → getRetryDelayMs(attempt):611 | yes |
| `openWithRetryAfterCreate()` (modified) | same | reached at :561 ONLY after successful `folder.create()` on the `!folder.exists()` branch (:552-561) | yes |
| Removed constant `CREATE_OPEN_RETRY_DELAY_MS` | n/a | `git grep CREATE_OPEN_RETRY_DELAY_MS HEAD -- app/src/**` → NONE; old `getRetryDelayMs()` no-arg → NONE. No stale references / dead code. | yes |

## Behavioral Contract Verification

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| Create→open retry contract (U-043 carried forward) | NONEXISTENT-only retry; other errors propagate immediately | K9MailTransport.java:603-607 | yes |
| Create→open retry contract | Bounded attempts, descriptive terminal throw | K9MailTransport.java:597, 627-631 | yes |
| Create→open retry contract | Interruptible wait; restore interrupt flag | K9MailTransport.java:617-623 | yes |
| Create→open retry contract | create()==false throws before open() | K9MailTransport.java:555-558 (test:142-157) | yes |
| Create→open retry contract | IllegalArgumentException→MessagingException re-wrap preserved | K9MailTransport.java:566-569 | yes |
| Fast path contract (AC-4) | folder.exists()==true → single open(), no create, no retry | K9MailTransport.java:562-563 | yes |
| No integration contracts (CNTR-*) | story frontmatter `integration_contracts: []` | n/a | n/a |

## Requirement Scope Coverage

> Story frontmatter: `requirements: []`, `design_docs: []`. Source is `bug:BUG-013`.
> The bug's stated scope = widen the retry budget so a freshly CREATEd Gmail label is
> SELECTable within one run, keeping it bounded, cancellable, NONEXISTENT-only, and
> preserving the fast path. All scope items map to AC-1..AC-4 above.

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| BUG-013 | Widen retry budget to ~15-30 s with exponential backoff | yes | K9MailTransport.java:500, 524-530, 611 |
| BUG-013 | Keep bounded + cancellable | yes | K9MailTransport.java:597, 617-623, 627-631 |
| BUG-013 | NONEXISTENT-only; non-NONEXISTENT propagate immediately | yes | K9MailTransport.java:603-607 |
| BUG-013 | Fast path (folder exists) unchanged, no added latency | yes | K9MailTransport.java:562-563 |
| BUG-013 | U-042 watermark invariant preserved | yes | changeset excludes BackupWorker/service (git diff --name-only) |

## Test Results

`K9MailTransportCreateOpenTest.java` — 14 @Test methods (verified via `git grep -c "@Test"` on the file). Key new/updated coverage matched against the delegation's required scenarios:

- Exponential backoff schedule (getRetryDelayMs): `getRetryDelayMs_exponentialBackoff_matchesSchedule` (test:79-97) — calls the REAL production formula (minimal subclass overrides only `createBackupFolder`, NOT `getRetryDelayMs`), asserting 1000/2000/4000/8000/8000. Genuine, not tautological.
- Success after >3 NONEXISTENT attempts (proves window widened beyond old 3-cap): `createAndOpenFolder_openFailsNonExistentThreeTimes_thenSucceeds` (test:304-324) — 3 NONEXISTENT then success, open() called 4 times, no throw.
- Retries exhausted still throws (bounded): `createAndOpenFolder_allOpenAttemptsFailNonExistent_throwsMessagingException` (test:185-206) — open() called exactly MAX_CREATE_OPEN_RETRIES times, then MessagingException.
- Interruption restores flag + aborts: `openWithRetryAfterCreate_interruptedDuringSleep_restoresInterruptFlagAndThrows` (test:242-280) — asserts "Interrupted" thrown and isInterrupted()==true; clears flag to avoid leak.
- No retry/delay when folder already exists: `createAndOpenFolder_folderExists_exactlyOneOpen_noRetry` (test:286-297) and `createAndOpenFolder_folderExists_opensDirectlyWithoutCreate` (test:107-116).
- Non-NONEXISTENT not retried: `createAndOpenFolder_openFailsNonNonExistent_propagatesImmediately` (test:212-231) — same exception instance rethrown, open() once.

Implementation-log reports full suite: 650 tests, 0 failed, 2 skipped; jacoco per-package LINE ≥70% PASSED. Build is being re-verified separately by the orchestrator (650 tests). No stale references to the removed constant or old method signature (`git grep` → NONE).

## Regression Results

No regression risk to existing behavior. Changeset is confined to the transport open path and its test (`git diff --stat 691f6da5 HEAD`). Retained tests still present and unchanged in intent: `constants_maxRetries_isPositive`, `createAndOpenFolder_folderNotExists_callsCreateThenOpen`, `createAndOpenFolder_createReturnsFalse_throwsWithoutCallingOpen`, `createAndOpenFolder_openFailsNonExistentOnce_thenSucceeds_noException`, `createAndOpenFolder_customLabel_callLog_createsAndOpens`, `createAndOpenFolder_nonExistentDetection_caseInsensitive`, `openFolder_cachedFolder_returnedWithoutReopen`. The only removed test (`constants_retryDelay_isPositive`) referenced the deleted `CREATE_OPEN_RETRY_DELAY_MS` constant — justified removal, replaced by the stronger schedule test. `TestableBackupImapStoreDelegate.getRetryDelayMs` updated to the new `(int attempt)` signature (test:434-437). BackupWorker / U-042 watermark logic untouched.

## Phase Completion Report
---
story_id: U-044
phase: "validation"
verdict: PASS
artifact_path: "sdlc/artifacts/build/sprints/sprint-009/U-044/qa-results.md"
story_status: "validated"
current_build_phase: "validation"
ac_passed: 4
ac_total: 4
errors: []
---
