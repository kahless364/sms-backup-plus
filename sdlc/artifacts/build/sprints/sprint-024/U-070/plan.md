---
artifact_type: plan
story_id: "U-070"
gate_id: "planning"
checkpoint: "planning"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-21T12:09:01Z"
iteration: 2
base_commit: "2aad7d709d1f90e510d96369ed64d26b4e9d80d0"
contracts_verified:
  - CNTR-MODERNIZATION-007
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-003
  - CNTR-MODERNIZATION-002
risks_identified: 13
---

# Implementation Plan: U-070 — Back Up an SMS That Becomes Visible After Later Messages Were Backed Up, Exactly Once (6-Hour Look-Back and a Local Record)

Story: `sdlc/artifacts/build/sprints/sprint-024/U-070-late-visible-sms-are-backed-up.md` (read in full, including the
binding "Orchestrator rulings before planning", Record 16).
Requirement: `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-020-backup-completeness-for-late-visible-messages.md`
(approved; AC-1 to AC-24 implemented here, AC-25 not this story's).
Source bug: `sdlc/artifacts/stories/BUG-021-delayed-otp-sms-skipped-by-backup-watermark.md` (its Suggested Approach is the
starting point; where the story corrected it, the story wins).
Model plans: `sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md` (with its Amendment A1 lessons built in from the
start) and `sdlc/artifacts/build/sprints/sprint-022/U-066/plan.md`.

## Iteration 2 (after the plan gates, 2026-11-21)

The gates of iteration 1 were:
- plan-review **FAIL** (blockers B-1, B-2; observations O-1 to O-10);
- contract-verification **PASS** (O-1 to O-4);
- expert-depth-check **PASS**, with two recommendations (criteria 3 and 8).

The reports are `plan-review.md`, `contract-verification.md` and `expert-depth-check.md` in this workspace, read in
full. The orchestrator's rulings on them are binding: story Implementation Notes, "Rulings after the plan gates of
iteration 1", Record 18. Every change is made in place. The table says where. No finding is declined.

**Verified in this iteration, outside the repository.** The production Java of the plan (P1 to P5, as now written) was
extracted from this file by a script and applied to `HEAD` copies in the planner's scratch folder:
- it compiles with `javac --release 17 -Werror -Xlint:unchecked` against `android-35/android.jar` and
  `androidx.annotation` 1.2.0 (stub `App`, `Consts`, `MmsConsts`, `DataType` and `ContactGroupIds`);
- each of the 32 Java mutants of I8 has a unique `old_string` and compiles;
- the five Kotlin mutation anchors are unique in `HEAD` with P6 and P7 applied;
- the seven digest vectors of T5 were computed in Python and by a scratch Java copy of the planned `digest`, with equal
  results.

Kotlin test text was not compiled (no Kotlin compiler outside Gradle).

| Finding | Change, and where |
|---------|-------------------|
| plan-review **B-1** (B3 fails on correct code) | T3, B3: the five backed-up rows are uploaded in one first run and asserted, exactly as the report's fix says. Re-derived under the new fetch code (below): correct code `late, f0, f1` / `f2` / nothing; M-L19 (i) and (ii) both fail it (I8) |
| plan-review **B-2** (M-L19 (i) cannot bite B2) and ruling "every test bitten, every id detected" | M-L19 (i) now lists B3 only. Table 4.8 row B2 is "M-L1, M-L5, M-L18 (b)". The plan-review's "M-L8 (a)" is corrected: B2's MMS watermark is -1, so M-L8 (a) never reaches it. The whole test list was checked against the table. Four tests were bitten by no mutation, and each now has one single-edit mutation: S3 (new **M-L22**: Skip's SMS branch also taken for MMS), S4 (new **M-L23**: Skip writes `max`), W2 (new **M-L24**: the digest over UTF-8 again), W6 (new **M-L25**: the restore write's pruning removed). The new test B26 is bitten by new **M-L26**. Every one of the 50 tests is now in at least one must-fail set (4.8, "Bitten by"), and every id has a non-empty set (I8) |
| plan-review O-1 and ruling "exact failing sets" | Every I8 row now gives the **complete** failing set, re-derived test by test against the iteration-2 code. These are no longer must-fail plus "also". A row is met when the observed failing set equals the listed set (D16). The reviewer's additions are included: M-L2 S1, B18; M-L3 (3 h) B1, B14; M-L19 (ii) S5. So are others found in the re-derivation, for example M-L19 (ii) B16, M-L2 B4 and B22, and M-L5 R3 |
| plan-review O-2 and ruling (rows the SQL group clause excludes) | 2.8 new item 5 (Signatures moves to item 6): such rows are never recorded, so a widened group uploads the matching SMS of the last 6 h. Neither a duplicate nor a loss, and bounded by W. It is the opposite of item 4 (rows the converter rejects are recorded and not looked at again). Both sentences are in I10's declared items and section 11 item 7, for the next requirements pass |
| plan-review O-3 and ruling (digest input encoding) | P1 `digest` hashes the canonical text's UTF-16 code units, two bytes per char, big-endian, by hand, not through a charset encoder. The `StandardCharsets` import is gone. 2.5 gives the injectivity argument. T5 (W2) has seven new vectors, three of them unpaired-surrogate or `?` bodies that must differ. L21 is updated. M-L24 reverts to UTF-8 and is caught by W2 |
| plan-review O-4 and ruling (fresh store) | T7 `moveToAnotherPhone()` clears every file the descriptor excludes (after the copy, before the owner signs in again), so the test starts from what a restored phone has. 4.7 says what the test shows and does not show. New probe N7 and row D11 cover a secret store that does not survive the clear |
| plan-review O-5 | T1 KDoc of `SimulatedProcessDeath` corrected (in the backup path `doWork` turns it into `Result.failure`, after the same persisted state as a real death; it escapes the restore loop) |
| plan-review O-6 and ruling (counts and own files scoped) | I0 records `C0` (HEAD, tracked) and the foreign untracked tests. I9 counts this story's seven files separately, lists every other untracked or uncommitted test file as "not this story's", and explains the Gradle total from them. The "own files" check is limited to this story's path list. The "no code commit by anyone else" check covers this story's paths only. This is the U-068 iteration-2 scoping and the three binding corrections in the U-068 story (D17, constraint 12) |
| plan-review O-7 | I6 expects `@@ -87 +87,4 @@` and `@@ -276,0 +280,6 @@` for `README.md` |
| plan-review O-8 | P5b: the forward query sits in a `try`, and a `RuntimeException` closes the window cursor before it is rethrown |
| plan-review O-9 and ruling (two sessions) | New 5.1 "Sessions and hand-over". Session 1 is I0 to I7; session 2 is S2-0 (resume) and I8 to I11. Safe stopping points: after I1b, after I7, between I8 entries, before I9. The hand-over state is defined, with the resume step, which rebuilds the pristine copies from the stored blobs |
| plan-review O-10 | `base_commit` is `2aad7d70`. Line references are `BackupWorker.kt:423` (the seam's write) and `:355-357` (the else-branch) in 2.10 and section 9 |
| contract-verification **O-1** and ruling (concurrent restore) | P5b reads the record a second time, after both provider queries have returned, and filters window and forward rows against that read. The first read only sizes the forward `LIMIT`. 2.4 gives the ordering argument, and 2.7 has a new row. New test **B26** (a provider double hook performs the restore's two steps during the queries). New mutation **M-L26** (the second read replaced by the first) |
| contract-verification O-2 | Section 11 item 8: CNTR-MODERNIZATION-007's stale signatures (`appendMessages` returns `long`; `importMessageBody` exists) go to the next contract amendment |
| contract-verification O-3 | I9 records `git status --porcelain` and `git log $START..HEAD` for `sdlc/artifacts/design`, `requirements` and `build/change-records`. It checks that no file of this story's diff lies there. A change by someone else is recorded, not a stop |
| contract-verification O-4 | `contracts_verified` and section 8 now name CNTR-MODERNIZATION-003 (the backing-store invariant: `credentials.xml` excluded, the record in the default file) and CNTR-MODERNIZATION-002 (`pinned_certs` excluded) |
| expert-depth C3 and ruling (compute the trade-offs) | 2.5: collision figures computed; the privacy statement (what the file reveals, what it does not, the short-code recovery cost, the clear dates, the private file carried by Android's own backup, withheld codes recorded hours late); key stretching (PBKDF2) weighed and **rejected**, with the cost and the reason. 2.1 gains row 17 (`apply()` against `commit()`) and row 18 (a 32-bit digest). 2.6 gains the cumulative write cost, with the realistic bound for a large first backlog and for a burst inside one window |
| expert-depth C8 and ruling (edge cases, R6) | 2.9 states: clock moved backwards; time zones and daylight saving; dual SIM; network re-delivery; non-key column updates; a concurrent backup pruning between the two reads. R6 is resolved from the code: distinct unique-work names with REPLACE (`WorkManagerScheduler.kt:154`, `:289`, `:324`, `:365`, `:400`); no `android:process`, so one process and one `SMS_RECORD_LOCK`; worst case one duplicate per row both workers select, never a loss |
| expert-depth C1 note (label) | 2.0: "Transactional Outbox" replaced by "a consumer's checkpoint committed atomically with its deduplication state" (Kleppmann, *DDIA*, ch. 11, "Fault Tolerance"). Only the restore's write-ahead entry is Outbox-like, and the plan says so |
| depth-check consistency note (timestamp) | `timestamp` is from `date -u` |
| BUG-024 (placed after this story) | B12 (`ac9_cancelledDuringTheAppend_…`), `ProviderDouble` and `RecordingTransport` are kept as they were. The only additions are an optional `beforeQuery` hook on `ProviderDouble` (null by default) and the KDoc fix. Section 11 item 9 |

**Totals after iteration 2:**
- 50 new tests (iteration 1: 49; added B26);
- 26 mutation ids (was 21; added M-L22 to M-L26) in 37 runs (was 32);
- 47 Gradle invocations (was 42).

**Planning state.**
- Iteration 1 was written at `c9770c30`; this iteration at `2aad7d70`. Between the two:
  - U-067 landed (commit `c12a2788`);
  - U-068 has begun committing its increments (`533e9f51` JUnit XML on; `2aad7d70` its receiver test);
  - the rest are documents.
- `git diff --name-only c12a2788 HEAD -- app README.md build.gradle app/build.gradle` lists only
  `BackupBroadcastReceiverTest.java` and the root `build.gradle`, neither of them this story's. The uncommitted U-068
  work in the checkout (`WorkManagerScheduler.kt`, `SmsReceiverTest.java`, `WorkManagerSchedulerTest.kt`) is not this
  story's either.
- Code facts were read from `HEAD` with `git show HEAD:<path>`. No Gradle was run.
- The start count is taken at I0, after U-067, U-068 and U-069 have landed (row D1).

**Size.**
- Production: 2 new Java files, 5 edited files and 1 comment-only edit:
  - new: `preferences/SmsBackupWindow.java`, `service/LateVisibleSmsCursor.java`;
  - edited: `preferences/DataTypePreferences.java`, `service/BackupQueryBuilder.java`, `service/BackupItemsFetcher.java`,
    `service/BackupWorker.kt`, `service/RestoreWorker.kt`;
  - comment only: `mail/transport/MailTransport.java`.
  - Plus two README insertions.
- Tests: 7 new files, **50 new tests**. No existing test file is edited.
- Runs: 47 Gradle invocations (I0 1, I1 2, I2 1, I3 1, I4 2, I5 1, I7 1, I8 37, I9 1), in two sessions (5.1).

**Scope Concern 7 (size, Record 16 ruling "not split").** The story can be planned as one. It is large, but the parts
cannot be separated safely (story Scope Concern 11): without the restore write the look-back sends restored messages back
to Gmail, and without the upgrade seeding it sends the last six hours again. Verdict PASS, not NEEDS_INFO. If the
orchestrator still wanted a cut, the only safe seam is between I5 (restore) and I6 (documents), and that cut buys nothing.

**Binding rulings** (story Implementation Notes, Record 16, and "Binding Rulings Carried In"):
- DP-1: W = 6 h. DP-2: SMS only. DP-3: a row hidden at Skip time is uploaded when released within W. DP-5: a duplicate is
  preferred to a loss, and this fix introduces none beyond the two accepted ones.
- SC-1: AC-21 stands as the story reads it.
- SC-2: no count cap.
- SC-3: a comment-only `MailTransport.java` Javadoc correction is allowed if proven by diff and by identical class bytes.
- SC-4: BUG-021 moves to review after this story's QA; it does not close.
- SC-6: same millisecond, address and text is accepted.
- SC-7: not split.
- Order: after U-067, U-068 and U-069; line numbers are re-read at the start.
- Record 18, the two items of iteration 1:
  - rejected rows above the watermark are recorded (confirmed, inside ruling 25);
  - AC-22 applies to the log lines this story adds or changes, checked at DEBUG and above.
- Record 18, after the gates:
  - the mutation table is met when every new test fails under at least one mutation and every id is detected;
  - rows the SQL group clause excludes are never recorded (accepted);
  - the digest hashes the raw characters;
  - the record is read after the provider queries;
  - two developer sessions;
  - the privacy statement, with a decision on a slower digest;
  - BUG-024 relies on B12, the provider double and the recording transport.

## 1. Assumption Ledger

`verified` means read in this planning session, at the location given. `needs_check` means a developer step, with a
decision row for each outcome (section 7). Library facts were read with JDK 25 `javap` from jars in the local Gradle and
Maven caches (Robolectric `shadows-framework-4.15.1.jar`; Robolectric `android-all-instrumented-10-robolectric-5803371-i7`,
which is the Android 10 framework Robolectric runs at `sdk=29`). The jars were extracted to the planner's session scratch
folder. Nothing was written to the repository.

| # | Claim | Status | Evidence |
|---|-------|--------|----------|
| L1 | The forward SMS query is `date > ? AND type <> ?` plus `groupSelection`, args `[getMaxSyncedDate(SMS), "3"]`, projection null. The sort is `date` or `date LIMIT n`, with a LIMIT only when `max > 0`. The group clause starts with " AND", so the selection with a group has two spaces before `AND (` | verified | `BackupQueryBuilder.java:60-63`, `:103-116`, `:149-163`; `BackupQueryBuilderTest.java:38-40`, `:51` |
| L2 | `BackupItemsFetcherTest` builds the fetcher over a Mockito mock of `BackupQueryBuilder` and a mock resolver. `shouldGetItemsForDataType` asserts `verifyNoInteractions(resolver)`. A new builder method on the mock returns null, 0 or false | verified | `BackupItemsFetcherTest.java:32-49`; Mockito 5 defaults |
| L3 | `BulkFetcher.fetch` walks the types in order and reduces `max` by `cursor.getCount()` of each type's cursor. It breaks at 0. It is called at `BackupWorker.kt:251` and `:457` only | verified | `BulkFetcher.java:19-36`; `git grep -n "BulkFetcher(" -- app/src/main` |
| L4 | `BackupCursors` calls `getCount`, `isLast`, `moveToNext`, `moveToFirst` and `close` on each cursor. `MessageConverter.getMessageMap` calls `getColumnNames`, `getColumnIndex` and `getString`, and maps an `SQLiteException` to "[BLOB]" | verified | `BackupCursors.java:30-86`; `MessageConverter.java:218-238` |
| L5 | One `ConversionResult` holds at most one message, plus that row's full column map (`getMapList()`). `maxDate` starts at -1 and is the `X-smssync-date` header, which for SMS is the `date` column. The existing tests build results with an empty map or no message | verified | `MessageConverter.java:140-154`; `ConversionResult.java:9-53`; `HeaderGenerator.java:67`; `BackupWorkerWatermarkTest.kt:328-334`; `BackupWorkerIntegrationTest.kt:233` |
| L6 | Rows the converter does not upload: an empty address, or a person outside the chosen contact group (both for any SMS type). The loop then logs "no messages converted" | verified | `MessageGenerator.java:84-89`, `:308-313`; `BackupWorker.kt:354-357` (HEAD) |
| L7 | `Preferences(Context)` creates a new `DataTypePreferences` per instance over the one default preferences object. `Preferences(Context, SharedPreferences)` is public. The `DataTypePreferences` constructor is package-private. `isFirstBackup()` reads only the three `maxSyncedPreference` keys | verified | `Preferences.java:65-74`, `:220-227`; `DataTypePreferences.java:21-24` |
| L8 | The watermark keys are `max_synced_date`, `max_synced_date_mms` and `max_synced_date_calllog`. MMS is stored in seconds and returned ×1000 when above 0. `setMaxSyncedDate` writes outright with `commit()` | verified | `DataType.java:76-78`; `DataTypePreferences.java:59-70` |
| L9 | Android's own backup: `allowBackup="true"`, `fullBackupContent="@xml/backup_descriptor"`. The descriptor excludes only `sharedpref` `credentials.xml` and `pinned_certs.xml`. There is no `dataExtractionRules` resource | verified | `AndroidManifest.xml:95-96`; `backup_descriptor.xml:2-8`; `ls app/src/main/res/xml` |
| L10 | Restore inserts SMS only of type inbox or sent, after `smsExists` (`date = ? AND address = ? AND type = ?`, projection `_id`). It raises the SMS watermark only when higher, after the insert, then checkpoints. `executeRestoreWithValues` is `internal` and reaches the same `insertSmsValues` | verified | `RestoreWorker.kt:447-482`, `:599-617`, `:378` (HEAD) |
| L11 | Robolectric 4.15.1 `ShadowContentResolver.query` (5-arg) delegates to the provider registered for the authority. Without one, and with no cursor set, it returns null. `insert` delegates to `provider.insert` without wrapping its exceptions | verified | `javap -c ShadowContentResolver`: `query` `:387-420`, `insert` `:228-265` of the disassembly |
| L12 | Robolectric 4.15.1 **shadows `android.database.CursorWrapper`**. `ShadowCursorWrapper` re-implements every `Cursor` method by delegating to a field of its own. It shadows neither `AbstractCursor`, `MergeCursor` nor `MatrixCursor`: the only cursor shadows are `ShadowCursorWrapper` and the three `CursorWindow` shadows | verified | `javap -p -v ShadowCursorWrapper` (`@Implements(CursorWrapper)`); `unzip -l` of the jar |
| L13 | `ShadowLog.getLogs()` returns `LogItem`s with public `type`, `tag`, `msg` and `throwable` | verified | `javap -p ShadowLog`, `ShadowLog$LogItem` |
| L14 | `SharedPreferencesImpl` (Android 10). On load, if `<name>.xml.bak` exists, the main file is deleted and the backup is renamed over it. `writeToFile` renames the main file to `.bak`, writes the whole map to a new main file, `fsync`s it (`FileUtils.sync`) and deletes `.bak`. On failure it deletes the partial main file. One `commit()` is therefore all-or-nothing for every key of the file. The Android 16/17 implementation was not read | verified (Android 10); plausible, not verified (API 36/37) | `javap -c android/app/SharedPreferencesImpl` (`:144-155`, `:997-1095`, `:1164` of the disassembly) |
| L15 | `App.LOCAL_LOGV = BuildConfig.DEBUG`, which is true in debug unit tests. So the verbose lines gated on it run in tests. `PersonLookup.java:76` logs the address at VERBOSE ("Looked up unknown address: …"), and `MessageGenerator.java:311` logs the person record | verified | `App.java:91-92`; files read |
| L16 | `HeaderGenerator`: Message-ID is MD5 of `date + address + "v2" + type`. `References` uses the stored `reference_uid`, which is random when absent. `X-smssync-backup-time` is the wall clock. The `Date` header comes from `setSentDate`, in the JVM default time zone | verified | `HeaderGenerator.java:38-62`, `:108-128`; `MessageConverter.java:80-84` |
| L17 | Last JaCoCo XML on disk (`app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`, dated 2026-11-20, before U-067). LINE: `service` 632/822 = 76.9 %, floor 0.70; `preferences` 313/412 = 76.0 %, floor 0.65 | verified (as of that report); re-measured at I0 | python over the XML; `app/build.gradle:357-365`, `:513-517` |
| L18 | The U-063 tools exist and are read-only: `junit-xml.init.gradle` (turns XML on, prints `U063-DAEMON-JVM`), `sanitize.py`, `junit_totals.py`, `junit_cases.py` and `jacoco_pkgs.py`. Root `build.gradle:30` is `reports.junitXml.required = false` at `c9770c30`; U-068 AC-2 turns it on | verified | file headers; `build.gradle:25-31`; U-068 story `:86`, `:96`, `:129` |
| L19 | The new names are free under `app/`: `SmsBackupWindow`, `LateVisibleSms*`, `ProviderDouble`, `ProviderRow`, `ProviderQuery`, `ContactsProviderDouble`, `RecordingTransport`, `AppendedMessage`, `CountingSharedPreferences`, `SimulatedProcessDeath`, `LateSmsHarness`, `SmsFixture`, `SmsBackupRecordSizeTest`, `BackupQueryBuilderWindowTest`, `registerProvider` and the key `sms_backup_record` | verified | `git grep -l -w <name> -- app` (0 each, working tree including U-067's files) |
| L20 | `MailTransport.java` has no `default` or `static` method. Its abstract methods carry no `LineNumberTable`, so a Javadoc edit cannot change the class bytes. The edit also keeps the line count | verified | `git show HEAD:…/MailTransport.java`, 152 lines, no `default `/`static ` |
| L21 | Iteration 2. The digest test vectors of T5 (W2) are SHA-256 over the length-prefixed canonical text's UTF-16 code units, big-endian (Python `encode('utf-16-be', 'surrogatepass')`, Java UTF-16 lengths). They were computed twice, in Python and by a scratch Java copy of P1's `digest`, with equal results. Example: `entryOf("1700000000000", "+15550100001", "hello")` = `1700000000000:d6a72b9c218f03eb`. The same scratch Java shows that `"\uD800".getBytes(UTF_8)` decodes to `?`. Under UTF-8, bodies `"\uD800"`, `"\uD801"` and `"?"` all give digest `cf8f38b37afe353e`, which is the O-3 defect | verified (Python and Java 25) | session computation |
| L23 | Iteration 2. Backups run under distinct unique-work names, each with `ExistingWorkPolicy.REPLACE`: `INCOMING` `:154`, `REGULAR` `:289`, `BROADCAST_INTENT` `:324`, `backupType.name` for MANUAL and SKIP `:365`, and the restore's `config.uniqueName` `:400` in `WorkManagerScheduler.kt`. So a manual backup, an incoming backup and a restore can run at the same time. No `android:process` appears under `app/src/main`, so all of them run in one process | verified | `git grep -n "enqueueUniqueWork\|ExistingWorkPolicy" HEAD -- app/src/main`; `git grep -n "android:process" HEAD -- app/src/main` (no hit) |
| L24 | Iteration 2. P1 to P5 as written in this plan compile with `javac --release 17 -Werror -Xlint:unchecked` (JDK 25) against `android-35/android.jar`. All 32 Java mutants of I8 have a unique `old_string` and compile. The five Kotlin anchors (M-L11, M-L16 (a), M-L16 (b), M-L20, M-L22) occur exactly once in `HEAD` with P6 and P7 applied | verified (Java compile; Kotlin anchors by text count) | scratch copies extracted from this file by script; stubs for `App`, `Consts`, `MmsConsts`, `DataType`, `ContactGroupIds` |
| L22 | The five Message-IDs of the AC-6 fixture (MD5 of `date + address + "v2" + type`), written with `(at)` for the at sign: `<a47de1477df7a1f56e069f77b15f2cf9(at)sms-backup-plus.local>`, `<679273385a77bd897b6bcb3e625fb929(at)…>`, `<c8d7686d3b767f1559fb59169aa79b09(at)…>`, `<cb725351ed0002dc94fe152f10504f8e(at)…>`, `<e32b0d35778fa5403a48c10cc5087d80(at)…>` | verified (Python) | session computation; cross-checked at I1b |
| N1 | The real `MessageConverter` (real `PersonLookup` over a resolver with no contacts provider, so the person is unknown) converts provider-double rows inside `BackupWorker.doWork` under Robolectric | needs_check | I1 probe; rows D3 and D4. `MessageConverterTest.java:109-129` runs the real generator over a `MatrixCursor`, and `BackupWorkerIntegrationTest` builds the converter in `doWork`, so this is likely |
| N2 | `LateVisibleSmsCursor` (`AbstractCursor` over a `MergeCursor`) behaves under Robolectric as on a device, because neither class is shadowed (L12) | needs_check | I4 run; row D9 |
| N3 | In the Robolectric test JVM, `File(context.applicationInfo.dataDir, "shared_prefs")` lists the default preferences file after a `commit()` | needs_check | I5 run of S10/S11; row D11, with a fallback that still proves AC-17 |
| N4 | The Gradle unit-test JVM's working directory is the `app` module directory, so `user.dir/build/u070/…` is `app/build/u070/…` | needs_check | I1; row D5 (fallback: read the capture from the JUnit XML failure text) |
| N5 | `Mockito.spy` of the Kotlin final class `WorkerEngineFactory` works with the inline mock maker (Mockito 5.18.0 default) | needs_check | I4 run of B25; row D10 |
| N6 | A test that replaces `ShadowContentResolver`'s provider for `sms` mid-test (`registerProviderInternal` again) serves later queries from the new provider | needs_check | I5 run of S10/S11; row D11 |
| N7 | Iteration 2. Clearing the excluded files (`credentials.xml`, `pinned_certs.xml`) in a test leaves `AuthPreferences(context)` and the backup worker able to start. The IMAP user is in the default file (`AuthPreferences.java:204-206`), and the backup path reads no secret (the transport comes from the test factory) | needs_check | I5 run of S10/S11; row D11 |

**Not used, so not verified:** in-memory SQLite under this project's Robolectric mode. The hand-written evaluator is
chosen on its merits (2.1, row 15), so the story's "not verified" item no longer matters.

## 2. Design (decided)

### 2.0 Principles applied

- **Watermarks with allowed lateness.** This is the streaming-systems vocabulary: Akidau et al., "The Dataflow Model",
  *PVLDB* 8(12), 2015, section 2.3 "Windowing and Triggering", and Akidau, Chernyak and Lax, *Streaming Systems*
  (O'Reilly 2018), ch. 3 "Watermarks" and ch. 2's "allowed lateness" (horizon) and garbage collection.
  - The SMS watermark is a heuristic watermark. A withheld SMS is late data.
  - W is the allowed-lateness horizon. Data beyond it is dropped by design (REQ AC-5).
  - The record is the per-window state that the horizon lets us garbage-collect: pruning at `watermark - W`.
  - What the book calls "late data within the horizon updates the result" is AC-1. What it calls "state
    bounded by the horizon, not by history" is AC-20.
- **Idempotent Receiver / duplicate detection on the client side.** Hohpe and Woolf, *Enterprise Integration Patterns*,
  ch. 10, "Idempotent Receiver". IMAP APPEND is not idempotent (REQ fact 13), so the sender keeps the message-identity
  ledger. It is keyed on content (date, address, body), not on a transport id. This is the EIP advice for when the sender
  cannot attach a stable id: here `_id` changes on restore and on a new phone, and the Message-ID includes the type.
- **At-least-once delivery plus deduplication equals effectively once.** The duplicate window is exactly the one
  in-flight append (REQ AC-10). That is the same exposure as today, because the watermark and the record are written in
  **one atomic commit** after the server confirms (L14).
  - Iteration 2, the accurate name: this is a consumer's checkpoint committed atomically with its deduplication state
    (Kleppmann, *Designing Data-Intensive Applications*, ch. 11, "Fault Tolerance": "Idempotence" and "Atomic commit
    revisited"). The position (the watermark) and the set of messages already handled (the record) are one write, after
    the send. It is chosen over two stores.
  - It is not a Transactional Outbox (Richardson, *Microservices Patterns*, ch. 3.3.7). An Outbox records the outgoing
    message *before* it is sent, for a relay. Only the restore's write-ahead entry below is Outbox-like: written before
    the effect it guards.
- **Write-ahead rule.** Mohan et al., "ARIES", *ACM TODS* 17(1), 1992, section 1: log before data. A restore writes the
  row's record entry **before** the provider insert. A crash between the two can only leave an entry for a row that was
  never inserted, which is harmless because the message is already on the server. The other order can leave an inserted
  row unrecorded, which is the duplicate REQ AC-16 forbids.
- **Test doubles** (Meszaros, *xUnit Test Patterns*, ch. 23):
  - **Fake Object**: `ProviderDouble`, a working in-memory provider that evaluates the app's own selection texts;
  - **Test Spy**: `RecordingTransport`, recording every port call and each appended message's identity headers;
  - **Decorator** (Gamma et al., "Decorator"): `CountingSharedPreferences`, which counts commits per key set;
  - **Configurable Test Stub**: the `ContactsProviderDouble`.
- **Mutation testing.** DeMillo, Lipton and Sayward, "Hints on Test Data Selection", *IEEE Computer* 11(4), 1978. The
  M-L table uses these mutant classes:
  - relational-operator replacement (M-L4 (a));
  - constant replacement (M-L2, M-L3, M-L4 (b), M-L13 (b));
  - statement deletion (M-L13 (a), M-L14, M-L15, M-L16 (a), M-L20);
  - statement reordering (M-L11, M-L16 (b));
  - decision negation or widening (M-L5, M-L8, M-L17, M-L18, M-L21, M-L22);
  - variable or expression replacement (M-L10, M-L19, M-L23, M-L24, M-L26);
  - statement deletion added in iteration 2 (M-L25).
- **Seam.** Feathers, *Working Effectively with Legacy Code*, ch. 4, "The Seam Model". The fetch layer reaches the record
  through the `DataTypePreferences` that `BackupQueryBuilder` already holds (IC-3). A mocked builder returns null and the
  old path runs: the null-object seam that keeps `BackupItemsFetcherTest` unedited.

**Anti-patterns named and avoided.**
- **Moving the watermark backwards to look back** (re-select by lowering the watermark). It re-uploads every row of the
  last W each run (2.1, row 2).
- **Bounded-by-count state that silently drops live entries** (an LRU cap). That is a loss path (REQ AC-23; SC-2).
- **Probabilistic membership (Bloom filter) for "already sent".** A false positive is a silent loss (2.1, row 9).
- **Two stores for one invariant** (a separate file or table for the record). Two commits leave a gap where the
  watermark and the record disagree (2.1, row 10).
- **Logging message content** (REQ privacy, AC-22). The new lines carry counts only. A pre-existing debug-build verbose
  line does leak the address (L15). Record 18 rules it out of this story; the orchestrator raises a separate bug
  (section 11 item 3).
- **Check-then-act across a concurrent writer** (iteration 2, contract-verification O-1). Reading the record *before*
  the provider queries would let a restore's write-ahead plus insert fall between the check and the act. The record is
  read again after the queries (2.4).
- **Charset-encoding before hashing** (iteration 2, O-3). A charset encoder replaces malformed chars, so the map from
  strings to bytes is not one-to-one. The code units are hashed instead (2.5).
- **Shadowed framework class in a test.** `CursorWrapper` is replaced by a Robolectric shadow (L12), so a
  `CursorWrapper` subclass would be tested against a different implementation than production. `AbstractCursor` is used
  instead (2.1, row 12).

### 2.1 The fix and the alternatives

**Mechanism (the bug's Suggested Approach, kept, with the changes listed in 2.2).**
- A **window query**, `watermark - W < date <= watermark`, with the forward query's draft exclusion and group clause, no
  LIMIT, sent before the forward query whenever the watermark is above 0.
- A **local record** of the rows uploaded or examined in the window. It is one string preference next to the watermark,
  and each entry is `date:digest16`.
- Window rows found in the record are dropped. The rest are the late-visible rows: they come first and count against the
  per-run limit. The forward query takes the remaining budget.
- The SMS watermark is written as `max(stored, confirmed)`, together with the record, in one `commit()`.
- Skip and an upgrade seed the record. A restore writes ahead. A Reset clears.

| # | Option | Gained | Gives way | Decision |
|---|--------|--------|-----------|----------|
| 1 | Look-back query plus a local record in the watermark's preferences file | Every REQ criterion; no port change; no server call; bounded state; travels with the watermark under Android backup | A second SMS query per run; about 3 KB more per commit in normal use | **Taken** |
| 2 | Widen the watermark backwards (`date > watermark - W`, no record) | No new state | Every SMS of the last W is uploaded again on every run (REQ AC-7) | Rejected |
| 3 | Search the server for the Message-ID before each append | No local state | Needs a new port method (REQ Constraints: no port change without a change record; CNTR-MODERNIZATION-007); one round trip per row (REQ AC-21 forbids extra server calls); the Message-ID includes the type, so an outbox-to-sent row would be appended twice (REQ AC-8); relies on server search semantics | Rejected |
| 4 | An SQLite table of uploaded rows | Indexed lookups | A schema and its migration; a second store that cannot be committed atomically with the watermark (AC-10); the descriptor would also have to be checked for the `database` domain | Rejected |
| 5 | Key on the provider `_id` | Cheap | A restore and a new phone give new ids, so rows are uploaded again (REQ AC-16, AC-19) | Rejected (M-L7) |
| 6 | Key including the row type | Matches the Message-ID | An outbox row that becomes sent is uploaded twice (REQ AC-8) | Rejected (M-L6) |
| 7 | Store date, address and body in clear | Debuggable | Message text and numbers in a file that Android backup copies; no need for it | Rejected for a digest (2.5) |
| 8 | HMAC with a per-install key (RFC 2104) | Resists offline guessing of short texts | The key must travel with the record for AC-17. In the same file it adds nothing against whoever holds the file. In `credentials.xml` (excluded from backup) it breaks AC-17 | Rejected (2.5) |
| 9 | Bloom filter or a count-capped list | Fixed size | A false positive or an evicted live entry is a silent loss (REQ AC-23; SC-2) | Rejected |
| 10 | Record in a separate preferences file | Smaller main file | Two files mean two commits, so watermark and record can disagree after a crash (AC-10) | Rejected |
| 11 | A counted (multiset) record (SC-3) | Closes the same-ms-same-text case | More state and code for a case SC-6 accepts | Not planned (ruled SC-6) |
| 12 | The merged cursor: `AbstractCursor` over a `MergeCursor` (taken); a `CursorWrapper` subclass; a `MatrixCursor` copy | `AbstractCursor`: real framework code in both environments; values come from the provider's own cursor | `CursorWrapper` is shadowed by Robolectric (L12). A copy loses column types, so BLOB columns read differently and the "[BLOB]" mapping (L4) is not preserved | `AbstractCursor` |
| 13 | Window query with or without LIMIT | No LIMIT: recorded rows cannot fill it | Reads every row of the last W each run: normally tens | No LIMIT |
| 14 | Detect "upgrade" by an empty record or by a marker | The marker separates "no record yet" from "record of an empty window" | One more key | Marker (`sms_backup_record_version`) |
| 15 | Provider double: hand-written evaluator or in-memory SQLite | The evaluator throws on any selection it does not know, so a changed query shape fails loudly; it does not depend on Robolectric's SQLite mode or on SQLite's "LIMIT inside ORDER BY" idiom | Some code in the test | Evaluator |
| 16 | At upgrade: seed the window, or upload it | Seeding meets REQ AC-18 (no duplicate) | A message the earlier version lost stays lost (REQ Out of scope) | Seed |
| 17 | `apply()` instead of `commit()` (iteration 2) | The worker thread does not wait for the file write and `fsync` | A write queued but not yet on disk when the process dies is lost, so every row appended since the last flushed write is uploaded again. The duplicate window grows from one in-flight append (REQ AC-10) to every queued row. Today's `setMaxSyncedDate` already uses `commit()` | Rejected (B13 asserts `applies == 0`; M-L12 (b)) |
| 18 | A 32-bit digest, 8 hex characters (iteration 2) | 8 of 31 bytes saved (26 %) | The loss probability per late row rises by 2³², to k·2.3·10⁻¹⁰ (2.5) | Rejected: the saving is small next to the whole file, which every commit rewrites anyway |
| 19 | Key stretching (PBKDF2-HMAC-SHA256, RFC 8018 §5.2) instead of one SHA-256 (iteration 2) | Each guess costs an attacker 10⁴ hashes instead of 1 | About 10 ms per entry on the phone, every window row on every run (2.5). A 6-digit code still falls to 10¹⁰ hashes, which takes seconds to minutes on a GPU | Rejected (2.5, "Key stretching") |
| 20 | Read the record once, before the queries (iteration 1) | One read | A restore that writes ahead and inserts between the read and the queries uploads the restored row: the duplicate REQ AC-16 forbids | Rejected: read again after the queries (2.4; B26; M-L26) |

**Cost of the choice, stated.** Each run reads up to every row of the last W twice: the window query, then the
forward query for the rows above. It also computes a SHA-256 per window row, which is microseconds each. Each uploaded
row's `commit()` rewrites the preferences file with the record, about 31 bytes per entry (2.6).

### 2.2 Where the plan departs from the story's Technical Context, and why

1. **Separator `,` instead of a line break.** The record is one single-line string. The entries hold digits, `-`, `:`
   and hex only, so a comma needs no XML escaping in the preferences file.
2. **Restore pruning uses the stored watermark, not `max(stored, row date)`.**
   - Pruning by the higher, future watermark would remove entries that the current window still needs if the insert then
     fails. The watermark is raised only after a successful insert (`RestoreWorker.kt:462-468`).
   - The recorded set is the same as the story's rule. When the row date is at or above the stored watermark, both rules
     record it. When it is below, both reduce to `date > stored - W`.
3. **Examined rows are recorded above the watermark too.** That is a row the converter rejected while it was a forward
   row. Otherwise it would be re-selected by the window as soon as a later row passes it (AC-21). Entries above the
   watermark already widen the forward LIMIT (story Mechanism 7), so they cannot starve it. The consequence is in 2.8
   item 4.
4. **The "is there a watermark" guard is in the fetch layer only.** The builder methods always build. One guard means
   M-L21 is a real single-site mutation.
5. **The forward cursor is returned unchanged when nothing is late and nothing was filtered.** The in-order path then
   hands `BackupCursors` the very cursor it gets today (AC-6).
6. **Iteration 2: the record is read twice in a fetch.** The first read sizes the forward `LIMIT`. The second, after
   both provider queries have returned, decides which rows are late and which forward rows are dropped (2.4,
   contract-verification O-1). The story's Mechanism 1 reads it once.
7. **Iteration 2: the digest input is the canonical text's UTF-16 code units, big-endian.** The story says only "SHA-256
   over date, address and body"; this fixes the byte form (2.5, Record 18).

### 2.3 State model

Keys, all in the default preferences file (`PreferenceManager.getDefaultSharedPreferences`, `Preferences.java:67`):

| Key | Type | Meaning |
|-----|------|---------|
| `max_synced_date` | long | SMS watermark (existing) |
| `sms_backup_record` | string | Entries `date:digest16`, comma-separated, ordered by date then digest; absent when empty |
| `sms_backup_record_version` | int (1) | **Marker**: the record covers the window below the watermark. Written by the first upload commit (stored watermark ≤ 0), by Skip and by the upgrade seeding. Never written by a restore. Removed by Reset |

What each event does:

| Event | Watermark | Record | Marker | Criterion |
|-------|-----------|--------|--------|-----------|
| First run (watermark absent or ≤ 0) | as today (forward only; -1 sentinel when nothing) | entries of uploaded rows | written by the first upload commit | AC-15 |
| Upload commit (SMS) | `max(stored, confirmed)` | + entries of the appended row; prune `≤ watermark - W` | written if stored ≤ 0 or already present | AC-10, AC-11, AC-20 |
| Examined row (converted to nothing) | unchanged | + entry; prune `≤ stored - W` | unchanged | AC-21 |
| Upgrade (watermark > 0, no marker, first fetch) | unchanged | ∪ visible rows in `(watermark - W, watermark]`; prune | written | AC-16 |
| Skip (SMS in the type set) | outright, newest visible non-draft date (as today) | ∪ visible rows in `(new - W, new]`; prune | written | AC-12, AC-11 |
| Restore insert (write-ahead, before insert) | unchanged by this write; raised after the insert as today | + entry; prune `≤ stored - W` | unchanged | AC-14 |
| Reset or account removal (`clearLastSyncData`) | removed (as today) | removed | removed | AC-13 |
| Android's own backup to a new phone | copied | copied (same file) | copied | AC-17 |

**Upgrade, and why an empty record is treated as "seed first".** An installation upgraded from the version before this
change has a watermark and no record. Without seeding, every SMS of the last 6 hours would be uploaded again (REQ AC-18
forbids that). With seeding, everything visible at that first fetch is treated as backed up, which is today's outcome for
it, and anything that becomes visible afterwards is uploaded. A restore before the first post-upgrade backup adds entries
but no marker, so it cannot hide the need to seed. The marker test in the upload commit (`stored ≤ 0 || marker`) means no
commit can write the marker over a watermark that was never seeded.

**The constant W** (`SmsBackupWindow.LOOK_BACK_MS = 21_600_000L`).
- Derivation: the platform's three hours plus three hours of margin for release-time jitter and clock differences,
  neither measured (REQ Rationale).
- Measured against the watermark, because a backup cannot move the watermark past a withheld message's release time
  before it has seen the message.
- S5-I1 Run T is the only evidence of the delay's length, and a longer delay revisits W by change record.
- It is read by the window query, the seed query (both through `BackupQueryBuilder.windowArgs`) and by
  `SmsBackupWindow.prune`, which every record write uses. Skip, upgrade and restore therefore read it too.
- AC-4's `git grep` finds the one definition.

**Per-run limit (AC-2, AC-21).**
- Window rows not in the record are taken first, up to `max`.
- `remaining = max - taken`. The forward query runs with `LIMIT remaining + (entries above the watermark)` and drops
  recorded rows.
- Already-recorded window rows are not counted. The cursor's `getCount()` is `taken + forward accepted`, so
  `BulkFetcher` passes MMS and call log exactly the remaining budget.
- With `max ≤ 0` there is no limit, as today.

### 2.4 Fetch algorithm (`BackupItemsFetcher.getItemsForDataType`, SMS only)

1. Get the builder's preferences (`getDataTypePreferences()`). If null (a mocked builder), or the type is not SMS, or the
   SMS watermark is ≤ 0: today's single query. That covers first run and the -1 sentinel; no look-back query is sent
   (AC-15).
2. If the marker is absent: run the seed query, write the seed with the marker in one commit, and log the seed line.
3. **First read** of the record (`before`).
4. Run the window query (no LIMIT). Count the rows not in `before` (`lateBefore`).
   `remaining = max − min(lateBefore, max)`, or `max` itself when `max ≤ 0`.
5. If `max ≤ 0` or `remaining > 0`: run the forward query with `LIMIT remaining + entriesAbove(before, watermark)`.
   A `RuntimeException` from it closes the window cursor before it is rethrown (plan-review O-8).
6. **Second read** of the record (`record`), now that both queries have returned.
7. Window rows: recorded in `record` means "already backed up" (counted for the log). Otherwise the row is late, taken
   while `taken < max` (or always when `max ≤ 0`). If any were taken, log the selection line (counts only).
8. Forward rows: drop those in `record` (only possible when `entriesAbove(record) > 0`), and keep up to `remaining`.
9. If nothing is late and nothing was dropped, return the forward cursor, repositioned before the first row (today's
   cursor). Otherwise return `LateVisibleSmsCursor` over `MergeCursor(window, forward)`: the late positions, then the
   kept forward positions.

**Why the second read closes the restore race** (contract-verification O-1; Record 18).
- A restore runs `recordRestoredSms(values)` (one `commit()`) and only then `insert` (P7). The backup reads the record
  again only after its queries have returned.
- Suppose a query returns a restored row R. Then R's insert completed before that query read the provider. R's
  write-ahead commit completed, in the restore thread, before the insert began.
- `SharedPreferencesImpl` updates its in-memory map and serves `getString` under one lock (L14). So the backup's
  second read acquires that lock after the restore released it, and sees R's entry. R is dropped.
- The one remaining order: a row inserted after the queries returned. It is not in this run's cursors at all, so it
  cannot be uploaded by this run, and the next run's second read sees its entry.
- Hence no restored row is uploaded, whatever the interleaving of one restore and one backup in one process (L23).
- **Budget.** The record only grows between the two reads, except for the case in 2.9 "a concurrent backup pruning".
  So the late rows of step 7 are at most `min(lateBefore, max)`, and late plus kept forward rows never exceed `max`.
  When the record grew, the run takes fewer rows than its budget allows; the rest wait for the next run, which is no
  loss.

### 2.5 Privacy (AC-22 and the record)

- **What is stored.** Per row, the date and the first 16 hex characters (64 bits) of SHA-256 over the canonical text
  `date|len(address)|address|len(body)|body`. Lengths are Java `String.length()`, in UTF-16 code units. The text is
  hashed as its **UTF-16 code units, two bytes per char, big-endian**, written by hand (P1 `digest`). No address and no
  text are stored.
- **Why two different (date, address, body) triples never give the same digest input** (iteration 2; plan-review O-3;
  Record 18).
  - (i) **The triple to canonical text is one-to-one.** The text starts with the decimal date and `|`. Then come
    `len(address)` in decimal and `|`. Exactly `len(address)` chars follow, then `|`, `len(body)`, `|`, and exactly
    `len(body)` chars, and the text ends. Every field's extent is fixed by a prefix read before it, so a parser
    recovers the triple uniquely, whatever chars (`|` and digits included) the address and body contain. Address
    `+1555010000` with body `1hello` therefore differs from `+15550100001` with `hello` (T5 vector 4).
  - (ii) **The canonical text to bytes is one-to-one and total.** Each `char` (any 16-bit value, an unpaired surrogate
    included) becomes exactly two bytes, so the byte string has length `2·n` and is decoded back by pairing. A charset
    encoder (`getBytes(UTF_8)`, and also Java's `UTF_16BE` charset) replaces a malformed char with a substitute. That
    makes bodies `"\uD800"`, `"\uD801"` and `"?"` hash alike: Python and scratch Java both give `cf8f38b37afe353e` for
    all three under UTF-8 (L21). The code-unit form gives three different digests (T5 vectors 5 to 7; M-L24).
  - The only conflation left is by design: a null field and an empty field are both "", because neither has text.
- **Collisions, computed** (REQ AC-19, SC-6; expert-depth C3). A loss needs a late row whose `date:digest16` equals a
  recorded entry, so only entries with the same millisecond compete.
  - For k such entries, the chance per late row is k/2⁶⁴ ≈ k·5.4·10⁻²⁰.
  - In normal use k ≤ 1: about 5·10⁻²⁰ per late row, and about 5·10⁻¹⁵ over 10⁵ late rows, which is more late rows
    than the phone will ever see.
  - Even if all 5,000 entries of the stress case shared one millisecond: 2.7·10⁻¹⁶.
  - Ignoring the date entirely, the birthday bound over 5,000 entries is 5,000²/2⁶⁵ ≈ 6.8·10⁻¹³.
  - A collision's effect is one message treated as backed up, a loss. It is far below the accepted SC-6 case (same
    millisecond, address and text), which collides by construction.
  - A 32-bit digest would save 8 of 31 bytes (26 %) and raise the chance to k·2.3·10⁻¹⁰ (2.1 row 18). A longer digest
    buys nothing measurable. 16 hex characters is kept.
- **Where it lives.** In the default preferences file, `MODE_PRIVATE`, next to the watermark. Android's own backup
  copies it with the watermark, which AC-17 requires. The encrypted store (`credentials.xml`, `EncryptedPrefsSecretStore`)
  is excluded from backup (L9). Putting the record there would break AC-17, and an HMAC key in it would too (2.1, row 8).
- **What the record reveals, and to whom** (iteration 2; SC-10; Record 18; expert-depth C3). The file is the app's
  private default preferences file (`MODE_PRIVATE`). It is readable on the phone only with root or a debuggable build,
  and it travels in Android's own backup (L9; CNTR-MODERNIZATION-003 keeps only `credentials.xml` out of it).
  - **A holder of the file can learn:**
    - (a) the time, to the millisecond, and the number of SMS received or sent in the last 6 hours, because the dates
      are stored in clear. That is the whole metadata of the window, even with no message text;
    - (b) the text of a short message whose address and form can be guessed. A 6-digit one-time code is recovered by
      trying its 10⁶ values: 10⁶ SHA-256 evaluations, well under a second on one CPU core;
    - (c) confirmation of any guessed (address, body) pair, such as "Ok" to a known number.
  - **They cannot learn:** any text with real entropy; an address on its own (a guess needs address and body
    together); anything pruned (older than W below the watermark).
  - **Mitigating facts.** On Android 17 a withheld one-time code is recorded only after it becomes visible, at least
    3 hours after receipt, so it has normally expired by the time it is in the record. A code that is not withheld is
    recorded within minutes and may still be valid for its few minutes. The same texts are in the owner's mailbox.
- **Key stretching, decided: not used** (iteration 2; Record 18 asks for a decision; 2.1 row 19).
  - The candidate is PBKDF2-HMAC-SHA256 (RFC 8018 §5.2) with a fixed, public salt. It needs no key, so it would keep
    AC-17.
  - At 10⁴ iterations it costs about 10 ms per entry on the phone. Every window row is hashed on every run (twice a run
    for the two reads' lookups, since entries are recomputed per row). That is tens of rows normally (under 1 s), and
    10 s or more per run at 1,000 rows in 6 hours.
  - What it buys: a 6-digit code goes from 10⁶ to 10¹⁰ hash evaluations, which is seconds to minutes on one GPU. The
    outcome for the case that matters, a short code, does not change.
  - It is rejected: the cost is real on every run, the protection is a delay for short codes only, and the exposure is
    already bounded to 6 hours of history that is also in the mailbox.
  - A secret key would protect only if the key does not travel with the file, and a key that does not travel breaks
    AC-17 (2.1 row 8).
- **Logs.** Two new lines, counts only:
  - `BackupItemsFetcher: look-back seeded <n> SMS already on the phone (upgrade|skip)`;
  - `BackupItemsFetcher: look-back selected <n> late SMS, skipped <m> already backed up`.
  - No exception message is built from row content. B24 asserts all of this, at log priority DEBUG and above (L15;
    Record 18: AC-22 applies to the lines this story adds or changes).

### 2.6 Performance and cost (AC-19, AC-20)

- **Rows examined.** The window's lower bound is exclusive at `watermark - W`, computed from the watermark at the start
  of the run (`windowArgs`). B21 asserts that no returned row is dated at or below `start watermark - W`. The forward
  query returns rows above the watermark only, as today.
- **Queries.** One extra SMS query per fetch: the window. One more, the seed query, only on the upgrade run and once per
  Skip. The XOAuth2 retry fetch repeats both queries, as it repeats the forward query today (B25 counts two window
  queries).
- **Server calls.** None added. A run uploading n rows makes `checkSettings` once, `openFolder` and `appendMessages` n
  times each, and `closeFolders` once. A run selecting nothing makes no transport call. B21 asserts both. The AC-6 capture
  includes the call list of every run, from the start commit.
- **Size** (REQ AC-23; story "Size").
  - An entry is a 13-digit date, `:`, 16 hex and `,`: 31 bytes.
  - Entries are bounded by the SMS dated within W below the watermark, plus entries above it: rows a restore wrote ahead
    of an interrupted insert, and forward rows the converter rejected. Those are normally zero.
  - Normal use (tens of SMS in 6 h): under 100 entries, about 3.1 KB. A 1,000-message day: 31 KB. 5,000: 155 KB. All are
    correct, only larger.
  - The per-row commit cost at the stress case was not measured (SC-8).
- **Cumulative write cost** (iteration 2; expert-depth C3). Every uploaded row's `commit()` rewrites the whole file, as
  today, now including the record.
  - A run uploading n rows while the record holds r entries adds about 31·(n·r + n²/2) bytes of writing, if every row
    stays in the window.
  - **The realistic bound.** The record holds only entries within W below the *current* watermark, and each commit
    prunes. So r is the number of SMS within 6 hours of the row being committed, not the number uploaded in the run.
  - **A large first backlog**, say 20,000 SMS over years with no per-run limit: the watermark walks forward through
    history and the record follows it. r is the number of messages in the 6 hours around each one, typically tens. The
    added writing is about 20,000 × 31 × 50 ≈ 31 MB over the whole backlog, about 1.5 KB per commit, on top of the
    rewrite of the rest of the file that happens today.
  - **A burst inside one window**, n messages within 6 hours, all in one run: r grows to n, and the run adds about
    31·n²/2 bytes. That is 15.5 MB for n = 1,000 and 388 MB for n = 5,000. The per-run limit bounds n when the owner
    sets one.
  - The `fsync` per commit exists today, so the added cost is bytes and XML serialisation only. The stress case is
    accepted unmeasured (SC-8).
  - Z1 runs 200 backups of 50 SMS and asserts the count and length after the 200th are no larger than after the 2nd.
    Both should be 50 entries, 1,549 characters.
- **Coverage (JaCoCo LINE).**
  - New code lands in `service` (fetcher, cursor, builder, worker; about 140 lines) and `preferences` (`SmsBackupWindow`
    and the `DataTypePreferences` methods; about 140 lines).
  - Expected uncovered: the `NoSuchAlgorithmException` catch, and `getShort`, `getFloat`, `getDouble` and `getBlob` of
    the cursor. That is under 15 lines.
  - From L17's figures: `service` ≈ 757/962 = 78.7 % (floor 70 %, margin about 8.7 points); `preferences` ≈
    443/552 = 80.3 % (floor 65 %).
  - Both are re-measured at I0 and I9 (`jacoco_pkgs.py`). A drop below the I0 value is not a stop unless the floor
    fails, but it is reported.

### 2.7 Atomicity and failure windows (AC-9, AC-10, AC-14)

What a process death at each point leaves, for one SMS row:

| # | Point | State left | Next run | Criterion; unit-testable? |
|---|-------|-----------|----------|----------------------------|
| 1 | During the upgrade seed query (read only) | nothing written | seeds again | AC-16; yes (re-run) |
| 2 | After the seed commit | record ∪ seed, marker | no re-seed | AC-16; yes |
| 3 | During the window or forward query, or conversion | nothing written | same selection | AC-9; yes |
| 4 | Append requested, not confirmed (folder open throws, login fails, append throws, cancellation) | watermark and record unchanged (BUG-010 order kept: open, append, then the write) | row selected again (late: window; forward: forward query) and uploaded | AC-9, REQ AC-11; yes (B9 to B12) |
| 5 | Server confirmed, process dies before `commit()` | unchanged | that one row uploaded again: the accepted duplicate, the window that exists today and no wider | AC-10; yes (B13: the transport records, then throws) |
| 6 | Inside `commit()` | the platform's write leaves the old file or the new file (L14), never a mix | 5 or nothing | AC-10; **not unit-testable** (needs a process kill mid-write); Android 10 code read, API 36/37 assumed |
| 7 | After `commit()` | watermark and record together | row not selected | AC-7; yes |
| 8 | Restore: after the write-ahead commit, before the insert | entry for a row not in the provider | harmless: if that SMS later appears with the same triple, it is already on the server | AC-14; not tested (no such failure point in the double; reasoning only) |
| 9 | Restore: after the insert, before the watermark raise | row in the provider, entry present, watermark old | forward filter drops it | AC-14 (b); yes (S8) |
| 10 | Restore: after the raise | as today | window filter drops it | AC-14 (a); yes (S7) |
| 11 | Iteration 2: a restore records and inserts a row while a backup's queries run (no death; a concurrent interleaving) | the backup's second record read holds the entry (2.4 argument) | the row is not uploaded, in this run or later | AC-14, REQ AC-16; yes (B26, M-L26) |

**What `SharedPreferences.commit()` does and does not guarantee** (L14).
- **Does:**
  - it updates the in-memory map, then writes the whole file synchronously on the calling thread, through `.bak`
    rename, write, `fsync` and `.bak` delete;
  - a restart after a crash at any point loads either the previous or the new content;
  - every key in one commit is therefore atomic with every other.
- **Does not:**
  - (a) when `commit()` returns false (I/O error), the in-memory map already holds the new values while the disk holds
    the old ones. This process then behaves as if saved and a restart as if not. That is window 5 again, a duplicate,
    never a loss. The return value is ignored, as `setMaxSyncedDate` ignores it today;
  - (b) nothing across files: the restore checkpoint is in another file, which is unchanged and irrelevant here;
  - (c) no ordering with `apply()` writes made elsewhere: none touch these keys.
- **Concurrency** (iteration 2: SC-9 and R6 resolved from the code, L23).
  - **They can overlap.** Each backup type is enqueued under its own unique-work name with REPLACE, so a manual, an
    incoming, a regular and a broadcast backup can run at the same time, and so can a restore. All run in the one app
    process (no `android:process`).
  - **The lock's scope.** `SMS_RECORD_LOCK` is a `static final` field of `DataTypePreferences`: one object per class
    loader, so one per process. Every `DataTypePreferences` instance of the process (each `Preferences` creates one)
    takes it for every read-modify-write of the SMS watermark and record:
    - the upload commit; the examined-row write; the seed; Skip; the restore write; `clearLastSyncData`.
  - Reads (`getSmsBackupRecord`) take only the preferences object's own lock, which is enough, because each write is a
    single `commit()`.
  - **Worst case.** Two backups that select the same late or forward row before either commits both upload it: one
    duplicate per such row. Today the same happens for forward rows. No loss is possible: the watermark is
    `max(stored, confirmed)` under the lock, and the record is a set union under the lock, so neither a watermark nor
    an entry is ever overwritten by an older value.
  - A restore and a backup: 2.4 and row 11 (no duplicate).
  - Mutual exclusion would need a scheduler change (CNTR-MODERNIZATION-004) and is not this story's.

### 2.8 Behaviour preserved (AC-6, AC-23)

1. **Unedited and green.** The PIN set: `BackupQueryBuilderTest` (all eight), `BackupWorkerWatermarkTest`,
   `BackupWorkerIntegrationTest`, `BackupItemsFetcherTest`, `BulkFetcherTest`, `BackupCursorsTest`, `BackupWorkerTest`,
   U-067's `BackupWorkerLoopCancellationTest`, `PreferencesTest`, `RestoreWorkerIntegrationTest`,
   `RestoreWorkerCheckpointTest`, `RestoreWorkerTest`, `MessageConverterTest` and `K9MailTransportTest`. I8 and I9 show
   `git diff --stat` empty over `app/src/test` apart from the seven new files.
2. **The forward query text is unchanged.** It is shown four ways:
   - `BackupQueryBuilderTest:38-40`, `:51` pass unedited;
   - the builder diff has exactly two hunks, neither touching old lines 103-147 (I3 check);
   - the AC-6 literal (captured on the start commit) holds every header and call;
   - B4 and B16 assert the recorded forward selection text.
3. **The two deliberate outcome changes:**
   - (1) an SMS hidden at Skip and released within W is uploaded (REQ AC-14; S2);
   - (2) a restored SMS that an interruption left above the watermark is not uploaded (REQ AC-16; S8).
   - The late-visible SMS themselves are the feature.
4. **A stated difference: rows the converter rejects are recorded** (Record 18: confirmed, inside ruling 25). An SMS
   the converter rejects (an empty address, or a person outside the chosen group) is recorded as examined (2.2 item 3),
   so no later run looks at it again.
   - A run that finds only such rows no longer logs in to the server, and such a row no longer counts against the
     per-run limit. Today it is re-selected every run until a later row passes it.
   - A row the contact-group rule excluded is not looked at again if the rule is changed later. Today that already holds
     for every row below the watermark.
   - Uploads, headers, folders and watermarks of every row that is uploaded today are identical.
5. **A stated difference: rows the SQL contact-group clause excludes are never recorded** (iteration 2; plan-review O-2;
   Record 18).
   - The window query carries the group clause (`type = 2 OR person IN (...)`), so a row it excludes is never returned,
     never examined and never recorded.
   - If the owner later widens the group (for example to everybody), every such SMS dated within 6 hours below the
     watermark is uploaded by the next backup. Today it is never uploaded.
   - It is neither a duplicate nor a loss, and it is bounded by W. It is accepted.
   - **It is the opposite of item 4.** Rows the converter rejects are recorded and not looked at again; rows the SQL
     clause excludes are not recorded and are looked at again.
   - Both sentences go to the next requirements pass (I10; section 11 item 7).
6. **Signatures and wiring.** `appendBatchAndUpdateWatermark` keeps its signature.
   The `BackupItemsFetcher` and `BackupQueryBuilder` constructors and both `BulkFetcher(` sites are unchanged. Every
   `MailTransport` double compiles unchanged (IC-3).

### 2.9 Edge cases

- **Equal dates.** Two rows with the same date and different address or text have different keys. Rows equal in all
  three share one (SC-6, accepted).
- **Null date.** The SQL `date > ?` and `date <= ?` exclude it. `entryOf` returns null, so the row is never recorded.
- **Null or empty address or body.** Treated as empty text in the key.
- **Watermark -1 sentinel, 0, or absent.** No look-back (≤ 0).
- **Skip on an empty provider.** Writes -1 outright with the marker, as today's -1.
- **Future-dated row** (SC-12). It raises the watermark and shortens the effective window. No clamp; that would be a
  requirement change.
- **Restore insert returns null.** The entry is recorded, the row is absent, which is harmless (2.7 row 8).
- **XOAuth2 retry.** It fetches again. The marker is already written if the first fetch seeded, so it does not seed
  twice.
- **MMS seconds.** The MMS write is exactly `setMaxSyncedDate` (B20).
- **Contact-group change after a row was recorded as examined.** It stays excluded (Record 16 consequence, logged).
- **Provider normalising the address or body on a restore insert.** The keys would then differ and the restored row would
  be uploaded once: a duplicate, not a loss. This cannot be shown on the provider double and is risk R7.
- **Iteration 2 (expert-depth C8):**
  - **Device clock moved backwards** by d after a backup. A new SMS is dated by the clock, so it is dated below the
    watermark. If d < W it falls inside the window and is uploaded once, which today's code does not do. If d ≥ W it
    is not uploaded, as today. A forward jump is SC-12.
  - **Time zones and daylight saving.** `date` is UTC epoch milliseconds. The key, the window and the pruning do not
    depend on the zone. Only the mail `Date` header uses the default zone (L16), and R3 fixes it to UTC.
  - **Dual SIM.** `sub_id` is not in the key. The same text from the same sender in the same millisecond on both SIMs
    gives one entry, which is the SC-6 case. Nothing else changes.
  - **The network delivers the same SMS twice.** The provider holds two rows with different dates: two keys and two
    Message-IDs, and both are uploaded, as today. The fix does not try to de-duplicate network re-delivery.
  - **The SMS app updates columns outside the key** (`read`, `seen`, `status`, `thread_id`, `type`). The key is
    unchanged, so the row is not uploaded again (B8 for `type`). A change to `date`, `address` or `body` changes the key
    and gives one duplicate, never a loss (as R7).
  - **A concurrent backup prunes between this fetch's two reads.** Another backup raised the watermark and pruned
    entries this fetch's older window still covers. Those rows can be selected again: one duplicate, never a loss
    (2.7 "Concurrency").

### 2.10 Start state (iteration 1 written while U-067 was being built; iteration 2 at `2aad7d70`, after U-067)

Iteration 2: U-067 has landed (`c12a2788`). At `HEAD` the anchors read `BackupWorker.kt:207` (Skip), `:355-357` (the
else-branch) and `:423` (the seam's write), and `RestoreWorker.kt:461` (the insert). The iteration-1 text below is kept
for its reasoning; its "after U-067" numbers are superseded by these.

- **What this plan relies on from U-067** (its plan, section 3.1):
  - `BackupWorker.kt`: the import line becomes `import kotlinx.coroutines.currentCoroutineContext`; `backupCursors`
    gains the line `@androidx.annotation.VisibleForTesting(otherwise = …PRIVATE)` and becomes `internal`; the check
    becomes `currentCoroutineContext().ensureActive()`;
  - `RestoreWorker.kt`: the same import line, and the checks at `:311` and `:399`;
  - seven new tests.
- None of this story's `old_string`s contain those lines. Every edit is matched by exact text.
- The text relied on is unchanged by U-067:
  - `executeSkip`'s loop body (HEAD `:205-211`);
  - the loop's else-branch (HEAD `:354-357`, `:355-358` after U-067);
  - the seam's tail (HEAD `:419-423`, `:420-424` after U-067);
  - `insertSmsValues`'s insert line (HEAD `:461`; no line moves in that file).
- **U-068 and U-069 share no file with this story.**
  - U-068's AC-9 lists `BackupWorker.kt` and `RestoreWorker.kt` among the files that must show an empty diff. Its
    `app/src/main` list of nine files contains none of this story's files.
  - U-068 edits root `build.gradle:30`, which turns JUnit XML on. That affects only how totals are read (row D2). It
    leaves `README.md:38` alone (its Scope Concern 3).
  - U-069's AC-13 (a) restricts its `app/src/main` diff to `worker/BackupTriggerWorker.kt`. Its README mention (`:257`)
    is in its "not established" list.
  - I0 verifies all of this from the log (row D1).

## 3. Exact Edits

All production `old_string`s are copied from `HEAD`. Use the Edit tool for existing files: their working tree is CRLF, and
Edit keeps the endings. Use Write for new files. I0 confirms that each single-line anchor below occurs exactly once
(row D6). If Edit reports "not found" or "not unique": stop and report.

### 3.1 New file P1 — `app/src/main/java/com/zegoggles/smssync/preferences/SmsBackupWindow.java`

```java
package com.zegoggles.smssync.preferences;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.Telephony;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * U-070 (REQ-MODERNIZATION-020, BUG-021): the SMS look-back bound W and the local record of
 * the SMS rows a backup has uploaded or examined within W below the SMS watermark.
 *
 * <p>The record is one string preference ({@link #RECORD_KEY}) in the default preferences
 * file, next to the watermark {@code max_synced_date}, so that Android's own app backup copies
 * both together (backup_descriptor.xml excludes only credentials.xml and pinned_certs.xml).
 * Entries are {@code <date>:<digest>}, comma-separated: the row's date in epoch milliseconds
 * and the first 16 hex characters of SHA-256 over the length-prefixed date, address and body,
 * hashed as their raw UTF-16 code units (two bytes per char, big-endian), so that no two
 * different texts, malformed ones included, give the same input. The record holds no address
 * and no text. The key leaves out the row type (an
 * outbox row that becomes sent must not be uploaded again) and the provider _id (a restore
 * and a move to another phone give rows new ids).
 */
public final class SmsBackupWindow {
    /**
     * W, the SMS look-back bound (REQ-MODERNIZATION-020 AC-3): 6 hours.
     *
     * <p>Derivation: Android 17 holds back an SMS containing a one-time code for three hours
     * after receipt (research 2026-11-21, sections 4.5 and 4.6); a margin of three more hours
     * covers release-time jitter and differences between the clock that dates a message and
     * the clock that releases it. Neither is measured. 3 h + 3 h = 6 h.
     *
     * <p>W is measured against the SMS watermark, not against the time the backup runs: a
     * backup cannot move the watermark past a withheld message's release time before it has
     * seen the message (messages carry receipt-time dates, none later than the clock), so a
     * backup that runs late still reaches it. The only evidence of the delay's length is run T
     * of the platform measurement S5-I1 (DES-MODERNIZATION-014, D14); a longer delay revisits W
     * by change record. Every user of W reads this constant: the window and seed queries
     * (BackupQueryBuilder) and the record's pruning, which the upload, Skip, upgrade and
     * restore writes all apply.
     */
    public static final long LOOK_BACK_MS = 21_600_000L;

    /** Preference key of the record. */
    public static final String RECORD_KEY = "sms_backup_record";
    /**
     * Preference key of the record's format version. Its presence means the record covers the
     * window below the watermark: the first backup's upload commit, Skip and the upgrade
     * seeding write it; a restore never does; a Reset removes it.
     */
    public static final String MARKER_KEY = "sms_backup_record_version";
    static final int RECORD_VERSION = 1;

    private static final int DIGEST_HEX_CHARS = 16;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** Orders entries by date, then by text, so the stored record is deterministic. */
    private static final Comparator<String> BY_DATE = new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
            final int byDate = Long.compare(dateOf(a), dateOf(b));
            return byDate != 0 ? byDate : a.compareTo(b);
        }
    };

    private interface Row {
        @Nullable String get(String column);
    }

    private SmsBackupWindow() {
    }

    /** @return the record entry of an SMS, or null when its date is missing or not a number */
    public static @Nullable String entryOf(@Nullable String date, @Nullable String address, @Nullable String body) {
        if (date == null) return null;
        final long millis;
        try {
            millis = Long.parseLong(date.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        final String a = address == null ? "" : address;
        final String b = body == null ? "" : body;
        return millis + ":" + digest(millis + "|" + a.length() + "|" + a + "|" + b.length() + "|" + b);
    }

    /** @return the entry of a converted row's column map (ConversionResult.getMapList), or null */
    public static @Nullable String entryOf(@Nullable final Map<String, String> row) {
        if (row == null) return null;
        return entryOfRow(new Row() {
            @Override
            public String get(String column) {
                return row.get(column);
            }
        });
    }

    /** @return the entry of the cursor's current row, or null */
    public static @Nullable String entryOf(@NonNull final Cursor row) {
        return entryOfRow(new Row() {
            @Override
            public String get(String column) {
                final int index = row.getColumnIndex(column);
                return index < 0 || row.isNull(index) ? null : row.getString(index);
            }
        });
    }

    /** @return the entry of an SMS about to be restored, or null */
    public static @Nullable String entryOf(@NonNull final ContentValues values) {
        return entryOfRow(new Row() {
            @Override
            public String get(String column) {
                return values.getAsString(column);
            }
        });
    }

    /** @return the number of entries dated above {@code watermark} */
    public static int countAbove(@NonNull Set<String> record, long watermark) {
        int n = 0;
        for (String entry : record) {
            if (dateOf(entry) > watermark) n++;
        }
        return n;
    }

    private static @Nullable String entryOfRow(Row row) {
        return entryOf(row.get(Telephony.TextBasedSmsColumns.DATE),
                row.get(Telephony.TextBasedSmsColumns.ADDRESS),
                row.get(Telephony.TextBasedSmsColumns.BODY));
    }

    static @NonNull Set<String> parse(@Nullable String serialised) {
        final Set<String> record = new TreeSet<String>(BY_DATE);
        if (serialised == null) return record;
        for (String entry : serialised.split(",")) {
            if (isEntry(entry)) record.add(entry);
        }
        return record;
    }

    static @Nullable String serialise(@NonNull Set<String> record) {
        if (record.isEmpty()) return null;
        final StringBuilder sb = new StringBuilder();
        for (String entry : record) {
            if (sb.length() > 0) sb.append(',');
            sb.append(entry);
        }
        return sb.toString();
    }

    /**
     * Removes every entry dated at or below {@code watermark - W}: no window below this
     * watermark, or a higher one, reaches it again. Nothing is removed while there is no
     * watermark (0 or below).
     */
    static void prune(@NonNull Set<String> record, long watermark) {
        if (watermark <= 0) return;
        final long bound = watermark - LOOK_BACK_MS;
        for (Iterator<String> it = record.iterator(); it.hasNext(); ) {
            if (dateOf(it.next()) <= bound) it.remove();
        }
    }

    static long dateOf(@NonNull String entry) {
        return Long.parseLong(entry.substring(0, entry.indexOf(':')));
    }

    private static boolean isEntry(String text) {
        final int colon = text.indexOf(':');
        if (colon <= 0 || text.length() - colon - 1 != DIGEST_HEX_CHARS) return false;
        try {
            Long.parseLong(text.substring(0, colon));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * SHA-256 of the canonical text's UTF-16 code units, two bytes per char, big-endian. This is
     * total and one-to-one over Java strings, unpaired surrogates included; a charset encoder
     * (UTF-8, or the platform's UTF-16) would replace a malformed char, so two different bodies
     * could hash alike (plan 2.5).
     */
    private static String digest(String canonical) {
        try {
            final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            final byte[] units = new byte[canonical.length() * 2];
            for (int i = 0; i < canonical.length(); i++) {
                final char c = canonical.charAt(i);
                units[2 * i] = (byte) (c >> 8);
                units[2 * i + 1] = (byte) c;
            }
            final byte[] hash = sha256.digest(units);
            final char[] out = new char[DIGEST_HEX_CHARS];
            for (int i = 0; i < DIGEST_HEX_CHARS / 2; i++) {
                out[2 * i] = HEX[(hash[i] >> 4) & 0xF];
                out[2 * i + 1] = HEX[hash[i] & 0xF];
            }
            return new String(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
```

### 3.2 P2 — `DataTypePreferences.java` (three Edits)

**P2a.** `old_string`:
```
import android.content.SharedPreferences;
import com.zegoggles.smssync.mail.DataType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
```
`new_string`:
```
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.zegoggles.smssync.mail.DataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
```

**P2b.** `old_string` (HEAD `:68-70`):
```
    public boolean setMaxSyncedDate(DataType dataType, long max) {
        return sharedPreferences.edit().putLong(dataType.maxSyncedPreference, max).commit();
    }
```
`new_string`:
```java
    public boolean setMaxSyncedDate(DataType dataType, long max) {
        return sharedPreferences.edit().putLong(dataType.maxSyncedPreference, max).commit();
    }

    // -------------------------------------------------------------------------
    // U-070 (REQ-MODERNIZATION-020, BUG-021): the SMS look-back record, kept beside the SMS
    // watermark in this preferences file (SmsBackupWindow). Each write is one commit() that
    // carries every key it changes; SMS_RECORD_LOCK serialises the read-modify-write across
    // the DataTypePreferences instances of one process (each Preferences creates its own).
    // -------------------------------------------------------------------------

    private static final Object SMS_RECORD_LOCK = new Object();

    /** @return the SMS record's entries (date:digest), oldest first, as a new mutable set */
    public @NonNull Set<String> getSmsBackupRecord() {
        return SmsBackupWindow.parse(sharedPreferences.getString(SmsBackupWindow.RECORD_KEY, null));
    }

    /** @return true when the record covers the window below the SMS watermark (SmsBackupWindow.MARKER_KEY) */
    public boolean hasSmsBackupRecordMarker() {
        return sharedPreferences.contains(SmsBackupWindow.MARKER_KEY);
    }

    /**
     * The write after a confirmed append (BUG-010 order: open the folder, append, then this).
     * MMS and call log: exactly {@link #setMaxSyncedDate}. SMS, in one commit: the watermark
     * becomes max(stored, confirmed), so it never moves down (REQ-MODERNIZATION-020 AC-12); the
     * appended rows join the record; entries no window can reach any more are pruned; and the
     * marker is written only if the stored watermark was not above 0 or the marker is already
     * there, so this write can never hide the need for the upgrade seeding.
     */
    public boolean setMaxSyncedDateAfterAppend(DataType dataType, long confirmedMaxDate,
                                               @Nullable List<Map<String, String>> appendedRows) {
        if (dataType != DataType.SMS) {
            return setMaxSyncedDate(dataType, confirmedMaxDate);
        }
        synchronized (SMS_RECORD_LOCK) {
            final long stored = sharedPreferences.getLong(dataType.maxSyncedPreference, MAX_SYNCED_DATE);
            final long watermark = Math.max(stored, confirmedMaxDate);
            final Set<String> record = getSmsBackupRecord();
            if (appendedRows != null) {
                for (Map<String, String> row : appendedRows) {
                    final String entry = SmsBackupWindow.entryOf(row);
                    if (entry != null) record.add(entry);
                }
            }
            SmsBackupWindow.prune(record, watermark);
            final SharedPreferences.Editor editor = sharedPreferences.edit()
                .putLong(dataType.maxSyncedPreference, watermark);
            putSmsRecord(editor, record);
            if (stored <= 0 || hasSmsBackupRecordMarker()) {
                editor.putInt(SmsBackupWindow.MARKER_KEY, SmsBackupWindow.RECORD_VERSION);
            }
            return editor.commit();
        }
    }

    /**
     * AC-21: records an SMS row that a backup examined and did not upload (the converter returned
     * nothing for it), so the look-back does not select it again. The watermark is not written.
     */
    public boolean recordExaminedSms(@NonNull Cursor row) {
        final String entry = SmsBackupWindow.entryOf(row);
        if (entry == null) return false;
        synchronized (SMS_RECORD_LOCK) {
            final Set<String> record = getSmsBackupRecord();
            record.add(entry);
            SmsBackupWindow.prune(record, storedSmsWatermark());
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            putSmsRecord(editor, record);
            return editor.commit();
        }
    }

    /**
     * Upgrade (REQ-MODERNIZATION-020 AC-18): the entries of the SMS visible now within W below
     * the stored watermark join the record (with any a restore wrote) and the marker is written,
     * in one commit. The watermark is not written.
     */
    public boolean seedSmsBackupRecord(@NonNull Collection<String> entries) {
        synchronized (SMS_RECORD_LOCK) {
            final Set<String> record = getSmsBackupRecord();
            record.addAll(entries);
            SmsBackupWindow.prune(record, storedSmsWatermark());
            final SharedPreferences.Editor editor = sharedPreferences.edit()
                .putInt(SmsBackupWindow.MARKER_KEY, SmsBackupWindow.RECORD_VERSION);
            putSmsRecord(editor, record);
            return editor.commit();
        }
    }

    /**
     * Skip (REQ-MODERNIZATION-020 AC-13): the SMS watermark is set outright, as Skip always did,
     * and the entries of the SMS visible now within W below it join the record, with the marker,
     * in one commit.
     */
    public boolean setSmsWatermarkAndSeed(long watermark, @NonNull Collection<String> entries) {
        synchronized (SMS_RECORD_LOCK) {
            final Set<String> record = getSmsBackupRecord();
            record.addAll(entries);
            SmsBackupWindow.prune(record, watermark);
            final SharedPreferences.Editor editor = sharedPreferences.edit()
                .putLong(DataType.SMS.maxSyncedPreference, watermark)
                .putInt(SmsBackupWindow.MARKER_KEY, SmsBackupWindow.RECORD_VERSION);
            putSmsRecord(editor, record);
            return editor.commit();
        }
    }

    /**
     * Restore (REQ-MODERNIZATION-020 AC-16): the write-ahead entry of an SMS about to be
     * inserted, made before the insert so that no interruption can leave a restored row
     * unrecorded. Pruned against the stored watermark, so an entry the current window cannot
     * reach is not kept. Writes neither the watermark (the restore raises it after the insert,
     * as before) nor the marker.
     */
    public boolean recordRestoredSms(@NonNull ContentValues values) {
        final String entry = SmsBackupWindow.entryOf(values);
        if (entry == null) return false;
        synchronized (SMS_RECORD_LOCK) {
            final long stored = storedSmsWatermark();
            final Set<String> record = getSmsBackupRecord();
            record.add(entry);
            SmsBackupWindow.prune(record, stored);
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            putSmsRecord(editor, record);
            return editor.commit();
        }
    }

    private long storedSmsWatermark() {
        return sharedPreferences.getLong(DataType.SMS.maxSyncedPreference, MAX_SYNCED_DATE);
    }

    private static void putSmsRecord(SharedPreferences.Editor editor, Set<String> record) {
        final String serialised = SmsBackupWindow.serialise(record);
        if (serialised == null) {
            editor.remove(SmsBackupWindow.RECORD_KEY);
        } else {
            editor.putString(SmsBackupWindow.RECORD_KEY, serialised);
        }
    }
```

**P2c.** `old_string` (HEAD `:79-85`):
```
    public void clearLastSyncData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (DataType type : DataType.values()) {
            editor.remove(type.maxSyncedPreference);
        }
        editor.commit();
    }
```
`new_string`:
```java
    public void clearLastSyncData() {
        synchronized (SMS_RECORD_LOCK) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            for (DataType type : DataType.values()) {
                editor.remove(type.maxSyncedPreference);
            }
            // U-070: Reset and account removal also forget the SMS look-back record and its
            // marker (REQ-MODERNIZATION-020 AC-15), so nothing from earlier backups suppresses an SMS.
            editor.remove(SmsBackupWindow.RECORD_KEY);
            editor.remove(SmsBackupWindow.MARKER_KEY);
            editor.commit();
        }
    }
```
`getMaxSyncedDate`, `setMaxSyncedDate` and `getMostRecentSyncedDate` keep their text (`git diff` shows them as context
only).

### 3.3 P3 — `BackupQueryBuilder.java` (two Edits; old lines 103-147 untouched)

**P3a.** `old_string` `import com.zegoggles.smssync.preferences.DataTypePreferences;` → `new_string`: that line, then
`import com.zegoggles.smssync.preferences.SmsBackupWindow;`.

**P3b.** `old_string` (HEAD `:162-164`, the end of the class):
```
            TextUtils.join(",", ids.toArray(new Long[ids.size()])));
    }
}
```
`new_string`:
```java
            TextUtils.join(",", ids.toArray(new Long[ids.size()])));
    }

    // -------------------------------------------------------------------------
    // U-070 (REQ-MODERNIZATION-020, BUG-021): the SMS look-back below the watermark.
    // The forward SMS query (getQueryForSMS) and the MMS and call-log queries are unchanged.
    // -------------------------------------------------------------------------

    private static final String[] SMS_RECORD_PROJECTION = {
        Telephony.TextBasedSmsColumns.DATE,
        Telephony.TextBasedSmsColumns.ADDRESS,
        Telephony.TextBasedSmsColumns.BODY
    };

    /** @return the preferences this builder reads; the fetch layer reaches the SMS record through them */
    @Nullable DataTypePreferences getDataTypePreferences() {
        return preferences;
    }

    /**
     * The window query: SMS with watermark - W < date <= watermark, with the forward query's
     * draft exclusion and contact-group clause, sorted by date and without LIMIT (the fetch
     * layer drops rows already in the record before the per-run limit applies, so recorded rows
     * cannot fill a LIMIT and starve a late row). The caller sends it only above watermark 0.
     */
    Query buildSmsWindowQuery(@Nullable ContactGroupIds groupIds, long watermark) {
        return new Query(Consts.SMS_PROVIDER,
            null,
            String.format(Locale.ENGLISH,
                "%s > ? AND %s <= ? AND %s <> ? %s",
                    Telephony.TextBasedSmsColumns.DATE,
                    Telephony.TextBasedSmsColumns.DATE,
                    Telephony.TextBasedSmsColumns.TYPE,
                    groupSelection(SMS, groupIds)).trim(),
            windowArgs(watermark),
            Telephony.TextBasedSmsColumns.DATE);
    }

    /**
     * The seeding query (Skip and upgrade): every visible non-draft SMS within W below the
     * watermark, without the contact-group clause (Skip's newest-date query has none), reading
     * only the columns of the record's key.
     */
    Query buildSmsSeedQuery(long watermark) {
        return new Query(Consts.SMS_PROVIDER,
            SMS_RECORD_PROJECTION,
            String.format(Locale.ENGLISH, "%s > ? AND %s <= ? AND %s <> ?",
                    Telephony.TextBasedSmsColumns.DATE,
                    Telephony.TextBasedSmsColumns.DATE,
                    Telephony.TextBasedSmsColumns.TYPE),
            windowArgs(watermark),
            Telephony.TextBasedSmsColumns.DATE);
    }

    private static String[] windowArgs(long watermark) {
        return new String[] {
            String.valueOf(watermark - SmsBackupWindow.LOOK_BACK_MS),
            String.valueOf(watermark),
            String.valueOf(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT)
        };
    }
}
```
I3 check: `git diff -U0 HEAD -- $BQB | grep '^@@'` prints exactly two hunks, with old ranges `-13,0` and `-163,0`.

### 3.4 New file P4 — `app/src/main/java/com/zegoggles/smssync/service/LateVisibleSmsCursor.java`

```java
package com.zegoggles.smssync.service;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.MergeCursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * U-070 (REQ-MODERNIZATION-020): the SMS rows of one backup run as one cursor: the accepted
 * rows of the look-back (window) query first, then the accepted rows of the forward query,
 * each group in the provider's ascending date order. Only positions are mapped; every value
 * is read from the provider's own cursor (through a MergeCursor), so a row converts exactly as
 * it does when it comes from the forward query alone. AbstractCursor, not CursorWrapper:
 * Robolectric replaces CursorWrapper with a shadow, so a CursorWrapper subclass would be tested
 * against a different implementation than the one that runs on the phone.
 */
final class LateVisibleSmsCursor extends AbstractCursor {
    private final Cursor rows;
    private final int[] positions;

    private LateVisibleSmsCursor(@NonNull Cursor rows, @NonNull int[] positions) {
        this.rows = rows;
        this.positions = positions;
    }

    static @NonNull Cursor of(@NonNull Cursor window, @NonNull List<Integer> windowRows,
                              @Nullable Cursor forward, @NonNull List<Integer> forwardRows) {
        final Cursor tail = forward != null ? forward : BackupItemsFetcher.emptyCursor();
        final int offset = window.getCount();
        final int[] positions = new int[windowRows.size() + forwardRows.size()];
        int i = 0;
        for (int row : windowRows) positions[i++] = row;
        for (int row : forwardRows) positions[i++] = offset + row;
        return new LateVisibleSmsCursor(new MergeCursor(new Cursor[]{window, tail}), positions);
    }

    @Override public int getCount() { return positions.length; }

    @Override public boolean onMove(int oldPosition, int newPosition) {
        return rows.moveToPosition(positions[newPosition]);
    }

    @Override public String[] getColumnNames() { return rows.getColumnNames(); }
    @Override public String getString(int column) { return rows.getString(column); }
    @Override public short getShort(int column) { return rows.getShort(column); }
    @Override public int getInt(int column) { return rows.getInt(column); }
    @Override public long getLong(int column) { return rows.getLong(column); }
    @Override public float getFloat(int column) { return rows.getFloat(column); }
    @Override public double getDouble(int column) { return rows.getDouble(column); }
    @Override public boolean isNull(int column) { return rows.isNull(column); }
    @Override public int getType(int column) { return rows.getType(column); }
    @Override public byte[] getBlob(int column) { return rows.getBlob(column); }

    @Override public void close() {
        super.close();
        rows.close();
    }
}
```

### 3.5 P5 — `BackupItemsFetcher.java` (two Edits)

**P5a.** `old_string`:
```
import com.zegoggles.smssync.mail.DataType;
import javax.inject.Inject;
```
`new_string`:
```
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.preferences.DataTypePreferences;
import com.zegoggles.smssync.preferences.SmsBackupWindow;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
```

**P5b.** `old_string` (HEAD `:37-40`):
```
    public @NonNull Cursor getItemsForDataType(DataType dataType, ContactGroupIds group, int max) {
        if (LOCAL_LOGV) Log.v(TAG, "getItemsForDataType(type=" + dataType + ", max=" + max + ")");
        return performQuery(queryBuilder.buildQueryForDataType(dataType, group, max));
    }
```
`new_string`:
```java
    public @NonNull Cursor getItemsForDataType(DataType dataType, ContactGroupIds group, int max) {
        if (LOCAL_LOGV) Log.v(TAG, "getItemsForDataType(type=" + dataType + ", max=" + max + ")");
        if (dataType == DataType.SMS) {
            // U-070: null when the builder is a test mock (BackupItemsFetcherTest): no look-back.
            final DataTypePreferences preferences = queryBuilder.getDataTypePreferences();
            if (preferences != null) {
                final long watermark = preferences.getMaxSyncedDate(DataType.SMS);
                if (watermark > 0) {
                    return getSmsItemsWithLookBack(preferences, watermark, group, max);
                }
            }
        }
        return performQuery(queryBuilder.buildQueryForDataType(dataType, group, max));
    }

    /**
     * U-070 (REQ-MODERNIZATION-020, BUG-021): the SMS rows of a run once a watermark exists.
     * Rows within W below the watermark that the record does not hold (late-visible SMS) come
     * first, in date order, and count against the per-run limit; rows already in the record are
     * not counted. The forward query (unchanged text) takes the remaining budget; its LIMIT is
     * widened only by the record entries above the watermark (normally none), whose rows are
     * dropped. The record is read a second time after both queries have returned, and the rows
     * are filtered against that read: a restore writes a row's entry before it inserts the row,
     * so a row a concurrent restore inserted is never mistaken for a late one (plan 2.4).
     * With nothing late and nothing dropped, the forward cursor is returned as it is.
     */
    private @NonNull Cursor getSmsItemsWithLookBack(@NonNull DataTypePreferences preferences, long watermark,
                                                    @Nullable ContactGroupIds group, int max) {
        if (!preferences.hasSmsBackupRecordMarker()) {
            // Upgrade (REQ-MODERNIZATION-020 AC-18): a watermark from the version before U-070 and no
            // record. Every SMS visible now within W below the watermark is treated as backed up
            // (today's outcome for it); an SMS that becomes visible later is backed up.
            final Set<String> seed = queryEntries(queryBuilder.buildSmsSeedQuery(watermark));
            preferences.seedSmsBackupRecord(seed);
            Log.i(TAG, String.format(Locale.ENGLISH,
                    "BackupItemsFetcher: look-back seeded %d SMS already on the phone (upgrade)", seed.size()));
        }
        // First read: only to size the forward query's LIMIT.
        final Set<String> before = preferences.getSmsBackupRecord();

        final Cursor window = performQuery(queryBuilder.buildSmsWindowQuery(group, watermark));
        Cursor forward = null;
        final int remaining;
        try {
            int lateBefore = 0;
            for (int i = 0; window.moveToPosition(i); i++) {
                if (!isRecorded(before, SmsBackupWindow.entryOf(window))) lateBefore++;
            }
            // Late rows count against the per-run limit; rows already in the record do not.
            remaining = max > 0 ? max - Math.min(lateBefore, max) : max;
            if (max <= 0 || remaining > 0) {
                forward = performQuery(queryBuilder.buildQueryForDataType(DataType.SMS, group,
                        max > 0 ? remaining + SmsBackupWindow.countAbove(before, watermark) : max));
            }
        } catch (RuntimeException e) {
            window.close();
            throw e;
        }

        // Second read, after both queries have returned (REQ-MODERNIZATION-020 AC-16 with a restore
        // running at the same time): every restored row these queries returned is in this read.
        final Set<String> record = preferences.getSmsBackupRecord();
        final List<Integer> late = new ArrayList<Integer>();
        int alreadyBackedUp = 0;
        for (int i = 0; window.moveToPosition(i); i++) {
            if (isRecorded(record, SmsBackupWindow.entryOf(window))) {
                alreadyBackedUp++;
            } else if (max <= 0 || late.size() < max) {
                late.add(i);
            }
        }
        if (!late.isEmpty()) {
            Log.i(TAG, String.format(Locale.ENGLISH,
                    "BackupItemsFetcher: look-back selected %d late SMS, skipped %d already backed up",
                    late.size(), alreadyBackedUp));
        }

        final List<Integer> accepted = new ArrayList<Integer>();
        if (forward != null) {
            final int entriesAbove = SmsBackupWindow.countAbove(record, watermark);
            for (int i = 0; forward.moveToPosition(i); i++) {
                if (entriesAbove > 0 && isRecorded(record, SmsBackupWindow.entryOf(forward))) continue;
                if (max > 0 && accepted.size() >= remaining) break;
                accepted.add(i);
            }
        }

        if (late.isEmpty() && forward != null && accepted.size() == forward.getCount()) {
            window.close();
            forward.moveToPosition(-1);
            return forward;
        }
        return LateVisibleSmsCursor.of(window, late, forward, accepted);
    }

    /**
     * U-070 (REQ-MODERNIZATION-020 AC-13, AC-14): Skip for SMS. The newest visible non-draft SMS
     * date becomes the SMS watermark, set outright as before, and every SMS visible now within W
     * below it is recorded as backed up, in one commit with the marker. An SMS the provider does
     * not return now (hidden from the app) is not recorded, so it is backed up when it becomes
     * visible within W. Connects to no server.
     *
     * @return the number of SMS recorded
     * @throws SecurityException if app does not hold necessary permissions
     */
    public int skipSms(@NonNull DataTypePreferences preferences) {
        final long newest = getMostRecentTimestamp(DataType.SMS);
        final Set<String> seed = newest > 0
                ? queryEntries(queryBuilder.buildSmsSeedQuery(newest))
                : new HashSet<String>();
        preferences.setSmsWatermarkAndSeed(newest, seed);
        Log.i(TAG, String.format(Locale.ENGLISH,
                "BackupItemsFetcher: look-back seeded %d SMS already on the phone (skip)", seed.size()));
        return seed.size();
    }

    private @NonNull Set<String> queryEntries(@Nullable BackupQueryBuilder.Query query) {
        final Set<String> entries = new HashSet<String>();
        final Cursor cursor = performQuery(query);
        try {
            while (cursor.moveToNext()) {
                final String entry = SmsBackupWindow.entryOf(cursor);
                if (entry != null) entries.add(entry);
            }
        } finally {
            cursor.close();
        }
        return entries;
    }

    private static boolean isRecorded(@NonNull Set<String> record, @Nullable String entry) {
        return entry != null && record.contains(entry);
    }
```

### 3.6 P6 — `BackupWorker.kt` (three Edits; text identical at HEAD and after U-067)

**P6a (Skip).** `old_string`:
```
            try {
                preferences.dataTypePreferences.setMaxSyncedDate(
                    type,
                    fetcher.getMostRecentTimestamp(type)
                )
            } catch (e: SecurityException) {
```
`new_string`:
```kotlin
            try {
                if (type == DataType.SMS) {
                    // U-070 (REQ-MODERNIZATION-020 AC-13, AC-14): the SMS watermark is set as before
                    // and every SMS visible now within W below it is recorded as backed up, in one
                    // commit; an SMS hidden from the app now is backed up when Android shows it.
                    fetcher.skipSms(preferences.dataTypePreferences)
                } else {
                    preferences.dataTypePreferences.setMaxSyncedDate(
                        type,
                        fetcher.getMostRecentTimestamp(type)
                    )
                }
            } catch (e: SecurityException) {
```

**P6b (examined row).** `old_string`:
```
                    Log.w(TAG, "BackupWorker: no messages converted")
                    currentItemsToSync -= 1
```
`new_string`:
```kotlin
                    Log.w(TAG, "BackupWorker: no messages converted")
                    // U-070 (AC-21): an SMS row that converts to nothing is recorded as examined
                    // (watermark unchanged), so the look-back does not select it again every run.
                    if (cursor.type == DataType.SMS) {
                        preferences.dataTypePreferences.recordExaminedSms(cursor.cursor)
                    }
                    currentItemsToSync -= 1
```

**P6c (the seam; signature unchanged).** `old_string`:
```
        // result field). Per-batch write for durable checkpoint behavior (U-016).
        dataTypePreferences.setMaxSyncedDate(type, confirmedMaxDate)
```
`new_string`:
```kotlin
        // result field). Per-batch write for durable checkpoint behavior (U-016).
        // U-070 (REQ-MODERNIZATION-020 AC-10, AC-12): for SMS the watermark becomes
        // max(stored, confirmedMaxDate) and the appended row joins the look-back record in the
        // same commit; MMS and call log are written exactly as before.
        dataTypePreferences.setMaxSyncedDateAfterAppend(type, confirmedMaxDate, result.mapList)
```

### 3.7 P7 — `RestoreWorker.kt` (one Edit)

`old_string`: `            val uri = ctx.contentResolver.insert(Consts.SMS_PROVIDER, values)`
`new_string`:
```kotlin
            // U-070 (REQ-MODERNIZATION-020 AC-16): write-ahead record of the row before the insert,
            // so no backup uploads a restored SMS again, even when an interruption leaves it above
            // the watermark. The watermark raise below is unchanged.
            preferences.dataTypePreferences.recordRestoredSms(values)
            val uri = ctx.contentResolver.insert(Consts.SMS_PROVIDER, values)
```
`smsExists`, the raise at `:464-468` and the checkpoint stay as they are.

### 3.8 P8 — `MailTransport.java` Javadoc (comment only, same line count; ruling SC-3)

`old_string` (HEAD `:74-77`):
```
     * Callers MUST use this return value — not any separately-computed date — as the watermark
     * to advance for this batch. If the append fails, this method throws and never returns a
     * date, so the caller's watermark is never updated. This makes the
     * "watermark only advances on confirmed append" invariant compiler-enforced.
```
`new_string` (4 lines):
```
     * Callers MUST use this return value — not any separately-computed date — as the date to
     * which the watermark may advance for this batch; the SMS watermark never moves down (U-070).
     * If the append fails, this method throws and never returns a date, so the caller's watermark
     * is never updated ("watermark only advances on confirmed append", compiler-enforced).
```
Proof (I6, I9):
- `git diff -U0` shows one hunk `@@ -74,4 +74,4 @@`, and every changed line starts with `     * `.
- The SHA-256 of `MailTransport.class` under `app/build/intermediates/javac/{debug,release}/…/mail/transport/` from the
  I0 gate equals that from the I9 gate (L20).
- Otherwise: row D13. Restore the file from HEAD by the filtered form, record "Javadoc left knowingly out of date", and
  report. No code line of the port is ever touched.

### 3.9 P9 — `README.md` (two Edits; working tree CRLF)

**P9a (Skip paragraph, `:82-87`).** `old_string`:
```
uninstalled SMS Backup and this initial backup won't ever be backed up to
Gmail.
```
`new_string`:
```
uninstalled SMS Backup and this initial backup won't ever be backed up to
Gmail. A message that Android is still hiding from SMS Backup+ at that moment
(for example a message containing a one-time code, which Android 17 holds back
for three hours) is not marked "backed up": it is backed up when Android shows
it, provided that happens within 6 hours of the message's date.
```

**P9b (after the Reset answer, `:274-275`).** `old_string`:
`sync state. All messages on the phone will be backed up on the next run.`
`new_string`:
```
sync state. All messages on the phone will be backed up on the next run.

SMS Backup+ backs up a message that Android shows it late, for example a
message containing a one-time code that Android 17 holds back for three hours,
provided Android shows it within 6 hours of the message's date. A message shown
later than that may not be backed up. A Reset backs up every message on the
phone again.
```
The Reset answer's meaning is unchanged, and the new paragraph repeats it. The "Package file was not signed" answer
(`:243-247`, Skip after a reinstall) stays true and is not edited: Skip still marks everything visible as backed up.

## 4. Tests (full text)

Constraint C10 applies to all of it: no test text contains the string "@Test" other than the annotations. No `@Ignore`, no
`Assume`. Package `com.zegoggles.smssync.service` unless stated. Robolectric `@Config(sdk = [29])`, as the neighbouring
classes use.

### 4.1 T1 — new `app/src/test/java/com/zegoggles/smssync/service/LateVisibleSmsFixtures.kt`

It uses only APIs that exist on the start commit, so it compiles there (I1).

```kotlin
package com.zegoggles.smssync.service

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.internet.TextBody
import com.zegoggles.smssync.contacts.ContactAccessor
import com.zegoggles.smssync.di.MailTransportFactory
import com.zegoggles.smssync.mail.ConversionResult
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.mail.MessageConverter
import com.zegoggles.smssync.mail.transport.BackupFolderHandle
import com.zegoggles.smssync.mail.transport.FetchSpec
import com.zegoggles.smssync.mail.transport.MailMessageHandle
import com.zegoggles.smssync.mail.transport.MailTransport
import com.zegoggles.smssync.mail.transport.MailTransportTestFactories
import com.zegoggles.smssync.mail.transport.MessageImportResult
import com.zegoggles.smssync.preferences.AuthPreferences
import com.zegoggles.smssync.preferences.DataTypePreferences
import com.zegoggles.smssync.preferences.Preferences
import kotlinx.coroutines.runBlocking
import org.robolectric.shadows.ShadowContentResolver
import java.util.Date
import java.util.concurrent.Executors

/** U-070: values the late-visible SMS tests share. */
internal object SmsFixture {
    const val INBOX = 1
    const val SENT = 2
    const val DRAFT = 3
    const val OUTBOX = 4
    const val HOUR = 3_600_000L
    /** W as REQ-MODERNIZATION-020 states it (6 hours); production reads SmsBackupWindow.LOOK_BACK_MS. */
    const val W_MS = 6 * HOUR
    /** The watermark most scenarios establish (epoch ms, 2023-11-14). */
    const val D = 1_700_000_000_000L
    /** IMAP user name; MessageConverter needs one (Address of null throws). Not an e-mail address. */
    const val OWNER = "backup-owner"
}

/**
 * U-070: a process death simulated inside a test. It propagates out of the restore loop
 * (executeRestoreWithValues catches only IllegalStateException). In the backup path doWork's
 * catch (e: Exception) turns it into Result.failure, after the same persisted state a real
 * death leaves (the throw comes before the commit).
 */
internal class SimulatedProcessDeath(message: String) : RuntimeException(message)

/** U-070: one row of a provider double. */
internal class ProviderRow(
    val id: Long,
    val date: Long,
    val address: String?,
    val body: String?,
    var type: Int,
    val person: Long?
) {
    var hidden = false
}

/** U-070: one query a provider double received, with the ids and dates it returned. */
internal class ProviderQuery(
    val uri: Uri,
    val projection: List<String>?,
    val selection: String?,
    val args: List<String>,
    val sortOrder: String?,
    val returnedIds: List<Long>,
    val returnedDates: List<Long>
) {
    /** The selection with each "?" replaced by its argument, e.g. "date > 2000 AND type <> 3". */
    val bound: String
        get() {
            var i = 0
            return (selection ?: "").replace(Regex("\\?")) { args.getOrElse(i++) { "?" } }
        }

    override fun toString() = "$uri [$bound] sort=$sortOrder returned=$returnedIds"
}

/**
 * U-070: a test-local provider for the SMS, MMS or call-log authority (a Fake Object). It
 * evaluates exactly the selection texts the app sends and throws AssertionError (not caught by
 * the workers' catch of Exception) on any other selection or sort order, records every query
 * with the rows it returned, and can add a row, change a row's type, hide a row until it is
 * released (Android 17's withholding as the app sees it) and store a row then throw from insert.
 */
internal class ProviderDouble(private val kind: DataType) : ContentProvider() {
    val rows = mutableListOf<ProviderRow>()
    val queries = mutableListOf<ProviderQuery>()
    /** When true, the next insert stores the row and then throws [SimulatedProcessDeath]. */
    var dieAfterNextInsert = false
    /**
     * Iteration 2 (B26): called with the selection text at the start of every query, before any
     * row is selected; a test uses it to run another writer between the app's reads. Null by
     * default, so the double behaves exactly as before.
     */
    var beforeQuery: ((String) -> Unit)? = null
    private var nextId = 1L

    fun add(date: Long, address: String?, body: String?, type: Int = SmsFixture.INBOX,
            person: Long? = null, id: Long? = null): Long {
        val rowId = id ?: nextId
        nextId = maxOf(nextId, rowId + 1)
        rows.add(ProviderRow(rowId, date, address, body, type, person))
        return rowId
    }

    fun row(id: Long): ProviderRow = rows.single { it.id == id }
    fun setType(id: Long, type: Int) { row(id).type = type }
    fun hide(id: Long) { row(id).hidden = true }
    fun release(id: Long) { row(id).hidden = false }
    fun clearQueries() = queries.clear()

    override fun onCreate() = true
    override fun getType(uri: Uri): String? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (kind != DataType.SMS) throw AssertionError("ProviderDouble($kind): insert is served for SMS only")
        val v = values ?: return null
        val id = add(
            date = v.getAsString("date").toLong(),
            address = v.getAsString("address"),
            body = v.getAsString("body"),
            type = v.getAsInteger("type") ?: SmsFixture.INBOX
        )
        if (dieAfterNextInsert) {
            dieAfterNextInsert = false
            throw SimulatedProcessDeath("process died after the insert of row $id")
        }
        return Uri.parse("content://sms/$id")
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        beforeQuery?.invoke(selection.orEmpty())
        val args = selectionArgs?.toList() ?: emptyList()
        val match = matcher(selection, args)
        var selected = rows.filter { !it.hidden && match(it) }
        val sort = SORT.matchEntire(sortOrder ?: "date")
            ?: throw AssertionError("ProviderDouble($kind): unrecognised sort order [$sortOrder]")
        selected = if (sort.groupValues[1].isNotEmpty()) selected.sortedByDescending { it.date }
                   else selected.sortedBy { it.date }
        if (sort.groupValues[2].isNotEmpty()) selected = selected.take(sort.groupValues[2].toInt())
        val columns = projection?.toList() ?: allColumns()
        val cursor = MatrixCursor(columns.toTypedArray())
        selected.forEach { r -> cursor.addRow(columns.map { value(r, it) }.toTypedArray()) }
        queries.add(ProviderQuery(uri, projection?.toList(), selection, args, sortOrder,
            selected.map { it.id }, selected.map { it.date }))
        return cursor
    }

    private fun matcher(selection: String?, a: List<String>): (ProviderRow) -> Boolean {
        if (selection == null) {
            if (kind == DataType.SMS) throw AssertionError("ProviderDouble(SMS): query without selection")
            return { true }
        }
        FORWARD.matchEntire(selection)?.let { m ->
            val typeColumn = if (kind == DataType.MMS) "m_type" else "type"
            if (kind == DataType.CALLLOG || m.groupValues[1] != typeColumn) unknown(selection)
            val inGroup = groupFilter(m.groupValues[2], m.groupValues[3])
            return { it.date > a[0].toLong() && it.type != a[1].toInt() && inGroup(it) }
        }
        WINDOW.matchEntire(selection)?.let { m ->
            if (kind != DataType.SMS) unknown(selection)
            val inclusive = m.groupValues[1] == ">="
            val inGroup = groupFilter(m.groupValues[2], m.groupValues[3])
            return {
                (if (inclusive) it.date >= a[0].toLong() else it.date > a[0].toLong()) &&
                    it.date <= a[1].toLong() && it.type != a[2].toInt() && inGroup(it)
            }
        }
        when (selection) {
            "type <> ?" -> if (kind == DataType.SMS) return { it.type != a[0].toInt() }
            "date > ?" -> if (kind == DataType.CALLLOG) return { it.date > a[0].toLong() }
            "date = ? AND address = ? AND type = ?" -> if (kind == DataType.SMS) return {
                it.date.toString() == a[0] && it.address == a[1] && it.type.toString() == a[2]
            }
        }
        unknown(selection)
    }

    private fun unknown(selection: String): Nothing =
        throw AssertionError("ProviderDouble($kind): unrecognised selection [$selection]")

    private fun groupFilter(clause: String, ids: String): (ProviderRow) -> Boolean {
        if (clause.isEmpty()) return { true }
        val people = ids.split(",").filter { it.isNotEmpty() }.map { it.toLong() }.toSet()
        return { it.type == SmsFixture.SENT || (it.person != null && it.person in people) }
    }

    private fun allColumns(): List<String> = when (kind) {
        DataType.MMS -> listOf("_id", "thread_id", "date", "msg_box", "read", "m_type")
        DataType.CALLLOG -> listOf("_id", "number", "duration", "date", "type")
        else -> listOf("_id", "thread_id", "address", "person", "date", "read", "status", "type", "body")
    }

    private fun value(r: ProviderRow, column: String): Any? = when (column) {
        "_id" -> r.id
        "thread_id" -> 1L
        "address", "number" -> r.address
        "person" -> r.person
        "date" -> r.date
        "read" -> 1
        "status" -> -1
        "type", "m_type", "msg_box" -> r.type
        "body" -> r.body
        "duration" -> 0
        else -> throw AssertionError("ProviderDouble($kind): unknown column $column")
    }

    private companion object {
        const val GROUP = """( {2}AND \(type = 2 OR person IN \(([-0-9,]*)\)\))?"""
        val FORWARD = Regex("""^date > \? AND (type|m_type) <> \?$GROUP$""")
        val WINDOW = Regex("""^date (>|>=) \? AND date <= \? AND type <> \?$GROUP$""")
        val SORT = Regex("""^date( DESC)?(?: LIMIT ([0-9]+))?$""")
    }
}

/**
 * U-070: a Configurable Test Stub of the contacts provider: phone lookup by address, group
 * membership for one group query, and no e-mail addresses.
 */
internal class ContactsProviderDouble : ContentProvider() {
    /** address -> (contact id, raw contact id) */
    val contacts = mutableMapOf<String, Pair<Long, Long>>()
    /** group row id -> member addresses */
    val groups = mutableMapOf<Long, MutableList<String>>()

    override fun onCreate() = true
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw AssertionError("contacts insert")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val segments = uri.pathSegments
        return when {
            segments.size == 2 && segments[0] == "phone_lookup" ->
                MatrixCursor(arrayOf("_id", "display_name")).apply {
                    contacts[segments[1]]?.let { addRow(arrayOf<Any>(it.first, "Contact ${it.first}")) }
                }
            segments == listOf("data") && selection == "data1 = ? AND mimetype = ?" ->
                MatrixCursor(arrayOf("contact_id", "raw_contact_id", "data1")).apply {
                    val group = selectionArgs!![0].toLong()
                    groups[group].orEmpty().mapNotNull { contacts[it] }
                        .forEach { addRow(arrayOf<Any>(it.first, it.second, group)) }
                }
            segments == listOf("data", "emails") -> MatrixCursor(arrayOf("data1"))
            else -> throw AssertionError("ContactsProviderDouble: unexpected query $uri [$selection]")
        }
    }
}

/** U-070: one message the recording transport appended (at sign written as "(at)" in every text). */
internal class AppendedMessage(
    val folder: String?,
    val messageId: String?,
    val smsId: String?,
    val date: String?,
    val address: String?,
    val body: String?,
    /** Folder, every header except X-smssync-backup-time and X-smssync-version (sorted), SEEN, body. */
    val canonical: String
) {
    val triple: String get() = "$date|$address|$body"
}

/**
 * U-070: a Test Spy of the MailTransport port. Records every port call and, for every appended
 * message, its Message-ID, X-smssync-id, date, address and body; returns the result's max date
 * as the real adapter does. Can fail at each point before the server's confirmation, record and
 * then fail (a death after the confirmation), and serve SMS items to a restore.
 */
internal class RecordingTransport : MailTransport {
    val appended = mutableListOf<AppendedMessage>()
    val calls = mutableListOf<String>()
    var onCheckSettings: (() -> Unit)? = null
    var onOpenFolder: (() -> Unit)? = null
    var beforeAppend: (() -> Unit)? = null
    var afterAppendRecorded: (() -> Unit)? = null
    /** SMS items served to a restore: getMessages on the SMS folder returns one handle each. */
    var restoreItems: List<ContentValues> = emptyList()
    private var openedType: DataType? = null
    private var openedFolder: String? = null

    fun count(call: String) = calls.count { it == call }

    override fun checkSettings() {
        calls.add("checkSettings")
        onCheckSettings?.invoke()
    }

    override fun openFolder(type: DataType?, prefs: DataTypePreferences?): BackupFolderHandle {
        calls.add("openFolder")
        onOpenFolder?.invoke()
        openedType = type
        openedFolder = if (type != null) prefs?.getFolder(type) else null
        return MailTransportTestFactories.createFolderHandle()
    }

    override fun appendMessages(folder: BackupFolderHandle?, result: ConversionResult?): Long {
        calls.add("appendMessages")
        beforeAppend?.invoke()
        val r = result!!
        r.messages.forEach { appended.add(describe(it)) }
        afterAppendRecorded?.invoke()
        return r.maxDate
    }

    override fun getMessages(folder: BackupFolderHandle?, max: Int, flagged: Boolean,
                             since: Date?): MutableList<MailMessageHandle> {
        calls.add("getMessages")
        return if (openedType == DataType.SMS) {
            restoreItems.indices.map { MailTransportTestFactories.createHandle("restore-$it") }.toMutableList()
        } else mutableListOf()
    }

    override fun fetch(folder: BackupFolderHandle?, handles: MutableList<MailMessageHandle>?, profile: FetchSpec?) {
        calls.add("fetch")
    }

    override fun closeFolders() {
        calls.add("closeFolders")
    }

    override fun importMessageBody(folder: BackupFolderHandle?, handle: MailMessageHandle?,
                                   converter: MessageConverter?): MessageImportResult {
        calls.add("importMessageBody")
        val uid = handle?.uid ?: "unknown"
        return MessageImportResult(uid, DataType.SMS, ContentValues(restoreItems[uid.removePrefix("restore-").toInt()]))
    }

    private fun describe(m: Message): AppendedMessage {
        fun at(s: String?) = s?.replace("@", "(at)")
        fun h(name: String) = at(m.getHeader(name)?.firstOrNull())
        val headers = m.headerNames.orEmpty()
            .filter { it != "X-smssync-backup-time" && it != "X-smssync-version" }
            .sorted()
            .joinToString(";") { name -> name + "=" + m.getHeader(name).orEmpty().joinToString(",") }
        val body = (m.body as? TextBody)?.rawText
        return AppendedMessage(
            openedFolder, h("Message-ID"), h("X-smssync-id"), h("X-smssync-date"), h("X-smssync-address"), body,
            at("folder=$openedFolder|$headers|seen=${m.isSet(Flag.SEEN)}|body=$body")!!
        )
    }
}

/** U-070: a Decorator over SharedPreferences that records, for each commit(), the keys it wrote or removed. */
internal class CountingSharedPreferences(private val delegate: SharedPreferences) : SharedPreferences by delegate {
    val commits = mutableListOf<Set<String>>()
    var applies = 0
        private set

    override fun edit(): SharedPreferences.Editor = KeyRecordingEditor(delegate.edit())

    private inner class KeyRecordingEditor(private val editor: SharedPreferences.Editor) : SharedPreferences.Editor {
        private val keys = linkedSetOf<String>()
        private fun record(key: String?): SharedPreferences.Editor { keys.add(key ?: "<null>"); return this }
        override fun putString(key: String?, value: String?): SharedPreferences.Editor { editor.putString(key, value); return record(key) }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor { editor.putStringSet(key, values); return record(key) }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor { editor.putInt(key, value); return record(key) }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor { editor.putLong(key, value); return record(key) }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { editor.putFloat(key, value); return record(key) }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { editor.putBoolean(key, value); return record(key) }
        override fun remove(key: String?): SharedPreferences.Editor { editor.remove(key); return record(key) }
        override fun clear(): SharedPreferences.Editor { editor.clear(); return record("<clear>") }
        override fun commit(): Boolean { commits.add(keys.toSet()); return editor.commit() }
        override fun apply() { applies++; editor.apply() }
    }
}

/** U-070: registers [provider] for [authority] with Robolectric's content resolver. */
internal fun registerProvider(context: Context, authority: String, provider: ContentProvider) {
    provider.attachInfo(context, null)
    ShadowContentResolver.registerProviderInternal(authority, provider)
}

/**
 * U-070: the shared set-up: an SMS provider double, the test WorkManager, an IMAP user name,
 * and one backup through BackupWorker.doWork (the production entry) with a given transport.
 */
internal class LateSmsHarness(val context: Context) {
    val sms = ProviderDouble(DataType.SMS)
    val preferences = Preferences(context)
    val dataTypePreferences: DataTypePreferences get() = preferences.dataTypePreferences

    init {
        registerProvider(context, "sms", sms)
        AuthPreferences(context).setImapUser(SmsFixture.OWNER)
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .setWorkerFactory(BackupWorker.TestableBackupWorkerFactory())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    fun registerMms(): ProviderDouble = ProviderDouble(DataType.MMS).also { registerProvider(context, "mms", it) }
    fun registerCallLog(): ProviderDouble = ProviderDouble(DataType.CALLLOG).also { registerProvider(context, "call_log", it) }
    fun registerContacts(): ContactsProviderDouble =
        ContactsProviderDouble().also { registerProvider(context, "com.android.contacts", it) }

    fun setMaxItemsPerSync(n: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("max_items_per_sync", n.toString()).commit()
    }

    fun watermark(): Long = dataTypePreferences.getMaxSyncedDate(DataType.SMS)

    /**
     * One backup through doWork (tag MANUAL, or SKIP when [skip]). [sharedPreferences] replaces
     * the worker's preferences object; [engine] may replace its WorkerEngineFactory.
     */
    fun backup(transport: MailTransport, skip: Boolean = false, sharedPreferences: SharedPreferences? = null,
               engine: ((WorkerEngineFactory) -> WorkerEngineFactory)? = null): ListenableWorker.Result {
        val factory = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String,
                                      workerParameters: WorkerParameters): ListenableWorker? {
                if (workerClassName != BackupWorker::class.java.name) return null
                val prefs = if (sharedPreferences != null) Preferences(appContext, sharedPreferences) else Preferences(appContext)
                val authPrefs = AuthPreferences(appContext)
                val engineFactory = WorkerEngineFactory(appContext, prefs, authPrefs)
                return BackupWorker(appContext, workerParameters, prefs, authPrefs, MailTransportFactory { transport },
                    ContactAccessor(), engine?.invoke(engineFactory) ?: engineFactory)
            }
        }
        val worker = TestListenableWorkerBuilder<BackupWorker>(context)
            .setWorkerFactory(factory)
            .setTags(listOf(if (skip) BackupType.SKIP.name else BackupType.MANUAL.name))
            .build() as BackupWorker
        return runBlocking { worker.doWork() }
    }

    /** One backup with a fresh recording transport; returns the transport. */
    fun backupRecording(): RecordingTransport = RecordingTransport().also { backup(it) }
}
```

### 4.2 T2 — new `LateVisibleSmsRegressionTest.kt` (written first; red on the start commit)

```kotlin
package com.zegoggles.smssync.service

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.zegoggles.smssync.service.SmsFixture.INBOX
import com.zegoggles.smssync.service.SmsFixture.SENT
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.TimeZone

/**
 * U-070 (BUG-021 AC-9, REQ-MODERNIZATION-020 AC-1 and AC-24): the regression test and the
 * in-order sequence. Both use only what exists on the start commit, are run there first (the
 * AC-1 tests fail, the AC-6 sequence is captured), and then pass after the change.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LateVisibleSmsRegressionTest {

    private lateinit var h: LateSmsHarness

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        h = LateSmsHarness(RuntimeEnvironment.application)
    }

    private fun ids(t: RecordingTransport) = t.appended.map { it.smsId!!.toLong() }

    /**
     * AC-1: message B (date 2000, inbox) is uploaded and the SMS watermark is 2000; message A
     * (date 1000, inbox) is then added. The next backup uploads A exactly once, does not upload B
     * again and leaves the watermark at 2000; a further backup uploads nothing. On the start
     * commit the run's query record shows "date > 2000 AND type <> 3" and A is not uploaded.
     */
    @Test
    fun ac1_lateVisibleSms_isUploadedOnceByTheNextBackup() {
        val b = h.sms.add(2000L, "+15550100002", "message B")
        assertThat(ids(h.backupRecording())).containsExactly(b)
        assertThat(h.watermark()).isEqualTo(2000L)

        val a = h.sms.add(1000L, "+15550100001", "message A")
        h.sms.clearQueries()
        val second = h.backupRecording()
        assertWithMessage("SMS queries of the run: ${h.sms.queries}").that(ids(second)).containsExactly(a)
        assertThat(h.watermark()).isEqualTo(2000L)

        assertThat(h.backupRecording().appended).isEmpty()
    }

    /** AC-1: other backups ran in between before A became visible; A is still uploaded once. */
    @Test
    fun ac1_lateVisibleSms_afterBackupsInBetween_isUploadedOnce() {
        h.sms.add(1_000_000L, "+15550100002", "message B")
        h.backupRecording()
        h.backupRecording()
        val c = h.sms.add(1_100_000L, "+15550100003", "message C")
        assertThat(ids(h.backupRecording())).containsExactly(c)
        h.backupRecording()
        val a = h.sms.add(900_000L, "+15550100001", "message A")
        h.sms.clearQueries()
        assertWithMessage("SMS queries of the run: ${h.sms.queries}").that(ids(h.backupRecording())).containsExactly(a)
        assertThat(h.watermark()).isEqualTo(1_100_000L)
        assertThat(h.backupRecording().appended).isEmpty()
    }

    /**
     * AC-6 (REQ AC-6, AC-24): rows that never become visible below the watermark. Four runs
     * upload the same messages, in the same order, with the same headers (apart from
     * X-smssync-backup-time and X-smssync-version), to the same folder, with the same transport
     * calls and the same watermark after each run, as the start commit. EXPECTED_IN_ORDER is
     * the literal captured by running this body on the start commit (plan step I1).
     */
    @Test
    fun ac6_inOrderSequence_isTheStartCommitSequence() {
        val zone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            h.preferences.setReferenceUid("u070reference000000000000")
            val t0 = 1_700_000_000_000L
            val observed = mutableListOf<String>()
            h.sms.add(t0 + 1000, "+15550100001", "in-order one", type = INBOX)
            h.sms.add(t0 + 2000, "+15550100002", "in-order two", type = SENT)
            backupAndRecord(observed, 1)
            h.sms.add(t0 + 3000, "+15550100001", "in-order three", type = INBOX)
            backupAndRecord(observed, 2)
            h.sms.add(t0 + 4000, "+15550100002", "in-order four", type = SENT)
            h.sms.add(t0 + 5000, "+15550100001", "in-order five", type = INBOX)
            backupAndRecord(observed, 3)
            backupAndRecord(observed, 4)
            val literal = observed.joinToString("\n") {
                "            \"" + it.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\","
            }
            if (EXPECTED_IN_ORDER.isEmpty()) {
                val out = File(System.getProperty("user.dir"), "build/u070/ac6-in-order-capture.txt")
                out.parentFile!!.mkdirs()
                out.writeText(literal + "\n")
            }
            assertWithMessage("captured in-order sequence:\n$literal\n")
                .that(observed).containsExactlyElementsIn(EXPECTED_IN_ORDER).inOrder()
        } finally {
            TimeZone.setDefault(zone)
        }
    }

    private fun backupAndRecord(observed: MutableList<String>, run: Int) {
        val t = h.backupRecording()
        t.appended.forEach { observed.add("run=$run|${it.canonical}") }
        observed.add("run=$run|watermark=${h.watermark()}|calls=${t.calls}")
    }

    companion object {
        /** I1b: the lines of app/build/u070/ac6-in-order-capture.txt from the start commit, pasted unchanged. */
        private val EXPECTED_IN_ORDER: List<String> = listOf<String>(
        )
    }
}
```

### 4.3 T3 — new `LateVisibleSmsBackupTest.kt` (26 tests; written at I4; iteration 2 changed B3 and added B26)

```kotlin
package com.zegoggles.smssync.service

import android.content.ContentValues
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.testing.TestListenableWorkerBuilder
import com.fsck.k9.mail.store.imap.ImapMessage
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.zegoggles.smssync.auth.TokenRefresher
import com.zegoggles.smssync.mail.ConversionResult
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.mail.Headers
import com.zegoggles.smssync.mail.transport.MailException
import com.zegoggles.smssync.mail.transport.XOAuth2FailedException
import com.zegoggles.smssync.preferences.SmsBackupWindow
import com.zegoggles.smssync.service.SmsFixture.D
import com.zegoggles.smssync.service.SmsFixture.DRAFT
import com.zegoggles.smssync.service.SmsFixture.HOUR
import com.zegoggles.smssync.service.SmsFixture.OUTBOX
import com.zegoggles.smssync.service.SmsFixture.SENT
import com.zegoggles.smssync.service.SmsFixture.W_MS
import com.zegoggles.smssync.service.exception.RequiresLoginException
import kotlinx.coroutines.CancellationException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * U-070 (REQ-MODERNIZATION-020): the look-back below the SMS watermark and the local record,
 * driven through BackupWorker.doWork with a provider double and a recording transport; dates
 * are fixed epoch values and the selection uses the watermark, never the wall clock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LateVisibleSmsBackupTest {

    private lateinit var h: LateSmsHarness

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        h = LateSmsHarness(RuntimeEnvironment.application)
    }

    private fun ids(t: RecordingTransport) = t.appended.map { it.smsId!!.toLong() }

    /** Uploads one row at D, so the watermark is D, the record holds it and the marker exists. */
    private fun establishWatermarkAtD(): Long {
        val id = h.sms.add(D, "+15550100009", "watermark row")
        assertThat(ids(h.backupRecording())).containsExactly(id)
        assertThat(h.watermark()).isEqualTo(D)
        return id
    }

    private fun isWindowQuery(q: ProviderQuery) = q.selection!!.startsWith("date > ? AND date <= ?")

    // ---- AC-2: the per-run limit --------------------------------------------------------

    /** AC-2 (a): limit 2 and three late-visible SMS: the first backup uploads two (oldest first), the next the last. */
    @Test
    fun ac2a_limitN_nPlusOneLate_firstRunUploadsN_nextRunUploadsTheLast() {
        establishWatermarkAtD()
        val late = (1..3).map { h.sms.add(D - it * HOUR, "+1555010000$it", "late $it") }
        h.setMaxItemsPerSync(2)
        assertThat(ids(h.backupRecording())).containsExactly(late[2], late[1]).inOrder()
        assertThat(ids(h.backupRecording())).containsExactly(late[0])
        assertThat(h.backupRecording().appended).isEmpty()
        assertThat(h.watermark()).isEqualTo(D)
    }

    /** AC-2 (b): limit 5, two late SMS and one forward SMS: the MMS query of the same run gets LIMIT 2. */
    @Test
    fun ac2b_lateRowsCountAgainstTheLimit_mmsBudgetReducedByExactlyThatNumber() {
        val mms = h.registerMms()
        establishWatermarkAtD()
        h.sms.add(D - 2 * HOUR, "+15550100001", "late 1")
        h.sms.add(D - HOUR, "+15550100002", "late 2")
        h.sms.add(D + 1000, "+15550100003", "forward 1")
        h.setMaxItemsPerSync(5)
        mms.clearQueries()
        assertThat(h.backupRecording().appended).hasSize(3)
        assertThat(mms.queries.map { it.sortOrder }).containsExactly("date LIMIT 2")
    }

    /**
     * AC-2 (c) (REQ AC-22): limit 3; five SMS already backed up within W below the watermark, one
     * late-visible SMS and three forward SMS. The run uploads three items, the late one first;
     * the next run uploads the remaining forward SMS; the backed-up rows are not counted.
     */
    @Test
    fun ac2c_alreadyBackedUpWindowRows_areNotCountedAgainstTheLimit() {
        val backedUp = (4 downTo 1).map { h.sms.add(D - it * 1000L, "+1555010010$it", "backed up $it") } +
            h.sms.add(D, "+15550100009", "watermark row")
        assertThat(ids(h.backupRecording())).containsExactlyElementsIn(backedUp).inOrder()
        assertThat(h.watermark()).isEqualTo(D)
        val late = h.sms.add(D - 500, "+15550100201", "late")
        val forward = (1..3).map { h.sms.add(D + it * 1000L, "+1555010030$it", "forward $it") }
        h.setMaxItemsPerSync(3)
        assertThat(ids(h.backupRecording())).containsExactly(late, forward[0], forward[1]).inOrder()
        assertThat(ids(h.backupRecording())).containsExactly(forward[2])
        assertThat(h.backupRecording().appended).isEmpty()
    }

    // ---- AC-3: same selection rules -----------------------------------------------------

    /**
     * AC-3: with a contact group chosen, a late draft and a late message outside the group are not
     * uploaded; a late message inside the group is. The window query carries the forward query's
     * draft exclusion and group clause.
     */
    @Test
    fun ac3_lateDraftAndOutOfGroupNotUploaded_inGroupUploaded_windowCarriesTheSameClauses() {
        val contacts = h.registerContacts()
        contacts.contacts["+15550200001"] = 7L to 70L
        contacts.contacts["+15550200002"] = 8L to 71L
        contacts.groups[5L] = mutableListOf("+15550200001")
        PreferenceManager.getDefaultSharedPreferences(h.context).edit().putString("backup_contact_group", "5").commit()

        h.sms.add(D, "+15550200001", "in the group, uploaded first", person = 70L)
        assertThat(h.backupRecording().appended).hasSize(1)

        h.sms.add(D - 3 * HOUR, "+15550200001", "late draft", type = DRAFT, person = 70L)
        h.sms.add(D - 2 * HOUR, "+15550200002", "late, outside the group", person = 71L)
        val inGroup = h.sms.add(D - HOUR, "+15550200001", "late, inside the group", person = 70L)
        h.sms.clearQueries()
        assertThat(ids(h.backupRecording())).containsExactly(inGroup)

        val window = h.sms.queries.single { isWindowQuery(it) }
        assertThat(window.selection).isEqualTo("date > ? AND date <= ? AND type <> ?  AND (type = 2 OR person IN (70))")
        assertThat(window.args).containsExactly((D - W_MS).toString(), D.toString(), "3").inOrder()
        val forward = h.sms.queries.single { it.selection!!.startsWith("date > ? AND type <> ?") }
        assertThat(forward.selection).isEqualTo("date > ? AND type <> ?  AND (type = 2 OR person IN (70))")
    }

    // ---- AC-5: the boundary --------------------------------------------------------------

    /** AC-5: a message that becomes visible W minus 1 ms below the watermark is uploaded. */
    @Test
    fun ac5_lateRowAtWMinus1msBelowTheWatermark_isUploaded() {
        establishWatermarkAtD()
        val edge = h.sms.add(D - W_MS + 1, "+15550100001", "W minus 1 ms below")
        assertThat(ids(h.backupRecording())).containsExactly(edge)
    }

    /** AC-5: a message exactly W below the watermark is not returned by the provider (the bound is exclusive). */
    @Test
    fun ac5_rowExactlyWBelowTheWatermark_isNotReturnedByTheProvider() {
        establishWatermarkAtD()
        val atW = h.sms.add(D - W_MS, "+15550100001", "exactly W below")
        h.sms.clearQueries()
        h.backupRecording()
        assertThat(h.sms.queries.flatMap { it.returnedIds }).doesNotContain(atW)
    }

    // ---- AC-7, AC-8: no row stored twice -------------------------------------------------

    /** AC-7: four uninterrupted runs with a late row between runs: no row, identity or triple twice. */
    @Test
    fun ac7_runsWithALateRow_noRowIdentityOrTripleTwice() {
        val all = mutableListOf<AppendedMessage>()
        h.sms.add(D - HOUR, "+15550100001", "one")
        h.sms.add(D, "+15550100002", "two")
        all += h.backupRecording().appended
        h.sms.add(D - 30 * 60_000L, "+15550100003", "late three")
        all += h.backupRecording().appended
        h.sms.add(D + 1000, "+15550100004", "four")
        all += h.backupRecording().appended
        all += h.backupRecording().appended
        assertThat(all).hasSize(4)
        assertThat(all.map { it.smsId }).containsNoDuplicates()
        assertThat(all.map { it.messageId }).containsNoDuplicates()
        assertThat(all.map { it.triple }).containsNoDuplicates()
    }

    /** AC-8: a row uploaded as outbox whose type then becomes sent (same date, within W) is not appended again. */
    @Test
    fun ac8_rowWhoseTypeChangesFromOutboxToSent_isNotAppendedAgain() {
        val outbox = h.sms.add(D - HOUR, "+15550100001", "queued", type = OUTBOX)
        h.sms.add(D, "+15550100002", "watermark row")
        assertThat(h.backupRecording().appended).hasSize(2)
        h.sms.setType(outbox, SENT)
        assertThat(h.backupRecording().appended).isEmpty()
    }

    // ---- AC-9: no state change without a confirmed append ---------------------------------

    /** The interruption configured on the transport happens before the server confirms the first append of the run. */
    private fun assertInterruptedBeforeConfirmation(configure: RecordingTransport.() -> Unit) {
        establishWatermarkAtD()
        val late = h.sms.add(D - HOUR, "+15550100001", "late")
        val forward = h.sms.add(D + 1000, "+15550100002", "forward")
        val recordBefore = h.dataTypePreferences.getSmsBackupRecord()
        h.backup(RecordingTransport().apply(configure))
        assertThat(h.watermark()).isEqualTo(D)
        assertThat(h.dataTypePreferences.getSmsBackupRecord()).isEqualTo(recordBefore)
        assertThat(ids(h.backupRecording())).containsExactly(late, forward).inOrder()
    }

    /** AC-9: the folder open throws. */
    @Test
    fun ac9_openFolderFails_nothingSaved_rowsUploadedByTheNextBackup() =
        assertInterruptedBeforeConfirmation { onOpenFolder = { throw MailException("folder error") } }

    /** AC-9: the append throws. */
    @Test
    fun ac9_appendFails_nothingSaved_rowsUploadedByTheNextBackup() =
        assertInterruptedBeforeConfirmation { beforeAppend = { throw MailException("append error") } }

    /** AC-9: the login fails. */
    @Test
    fun ac9_loginFails_nothingSaved_rowsUploadedByTheNextBackup() =
        assertInterruptedBeforeConfirmation { onCheckSettings = { throw RequiresLoginException() } }

    /** AC-9: the run is cancelled while the append is in flight, before the server confirms. */
    @Test
    fun ac9_cancelledDuringTheAppend_nothingSaved_rowsUploadedByTheNextBackup() =
        assertInterruptedBeforeConfirmation { beforeAppend = { throw CancellationException("cancelled before confirmation") } }

    // ---- AC-10: an interruption after the server's confirmation ----------------------------

    /**
     * AC-10: the transport records the append of "forward 1" and the process then dies before the
     * state is saved. The next backup appends that one message again (accepted), no other
     * message twice, and a further backup appends none. One commit per saved row carries the
     * watermark and the record together; no apply().
     */
    @Test
    fun ac10_deathAfterTheServerConfirmed_onlyThatMessageTwice_oneCommitPerRowCarriesBoth() {
        val counting = CountingSharedPreferences(PreferenceManager.getDefaultSharedPreferences(h.context))
        establishWatermarkAtD()
        val late = h.sms.add(D - HOUR, "+15550100001", "late")
        val f1 = h.sms.add(D + 1000, "+15550100002", "forward 1")
        val f2 = h.sms.add(D + 2000, "+15550100003", "forward 2")
        var appends = 0
        val dying = RecordingTransport().apply {
            afterAppendRecorded = { if (++appends == 2) throw SimulatedProcessDeath("died after the confirmed append") }
        }
        h.backup(dying, sharedPreferences = counting)
        assertThat(ids(dying)).containsExactly(late, f1).inOrder()
        val smsCommits = counting.commits.filter {
            DataType.SMS.maxSyncedPreference in it || SmsBackupWindow.RECORD_KEY in it
        }
        assertThat(smsCommits).hasSize(1)
        assertThat(smsCommits.single()).containsAtLeast(DataType.SMS.maxSyncedPreference, SmsBackupWindow.RECORD_KEY)
        assertThat(counting.applies).isEqualTo(0)

        val next = h.backupRecording()
        assertThat(ids(next)).containsExactly(f1, f2).inOrder()
        val further = h.backupRecording()
        assertThat(further.appended).isEmpty()
        val twice = (ids(dying) + ids(next)).groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertThat(twice).containsExactly(f1)
    }

    // ---- AC-11: the watermark never moves backwards ----------------------------------------

    /** AC-11: a backup that uploads only a late row older than the stored watermark leaves it at least as high. */
    @Test
    fun ac11_backupOfOnlyAnOlderLateRow_neverLowersTheWatermark() {
        establishWatermarkAtD()
        h.sms.add(D - 5 * HOUR, "+15550100001", "older late row")
        assertThat(h.backupRecording().appended).hasSize(1)
        assertThat(h.watermark()).isAtLeast(D)
    }

    /** AC-11: a result with no parseable date (max date -1) leaves the SMS watermark unchanged (production seam). */
    @Test
    fun ac11_confirmedDateMinusOne_leavesTheWatermarkUnchanged() {
        h.dataTypePreferences.setMaxSyncedDate(DataType.SMS, 5000L)
        val transport = RecordingTransport()
        val worker = TestListenableWorkerBuilder<BackupWorker>(h.context)
            .setWorkerFactory(BackupWorker.TestableBackupWorkerFactoryWithTransport(transport))
            .build() as BackupWorker
        worker.appendBatchAndUpdateWatermark(transport, DataType.SMS, ConversionResult(DataType.SMS), h.dataTypePreferences)
        assertThat(h.watermark()).isEqualTo(5000L)
    }

    // ---- AC-15: first run ------------------------------------------------------------------

    /** AC-15: no watermark: only today's query is sent; every SMS is uploaded once, within the limit. */
    @Test
    fun ac15_firstRun_sendsOnlyTodaysQuery_andUploadsEverySmsOnce() {
        val rows = (1..3).map { h.sms.add(D + it * 1000L, "+1555010000$it", "first run $it") }
        h.setMaxItemsPerSync(2)
        assertThat(h.preferences.isFirstBackup).isTrue()
        assertThat(ids(h.backupRecording())).containsExactly(rows[0], rows[1]).inOrder()
        assertThat(h.sms.queries.map { it.selection }).containsExactly("date > ? AND type <> ?")
        assertThat(h.sms.queries.single().args).containsExactly("-1", "3").inOrder()
        assertThat(ids(h.backupRecording())).containsExactly(rows[2])
        assertThat(h.backupRecording().appended).isEmpty()
    }

    /**
     * AC-15: a first run that only examines a row the converter rejects records it but stays a
     * first run (the record's keys never end it); a first run that finds nothing writes the -1
     * sentinel to SMS and MMS, as today.
     */
    @Test
    fun ac15_recordOnlyWritesKeepTheFirstRun_emptyFirstRunWritesTheSentinel() {
        h.sms.add(D, "", "no address")
        h.backupRecording()
        assertThat(h.preferences.isFirstBackup).isTrue()
        assertThat(h.dataTypePreferences.getSmsBackupRecord()).hasSize(1)
        h.sms.rows.clear()
        h.backupRecording()
        val raw = PreferenceManager.getDefaultSharedPreferences(h.context)
        assertThat(raw.getLong(DataType.SMS.maxSyncedPreference, 0L)).isEqualTo(-1L)
        assertThat(raw.getLong(DataType.MMS.maxSyncedPreference, 0L)).isEqualTo(-1L)
        assertThat(h.preferences.isFirstBackup).isFalse()
    }

    // ---- AC-16: upgrade --------------------------------------------------------------------

    /**
     * AC-16: a stored watermark and no record (the version before U-070). The first backup
     * uploads exactly the SMS above the watermark; every SMS visible at its start at or below the
     * watermark is not uploaded (today's outcome); an SMS that becomes visible afterwards is.
     */
    @Test
    fun ac16_upgrade_firstBackupUploadsExactlyTheForwardRows_laterLateRowUploaded() {
        h.sms.add(D - W_MS - HOUR, "+15550100001", "older than W")
        h.sms.add(D - 2 * HOUR, "+15550100002", "below, inside W")
        h.sms.add(D, "+15550100003", "at the watermark")
        val above1 = h.sms.add(D + 1000, "+15550100004", "above 1")
        val above2 = h.sms.add(D + 2000, "+15550100005", "above 2")
        h.dataTypePreferences.setMaxSyncedDate(DataType.SMS, D)
        assertThat(h.dataTypePreferences.hasSmsBackupRecordMarker()).isFalse()
        assertThat(ids(h.backupRecording())).containsExactly(above1, above2).inOrder()
        val late = h.sms.add(D + 1500, "+15550100006", "visible after the first post-upgrade backup")
        assertThat(ids(h.backupRecording())).containsExactly(late)
        assertThat(h.backupRecording().appended).isEmpty()
    }

    // ---- AC-18: MMS and call log unchanged ---------------------------------------------------

    /** AC-18: late MMS and call-log rows are not selected; their queries are today's; no window query for either. */
    @Test
    fun ac18_mmsAndCallLog_keepTodaysQueries_noWindowQuery_lateRowsNotSelected() {
        val mms = h.registerMms()
        val calls = h.registerCallLog()
        h.dataTypePreferences.setBackupEnabled(true, DataType.CALLLOG)
        h.dataTypePreferences.setMaxSyncedDate(DataType.MMS, 1_700_000_000L)
        h.dataTypePreferences.setMaxSyncedDate(DataType.CALLLOG, D)
        h.dataTypePreferences.setMaxSyncedDate(DataType.SMS, D)
        mms.add(1_700_000_000L - 3_600L, null, null, type = 128)
        calls.add(D - HOUR, "+15550100001", null, type = 3)
        h.sms.add(D, "+15550100009", "sms row at the watermark")
        h.backupRecording()
        assertThat(mms.queries.map { it.selection }).containsExactly("date > ? AND m_type <> ?")
        assertThat(mms.queries.single().args).containsExactly("1700000000", "134").inOrder()
        assertThat(mms.queries.single().returnedIds).isEmpty()
        assertThat(calls.queries.map { it.selection }).containsExactly("date > ?")
        assertThat(calls.queries.single().args).containsExactly(D.toString())
        assertThat(calls.queries.single().returnedIds).isEmpty()
    }

    /** AC-18: an MMS upload writes the MMS watermark exactly as today: the confirmed value, in seconds, outright. */
    @Test
    fun ac18_mmsWatermark_isWrittenAsToday_inSecondsAndOutright() {
        val raw = PreferenceManager.getDefaultSharedPreferences(h.context)
        h.dataTypePreferences.setMaxSyncedDate(DataType.MMS, 1_700_000_500L)
        val transport = StubMailTransport()
        val worker = TestListenableWorkerBuilder<BackupWorker>(h.context)
            .setWorkerFactory(BackupWorker.TestableBackupWorkerFactoryWithTransport(transport))
            .build() as BackupWorker
        val result = ConversionResult(DataType.MMS)
        val message = Mockito.mock(ImapMessage::class.java)
        Mockito.`when`(message.getHeader(Headers.DATE)).thenReturn(arrayOf("1700000100"))
        result.add(message, emptyMap())
        worker.appendBatchAndUpdateWatermark(transport, DataType.MMS, result, h.dataTypePreferences)
        assertThat(raw.getLong(DataType.MMS.maxSyncedPreference, 0L)).isEqualTo(1_700_000_100L)
        assertThat(h.dataTypePreferences.getMaxSyncedDate(DataType.MMS)).isEqualTo(1_700_000_100_000L)
        assertThat(h.dataTypePreferences.getSmsBackupRecord()).isEmpty()
    }

    // ---- AC-19: cost ---------------------------------------------------------------------------

    /**
     * AC-19: no row dated W or more below the run's starting watermark is returned; a run sends one
     * SMS query beyond today's (the upgrade run one more, the seed, once); a run uploading one row
     * makes today's transport calls; a run selecting nothing makes none.
     */
    @Test
    fun ac19_onlyRowsWithinWAreReturned_oneExtraQuery_noExtraServerCall() {
        h.sms.add(D - W_MS - 30 * 60_000L, "+15550100001", "6.5 h below")
        h.sms.add(D - W_MS, "+15550100002", "exactly W below")
        h.sms.add(D - HOUR, "+15550100003", "1 h below")
        h.sms.add(D, "+15550100004", "at the watermark")
        h.dataTypePreferences.setMaxSyncedDate(DataType.SMS, D)
        val upgrade = h.backupRecording()
        assertThat(upgrade.calls).isEmpty()
        assertThat(h.sms.queries).hasSize(3)
        assertThat(h.sms.queries.count { it.projection == listOf("date", "address", "body") }).isEqualTo(1)
        assertThat(h.sms.queries.flatMap { it.returnedDates }.all { it > D - W_MS }).isTrue()

        val late = h.sms.add(D - 2 * HOUR, "+15550100005", "late")
        h.sms.clearQueries()
        val run = h.backupRecording()
        assertThat(ids(run)).containsExactly(late)
        assertThat(h.sms.queries).hasSize(2)
        assertThat(h.sms.queries.flatMap { it.returnedDates }.all { it > D - W_MS }).isTrue()
        assertThat(run.calls).containsExactly("checkSettings", "openFolder", "appendMessages", "closeFolders").inOrder()

        h.sms.clearQueries()
        val idle = h.backupRecording()
        assertThat(idle.calls).isEmpty()
        assertThat(h.sms.queries).hasSize(2)
    }

    // ---- AC-20: pruning boundary -------------------------------------------------------------

    /** AC-20: after the watermark reaches D, the entry W minus 1 ms below stays and suppresses its row; the one exactly W below is gone. */
    @Test
    fun ac20_pruning_entryWMinus1msBelowKeptAndSuppresses_entryExactlyWBelowRemoved() {
        h.sms.add(D - W_MS, "+15550100001", "exactly W below")
        h.sms.add(D - W_MS + 1, "+15550100002", "W minus 1 ms below")
        h.sms.add(D, "+15550100003", "watermark row")
        assertThat(h.backupRecording().appended).hasSize(3)
        val dates = h.dataTypePreferences.getSmsBackupRecord().map { it.substringBefore(':').toLong() }
        assertThat(dates).containsExactly(D - W_MS + 1, D).inOrder()
        assertThat(h.backupRecording().appended).isEmpty()
    }

    // ---- AC-21: rows the converter does not upload ------------------------------------------

    /**
     * AC-21: limit 2 and two late rows the converter rejects (empty address): after one run that
     * examined them they are recorded, a genuine late SMS is uploaded by the next run, and a run
     * that finds only such rows makes no transport call.
     */
    @Test
    fun ac21_rowsTheConverterRejects_recordedAsExamined_lateSmsNotStarved_noLogin() {
        establishWatermarkAtD()
        h.sms.add(D - 3 * HOUR, "", "no address 1")
        h.sms.add(D - 2 * HOUR, "", "no address 2")
        h.setMaxItemsPerSync(2)
        assertThat(h.backupRecording().appended).isEmpty()
        val late = h.sms.add(D - HOUR, "+15550100001", "genuine late")
        assertThat(ids(h.backupRecording())).containsExactly(late)
        assertThat(h.backupRecording().calls).isEmpty()
    }

    // ---- AC-22: observable and private -------------------------------------------------------

    /**
     * AC-22: the seeding and selection lines carry counts only; no log line (priority DEBUG and
     * above), exception message or preference value carries the address or body; every record
     * entry is a date and a 16-hex digest. VERBOSE lines are excluded: they run only in debug
     * builds (App.LOCAL_LOGV) and PersonLookup's existing one logs the address (plan 2.5).
     */
    @Test
    fun ac22_logLinesCarryCountsOnly_noAddressOrBodyAnywhere() {
        val address = "+44 PRIVATE ADDRESS"
        val body = "PRIVATE BODY TEXT 271828"
        h.sms.add(D, "+15550100009", "watermark row")
        h.dataTypePreferences.setMaxSyncedDate(DataType.SMS, D)
        ShadowLog.clear()
        h.backupRecording()
        h.sms.add(D - HOUR, address, body)
        assertThat(h.backupRecording().appended).hasSize(1)
        val items = ShadowLog.getLogs().filter { it.type >= Log.DEBUG }
        val lines = items.map { it.msg }
        assertThat(lines).contains("BackupItemsFetcher: look-back seeded 1 SMS already on the phone (upgrade)")
        assertThat(lines).contains("BackupItemsFetcher: look-back selected 1 late SMS, skipped 1 already backed up")
        lines.filter { it.startsWith("BackupItemsFetcher: look-back") }.forEach { assertThat(it).matches(LOOK_BACK_LINE) }
        (lines + items.mapNotNull { it.throwable?.message }).forEach {
            assertThat(it).doesNotContain(address)
            assertThat(it).doesNotContain(body)
        }
        PreferenceManager.getDefaultSharedPreferences(h.context).all.values.forEach {
            assertThat(it.toString()).doesNotContain(address)
            assertThat(it.toString()).doesNotContain(body)
        }
        h.dataTypePreferences.getSmsBackupRecord().forEach { assertThat(it).matches("-?[0-9]+:[0-9a-f]{16}") }
    }

    // ---- IC-2: the XOAuth2 retry fetch reaches the look-back ---------------------------------

    /** IC-2: the second fetch call site (the XOAuth2 retry, BackupWorker handleAuthError) also runs the window query. */
    @Test
    fun ic2_xoauthRetryPath_alsoRunsTheLookBack() {
        establishWatermarkAtD()
        val late = h.sms.add(D - HOUR, "+15550100001", "late")
        var logins = 0
        val transport = RecordingTransport().apply {
            onCheckSettings = { if (++logins == 1) throw XOAuth2FailedException(400, null) }
        }
        h.sms.clearQueries()
        h.backup(transport, engine = { real ->
            Mockito.spy(real).also {
                Mockito.doReturn(Mockito.mock(TokenRefresher::class.java)).`when`(it).createTokenRefresher()
            }
        })
        assertWithMessage("SMS queries: ${h.sms.queries}").that(ids(transport)).containsExactly(late)
        assertThat(h.sms.queries.count { isWindowQuery(it) }).isEqualTo(2)
    }

    // ---- Iteration 2: a restore running while the backup's queries run (contract-verification O-1) ----

    /**
     * REQ AC-16 with a concurrent restore: the provider double performs a restore's two steps
     * (the write-ahead entry, then the insert) at the start of the window query, for a row within
     * W below the watermark, and at the start of the forward query, for a row above it. That is
     * after any record read the fetch makes before its queries. The backup uploads neither restored
     * row, uploads the genuine late SMS, and the next backup uploads nothing.
     */
    @Test
    fun race_restoreRecordsAndInsertsWhileTheQueriesRun_restoredRowsNotUploaded() {
        establishWatermarkAtD()
        val late = h.sms.add(D - 3 * HOUR, "+15550100001", "genuine late")
        fun restoreStep(date: Long, address: String) {
            val values = ContentValues().apply {
                put("date", date); put("address", address); put("body", "restored $date"); put("type", SmsFixture.INBOX)
            }
            h.dataTypePreferences.recordRestoredSms(values)
            h.sms.add(date, address, "restored $date")
        }
        h.sms.beforeQuery = { selection ->
            when {
                selection.startsWith("date > ? AND date <= ?") -> restoreStep(D - HOUR, "+15550300001")
                selection.startsWith("date > ? AND type <> ?") -> restoreStep(D + HOUR, "+15550300002")
            }
        }
        val run = h.backupRecording()
        h.sms.beforeQuery = null
        assertWithMessage("SMS queries: ${h.sms.queries}").that(ids(run)).containsExactly(late)
        assertThat(h.sms.rows).hasSize(4)
        assertThat(h.backupRecording().appended).isEmpty()
    }

    private companion object {
        const val LOOK_BACK_LINE = "BackupItemsFetcher: look-back (seeded [0-9]+ SMS already on the phone \\((upgrade|skip)\\)|selected [0-9]+ late SMS, skipped [0-9]+ already backed up)"
    }
}
```

### 4.4 T4 — new `SmsBackupRecordSizeTest.kt` (1 test; its own class so mutation runs can leave it out)

```kotlin
package com.zegoggles.smssync.service

import androidx.preference.PreferenceManager
import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.preferences.SmsBackupWindow
import com.zegoggles.smssync.service.SmsFixture.HOUR
import com.zegoggles.smssync.service.SmsFixture.W_MS
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** U-070 AC-20 (REQ AC-23): the record is bounded by the window, not by the age of the install. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SmsBackupRecordSizeTest {

    /** 200 backups of 50 SMS each, in date order, each group more than W after the one before. */
    @Test
    fun ac20_twoHundredBackupsOfFifty_recordAfterThe200thNoLargerThanAfterThe2nd() {
        @Suppress("DEPRECATION")
        val h = LateSmsHarness(RuntimeEnvironment.application)
        val transport = RecordingTransport()
        val raw = PreferenceManager.getDefaultSharedPreferences(h.context)
        val t0 = 1_700_000_000_000L
        var countAfter2 = -1
        var lengthAfter2 = -1
        for (run in 1..200) {
            val groupStart = t0 + (run - 1) * (W_MS + HOUR)
            for (i in 0 until 50) {
                h.sms.add(groupStart + i * 1000L, "+1555${run.toString().padStart(3, '0')}${i.toString().padStart(4, '0')}", "run $run message $i")
            }
            h.backup(transport)
            val count = h.dataTypePreferences.getSmsBackupRecord().size
            val length = raw.getString(SmsBackupWindow.RECORD_KEY, "")!!.length
            if (run == 2) { countAfter2 = count; lengthAfter2 = length }
            if (run == 200) {
                assertThat(count).isAtMost(countAfter2)
                assertThat(length).isAtMost(lengthAfter2)
            }
        }
        assertThat(countAfter2).isEqualTo(50)
        assertThat(transport.appended).hasSize(200 * 50)
        assertThat(transport.appended.map { it.smsId }).containsNoDuplicates()
    }
}
```

### 4.5 T5 — new `app/src/test/java/com/zegoggles/smssync/preferences/SmsBackupWindowTest.kt` (6 tests)

```kotlin
package com.zegoggles.smssync.preferences

import android.content.ContentValues
import android.content.Context
import androidx.preference.PreferenceManager
import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.service.CountingSharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/** U-070: W, the record's key and text form, and the DataTypePreferences writes that keep it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SmsBackupWindowTest {

    private lateinit var context: Context
    private val wm = 1_700_000_000_000L
    private val w = SmsBackupWindow.LOOK_BACK_MS

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        context = RuntimeEnvironment.application
    }

    /** AC-4: W is 6 hours, 21,600,000 ms. */
    @Test
    fun ac4_lookBackIsSixHours() {
        assertThat(SmsBackupWindow.LOOK_BACK_MS).isEqualTo(21_600_000L)
        assertThat(SmsBackupWindow.LOOK_BACK_MS).isEqualTo(TimeUnit.HOURS.toMillis(6))
    }

    /**
     * The key: the date and a stable truncated SHA-256 of the length-prefixed fields, hashed as
     * UTF-16 code units (vectors computed independently, L21). Malformed text (unpaired
     * surrogates) and "?" give three different keys.
     */
    @Test
    fun entry_isTheDateAndAStableDigest_ofLengthPrefixedFields() {
        val d = "1700000000000"
        assertThat(SmsBackupWindow.entryOf(d, "+15550100001", "hello")).isEqualTo("1700000000000:d6a72b9c218f03eb")
        assertThat(SmsBackupWindow.entryOf(d, null, null)).isEqualTo("1700000000000:3706ccdcb316cbe4")
        assertThat(SmsBackupWindow.entryOf(d, "+15550100001", "héllo 😀")).isEqualTo("1700000000000:5588e89c715c32e9")
        assertThat(SmsBackupWindow.entryOf(d, "+1555010000", "1hello")).isEqualTo("1700000000000:49c3523d5656f055")
        assertThat(SmsBackupWindow.entryOf(d, "+15550100001", "\uD800")).isEqualTo("1700000000000:b237423d5ff1a2c6")
        assertThat(SmsBackupWindow.entryOf(d, "+15550100001", "\uD801")).isEqualTo("1700000000000:e636bc987e5e57c5")
        assertThat(SmsBackupWindow.entryOf(d, "+15550100001", "?")).isEqualTo("1700000000000:570e37f30ca551d2")
        assertThat(SmsBackupWindow.entryOf("not a date", "+15550100001", "hello")).isNull()
        assertThat(SmsBackupWindow.entryOf(null, "+15550100001", "hello")).isNull()
    }

    /** The text form round-trips, ignores malformed parts, prunes at or below watermark - W, and counts entries above. */
    @Test
    fun record_roundTrips_prunesAtOrBelowWatermarkMinusW_countsEntriesAbove() {
        val record = SmsBackupWindow.parse(null)
        listOf(wm - w, wm - w + 1, wm, wm + 5).forEach { record.add(SmsBackupWindow.entryOf(it.toString(), "+1", "x")!!) }
        val text = SmsBackupWindow.serialise(record)!!
        assertThat(SmsBackupWindow.parse(text)).containsExactlyElementsIn(record).inOrder()
        assertThat(SmsBackupWindow.parse("$text,garbage,12:short")).containsExactlyElementsIn(record)
        SmsBackupWindow.prune(record, wm)
        assertThat(record.map { SmsBackupWindow.dateOf(it) }).containsExactly(wm - w + 1, wm, wm + 5).inOrder()
        assertThat(SmsBackupWindow.countAbove(record, wm)).isEqualTo(1)
        assertThat(SmsBackupWindow.serialise(SmsBackupWindow.parse(""))).isNull()
    }

    /**
     * AC-10, AC-11: the SMS write after an append is max(stored, confirmed), adds the row, and is
     * one commit carrying watermark and record; over a watermark with no marker (an upgrade not
     * yet seeded) it does not write the marker; over no watermark (a first backup) it does.
     */
    @Test
    fun afterAppend_sms_maxOfStoredAndConfirmed_oneCommit_markerOnlyWhenSafe() {
        val counting = CountingSharedPreferences(PreferenceManager.getDefaultSharedPreferences(context))
        val prefs = Preferences(context, counting).dataTypePreferences
        prefs.setMaxSyncedDate(DataType.SMS, 9_000L)
        counting.commits.clear()
        prefs.setMaxSyncedDateAfterAppend(DataType.SMS, 5_000L, listOf(mapOf("date" to "5000", "address" to "+1", "body" to "x")))
        assertThat(prefs.getMaxSyncedDate(DataType.SMS)).isEqualTo(9_000L)
        assertThat(prefs.getSmsBackupRecord()).containsExactly(SmsBackupWindow.entryOf("5000", "+1", "x"))
        assertThat(prefs.hasSmsBackupRecordMarker()).isFalse()
        assertThat(counting.commits).hasSize(1)
        assertThat(counting.commits.single()).containsAtLeast(DataType.SMS.maxSyncedPreference, SmsBackupWindow.RECORD_KEY)

        prefs.clearLastSyncData()
        prefs.setMaxSyncedDateAfterAppend(DataType.SMS, 100L, emptyList())
        assertThat(prefs.hasSmsBackupRecordMarker()).isTrue()
        assertThat(prefs.getMaxSyncedDate(DataType.SMS)).isEqualTo(100L)
    }

    /** AC-13: Reset and account removal (clearLastSyncData) remove the watermarks, the record and the marker. */
    @Test
    fun clearLastSyncData_removesTheRecordAndTheMarker() {
        val prefs = Preferences(context).dataTypePreferences
        prefs.setMaxSyncedDateAfterAppend(DataType.SMS, wm, listOf(mapOf("date" to wm.toString(), "address" to "+1", "body" to "x")))
        assertThat(prefs.getSmsBackupRecord()).hasSize(1)
        assertThat(prefs.hasSmsBackupRecordMarker()).isTrue()
        prefs.clearLastSyncData()
        assertThat(prefs.getSmsBackupRecord()).isEmpty()
        assertThat(prefs.hasSmsBackupRecordMarker()).isFalse()
        assertThat(Preferences(context).isFirstBackup).isTrue()
    }

    /** AC-14: the restore write records only rows the window can reach, and writes neither watermark nor marker. */
    @Test
    fun recordRestoredSms_recordsOnlyRowsTheWindowCanReach() {
        val prefs = Preferences(context).dataTypePreferences
        prefs.setMaxSyncedDate(DataType.SMS, wm)
        fun values(date: Long) = ContentValues().apply { put("date", date); put("address", "+1"); put("body", "r$date"); put("type", 1) }
        prefs.recordRestoredSms(values(wm - w - 1))
        prefs.recordRestoredSms(values(wm - w + 1))
        prefs.recordRestoredSms(values(wm + 3_600_000L))
        assertThat(prefs.getSmsBackupRecord().map { SmsBackupWindow.dateOf(it) }).containsExactly(wm - w + 1, wm + 3_600_000L).inOrder()
        assertThat(prefs.getMaxSyncedDate(DataType.SMS)).isEqualTo(wm)
        assertThat(prefs.hasSmsBackupRecordMarker()).isFalse()
    }
}
```

### 4.6 T6 — new `BackupQueryBuilderWindowTest.kt` (3 tests)

```kotlin
package com.zegoggles.smssync.service

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.contacts.ContactGroupIds
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.preferences.DataTypePreferences
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** U-070: the window and seed queries (the forward queries are pinned, unedited, by BackupQueryBuilderTest). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BackupQueryBuilderWindowTest {

    private val wm = 1_700_000_000_000L
    private val builder = BackupQueryBuilder(Mockito.mock(DataTypePreferences::class.java))

    /** The window: W below the watermark (exclusive) up to it (inclusive), drafts excluded, sorted by date, no LIMIT. */
    @Test
    fun windowQuery_selectsWBelowTheWatermark_draftsExcluded_sortedByDate_noLimit() {
        val q = builder.buildSmsWindowQuery(null, wm)
        assertThat(q.uri).isEqualTo(Uri.parse("content://sms"))
        assertThat(q.projection).isNull()
        assertThat(q.selection).isEqualTo("date > ? AND date <= ? AND type <> ?")
        assertThat(q.selectionArgs.toList()).containsExactly((wm - 21_600_000L).toString(), wm.toString(), "3").inOrder()
        assertThat(q.sortOrder).isEqualTo("date")
    }

    /** The window carries exactly the forward query's contact-group clause. */
    @Test
    fun windowQuery_carriesTheForwardQuerysContactGroupClause() {
        val ids = ContactGroupIds().apply { add(1L, 20L) }
        assertThat(builder.buildSmsWindowQuery(ids, wm).selection)
            .isEqualTo("date > ? AND date <= ? AND type <> ?  AND (type = 2 OR person IN (20))")
        assertThat(builder.buildQueryForDataType(DataType.SMS, ids, 5)!!.selection)
            .isEqualTo("date > ? AND type <> ?  AND (type = 2 OR person IN (20))")
    }

    /** The seed query (Skip, upgrade): same bounds, no group clause, the key columns only. */
    @Test
    fun seedQuery_sameBounds_noGroupClause_keyColumnsOnly() {
        val q = builder.buildSmsSeedQuery(wm)
        assertThat(q.projection!!.toList()).containsExactly("date", "address", "body").inOrder()
        assertThat(q.selection).isEqualTo("date > ? AND date <= ? AND type <> ?")
        assertThat(q.selectionArgs.toList()).containsExactly((wm - 21_600_000L).toString(), wm.toString(), "3").inOrder()
        assertThat(q.sortOrder).isEqualTo("date")
    }
}
```

### 4.7 T7 — new `LateVisibleSmsSkipResetRestoreTest.kt` (11 tests; written at I5)

**What S10 and S11 show, and what they do not** (iteration 2; plan-review O-4; Record 18).
- **They show:**
  - the record travels in a file the descriptor lets Android copy, with the names parsed from the resource;
  - after the copied files are re-created from their contents, the excluded files are emptied, and the provider is
    replaced by one holding the same messages under new row ids, the first backup uploads nothing already backed up
    (S10), and does upload a within-W SMS the old phone never uploaded (S11).
- **They do not show:**
  - Android's backup transport itself (key/value or full backup, its encryption, what a device-to-device transfer copies
    on Android 12 and above, where the platform may ignore the descriptor);
  - a different app version on the new phone;
  - a provider that changed a row's date, address or text, where a duplicate is accepted (REQ AC-19).
- The test empties the excluded files rather than deleting them. A preferences object for those names may stay cached in
  the process, and an emptied file holds what a fresh phone holds: nothing.

```kotlin
package com.zegoggles.smssync.service

import android.content.ContentValues
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.R
import com.zegoggles.smssync.contacts.ContactAccessor
import com.zegoggles.smssync.di.MailTransportFactory
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.mail.transport.MailTransport
import com.zegoggles.smssync.preferences.AuthPreferences
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.preferences.SmsBackupWindow
import com.zegoggles.smssync.service.SmsFixture.D
import com.zegoggles.smssync.service.SmsFixture.HOUR
import com.zegoggles.smssync.service.SmsFixture.INBOX
import com.zegoggles.smssync.service.SmsFixture.OWNER
import com.zegoggles.smssync.service.SmsFixture.W_MS
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import java.io.File

/** U-070: Skip, Reset, restore and Android's own backup to another phone, through their production entries. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LateVisibleSmsSkipResetRestoreTest {

    private lateinit var h: LateSmsHarness

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        h = LateSmsHarness(RuntimeEnvironment.application)
    }

    private fun ids(t: RecordingTransport) = t.appended.map { it.smsId!!.toLong() }

    /** A transport that fails the test on any call: Skip connects to no server. */
    private fun refusingTransport() = RecordingTransport().apply {
        onCheckSettings = { throw AssertionError("Skip must not connect") }
        onOpenFolder = { throw AssertionError("Skip must not open a folder") }
        beforeAppend = { throw AssertionError("Skip must not upload") }
    }

    // ---- AC-12, AC-11: Skip ------------------------------------------------------------------

    /** AC-12 (a): every SMS visible at Skip, W or more below the new watermark or within W, is not uploaded later. */
    @Test
    fun ac12a_skip_everySmsVisibleAtSkip_isNeverUploaded() {
        h.sms.add(D - W_MS - HOUR, "+15550100001", "W + 1 h below")
        h.sms.add(D - W_MS, "+15550100002", "exactly W below")
        h.sms.add(D - 2 * HOUR, "+15550100003", "2 h below")
        h.sms.add(D, "+15550100004", "newest")
        val skip = refusingTransport()
        assertThat(h.backup(skip, skip = true)).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertThat(skip.calls).isEmpty()
        assertThat(h.watermark()).isEqualTo(D)
        val after = h.backupRecording()
        assertThat(after.appended).isEmpty()
        assertThat(after.calls).isEmpty()
    }

    /** AC-12 (b) (DP-3): an SMS hidden from the app at Skip and released within W is uploaded once. */
    @Test
    fun ac12b_skip_smsHiddenAtSkipAndReleasedWithinW_isUploadedOnce() {
        h.sms.add(D, "+15550100004", "newest")
        val hidden = h.sms.add(D - HOUR, "+15550100001", "one-time code 123456")
        h.sms.hide(hidden)
        h.backup(refusingTransport(), skip = true)
        h.sms.release(hidden)
        assertThat(ids(h.backupRecording())).containsExactly(hidden)
        assertThat(h.backupRecording().appended).isEmpty()
    }

    /** AC-12: Skip over a type set without SMS leaves the SMS watermark and the record untouched. */
    @Test
    fun ac12_skipWithoutSms_leavesTheSmsWatermarkAndRecordUntouched() {
        h.sms.add(D, "+15550100004", "backed up")
        h.backupRecording()
        val record = h.dataTypePreferences.getSmsBackupRecord()
        h.dataTypePreferences.setBackupEnabled(false, DataType.SMS)
        h.sms.add(D + HOUR, "+15550100005", "newer")
        h.backup(refusingTransport(), skip = true)
        assertThat(h.watermark()).isEqualTo(D)
        assertThat(h.dataTypePreferences.getSmsBackupRecord()).isEqualTo(record)
    }

    /** AC-11: Skip still sets the SMS watermark outright (here lower than the stored one). */
    @Test
    fun ac11_skip_stillSetsTheWatermarkOutright() {
        h.dataTypePreferences.setMaxSyncedDate(DataType.SMS, D + 5 * HOUR)
        h.sms.add(D, "+15550100004", "newest visible")
        h.backup(refusingTransport(), skip = true)
        assertThat(h.watermark()).isEqualTo(D)
    }

    // ---- AC-13: Reset and account removal ------------------------------------------------------

    /**
     * AC-13: after backups and a late row, clearLastSyncData returns the app to a first run with
     * no record and no marker; Backup then uploads every visible SMS once over three runs with
     * limit 2, and a further backup uploads none.
     */
    @Test
    fun ac13_reset_backupThenUploadsEveryVisibleSmsOnce_withTheLimit() {
        val rows = (1..4).map { h.sms.add(D + it * 1000L, "+1555010000$it", "message $it") }
        assertThat(h.backupRecording().appended).hasSize(4)
        val late = h.sms.add(D + 500, "+15550100009", "late")
        h.dataTypePreferences.clearLastSyncData()
        assertThat(h.preferences.isFirstBackup).isTrue()
        assertThat(h.dataTypePreferences.getSmsBackupRecord()).isEmpty()
        assertThat(h.dataTypePreferences.hasSmsBackupRecordMarker()).isFalse()
        h.setMaxItemsPerSync(2)
        val uploaded = mutableListOf<Long>()
        repeat(3) { uploaded += ids(h.backupRecording()) }
        assertThat(uploaded).containsExactly(late, rows[0], rows[1], rows[2], rows[3]).inOrder()
        assertThat(h.backupRecording().appended).isEmpty()
    }

    /** AC-13: Reset, then Skip: AC-12 applies, nothing visible is uploaded. */
    @Test
    fun ac13_reset_thenSkip_nothingVisibleIsUploaded() {
        h.sms.add(D - HOUR, "+15550100001", "one")
        h.sms.add(D, "+15550100002", "two")
        h.backupRecording()
        h.dataTypePreferences.clearLastSyncData()
        h.backup(refusingTransport(), skip = true)
        assertThat(h.backupRecording().appended).isEmpty()
    }

    // ---- AC-14: restore -------------------------------------------------------------------------

    private fun restoreItem(date: Long, address: String, body: String) = ContentValues().apply {
        put("date", date); put("address", address); put("body", body); put("type", INBOX); put("read", 1)
    }

    private fun restoreFactory(transport: MailTransport) = object : WorkerFactory() {
        override fun createWorker(appContext: Context, workerClassName: String,
                                  workerParameters: WorkerParameters): ListenableWorker? {
            if (workerClassName != RestoreWorker::class.java.name) return null
            val prefs = Preferences(appContext)
            val authPrefs = AuthPreferences(appContext)
            return RestoreWorker(appContext, workerParameters, prefs, authPrefs, MailTransportFactory { transport },
                InMemoryCheckpointStore(), RestoreInsertInterceptor.NoOp, ContactAccessor(),
                WorkerEngineFactory(appContext, prefs, authPrefs))
        }
    }

    private fun restoreLoopWorker() = TestListenableWorkerBuilder<RestoreWorker>(h.context)
        .setWorkerFactory(RestoreWorker.TestableRestoreWorkerFactory(InMemoryCheckpointStore(), RestoreInsertInterceptor.NoOp))
        .build() as RestoreWorker

    /** AC-14 (a): a restore from the server (doWork) inserts SMS within W below and above the watermark; a later backup uploads none of them. */
    @Test
    fun ac14a_restoreFromTheServer_laterBackupUploadsNoneOfTheRestoredSms() {
        h.sms.add(D, "+15550100009", "watermark row")
        h.backupRecording()
        val transport = RecordingTransport().apply {
            restoreItems = listOf(restoreItem(D - 2 * HOUR, "+15550300001", "restored below"),
                                  restoreItem(D + HOUR, "+15550300002", "restored above"))
        }
        val restore = TestListenableWorkerBuilder<RestoreWorker>(h.context).setWorkerFactory(restoreFactory(transport)).build() as RestoreWorker
        assertThat(runBlocking { restore.doWork() }).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertThat(h.sms.rows).hasSize(3)
        assertThat(h.watermark()).isEqualTo(D + HOUR)
        assertThat(h.backupRecording().appended).isEmpty()
    }

    /** AC-14 (b): the process dies after the insert and before the watermark is raised; the next backup does not upload that row. */
    @Test
    fun ac14b_deathAfterTheInsertBeforeTheWatermarkIsRaised_rowNotUploaded() {
        h.sms.add(D, "+15550100009", "watermark row")
        h.backupRecording()
        h.sms.dieAfterNextInsert = true
        val death = runCatching {
            runBlocking { restoreLoopWorker().executeRestoreWithValues(listOf(restoreItem(D + HOUR, "+15550300003", "restored, then died")), 0, h.preferences, h.context, "u070_restore_death") }
        }.exceptionOrNull()
        assertThat(generateSequence(death) { it.cause }.any { it is SimulatedProcessDeath }).isTrue()
        assertThat(h.sms.rows).hasSize(2)
        assertThat(h.watermark()).isEqualTo(D)
        val genuine = h.sms.add(D + 2 * HOUR, "+15550100010", "a genuine new SMS")
        assertThat(ids(h.backupRecording())).containsExactly(genuine)
    }

    /** AC-14 (c): a late row present before a restore that raises the watermark by about 2 hours is uploaded by the next backup. */
    @Test
    fun ac14c_lateRowBeforeARestoreThatRaisesTheWatermark_isUploaded() {
        h.sms.add(D, "+15550100009", "watermark row")
        h.backupRecording()
        val lateDate = D - 60_000L
        val late = h.sms.add(lateDate, "+15550100001", "late, visible before the restore")
        runBlocking { restoreLoopWorker().executeRestoreWithValues(listOf(restoreItem(lateDate + 2 * HOUR, "+15550300004", "restored 2 h above the late row")), 0, h.preferences, h.context, "u070_restore_raise") }
        assertThat(h.watermark()).isEqualTo(lateDate + 2 * HOUR)
        assertThat(ids(h.backupRecording())).containsExactly(late)
        assertThat(h.backupRecording().appended).isEmpty()
    }

    // ---- AC-17: Android's own backup to another phone -------------------------------------------

    private fun excludedSharedPrefFiles(): Set<String> {
        val parser = h.context.resources.getXml(R.xml.backup_descriptor)
        val excluded = mutableSetOf<String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "exclude" &&
                parser.getAttributeValue(null, "domain") == "sharedpref") {
                excluded += parser.getAttributeValue(null, "path")
            }
            event = parser.next()
        }
        return excluded
    }

    /**
     * Copies to a fresh store the preference files that backup_descriptor.xml lets Android copy
     * (the excluded names are parsed, not hard-coded): each copied file is snapshotted, cleared
     * and written back from the snapshot, and every excluded file is cleared, as a restored phone
     * has none of them (iteration 2, plan-review O-4). The owner then signs in again. Asserts the
     * record travels in a copied file.
     */
    private fun moveToAnotherPhone() {
        val excluded = excludedSharedPrefFiles()
        val defaultFile = h.context.packageName + "_preferences.xml"
        assertThat(excluded).doesNotContain(defaultFile)
        val listed = File(h.context.applicationInfo.dataDir, "shared_prefs").listFiles().orEmpty()
            .map { it.name }.filter { it.endsWith(".xml") }
        assertThat(listed).contains(defaultFile)
        val copied = listed.filter { it !in excluded }
            .associateWith { h.context.getSharedPreferences(it.removeSuffix(".xml"), Context.MODE_PRIVATE).all.toMap() }
        assertThat(copied.values.any { it.containsKey(SmsBackupWindow.RECORD_KEY) }).isTrue()
        copied.forEach { (name, values) ->
            val editor = h.context.getSharedPreferences(name.removeSuffix(".xml"), Context.MODE_PRIVATE).edit().clear()
            values.forEach { (key, value) ->
                when (value) {
                    is Long -> editor.putLong(key, value)
                    is Int -> editor.putInt(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Float -> editor.putFloat(key, value)
                    is String -> editor.putString(key, value)
                    is Set<*> -> editor.putStringSet(key, value.map { it.toString() }.toSet())
                }
            }
            editor.commit()
        }
        excluded.forEach { h.context.getSharedPreferences(it.removeSuffix(".xml"), Context.MODE_PRIVATE).edit().clear().commit() }
        AuthPreferences(h.context).setImapUser(OWNER)
    }

    private val oldPhone = listOf(
        Triple(D - 2 * HOUR, "+15550100001", "one"),
        Triple(D - HOUR, "+15550100002", "two"),
        Triple(D, "+15550100003", "three")
    )

    private fun newPhoneWithTheSameMessagesUnderNewIds(): ProviderDouble {
        oldPhone.forEach { h.sms.add(it.first, it.second, it.third) }
        assertThat(h.backupRecording().appended).hasSize(3)
        moveToAnotherPhone()
        val newPhone = ProviderDouble(DataType.SMS)
        oldPhone.forEachIndexed { i, r -> newPhone.add(r.first, r.second, r.third, id = 1001L + i) }
        registerProvider(h.context, "sms", newPhone)
        return newPhone
    }

    /** AC-17: the first backup on the new phone uploads nothing dated at or below the restored watermark. */
    @Test
    fun ac17_androidBackupToAnotherPhone_firstBackupThereUploadsNothingAlreadyBackedUp() {
        val newPhone = newPhoneWithTheSameMessagesUnderNewIds()
        assertThat(h.backupRecording().appended).isEmpty()
        assertThat(newPhone.queries).isNotEmpty()
    }

    /** AC-17: an SMS within W below the restored watermark that the old phone never uploaded is uploaded. */
    @Test
    fun ac17_androidBackupToAnotherPhone_smsTheOldPhoneNeverUploaded_isUploaded() {
        val newPhone = newPhoneWithTheSameMessagesUnderNewIds()
        newPhone.add(D - 30 * 60_000L, "+15550100004", "only on the new phone", id = 2001L)
        assertThat(ids(h.backupRecording())).containsExactly(2001L)
    }
}
```

### 4.8 Test table (50 new tests)

"Bitten by" is iteration 2's complete list. It is derived test by test and is the transpose of the I8 failing sets.
Every test has at least one mutation, and every mutation id has at least one test (Record 18).

| Id | Class.test | AC | Bitten by |
|----|-----------|----|-----------|
| R1 | Regression.ac1_lateVisibleSms_isUploadedOnceByTheNextBackup | AC-1, AC-11 | red on start; M-L1, M-L5, M-L10, M-L18b |
| R2 | Regression.ac1_lateVisibleSms_afterBackupsInBetween_isUploadedOnce | AC-1 | red on start; M-L1, M-L5, M-L10, M-L18b |
| R3 | Regression.ac6_inOrderSequence_isTheStartCommitSequence | AC-6, AC-19 (calls) | M-L5 |
| B1 | Backup.ac2a_limitN_nPlusOneLate_… | AC-2 (a) | M-L1, M-L3a, M-L5, M-L10, M-L18b |
| B2 | Backup.ac2b_lateRowsCountAgainstTheLimit_… | AC-2 (b) | M-L1, M-L5, M-L18b (not M-L19i: plan-review B-2; not M-L8a: B2's MMS watermark is -1) |
| B3 | Backup.ac2c_alreadyBackedUpWindowRows_… | AC-2 (c) | M-L1, M-L5, M-L18b, M-L19i, M-L19ii |
| B4 | Backup.ac3_lateDraftAndOutOfGroup… | AC-3 | M-L1, M-L2i, M-L2ii, M-L3a, M-L3b, M-L4a, M-L4b, M-L5, M-L9a, M-L9b, M-L18b |
| B5 | Backup.ac5_lateRowAtWMinus1ms… | AC-5 | M-L1, M-L3a, M-L4b, M-L5, M-L18b |
| B6 | Backup.ac5_rowExactlyWBelow… | AC-5 | M-L2i, M-L2ii, M-L3b, M-L4a |
| B7 | Backup.ac7_runsWithALateRow_… | AC-7 | M-L1, M-L5, M-L18b |
| B8 | Backup.ac8_rowWhoseTypeChanges… | AC-8 | M-L5, M-L6 |
| B9 | Backup.ac9_openFolderFails_… | AC-9 | M-L1, M-L5, M-L18b |
| B10 | Backup.ac9_appendFails_… | AC-9 | M-L1, M-L5, M-L11, M-L18b |
| B11 | Backup.ac9_loginFails_… | AC-9 | M-L1, M-L5, M-L18b |
| B12 | Backup.ac9_cancelledDuringTheAppend_… | AC-9 | M-L1, M-L5, M-L11, M-L18b |
| B13 | Backup.ac10_deathAfterTheServerConfirmed_… | AC-10 | M-L1, M-L5, M-L11, M-L12a, M-L12b, M-L18b |
| B14 | Backup.ac11_backupOfOnlyAnOlderLateRow_… | AC-11 | M-L1, M-L3a, M-L5, M-L10, M-L18b |
| B15 | Backup.ac11_confirmedDateMinusOne_… | AC-11 | M-L10 |
| B16 | Backup.ac15_firstRun_sendsOnlyTodaysQuery_… | AC-15 | M-L5, M-L19ii, M-L21 |
| B17 | Backup.ac15_recordOnlyWritesKeepTheFirstRun_… | AC-15, AC-21 | M-L20 |
| B18 | Backup.ac16_upgrade_… | AC-16 | M-L1, M-L2i, M-L2ii, M-L5, M-L6, M-L7, M-L18a, M-L18b |
| B19 | Backup.ac18_mmsAndCallLog_keepTodaysQueries_… | AC-18 | M-L8a |
| B20 | Backup.ac18_mmsWatermark_isWrittenAsToday_… | AC-18 | M-L8b |
| B21 | Backup.ac19_onlyRowsWithinWAreReturned_… | AC-19 | M-L1, M-L2i, M-L2ii, M-L3b, M-L4a, M-L5, M-L6, M-L7, M-L10, M-L18a, M-L18b |
| B22 | Backup.ac20_pruning_entryWMinus1msBelow… | AC-20 | M-L2i, M-L2ii, M-L3a, M-L3b, M-L4a, M-L5, M-L13a, M-L13b |
| B23 | Backup.ac21_rowsTheConverterRejects_… | AC-21 | M-L1, M-L5, M-L18b, M-L20 |
| B24 | Backup.ac22_logLinesCarryCountsOnly_… | AC-22 | M-L1, M-L5, M-L18a, M-L18b |
| B25 | Backup.ic2_xoauthRetryPath_alsoRunsTheLookBack | IC-2 | M-L1, M-L4a, M-L5, M-L18b |
| B26 | Backup.race_restoreRecordsAndInsertsWhileTheQueriesRun_… (iteration 2) | AC-14, REQ AC-16 | M-L1, M-L3a, M-L5, M-L7, M-L17, M-L18b, M-L26 |
| Z1 | SmsBackupRecordSizeTest.ac20_twoHundredBackupsOfFifty_… | AC-20 | M-L13a |
| S1 | SkipResetRestore.ac12a_skip_… | AC-12 (a) | M-L2i, M-L2ii, M-L4a, M-L5, M-L6, M-L7, M-L15 |
| S2 | SkipResetRestore.ac12b_skip_smsHiddenAtSkip… | AC-12 (b), AC-23 (1) | M-L1, M-L5, M-L6, M-L7, M-L15, M-L18b |
| S3 | SkipResetRestore.ac12_skipWithoutSms_… | AC-12 | M-L22 (iteration 2) |
| S4 | SkipResetRestore.ac11_skip_stillSetsTheWatermarkOutright | AC-11 | M-L23 (iteration 2) |
| S5 | SkipResetRestore.ac13_reset_backupThenUploads… | AC-13 | M-L5, M-L14a, M-L14b, M-L19ii |
| S6 | SkipResetRestore.ac13_reset_thenSkip_… | AC-13 | M-L5, M-L6, M-L7, M-L15 |
| S7 | SkipResetRestore.ac14a_restoreFromTheServer_… | AC-14 (a) | M-L5, M-L7, M-L16a |
| S8 | SkipResetRestore.ac14b_deathAfterTheInsert… | AC-14 (b), AC-23 (2) | M-L5, M-L7, M-L16a, M-L16b, M-L17 |
| S9 | SkipResetRestore.ac14c_lateRowBeforeARestore… | AC-14 (c) | M-L1, M-L5, M-L7, M-L16a, M-L18b |
| S10 | SkipResetRestore.ac17_…firstBackupThereUploadsNothing… | AC-17 | M-L5, M-L7 |
| S11 | SkipResetRestore.ac17_…smsTheOldPhoneNeverUploaded… | AC-17 | M-L1, M-L5, M-L7, M-L18b |
| W1 | SmsBackupWindowTest.ac4_lookBackIsSixHours | AC-4 | M-L3a, M-L3b |
| W2 | SmsBackupWindowTest.entry_isTheDateAndAStableDigest_… | AC-22, key | M-L24 (iteration 2) |
| W3 | SmsBackupWindowTest.record_roundTrips_prunes… | AC-20 | M-L13a, M-L13b |
| W4 | SmsBackupWindowTest.afterAppend_sms_… | AC-10, AC-11, AC-16 | M-L6, M-L7, M-L10, M-L12a, M-L12b, M-L18b |
| W5 | SmsBackupWindowTest.clearLastSyncData_removes… | AC-13 | M-L14a, M-L14b, M-L18b |
| W6 | SmsBackupWindowTest.recordRestoredSms_… | AC-14 | M-L13a, M-L13b, M-L25 (iteration 2) |
| Q1 | BackupQueryBuilderWindowTest.windowQuery_selects… | AC-4, AC-19 | M-L2i, M-L2ii, M-L3a, M-L3b, M-L4a, M-L4b, M-L9a |
| Q2 | BackupQueryBuilderWindowTest.windowQuery_carries… | AC-3 | M-L4a, M-L9b |
| Q3 | BackupQueryBuilderWindowTest.seedQuery_… | AC-12, AC-16 | M-L2i, M-L2ii, M-L3a, M-L3b, M-L4b, M-L9a |

Per file (`grep -c "@Test"`): Regression 3, Backup 26, Size 1, SkipResetRestore 11, SmsBackupWindowTest 6,
BackupQueryBuilderWindowTest 3. Total **50**.

## 5. Increments

### 5.0 Environment and conventions (every step)

```bash
# From the repository root, in a shell WITHOUT MSYS_NO_PATHCONV.
unset MSYS_NO_PATHCONV
W=sdlc/artifacts/build/sprints/sprint-024/U-070; E=$W/evidence; M=$E/mutations
T=sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools      # read-only
INIT=$T/junit-xml.init.gradle
R=app/build/test-results/testDebugUnitTest
J=app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
S=app/src/main/java/com/zegoggles/smssync
BW=$S/service/BackupWorker.kt; RW=$S/service/RestoreWorker.kt
BIF=$S/service/BackupItemsFetcher.java; BQB=$S/service/BackupQueryBuilder.java; LVC=$S/service/LateVisibleSmsCursor.java
DTP=$S/preferences/DataTypePreferences.java; SBW=$S/preferences/SmsBackupWindow.java
MT=$S/mail/transport/MailTransport.java
TS=app/src/test/java/com/zegoggles/smssync/service; TP=app/src/test/java/com/zegoggles/smssync/preferences
WORK=<a u070-work folder inside the developer's own session scratch folder>   # raw logs, pristine copies; never written into a repository file
```
- **Class sets** (`--tests` values):
  - **NEW5** = `com.zegoggles.smssync.service.LateVisibleSmsRegressionTest`, `…service.LateVisibleSmsBackupTest`,
    `…service.LateVisibleSmsSkipResetRestoreTest`, `…service.BackupQueryBuilderWindowTest`,
    `com.zegoggles.smssync.preferences.SmsBackupWindowTest` (49 cases: 3 + 26 + 11 + 3 + 6).
  - **SIZE** = `…service.SmsBackupRecordSizeTest` (1).
  - **PIN** = `…service.BackupQueryBuilderTest`, `BackupWorkerWatermarkTest`, `BackupWorkerIntegrationTest`,
    `BackupItemsFetcherTest`, `BulkFetcherTest`, `BackupCursorsTest`, `BackupWorkerTest`,
    `BackupWorkerLoopCancellationTest`, `RestoreWorkerIntegrationTest`, `RestoreWorkerCheckpointTest`,
    `RestoreWorkerTest`, `com.zegoggles.smssync.preferences.PreferencesTest`,
    `com.zegoggles.smssync.mail.MessageConverterTest`, `com.zegoggles.smssync.mail.transport.K9MailTransportTest`.
- **TR(name, classes…)**: one Gradle call, foreground, tool timeout 600000 ms, results folder deleted first:
  ```bash
  rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:testDebugUnitTest --tests "<C1>" [--tests "<C2>" ...] > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log
  python -I -X utf8 $T/sanitize.py $WORK/logs/<name>.log $E/<name>.txt --grep '^(BUILD |FAILURE|U063-DAEMON-JVM|exit=)|tests completed|FAILED$'
  python -I -X utf8 $T/junit_cases.py $R > $WORK/logs/<name>-cases.log; python -I -X utf8 $T/sanitize.py $WORK/logs/<name>-cases.log <dest>-cases.txt
  ```
  For a failure message:
  `python -I -X utf8 -c "import sys,glob,xml.etree.ElementTree as X;[print(c.get('name'),'::',(f.get('message') or '')[:4000]) for p in glob.glob(sys.argv[1]+'/TEST-*.xml') for c in X.parse(p).iter('testcase') for f in c.findall('failure')]" $R > $WORK/logs/<name>-msg.log`,
  then sanitise into `<dest>-message.txt`.
- **Gate G**: `rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease :app:jacocoTestReport > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log`.
  - Then sanitise with
    `--grep '^(BUILD |FAILURE|> Task :app:(assemble|test|jacoco)|U063-DAEMON-JVM|exit=)|tests completed|FAILED$'`.
  - Then `python -I -X utf8 $T/junit_totals.py $R > $E/<name>-junit-totals.txt` and
    `python -I -X utf8 $T/jacoco_pkgs.py $J > $WORK/logs/<name>-jacoco.log`. Sanitise the second with
    `--grep 'smssync/(service|preferences)$|smssync/(service|preferences) '`.
  - `:app:jacocoTestReport` is reporting only; it changes no floor.
- **Timestamps** come from `date -u +%Y-%m-%dT%H:%M:%SZ`.
- **CRLF check of an edited tracked file**:
  `python -I -c "import sys;d=open(sys.argv[1],'rb').read();print(sys.argv[1],d.count(b'\n'),d.count(b'\r\n'))" <file>`.
  The two counts are equal.
- **This story's own paths** (iteration 2; the U-068 scoping, plan-review O-6, Record 18). The variable `OWN` is used
  by I0 and I9:
  ```bash
  OWN="$SBW $LVC $DTP $BQB $BIF $BW $RW $MT README.md $TS/LateVisibleSmsFixtures.kt $TS/LateVisibleSmsRegressionTest.kt $TS/LateVisibleSmsBackupTest.kt $TS/SmsBackupRecordSizeTest.kt $TS/LateVisibleSmsSkipResetRestoreTest.kt $TS/BackupQueryBuilderWindowTest.kt $TP/SmsBackupWindowTest.kt"
  MINE="$TS/LateVisibleSmsFixtures.kt $TS/LateVisibleSmsRegressionTest.kt $TS/LateVisibleSmsBackupTest.kt $TS/SmsBackupRecordSizeTest.kt $TS/LateVisibleSmsSkipResetRestoreTest.kt $TS/BackupQueryBuilderWindowTest.kt $TP/SmsBackupWindowTest.kt"
  ```
  Together with `$W`, these are the only paths whose changes count as this story's. Any other changed, uncommitted or
  untracked file in the checkout is "not this story's". It is recorded with its path, and at I9 with its `@Test`
  count. It is never counted as this story's, and it is never a stop except as rows D1 and D17 say.

### 5.1 Sessions and hand-over (iteration 2; plan-review O-9; Record 18)

The run is planned as **two developer sessions**. Neither session commits anything.
- **Session 1: I0 to I7.** It ends with every production edit and every test in place and green (I7), the pristine
  copies taken, the seven `<NAME>_NEW` blobs written to the object store (`git hash-object -w`), and `PROD_DIFF`
  recorded. Its last log entry is the hand-over block below, with "Next step: S2-0".
- **Session 2: S2-0, then I8 to I11.**
  - **S2-0, resume** (no Gradle):
    1. `git rev-parse HEAD`, and `git log --oneline $START..HEAD`. Commits by others are recorded. A commit that
       touches `OWN` is row D1.
    2. `git status --porcelain -- $OWN` lists exactly this story's seven ` M` paths (the five edited production files,
       `MailTransport.java` and `README.md`) and nine `??` paths (the two new production files and the seven test
       files).
    3. `git hash-object` of the seven files equals `<NAME>_NEW`.
    4. `git diff HEAD -- app/src/main | git hash-object --stdin` equals `PROD_DIFF`.
    5. `mutation-log.md` has no `APPLYING` entry without a `restored` line.
    6. Rebuild `$WORK/pristine/` in the new session's own work folder from the blobs, which needs no copy from session 1:
       - tracked CRLF files: `git cat-file --filters --path=<path> <NAME>_NEW > $WORK/pristine/<file>`;
       - the two new files: `git cat-file blob <NAME>_NEW > $WORK/pristine/<file>` when I7 recorded LF, otherwise
         `--filters`.
       - Then `cmp` each pristine copy with the working-tree file. All must be identical.
    - Any difference is row D22.
- **Other safe stopping points.** At each one the session may end, and the next starts with S2-0's steps 1 to 5, plus
  step 6 if mutations follow:
  - after I1b (the start evidence is frozen; no production edit yet; the next step is I2);
  - after I7 (the planned hand-over);
  - between two I8 entries (each finished entry has its `restored` line; the next step is the next id);
  - before I9 (every mutation entry is closed; the next step is I9).
- **What the hand-over state guarantees.**
  - Nothing is half-edited: every production and test edit of the finished steps is complete, and the CRLF check
    passes.
  - No mutation is in place: the I8 clean check passes.
  - No Gradle run is in flight.
  - The implementation log's last entry is a block giving: `Session <n> ends at <clock>`; the last finished step; the
    next step; `START`; the seven blobs; `PROD_DIFF`; the counts so far.

### I0 — Baseline (no edit before this passes)

1. One call into `$WORK/logs/I0-baseline.log`, sanitised to `$E/I0-baseline.txt`:
   ```bash
   date -u +%Y-%m-%dT%H:%M:%SZ; git rev-parse HEAD; git rev-parse --abbrev-ref HEAD
   git merge-base --is-ancestor c9770c303690a95b6200e82c73316ba8ce2bb4e8 HEAD; echo "descendant-exit=$?"
   git status --porcelain -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat README.md; echo "porcelain-end"
   git log --oneline c9770c30..HEAD -- $BW $RW $BIF $BQB $DTP $MT README.md app/src/test/java/com/zegoggles/smssync/service/BackupWorkerLoopCancellationTest.kt; echo "touch-log-end"
   git log --oneline c9770c30..HEAD -- app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt; echo "u069-end"
   grep -n "minSdkVersion" app/build.gradle; grep -n "junitXml.required" build.gradle
   grep -c -F "currentCoroutineContext().ensureActive()" $BW $RW; grep -c -F "internal suspend fun backupCursors(" $BW
   for a in "                preferences.dataTypePreferences.setMaxSyncedDate(" '                    Log.w(TAG, "BackupWorker: no messages converted")' "        dataTypePreferences.setMaxSyncedDate(type, confirmedMaxDate)"; do printf '%s ' BW; grep -c -x -F "$a" $BW; done
   grep -c -x -F "            val uri = ctx.contentResolver.insert(Consts.SMS_PROVIDER, values)" $RW
   grep -c -x -F "        return performQuery(queryBuilder.buildQueryForDataType(dataType, group, max));" $BIF
   grep -c -x -F "import com.zegoggles.smssync.preferences.DataTypePreferences;" $BQB
   grep -c -x -F "            TextUtils.join(\",\", ids.toArray(new Long[ids.size()])));" $BQB
   grep -c -x -F "    public boolean setMaxSyncedDate(DataType dataType, long max) {" $DTP; grep -c -x -F "    public void clearLastSyncData() {" $DTP
   grep -c -F "as the watermark" $MT; grep -c -x -F "Gmail." README.md; grep -c -F "sync state. All messages on the phone will be backed up on the next run." README.md
   for f in $SBW $LVC $TS/LateVisibleSmsFixtures.kt $TS/LateVisibleSmsRegressionTest.kt $TS/LateVisibleSmsBackupTest.kt $TS/SmsBackupRecordSizeTest.kt $TS/LateVisibleSmsSkipResetRestoreTest.kt $TS/BackupQueryBuilderWindowTest.kt $TP/SmsBackupWindowTest.kt; do test -e $f; echo "exists=$? $f"; done
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"   # C0 (tracked, at START)
   git grep -h "@Ignore" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Ignore"
   git ls-files --others --exclude-standard -- app; git diff --name-only HEAD -- app; echo "foreign-app-end"   # expected: nothing
   grep -v '^#' gradle/gradle-daemon-jvm.properties
   git hash-object $BW $RW $BIF $BQB $DTP $MT README.md
   ```
   Expected:
   - `descendant-exit=0`. Nothing before `porcelain-end`.
   - `touch-log-end` is preceded by U-067's commit(s) only, for `BackupWorker.kt`, `RestoreWorker.kt` and its new test,
     and by no other commit.
   - `u069-end` is preceded by at least one commit.
   - `minSdkVersion 24`. The two U-067 greps print `1`, `2` and `1`.
   - Every anchor count is `1`.
   - Every `exists=1`.
   - Record the count as **"the count at the start of this story"**, `C0`, with the HEAD id, and the `@Ignore` count.
   - Nothing before `foreign-app-end`. The sprint is serial (U-068, then U-069, then this story), so no other story's
     file is uncommitted under `app` at the start. If one is, it is row D1 (iteration 2: the orchestrator may rule
     that it stays; then it is recorded as foreign with its `@Test` count `F0`, and I9 explains it).
   - `toolchainVersion=25`.
   - **Rows:** D1, D2, D6.
2. Gate G (`I0-gates`). Expected `exit=0`, `U063-DAEMON-JVM: java.version=25…`, `failures=0 errors=0`. Record `<n>`
   (Gradle total), `<s>` (skipped, at most 2) and the JaCoCo `service` and `preferences` ratios. **Row:** D1.
3. `$E/I0-mailtransport-classes.txt`: `sha256sum` of
   `app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/zegoggles/smssync/mail/transport/MailTransport.class`
   and the `release/compileReleaseJavaWithJavac` twin, with repository-relative names. **Row:** D13 (a file is
   missing).
4. Create `$W/implementation-log.md` with the I0 entry: the clock, `START`, the count with its commit, `<s>`, `<n>`, the
   JaCoCo ratios, and the XML source (row D2).

### I1 — Tests first, on the start commit (no production edit)

1. Write T1 and T2 exactly as in 4.1 and 4.2, with `EXPECTED_IN_ORDER` empty.
2. TR(`I1-red`, `…LateVisibleSmsRegressionTest`). Expected: three cases, all failing with an `AssertionError`:
   - R1 and R2: the message contains `SMS queries of the run:` with `[date > 2000 AND type <> 3]` (R1) or
     `[date > 1100000 AND type <> 3]` (R2), `returned=[]`, and "A not uploaded";
   - R3: `captured in-order sequence:` and the file `app/build/u070/ac6-in-order-capture.txt` exists.
3. Save sanitised:
   - `$E/I1-red-message.txt`, the failure messages, its first line `START=<id>`;
   - `$E/I1-ac6-capture.txt`, a copy of the capture file;
   - the cross-check:
     `for m in a47de1477df7a1f56e069f77b15f2cf9 679273385a77bd897b6bcb3e625fb929 c8d7686d3b767f1559fb59169aa79b09 cb725351ed0002dc94fe152f10504f8e e32b0d35778fa5403a48c10cc5087d80; do printf '%s ' $m; grep -c "$m" app/build/u070/ac6-in-order-capture.txt; done`,
     each `1` (L22).
4. **Rows:** D3 (N1), D4, D5 (N4).

### I1b — Literal confirmed on the start commit

1. Paste the capture file's lines, unchanged, between `listOf<String>(` and `)` in T2. This is the only edit.
2. TR(`I1-start-confirm`, Regression). Expected: R3 passes, and R1 and R2 fail exactly as at I1. Record in
   `$E/I1-start-confirm.txt`: `START`, the three results, and `git hash-object $TS/LateVisibleSmsRegressionTest.kt`.
   This proves "literal recorded on the start commit" (AC-6) and "fails on the start commit" (AC-1).
3. **Row:** D4.

### I2 — The record layer (P1, P2) and T5

1. Apply P1 (Write), then P2a, P2b and P2c. Write T5.
2. Run the CRLF check on `$DTP`.
3. TR(`I2`, `…preferences.SmsBackupWindowTest`, `…preferences.PreferencesTest`). Expected: 6 + 3 pass. W2's vectors match
   L21. **Rows:** D7, D8.

### I3 — The queries (P3) and T6

1. Apply P3a and P3b. Write T6.
2. `$E/I3-hunks.txt` = `git diff -U0 HEAD -- $BQB | grep '^@@'`. Expected: two hunks, old ranges `-13,0` and `-163,0`
   (the forward query and the MMS and call-log code are untouched).
3. TR(`I3`, `…BackupQueryBuilderWindowTest`, `…BackupQueryBuilderTest`). Expected: 3 + 8 pass. **Rows:** D7, D12.

### I4 — Fetch layer and worker (P4, P5, P6); T3, T4

1. Apply P4 (Write), P5a and P5b, then P6a, P6b and P6c. Run the CRLF check on `$BIF` and `$BW`.
2. TR(`I4-existing`, Regression plus the backup part of PIN: `BackupItemsFetcherTest`, `BulkFetcherTest`,
   `BackupCursorsTest`, `BackupWorkerWatermarkTest`, `BackupWorkerIntegrationTest`, `BackupWorkerTest`,
   `BackupWorkerLoopCancellationTest`, `BackupQueryBuilderTest`). Expected:
   - all pass;
   - R1 and R2 are now green;
   - R3 is green with the **start** literal unchanged (AC-6);
   - `git diff --stat HEAD -- app/src/test` shows no tracked test file.
3. Write T3 and T4. TR(`I4-new`, `…LateVisibleSmsBackupTest`, `…SmsBackupRecordSizeTest`). Expected 26 + 1 pass,
   including B3 as corrected in iteration 2 and the race test B26. Record the Z1 duration from the XML (`time=`).
4. **Rows:** D7 to D10, D14.

### I5 — Restore (P7); T7

1. Apply P7 and run the CRLF check. Write T7.
2. TR(`I5`, `…LateVisibleSmsSkipResetRestoreTest`, `RestoreWorkerIntegrationTest`, `RestoreWorkerCheckpointTest`,
   `RestoreWorkerTest`). Expected: 11 pass, and all restore cases pass. **Rows:** D7, D11.

### I6 — Documents (P8, P9)

1. Apply P8, P9a and P9b. Run the CRLF check on `$MT` and `README.md`.
2. `$E/I6-docs.txt`:
   ```bash
   git diff -U0 HEAD -- $MT | grep '^@@'                                              # @@ -74,4 +74,4 @@
   git diff -U0 HEAD -- $MT | grep -E '^[-+]' | grep -vE '^(\+\+\+|---) ' | grep -vcE '^[-+]     \* '; echo "mt-non-comment-end"   # 0
   git diff -U0 HEAD -- README.md | grep '^@@'                                         # @@ -87 +87,4 @@ and @@ -276,0 +280,6 @@ (plan-review O-7, measured on the applied edits)
   ```
3. **Row:** D13.

### I7 — Pre-mutation run and pristine copies

1. TR(`I7`, NEW5 + SIZE + PIN). Expected: every case passes. Cases go to `$E/I7-cases.txt`.
2. `cp $SBW $LVC $DTP $BQB $BIF $BW $RW "$WORK/pristine/"`. Run `git hash-object -w` on the seven files and record each
   as `<NAME>_NEW`.
3. Record the endings of the two new files: `grep -c $'\r$' $SBW $LVC` against `wc -l`. A CR count of 0 means LF, and
   their recovery uses `git cat-file blob`. Equal counts mean CRLF, and recovery uses `--filters`.
4. Record `PROD_DIFF` = `git diff HEAD -- app/src/main | git hash-object --stdin`.
5. Write the hand-over block (5.1). **This is the end of session 1.** The next step is S2-0.
6. **Row:** D14.

### I8 — Mutations (AC-24; write-ahead and restore protocol; session 2)

`$E/mutation-log.md` starts with a header containing:
- the title;
- "Protocol: U-070 plan section I8 (U-066 AC-9 form: production change uncommitted; `git hash-object` of every changed
  production file recorded first; one Edit, one targeted run, one restore by copying the pristine file, hash, diff-hash
  and porcelain checks, then a `restored` line; never present when a gate, a review or a commit runs). Repository-relative
  paths.";
- `START`, the seven `<NAME>_NEW` blobs, `PROD_DIFF`;
- the **recovery note**: "If a session is lost while an entry says `APPLYING` and has no `restored` line, nothing else
  runs until the file's hash has been compared with its `<NAME>_NEW` and, if it differs, the file has been restored from
  its pristine copy. If that copy is lost, restore by blob:
  - tracked CRLF files: `git cat-file --filters --path=<path> <blob> > <path>`;
  - new files: `git cat-file blob <blob> > <path>` when I7 recorded LF, or the `--filters` form when it recorded CRLF.

  Then check that `git hash-object` equals the blob. `git checkout` is never used: it would discard the change."

**Clean check** (before and after each entry):
- the seven hashes equal `<NAME>_NEW`;
- `git diff HEAD -- app/src/main | git hash-object --stdin` equals `PROD_DIFF`;
- `git status --porcelain -- app/src/main` lists exactly this story's six ` M` files (`MailTransport.java`,
  `DataTypePreferences.java`, `BackupItemsFetcher.java`, `BackupQueryBuilder.java`, `BackupWorker.kt`,
  `RestoreWorker.kt`) and its two `??` files.
- Iteration 2: any other path under `app/src/main` (not this story's) is recorded and is row D1. With a serial sprint
  none is expected.

**Each entry:**
1. Clean check.
2. Write-ahead `### <id> APPLYING <clock>`: the file, its hash, the edit, the class set and the **expected failing set**
   (copied from the list below).
3. One Edit; Read the lines first.
4. One TR(`<id>`, NEW5, plus SIZE for M-L13a and M-L13b), with cases into `$M/<id>-cases.txt` and messages into
   `$M/<id>-message.txt`.
5. Restore at once with `cp "$WORK/pristine/<file>" <path>`.
6. Clean check, then append:
   - `run_exit`, `cases`, `failed` (the ids), `passed`;
   - `expected: matches | does not match` (the observed failing set compared with the listed set **as sets**);
   - `verdict`;
   - `restored: <clock> hash-ok diff-hash-ok porcelain-ok`.
7. The session may end here (5.1); the next entry starts with step 1.

**The mutations: 26 ids, 37 runs** (iteration 2: M-L22 to M-L26 added; M-L19 rewritten for the new fetch code).
- Each gives the file, the exact `old_string` → `new_string`, and the **complete failing set** over the 49 NEW5 cases
  (plus Z1 where SIZE is included).
- The sets were derived test by test from the test text of section 4 and the production text of section 3, as they
  stand in iteration 2. Every NEW5 test not listed must pass.
- Every Java `old_string` was checked unique, and every Java mutant compiled, in the planner's scratch copy (L24).
- Story table rows are given in brackets.

- **M-L1** [no look-back] — `$BIF`: `        final Cursor window = performQuery(queryBuilder.buildSmsWindowQuery(group, watermark));` →
  `        final Cursor window = emptyCursor();`.
  Fails (23): R1, R2, B1, B2, B3, B4, B5, B7, B9, B10, B11, B12, B13, B14, B18, B21, B23, B24, B25, B26, S2, S9, S11.
- **M-L2 (i)** [look-back unbounded] — `$BQB`: `            String.valueOf(watermark - SmsBackupWindow.LOOK_BACK_MS),` →
  `            String.valueOf(Long.MIN_VALUE),`. The window and the seed share `windowArgs`, so the seed takes rows older
  than W, the commit prunes them, and the window returns them unrecorded.
  Fails (8): B4, B6, B18, B21, B22, S1, Q1, Q3.
- **M-L2 (ii)** — same `old_string` → `            String.valueOf(watermark - 10 * SmsBackupWindow.LOOK_BACK_MS),`.
  Fails (8): B4, B6, B18, B21, B22, S1, Q1, Q3.
- **M-L3 (3 h)** — `$SBW`: `    public static final long LOOK_BACK_MS = 21_600_000L;` → `    public static final long LOOK_BACK_MS = 10_800_000L;`.
  Fails (9): B1 (the row exactly 3 h below falls outside), B4 (window arguments), B5, B14 (5 h below), B22, B26 (its
  late row is 3 h below), W1, Q1, Q3.
- **M-L3 (7 h)** — same `old_string` → `    public static final long LOOK_BACK_MS = 25_200_000L;`.
  Fails (7): B4, B6, B21, B22, W1, Q1, Q3.
- **M-L4 (a)** [inclusive bound; the provider double evaluates `>=`] — `$BQB`: `                "%s > ? AND %s <= ? AND %s <> ? %s",` →
  `                "%s >= ? AND %s <= ? AND %s <> ? %s",`.
  Fails (8): B4 (its window query no longer matches `isWindowQuery`), B6, B21, B22, B25 (window count 0), S1 (the
  row exactly W below is returned but was not seeded), Q1, Q2.
- **M-L4 (b)** [shifted by 1 ms] — `$BQB`: `            String.valueOf(watermark - SmsBackupWindow.LOOK_BACK_MS),` →
  `            String.valueOf(watermark - SmsBackupWindow.LOOK_BACK_MS + 1),`.
  Fails (4): B4, B5, Q1, Q3.
- **M-L5** [window rows not filtered against the record] — `$BIF`: `            if (isRecorded(record, SmsBackupWindow.entryOf(window))) {` →
  `            if (false) {`.
  Fails (33): R1, R2, R3, B1, B2, B3, B4, B5, B7, B8, B9, B10, B11, B12, B13, B14, B16, B18, B21, B22, B23, B24, B25, B26,
  S1, S2, S5, S6, S7, S8, S9, S10, S11.
- **M-L6** [key includes the type] — `$SBW`: `                row.get(Telephony.TextBasedSmsColumns.BODY));` →
  `                row.get(Telephony.TextBasedSmsColumns.BODY) + row.get(Telephony.TextBasedSmsColumns.TYPE));`. The seed
  projection has no type column, and W4's map has no type key.
  Fails (7): B8, B18, B21, S1, S2, S6, W4.
- **M-L7** [key uses `_id`] — same `old_string` → `                row.get(Telephony.TextBasedSmsColumns.BODY) + row.get("_id"));`.
  A restore's values, the seed projection and W4's map carry no `_id`.
  Fails (12): B18, B21, B26, S1, S2, S6, S7, S8, S9, S10, S11, W4.
- **M-L8 (a)** [look-back applied to MMS] — `$BIF`: `        if (dataType == DataType.SMS) {` →
  `        if (dataType == DataType.SMS || dataType == DataType.MMS) {`. Only B19 has an MMS watermark above 0 in a
  fetch.
  Fails (1): B19.
- **M-L8 (b)** [watermark guard applied to MMS] — `$DTP`: `        if (dataType != DataType.SMS) {` →
  `        if (dataType == DataType.CALLLOG) {`.
  Fails (1): B20.
- **M-L9 (a)** [draft exclusion dropped] — `$BQB`, two lines:
  `            String.valueOf(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT)` / `        };` →
  `            String.valueOf(-1)` / `        };`.
  Fails (3): B4, Q1, Q3.
- **M-L9 (b)** [group clause dropped] — `$BQB`, two lines: `                    groupSelection(SMS, groupIds)).trim(),` /
  `            windowArgs(watermark),` → `                    "").trim(),` / `            windowArgs(watermark),`.
  Fails (2): B4, Q2.
- **M-L10** [watermark moved backwards] — `$DTP`: `            final long watermark = Math.max(stored, confirmedMaxDate);` →
  `            final long watermark = confirmedMaxDate;`.
  Fails (7): R1, R2, B1 (its final watermark assertion), B14, B15, B21 (the lowered watermark brings older rows into
  the idle run's window), W4.
- **M-L11** [record written before the confirmation] — `$BW`: `        val confirmedMaxDate: Long = transport.appendMessages(folder, result)` →
  two lines: `        dataTypePreferences.setMaxSyncedDateAfterAppend(type, DataType.Defaults.MAX_SYNCED_DATE, result.mapList)` /
  `        val confirmedMaxDate: Long = transport.appendMessages(folder, result)`. It is placed after `openFolder`, so B9
  and B11 (which fail before it) pass.
  Fails (3): B10, B12, B13.
- **M-L12 (a)** [separate commit] — `$DTP`, two lines:
  `            final SharedPreferences.Editor editor = sharedPreferences.edit()` /
  `                .putLong(dataType.maxSyncedPreference, watermark);` → two lines:
  `            sharedPreferences.edit().putLong(dataType.maxSyncedPreference, watermark).commit();` /
  `            final SharedPreferences.Editor editor = sharedPreferences.edit();`.
  Fails (2): B13, W4.
- **M-L12 (b)** [`apply()`] — `$DTP`, three lines:
  `                editor.putInt(SmsBackupWindow.MARKER_KEY, SmsBackupWindow.RECORD_VERSION);` / `            }` /
  `            return editor.commit();` → the same first two lines, then `            editor.apply();` /
  `            return true;`.
  Fails (2): B13, W4.
- **M-L13 (a)** [pruning removed; SIZE included] — `$SBW`: `        if (watermark <= 0) return;` → `        if (true) return;`.
  Fails (4): B22, W3, W6, Z1.
- **M-L13 (b)** [pruning 1 ms too wide; SIZE included] — `$SBW`: `        final long bound = watermark - LOOK_BACK_MS;` →
  `        final long bound = watermark - LOOK_BACK_MS + 1;`.
  Fails (3): B22, W3, W6. Z1 passes.
- **M-L14 (a)** [Reset leaves the record] — `$DTP`, two lines: `            editor.remove(SmsBackupWindow.RECORD_KEY);` /
  `            editor.remove(SmsBackupWindow.MARKER_KEY);` → `            editor.remove(SmsBackupWindow.MARKER_KEY);`.
  Fails (2): S5, W5.
- **M-L14 (b)** [Reset leaves the marker] — same two lines → `            editor.remove(SmsBackupWindow.RECORD_KEY);`.
  Fails (2): S5, W5.
- **M-L15** [Skip seeding removed] — `$BIF`: `                ? queryEntries(queryBuilder.buildSmsSeedQuery(newest))` →
  `                ? new HashSet<String>()`.
  Fails (3): S1, S2, S6.
- **M-L16 (a)** [restore write-ahead removed] — `$RW`: `            preferences.dataTypePreferences.recordRestoredSms(values)` →
  (the line deleted; its comment stays).
  Fails (3): S7, S8, S9.
- **M-L16 (b)** [restore write made after the insert] — `$RW`, two lines:
  `            preferences.dataTypePreferences.recordRestoredSms(values)` /
  `            val uri = ctx.contentResolver.insert(Consts.SMS_PROVIDER, values)` → the same two lines in the other order.
  Fails (1): S8.
- **M-L17** [forward rows not filtered] — `$BIF`:
  `                if (entriesAbove > 0 && isRecorded(record, SmsBackupWindow.entryOf(forward))) continue;` →
  `                if (false) continue;`.
  Fails (2): S8, B26.
- **M-L18 (a)** [upgrade seeding removed] — `$BIF`: `        if (!preferences.hasSmsBackupRecordMarker()) {` →
  `        if (false) {`.
  Fails (3): B18, B21, B24.
- **M-L18 (b)** [marker never seen, so every run seeds] — `$DTP`:
  `        return sharedPreferences.contains(SmsBackupWindow.MARKER_KEY);` → `        return false;`.
  Fails (25): R1, R2, B1, B2, B3, B4, B5, B7, B9, B10, B11, B12, B13, B14, B18, B21, B23, B24, B25, B26, S2, S9, S11,
  W4, W5.
- **M-L19 (i)** [late rows not counted against the limit] — `$BIF`:
  `            remaining = max > 0 ? max - Math.min(lateBefore, max) : max;` → `            remaining = max;`.
  Fails (1): B3.
- **M-L19 (ii)** [already-backed-up window rows counted] — `$BIF`:
  `                if (!isRecorded(before, SmsBackupWindow.entryOf(window))) lateBefore++;` → `                lateBefore++;`.
  Fails (3): B3, B16 (its second run's two recorded rows use the limit), S5 (the same after the Reset).
- **M-L20** [rows the converter rejects not recorded] — `$BW`: `                    if (cursor.type == DataType.SMS) {` →
  `                    if (false) {`.
  Fails (2): B17, B23.
- **M-L21** [look-back while the watermark is -1] — `$BIF`: `                if (watermark > 0) {` →
  `                if (watermark != 0) {`.
  Fails (1): B16.
- **M-L22** (iteration 2) [Skip's SMS branch also taken for another type] — `$BW`: `                if (type == DataType.SMS) {` →
  `                if (type == DataType.SMS || type == DataType.MMS) {`.
  Fails (1): S3 (Skip over a type set without SMS then sets the SMS watermark).
- **M-L23** (iteration 2) [Skip no longer sets the watermark outright] — `$DTP`:
  `                .putLong(DataType.SMS.maxSyncedPreference, watermark)` →
  `                .putLong(DataType.SMS.maxSyncedPreference, Math.max(watermark, storedSmsWatermark()))`.
  Fails (1): S4.
- **M-L24** (iteration 2) [digest over a charset encoding again] — `$SBW`: `            final byte[] hash = sha256.digest(units);` →
  `            final byte[] hash = sha256.digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));`.
  Fails (1): W2. Every other test uses keys computed one way, which stay consistent.
- **M-L25** (iteration 2) [restore write keeps entries no window can reach] — `$DTP`:
  `            SmsBackupWindow.prune(record, stored);` → (the line deleted).
  Fails (1): W6.
- **M-L26** (iteration 2) [record read once, before the queries: contract-verification O-1] — `$BIF`:
  `        final Set<String> record = preferences.getSmsBackupRecord();` → `        final Set<String> record = before;`.
  Fails (1): B26.

**Coverage of the table** (Record 18).
- Every mutation id has a non-empty failing set.
- Every one of the 50 tests is in at least one set; 4.8's "Bitten by" is the transpose of this list.
- The story's table rows map as follows:
  - M-L19 "AC-2 (b), (c)" is met by B3 (AC-2 (c)) for both variants. AC-2 (b)'s MMS budget is the cursor count, which
    `BulkFetcher` reduces whatever the forward `LIMIT` is, so no single edit of the budget can make B2 fail (plan-review
    B-2; ruled). B2 is bitten by M-L1, M-L5 and M-L18 (b).
  - M-L1's story set "AC-3 late-in-group case" is B4.
  - M-L2's "window-query text test" is Q1 and Q3.

The order is M-L1 to M-L26 as listed. At the end, a summary table under the header: id, file, failing set observed,
`expected: matches`, verdict. **Rows:** D15, D16.

### I9 — Final gate, counts and file checks (session 2)

1. The I8 clean check passes, and every entry has its `restored` line.
2. Gate G (`I9-gates`). Expected:
   - `exit=0`, `failures=0 errors=0 skipped=<s>` (not above the start, and at most 2);
   - `tests=<n> + 50 + Fd`, where `Fd` is the change in foreign test cases since I0 (step 4; normally 0);
   - JaCoCo passes, with the `service` and `preferences` ratios recorded beside I0's.
   - Then `$E/I9-mailtransport-classes.txt` as at I0, and
     `diff $E/I0-mailtransport-classes.txt $E/I9-mailtransport-classes.txt; echo "mt-classes-diff-exit=$?"`, which
     prints 0.
   - **Rows:** D13, D17.
3. `$E/I9-checks.txt`. Iteration 2: every check is scoped to `OWN` and `MINE` (5.0), with the U-068 scoping and its
   three binding corrections:
   ```bash
   # Counts, this story's files only (REQ-MODERNIZATION-015 AC-13 as amended by CR-016; plan-review O-6)
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"            # C_head: C0, unless others committed tests since START (listed below)
   git grep --untracked -h "@Test" -- $MINE | grep -c "@Test"                                               # 50
   for f in $MINE; do printf '%s ' $f; grep -c "@Test" $f; done                                             # fixtures 0, 3, 26, 1, 11, 3, 6
   git log --format='%h %s' $START..HEAD -- 'app/src/test'; echo "foreign-test-commits-end"                 # each listed with its commit id; not this story's
   git ls-files --others --exclude-standard -- app/src/test | grep -v -F -x -f <(printf '%s\n' $MINE); echo "foreign-untracked-tests-end"
   git diff --name-only HEAD -- app/src/test; echo "foreign-modified-tests-end"                             # nothing (no tracked test edited by this story)
   # this story's own files: the changed paths, limited to OWN and $W (U-068 correction 1)
   { git diff --name-only HEAD -- $OWN; git ls-files --others --exclude-standard -- $OWN $W; } | LC_ALL=C sort -u
   git diff -U0 HEAD -- $BQB | grep '^@@'                                                 # -13,0 and -163,0 only (AC-18)
   git diff -U0 HEAD -- $DTP | grep '^@@'                                                 # imports; after :70; :79-85 (no hunk over :59-66)
   git grep -n -E "21600000|21_600_000|6 \* 60 \* 60" -- app/src/main                     # one line, SmsBackupWindow.java (AC-4)
   git grep -n -E "max_synced|MaxSyncedDate|maxSyncedPreference|clearLastSyncData|getMostRecentSyncedDate" -- app/src/main   # IC-1 table
   git grep -n "BulkFetcher(" -- app/src/main                                             # BackupWorker.kt, two sites (IC-2)
   git diff --quiet HEAD -- $S/activity/fragments/AdvancedSettings.java $S/activity/StatusPreference.java $S/activity/fragments/MainSettings.java $S/preferences/Preferences.java; echo "display-exit=$?"   # 0 (AC-11, IC-1)
   git diff --stat $START -- k9mail-vendored settings.gradle gradle app/lint-baseline.xml app/src/main/AndroidManifest.xml gradle/verification-metadata.xml; echo "build-diff-end"   # nothing from this story (AC-25); a commit by another story is listed, not counted
   git diff HEAD -- $OWN | grep -E '^\+' | grep -c -E 'import com\.fsck|k9mail'; echo "k9-imports-end"   # 0 (AC-25)
   git grep -n "@Ignore\|Assume" -- $MINE; echo "mine-ignore-exit=$?"                     # exit 1 (none)
   # no code commit by anyone else on this story's paths (U-068 correction 2: scoped to this story's paths only)
   git log --format='%h %s' $START..HEAD -- $OWN; echo "own-path-commits-end"            # nothing
   # design, requirement, contract and change-record directories (contract-verification O-3)
   git status --porcelain -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records; echo "records-status-end"
   git log --format='%h %s' $START..HEAD -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records; echo "records-log-end"
   ```
   **Expected and how it is read:**
   - `C_head` equals `C0` plus the tests that `foreign-test-commits-end` lists. Those are not this story's and are
     listed with their commit ids.
   - The end count is **`C0` + 50**, taken as `C_head` + `MINE` minus the foreign commits' tests, over the working tree
     before the orchestrator's commit (`git grep --untracked` over this story's files, said so). After the commit it is
     `git grep … HEAD`, and equals the same figure.
   - `foreign-untracked-tests-end` and `foreign-modified-tests-end` list any other story's uncommitted test files. They
     are recorded as "not this story's". Their `@Test` count, `F1`, is recorded. The Gradle total of step 2 is
     explained with `Fd = F1 − F0`. None is counted as this story's.
   - The own-files list is exactly the 16 paths of `OWN` plus this story's own files in `$W` (the implementation log and
     `evidence/`). A file in `$W` that the developer did not write, for example a gate report, is recorded and is not a
     stop.
   - `own-path-commits-end` and `records-status-end` are preceded by nothing. `records-log-end` may list document
     commits by the orchestrator: these are recorded as not this story's. The check shows that no file of this story's
     diff lies under those directories, because `OWN` holds none.
   - A change outside this story's paths (`sprint-024/**`, other stories' files under `app`, document commits) is
     recorded with its path and is never a stop. **Row:** D17.
4. `Fd` for step 2: run `git grep --untracked -h "@Test" -- <foreign untracked test files> | grep -c "@Test"` at I0
   (`F0`, normally 0) and here (`F1`).

### I10 — Implementation log and hand-off

For each step, log the clock, a command summary (repository-relative; `$WORK` named only as "the work folder"), the
result line and any decision row used. Close with:
- **Counts:** the 50 tests by name, relative to "the count at the start of this story" (`C0` with its commit); none
  deleted or edited. Any test added by another story between `START` and the end is listed with its commit id and not
  counted. Any foreign uncommitted test file is listed as "not this story's", with its count.
- **Declared items:**
  - the upgrade treatment (2.3);
  - the largest expected size, and the cumulative write cost (2.6);
  - the departures (2.2, items 1 to 7);
  - the two stated differences for the next requirements pass (2.8 items 4 and 5), in these words: "rows the converter
    rejects are recorded and are not looked at again, also if the contact-group rule is changed later" and "rows the
    SQL contact-group clause excludes are never recorded, so widening the group uploads the matching SMS of the last
    6 hours, which is the opposite of the rows the converter rejects";
  - the AC-22 scope (Record 18: the lines this story adds or changes; the two existing VERBOSE lines go to a separate
    bug);
  - the privacy statement and the key-stretching decision (2.5);
  - the `MailTransport` Javadoc outcome;
  - IC-4 "not run" unless the orchestrator runs it;
  - the hand-over blocks of the two sessions (5.1).
- **"Runtime Claims"**: only what the evidence files show.
- Last line: "Hygiene scan: step I11, result in `evidence/I11-scan.txt`".

### I11 — Evidence-hygiene scan (last)

The U-067 I10 form, with the code diff and the new files:
```bash
P='[a-z0-9._%+-]+@[a-z0-9-]+\.[a-z]{2,}|Users[/\\]|App''Data'
L=$WORK/logs
git diff HEAD -- app/src k9mail-vendored README.md > $L/I11-code.diff; cat $SBW $LVC $TS/LateVisibleSms*.kt $TS/SmsBackupRecordSizeTest.kt $TS/BackupQueryBuilderWindowTest.kt $TP/SmsBackupWindowTest.kt >> $L/I11-code.diff
{
  grep -rlEi --exclude=compiled-pipeline.json "$P" $W $L/I11-code.diff > $L/I11-pattern-files.log
  echo "pattern-scan exit=$? files=$(wc -l < $L/I11-pattern-files.log)"
  for label in user-name host-name; do
    case $label in user-name) v="${USERNAME:-$USER}" ;; host-name) v="${COMPUTERNAME:-$(hostname)}" ;; esac
    if [ ${#v} -lt 3 ]; then echo "$label-scan value-unavailable"; continue; fi
    grep -rliF --exclude=compiled-pipeline.json -e "$v" $W $L/I11-code.diff > $L/I11-$label-files.log
    echo "$label-scan exit=$? files=$(wc -l < $L/I11-$label-files.log)"
  done
} > $L/I11-scan.log 2>&1
```
Expected: every scan `exit=1 files=0`. Sanitise into `$E/I11-scan.txt`. The test texts write the at sign as "(at)"
precisely so the AC-6 literal and messages pass this scan. **Row:** D18.

## 6. Criterion-to-Step Map

| Criterion | Where shown |
|-----------|-------------|
| AC-1 | R1, R2: red at I1/I1b on `START`, green at I4; M-L1 |
| AC-2 | B1, B2, B3 (B3 corrected in iteration 2); M-L1, M-L5, M-L19 (B3) |
| AC-3 | B4, Q2; M-L9 |
| AC-4 | W1, Q1; I9 `git grep` (one definition); the comment in P1; M-L3 |
| AC-5 | B5, B6; M-L2, M-L3, M-L4 |
| AC-6 | R3 (literal from `START`, I1b; green at I4 unchanged); PIN unedited (I4, I9 `tracked-test-diff-end`) |
| AC-7, AC-8 | B7, B8; M-L5, M-L6 |
| AC-9 | B9 to B12; `BackupWorkerWatermarkTest` unedited; M-L11 |
| AC-10 | B13 (death after confirmation; one commit carrying both, no apply); W4; M-L12; 2.7 rows 5 and 6 |
| AC-11 | B14, B15, S4; I9 `display-exit=0`; M-L10 |
| AC-12 | S1, S2, S3; M-L15, M-L22 |
| AC-13 | S5, S6, W5; M-L14 |
| AC-14 | S7 (doWork), S8, S9, W6, and (iteration 2) B26 for a restore running at the same time; restore PIN unedited (d); M-L16, M-L17, M-L25, M-L26 |
| AC-15 | B16, B17; existing first-run tests unedited; M-L21 |
| AC-16 | B18 (literal list); M-L18; 2.3 "Upgrade" |
| AC-17 | S10, S11 (descriptor parsed); M-L5, M-L7 |
| AC-18 | B19, B20; `BackupQueryBuilderTest:56-87` unedited; I3/I9 hunks; M-L8 |
| AC-19 | B21, R3 (calls); 2.6; M-L2, M-L3 |
| AC-20 | Z1, B22, W3; 2.6 Size; M-L13 |
| AC-21 | B23, B17; M-L20 |
| AC-22 | B24, W2 (with the code-unit vectors); 2.5; M-L24 |
| AC-23 | 2.8; PIN unedited; S2 and S8 are the two deliberate changes |
| AC-24 | I8 `mutation-log.md`: 26 ids, 37 runs, exact failing sets; every test bitten, every id detected (Record 18) |
| AC-25 | I9 checks (build, manifest, metadata, lint baseline, k9 imports, port: comment only) |
| AC-26 | P8, P9; I6, I9 class bytes |
| AC-27 | I0 and I9 gates; counts (start with commit; end `--untracked`); no floor or limit edit; no `@Ignore` |
| IC-1 | Section 9 table and the I9 grep |
| IC-2 | Entry points: `doWork` (R*, B*, S1 to S6, S10, S11), the XOAuth2 retry (B25), `RestoreWorker.doWork` (S7) and the restore loop (S8, S9), `clearLastSyncData` (S5, S6, W5); `BulkFetcher(` grep |
| IC-3 | Signatures unchanged (P6c; constructors untouched); mocked builder null-safe (`BackupItemsFetcherTest` unedited, I4); every `MailTransport` double compiles (gate) |
| IC-4 | Section 10 |

## 7. Decision Table

| Row | Observation | What the developer does |
|-----|-------------|-------------------------|
| D1 | I0: not a descendant of `c9770c30`; a story path dirty; another story's file uncommitted under `app` (`foreign-app-end`), unless the orchestrator rules that it stays, in which case it is recorded as foreign (iteration 2); a commit since `c9770c30` touches this story's files other than U-067's; U-067 not landed (U-067 greps); U-069 not landed (`u069-end` empty); minSdk not 24 (U-068 not landed); toolchain not 25; a new file already exists; or the gate fails | Stop and report with the sanitised evidence. Nothing has been edited |
| D2 | I0: `junitXml.required = true` already in `build.gradle` (U-068 landed) | Keep passing `-I "$INIT"`. It sets the same flag and prints the JVM line. Record "XML: build file (U-068) and init script". If false: "XML: init script" |
| D3 | I1: R1/R2 fail with an exception from conversion (not an assertion), e.g. an NPE in `MessageGenerator` or `PersonLookup` (N1 false) | Fallback fixture, which still proves AC-1 through the real fetch layer. Drive `BulkFetcher(BackupItemsFetcher(resolver, BackupQueryBuilder(prefs.dataTypePreferences))).fetch(...)` and the `internal` `backupCursors` (U-067), with a `MessageConverter` built as in `MessageConverterTest.java:115-121` (mocked `PersonLookup` returning a record whose `getAddress` is the address). Change only the harness's `backup()` body. Log it, and tell the orchestrator before any production edit (story "Real collaborators"). IC-2's `doWork` entry is then shown by B25 and S1 only if they pass; if no route works: stop and report |
| D4 | I1 or I1b: R1/R2 **pass** on `START`, or R3 fails at I1b | Stop and report. The regression test does not discriminate (R1/R2), or the capture is not deterministic (R3: compare the two captures and name the differing field) |
| D5 | I1: no capture file (N4 false) | Take the literal from R3's failure text in the JUnit XML (`<failure>` text after "captured in-order sequence:"). Log the source |
| D6 | I0: an anchor count is not 1, or Edit later reports "not found" or "not unique" | Stop and report. Edits are never applied by line number alone |
| D7 | A new test class does not compile | Correct the **test** text only, keeping every assertion. Log it. If the only cure is a production signature: stop and report |
| D8 | I2: a W2 vector differs | Stop and report: the Java digest differs from the independently computed vector (encoding or length rule). Do not edit the vector to match |
| D9 | I4: a test fails in cursor positioning or column reads (N2) | Diagnose against 2.4. If `MergeCursor`/`AbstractCursor` misbehave only under Robolectric: stop and report. No shadow-specific production code |
| D10 | I4: B25 fails at `Mockito.spy` (N5) | Replace the spy with `Mockito.mock(WorkerEngineFactory::class.java)` whose `createPersonLookup`, `createMessageConverter` and `createCalendarSyncerIfEnabled` answer by delegating to the real instance, and `createTokenRefresher` returns the mock. Test text only; log it |
| D11 | I5: S10/S11 fail at `listed` contains the default file (N3); re-registering the provider does not take effect (N6); or (iteration 2, N7) an exception from `AuthPreferences` or the secret store after the excluded files are cleared | N3: use `listed = listOf(defaultFile)` (the default file is the one that must travel; a record moved elsewhere would still fail S10). N6: register the new provider under `"sms"` after `ShadowContentResolver.reset()`. N7: clear only `pinned_certs.xml` and leave `credentials.xml` (it holds only the three secrets, CNTR-MODERNIZATION-003, none of which the backup path reads). Record in the log, word for word, that S10/S11 then do not show the absence of `credentials.xml`. Test text only; log it. If still failing: stop and report |
| D12 | I3: a hunk overlaps old lines 103-147 | Re-apply P3 exactly. Never edit the forward, MMS or call-log code |
| D13 | I6/I9: a non-comment line in the `MailTransport` diff, or `mt-classes-diff-exit` not 0, or a class file missing at I0 | Restore the file with `git cat-file --filters --path=$MT $(git rev-parse HEAD:$MT) > $MT`. Record "Javadoc left knowingly out of date; AC-26's port sentence not done" and report (ruling SC-3) |
| D14 | A PIN test fails at I4, I5 or I7 | Stop and report: existing behaviour changed. No PIN file is ever edited |
| D15 | I8: a mutation does not compile | Log `invalid` with the compiler line. Use the nearest compiling form with the same intent (for example `if (1 > 2)` for `if (false)`), run as `<id>'`. An invalid entry never counts |
| D16 | I8 (iteration 2: the sets are exact): the observed failing set differs from the listed set | Restore first. **(a) A listed test passes:** log it. If the mutation's id now has no failing test at all, log a **finding** "AC-24 unmet for <id>"; finish the remaining entries, then stop and report before I9. Otherwise log `expected: does not match` with the difference, as a planning error for the reviewers, not a stop. **(b) An unlisted test fails:** log `expected: does not match` with the extra ids and the failure messages, as a deviation for the reviewers, still a detection. Either way the developer never edits a test or a mutation to make the sets agree, and never re-runs a mutation to get a different set. Record 18's rule (every test bitten by at least one mutation, every id detected) is checked over the whole log at the end of I8: an unmet half is a finding and a stop before I9 |
| D17 | I9: gate not 0; failures; skipped above the start or above 2; Gradle total not `<n> + 50 + Fd`; the end count not `C0` + 50 after the foreign commits of `foreign-test-commits-end` are taken out; an own-path check not as expected; a commit since `START` touching `OWN` | Stop and report with the evidence. No floor, `limit` or `minimum` edit; no `@Ignore`. A change outside this story's paths, another story's uncommitted file, or a document commit is recorded with its path and is not a stop |
| D18 | I11: any scan `exit=0` | Re-sanitise or correct the named file and repeat. A hit in a code file: stop and report |
| D19 | The tool moves a Gradle run to the background | Wait for the completion notice; never poll; start nothing else |
| D20 | "bash.exe … not found", or a process killed without a visible reason | Stop and report (endpoint security). Do not work around it |
| D21 | Z1 takes longer than 10 minutes in one run | Not a failure. Record the time. If the 600000 ms tool timeout is hit, report: the criterion fixes 200 × 50 and it is not reduced |
| D22 | S2-0 (resume) or a resume at any safe stopping point: a hash differs from `<NAME>_NEW`; `PROD_DIFF` differs; an `APPLYING` entry has no `restored` line; a `cmp` of a rebuilt pristine copy differs; or a commit since `START` touches `OWN` | For an open `APPLYING` entry: restore by the recovery note first. Then repeat the check. If anything still differs: stop and report, and start no Gradle run. Never re-create a pristine copy from the working tree |
| D23 | I8: B26 passes under M-L26 (the iteration-1 single read) | The race test is not observing the window. Check that `beforeQuery` runs before the provider selects rows. Test text only; log it. If still undetected: finding, as D16 (a) |

## 8. Contracts, Standards, Risks

**CNTR-MODERNIZATION-007** (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md`, the port). Used
as it is: `checkSettings`, `openFolder`, `appendMessages`, `closeFolders`, and for restore `getMessages` and
`importMessageBody`. No signature changes. The returned confirmed date is still the only value taken from the port. P8 is
a Javadoc correction, comment only, proven by diff and class bytes. The contract states no watermark, so no contract
change follows.

**CNTR-MODERNIZATION-004** (`…/CNTR-MODERNIZATION-004-backup-scheduler.md`, INV-4). Read and not changed: no enqueue path
changes. The worker `Result`s and progress keys are unchanged.
- Iteration 2 (contract-verification row 13 and O-1): INV-1 gives one run per unique name, not one run overall. Rule 8
  makes the distinct names deliberate (L23). The plan therefore assumes a backup and a restore, or two backups, can
  overlap, and handles both (2.4, 2.7 "Concurrency").
- Validation Rule 6 (checkpoint after each successful insert, before the cursor advances) is unchanged: the write-ahead
  call sits before the insert, inside the existing `smsExists` branch, and is idempotent on replay.

**CNTR-MODERNIZATION-003** (iteration 2, contract-verification O-4; the backing-store invariant). `credentials.xml` stays
excluded from Android's auto-backup, and no file holding the three secret keys (`login_password`, `oauth2_token`,
`oauth2_refresh_token`) is included. The record keys `sms_backup_record` and `sms_backup_record_version` go into the
default preferences file, which the contract's Note already describes as holding plaintext non-secret user data. Neither
key is a secret. Every new write is `commit()`, and the secret store is untouched. The descriptor and the manifest are
not edited (I9).

**CNTR-MODERNIZATION-002** (iteration 2, O-4). `pinned_certs.xml` stays excluded. It is neither touched nor read. T7's
fresh-store step empties it as a restored phone has it.

`integration_contracts: []` is right. The record is state inside one module, shared by the backup worker, the restore
worker and settings reset. No component boundary is crossed, so the Contract Gate does not apply. This decision is noted
for the story's Implementation Notes (story "Integration Contract References").

**Requirements read.**
- REQ-MODERNIZATION-020: in full.
- REQ-MODERNIZATION-017: Constraints "Behavior preserved" (the story cites it; satisfied by 2.8).
- REQ-MODERNIZATION-015 AC-13 as amended by CR-016: the count rule (I0, I9).
- No REQ of `type: standard` constrains this change; U-066 and U-067 found none and this story adds no such area.

**Risks**

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|------------|--------|------------|
| R1 | The real converter does not run over the provider double (N1) | Low: the generator ran over a `MatrixCursor` in `MessageConverterTest`, and `doWork` already builds it | Medium: fixture rework | D3 fallback keeps the real fetch layer |
| R2 | The AC-6 capture is not deterministic (random `reference_uid`, time zone, `backup-time`) | Low: all three are fixed or excluded (L16) | Medium: R3 unusable | I1b re-runs on `START`; D4 |
| R3 | Mutation runs take long (37 runs) | High | Low: time only | SIZE excluded except for M-L13; foreground; two sessions with safe stopping points (5.1); D19 |
| R4 | A mutation is masked by the defence in depth of another site | Low: one guard per behaviour by design (2.2 item 4). The two record reads are one site each (M-L19 (ii) on the first, M-L5 and M-L26 on the second) | High: an undetected weakness | Exact failing sets; D16 |
| R5 | Per-row commits grow with the record (SC-8) | Low in normal use (about 3 KB) | Low to medium at 5,000 entries (155 KB per commit) | Stated; not tested; for the runtime-library story to measure if wanted |
| R6 | Two backup workers overlap (SC-9; iteration 2, resolved from the code) | Possible: each backup type has its own unique-work name with REPLACE (`WorkManagerScheduler.kt:154`, `:289`, `:324`, `:365`, `:400`), so a manual and an incoming backup can run at once (L23) | Low: both can select the same late or forward row before either commits, so it is uploaded twice, as today for forward rows. No loss: the watermark takes `max()` and the record a set union under `SMS_RECORD_LOCK`, one lock per process, and both workers run in the one app process (no `android:process`) | 2.7 "Concurrency". Mutual exclusion would need a scheduler change (CNTR-MODERNIZATION-004) and is not designed here |
| R7 | The real provider normalises the address or body on a restore insert, so keys differ | Unknown (not observable on the double) | Low: one duplicate per restored row in the window, never a loss | Stated (2.9); a device check could show it |
| R8 | Android 16/17 `SharedPreferencesImpl` differs from Android 10 (L14) | Low | High in principle (atomicity) | The same code path since API 1 is assumed; stated as not verified |
| R9 | A digest collision causes a loss | Negligible: about k·5.4·10⁻²⁰ per late row (2.5) | High for that message | 64-bit digest plus the clear date |
| R9b | Two malformed bodies hash alike through a charset encoding (plan-review O-3) | Removed in iteration 2 | High for that message | Code-unit hashing; W2 vectors 5 to 7; M-L24 |
| R10 | The record is used to recover short message texts and the timing of the last 6 hours | Low (needs the private file: root, a debuggable build, or the backup channel) | Low to medium (a 6-digit code falls to 10⁶ guesses; dates in clear; 6 hours only; the texts are in the mailbox) | 2.5 states it in full; key stretching weighed and rejected (2.1 row 19); for the security review |
| R11 | Robolectric's `CursorWrapper` shadow hides a wrapper defect | Avoided: `AbstractCursor` is used (L12) | — | 2.1 row 12 |
| R12 | Edits change line endings (CRLF) | Low: Edit keeps them | Low: diff noise | CRLF check after each edit; `--filters` recovery |
| R13 | A restore running during a backup uploads a restored row (contract-verification O-1) | Removed in iteration 2 (was low: two workers with distinct names) | Medium: the duplicate REQ AC-16 forbids | Second record read after the queries (2.4); B26; M-L26 |

## 9. IC-1 — Every reader and writer of the SMS watermark (`git grep` at `c9770c30`, line numbers re-read at `2aad7d70`; re-run at I9)

| Hit | Role | Handled |
|-----|------|---------|
| `BackupQueryBuilder.java:112` | the forward query reads it | unchanged; the window and seed read the watermark passed in by the fetcher, which reads it once per fetch |
| `BackupQueryBuilder.java:119`, `:144` | MMS and call-log reads | unchanged (AC-18) |
| `BackupWorker.kt:207` | Skip writes it | now through `skipSms`, then `setSmsWatermarkAndSeed` (outright, as before; S4) |
| `BackupWorker.kt:259-260` | first-run sentinel | unchanged (B17) |
| `BackupWorker.kt:423` (at `2aad7d70`, after U-067) | backup writes it | now `setMaxSyncedDateAfterAppend` (max for SMS; MMS and call log as before) |
| `BackupWorker.kt:71`, `:292`, `:297`, `:388`, `:416` | comments | unchanged |
| `RestoreWorker.kt:466-467` | restore reads and raises it | unchanged; a write-ahead record call is added before the insert |
| `DataTypePreferences.java:59-85` | read, write, `getMostRecentSyncedDate`, `clearLastSyncData` | read and write unchanged; `clearLastSyncData` also removes the record and the marker |
| `Preferences.java:222` | the presence of the key decides `isFirstBackup()` | unchanged; the record keys are separate (B17) |
| `AdvancedSettings.java:230`, `StatusPreference.java:370` | display only | unchanged (`display-exit=0`) |
| `MainSettings.java:82`, `:94` | call `clearLastSyncData` | unchanged |
| `DataType.java:30`, `:41`, `:51`, `:76-78` | key definitions | unchanged |

## 10. IC-4 (optional Android 16 smoke) and the hand-off to the targetSdk 37 story

**IC-4** is the orchestrator's only; no developer step. On `Pixel_9_Pro` (API 36), with a stored watermark and no record,
install the debug build with `adb -s <serial> install -r`, keeping the data, then start a manual backup.
- **It can show:**
  - the upgrade seeding against real preferences and a real provider (the line
    `BackupItemsFetcher: look-back seeded <n> SMS already on the phone (upgrade)`);
  - no re-upload of the window (`starting backup (k messages)` counts only rows above the watermark);
  - one test SMS uploaded once, then `nothing to backup` or `backedUp=0` twice.
- **It cannot show** anything about withholding (Android 16 does not withhold), the late-visible path, Android 17, or any
  physical device.
- **Constraints** (story IC-4):
  - `Pixel_10_Pro` is never touched;
  - only the owner enters the password;
  - the app is never uninstalled;
  - `MSYS_NO_PATHCONV=1` is set only for adb commands with Unix paths;
  - evidence is text only;
  - otherwise "not run".

**For the targetSdk 37 story (S5):**
- (1) REQ-MODERNIZATION-020 AC-25 and BUG-021 AC-12, the API 37 device check of this fix, are that story's QA step after
  S5-I1. BUG-021 moves to review after this story's QA, with AC-12 "not reached" (ruling SC-4).
- (2) D14's Run R reads the `SmsBroadcastReceiver` and `BackupTriggerWorker` lines. The receiver's lines will not appear
  (BUG-023 Observation), and after U-069 the scheduler's line does not identify the receiver. Run R should read
  `BackupTriggerWorker.doWork: content-URI trigger fired; ...`.
- (3) On Android 17 the two new log lines are the observable signal of the look-back: the selection line on the first
  backup after a release.
- (4) If Run T observes a delay above 3 hours, or no visibility by T0 + 6 h, W is revisited by change record. The
  constant is in one place (P1).

## 11. Items for the Orchestrator

1. **No ruling is needed before the build.** Scope Concern 7 is answered: plannable as one (PASS).
2. **AC-21 consequence above the watermark: ruled** (Record 18: confirmed, inside ruling 25). 2.8 item 4 states it as a
   named difference.
3. **AC-22 and the pre-existing debug-build log lines: ruled** (Record 18). AC-22 applies to the lines this story adds
   or changes, checked at DEBUG and above. `PersonLookup.java:76` and `MessageGenerator.java:311` are not changed here;
   the orchestrator raises a separate low-severity bug for them.
4. **Story text corrections for the record** (no plan change):
   - Affected Code says the record is "one `date:digest` per line". It is comma-separated (2.2 item 1).
   - Restore pruning uses the stored watermark (2.2 item 2). The recorded set is identical to the story's rule.
5. **The security review** should read 2.5 (SC-10): the digest and its encoding, what the file reveals (the dates in
   clear, short codes recoverable by trying the code space), why no key is possible under AC-17, and the key-stretching
   decision.
6. **Commit.** The developer commits nothing in either session. After the orchestrator's commit, the HEAD count equals
   the log's end count.
7. **For the next requirements pass** (Record 18; I10), the two stated differences, side by side (2.8 items 4 and 5):
   - rows the converter rejects are recorded and are not looked at again, also if the contact-group rule changes later;
   - rows the SQL contact-group clause excludes are never recorded, so widening the group uploads the matching SMS of the
     last 6 hours. This is the opposite of the first.
8. **For the next CNTR-MODERNIZATION-007 amendment** (contract-verification O-2). The contract's port listing is stale
   against the code:
   - it shows `appendMessages(...): void`, while the port declares `long appendMessages(...)` with the confirmed-date
     contract (`MailTransport.java:84-87`);
   - it does not list `importMessageBody` (`:149`).

   This plan follows the code. Its Javadoc edit (P8) is true against the code. No contract is edited by this story.
9. **For BUG-024** (placed after this story; its Suggested Approach relies on this story's test fixtures).
   - Unchanged from iteration 1: B12 `ac9_cancelledDuringTheAppend_nothingSaved_rowsUploadedByTheNextBackup` (it throws
     `CancellationException` from `appendMessages` while the job is active, inside `runBlocking`), `RecordingTransport`,
     and `ProviderDouble`'s selection evaluator, row operations and insert behaviour.
   - Changed in iteration 2, additively only:
     - `ProviderDouble` gains an optional `beforeQuery: ((String) -> Unit)?`, null by default and called at the start
       of `query`;
     - the KDoc of `SimulatedProcessDeath` is corrected;
     - `LateSmsHarness` is unchanged.
   - Nothing BUG-024 relies on has a different behaviour.

## 12. Constraints for the Developer (binding)

1. Run Gradle in the foreground with a 600000 ms tool timeout on every call. **Never with `MSYS_NO_PATHCONV=1`** (it breaks
   the wrapper's classpath). This story has no adb step.
2. If the tool moves a run to the background, wait for its completion notice. Never poll; start nothing meanwhile.
3. No background or detached shells, no `&`, no `sleep` or polling loops, no helper agents for shell work, no heredocs for
   editing (Write and Edit tools only).
4. On "bash.exe … not found", or a process killed without a visible reason: stop and report (endpoint security).
5. Raw logs, captures and pristine copies stay in the work folder outside the repository. Only sanitised evidence
   (`sanitize.py`) enters `$W`. Never write an e-mail address, host name, user name or user-profile path into any
   repository file.
6. Take timestamps from `date -u +%Y-%m-%dT%H:%M:%SZ`.
7. Commit nothing.
8. Edit production only as P1 to P9 say, plus the logged mutations, each restored and proven by blob hash, diff hash and
   porcelain.
9. No dependency, Gradle, build-file or verification-metadata change. No JaCoCo floor, `limit` or `minimum` edit. No
   `@Ignore`, no `Assume`. No edit to any existing test file.
10. No test text may contain the string "@Test" except the annotations.
11. No new file under `app/` other than P1, P4 and T1 to T7. The capture file under `app/build/` is build output.
12. Leave other work in the checkout alone (iteration 2: the U-068 iteration-2 form of this constraint). That is:
    - `sdlc/.skill-*` (the delegation audit log, `sdlc/.skill-context.json`);
    - `sdlc/artifacts/build/sprints/sprint-024/**` except this story's own files in `$W` (the implementation log and
      `evidence/`). In `$W`, this plan, the plan-gate reports and `compiled-pipeline.json` are not the developer's;
    - `sdlc/artifacts/stories/**` and the decision appendix;
    - any design, requirement, contract or change record; the story file;
    - any other story's changed, uncommitted or untracked file under `app` or elsewhere. Never stash, restore, edit or
      commit someone else's change.

    The orchestrator may commit documents (never code) while you run, and planners and reviewers of other stories write
    under other folders. Changes outside this story's paths (`OWN`, `$W`) are recorded if noticed and are never a reason
    to stop, except as rows D1 and D17 say.
13. No emulator, no device, no `adb`.
14. Two sessions (5.1). A session ends only at a safe stopping point and with the hand-over block written. A new session
    starts with S2-0 (or its steps 1 to 5) and with row D22. Commit nothing in either session.

## 13. Quality Attribute Coverage (ISO/IEC 25010)

| Characteristic | Finding |
|----------------|---------|
| Functional Suitability | **Completeness:** a late-visible SMS within W is uploaded (AC-1). **Correctness:** exactly once, including type changes, restore and a new phone (AC-7, AC-8, AC-14, AC-17). **Appropriateness:** SMS only, as ruled (AC-18). Each criterion has a test and a mutation |
| Performance Efficiency | **Time behaviour:** one extra provider query per run over the last 6 h of rows (tens normally), and one SHA-256 per window row. **Resource utilisation:** about 31 bytes per entry, bounded by the window (Z1). No extra server round trip (B21). The per-commit file size is stated, not measured at stress (SC-8) |
| Compatibility | **Co-existence:** the same preferences file and keys, and MMS and call log untouched. **Interoperability:** the port is unchanged (comment only), there is no server-side assumption, and Android's own backup carries the record (AC-17). **Upgrade compatibility:** seeding (AC-16) |
| Usability | No UI change. Two README sentences tell the owner the 6-hour bound and the Skip consequence (AC-26) |
| Reliability | **Fault tolerance and recoverability:** the BUG-010 order is kept. Watermark and record are in one atomic commit (L14), the restore is write-ahead, and a crash at any point leaves at most one duplicate, never a loss (2.7). The watermark never moves down (AC-11) |
| Security | **Confidentiality:** no address or body in the record or in the new logs (AC-22). What the file does reveal is stated: the dates of the last 6 hours, and short codes recoverable by guessing (2.5). The digest input is one-to-one (code units, length prefixes). Key stretching was weighed and rejected with its cost. The pre-existing debug VERBOSE lines go to a separate bug (Record 18). No permission and no manifest change (AC-25); CNTR-MODERNIZATION-003 and -002 hold |
| Maintainability | **Modifiability:** W is one constant with its derivation. The record format is versioned by the marker. **Testability:** a Fake provider that rejects unknown selections (with an optional hook for a concurrent writer), a Spy transport, a counting Decorator; 26 mutation ids (37 runs) with exact failing sets. **Analysability:** counts-only log lines |
| Portability | **Adaptability:** framework APIs only (`AbstractCursor`, `MergeCursor`, `MessageDigest` SHA-256), present at minSdk 21 and 24; the digest's byte form is written by hand and depends on no charset. No Robolectric-shadow-dependent production code (L12) |

## 14. Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-070 story | `sdlc/artifacts/build/sprints/sprint-024/U-070-late-visible-sms-are-backed-up.md` | Read in full, including the Record 16 rulings |
| REQ-MODERNIZATION-020 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-020-backup-completeness-for-late-visible-messages.md` | Read in full |
| BUG-021 | `sdlc/artifacts/stories/BUG-021-delayed-otp-sms-skipped-by-backup-watermark.md` | Suggested Approach as cited by the story (the story's corrections govern) |
| U-067 plan | `sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md` | Read in full: model, Amendment A1, post-U-067 text |
| U-066 plan | `sdlc/artifacts/build/sprints/sprint-022/U-066/plan.md` | Class-bytes method (`:1490-1541`, `:2006`, `:2034`) |
| U-068, U-069 stories | `sdlc/artifacts/build/sprints/sprint-024/U-068-minsdk-24-floor.md`, `…/U-069-content-trigger-backup-goes-through-the-scheduler.md` | File overlap (AC-9, AC-13, Not touched, `:257`) |
| Contracts | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md`, `…/CNTR-MODERNIZATION-004-backup-scheduler.md` | As cited by the story; the port signatures were read from `MailTransport.java` |
| Production | `app/src/main/java/com/zegoggles/smssync/service/BackupItemsFetcher.java`, `BackupQueryBuilder.java`, `BulkFetcher.java`, `BackupCursors.java`, `BackupConfig.java`, `BackupWorker.kt`, `RestoreWorker.kt` (`:140-262`, `:378-629`, `:755-787`), `WorkerEngineFactory.kt`; `…/preferences/DataTypePreferences.java`, `Preferences.java`, `AuthPreferences.java` (`:200-222`); `…/mail/ConversionResult.java`, `MessageConverter.java`, `MessageGenerator.java`, `HeaderGenerator.java`, `Headers.java`, `PersonLookup.java`, `DataType.java`, `transport/MailTransport.java`, `transport/MessageImportResult.java`, `transport/XOAuth2FailedException.java`; `…/contacts/ContactGroupIds.java`, `ContactGroup.java`, `ContactAccessor.java` (`:73-92`); `…/App.java:91-93`; `Consts.java`; `Defaults.java`; `utils/Sanitizer.java` | Read in full or at the cited lines (HEAD) |
| Resources and build | `app/src/main/AndroidManifest.xml:95-96`, `app/src/main/res/xml/backup_descriptor.xml`, `app/build.gradle:140-170`, `:262-520`, `build.gradle:25-31`, `app/src/test/resources/robolectric.properties` | Backup, floors, test setup |
| Tests | `BackupItemsFetcherTest.java`, `BackupQueryBuilderTest.java`, `BackupWorkerWatermarkTest.kt`, `BackupWorkerIntegrationTest.kt`, `MessageConverterTest.java` (in full); `RestoreWorkerCheckpointTest.kt` (`:1-140`, `:400-595`); `RestoreWorkerIntegrationTest.kt` (`:60-100`, `:375-415`); `MailTransportTestFactories.java` (signatures) | Fixtures, null-safety, the PIN set |
| Vendored library | `k9mail-vendored/src/main/java/com/fsck/k9/mail/Message.java:136`, `internet/MimeMessage.java:402-413`, `internet/TextBody.java`, `Address.java:41-61` | Header and body API for the Spy |
| Compiled libraries | Robolectric `shadows-framework-4.15.1.jar` (`ShadowContentResolver`, `ShadowCursorWrapper`, `ShadowLog`); `android-all-instrumented-10-robolectric-5803371-i7.jar` (`SharedPreferencesImpl`), read with `javap` in the planner's scratch folder | L11 to L14 |
| Tools | `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools/` | Headers of the five tools |
| Coverage | `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml` (2026-11-20) | L17 |
| Standards | plugin cache 1.7.2 `knowledge/standards/expert-exceeding-depth.md`, `truth-and-accuracy.md`, `phase-completion-report.md`; `templates/build-artifacts/build-plan.md` | Depth criteria, the PCR form, frontmatter |
| REQ and DES corpus | `sdlc/artifacts/requirements/modernization/`, `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-…` | REQ-020 in full. REQ-017, REQ-015 AC-13 and DES-014 were not re-read in full: their lines relevant here are quoted by the story, and this plan changes none of them |
| Plan-gate reports (iteration 2) | `sdlc/artifacts/build/sprints/sprint-024/U-070/plan-review.md`, `contract-verification.md`, `expert-depth-check.md` | Read in full; the "Iteration 2" table |
| Story rulings (iteration 2) | the U-070 story, Implementation Notes, Record 18 (both blocks) | Binding rulings |
| U-068 plan and story (iteration 2) | `sdlc/artifacts/build/sprints/sprint-024/U-068/plan.md` (I7 checks `:1150-1165`, counts 5.3, constraint 11); `U-068-minsdk-24-floor.md:298-305` (the three binding corrections) | The scoping of counts, own files and foreign commits |
| BUG-024 | `sdlc/artifacts/stories/BUG-024-cancelled-backup-or-restore-reported-as-failure.md` (as described by the orchestrator: it relies on B12, the provider double and the recording transport) | Section 11 item 9 |
| Scheduler (iteration 2) | `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` (`git grep` of `enqueueUniqueWork`/`ExistingWorkPolicy`); `android:process` search | L23, R6 |
| Scratch compile (iteration 2) | the planner's session scratch folder: HEAD copies with this plan's P1 to P5 applied by script; JDK 25 `javac`; `android-35/android.jar`; `androidx.annotation` 1.2.0 from the Gradle cache | L24; the digest vectors (L21) |

## Verdict

**PASS (iteration 2).** Every finding of the three gates is taken up in place (the "Iteration 2" table), and none is
declined.
- **The blockers:**
  - B3 now backs up its five rows in one first run, as the review's fix says;
  - M-L19 (i) lists only the test it can bite, B3, and B2's "Bitten by" is corrected.
- **Coverage.** The whole test list was checked against the mutation table. The four tests no mutation reached (S3, S4,
  W2, W6) each have one new single-edit mutation (M-L22 to M-L25). Every I8 row now carries its exact failing set,
  derived test by test, and the table is its transpose. There are 26 ids and 37 runs, every id detected and every one
  of the 50 tests bitten.
- **Production changes:**
  - the concurrent-restore duplicate found by contract verification is closed by a second record read after the
    queries, shown by B26 and M-L26;
  - the digest hashes UTF-16 code units, with recomputed vectors that Python and Java agree on.
- **Scratch verification.** The Java of P1 to P5 compiles under `-Werror` in a scratch copy, and all 32 Java mutants
  compile there.
- The run is planned as two sessions, with a defined hand-over and resume.

What follows is the iteration-1 verdict, still true where not superseded above.

The story's facts hold against the code at `c9770c30` and at `2aad7d70`. The design meets every one of its 27 criteria
and 4 integration criteria with a named test or check:
- a window query in the fetch layer, null-safe against the mocked builder;
- a content-keyed, digest-only record in the watermark's own preferences file, bounded by W and with no count cap;
- one atomic commit for the watermark and the record after the confirmed append;
- seeding at upgrade and Skip, a write-ahead entry at restore, clearing at Reset.

Every library or framework fact the story left open is either read from the compiled jars (L11 to L14) or turned into a
probe with an outcome for each result (N1 to N7). In particular:
- Robolectric shadows `CursorWrapper`, which is why the cursor is an `AbstractCursor`;
- `ShadowContentResolver` neither wraps provider exceptions nor answers without a provider;
- `SharedPreferencesImpl` commits all-or-nothing.

The plan departs from the story's Technical Context in seven stated places (2.2). None weakens a criterion. The 50 tests
are given in full. The regression test and the in-order literal are run first on the start commit. The 26 mutation ids
(37 runs) are given with exact text and exact failing sets. No item needs a ruling before the build: section 11 items
2 and 3 are ruled, and items 7 to 9 are hand-overs.

## Phase Completion Report
---
story_id: "U-070"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
