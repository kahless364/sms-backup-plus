---
artifact_type: code-review
story_id: U-030
verdict: PASS
agent: Developer
timestamp: "2026-06-04"
---

# Code Review: U-030

## Summary

All changes are minimal, focused, and contract-conformant. No architectural deviations detected.

## Findings

### K9MailTransport.java

- Constructor throws clause narrowed correctly. The `try/catch(MessagingException e)` wraps only the `BackupImapStoreDelegate` construction as specified.
- `BinaryTempFileBody.setTempDirectory(context.getCacheDir())` correctly placed AFTER the try/catch block, executing in the normal path after delegate is built. This preserves the required sequencing.
- `new MailException(e)` used (not `new MailException(e.getMessage())`), preserving the cause chain for `State.getDetailedErrorMessage()`.
- Package-private test constructor unchanged — verified.
- `com.fsck.k9.mail.MessagingException` import retained in K9MailTransport.java (permitted adapter zone).

### MessageConverter.java

- `convertMessages` signature changed from `throws MessagingException` to `throws MailException`. The internal catch wraps correctly with cause preserved.
- The package-private test constructor added is minimal: it takes a `MessageGenerator` and sets the field directly, matching the shape of a standard test seam.
- `messageToContentValues` and `getDataType` still declare `throws MessagingException` (unchanged) — correct, those are NOT converter-boundary methods and are called from within the k-9 adapter zone.

### ServiceBase.java

- FQN catch removed cleanly. No dangling references.
- Method signature `throws MailException` on `getMailTransport()` is unchanged (already correct).

### BackupTask.java

- Inner try/catch removed. The assignment `final ConversionResult result = converter.convertMessages(...)` sits inside the existing outer `try` block which catches `MailException e`. Compile-safety confirmed.
- Updated comment references U-030 as the source of the change.

### Comment Scrub (5 files)

- All 12 occurrences reworded to remove FQN literals. Rewording is semantically equivalent — the purpose (documenting what was removed in U-026) is preserved without naming k-9 types.

### Tests

- `K9MailTransportTest`: Two updated tests now correctly expect `MailException` with `getCause() instanceof MessagingException`.
- New `causeChain_C1_*` test verifies the C-1 path cause chain.
- New `convertMessages_messagingExceptionWrappedAsMailException_causeChainPreserved` test verifies C-2 path cause chain with message "conversion failed".
- All existing auth-escalation tests (XOAuth2, RequiresLoginException) unchanged and passing.

## Issues

None. All ACs satisfied.
