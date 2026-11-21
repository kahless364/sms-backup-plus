---
artifact_type: gate-result
story_id: "U-070"
gate: plan-review
gate_id: "plan-review"
checkpoint: plan-gate
verdict: PASS
iteration: 2
agent: "Solution Architect (separate instance, plan-review)"
timestamp: "2026-11-21T12:35:50Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md"
base_commit: "c8e98a7f98e5c28593bddbf207c6d19ef5f1ed52"
blockers: 0
observations: 4
---

# Gate Report: gate-result — U-070

## Gate: plan-review @ plan-gate

## Verdict: PASS

## Iteration 1 (history)

Iteration 1 was a FAIL with two blockers. Both were in test text and the mutation table. The design and the production edits were found correct against the code.

- **B-1.** Test B3 (`ac2c_…`, AC-2 (c), REQ AC-22) failed on correct code. It added four rows and then called `establishWatermarkAtD()`, which asserts that the first run uploads only the D row. With no watermark and no limit, the first run uploads all five.
- **B-2.** M-L19 (i) listed B2 as must-fail, but B2 passes under that mutant, because the MMS budget comes from the cursor count. Row D16 would then have stopped the run before I9.

Iteration 1 also had ten observations (O-1 to O-10).

## Summary

I re-read the plan in full in its iteration-2 form (4,094 lines; `iteration: 2`; base `2aad7d70`). I also read the story's "Rulings after the plan gates of iteration 1" (Record 18).

**Base.** HEAD is now `c8e98a7f`, after U-068's local commits `2aad7d70`, `853483e2` and `c8e98a7f`. `git diff --name-only 2aad7d70 HEAD` is empty for:
- `app/src/main/.../service`;
- `app/src/main/.../preferences`;
- `app/src/main/.../mail`;
- `README.md`.

So the plan's anchors still hold. Every source was read with `git show c8e98a7f:<path>`. The only working-tree file I read is the plan itself, which is uncommitted.

**Edits applied outside the repository.**
- **Application.** A script extracted every production edit of iteration 2 from the plan by line range and applied it to `git show` copies in my scratch folder. The edits are P2a to P2c, P3a, P3b, P5a, P5b, P6a to P6c, P7, P8, P9a and P9b. Every `old_string` occurs exactly once.
- **Compile.** P1 to P5 compile with `javac --release 17 -Werror -Xlint:unchecked` against `android-35/android.jar`, using a stub `App`.
- **Mutants.** I applied 14 Java mutants alone and compiled each: every new or rewritten one (M-L19 (i), M-L19 (ii), M-L23, M-L24, M-L25, M-L26), plus M-L1, M-L4 (a), M-L5, M-L8 (a), M-L10, M-L13 (a), M-L17 and M-L18 (b). Each `old_string` is unique, and each compiles. The new Kotlin anchor of M-L22 occurs exactly once in the edited `BackupWorker.kt`.
- **Counts.**
  - Tests: 50 tests in the seven files (3, 26, 1, 11, 6, 3; fixtures 0).
  - Mutation runs: 37, of which 32 are Java and 5 Kotlin.
  - Gradle invocations: 47 = 1 + 2 + 1 + 1 + 2 + 1 + 1 + 37 + 1.

**What I checked, in the order the coordinator asked.**

