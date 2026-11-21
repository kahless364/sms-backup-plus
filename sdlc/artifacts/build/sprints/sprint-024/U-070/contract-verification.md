---
artifact_type: gate-result
gate: contract-verification
gate_id: "contract-verification"
checkpoint: plan-gate
story_id: "U-070"
verdict: PASS
iteration: 1
agent: "Solution Architect (separate instance)"
timestamp: "2026-11-21T11:23:11Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md"
base_commit: "36e715e71c37bc3c908d5bc03edce3d782a5d076"
contracts_checked:
  - CNTR-MODERNIZATION-007 (v2; the port, Validation Rule 1, Breaking change policy, Backward compatibility, Notes)
  - CNTR-MODERNIZATION-004 (v3; INV-1 to INV-4, Validation Rules 6, 7, 8, Error Handling, Versioning)
  - CNTR-MODERNIZATION-003 (v1; Backing-store invariant, the three secret keys, Versioning)
  - CNTR-MODERNIZATION-002 (pinned_certs file and its backup exclusion; searched, not touched)
  - CNTR-MODERNIZATION-001, -005, -006, -008 (searched for watermark, backup descriptor, restore, Message-ID, X-smssync; nothing in the plan reaches a site they bind)
---

# Gate Report: gate-result — U-070

## Gate: contract-verification @ plan-gate

## Verdict: PASS

The plan was written at `c9770c30`; this review is at HEAD `36e715e7`. `git status --porcelain` for `sdlc/artifacts/design`, `sdlc/artifacts/requirements`, `sdlc/artifacts/build/change-records` and `app/src/main` is empty in this checkout, so the contracts and the production files read here are the ones the plan read. `BackupWorker.kt` and `RestoreWorker.kt` were read with `git show HEAD:<path>`. The plan's design sections (2.1 to 2.10), edit blocks P2 to P9 (3.2, 3.5 to 3.9), the fetch algorithm, the risks (section 8), the hand-over items (sections 10 and 11) and the criterion map were read; tests were searched, not read line by line. No contract needs a version step. There are no blockers. There are four observations; O-1 is the one worth acting on before the build.

## Findings

### 1. Contract clause by clause

