---
artifact_type: gate-result
gate: contract-verification
gate_id: "contract-verification"
checkpoint: plan-gate
story_id: "U-070"
verdict: PASS
iteration: 2
agent: "Solution Architect (separate instance)"
timestamp: "2026-11-21T12:26:37Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md"
base_commit: "c8e98a7f98e5c28593bddbf207c6d19ef5f1ed52"
contracts_checked:
  - CNTR-MODERNIZATION-007 (v2; port signatures, Validation Rule 1, Breaking change policy)
  - CNTR-MODERNIZATION-004 (v3; INV-1, Validation Rules 6 to 8, Error Handling)
  - CNTR-MODERNIZATION-003 (v1; Backing-store invariant)
  - CNTR-MODERNIZATION-002 (pinned_certs exclusion)
  - CNTR-MODERNIZATION-001, -005, -006, -008 (searched in iteration 1; nothing in iteration 2 reaches a site they bind)
---

# Gate Report: gate-result — U-070

## Gate: contract-verification @ plan-gate

## Verdict: PASS

## What changed since iteration 1

Iteration 2 of the plan (planned at `2aad7d70`, `iteration: 2`) answers the plan-review's two test-text blockers and my iteration-1 observations. For this gate the substantive changes are: (a) my O-1 is taken up. The fetch layer now reads the record twice: a first read only sizes the forward `LIMIT`, and a second read, taken after both provider queries have returned, is the one window and forward rows are filtered against (plan 2.4 steps 3 to 8, the "Why the second read closes the restore race" argument, 2.7 row 11, risk R13, test B26, mutation M-L26 and failure-table row D23). (b) O-2 is handed to the next CNTR-007 amendment (plan section 11 item 8). (c) O-3 became an I9 record of `git status` and `git log` for the design, requirements and change-record directories, scoped to this story's files (plan `:3700-3701`). (d) O-4: CNTR-003 and CNTR-002 are in `contracts_verified` and section 8. Other changes with no contract surface: the digest hashes UTF-16 code units by hand (the stored form is still the date plus 16 hex characters); the Android-backup test empties every excluded preferences file; rows the SQL contact-group clause excludes are never recorded (accepted, named, plan 2.8 item 5); counts and scoping corrections. I re-read the plan's 2.4, 2.7, 2.8 item 5, 2.9, P7 (`:1444-1455`), P8 (`:1457-1478`) and the iteration table, and re-checked the contracts. Design, requirements and change-record directories are unchanged between the iteration-1 base `36e715e7` and now (`git diff --stat` over `sdlc/artifacts/design` and `sdlc/artifacts/requirements` is empty).

## Findings

### 1. Does the second read close the race against CNTR-004 Rule 6 and REQ-MODERNIZATION-020 AC-16, and open no new window?

Ordering argument (plan `:371-380`), checked against the restore code at HEAD (`RestoreWorker.kt` `insertSmsValues`: `smsExists` guard, then insert, then watermark raise, then `checkpointStore.write`) and P7 (record, then insert, inside the same guard):

| Interleaving of one restore row R and one backup fetch | Outcome | Result |
|--------------------------------------------------------|---------|--------|
| R's write-ahead commit and insert both complete before a query reads the provider | R can be in a cursor. The commit updates `SharedPreferencesImpl`'s in-memory map before the insert starts (L14); the second read comes after the queries return, so it sees R's entry and drops R (window filter or forward filter) | Closed. This is the iteration-1 hole |
| Write-ahead commit before the second read, insert after the queries returned | R is in no cursor of this run. The next run's second read sees the entry (an entry above or inside the window is kept, or pruned only when no window can reach it) | Closed |
| Both after the second read | Same: R is in no cursor of this run; entry exists for the next run | Closed |
| Write-ahead commit after the second read, insert after the queries returned | R in no cursor of this run. Next run reads the entry | Closed |
| Write-ahead committed, insert never happens (process death, or the insert returns null) | An entry for an absent row; harmless (2.7 row 8): if the same triple appears it is on the server already | No new window |
| Restore replayed from the checkpoint | The record is a set and `smsExists` is evaluated again, so the same entry is written again | Idempotent; CNTR-004 Rule 6 unchanged (checkpoint after the insert, before the cursor advances) |

