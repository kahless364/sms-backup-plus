---
story_id: U-069
phase: security-review
verdict: PASS
reviewed_commit: d7a69974
---

## Security Review: U-069

### Summary
The content-trigger path now reaches `WorkManagerScheduler.scheduleIncoming()`, which applies the encryption-degraded gate, the auto-backup and delay rule, and the Wi-Fi-only network constraint on every call. The worker takes no input data and is not exported. The change adds no new exposure.

Verdict: PASS

### Findings

#### Critical
- None.

#### High
- None.

#### Medium
- None.

#### Low
- None.

#### Info
- I-1 (item 1, no regression): `BackupTriggerWorker.kt:60-68` reads `preferences.incomingTimeoutSecs` for the log line only, then calls `scheduler.scheduleIncoming()` and returns `Result.success()` whether or not work was enqueued. This is correct for a gate. Returning failure or retry would make WorkManager re-run the periodic trigger needlessly. The scheduler, not the worker, makes the decision.
- I-2 (item 1, order of checks): in `WorkManagerScheduler.kt` at the commit, `scheduleIncoming()` (lines 133-157) checks `secretStore.isEncryptionDegraded()` first (L134-137, returns null, no enqueue). It then checks `!isAutoBackupEnabled || timeoutSecs <= 0` (L139-143, returns null). Only then does it build the request, with `networkConstraints(INCOMING)` (L145-153). `networkConstraints` (L463-473) returns UNMETERED when `isWifiOnly` and CONNECTED otherwise. Only BROADCAST_INTENT gets `Constraints.NONE`. So in every case the content-trigger-initiated INCOMING work is gated and constrained. The old code skipped all three.
- I-3 (item 1, enqueue inventory under `app/src/main` at the commit). There is no enqueue outside `WorkManagerScheduler.kt`. `BackupTriggerWorker.kt` no longer imports `OneTimeWorkRequest`, `WorkManager` or `BackupWorker`. Its remaining hits are KDoc text.

  | Site (WorkManagerScheduler.kt) | Enqueues | Degraded gate | Auto-backup rule | Network constraint |
  |---|---|---|---|---|
  | L145-153 `scheduleIncoming` | BackupWorker, INCOMING | yes | yes | yes (Wi-Fi-only aware) |
  | L182-193 `scheduleRegular` (periodic) | BackupWorker, REGULAR | yes | yes | yes |
  | L232-242 `scheduleContentTrigger` (periodic) | BackupTriggerWorker | yes | no (not needed; it only schedules `scheduleIncoming()`) | none (content-URI trigger only) |
  | L273-281 `scheduleBootup` | BackupWorker, REGULAR | yes | yes (cancels all when off) | yes |
  | L309-316 `scheduleImmediate` | BackupWorker, BROADCAST_INTENT | yes | no, by design (Tasker and automation broadcast) | `Constraints.NONE` by design (CNTR-005) |
  | L350-357 `scheduleManual` | BackupWorker, MANUAL or SKIP | yes | no, by design (user-initiated) | `Constraints.NONE` by design |
  | L385-392 `scheduleRestore` | RestoreWorker | yes | n/a | none by design |

  The `SmsBroadcastReceiver.java:60` call goes through `scheduleIncoming()`, so it is gated. The receiver is known never to be delivered and has its own removal story. Every site that enqueues a `BackupWorker` checks `isEncryptionDegraded()`. The ones without the auto-backup or network rules are user-initiated or explicit-automation paths whose design is documented. None is introduced or changed by this story.