| # | Contract clause (file:line) | What the plan does (plan:line) | Result |
|---|-----------------------------|--------------------------------|--------|
| 1 | CNTR-007 port signatures (`:94-132`) and Breaking change policy: "adding a new port operation, removing one, or changing a signature is breaking" (`:332-337`) | No port method is added, removed or changed. The one edit to `MailTransport.java` is a Javadoc rewrite of four lines by four lines (P8, `:1211-1232`): one hunk `@@ -74,4 +74,4 @@`, every changed line starting with `     * `, identical class-file SHA-256 at I0 and I9 (`:1227-1230`; L20 `:90`). I read the HEAD file (`MailTransport.java:70-98`): the four lines are inside the Javadoc of `appendMessages`; there is no code on them; the file has no `default` or `static` method | Conforms |
| 2 | CNTR-007 Validation Rule 1: no `com.fsck.k9` in `service/` (`:345-349`) | The new main-code blocks (P1, P2, P4 fetcher, cursor, P6, P7) import no `com.fsck.k9` type. The only `com.fsck.k9` imports in the plan are in test fixtures (`:1292-1294`, `:1853`), and the existing service tests already do the same (`BackupWorkerWatermarkTest.kt`, `BackupWorkerLoopCancellationTest.kt`, `CalendarSyncerTest.java`) | Conforms |
| 3 | CNTR-007 Javadoc sentence against the contract that owns the port: the contract states no watermark. `grep -i "watermark\|max_synced"` over `sdlc/artifacts/design/contracts` returns nothing | The new sentence says the returned date is "the date to which the watermark may advance for this batch; the SMS watermark never moves down (U-070)" (`:1222-1225`). It states a rule for callers, not for the port, so nothing in the contract can contradict it. It is true once P2b and P6c land (the SMS write is `max(stored, confirmed)`, P2b `:716-735`), and both land in the same story. It keeps the BUG-010 rule ("watermark only advances on confirmed append") | Conforms. Note O-2 on the contract's own stale text |
| 4 | CNTR-007 Notes and the `MailException` backstop (`:333-341`, `:447-459`) | No exception mapping touched; no new `throws`. The window query and the record never call the port | Not applicable |
| 5 | Contract gate: `integration_contracts: []` (story frontmatter) | Plan section 8 (`:3364-3366`) says the record is state inside one module shared by the backup worker, the restore worker and settings reset. I checked the contract set: no contract names `DataTypePreferences` except as the app-owned parameter of `openFolder` (`CNTR-007:102-107`), whose signature is unchanged. No new component boundary is created (`BackupItemsFetcher`, `DataTypePreferences`, `RestoreWorker` and `BackupWorker` already depend on each other) | Conforms |
| 6 | CNTR-003 Backing-store invariant (`:109-122`): `credentials.xml` stays excluded; "no file holding the three secret keys is ever included in Android auto-backup" | The record keys `sms_backup_record` and `sms_backup_record_version` go into the default preferences file (`Preferences.java:67`; P2b), not `credentials.xml` and not a new file. Neither key is one of the three secrets (`login_password`, `oauth2_token`, `oauth2_refresh_token`). The plan edits neither `backup_descriptor.xml` nor `AndroidManifest.xml` (read at HEAD: `allowBackup="true"` `:95`, `fullBackupContent` `:96`; the descriptor excludes only `credentials.xml` and `pinned_certs.xml`). I1 to I9 diff checks list the manifest as empty (plan `:3240`) | Conforms |
| 7 | CNTR-003 Note (`:103-107`): `oauth2_user` and `login_user` "live in the default `SharedPreferences` … plaintext" | The default file already holds non-secret user data and is already copied by Android; the contract does not forbid other non-secret data there. The record holds dates and 16-hex-character SHA-256 prefixes, no address and no text (plan 2.5 `:277-290`; `entryOf` P1 `:520-531`). No contract, in the set I searched, forbids message-derived data in the default file | Conforms. Residual risk is a security-review item (plan 2.5, SC-10), not a contract matter |
| 8 | CNTR-003 Versioning (`:124-136`): `put` from `commit()` to `apply()` is breaking (for the secret store) | Every new write is one `commit()` carrying all keys it changes (P2b `:733`, `:750`, `:767`, `:785`, `:805`). The secret store is untouched | Conforms |
| 9 | CNTR-002 `pinned_certs` file excluded (`:83-86`) and the default-file flag `transport_security_notice_shown` (`:194`) | Not touched; the new keys do not collide with the existing key names (`git grep` per plan L19, `:89`) | Not applicable |
| 10 | The requirement's AC-19 (the story's AC-17: the record travels with the watermark to another phone) | The record, the marker and the watermark are in the same file, which the descriptor lets Android copy (L9 `:79`; plan 2.3 table row "Android's own backup" `:232`). Tests S10 and S11 parse the descriptor at test time (`:2792-2870`, `:2914-2915`), so a later exclusion would fail them. The marker travels too, so the new phone does not re-seed | Conforms |
| 11 | Headers and identity (REQ-020 fact 13; Message-ID is MD5 of date, address, "v2" and type, `HeaderGenerator.java:108-128`) | The plan edits no file under `mail/` except the `MailTransport.java` comment; the six-file `app/src/main` list is checked at I9 (`:3122`, `:3233`). `appendBatchAndUpdateWatermark` keeps its signature and still passes the same `ConversionResult` to `appendMessages` (P6c `:1195`). The record is built from `result.mapList`, read only (`ConversionResult.getMapList`, `ConversionResult.java:47`). The AC-6 literal is captured on the start commit and compares every header except `X-smssync-backup-time` and `X-smssync-version` (`:1542`, `:1617-1619`, `:375`) | Conforms. `Message-ID`, `X-smssync-id`, `-date`, `-address` are unchanged by construction and by a test |
| 12 | CNTR-004 Validation Rule 6 (`:278`): checkpoint "after each successful insert and before cursor advance"; `smsExists` guard enforces no duplicate rows | P7 (`:1198-1209`) puts one line, `recordRestoredSms(values)`, directly before `ctx.contentResolver.insert(...)`, inside the existing `if (... && !smsExists(values, ctx))`. The order is now record, insert, watermark raise, checkpoint (`RestoreWorker.kt` HEAD `:461-474`). The checkpoint write (`checkpointStore.write`, after the insert) and the `smsExists` guard are untouched. The record is a set, so a replay of the same index after a crash writes the same entry again. A row skipped by `smsExists` is not recorded, which equals today's behaviour | Conforms. Idempotent on replay; the checkpoint rule is unchanged |
| 13 | CNTR-004 INV-1 (`:240`): single flight per unique name, REPLACE | The plan does not rely on single flight; it adds a static lock for the record's read-modify-write (P2b `:691`; 2.7 `:361-363`) and says overlap was not examined (R6, `:3383`). I examined it: `WorkManagerScheduler.kt` enqueues under distinct names (`INCOMING` `:154`, `REGULAR` `:289`, `BROADCAST_INTENT` `:324`, `backupType.name` for MANUAL and SKIP `:365`, the restore name `:400`), and CNTR-004 Rule 8 makes that deliberate. So INV-1 guarantees one run per name, not one run overall, and a backup and a restore may overlap. The lock covers the lost-entry case. See O-1 for the one overlap the lock does not cover | Conforms, with O-1 |
| 14 | CNTR-004 Validation Rule 7 and Error Handling: cancellation is cooperative, terminal; a replaced run is cancelled | After U-067, `ensureActive()` runs at the top of each loop pass (`BackupWorker.kt` HEAD `:330-332`, `currentCoroutineContext()` after U-067). The append and the write are one pass: `openFolder`, `appendMessages`, then the single commit (`appendBatchAndUpdateWatermark`, HEAD `:417-423`; P6c). A cancellation therefore lands either before the pass (nothing written for that batch) or after the commit, at the next pass. It cannot fall between the confirmed append and the commit unless the process dies. That one point is plan 2.7 window 5, the duplicate the requirement accepts (REQ-020 AC-10); the cancellation-before-confirmation case is covered by test B-AC9 (`:2064-2067`) | Conforms |
| 15 | CNTR-004 INV-2, INV-3, INV-4, Rule 8 (constraints, backoff, content trigger, `scheduleManual`) | No enqueue path, constraint, backoff, tag or unique name changes (plan section 8 `:3361-3362`). Skip still runs under `scheduleManual(SKIP)`; the worker's `Result` and progress keys are unchanged; P6a is inside `executeSkip` only | Not applicable (unchanged) |
| 16 | CNTR-004 Versioning and CNTR-007 Versioning: what needs a version step | No port signature, `BackupType` value, `SchedulerState` variant or invariant changes; no new k-9 throwable; the Javadoc line is not contract text. No contract version step arises | Conforms |

