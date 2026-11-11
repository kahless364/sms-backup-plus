---
type: bug
status: ready
artifact_type: bug
severity: medium
priority: medium
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items: []
platforms: []
tags: []
id: BUG-018
title: Restore reports FAILURE/ERROR after all messages are restored when the optional thread-refresh step throws IllegalArgumentException (Unknown URL)
domain: build
origin: qa
updated_at: '2026-11-11T22:10:58.378Z'
---

# BUG-018: Restore reports FAILURE/ERROR after all messages are restored when the optional thread-refresh step throws IllegalArgumentException (Unknown URL)

## Description
A restore that inserts every message successfully is reported as failed. After the last message is inserted, `RestoreWorker.updateAllThreadsIfAnySmsRestored` calls `contentResolver.delete(content://sms/conversations/-1)`. On Android 16 (API 36) this throws `IllegalArgumentException: Unknown URL`. The call is not guarded. The exception skips the success path (`checkpointStore.clear`, the "finished" log and `Result.success`) and propagates to `doWork()`'s generic `catch (e: Exception)`, which returns `Result.failure`. The UI therefore shows ERROR with zeroed counts, although the data was restored correctly.

## Steps to Reproduce
Environment: emulator AVD Pixel_9_Pro (Android 16 / API 36, google_apis_playstore x86_64), debug APK built from commit 38845a34 (code identical to HEAD b09bda5d of branch sdlc/modernization-plan; only docs changed since).
1. Configure IMAP (imap.gmail.com:993, TLS, Gmail app password). The Gmail label "SMS" already holds 8 backed-up messages. 3 are already on the device and 5 are not.
2. In the app tap RESTORE, confirm the app's "Required app change" dialog, and in the Android role dialog set SMS Backup+ as the default SMS app.
3. Wait for the restore to finish.
4. Observe: the worker ends with FAILURE and the UI state is ERROR, although the 5 missing messages are present in `content://sms`.

## Expected Behavior
When all messages are restored, the worker returns success and the UI shows a completed restore with correct restored and duplicate counts. A failure of the optional thread-refresh step does not turn a successful restore into a reported failure.

## Actual Behavior
- All 8 messages are fetched and the progress events reach currentRestoredCount 8 of itemsToRestore=8.
- `updateAllThreadsIfAnySmsRestored` throws `IllegalArgumentException: Unknown URL`.
- The worker returns `Result.failure` (WorkManager: "Worker result FAILURE").
- The UI receives `RestoreState{state=ERROR, currentRestoredCount=0, itemsToRestore=0, actualRestoredCount=0, duplicateCount=0}`.
- "RestoreWorker: finished updating threads" and "RestoreWorker: finished (restored=...)" are never logged.
- Device data is correct: `content query --uri content://sms` shows 8 rows (3 original plus 5 new, _id 4..8), with no duplicates of the 3 pre-existing messages.
- The app process then exited and Android returned the SMS role to com.google.android.apps.messaging.

## Evidence
Logcat, tag SMSBackup+, pid 3359:
```
16:00:21.242 I Found 8 msgs
16:00:35-16:01:03  eight "UID FETCH n (UID BODY.PEEK[])" round trips; restoreStateChanged events
                   count currentRestoredCount 1..8 of itemsToRestore=8
                   (actualRestoredCount=0, duplicateCount=0 in every progress event)
16:01:03.665 D RestoreWorker: updating threads
16:01:03.669 E RestoreWorker: unrecoverable error
16:01:03.669 E java.lang.IllegalArgumentException: Unknown URL
16:01:03.670 I WM-WorkerWrapper: Worker result FAILURE for Work [ ... tags={ com.zegoggles.smssync.service.RestoreWorker, RESTORE } ]
16:01:03.673 D restoring SMS provider null
16:01:03.674 V restoreStateChanged:RestoreStateChanged{state=ERROR, currentRestoredCount=0, itemsToRestore=0, actualRestoredCount=0, duplicateCount=0}
16:01:03.675 W WorkManagerCancelCollector: error collecting cancel for RESTORE
16:01:03.675 W kotlinx.coroutines.JobCancellationException: StandaloneCoroutine was cancelled
```
- The full stack trace was not captured (the emulator was shut down before it could be re-read).
- RoleControllerServiceImpl: "Removing package that no longer qualifies for the role, package: com.zegoggles.smssync", then "Adding package as default/fallback role holder, package: com.google.android.apps.messaging".

Code (app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt:623-629):
```kotlin
private fun updateAllThreadsIfAnySmsRestored(ctx: Context) {
    if (smsIds.isNotEmpty()) {
        Log.d(TAG, "RestoreWorker: updating threads")
        ctx.contentResolver.delete(Uri.parse("content://sms/conversations/-1"), null, null)
        Log.d(TAG, "RestoreWorker: finished updating threads")
    }
}
```

