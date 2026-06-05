---
id: U-043
type: plan
verdict: PASS
---

# U-043 Plan: Create IMAP SMS Folder on First Backup to a Fresh Account

## Root Cause Analysis

### File: `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java:479-494`

`createAndOpenFolder` (lines 479-494):

```java
BackupFolder folder = new BackupFolder(this, label, type);
if (!folder.exists()) {
    Log.i(TAG, "Label '" + label + "' does not exist yet. Creating.");
    folder.create(FolderType.HOLDS_MESSAGES);
}
folder.open(Folder.OPEN_MODE_RW);
```

**Two defects:**

1. **Silent CREATE failure ignored** (line 485): `folder.create()` in `ImapFolder.create()` (vendored
   `ImapFolder.java:312-314`) catches `NegativeImapResponseException` and **returns `false`** silently.
   The current `createAndOpenFolder` does not check the return value. If CREATE fails for any reason
   (e.g. server rejects), `open()` is still attempted and will always fail with NONEXISTENT.

2. **Gmail create-then-select propagation gap** (root cause of the live failure): Gmail's IMAP server
   maps folders to labels. After `CREATE "SMS"` succeeds (returns `true`), the new label is not
   immediately selectable — `SELECT "SMS"` returns `NO [NONEXISTENT] Unknown Mailbox: SMS`. This is a
   well-documented Gmail quirk: the label exists but needs a brief delay before it becomes SELECTable.
   The vendored `ImapFolder.create()` (lines 307-321) issues `CREATE %s` then returns `true`, but
   **does not set `this.exists = true`** and does not verify selectability. The subsequent
   `folder.open()` → `internalOpen()` → `executeSimpleCommand("SELECT SMS")` fails immediately.

### Why `create()` runs (confirms BUG-011 evidence)

`folder.exists()` (vendored `ImapFolder.java:252-289`) correctly returns `false` on a fresh account
(STATUS → NegativeImapResponseException → returns false). So the `if (!folder.exists())` guard IS
entered and `create()` IS invoked. The failure is at the `open()` step after `create()` returns `true`
— the CREATE command succeeded on the wire but the Gmail label is not yet selectable.

### Vendored module assessment

`ImapFolder.create()` is correct per IMAP spec: it issues `CREATE`, returns `true` on OK. The
propagation gap is a Gmail-specific behavior that must be handled at the caller level. **No vendored
module change is required.** The fix belongs entirely in `createAndOpenFolder`.

## Fix Plan

### Change 1: Check `create()` return value

Replace:
```java
folder.create(FolderType.HOLDS_MESSAGES);
```
With:
```java
boolean created = folder.create(FolderType.HOLDS_MESSAGES);
if (!created) {
    throw new MessagingException("Failed to create folder/label '" + label + "'");
}
```

### Change 2: Retry `open()` after `create()` with back-off (Gmail propagation gap)

After a successful `create()`, attempt `open()` up to `MAX_CREATE_OPEN_RETRIES` times (3), sleeping
`CREATE_OPEN_RETRY_DELAY_MS` (1000 ms) between attempts when NONEXISTENT is returned. Only retry on
`NegativeImapResponseException`; propagate all other exceptions immediately.

```java
if (!folder.exists()) {
    boolean created = folder.create(FolderType.HOLDS_MESSAGES);
    if (!created) {
        throw new MessagingException("Failed to create folder/label '" + label + "'");
    }
    // Gmail: newly-created label may not be immediately selectable (propagation gap).
    // Retry open() with back-off.
    openWithRetryAfterCreate(folder);
} else {
    folder.open(Folder.OPEN_MODE_RW);
}
```

Helper method:
```java
private void openWithRetryAfterCreate(BackupFolder folder) throws MessagingException {
    for (int attempt = 1; attempt <= MAX_CREATE_OPEN_RETRIES; attempt++) {
        try {
            folder.open(Folder.OPEN_MODE_RW);
            return; // success
        } catch (NegativeImapResponseException e) {
            if (attempt < MAX_CREATE_OPEN_RETRIES) {
                Log.w(TAG, "Folder open after create returned NONEXISTENT (attempt " + attempt +
                        "); retrying in " + CREATE_OPEN_RETRY_DELAY_MS + "ms", e);
                try { Thread.sleep(CREATE_OPEN_RETRY_DELAY_MS); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new MessagingException("Interrupted while waiting to retry folder open", ie);
                }
            } else {
                throw new MessagingException(
                        "Folder '" + folder.getName() + "' was created but is still not selectable " +
                        "after " + MAX_CREATE_OPEN_RETRIES + " attempts", e);
            }
        }
    }
}
```

Constants (package-private for test access):
```java
static final int MAX_CREATE_OPEN_RETRIES = 3;
static final long CREATE_OPEN_RETRY_DELAY_MS = 1000L;
```

### AC satisfaction

- **AC-1**: After fix, first backup creates the label (CREATE) then opens it with retry-on-NONEXISTENT.
  On Gmail, this resolves the propagation gap.
- **AC-2**: Root cause identified (create() return not checked; Gmail SELECT-after-CREATE gap). Fix
  addresses both.
- **AC-3**: Idempotent path unchanged — if `folder.exists()` is `true`, `open()` is called directly
  with no retry overhead.
- **AC-4**: Unit tests cover: `create()` called when `exists()==false`; `create()` NOT called when
  `exists()==true`; silent `create()` failure throws; retry on NONEXISTENT; success on retry; open
  without retry when folder exists.

### Files changed

| File | Change |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java` | `createAndOpenFolder` + `openWithRetryAfterCreate` helper + constants |
| `app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportCreateOpenTest.java` | New test class for the create/open path |

Vendored module: **not touched**.
