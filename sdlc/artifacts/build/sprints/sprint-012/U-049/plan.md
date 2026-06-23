---
artifact_type: plan
story_id: "U-049"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-23"
---

# Plan: U-049 — Retire Legacy Backup/Restore Service Dual-Dispatch Layer

## Approach

Delete SmsBackupService, SmsRestoreService, ServiceBase and their manifest entries.
Move foreground notification into workers via setForeground(ForegroundInfo(...)). 
Re-home WorkInfo observation and state emission into MainViewModel.
Wire MainActivity → viewModel.startBackup()/startRestore() directly.

## Implementation Steps

1. Add `setForeground(ForegroundInfo(...))` to BackupWorker.doWork() and RestoreWorker.doWork()
2. Extend MainViewModel to accept BackupScheduler + Context, add startBackup()/startRestore(),
   observeBackupWork()/observeRestoreWork(), cancel collectors, onCleared() cleanup (BUG-005)
3. Update MainActivity: remove SmsBackupService/SmsRestoreService imports, replace startService calls
4. Delete SmsBackupService.java, SmsRestoreService.java, ServiceBase.java
5. Remove `<service>` entries from AndroidManifest.xml
6. Delete dead MainViewModelFactory.kt (broken by constructor change)
7. Delete SmsBackupServiceTest.java, SmsRestoreServiceTest.java
8. Update MainViewModelTest.kt to pass new constructor args
9. Update MainActivityRestoreTest.java to remove SmsRestoreService reference

## Preserved Contracts

- CNTR-MODERNIZATION-005: BackupBroadcastReceiver untouched
- BUG-005: viewModelScope auto-cancels collectors in onCleared()
- BUG-009/U-041: onActivityResult re-entry guard intact; startRestore() path preserved
