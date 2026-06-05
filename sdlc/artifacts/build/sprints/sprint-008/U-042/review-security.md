---
artifact_type: review-security
story_id: "U-042"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-06-05"
blockers: 0
warnings: 1
---

# Security Review: U-042

## Review Summary

U-042 fixes a critical data-integrity bug (BUG-010) in which the backup watermark
(`max_synced_date`) advanced even when no IMAP append was confirmed, causing silently-skipped
messages on subsequent runs. The change is mechanically minimal — a `void` → `long` return type
change on one interface method plus wiring the return value to the watermark write — and
introduces no new attack surface. No credentials, message content, or sensitive user data are
introduced into logs, persisted data stores, or error messages. The fix is
compiler-enforced: the watermark write is structurally unreachable unless `appendMessages`
returns normally.

## Verdict: PASS

---

## Findings

### Blockers

None.

### Warnings

#### W-01: Empty-result `appendMessages` returns sentinel -1 as watermark (low, informational)

**File:** `K9MailTransport.java:206`, `ConversionResult.java:13`

**Detail:** `ConversionResult.getMaxDate()` returns `DataType.Defaults.MAX_SYNCED_DATE` (-1)
when no messages have been added (the default field initialiser). If `appendMessages` is somehow
called with an empty `ConversionResult` and the k-9 append layer does not throw, the adapter
returns -1, and `setMaxSyncedDate` persists -1 as the watermark for that data type. This would
reset the watermark to its initial sentinel, causing all messages ever received to be
re-uploaded on the next run (data duplication, not data loss). In production this scenario
cannot arise because `BackupWorker.backupCursors()` gates `appendMessages` behind an
`if (!result.isEmpty)` check at line 321 — an empty result is logged and the loop continues
without calling `openFolder` or `appendMessages`. The non-empty guard is present and correct.
This is documented as a warning because the interface contract does not mandate that
`appendMessages` must not be called on an empty result; a future caller that omits the guard
could trigger the regression. No code change required; a defensive `assert` or
`preconditionCheck` on the adapter side would close the gap completely.

**Severity:** Low (no production path reaches this state; informational only)

---

### Security Checklist

- [x] No hardcoded credentials or secrets
      Verified: no literals resembling tokens, passwords, or keys appear in any changed file.
      `getStoreUriForLogging()` (existing, unchanged) masks the password portion of the IMAP
      URI with 'X' characters; the raw URI is never logged.

- [x] Input validation on all user inputs
      Not applicable to this change: the changed code path operates entirely on
      internally-derived values (epoch timestamps from message DATE headers, data-type enum,
      IMAP transport handle). No user-supplied input enters the watermark-advance logic.

- [x] Output encoding prevents injection attacks
      Not applicable: the new `Log.d` line at `BackupWorker.kt:346` logs only two values —
      `cursor.type` (a `DataType` enum; `toString()` is the enum name) and `confirmedMaxDate`
      (a `long` epoch timestamp). Neither value is user-controlled text, neither is an IMAP
      command fragment, and Android logcat does not execute log content. No injection vector.

- [x] Authentication tokens handled securely
      No change to auth token handling. The XOAuth2 retry path and `RequiresLoginException`
      handling are pre-existing and unchanged by this diff. `appendMessages` propagates
      `XOAuth2FailedException` and `RequiresLoginException` from the k-9 layer without
      modification, preserving the existing auth-failure escalation contract.

- [x] Authorization checks on all protected resources
      Not applicable: backup data access is governed by Android `READ_SMS` and
      `READ_CONTACTS` permissions (declared in the manifest; enforced by the OS). No
      privilege boundaries are crossed in the changed code; the watermark is a private
      SharedPreference keyed per data type.

- [x] Sensitive data encrypted at rest and in transit
      The watermark (`max_synced_date`) is an epoch long in SharedPreferences. It is a
      timestamp — not message content, not contact data, not credentials — so storage of it
      unencrypted is acceptable and consistent with the pre-existing design.
      IMAP transport (TLS policy via `MailTransportConfig.tlsPolicy`) is unchanged by this
      diff; the encryption-in-transit path is pre-existing.

