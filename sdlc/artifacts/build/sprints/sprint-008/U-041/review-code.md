---
artifact_type: review-code
story_id: U-041
verdict: PASS
agent: Code Reviewer
timestamp: 2026-06-05
blockers: 0
warnings: 1
---

# Code Review: U-041 (Re-review — supersedes prior FAIL)

> **This review supersedes the prior FAIL verdict dated 2026-06-05.** The prior review
> identified one blocker: the `onActivityResult` `REQUEST_CHANGE_DEFAULT_SMS_PACKAGE` case
> gated re-entry to `startRestore()` on `preferences.getSmsDefaultPackage() != null`, which
> the Q+ branch never sets — so restore never started on Q+ after role grant. A remediation
> commit has been applied. This review validates the remediation on the current HEAD.

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | N/A — no CNTR-* contracts apply to this story |
| Test coverage | Full — 5 tests covering forward path, guard, round-trip positive, round-trip negative |
| Code quality | Clean; fix is minimal and targeted; comments explain the invariant precisely |

## Verdict: PASS

---

## Findings

### Blockers

None.

The specific blocker from the prior review is confirmed resolved (see Integration Verified
section below for the traced proof).

---

### Warnings

**WARN-1: `bug009_onQPlus_requestDefaultSmsPackageChange_doesNotShowErrorToast` retains a vacuous assertion (pre-existing, not introduced by remediation)**

File: `app/src/test/java/com/zegoggles/smssync/activity/MainActivityRestoreTest.java`, line 122

```java
assertThat(shownToastCount()).isEqualTo(0);
assertThat(getTextOfLatestToast()).isNotEqualTo(errorToastText);  // always passes when count == 0
```

`ShadowToast.getTextOfLatestToast()` returns `null` when no toast has been shown. `null` is
not equal to any non-null string, so the second assertion can never fail independently once
the first assertion (`shownToastCount() == 0`) has passed. The second assert adds no
discriminating signal — a real toast-shown regression would be caught by `shownToastCount()`
before the `isNotEqualTo` is evaluated. This is a test quality issue (redundant assertion), not
a correctness bug. It does not affect PASS/FAIL verdict. Recommend removing or replacing with
a conditional assertion (e.g., `assertThat(getTextOfLatestToast()).isNull()`) in a future
cleanup pass.

---

### Observations

**OBS-1: Blocker remediation is minimal, correct, and well-commented**

`app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java`, lines 232-243.

The fix introduces a single ternary guard:

```java
final boolean readyToRestore = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        ? isSmsBackupDefaultSmsApp(this)
        : preferences.getSmsDefaultPackage() != null;
if (readyToRestore) {
    startRestore();
}
```

On Q+: `isSmsBackupDefaultSmsApp(this)` delegates to `RoleManager.isRoleHeld(ROLE_SMS)` (verified
at `SmsReceiver.java:47-49`). After a successful role grant the role is held, so this returns
`true` and `startRestore()` proceeds. On pre-Q: `getSmsDefaultPackage() != null` preserves
the original invariant exactly. RESULT_CANCELED early-break at line 230 is unchanged.

**OBS-2: Round-trip call chain verified end-to-end**

Full Q+ restore path after role grant:

1. `onActivityResult(REQUEST_CHANGE_DEFAULT_SMS_PACKAGE, RESULT_OK, _)` — line 229
2. `readyToRestore = isSmsBackupDefaultSmsApp(this)` → `true` (role held) — line 238-239
3. `startRestore()` — line 242
4. `isSmsBackupDefaultSmsApp(this)` → `true` again (same RoleManager state) — line 424
5. `startService(new Intent(this, SmsRestoreService.class))` — line 425

No branch escapes or silent no-ops. The restore service intent is issued.

**OBS-3: Pre-Q behavior unchanged and verified**

The pre-Q path in `startRestore()` (lines 438-453) is untouched: legacy
`Sms.getDefaultSmsPackage()` is read, `setSmsDefaultPackage()` is called on non-empty result,
the tablet/no-telephony toast is restricted to pre-Q. The pre-Q `onActivityResult` guard
(`getSmsDefaultPackage() != null`) remains correct because `setSmsDefaultPackage()` is always
called before `requestDefaultSmsPackageChange()` on pre-Q.

