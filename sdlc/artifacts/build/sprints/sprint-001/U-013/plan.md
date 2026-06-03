---
artifact_type: plan
story_id: "U-013"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-02"
---

# Plan: U-013 — Introduce BackupScheduler port and LegacyScheduler adapter

## Approach

Branch-by-abstraction seam. Introduce a `BackupScheduler` interface port in the
`scheduler` package (no `core` module exists yet; the port lives in `app` in a
dedicated `scheduler` package, consistent with the port's contract that no Android
scheduler types appear in signatures). Wrap `BackupJobs` in a `LegacyScheduler`
adapter. Route all 7 call sites through the port via a static accessor on `App`.

## DI Approach

No Hilt available yet (U-022). Using `App.getScheduler(Context)` static accessor
pattern — the binding is set at `App.onCreate()` and accessible from BroadcastReceivers
via `context.getApplicationContext()`. Protected factory methods (`getScheduler()`) on
each call-site class allow test injection. This is the "EntryPoints.get fallback"
documented in the story's Technical Notes.

## Key Files

| File | Action |
|------|--------|
| `app/.../scheduler/BackupScheduler.java` | CREATE — port interface |
| `app/.../scheduler/LegacyScheduler.java` | CREATE — adapter |
| `app/.../scheduler/ScheduledJob.java` | CREATE — return value type |
| `app/.../scheduler/SchedulerState.java` | CREATE — observable state enum |
| `app/.../scheduler/SchedulerObservable.java` | CREATE — Java analog of StateFlow |
| `app/.../scheduler/RestoreSchedulerConfig.java` | CREATE — restore job config |
| `App.java` | MODIFY — binding + static accessor |
| `BackupBroadcastReceiver.java` | MODIFY — CS-1 |
| `BootReceiver.java` | MODIFY — CS-2 |
| `SmsBroadcastReceiver.java` | MODIFY — CS-3 |
| `SmsJobService.java` | MODIFY — CS-4, IC-3 |
| `SmsBackupService.java` | MODIFY — CS-5, CS-7 |
| `LegacySchedulerTest.java` | CREATE — 13 new tests |
| `SmsJobServiceTest.java` | MODIFY — add AC-4 debounce test |
| `SmsBackupServiceTest.java` | MODIFY — BackupJobs→BackupScheduler mock |
| `BootReceiverTest.java` | MODIFY — BackupJobs→BackupScheduler mock |
| `SmsBroadcastReceiverTest.java` | MODIFY — BackupJobs→BackupScheduler mock |

## Constraints

- `BackupJobs.java` — zero diffs (strangler seam requirement)
- `AlarmManagerDriver.java` — zero diffs
- `AndroidManifest.xml` BACKUP action — unchanged