- [x] Error messages don't leak internal details
      The error messages added in `K9MailTransport` for BUG-011 (folder create-failure and
      retry exhaustion messages) include only the folder label name and retry counts — no
      credentials, no raw IMAP server responses, no stack traces surfaced to user-visible
      UI. They appear only in Android logcat (DEBUG/WARN level) which is consistent with the
      codebase convention and with Android's logcat access model (requires `READ_LOGS`
      privilege to read programmatically). Exception messages in the new
      `MessagingException` throws include only the folder label string, which is a
      user-configured IMAP folder name (not PII).

- [x] Dependencies have no known critical vulnerabilities
      This diff introduces no new dependencies. No `build.gradle` changes appear in the
      diff. Dependency risk is unchanged from the baseline.

---

## Data-Integrity Integrity Assessment (Primary Risk Class)

**Does the fix genuinely close the silent-data-loss invariant?**

Yes. The closure is complete and compiler-enforced:

1. **`MailTransport.appendMessages` return type change (`void` → `long`):** The interface now
   requires the adapter to return a confirmed epoch timestamp on success. The compiler
   guarantees callers receive the value only if the method returns normally. A method that
   throws cannot return a value; there is no sentinel return path for the failure case.

2. **`K9MailTransport.appendMessages` adapter:** `result.getMaxDate()` is returned on the line
   immediately after `folder.folder.appendMessages(messages)` completes normally. If the k-9
   layer throws any exception (`MessagingException`, `AuthenticationFailedException`,
   `XOAuth2AuthenticationFailedException`), control transfers to the catch blocks, which
   re-throw translated app exceptions. The return statement is never reached. This is not a
   best-effort check — it is structurally impossible for a date to be returned if k-9 threw.

3. **`BackupWorker.backupCursors()` call site:** `val confirmedMaxDate: Long =
   transport.appendMessages(folder, result)` captures the confirmed value.
   `setMaxSyncedDate(cursor.type, confirmedMaxDate)` is on the very next executable statement
   after calendar sync (which is type-gated and does not affect the watermark). If
   `openFolder` or `appendMessages` throws, the Kotlin exception propagates out of the
   while-loop body; `setMaxSyncedDate` is not on any catch or finally path and is therefore
   never called for the failed batch.

4. **Per-batch checkpoint behavior (U-016 compatible):** The watermark write is inside the
   loop (per-batch), not after the loop. A batch-1-success / batch-2-failure scenario
   correctly leaves the watermark at the batch-1 confirmed date. This was verified by the
   `ac3c_partialSuccess_watermarkEqualsBatch1MaxDate` regression test.

5. **`confirmedMaxDate` is message-derived, not wall-clock:** `ConversionResult.getMaxDate()`
   tracks the maximum value of the `X-sms-date` (or equivalent) header parsed during
   conversion — set from the original SMS/MMS/calllog timestamp, not from `System.currentTimeMillis()`.
   This ensures the watermark correctly represents the latest message actually backed up,
   enabling exact-recovery semantics on retry.

6. **SKIP path unaffected:** `executeSkip()` calls `setMaxSyncedDate(type, fetcher.getMostRecentTimestamp(type))`
   directly — this path is intentionally marking all existing messages as "already seen"
   without appending them (the documented SKIP semantics). This path is unchanged and correct.

**Residual gap (not introduced by this diff):** If the k-9 `appendMessages` implementation
silently "succeeds" without actually persisting messages to the IMAP server (i.e. returns
without throwing even though the server rejected or discarded the messages), the adapter would
return a date and the watermark would advance for messages that were not durably stored. This
is a pre-existing limitation of the k-9 library's "fire-and-check" append behavior and is not
introduced or worsened by this change. It is outside the scope of U-042 as defined.
