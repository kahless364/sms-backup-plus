---
artifact_type: review-code
story_id: "U-025"
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
issues_found: 2
issues_blocking: 0
---

# Code Review: U-025

## Summary

Code review of the MailTransport port, ACL value types, exception hierarchy, and
K9MailTransport adapter. Two non-blocking notes documented.

## Review Findings

### Finding 1 — Composition over inheritance (non-blocking, design choice)

**File:** `K9MailTransport.java`

The `K9MailTransport` class uses composition rather than inheritance from `ImapStore`. This
diverges from the story description which says "reshape BackupImapStore into K9MailTransport".
The composition design is CORRECT and preferable: inheriting from `ImapStore` is blocked by a
Java compile error (the `checkSettings()` throws-clause conflict). The `BackupImapStoreDelegate`
inner class preserves all original IMAP logic verbatim; composition gives clean separation.

**Verdict:** Acceptable design decision. No action required.

### Finding 2 — `MailException.errorResourceId()` uses `status_unknown_error` (non-blocking)

**File:** `MailException.java:66`

The story AC-3 says to use `R.string.err_communication_error`. That resource does not exist
in `strings.xml`. The nearest generic-IMAP-failure string is `R.string.status_unknown_error`.
Documented in implementation-log. Does not affect functional behavior since `State.getErrorMessage`
only invokes `errorResourceId()` via the `LocalizableException` branch.

**Verdict:** Non-blocking. Should be tracked as a string-resource addition if a dedicated
communication-error message is desired.

## ACL Invariant Verification

- `MailTransport.java`: zero k-9 imports — PASS
- `MailTransportConfig.java`: zero k-9 imports — PASS
- `BackupFolderHandle.java`: k-9 import for package-private field — ACCEPTABLE (field is not public)
- `MailMessageHandle.java`: k-9 import for package-private field — ACCEPTABLE (field is not public)
- `FetchSpec.java`: zero k-9 imports — PASS
- `MailException.java`: zero k-9 imports — PASS
- `TemporaryImapException.java`: zero k-9 imports — PASS
- `XOAuth2FailedException.java`: zero k-9 imports — PASS

## Exception Translation Verification

Catch ordering in every K9MailTransport I/O method (verified at `K9MailTransport.java:132-144`):
1. `XOAuth2AuthenticationFailedException` — caught first (subtype of AuthenticationFailedException)
2. `AuthenticationFailedException` — caught second
3. `MessagingException` with magic-string check — caught third
4. Generic `MessagingException` — backstop

This ordering is correct and load-bearing for the `getStatus() == 400` retry path.

## Test Quality

- `MailExceptionTest`: 11 focused unit tests — PASS
- `K9MailTransportTranslationTest`: 9 tests verify catch ordering invariant — PASS
- `K9MailTransportHandleResolutionTest`: 4 tests verify ImapMessage resolution and FetchProfile dispatch — PASS
- `K9MailTransportTest`: 40+ tests covering validators, TLS factory selection, exception paths via mocked ImapFolder, MessageComparator — PASS
- `StateTest.shouldGetErrorMessagePrefix`: updated to use TemporaryImapException — PASS