**OBS-4: `requestDefaultSmsPackageChange()` unchanged and correct**

Lines 491-503 are verified unmodified from prior review. Q+ path calls
`SmsReceiver.enable(this)` before `startActivityForResult`, preventing the app from
missing incoming SMS after becoming default. Pre-Q path uses `ACTION_CHANGE_DEFAULT`. Both
paths use `REQUEST_CHANGE_DEFAULT_SMS_PACKAGE` as request code, preserving `onActivityResult`
dispatch.

**OBS-5: `restoreDefaultSmsProvider()` unchanged — AC-2 preserved**

Lines 505-515: `SmsReceiver.disable(this)` on Q+ releases the role by disabling the receiver;
pre-Q switch-back intent with `ACTION_CHANGE_DEFAULT` is intact. No regression.

**OBS-6: Static import of `isSmsBackupDefaultSmsApp` confirmed at top-of-file**

Line 100: `import static com.zegoggles.smssync.compat.SmsReceiver.isSmsBackupDefaultSmsApp;`
— confirms the symbol is available in `onActivityResult` context without any additional change.

---

## Patterns Verified

- [x] Follows existing code patterns — `Build.VERSION.SDK_INT >= Build.VERSION_CODES.*` branching; `@TargetApi` annotation; static import of `isSmsBackupDefaultSmsApp`
- [x] Error handling is appropriate — toast scoped to pre-Q only; no NPE risk; RESULT_CANCELED break preserved
- [x] Tests cover new functionality — 5 Robolectric tests at `@Config(sdk = Q)`: forward role request, no-error-toast, already-held guard, onActivityResult RESULT_OK round-trip, onActivityResult RESULT_CANCELED negative case
- [x] No hardcoded values that should be configurable
- [x] No unnecessary complexity — fix is a single ternary guard replacing the original `getSmsDefaultPackage() != null` check

## Integration Verified

- [x] New code is reachable from production entry points — `doPerform(Actions.Restore)` → `startRestore()` (line 420) → Q+ `else if` branch (line 426) → `requestDefaultSmsPackageChange()` (line 434)
- [x] Registries/dispatch maps — no new registrations required; existing `REQUEST_CHANGE_DEFAULT_SMS_PACKAGE` switch case in `onActivityResult` reused
- [x] Function signatures match at all call sites — `isSmsBackupDefaultSmsApp(Context)` called with `this` (Activity is a Context); `startRestore()` is a no-arg void private method; no arity mismatches
- [x] No dead code introduced — the `startRestore()` call at line 242 is now reachable on Q+ (role held after grant); the full round-trip terminates at `startService()` line 425
- [x] Integration path documented in implementation-log.md — remediation section explicitly traces onActivityResult → readyToRestore → startRestore() → startService()

**Prior blocker trace confirmation:**

Old guard (pre-remediation): `preferences.getSmsDefaultPackage() != null` — on Q+, the Q+
branch of `startRestore()` never calls `setSmsDefaultPackage()`, so this returned `false`
after every Q+ role grant. Restore was silently not called.

New guard (post-remediation): `isSmsBackupDefaultSmsApp(this)` on Q+ — delegates to
`RoleManager.isRoleHeld(ROLE_SMS)` (`SmsReceiver.java:47-49`). After a successful grant the
role is held, so this returns `true`. Restore is called. Blocker is resolved.

## Regression Check

- [x] Capabilities Inventory present in implementation-log.md
- [x] Every RETAINED item verified present in new code at cited file:line
- [x] Every INTENTIONALLY REMOVED item has valid justification citing story requirements
- [x] No capabilities missing from inventory

## Contract Verification

- [x] No CNTR-* contracts apply (story frontmatter `integration_contracts: []`)
- [x] No API/event interface changes — `startActivityForResult` with existing request code; no new fields or event shapes; no consumer contracts affected
