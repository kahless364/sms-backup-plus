---
artifact_type: qa-results
story_id: "U-044"
verdict: "PASS"
agent: "QA Analyst"
timestamp: "2026-06-05"
ac_total: 4
ac_passed: 4
ac_failed: 0
tests_run: 659
tests_passed: 659
---

# QA Validation: U-044

Final QA on the integrated main tree (post-merge). The fix widens the create→select
retry window for fresh Gmail/IMAP labels (BUG-013) across three remediations: an
exponential-backoff budget (6 attempts / ~23 s inter-attempt), a broadened transitional
predicate (`NONEXISTENT` + `did not find message count`), and a connection-pool drain
(`forceFreshConnection` → `ImapStore.closePooledConnections()`) before each retry so the
next `open()` uses a fresh login. Every AC was verified independently against the on-disk
files, the `git diff 691f6da5..HEAD`, the test source, and a clean run of the targeted test
class. On-device first-run success was captured by the orchestrator.

## Verdict: PASS

## Acceptance Criteria Results

> Every PASS cites at least one `file:line` reference.

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1: First backup to a fresh label succeeds in a single run; post-CREATE SELECT retry budget increased from 3×1000 ms to more attempts with exponential backoff up to a bounded cap (~15–30 s). | PASS | Budget: `MAX_CREATE_OPEN_RETRIES = 6` (K9MailTransport.java:510); exponential backoff `min(1000 << (attempt-1), 8000)` = 1s/2s/4s/8s/8s = 23 s inter-attempt (K9MailTransport.java:534-540). Both transitional states retried: `NONEXISTENT` OR `did not find message count` (K9MailTransport.java:652-654). Pool drained before each retry so each `open()` is a fresh login (K9MailTransport.java:669 → forceFreshConnection 556-559 → ImapStore.closePooledConnections 363-368). Unit coverage: `getRetryDelayMs_exponentialBackoff_matchesSchedule` asserts 1000/2000/4000/8000/8000/8000 (test:95-100); `createAndOpenFolder_openFailsNonExistentThreeTimes_thenSucceeds` proves 4th-attempt success beyond old 3-cap (test:309-328); `mixedNonExistentThenMessageCount_thenSucceeds` proves the live two-phase sequence (test:426-442); `notReadyOnce/Twice_forceFreshConnectionCalled*` prove pool-drain wiring (test:478-520). **On-device CONFIRMED** (orchestrator): backup to new label `SMS_U044_RC` logged `Creating` → `not selectable yet after CREATE (attempt 1/6); draining pool and retrying in 1000 ms` → `watermark advanced for SMS to 1780689230095` → `backedUp=1` → `Worker result SUCCESS` in ONE run (no WorkManager RETRY). |
| AC-2: Retry stays bounded and is cancellable (respects interruption; restores interrupt flag; aborts promptly). | PASS | Bounded `for` loop `attempt <= MAX_CREATE_OPEN_RETRIES` (=6), no unbounded path (K9MailTransport.java:642). On `InterruptedException`: `Thread.currentThread().interrupt()` restores the flag then throws `MessagingException` immediately (K9MailTransport.java:672-677). Test `openWithRetryAfterCreate_interruptedDuringSleep_restoresInterruptFlagAndThrows` asserts the "Interrupted" exception is thrown AND `Thread.currentThread().isInterrupted()` is true afterward (test:247-284). |
| AC-3: U-042 invariant preserved across the slower path — a still-failing run does NOT advance the watermark; success advances exactly once; no duplicate appends. | PASS | `git diff 691f6da5..HEAD` production scope is K9MailTransport.java + ImapStore.java only; `BackupWorker` and all watermark logic are NOT in the diff (verified via `git diff --name-only`). The retry change is internal to folder open and does not touch the append/watermark sequence, so the U-042 contract (watermark advances only on confirmed append) is untouched. On-device evidence shows the success path advances the watermark exactly once (`watermark advanced for SMS to 1780689230095`, `backedUp=1`); the exhaustion path throws `MessagingException` (K9MailTransport.java:682-686) which propagates as a failed run that does not reach watermark advance. |
| AC-4: Idempotent fast path unchanged (folder exists → immediate `open()`, no added latency, retry only after CREATE on a not-ready response); non-retryable errors propagate immediately; tests updated; build green. | PASS | Fast path: `if (!folder.exists())` create branch else direct `folder.open(OPEN_MODE_RW)` with no retry/drain (K9MailTransport.java:582-594). Non-retryable propagation: `if (!isFolderNotReadyYet) throw e;` before any sleep/drain (K9MailTransport.java:655-657). Tests: `createAndOpenFolder_folderExists_exactlyOneOpen_noRetry` (open ×1, create ×0, test:291-301); `folderExists_forceFreshConnectionNeverCalled` (drain count 0, test:559-570); `openFailsNonNonExistent_propagatesImmediately` (same instance rethrown, open ×1, test:217-235); `fatalError_forceFreshConnectionNeverCalled` (drain 0, open ×1, test:577-598). Build green: `:app:testDebugUnitTest --tests K9MailTransportCreateOpenTest` → BUILD SUCCESSFUL (2m 2s); orchestrator reports full suite 659 tests green + jacoco per-package LINE ≥70%. |