1. **B-1 and B-2 are fixed.**
   - **B3 (`plan.md` T3, scratch test lines 97-108).** The five backed-up rows are now uploaded in one first run and asserted in order. Under the new fetch code with limit 3:
     - the window holds 5 recorded rows plus `late`, so `lateBefore = 1` and `remaining = 2`;
     - the forward query is sent with `LIMIT 2` and returns f0 and f1;
     - the second read leaves `late` as the only late row, so the run uploads `late, f0, f1`;
     - the next run uploads f2, and the third uploads nothing.
   - **B3 under the two mutants:**
     - M-L19 (i) (`remaining = max`): `LIMIT 3`, so 4 uploads, and B3 fails;
     - M-L19 (ii) (`lateBefore++` unconditional): `lateBefore = 6`, so `remaining = 0` and there is no forward query. Only `late` is uploaded, and B3 fails.
   - **The rest of M-L19 (i)'s set, re-derived.** B1, B2, B16, B23 and S5 pass under it, so its set is {B3}, as listed.
   - **The rest of M-L19 (ii)'s set, re-derived.**
     - B16 fails: its second run's two recorded rows make `remaining = 0`, so rows[2] is not fetched.
     - S5 fails in the same way after the Reset.
     - B1, B2 and B23 pass, because their late rows already use the budget.

     So its set is {B3, B16, S5}, as listed.
   - **B2 is bitten** by M-L1 (no late rows, 1 upload instead of 3), M-L5 and M-L18 (b). Record 18's rule is met: every test fails under at least one mutation, and every id is detected. B-2 is closed under that ruling.
   - **The plan's correction about M-L8 (a) is wrong** (O-1): the plan's reason (B2's MMS watermark is −1) does not hold, because the mutated branch reads the SMS watermark. That makes the failing set listed for M-L8 (a) incomplete. It is not a blocker: D16 (b) logs it as a deviation and the run continues.

2. **The production edits that changed in iteration 2.**
   - **The second record read (P5b, `plan.md:1288-1336`).**
     - The first read `before` sizes only the forward `LIMIT`. The second read `record` comes after both queries have returned, and window and forward rows are filtered against it.
     - The ordering argument (2.4) holds. A restore commits the entry before it inserts the row (P7). If a query returns the restored row, the insert finished before the query read the provider, and the entry commit finished before the insert. The second read then takes the preferences lock after that commit, so it sees the entry.
     - Every `DataTypePreferences` in the process wraps the one default `SharedPreferences` object, so there is a single lock.
     - **Budget.** The record normally only grows between the two reads, so the late rows are at most `min(lateBefore, max)`, and late plus kept forward rows stay within `max`. The forward filter uses `entriesAbove(record)`, so an entry added during the forward query still drops its row.
     - **Cursor close.** A `RuntimeException` from the forward query closes the window cursor and is rethrown (O-8 closed). The fast path is unchanged: it still hands back today's forward cursor.
   - **The digest (P1 `digest`, scratch lines 195-214).**
     - It hashes two bytes per `char`, big-endian, by hand, with no charset encoder. The mapping is total and one-to-one over Java strings, and the length prefixes keep the fields apart. This meets the Record 18 ruling.
     - I recomputed **all seven** W2 vectors independently, in Python (`utf-16-be` with `surrogatepass`) and by running the compiled scratch `SmsBackupWindow.entryOf`. Both give exactly the plan's values, including the three malformed or `?` bodies:
       - `"\uD800"` → `b237423d5ff1a2c6`;
       - `"\uD801"` → `e636bc987e5e57c5`;
       - `"?"` → `570e37f30ca551d2`.
     - M-L24 (back to UTF-8) changes every vector, so W2 fails, and every other test keeps consistent keys.
   - **B26** (`race_restoreRecordsAndInsertsWhileTheQueriesRun_…`) **does show the race closed.**
     - **Set-up.** The provider double's `beforeQuery` hook runs a restore's two steps (`recordRestoredSms`, then the row's insert) at the start of the window query for D − 1 h, and at the start of the forward query for D + 1 h. Both are after the fetch's first read and before any row is selected.
     - **Correct code.** The window returns D − 3 h (late), D − 1 h (restored) and D. The forward query returns D + 1 h. The second read holds both restored entries, so the run uploads only the late row, and the next run uploads nothing.
     - **Under M-L26** (`record = before`):
       - D − 1 h is not in `before`, so it is taken as late;
       - `entriesAbove(before) = 0`, so D + 1 h is not filtered.

       Three rows are uploaded, and B26 fails.
     - **D23** covers a hook that does not fire.

3. **Mutation rows.**
   - **New and rewritten rows, re-derived exactly as listed:**
     - M-L19 (i) {B3};
     - M-L19 (ii) {B3, B16, S5};
     - M-L22 {S3}: Skip over {MMS} now runs `skipSms` and sets the SMS watermark to D + 1 h;
     - M-L23 {S4}: `max(D, D + 5 h)`;
     - M-L24 {W2};
     - M-L25 {W6}: the entry at `wm − w − 1` is kept;
     - M-L26 {B26}.
   - **Sampled rows against the "complete set" claim:**
     - M-L1, M-L2 (i), M-L3 (3 h), M-L5, M-L7, M-L10, M-L12 (a), M-L15, M-L16 (a), M-L17, M-L18 (b) and M-L21 match;
     - M-L8 (a) and M-L4 (a) do not (O-1).

   Both mismatches are D16 (b) deviations: still detections, not stops. Every id still has a detecting test, and every test is still bitten.
4. **Two sessions and scoped counts.**
   - **Resume.** S2-0 rebuilds the pristine copies from the `git hash-object -w` blobs and checks them with `cmp`.
     - With `core.autocrlf=true`, the stored blobs are LF.
     - `git cat-file --filters --path=` smudges them back to the CRLF working-tree bytes, and `cat-file blob` returns LF new files byte for byte.
     - So nothing on disk is lost between sessions.
   - **What session 2 needs from session 1.** Everything else it needs is in the repository: the sanitised `$E` files, including `I0-mailtransport-classes.txt`, and the implementation log, which holds `<n>`, `<s>`, `F0`, `C0`, the JaCoCo ratios, the LF/CRLF record, `START`, the blobs and `PROD_DIFF`.
   - **The hand-over state** is checkable by S2-0 steps 1 to 5 (hashes, `PROD_DIFF`, porcelain over `OWN`, open `APPLYING` entries), and D22 stops on any difference.
   - **Counts.** The I9 counts are scoped to `MINE` and `OWN`. Foreign untracked or modified test files are listed and counted separately as `F0` and `F1`, so the iteration-1 false stop (O-6) cannot happen. One small gap in the Gradle-total formula is O-3.
5. **Iteration-1 observations.**
   - Closed: O-2 (2.8 item 5), O-3 (the digest), O-4, O-5, O-6, O-7, O-8, O-9 (5.1) and O-10.
   - O-1 is closed as asked (M-L2: S1, B18; M-L3 (3 h): B1, B14; M-L19 (ii): S5, all in the sets). Its iteration-2 form, "complete sets", introduces two new mismatches (O-1 below).
   - None was left open without a reason.

**No new blocker.** Iteration 2 introduces nothing that would make the run fail, leave a criterion unproven, or ship a duplicate or a loss. The four observations below are recommended corrections; none changes a design decision or a production edit.

**Not reached or not executable here:**
- the Robolectric probes N1 to N7 (they need Gradle);
- a Kotlin compile of T1 to T7 and of P6 and P7 (read only);
- the JaCoCo estimate (not recomputed);
- IC-4.

The Java compile used the `app/build` class directory as a classpath for unchanged types such as `DataType`. That is build output, not source.

## Blockers

None.

## Observations

**O-1 — Two "complete" failing sets are incomplete, and the plan's correction of my iteration-1 M-L8 (a) remark is wrong.**

This is not a stop: D16 (b) logs an unlisted failure as "expected: does not match", still a detection. But the plan claims exact sets, so these two rows will be logged as planning errors.

- **M-L8 (a)** (`plan.md:3558-3561`; also the Iteration 2 table at `:55` and table 4.8 row B2).
  - **Why the plan's reason is wrong.** The mutated condition `dataType == SMS || dataType == MMS` leads into code that reads `preferences.getMaxSyncedDate(DataType.SMS)` (P5b, `:1257`). `getSmsItemsWithLookBack` then queries the **SMS** provider (`buildSmsWindowQuery`, `buildQueryForDataType(DataType.SMS, …)`). So the MMS watermark does not matter. Whenever the SMS watermark is above 0 and MMS (enabled by default) still has budget, the MMS fetch re-runs the SMS window and forward queries. It returns SMS rows as the MMS cursor.
  - **What then happens.** Converting those rows as MMS throws in `MmsSupport.getDetails`: `Integer.parseInt(msgMap.get("msg_box"))` gets null (`MmsSupport.java`, `getDetails`). That happens after the SMS rows are uploaded. B19 and B2 also send an `…/addr` query to the MMS double.
  - **Failing set, re-derived:**
    - B2 (`mms.queries` holds no `date LIMIT 2` query);
    - B4 (`single { isWindowQuery }` finds two window queries);
    - B19;
    - B21 (SMS query counts become 5, 4 and 4 instead of 3, 2 and 2);
    - B25 (4 window queries instead of 2);
    - B26 (the hook fires again during the MMS fetch, so `rows` has 6 entries, not 4).
  - **Fix.**
    - Set M-L8 (a)'s failing set to {B2, B4, B19, B21, B25, B26}.
    - Restore "M-L8 (a)" in the "Bitten by" column of rows B2, B4, B21, B25 and B26 of table 4.8.
    - Replace the sentence "Only B19 has an MMS watermark above 0 in a fetch" and the Iteration 2 table's "B2's MMS watermark is −1" with: "the mutated branch reads the SMS watermark and sends SMS queries for the MMS type."
- **M-L4 (a)** (`:3540-3543`).
  - **Why B26 fails.** B26's hook is keyed on `startsWith("date > ? AND date <= ?")`, which the mutated window text `date >= ? AND …` no longer matches. So the window-time restore step never runs, `h.sms.rows` ends at 3, and `hasSize(4)` fails.
  - **Fix.** Add B26 to M-L4 (a)'s set, making 9 tests, and add M-L4 (a) to B26's "Bitten by" column.

**O-2 — A concurrent restore's pruning is the same class as 2.9's "concurrent backup pruning", but it is not named.** `recordRestoredSms` prunes against the stored watermark, which the same restore may just have raised above this backup's start watermark (`RestoreWorker.kt:464-468`). Entries in (start − W, raised − W] can then disappear between the fetch's two reads, and those rows are selected again: one duplicate per row, never a loss, and `late` may then exceed `min(lateBefore, max)`. Fix: in 2.9's last bullet and 2.4 "Budget", say "a concurrent backup **or restore** pruning".

**O-3 — The I9 Gradle total omits committed foreign tests.**
- **The gap.** I9 step 2 expects `tests=<n> + 50 + Fd`, where `Fd` counts only foreign untracked or modified test files. Step 3 accounts for foreign *committed* tests (`C_head − C0`, listed by `foreign-test-commits-end`), but the Gradle formula does not. D17 would then stop on a commit by another story that adds tests.
- **Likelihood.** Very unlikely in this serial sprint, but the AC-27 wording anticipates it.
- **Fix.** Expect `tests=<n> + 50 + Fd + Fc`, where `Fc` is the number of test cases added by the commits that `foreign-test-commits-end` lists (normally 0), and name `Fc` in D17.

**O-4 — Small consistency points (no effect on the run):**
- S2-0 step 2 expects seven ` M` paths. After D13 (Javadoc left out), `MailTransport.java` is unmodified and there are six. Fix: add "(six after D13)".
- The `git hash-object -w` blobs are unreachable loose objects. Git keeps them for two weeks (`gc.pruneExpire`), which is ample for a two-session run. Fix (optional): note in 5.1 that session 2 should start within that time, or that the pristine copies can be rebuilt only while the blobs exist.

## Checks that hold (re-derived)

| Item | Plan says | I found |
|------|-----------|---------|
| Production `old_string`s (iteration 2) | each exactly once | each exactly once at `c8e98a7f` |
| Java compile | `--release 17 -Werror -Xlint:unchecked` clean | clean (API 35, stub `App`) |
| New and rewritten Java mutants | unique, compile | M-L19 i/ii, M-L23 to M-L26 and 8 sampled others: unique, compile |
| W2 vectors | seven, computed two ways | all seven recomputed in Python and by running the scratch Java: equal |
| B3 under the new fetch code | `late, f0, f1` / `f2` / nothing | the same; fails under M-L19 (i) and (ii) |
| B26 | restored rows not uploaded; fails under M-L26 | the same, traced through `beforeQuery` and the second read |
| Failing sets | complete | M-L1, 2 (i), 3 (3 h), 5, 7, 10, 12 (a), 15, 16 (a), 17, 18 (b), 19 (i), 19 (ii), 21 to 26 match; M-L8 (a) and M-L4 (a) short (O-1) |
| Record 18 coverage | every test bitten, every id detected | holds (including B2: M-L1, M-L5, M-L18 (b)) |
| Totals | 50 tests, 26 ids, 37 runs, 47 Gradle calls | the same |
| Two sessions | blob-rebuilt pristine copies, checkable hand-over | sound; nothing on disk lost; D22 covers every mismatch |
| Iteration-1 observations | all addressed | closed; O-1's new form gives the two mismatches above |