## Acceptance Criteria
- [ ] AC-1: The defect no longer reproduces following the steps above. The restore of 5 new messages (3 pre-existing) completes with the worker returning SUCCESS, the UI showing a finished restore state (not ERROR), and "RestoreWorker: finished (restored=...)" logged.
- [ ] AC-2: Existing functionality is not broken by the fix.
- [ ] AC-3: When the thread-refresh step throws, the restore is still reported as successful, the durable checkpoint for the work name is cleared, and the thread-refresh failure is logged at warning level or above with the exception.
- [ ] AC-4: The restored and duplicate counts shown after a successful restore are the actual values (restored = 5, duplicates = 3 in the reproduction scenario), not zeros.
- [ ] AC-5: An automated unit test fails before the fix and passes after it. The test makes the content provider throw `IllegalArgumentException` on delete of the thread-refresh URI while at least one SMS was restored, and asserts `Result.success` with STATE_FINISHED.

### Integration Criteria (if fix modifies component boundaries)
- [ ] IC-1: If `updateAllThreadsIfAnySmsRestored` is changed, all three call sites (RestoreWorker.kt:270, :331, :416) are updated or confirmed unaffected.
- [ ] IC-2: If the signature changes, it matches all callers.

## Impact Analysis
Checklist applied: `impact-analysis-checklist.md`. `sdlc/config.yaml` defines no `change_records.layers`, so the analysis is a grep of `app/src/main` and `app/src/test`. Effectively a single implementation layer (Android source code) plus its tests. This is a code-only fix with no cascade to design or requirement layers.

| # | Layer | Affected? | Files | Occurrences | Action Needed |
|---|-------|-----------|-------|-------------|---------------|
| 1 | app/src/main (source) | Yes | 1 (RestoreWorker.kt) | `updateAllThreadsIfAnySmsRestored`: 1 definition and 3 call sites. `content://sms/conversations`: 1 (:626) | Guard or remove the thread-refresh step |
| 2 | app/src/main UI mapping | Consumer only | MainViewModel.kt | FAILED maps to ERROR at :289-291 | No change expected; shows the symptom |
| 3 | app/src/test | Gap | RestoreWorkerCheckpointTest.kt, RestoreWorkerTest.kt, RestoreWorkerIntegrationTest.kt | No reference to `content://sms/conversations` or the function. FakeSmsContentProvider.delete returns 0 and never throws (:576) | Add a regression test |

Findings:
- Call sites:
  - :270 is in the `catch (e: MailException)` handler of `executeRestore`, followed by `Result.retry()`. An exception here would replace retry with failure, so it is also unguarded.
  - :331 is in the IMAP restore loop `runImapRestoreLoop` (the failing path). It runs after the loop and before `checkpointStore.clear` (:340), the "finished" log (:337) and `Result.success` (:342).
  - :416 is in the test-accessible `executeRestoreWithValues`, with the same ordering relative to `checkpointStore.clear` (:425) and `Result.success` (:427).
- Failure handling:
  - Both loops catch only `IllegalStateException` (:348, :433). `IllegalArgumentException` is not a subclass, so it propagates to `doWork()`'s `catch (e: Exception)` (:198-200). That logs "unrecoverable error" and returns `Result.failure` with `KEY_FAILURE_REASON` set to `e.message` ("Unknown URL").
  - `MainViewModel.mapWorkInfoToRestoreState` maps FAILED to `RestoreState(ERROR, 0, 0, 0, 0, null, MailException("RestoreWorker failed"))`. This explains the zeroed counts in the ERROR event.
- Side effects of the failure path (verified by code reading): the checkpoint is not cleared, so a later restore under the same work name may resume from a stale checkpoint (read at :300 and :388). Real-world effect unverified. Notification and maxSyncedDate behaviour not checked.
- Tests: no existing test covers the thread-refresh step or a throwing provider delete.
- Cascade: root layer is implementation (code); upstream (legacy `RestoreTask.updateAllThreadsIfAnySmsRestored()`, which the KDoc says is mirrored) is read-only reference. In-progress work: none identified in this search. Severity of the change: LOW (single file plus tests).

## Suggested Approach
Advisory only — refined during story planning.

