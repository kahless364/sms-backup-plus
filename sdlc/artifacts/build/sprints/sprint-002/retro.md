---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-002
started: 2026-06-04
completed: 2026-06-04
---

# Retro: sprint-002 — EPIC-MODERNIZATION-005 Debt Closure

## Business Summary

**Objective.** Close the three documented, bounded pieces of technical debt deferred during sprint-001 (the 29-story SMS Backup+ modernization), reaching the full architectural target state: zero k-9 leakage into the engine, a single CoroutineWorker execution path, and complete Hilt construction.

**Outcome.** All 3 stories completed (`done`) across 3 sequential dependency waves, each with 5/5 PASS workspace artifacts (15/15 total). The merged tree passed `assembleDebug` + `testDebugUnitTest` + `jacocoTestCoverageVerification` (per-package LINE ≥70% on `service*`/`mail*`/`auth*`) at every wave. Final suite: **588 tests green**. On-device launch smoke (emulator-5554, API 37) passed after U-031 and U-032 with no crash, no `ClassNotFound` from deletions, and no Hilt `MissingBinding`.

**Key Deliverables.**
- **U-030** — Sealed the MailTransport ACL exception boundary: narrowed `K9MailTransport` public constructor and `MessageConverter.convertMessages` to throw app-owned `MailException` only (cause chain preserved); scrubbed all `com.fsck.k9` references (code + javadoc) from `service.*`. Result: `grep -rn "com.fsck.k9" service/` → **zero** after U-031's deletions.
- **U-031** — Removed the legacy AsyncTask path: built a worker→foreground `WorkInfo` observer bridge (prerequisite), migrated manual dispatch to `scheduleManual(BackupType)` (CNTR-004 v2), rewired cancel to `WorkManager.cancelUniqueWork`, deleted `BackupTask`/`RestoreTask`/`BackupCancelCollector` + their tests + jacoco excludes, retained the foreground-service shells.
- **U-032** — Completed Hilt: `@AndroidEntryPoint` on `SmsBackupService`/`SmsRestoreService`, removed the `ServiceBase` null-check coexistence shims, removed the vestigial `CalendarSyncer` `@Inject`, migrated the Robolectric service tests.

**Decisions Made.**
- Sequenced strictly U-030 → U-031 → U-032 (single-threaded; the dependency chain forbids parallelism).
- U-031 retained `SmsBackupService`/`SmsRestoreService` as foreground-service shells that delegate to the scheduler (rather than deleting them) — they are live (`MainActivity` `startService`, `foregroundServiceType="dataSync"`).
- `scheduleManual` uses a `BackupType.name()` unique-work name distinct from `BROADCAST_INTENT` so manual and automation runs don't evict each other (CNTR-004 Validation Rule 8).

## Process Observations

**Process Compliance.** Yes — full requirements → design → contracts (+CRs) → stories → sprint → execution chain. Each story passed Artifact Librarian structural review, a Step-5b SA design-alignment audit, and the per-wave gate (terminal status + 5 PASS artifacts) before the next wave started. Merged-tree build-verification ran after every wave.

**Deviations.**
- **U-032 CalendarSyncer fix-not-verify (directed).** The U-032 story AC-7 was authored assuming U-031 had already removed the `CalendarSyncer` unbound-`calendarId` `@Inject`. U-031's story (AC) had assigned it to U-032. Net effect: each story punted the fix to the other (see Issues). The orchestrator directed U-032 at execution time to actually perform the fix, not merely verify it.
- **U-032 test migration.** Tests retain an anonymous service subclass overriding `onCreate()` (empty, no super) to bypass Hilt injection under Robolectric, rather than a full constructor-injection refactor. A known Robolectric+Hilt pattern; behaviorally sound; documented in U-032 implementation-log §AC-10. Literal grep `new SmsBackupService() {` → 1 match (intentional).

**Gate Results.**
- U-030: plan PASS, implementation-log PASS, review-code PASS, review-security PASS, qa-results PASS. Wave-1 gate PASS.
- U-031: plan PASS, implementation-log PASS, review-code PASS, review-security PASS, qa-results PASS. Wave-2 gate PASS.
- U-032: plan PASS, implementation-log PASS, review-code PASS, review-security PASS, qa-results PASS. Wave-3 gate PASS.

**Issues Encountered.**
1. **Story-coordination gap (CalendarSyncer).** U-031 and U-032 stories each delegated the `CalendarSyncer @Inject` removal to the other story; neither owned it. Caught during U-031 execution review (the agent left `@Inject` in place citing the story), resolved by directing U-032 to fix-and-verify. Root cause: the two stories were authored in parallel and the per-story alignment audits checked each story against the design independently — they did not cross-check the two stories' ownership of a shared prerequisite against each other. (The create-sprint Step-4c cross-story coherence audit would have been the natural catch point, but both stories carried `alignment_audit: passed`, so the cross-story value-comparison was skipped per the skill's own short-circuit.)
2. **Closing-bookend status mis-derivation.** The `bookend close` engine derived `final_status: completed-with-blockers` (with a `return_to_rework` / `identify_and_defer_dependent_stories` cascade) despite all 3 stories `done` and **zero** stories `blocked`. Verified objectively: 3/3 `status: done`, no `blocked`/`blocker`/`deferred` fields anywhere, 15/15 PASS artifacts, green build. Corrected the frontmatter to `completed` with a documented rationale. This mirrors the sprint-001 state-machine over-advance quirk and appears to be an engine heuristic mis-fire, not a real sprint condition.
3. **On-device end-to-end manual-backup verification gap.** `scheduleManual(MANUAL)` could not be exercised end-to-end on the emulator: `SmsBackupService` is (correctly) not exported, so `am start-service` is denied; and the service's pre-flight credentials check bails with `RequiresLoginException` before reaching `scheduleManual` when no account is configured. This is the same known live-verification gap as the backup/restore round-trip — it requires the configured test Google account. Compensating evidence used: clean launch smoke (no crash, WorkManager live via the same scheduler that owns `scheduleManual`, foreground driver `backupStateChanged` firing), `scheduleManual`/bridge unit tests, and green merged build.

**Recommendations.**
- **Cross-story prerequisite ownership.** When two stories in one decomposition share a prerequisite (e.g., a fix that must land in story A so story B's annotation is safe), the story authoring step should assert single ownership and the cross-story coherence audit (create-sprint Step 4c) should **not** be short-circuited by per-story `alignment_audit: passed` when stories have an explicit ordered dependency — a shared-prerequisite ownership check is exactly the cross-story class that per-story audits cannot catch.
- **Closing-bookend status derivation.** Investigate why `bookend close` derives `completed-with-blockers` when all stories are `done` and none are `blocked`. Recurs across sprint-001 and sprint-002; the engine's terminal-status heuristic should treat an all-`done` story set as `completed`.
- **Live-verification harness.** The deferred live-account E2E verification (backup/restore round-trip, Billing, People API, and now `scheduleManual` manual dispatch) is now blocking genuine on-device confidence for two consecutive sprints. Prioritize standing up the configured-test-account verification path.