### 2. Does the plan edit a contract or a design file?

No. The edit list is six production files and two new ones (plan "Size", `:36-41`), tests, and two README insertions (P9). The developer constraint 12 (`:3482-3483`) forbids touching "any design, requirement, contract or change record". See O-3: no check step enforces it, and `git status` shows none edited now.

### 3. Hand-over items that name contracts

| Item (plan:line) | Accurate? |
|------------------|-----------|
| Section 8: CNTR-007 "used as it is: `checkSettings`, `openFolder`, `appendMessages`, `closeFolders`, and for restore `getMessages` and `importMessageBody`" (`:3355-3357`) | Accurate against the code (`MailTransport.java:117` `fetch`, `:149` `importMessageBody`). The contract's own text lists `fetch` and not `importMessageBody` (`CNTR-007:94-132`); that drift is the contract's, see O-2 |
| Section 8: "The contract states no watermark, so no contract change follows" (`:3358-3359`) | Accurate: no match for "watermark" in `sdlc/artifacts/design/contracts` |
| Section 8: CNTR-004 "Read and not changed: no enqueue path changes. The worker `Result`s and progress keys are unchanged" (`:3361-3362`) | Accurate for the edits listed |
| Section 8: `integration_contracts: []` is right (`:3364-3366`) | Accurate (finding 5) |
| Section 10 item 2: Run R reads the `BackupTriggerWorker` line, not the receiver's (`:3430-3432`) | Not a contract statement; consistent with CNTR-004's note that the broadcast path is not INV-4 evidence |
| Frontmatter `contracts_verified` lists CNTR-007 and CNTR-004 only | Incomplete, not wrong: the plan's privacy and Android-backup analysis rests on the manifest and the descriptor, and does not name CNTR-003's Backing-store invariant or CNTR-002's exclusion. This gate verified both (rows 6 to 9). See O-4 |