- I-4 (item 2, no new exposure): the manifest at the commit is untouched. Searching for `BackupTriggerWorker` or `Worker` classes in `AndroidManifest.xml` finds nothing but comments. A `Worker` is not a manifest component and is run only by WorkManager's own scheduler, so another app cannot start it or supply its input. The only third-party-reachable entry points are existing, unchanged components: `BackupBroadcastReceiver` (exported, action `com.zegoggles.smssync.BACKUP`, gated by `scheduleImmediate()`'s degraded check) and the SMS-role components. The worker reads no input data (`inputData` appears in `app/src/main` only in `RestoreWorker.kt:229`). It is built by `HiltWorkerFactory` from `@Assisted context` and `params` plus the injected `BackupScheduler` and `Preferences`. Class name and package are unchanged, which keeps persisted CONTENT_TRIGGER work valid.
- I-5 (item 3, logging): the worker's log (`BackupTriggerWorker.kt:62-63`) writes only a fixed text and the numeric timeout. The scheduler lines on this path (`WorkManagerScheduler.kt` L135, L140-141, L155-156) write only a fixed text, the autoBackup boolean, the timeout number and the constraint name (UNMETERED or CONNECTED). There is no message body, address, account name or credential. Line 135 states "credentials in plaintext" as a condition, not a value.
- I-6 (item 4, test file): `BackupTriggerWorkerTest.kt` uses mocked `Preferences` and `SecretStore` and a real `WorkManagerScheduler` over a test WorkManager. A grep for password, token, secret value, e-mail pattern, IMAP host and phone-number patterns finds only the type name `SecretStore` and its mock. The reflection (`Class.forName` for `androidx.work.impl.*` at L423-454, and `getDeclaredMethod("bind", ...)` at L316) lives only in the test source set. `git grep` for `setAccessible|getDeclaredField|getDeclaredMethod` under `app/src/main` finds nothing in the commit tree. The test reflection reaches WorkManager's own database to read the stored work spec. That is a test-only read of a test-process database.
- I-7 (item 5, build file): `git show --stat d7a69974` for `app/` shows `build.gradle` (+2/-2, JaCoCo `worker*` floor 0.00 to 0.90 plus its comment), `BackupTriggerWorker.kt` and `BackupTriggerWorkerTest.kt`. No dependency, repository, signing, minSdk or permission line changed, and `AndroidManifest.xml` is not in the stat. The new imports (`androidx.hilt.work.HiltWorker`, `dagger.assisted.*`) are already used by `BackupWorker` and `RestoreWorker`, so no new artifact is needed.
- I-8 (item 6, evidence hygiene), `git grep` on the commit tree under `sdlc/artifacts/build/sprints/sprint-024/U-069/` (62 files):
  - E-mail pattern `@[A-Za-z0-9-]+\.[a-z]{2,}`: 1 hit, `plan.md:170`. It is the Java annotation text `@androidx.hilt...` from a ProGuard rule note and is benign, not an address. No `test@test.local` hit and no real address.
  - `Users[/\\]`, `C:[/\\]Users`, `AppData`, and the owner's user-name prefix (case-insensitive): 0 hits.
  - Owner surname and employer domain patterns in the evidence folder and in `app/src/test/.../worker`: 0 hits.
  - Host names: 0 hits for the patterns above. Note that the commit's author header carries an e-mail address, as every commit does. That is git metadata, not evidence content, and I make no finding on it.
- I-9 (known, not raised against this story): BUG-029 (a backup that starts off screen fails at `setForeground`) means INCOMING work this path now enqueues can fail on Android 12+. That is a reliability defect and does not widen exposure. This change does not make it worse. The only difference is that the gate is applied before enqueue, so fewer runs reach it on degraded or auto-backup-off devices.

### Security Checklist
- [x] No hardcoded credentials or secrets
- [x] Input validation on all user inputs (the worker takes no input; the changed code reads no external input)
- [x] Output encoding prevents injection attacks (not applicable to the change)
- [x] Authentication tokens handled securely (untouched)
- [x] Authorization checks on all protected resources (degraded gate and auto-backup rule now on the content-trigger path)
- [x] Sensitive data encrypted at rest and in transit (untouched; the gate prevents plaintext-credential runs)
- [x] Error messages don't leak internal details
- [x] Dependencies have no known critical vulnerabilities (none changed)

### Runtime Claims Verified
- [x] `## Runtime Claims` inventory: not re-verified by a probe from this reviewer (read-only, no builds or device commands). The behaviours the story relies on (degraded gate, auto-backup rule, constraint, REPLACE) are asserted by `BackupTriggerWorkerTest.kt`, which uses a real `WorkManagerScheduler`, and I confirmed each rule in the code at the commit (I-2). The unit run itself was not repeated here.
- [x] I read the implementation-log claims against the files at the commit, not as given.
- [ ] Probe adequacy audit and `../amp` confinement: not audited by this reviewer, because the task set read-only scope and named no runtime-behaviour acceptance criterion beyond what the unit tests exercise. No security-relevant exploit-path claim in this change depends on a device probe.
- [x] UNVERIFIED (escape-hatch) claims: none seen by this reviewer.
