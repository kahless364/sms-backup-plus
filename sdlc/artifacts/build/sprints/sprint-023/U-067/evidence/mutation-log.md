# U-067 mutation log (AC-6)

Protocol: U-067 plan section I6 (U-065 AC-7 / U-066 AC-9 form: write-ahead entry with clock time, one Edit, one targeted run, restore by copying the pristine file, hash and porcelain check, `restored` line). Repository-relative paths.

- START: `c9770c303690a95b6200e82c73316ba8ce2bb4e8` (documents only since `4095db28`; `git diff --stat 4095db28 HEAD -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat` printed nothing)
- BW_BLOB (HEAD `BackupWorker.kt`): `5c554ec3bf737383f5a8717fdfc518e5eac33993`
- RW_BLOB (HEAD `RestoreWorker.kt`): `871ec7c764be5f9557fdc25ec0683cb835cfeac7`
- BW_NEW (fixed `BackupWorker.kt`): `eb535f5c9c384e639f5077ce8ed3bd0ad7af350d`
- RW_NEW (fixed `RestoreWorker.kt`): `ffa9c57e90761dc56a5967e06b303e2d9080f81b`
- PROD_DIFF (`git diff HEAD -- app/src/main k9mail-vendored | git hash-object --stdin`): `97045c8341925a9d34cc0f4f4560269b3d237bd1`

Recovery note: If a session is lost while an entry says `APPLYING` and has no `restored` line, nothing else runs (no gate, no review, no commit) until the file's hash has been compared with `BW_NEW` or `RW_NEW` and, if it differs, the file has been restored from its pristine copy. If that copy is lost: `git cat-file --filters --path=<path> <BW_NEW|RW_NEW> > <path>`. This applies the checkout conversion, so the file gets its working-tree CRLF endings, byte for byte the same as the normal restore by `cp` from the pristine copy, and `git hash-object` equals the saved blob. `git checkout` is never used, because it would discard the fix.

Class set per entry: NEW3 = `BackupWorkerLoopCancellationTest`, `RestoreWorkerIntegrationTest`, `RestoreWorkerCheckpointTest` (31 cases).

Entries:

### M-1a APPLYING 2026-11-21T10:36:27Z
- file: app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt; hash before: eb535f5c9c384e639f5077ce8ed3bd0ad7af350d
- edit: site 1 call line `currentCoroutineContext().ensureActive()` -> `coroutineContext.ensureActive()` (old binding)
- class set: NEW3 (31 cases); expected: only backupCursors_cancelledAfterSecondAppend_stopsAtNextPass fails (appends expected 2, was 4)