The second-read order is the only one the write-ahead supports, so it closes the window I raised and creates no ordering between the restore's own steps and the contract's checkpoint rule. The first read sizes the forward `LIMIT` only; the entry count above the watermark can only grow between the reads, so the limit is never too small (the plan's budget argument, `:381-385`: late plus kept forward rows never exceed `max`). The plan's test (B26, a provider-double hook that performs the restore's two steps during the queries) bites M-L26 (the second read replaced by the first), and D23 covers a test that does not observe the window.

Two residual cases remain. Neither is a blocker; both are one duplicate, never a loss, and both need more than a restore and a backup.

- **O-1 (Info). A cursor that refills its window.** The argument says a row in a cursor was inserted before the query returned. That is exact for the first `CursorWindow`, which the provider fills when it returns the cursor. A result larger than one window (about 2 MB, thousands of SMS) is refilled by re-running the statement when the backup moves past the first window, and the forward cursor is stepped after the second read (step 8). A restore inserting a row at that moment, in the one-window-boundary case, is not covered by the argument. The window query is stepped before the second read (step 4 counts its rows), and the forward query is bounded by `max` plus the record's entries above the watermark, so a run would need a very large `max` or backlog. The plan could state the assumption in 2.4 in one line ("the cursors are not larger than one window"); no change to the design is needed.
- **O-2 (Info). Two backups pruning plus a restore.** Plan 2.9 states that a concurrent backup which raises the watermark and prunes between this fetch's two reads can make rows selectable again, and bounds it as one duplicate. With a restore in the mix the duplicate can be a restored row, which REQ AC-16 forbids as a fix-introduced duplicate. It needs three overlapping runs (two backups under different unique names, since INV-1 allows that, and a restore), so the plan's acceptance of it is reasonable. Stating in 2.9 that the case includes a restored row would make it honest; not needed to proceed.

### 2. Does anything in iteration 2 change a port, a header, the stored-state rules or the backup-exclusion rule?

| Surface | Iteration 2 | Result |
|---------|-------------|--------|
| `MailTransport` port (CNTR-007 `:94-132`) | P8 is unchanged: four Javadoc lines replaced by four, one hunk `@@ -74,4 +74,4 @@`, class bytes compared at I0 and I9 (`:1457-1478`). No method is added or changed. The new main code (P1 to P7) still names no `com.fsck.k9` type | Conforms |
| Headers (`Message-ID`, `X-smssync-*`) | Nothing under `mail/` is edited except the comment; the six-file `app/src/main` list stands; the AC-6 capture compares every header except `X-smssync-backup-time` and `X-smssync-version`. The digest change touches only `SmsBackupWindow.digest`, which builds the record key and is not a header | Conforms |
| Stored state and CNTR-003 (`:109-122`) | The record and marker stay in the default file, one `commit()` per write, no new file, no secret key. The digest's input encoding changes how the key is computed, not what is stored: still `date:` plus 16 hex characters, no address, no text. Plan 2.5 states the privacy consequence | Conforms |
| Backup-exclusion rule (descriptor excludes `credentials.xml` and `pinned_certs.xml`, manifest `allowBackup` and `fullBackupContent`) | No edit to the descriptor or manifest. The Android-backup test now empties every excluded preferences file before it checks the new phone, which is stricter than iteration 1 (it proves the record alone carries the state) | Conforms |
| Contact-group difference (plan 2.8 item 5) | A row the SQL group clause excludes is not returned, examined or recorded, so widening the group uploads that SMS within W. No contract binds the group clause (CNTR-008 is the contacts port and its signatures are untouched). The orchestrator's ruling is in the story's Implementation Notes. It is a behaviour difference stated in the plan, not a contract change | Not a contract matter; conforms |
| Scheduler (CNTR-004 INV-1 to INV-4, Rules 7 and 8) | No enqueue path changes. The plan now cites the distinct unique-work names (L23, `WorkManagerScheduler.kt:154`, `:289`, `:324`, `:365`, `:400`) and the single process, so R6 is resolved from the code and agrees with my iteration-1 finding | Conforms |

### 3. O-2 to O-4 of iteration 1, as stated

| Iteration-1 item | Iteration-2 handling | Result |
|------------------|----------------------|--------|
| O-2: CNTR-007 stale (`appendMessages` returns `long`, `importMessageBody` exists) | Section 11 item 8 hands it to the next CNTR-007 amendment; no contract edit here | Accepted as stated |
| O-3: no check that the design, requirement, contract and change-record directories stay untouched | I9 records `git status --porcelain` and `git log $START..HEAD` for the three directories and checks that no file of this story's diff lies there; a change by someone else is recorded, not a stop (`:3700-3701`). The scope matches the U-068 iteration-2 rule. A grep of the working tree now shows `app/src/main` modified by other work in this checkout; none of those files is a U-070 file and none was read for this gate | Accepted as stated |
| O-4: CNTR-003 and CNTR-002 not in the contract list | `contracts_verified` names CNTR-007, -004, -003, -002; section 8 describes the backing-store invariant and the `pinned_certs` exclusion | Accepted as stated |

### 4. Contract versions and edits

The plan edits no contract, design or requirement file. No port signature, `BackupType` value, `SchedulerState` variant or invariant changes and no new k-9 throwable arises, so no contract version step is needed.

## Blockers

None.

## Observations

**O-1 (Info).** The second-read argument assumes a cursor reflects the provider at the time the query returns, which holds for the first cursor window; state the one-window assumption in plan 2.4 (finding 1).

**O-2 (Info).** The three-run case (two backups pruning and a restore) can duplicate a restored row; say so in plan 2.9 (finding 1). Both observations are one duplicate, never a loss.

**O-3 (Info, carried from iteration 1's O-2).** CNTR-007's text is stale against the port (`appendMessages` returns `long`; `importMessageBody` exists); handed to the next amendment in plan section 11 item 8.

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