## Blockers

None.

## Observations

**O-1 (Medium, for the developer and the plan owner; a one-line ordering fix). The record is read before the provider queries, so a concurrent restore can slip a row past the write-ahead.** In `getSmsItemsWithLookBack` the record snapshot (`:1060`) is taken before the window query (`:1062`) and the forward query (`:1083`). The restore writes the entry before the insert, which is safe only if a reader reads the record after it reads the provider: if a restore's write-ahead and insert both happen after the snapshot but before the forward or window query returns, the row is visible but absent from the snapshot and is uploaded back to the mailbox. REQ-020 AC-16 forbids exactly that duplicate. The contract allows the overlap (finding 13: restore and backup have different unique names, and I found no guard that blocks one while the other runs in `WorkManagerScheduler.kt` or `BackupWorker.kt`). Narrow, and no test in the plan covers it, so the plan's R6 ("not examined", Medium impact, one duplicate) understates it a little. The fix keeps every other property: read the record again after both queries and filter against that (the `entriesAbove` count for the forward LIMIT may stay from the first read, it only widens the LIMIT). This does not need a plan iteration to proceed; it is a fetch-layer ordering change of the kind the plan's own principle ("write-ahead rule", 2.0 `:124-127`) calls for. The same race between two backup runs also exists today for forward rows, so it is not a regression.

**O-2 (Info, carried to the next contract amendment). CNTR-007's text is stale against the port in two places the plan correctly follows the code over.** The contract lists `appendMessages(...): void` (`:109`) and no `importMessageBody`; the port declares `long appendMessages(...)` (`MailTransport.java:84-87`, with the confirmed-date Javadoc the plan edits) and `importMessageBody` (`:149`). The plan reads the signatures from the file, not the contract (Artifacts Consulted, `:3510`), so nothing it does depends on the stale text, and the Javadoc correction is true against the code. Not this story's to fix (no contract edit); the plan should hand it to the next CNTR-007 amendment in section 11. It is the same kind of carried note as U-068's O-2.

**O-3 (Info). No step checks that the design, requirement, contract and change-record directories stay untouched.** Constraint 12 forbids the edit and `git status` is clean now, but the final checks (`:3231-3247`) scope to `app`, `k9mail-vendored`, the build files and `README.md`. One line at I9 (`git status --porcelain -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records` empty) would make "read, not edited" verifiable. Not blocking: the orchestrator can run it at the story's gate.

**O-4 (Info). The plan's contract list should name CNTR-003 and CNTR-002 as read.** They are the contracts that bind the preferences and backup-exclusion side of the plan, and the plan conforms to both (rows 6 to 9); the plan's section 8 and `contracts_verified` do not cite them. This report records the check. If the plan is revised for another reason, add them with one line each.

## Phase Completion Report
---
story_id: "U-070"
phase: "plan-gate/contract-verification"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-024/U-070/contract-verification.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