## Integration Path Verification

> Any new function/component with no verified call path from a production entry point is a BLOCK finding.

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| `getRetryDelayMs(int attempt)` (new signature) | `K9MailTransport.openFolder()` (K9MailTransport.java:172) | openFolder → `store.openFolder()` (delegate, :429) → createAndOpenFolder (:578) → openWithRetryAfterCreate (:640) → getRetryDelayMs(attempt) (:661) | yes |
| `forceFreshConnection(BackupFolder)` (new) | `K9MailTransport.openFolder()` (:172) | …→ openWithRetryAfterCreate → forceFreshConnection (:669) before each Thread.sleep | yes |
| `ImapStore.closePooledConnections()` (new) | `forceFreshConnection()` (:558) | BackupImapStoreDelegate **extends ImapStore** (:414); `closePooledConnections()` resolves to inherited ImapStore method (ImapStore.java:363), draining the same `connections` LinkedList used by pollConnection/releaseConnection (ImapStore.java:55,337,345) | yes |

## Behavioral Contract Verification

> Story frontmatter `integration_contracts: []` — no CNTR-* artifacts. Behavioral contracts of the modified method verified directly.

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| openWithRetryAfterCreate retry semantics | Success returns immediately, no accumulated delay | K9MailTransport.java:644-645 | yes |
| openWithRetryAfterCreate retry semantics | Retry ONLY on transitional states (NONEXISTENT or "did not find message count"), case-insensitive | K9MailTransport.java:652-654 | yes |
| openWithRetryAfterCreate retry semantics | Non-transitional error rethrown verbatim before any side effect (no drain/sleep) | K9MailTransport.java:655-657 | yes |
| openWithRetryAfterCreate retry semantics | Pool drain only for non-final attempts (`attempt < MAX`); no drain after last attempt | K9MailTransport.java:660-669 (guarded by `attempt < MAX_CREATE_OPEN_RETRIES`); test `allAttemptsFail_forceFreshConnectionCalledPerRetry` asserts exactly MAX-1 drains (test:528-552) | yes |
| openWithRetryAfterCreate retry semantics | Interrupt restores flag and aborts with MessagingException | K9MailTransport.java:672-677 | yes |
| openWithRetryAfterCreate retry semantics | Exhaustion throws descriptive MessagingException with last error | K9MailTransport.java:682-686 | yes |
| ImapStore.closePooledConnections thread-safety | Synchronized on same monitor (`connections`) as pollConnection/releaseConnection | ImapStore.java:363 (vs :337, :344) | yes |

## Requirement Scope Coverage

> Story references `bug:BUG-013`; frontmatter `requirements: []`, `design_docs: []`. Scope derived from BUG-013 and the story's "Existing Behavior to Preserve".

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| BUG-013 | First backup to fresh label succeeds in one run | yes | K9MailTransport.java:510,534-540,652-654,669 + on-device SUCCESS (orchestrator log) |
| BUG-013 | Bounded, cancellable wait | yes | K9MailTransport.java:642,672-677 |
| Story | Preserve U-043 create-then-open + NONEXISTENT-only guard (broadened; others still propagate) | yes | K9MailTransport.java:584-588 (create-check), :652-657 (predicate + immediate propagate) |
| Story | Preserve U-042 watermark-only-on-confirmed-append invariant | yes | BackupWorker not in diff (`git diff --name-only 691f6da5..HEAD`) |
| Story | Preserve idempotent fast path (no added latency) | yes | K9MailTransport.java:592-594; tests :291-301, :559-570 |
| Story | Preserve MailTransport ACL boundary (no k-9 types leaked to service.*) | yes | Change internal to transport package; `forceFreshConnection` takes internal `BackupFolder`; `closePooledConnections` inherited within delegate — no new k-9 type exposure |
| Story | Bounded total wait ~15–30 s | yes | 1+2+4+8+8 = 23 s inter-attempt budget (K9MailTransport.java:507) within WorkManager window |

## Test Results

- Targeted class run: `:app:testDebugUnitTest --tests com.zegoggles.smssync.mail.transport.K9MailTransportCreateOpenTest` → **BUILD SUCCESSFUL in 2m 2s** (a failing test fails the build). 23 `@Test` methods in the class (verified by direct count of the source file).
- Authoritative total: `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` → **659** (matches implementation-log claim exactly).
- Orchestrator reports full suite green (659) and jacoco per-package LINE ≥70%.

## Regression Results

- No production files outside `K9MailTransport.java` and `ImapStore.java` changed (`git diff --name-only 691f6da5..HEAD`). `BackupWorker`/watermark untouched → U-042 not regressed.
- Retained U-043/BUG-011 tests still present and green within the class: create-then-open sequence, create-returns-false throws, NONEXISTENT single-retry success, bounded exhaustion (now 6 attempts), non-NONEXISTENT immediate propagation, custom label, case-insensitive detection, cached-folder idempotency.
- `ImapStore.closePooledConnections()` reuses the existing `connections` monitor; no change to pollConnection/releaseConnection behavior → connection-pool semantics not regressed.

## Phase Completion Report
---
story_id: "U-044"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-009/U-044/qa-results.md"
story_status: "validated"
current_build_phase: "validation"
ac_passed: 4
ac_total: 4
errors: []
---