**Architectural considerations**
- The defect is a classification error. A best-effort post-processing step (thread refresh) shares one exception path with the restore itself, so any runtime exception from it is reported as a failed restore. The fix should mark the step as best-effort at its definition (`updateAllThreadsIfAnySmsRestored`, `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt:623-629`). Wrapping each call site would repeat the guard three times and is easy to miss on the next call site. One guard at the definition protects all three call sites (:270, :331, :416; scope verified via grep of `app/src`) with no signature change. IC-1 and IC-2 are then met because no caller changes.
- Catch only what the step can be expected to throw: `IllegalArgumentException` (observed) and possibly `SecurityException` (a plausible role-loss case, not observed). Log at WARN with the throwable and continue. Do not catch `Exception` or `Throwable`. On the JVM, `java.util.concurrent.CancellationException` (which Kotlin coroutines use) is a subclass of `IllegalStateException` and therefore of `RuntimeException`. A broad catch here would set a pattern that hides cancellation if the helper ever becomes `suspend`.
- Keep the refresh call. Do not delete it or change the URI in this fix. The legacy code (`RestoreTask.updateAllThreads()`, last present at the parent of commit b5333a20) explains why it exists: "thread dates + states might be wrong … passing a negative conversation id to delete should to [sic] the trick". It calls the same unguarded `content://sms/conversations/-1` delete, so the defect predates the WorkManager migration and was not introduced by it. **Unverified:** which API level stopped accepting this URI in the platform SMS provider, whether the provider now keeps thread metadata up to date by itself on insert, and whether `content://mms-sms/...` offers a working replacement. These are platform questions. They need AOSP `SmsProvider`/`MmsSmsProvider` source or an on-device check on several API levels. Treat them as a separate follow-up, not part of this fix.
- The `MailException` call site (:270) runs inside a catch block. Today an exception there turns `Result.retry()` into `Result.failure` through `doWork()`'s generic catch. The single guard at the definition also fixes this, and the regression test should cover it.

**Layer cascade (from Impact Analysis)**
- Implementation layer only for the guard: one file (`RestoreWorker.kt`). `MainViewModel.kt` maps the symptom to the UI and needs no change for AC-1 or AC-3.
- Test layer: `FakeSmsContentProvider.delete` (`app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerCheckpointTest.kt:576`) always returns 0. The regression test needs a provider that throws on delete, either a configurable flag or a subclass. `executeRestoreWithValues` (:378) is the test seam, and it reaches the :416 call site directly. The IMAP path (:331) is only reachable through `RestoreWorkerIntegrationTest`'s fake transport. Planning should decide whether both seams are tested, because the on-device failure came through :331.
- No REQ, DES or CNTR artifacts are affected. The `Result` contract (success, failure, retry plus `KEY_FAILURE_REASON`) is unchanged.

**One story or several: the guard and the count display are separate defects**
- AC-1, AC-3, AC-5, IC-1 and IC-2 are covered by the single guard. That part is one small story.
- **AC-4 will not be met by the guard alone.** This is the main planning risk:
  - `MainViewModel.mapWorkInfoToRestoreState` maps `SUCCEEDED` to a hard-coded `RestoreState(FINISHED_RESTORE, 0, 0, 0, 0, null, null)` (`app/src/main/java/com/zegoggles/smssync/activity/MainViewModel.kt:287-288`). It never reads `WorkInfo.outputData`.
  - `RestoreWorker` puts only `PROGRESS_KEY_RESTORED_COUNT` in its success output. A grep of `app/src/main` finds no duplicate-count key.
  - The RUNNING branch reads `RESTORED_COUNT` from `progress`, but the loops never set it there. That explains `actualRestoredCount=0` in every progress event in the Evidence.
  - Legacy `RestoreTask` computed `duplicateCount = max(0, uids.size - restoredCount)` and passed it to `FINISHED_RESTORE`. That behaviour was lost in the WorkManager migration (U-031/U-049). This is a separate count-reporting regression that the Android 16 failure was hiding.
  - It crosses the worker→ViewModel boundary: a new output key, mapping `outputData` on `SUCCEEDED`, and the `StatusPreference` text at `app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java:366`.
