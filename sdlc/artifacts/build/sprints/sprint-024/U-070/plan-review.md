---
artifact_type: gate-result
story_id: "U-070"
gate: plan-review
gate_id: "plan-review"
checkpoint: plan-gate
verdict: FAIL
iteration: 1
agent: "Solution Architect (separate instance, plan-review)"
timestamp: "2026-11-21T11:53:27Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md"
base_commit: "533e9f51b5f880cdad05c81880c401b130352b95"
blockers: 2
observations: 10
---

# Gate Report: gate-result — U-070

## Gate: plan-review @ plan-gate

## Verdict: FAIL

## Summary

I read the story in full, including both sets of binding rulings (Record 16 and Record 18), REQ-MODERNIZATION-020 AC-1 to AC-24, and the plan in full (3,550 lines). I checked the plan against the code itself and did not take its line numbers or quotations on trust.

**Base.** HEAD moved during the review from `c12a2788` to `533e9f51`. The four commits in between are documents plus root `build.gradle` (U-068 S1-I1, JUnit XML on; the plan's row D2 covers this). `git diff --name-only c12a2788 HEAD -- app README.md` is empty. The plan and story in the working tree equal HEAD. The worker files were read with `git show HEAD:`.

**Code traced.** I read these at HEAD:
- `BackupQueryBuilder`, `BackupItemsFetcher`, `BulkFetcher`, `BackupCursors`;
- `BackupWorker.kt`: `executeSkip`, `fetchAndBackupItems`, `backupCursors`, `appendBatchAndUpdateWatermark`, `handleAuthError`, `doWork` and its catch blocks, the two test factories;
- `RestoreWorker.kt`: `doWork`, `executeRestore`, `importMessage`, `insertSmsValues`, `smsExists`, `executeRestoreWithValues`, `TestableRestoreWorkerFactory`;
- `MessageConverter`, `MessageGenerator` (SMS path, `includePersonInBackup`), `HeaderGenerator`, `ConversionResult`, `PersonLookup`, `ContactAccessor`, `ContactGroupIds`;
- `DataTypePreferences`, `Preferences`, `DataType`, `Defaults`, `MailTransport`, `MessageImportResult`, `XOAuth2FailedException`, `RequiresLoginException`;
- `backup_descriptor.xml` and the manifest's backup attributes; k9 `Address`, `Message`, `TextBody`.

**Edits applied outside the repository.** A script extracted every production edit verbatim from the plan's line ranges and applied it to HEAD copies in my session scratch folder: P2a to P2c, P3a, P3b, P5a, P5b, P6a to P6c, P7, P8, P9a and P9b.
- Every `old_string` occurs exactly once.
- `javac -Werror -Xlint:unchecked` (the build's flags, `app/build.gradle:203`) against `android-35/android.jar` compiles P1, P2, P3, P4 and P5 with no warning, using a stub `App` that holds `TAG` and `LOCAL_LOGV`.
- The Kotlin edits (P6, P7) were read, not compiled. They are statement-level, and every member they use exists with the type used.
- `git diff --no-index -U0` hunks:
  - `BackupQueryBuilder`: `-13,0` and `-163,0`, which is the plan's I3 expectation. Old lines 103-147 are untouched, so the forward, MMS and call-log text is unchanged (AC-6, AC-18).
  - `MailTransport.java`: one hunk `@@ -74,4 +74,4 @@`. The non-comment count is 0.
- All 28 Java mutation `old_string`s occur exactly once, and each mutant compiles. The 4 Kotlin anchors (M-L11, M-L16 a/b, M-L20) occur exactly once in the edited files.
- The four W2 digest vectors and the five Message-ID MD5s (L21, L22) recompute exactly in Python, using Java UTF-16 lengths and `HeaderGenerator.createMessageId`'s input.

**Design: correct against the code.** I traced the design through the code and found no blocker in it.
- **Window and boundary.**
  - The window is `date > wm − W AND date <= wm`, and the forward query is `date > wm`. The two partition the rows, with no gap and no overlap at `wm`.
  - Pruning removes `date <= wm − W`, the same bound as the window's exclusive lower bound. So an entry leaves the record exactly when no window at this watermark or a higher one can return its row (AC-20).
  - A row exactly W below is not returned, and one W − 1 ms below is returned. This matches REQ AC-4 and AC-5 and story AC-5.
- **Arithmetic.** There is no overflow: the look-back runs only when `wm > 0`, and `prune` returns early when `wm <= 0`.
- **No clock.** The selection never reads a wall clock or a time zone.
- **Equal dates.** Rows with equal dates are separated by digest. When a forward `LIMIT` cuts through a run of equal dates, the rows left over become window rows on the next run and are uploaded. That is an improvement on today.
- **Per-run limit.**
  - Late rows are taken first, up to `max`, and recorded window rows are not counted.
  - When the late rows use the whole budget, the forward query is not sent, so the watermark cannot move past waiting late rows.
  - The cursor's `getCount()` is the number of late rows plus accepted forward rows, so `BulkFetcher` gives MMS and call log exactly the rest (AC-2, AC-21).
- **First run.** With `wm <= 0` there is no window query, and only the fetch layer checks this.
- **MMS and call log.** They are untouched: `setMaxSyncedDateAfterAppend` delegates to `setMaxSyncedDate` for both, and the examined-row write is gated on SMS.
- **Key.** It is the date plus 64 bits of SHA-256 over `date|len|address|len|body`. The length prefixes make the input string injective, and null fields count as "". It contains no type and no `_id`. One encoding caveat is in O-3.
- **Commit.** There is one `commit()` for the watermark, the record and the marker, and the watermark is `max(stored, confirmed)`. The marker test (`stored <= 0 || marker`) means no upload commit can hide the need for the upgrade seeding.
- **Restore.** Both restore paths, the server loop (`RestoreWorker.kt:529`) and `executeRestoreWithValues` (`:402`), reach the single write-ahead call before `:461`. That call sits after `smsExists`, so a skipped duplicate is not recorded.
- **Reset.** `clearLastSyncData` removes all five keys under the lock.

Failure windows (process death between any two steps), checked against plan 2.7:

| Point | State left | Outcome |
|-------|-----------|---------|
| During the seed query, window query, forward query or conversion | nothing written | the same selection runs next time; no loss |
| After the seed commit | record plus marker | no re-seed |
| Before the server confirms (open, login, append, cancellation) | unchanged (BUG-010 order kept) | the row is selected again; no loss |
| After the confirmation, before `commit()` | unchanged | that one row is uploaded twice: today's window, no wider |
| After `commit()` | watermark and record together | no duplicate |
| Restore: after the write-ahead entry, before the insert | entry without a row | harmless |
| Restore: after the insert, before the raise | entry above the watermark | the forward filter drops the row; the forward `LIMIT` is widened by the entries above |
| Two workers overlapping (SC-9) | either may upload the same late row | a duplicate is possible, as for forward rows today. No loss: the watermark never moves down, and the record's read-modify-write is under the static lock |

**Tests traced.** I traced 31 of the 49 tests against the real sources:
- R1, R2, R3;
- B1 to B4, B5, B6, B7, B8, B9 to B12, B13, B14, B15, B16, B17, B18, B19, B20, B21, B22, B23, B24, B25;
- Z1;
- W3, W4, W5, W6;
- Q1 to Q3;
- S1 to S11.

This covers every test the brief names (AC-1, AC-5, AC-7, AC-9, AC-10, AC-12, AC-14, AC-16, AC-17, AC-20, AC-21). Every fixture, constructor, preference key and seam exists as used, including:
- the `BackupWorker` and `RestoreWorker` constructors;
- `TestableRestoreWorkerFactory(store, interceptor)` and `InMemoryCheckpointStore` (`RestoreWorkerCheckpointTest.kt:580`);
- `RestoreInsertInterceptor.NoOp`;
- `MailTransportTestFactories.createFolderHandle` and `createHandle`;
- `MessageImportResult(String, DataType, ContentValues)`;
- `XOAuth2FailedException(int, Throwable)`;
- `RequiresLoginException()`;
- `ContactGroupIds.add(id, rawId)` and `contains`, which uses the contact id;
- `ContactAccessor`'s `data1 = ? AND mimetype = ?` query;
- `Message.getHeaderNames`, `TextBody.getRawText` and `Preferences.setReferenceUid`;
- the keys `max_items_per_sync` and `backup_contact_group`, which are string preferences read by `getStringAsInt`;
- `MmsConsts.DELIVERY_REPORT = "134"`.

The selection regexes of the provider double match the texts the code emits, including the two spaces before the group clause. R1 and R2 fail on the start commit for the stated reason (the forward-only selection, `returned=[]`). Existing pinned tests keep passing:
- every `BackupWorkerWatermarkTest` and `BackupWorkerIntegrationTest` case writes increasing or −1-based watermarks, so `max()` changes no expectation;
- `BackupItemsFetcherTest`'s mocked builder returns null from `getDataTypePreferences()`;
- tests that mock `Preferences`/`DataTypePreferences` return 0 from `getMaxSyncedDate`, which keeps them on today's path.

**Mutations.** I re-derived the failing set of all 32 runs. One prediction is wrong in a way that stops the run (B-2). Three runs have incomplete "also" lists (O-1). Every other must-fail set holds.

**Verdict: FAIL, with two blockers.**
- **B-1:** test B3 (AC-2 (c), REQ AC-22) fails on correct code because of its own set-up.
- **B-2:** the must-fail list of M-L19 (i) names B2, which passes under that mutant, so row D16 stops the story before I9.

Both fixes are confined to test text and the mutation table. No production edit, design decision or other assertion changes.

**Not reached or not executable here:**
- the Robolectric runtime probes N1 to N6, which need Gradle;
- a Kotlin compile of T1 to T7 and of P6 and P7 (read only);
- the exact extra failures under M-L8 (a), whose MMS conversion over SMS rows goes through `MmsSupport`, which I did not trace. Its must-fail test B19 fails regardless;
- the JaCoCo estimate (not recomputed);
- IC-4 (the orchestrator's).

## Blockers

### B-1 — B3 `ac2c_alreadyBackedUpWindowRows_areNotCountedAgainstTheLimit` fails on correct code (AC-2 (c), REQ AC-22 unproven; M-L19 (ii) evidence void)

**Where:** `sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md:1944-1945`, with the helper at `:1900-1905`.

**What goes wrong.**
1. The test adds four rows dated D − 4000 to D − 1000 and then calls `establishWatermarkAtD()`.
2. That helper adds the row at D and asserts that the first backup uploads exactly that one row: `containsExactly(id)`.
3. No watermark exists yet, and the default per-run limit is −1 (`Defaults.java:23`). So the first run's forward query, `date > -1 AND type <> 3`, returns and uploads all five rows.
4. The assertion fails before the scenario starts.

**Consequences.**
- I4-new ("25 + 1 pass") cannot be met, and no decision row covers it: D7 is for compile errors only.
- The REQ AC-22 / story AC-2 (c) scenario is never exercised.
- Under M-L19 (ii) the test "fails" for the wrong reason, so that entry would record a detection that proves nothing.

**Exact fix.** Replace plan lines 1944-1945:
```kotlin
        (4 downTo 1).forEach { h.sms.add(D - it * 1000L, "+1555010010$it", "backed up $it") }
        establishWatermarkAtD()
```
with:
```kotlin
        val backedUp = (4 downTo 1).map { h.sms.add(D - it * 1000L, "+1555010010$it", "backed up $it") } +
            h.sms.add(D, "+15550100009", "watermark row")
        assertThat(ids(h.backupRecording())).containsExactlyElementsIn(backedUp).inOrder()
        assertThat(h.watermark()).isEqualTo(D)
```
Leave the rest of the test unchanged. Re-derived with the fix:
- **Correct code:** the window holds 5 recorded rows and 1 late row. `late` = [late] and `remaining` = 2, so the forward query `LIMIT 2` returns f0 and f1. The run uploads `late, f0, f1`, the next run uploads f2, and the third run uploads nothing.
- **M-L19 (i):** `LIMIT 3`, so 4 uploads, and the test fails.
- **M-L19 (ii):** 3 − 1 − 5 < 0, so no forward query, and the test fails.
- **M-L1 and M-L5:** the test fails, as table 4.8 says.

### B-2 — M-L19 (i)'s must-fail list names B2, which passes under that mutant (D16 stops the story before I9)

**Where:**
- `plan.md:3205-3206`: "M-L19 (i) … `final int remaining = max;`. Must fail: B2, B3.";
- table 4.8, row B2, at `:2883`: "Bitten by … M-L19i".

**Why B2 passes.**
- **The B2 scenario.** The limit is 5. The window holds the recorded row D plus 2 late rows, and there is 1 forward row.
- **The mutant.** With `remaining = max` the forward query is sent with `LIMIT 5` instead of `LIMIT 3`. It still returns the single forward row.
- **The budget.** The SMS cursor's `getCount()` stays 2 + 1 = 3, so `BulkFetcher.java:30` still gives MMS `max(5 − 3, 0) = 2`. B2 asserts exactly `["date LIMIT 2"]` and 3 uploads, so it passes.
- **Why no test of this shape can catch it.** The MMS budget is driven by the cursor count, which this mutant does not change. A forward-row count above `remaining` would make both variants use the whole budget.

**Consequence.** D16 (`:3346`) says "a must-fail test passes → … log a finding 'AC-24 unmet for <id>', finish the remaining entries, then stop and report before I9". The run halts at the end of I8 on a false finding.

**Exact fix.**
1. `:3206`: "Must fail: B2, B3." → "Must fail: B3."
2. `:2883`: B2's "Bitten by" → "M-L1, M-L8a".
3. Add one sentence under the M-L19 entries: "The story's M-L19 row ('AC-2 (b), (c)') is met by B3 (AC-2 (c)) for both variants. AC-2 (b)'s budget is the cursor count, which `BulkFetcher` reduces whatever the forward `LIMIT` is; B2 is bitten by M-L1 and M-L8 (a)."
4. If the orchestrator reads the story's table as requiring AC-2 (b)'s test itself to fail under M-L19, that is a question for it before the build. No single-edit mutant of `remaining` can make B2 fail.

## Observations

**O-1 — Three "also expected to fail" lists are incomplete** (each produces an "expected: does not match" deviation under D16's second sentence, not a stop).
- **M-L2 (i) and (ii)** (`:3141-3144`) also fail S1 and B18:
  - the seed query shares `windowArgs`, so it seeds rows older than W;
  - `prune` then drops those rows;
  - the unbounded window returns them unrecorded, and they are uploaded.

  Fix: add "S1, B18" to both "Also" lists.
- **M-L3 (3 h)** (`:3145-3146`) also fails B1 (`late[2]` at exactly D − 3 h is outside the exclusive bound) and B14 (the row at D − 5 h is outside a 3-hour window). Fix: add "B1, B14".
- **M-L19 (ii)** (`:3207-3208`) also fails S5: after the Reset, run 2 has two recorded window rows, so `remaining = 2 − 0 − 2 = 0`, and r1 to r3 are never fetched. Fix: add "Also: S5".

**O-2 — An unnamed behaviour difference: rows excluded by the SQL contact-group clause become uploadable after a group change.** Plan 2.8 (`:365-388`) names the converter-rejection consequence (Record 16/18). It does not name the reverse case.
- A row that the window's SQL group clause (`type = 2 OR person IN (...)`) excludes is never returned, so it is never recorded.
- If the owner widens the group, for example to "everybody", every such SMS dated within W below the watermark is uploaded by the next backup. Today it is never uploaded.
- This is neither a loss nor a duplicate. It is also the opposite of what Record 16 says for converter-rejected rows, which stay excluded.

Fix: add a 2.8 item 6 stating this, put it in the I10 declared items, and tell the orchestrator (no code change).

**O-3 — The digest input is not injective for unpaired UTF-16 surrogates.**
- **Cause.** `canonical.getBytes(StandardCharsets.UTF_8)` (P1, `plan.md:630`) replaces an unpaired surrogate with `?`. So bodies `"\uD800"`, `"\uD801"` and `"?"`, with the same date and address, produce the same digest. A late row whose body is one of these would be treated as backed up: a loss.
- **Likelihood.** Very unlikely (malformed SMS text), and distinct from the accepted SC-6 case (identical text).
- **Fix, either of:**
  - (a) Hash the UTF-16 code units losslessly: feed `MessageDigest` two bytes per `char` of `canonical` instead of `getBytes(UTF_8)`. Recompute the four W2 vectors (`:2450-2453`) and L21 independently. D8 then compares against the new vectors.
  - (b) Keep the code and record it as accepted risk R9b in section 8.

**O-4 — AC-17's "fresh store" is approximated** (REQ AC-19: "copies to a fresh store exactly the files that `backup_descriptor.xml` lets Android copy").
- `moveToAnotherPhone()` (`:2814-2839`) clears and rewrites the copied files with their own contents. It leaves the excluded files in place.
- The test still proves that the record lives in a copied file (`:2823`) and that content keys survive new row ids. But it does not model the absence of the excluded files.

Fix: after the copy loop, add `excluded.forEach { h.context.getSharedPreferences(it.removeSuffix(".xml"), Context.MODE_PRIVATE).edit().clear().commit() }`, keeping the existing `setImapUser(OWNER)` after it.

**O-5 — The KDoc of `SimulatedProcessDeath` is inaccurate for the backup path.**
- `plan.md:1329` says the exception is "not caught by any catch of the workers". `BackupWorker.doWork` catches `Exception` (`BackupWorker.kt:168-171`), so in B13 the "death" becomes `Result.failure`.
- The persisted state is the same as after a real death (the throw precedes the commit), so B13 still proves AC-10.
- In S8 the exception does escape, because `executeRestoreWithValues` catches only `IllegalStateException`.

Fix: change the KDoc to "propagates out of the restore loop; in the backup path `doWork`'s `catch (e: Exception)` turns it into `Result.failure` after the same persisted state a real death leaves".

**O-6 — The I9 working-tree count can include other stories' untracked tests.**
- **Current state.** `git status` now shows `app/src/test/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiverTest.java` untracked: U-068 is in progress in this checkout.
- **At the start.** I0's porcelain check stops on such a file (D1).
- **During the run.** A file that appears mid-run makes `git grep --untracked` (`:3229`) differ from start + 49, and D17 stops.

Fix: in I9 also record `git ls-files --others --exclude-standard -- app/src/test`, subtract the `@Test` count of every listed file that is not one of the seven U-070 files, and list those files in the log as "not this story's". D17 then stops only on an unexplained difference.

**O-7 — The I6 README hunk comment does not match what git prints.**
- `plan.md:3088` says "two insertion hunks after old :87 and :275".
- `git diff -U0` of the applied P9a and P9b prints `@@ -87 +87,4 @@` (line 87, `Gmail.`, is rewritten) and `@@ -276,0 +280,6 @@`.

Fix: write those two headers as the expected value.

**O-8 — The window cursor leaks if the forward query throws.** In P5b (`:1062-1090`), a `SecurityException` from the forward `performQuery` leaves `window` open. The backup fails either way, so this is only a resource leak. Fix (optional): wrap the forward part in `try { … } catch (RuntimeException e) { window.close(); throw e; }`.

**O-9 — Feasibility and hand-over points.** The run is 42 Gradle invocations:
- 3 full gates (I0, I9, and Gate G inside them);
- 7 targeted runs;
- 32 mutation runs, each a recompilation plus 48 Robolectric cases, with Z1 added for M-L13.

That is several hours and a large context for one developer session, but it is feasible. M-L13 (a) is the slowest: the record grows to about 10,000 entries, each commit rewrites and `fsync`s a file of about 300 KB, and D19 and D21 cover the time. Natural hand-over points:
- after I1b (start evidence frozen);
- after I7 (all code and tests green, pristine copies and `<NAME>_NEW` blobs recorded);
- between any two I8 entries (the write-ahead log and the recovery note already support resumption);
- before I9.

Recommended: two sessions, I0 to I7 and then I8 to I11.

**O-10 — Stale line references (no effect: every edit is anchored by text).**
- Plan 2.10 and the IC-1 table (`:3399`) give "`:424` after U-067" for the seam's write. At HEAD, which is after U-067, it is `BackupWorker.kt:423`.
- The else-branch is `:355-357`.
- The plan's `base_commit` is `c9770c30`. Iteration 2 should cite the HEAD it re-reads.

## Checks that hold (re-derived)

| Item | Plan says | I found |
|------|-----------|---------|
| Production `old_string`s | each exactly once | each exactly once at HEAD (after U-067) |
| `BackupQueryBuilder` hunks | `-13,0`, `-163,0` | the same (`git diff --no-index -U0`) |
| `MailTransport.java` | one hunk `-74,4 +74,4`, comment lines only | the same; 0 non-comment lines |
| Java compile with `-Werror -Xlint:unchecked` | n/a | P1 to P5 compile clean (API 35) |
| 28 Java mutants | single edits | each `old_string` unique; each mutant compiles |
| W2 vectors and L22 Message-IDs | computed in Python | all nine recompute exactly |
| Test count per file | 3, 25, 1, 11, 6, 3 (49); fixtures 0 | the same, from the plan text; no `@Ignore` or `Assume`; no address pattern |
| I0 anchors | each `1` | each `1`. Git for Windows `grep -x` matches the CRLF working tree (checked on `BackupItemsFetcher.java`) |
| AC-4 grep at HEAD | no hit before P1 | no hit |
| U-068 and U-069 overlap | no common file | none. U-069 changes `BackupTriggerWorker.kt` and one `app/build.gradle` JaCoCo line; U-068 changes neither worker file nor any U-070 file |
| Must-fail sets | 32 runs | all hold except M-L19 (i) / B2 (B-2); the extra lists of M-L2, M-L3 (3 h) and M-L19 (ii) are short (O-1) |
| Criterion map | every AC and IC | every AC-1 to AC-27 and IC-1 to IC-4 has a step; REQ-020 AC-1 to AC-24 are all mapped (AC-25 is the targetSdk story's). REQ AC-22 / AC-2 (c) depends on B-1 |
| Developer constraints | foreground, 600000 ms, no `MSYS_NO_PATHCONV` for Gradle, wait for backgrounded runs, stop on kills, commit nothing, no build-file change, no `@Ignore` | all present (section 12; D19, D20) |
