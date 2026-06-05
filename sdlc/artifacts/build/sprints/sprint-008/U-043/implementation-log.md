---
id: U-043
type: implementation-log
verdict: PASS
story: U-043
sprint: '000008'
---

# U-043 Implementation Log: Create IMAP SMS Folder on First Backup to a Fresh Account

## Summary

Fixed BUG-011: First backup to a fresh Gmail/IMAP account (no "SMS" label) failed with
`NO [NONEXISTENT] Unknown Mailbox: SMS`. Two root-cause defects identified and fixed in
`K9MailTransport.BackupImapStoreDelegate.createAndOpenFolder`.

## Root Cause

**File:** `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java:479-494`
(original `createAndOpenFolder`)

**Defect 1 (silent CREATE failure, line 485):**
`folder.create(FolderType.HOLDS_MESSAGES)` returns `false` on `NegativeImapResponseException`
(vendored `ImapFolder.java:312-314`) but the caller never checked the return value. If CREATE
fails silently, `open()` is still attempted and always fails with NONEXISTENT.

**Defect 2 (Gmail label propagation gap, root cause of live failure):**
After a successful `CREATE "SMS"` (returns `true`), Gmail's IMAP server does not immediately
make the new label SELECTable. The subsequent `open()` → `SELECT "SMS"` returns
`NO [NONEXISTENT] Unknown Mailbox: SMS`. This is a well-documented Gmail behavior (RFC 5530
NONEXISTENT response code). The vendored `ImapFolder.create()` issues `CREATE` and returns
`true` correctly per IMAP spec; the gap must be handled at the caller level.

**Vendored module verdict:** Not touched. `ImapFolder.create()` is spec-correct.

## Changes

### File 1: `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java`

**Lines changed:** ~474-595 (within `BackupImapStoreDelegate` inner class)

Added:
- `MAX_CREATE_OPEN_RETRIES = 3` (static final, package-private)
- `CREATE_OPEN_RETRY_DELAY_MS = 1000L` (static final, package-private)
- `createBackupFolder(DataType, String)` — factory method (package-private, test seam)
- `getRetryDelayMs()` — returns retry delay (package-private, overrideable in test subclass)
- Updated `createAndOpenFolder` — checks `create()` return value; calls `openWithRetryAfterCreate()` after successful CREATE; direct `open()` for existing folders
- `openWithRetryAfterCreate(BackupFolder)` — retries `open()` up to `MAX_CREATE_OPEN_RETRIES` times on NONEXISTENT; propagates other exceptions immediately; logs each retry attempt

**Key behaviors:**
- `create()` return value now checked; `false` → `MessagingException` thrown immediately, `open()` not attempted
- NONEXISTENT detected case-insensitively via `msg.toUpperCase(Locale.US).contains("NONEXISTENT")`
- Retry delay applied via overrideable `getRetryDelayMs()` (production: 1000 ms; test: 0 ms)
- Interrupted sleep restores interrupt flag and throws `MessagingException`
- All-retries-exhausted path throws descriptive `MessagingException` with last error chained as cause
- Idempotent path (folder exists) unchanged: direct `open()`, no retry overhead

### File 2: `app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportCreateOpenTest.java`

**New file.** 11 `@Test` methods + `TestableBackupImapStoreDelegate` inner class.

Test coverage:
| Test method | What it covers |
|-------------|---------------|
| `constants_maxRetries_isPositive` | MAX_CREATE_OPEN_RETRIES > 0 |
| `constants_retryDelay_isPositive` | CREATE_OPEN_RETRY_DELAY_MS > 0 |
| `createAndOpenFolder_folderExists_opensDirectlyWithoutCreate` | AC-3: idempotent open path |
| `createAndOpenFolder_folderNotExists_callsCreateThenOpen` | AC-2/AC-4: create() called before open() |
| `createAndOpenFolder_createReturnsFalse_throwsWithoutCallingOpen` | Defect-1 fix: silent create failure |
| `createAndOpenFolder_openFailsNonExistentOnce_thenSucceeds_noException` | AC-1: Gmail propagation gap retry |
| `createAndOpenFolder_allOpenAttemptsFailNonExistent_throwsMessagingException` | AC-1: all retries exhausted |
| `createAndOpenFolder_openFailsNonNonExistent_propagatesImmediately` | Non-NONEXISTENT errors not retried |
| `createAndOpenFolder_customLabel_callLog_createsAndOpens` | AC-3: configurable folder name |
| `createAndOpenFolder_nonExistentDetection_caseInsensitive` | Case-insensitive NONEXISTENT detection |
| `openFolder_cachedFolder_returnedWithoutReopen` | AC-3: second call uses cache |

## Capabilities Inventory (Replacement/Rewrite Audit)

| Capability | Status |
|-----------|--------|
| Folder creation when label missing | RETAINED (K9MailTransport.java:527) — now also checks return value |
| Idempotent open for existing folder | RETAINED (K9MailTransport.java:518-519) |
| IAE → MessagingException re-wrap | RETAINED (K9MailTransport.java:539-543) |
| Log.i on label creation | RETAINED (K9MailTransport.java:526) |
| Configurable folder name (`imap_folder`) honored | RETAINED — label parameter passed through |

## Integration Path

New code path:
```
K9MailTransport.openFolder()                     [K9MailTransport.java:172]
  → BackupImapStoreDelegate.openFolder()         [K9MailTransport.java:416]
  → createAndOpenFolder()                        [K9MailTransport.java:521]
  → createBackupFolder() + create() check        [K9MailTransport.java:524-531]
  → openWithRetryAfterCreate()                   [K9MailTransport.java:558]
  → folder.open() with retry-on-NONEXISTENT      [K9MailTransport.java:562]
```

Called from:
- `BackupWorker.kt` (via `MailTransport.openFolder()` — service layer, no k-9 types)
- ACL boundary preserved: `service.*` never sees k-9 types

## Build / Test Results

```
./gradlew :app:assembleDebug                     → BUILD SUCCESSFUL
./gradlew :app:testDebugUnitTest                 → BUILD SUCCESSFUL
./gradlew :app:jacocoTestCoverageVerification    → BUILD SUCCESSFUL
```

All three tasks: BUILD SUCCESSFUL. No regressions detected.

**Authoritative @Test count (post-commit from HEAD):** 634

## Contract Adherence

No `integration_contracts` listed in U-043 frontmatter. No CNTR-* contracts to verify.

## AC Verification

| AC | Status | Evidence |
|----|--------|----------|
| AC-1 | PASS | `openWithRetryAfterCreate` retries on NONEXISTENT; Gmail propagation gap handled |
| AC-2 | PASS | Root cause identified (create() return not checked + SELECT-after-CREATE gap); both fixed |
| AC-3 | PASS | Idempotent path (`folder.exists()==true`) calls `open()` directly; cache prevents re-open |
| AC-4 | PASS | 11 unit tests cover all paths; build + jacoco coverage verification green |