- run_exit: 1 (evidence/mutations/M-1a.txt; cases M-1a-cases.txt; message M-1a-message.txt)
- cases: 31; failed: 1 (backupCursors_cancelledAfterSecondAppend_stopsAtNextPass: expected 2 but was 4, AssertionError); passed: 30
- expected: matches; verdict: DETECTED (the mutated line equals HEAD's old binding; the diff against HEAD at the site was empty, which shows the mutation present)
- restored: 2026-11-21T10:39:12Z hash-ok diff-hash-ok porcelain-ok

### M-1b APPLYING 2026-11-21T10:39:17Z
- file: app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt; hash before: eb535f5c9c384e639f5077ce8ed3bd0ad7af350d
- edit: site 1 call line -> `if (false) currentCoroutineContext().ensureActive()` (check deleted)
- class set: NEW3 (31 cases); expected: only backupCursors_cancelledAfterSecondAppend_stopsAtNextPass fails

- run_exit: 1 (evidence/mutations/M-1b.txt; cases M-1b-cases.txt; message M-1b-message.txt)
- cases: 31; failed: 1 (backupCursors_cancelledAfterSecondAppend_stopsAtNextPass: expected 2 but was 4, AssertionError); passed: 30
- expected: matches; verdict: DETECTED (mutated line confirmed before the run: `332:                if (false) currentCoroutineContext().ensureActive()`)
- restored: 2026-11-21T10:41:40Z hash-ok diff-hash-ok porcelain-ok

### M-2a APPLYING 2026-11-21T10:41:40Z
- file: app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt; hash before: ffa9c57e90761dc56a5967e06b303e2d9080f81b
- edit: site 2 (E5 three lines) call line -> `coroutineContext.ensureActive()` (old binding)
- class set: NEW3 (31 cases); expected: only doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass fails (importCalls expected 2, was 4)

- run_exit: 1 (evidence/mutations/M-2a.txt; cases M-2a-cases.txt; message M-2a-message.txt)
- cases: 31; failed: 1 (doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass: expected 2 but was 4, AssertionError); passed: 30
- expected: matches; verdict: DETECTED (mutated line confirmed before the run: `311:                    coroutineContext.ensureActive()`)
- restored: 2026-11-21T10:44:40Z hash-ok diff-hash-ok porcelain-ok

### M-2b APPLYING 2026-11-21T10:44:40Z
- file: app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt; hash before: ffa9c57e90761dc56a5967e06b303e2d9080f81b
- edit: site 2 (E5 three lines) call line -> `if (false) currentCoroutineContext().ensureActive()` (check deleted)
- class set: NEW3 (31 cases); expected: only doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass fails

- run_exit: 1 (evidence/mutations/M-2b.txt; cases M-2b-cases.txt; message M-2b-message.txt)
- cases: 31; failed: 1 (doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass: expected 2 but was 4, AssertionError); passed: 30
- expected: matches; verdict: DETECTED (mutated line confirmed before the run: `311:                    if (false) currentCoroutineContext().ensureActive()`)
- restored: 2026-11-21T10:47:17Z hash-ok diff-hash-ok porcelain-ok

### M-3a APPLYING 2026-11-21T10:47:17Z
- file: app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt; hash before: ffa9c57e90761dc56a5967e06b303e2d9080f81b
- edit: site 3 (E6 three lines) call line -> `coroutineContext.ensureActive()` (old binding)
- class set: NEW3 (31 cases); expected: executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass (rows expected 3, was 5) and executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing (expected 0, was 5) fail

- run_exit: 1 (evidence/mutations/M-3a.txt; cases M-3a-cases.txt; message M-3a-message.txt)
- cases: 31; failed: 2 (executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass: expected 3 but was 5; executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing: expected 0 but was 5; AssertionError both); passed: 29
- expected: matches; verdict: DETECTED (mutated line confirmed before the run: `399:                    coroutineContext.ensureActive()`)
- restored: 2026-11-21T10:50:16Z hash-ok diff-hash-ok porcelain-ok

### M-3b APPLYING 2026-11-21T10:50:16Z
- file: app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt; hash before: ffa9c57e90761dc56a5967e06b303e2d9080f81b
- edit: site 3 (E6 three lines) call line -> `if (false) currentCoroutineContext().ensureActive()` (check deleted)
- class set: NEW3 (31 cases); expected: the AC-3 and AC-4 tests fail

- run_exit: 1 (evidence/mutations/M-3b.txt; cases M-3b-cases.txt; message M-3b-message.txt)
- cases: 31; failed: 2 (executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass: expected 3 but was 5; executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing: expected 0 but was 5; AssertionError both); passed: 29
- expected: matches; verdict: DETECTED (mutated line confirmed before the run: `399:                    if (false) currentCoroutineContext().ensureActive()`)
- restored: 2026-11-21T10:52:53Z hash-ok diff-hash-ok porcelain-ok

## Summary

| Id | Site | Failing of 31 | Failing tests | Expected | Verdict |
|----|------|---------------|---------------|----------|---------|
| M-1a | 1 (BackupWorker, old binding) | 1 | backupCursors_cancelledAfterSecondAppend_stopsAtNextPass | matches | DETECTED |
| M-1b | 1 (check deleted) | 1 | backupCursors_cancelledAfterSecondAppend_stopsAtNextPass | matches | DETECTED |
| M-2a | 2 (runImapRestoreLoop, old binding) | 1 | doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass | matches | DETECTED |
| M-2b | 2 (check deleted) | 1 | doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass | matches | DETECTED |
| M-3a | 3 (executeRestoreWithValues, old binding) | 2 | executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass; executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing | matches | DETECTED |
| M-3b | 3 (check deleted) | 2 | same two | matches | DETECTED |

No mutation was invalid (all six compiled), none went undetected, no entry lacks a `restored` line. The three controls and every other case passed in every run. Each restore was by copy from the pristine file; the hashes and the diff hash equalled BW_NEW, RW_NEW and PROD_DIFF after every entry. The mutation (a) edits leave the now-unused import `kotlinx.coroutines.currentCoroutineContext` in place; it compiles.