- Recommendation: either split AC-4 into its own bug or story (preferred, because it changes the worker's output contract and the ViewModel mapping, has its own tests, and does not need the Android 16 emulator), or explicitly widen this bug to two files plus tests and record AC-4 as a separate root cause. Either way, do not let AC-4 hold up the one-line correctness fix. The current AC-1 wording ("finished restore state") can be met without correct counts.
- **Correction on AC-4's expected values:** under the legacy formula, duplicates means "fetched UIDs not inserted". In the reproduction (8 fetched, 3 pre-existing), the restored count is 5 and the duplicate count is 3. This only holds if no imports were skipped for other reasons (failed conversion, type filter). The two causes are indistinguishable under that formula, and planning should decide whether that is acceptable.

**Constraints and risks for planning**
- **Sequencing:** this fix cannot be built or unit-tested on this machine until BUG-017 is resolved (`sdlc/artifacts/stories/BUG-017-build-fails-on-jdk-25.md`, Gradle build fails on JDK 25). Schedule BUG-017 first, or use the JBR 17 `JAVA_HOME` workaround described in the project CLAUDE.md, if planning confirms it still works.
- **Attribution confidence:** no stack trace was captured. Linking the exception to the :626 delete depends on log order: "updating threads" (16:01:03.665) is followed 4 ms later by "unrecoverable error", and "finished updating threads" never appears. That is strong evidence but not proof. AC-1's on-device re-run is the real confirmation. Planning should require the new WARN log to include the throwable so the stack trace is captured next time.
- **Coverage gate:** jacoco enforces LINE ≥ 70% per package. The new catch branch must be exercised by the regression test, or it lowers `service` package coverage.
- **Adjacent risk, out of scope, not verified at runtime:** both restore loops catch `IllegalStateException` (:348, :433). `ensureActive()` throws `CancellationException`, which is a subclass of `IllegalStateException`, so cooperative cancellation may be returned as `Result.failure("illegal_state")` rather than propagating as CANCELLED. A grep found no cancel-related test in `RestoreWorkerCheckpointTest.kt`. Do not fix this under BUG-018. Raise it for separate triage.
- **Role loss after process exit** (Technical Context, Scope Concern) is not explained by this defect and should not be folded into this fix.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt (:623-629, call sites :270, :331, :416) | Unguarded `contentResolver.delete(content://sms/conversations/-1)`; an exception aborts the success path and yields `Result.failure` | To be determined during planning (see Root Cause Analysis) |
| app/src/main/java/com/zegoggles/smssync/activity/MainViewModel.kt (:285-292) | Maps FAILED to ERROR with zero counts | Likely none; listed as the symptom surface |
| app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerCheckpointTest.kt (FakeSmsContentProvider, :512-576) | `delete` returns 0 and never throws | Regression test needed for a throwing delete |

## Existing Behavior to Preserve
- Message insertion semantics: new messages are inserted, and pre-existing messages are not duplicated (the 3 existing messages stayed single).
- Checkpoint write-ordering invariants (insert, then durable checkpoint write, then interceptor hook) and clearing the checkpoint on SUCCEEDED but not on CANCELLED.
- Real failures (auth failure, `MailException` retry, `IllegalStateException` "illegal_state", backoff cap) still return their current Result values.
- Progress events (STATE_RESTORE with current item and total) and the STATE_FINISHED success payload keys.
- The `Result.retry()` behaviour in the `MailException` handler.
- Cooperative cancellation behaviour (CANCELLED state).

## Verification Steps
1. AC-1: Repeat the Steps to Reproduce on an Android 16 (API 36) emulator with 8 messages in Gmail, 3 already on the device. Confirm in logcat that "Worker result SUCCESS" appears for the RESTORE work, that "RestoreWorker: finished (restored=...)" is logged, and that no "unrecoverable error" appears. Confirm the UI shows the finished-restore state, not ERROR. Confirm `content query --uri content://sms` shows 8 rows with the 3 originals not duplicated.
2. AC-2: Run the full unit-test gates (`:app:testDebugUnitTest`, `:app:jacocoTestCoverageVerification`) and confirm they pass. Re-run the existing restore tests (RestoreWorkerTest, RestoreWorkerCheckpointTest, RestoreWorkerIntegrationTest) and confirm no regressions. Confirm the auth-failure, MailException retry and cancellation paths are unchanged.
3. AC-3: In the new test, make provider delete throw `IllegalArgumentException`. Assert `Result.success`, that the checkpoint store has no entry for the work name, and that a warning-or-higher log is emitted with the exception.
4. AC-4: In the reproduction run, confirm the final UI or log shows restored = 5 and duplicates = 3.
5. AC-5: Run the new test against the pre-fix code and confirm it fails. Run it against the fixed code and confirm it passes.

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context
- Found during on-device validation on 2026-11-11 (emulator Pixel_9_Pro, Android 16 / API 36), on a debug APK built from commit 38845a34.
- `updateAllThreadsIfAnySmsRestored` only runs if `smsIds` is non-empty (at least one SMS inserted). The KDoc says it mirrors the legacy `RestoreTask.updateAllThreadsIfAnySmsRestored()` (RestoreTask.java:329-333).
- Not verified:
  - whether the exception also occurs below API 36;
  - whether the thread-refresh step is still needed on modern Android;
  - the exact UI text for ERROR;
  - whether the failure path skips other post-restore bookkeeping (counts, notification). Checkpoint clear and the success Result are skipped, per code reading.
- Scope Concern: the SMS default role was returned to the Google messaging app after the process exited. This was observed and may be unrelated to this bug (not investigated).

## Supporting Documentation
- None identified.

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes
