---
artifact_type: code-review
story_id: U-040
verdict: PASS
agent: Developer
timestamp: 2026-06-04
---

# Code Review: U-040

## Summary
The fix is minimal and correct. The degraded-security flag mechanism is well-isolated:
a separate `credentials_meta` SharedPreferences file, a public read accessor, and a private
write helper. All U-012 and U-035 invariants are preserved.

## Review Findings

### Positive
- Separate file (`credentials_meta`) is the right choice — avoids polluting the default
  prefs namespace and keeps the flag readable without opening the encrypted store.
- `isEncryptionDegraded()` is the right shape for a future gating hook.
- `MIGRATION_COMPLETE_KEY` is NOT written on failure — exactly-once invariant is preserved.
- `setEncryptionDegraded(false)` on success correctly handles the "Keystore recovered" case.
- The `commit()` call in `setEncryptionDegraded()` is synchronous but this runs on the
  background migration thread — main thread not blocked (U-035 invariant preserved).
- Changing `getEncrypted()` to package-private is the correct test seam approach; it doesn't
  widen the API beyond the package.

### Concerns / Notes
- `getEncrypted()` is now package-private instead of private. This is intentional and safe
  within the `com.zegoggles.smssync.preferences` package, but it slightly weakens encapsulation.
  The benefit (testability of the failure path without reflection) outweighs this.
- The `credentials_meta` file is not currently excluded from Android backup. If the degraded
  flag persists through a backup/restore cycle, it could cause a false positive on a restored
  device where the Keystore is healthy. This is an acceptable edge case — the flag clears on
  the next successful migration (which will run since MIGRATION_COMPLETE_KEY was not written).
- Backup gating and user-visible notification are explicitly deferred per story scope.
  The `isEncryptionDegraded()` hook is ready for a future story.

## No Blocking Issues Found
All changes are within the stated scope of U-040. U-012 and U-035 invariants verified.
