---
artifact_type: implementation-log
story_id: U-041
verdict: PASS
agent: Developer
timestamp: 2026-06-05
---

# Implementation Log: U-041

## Summary

Fixed BUG-009: on Android Q+ (API 29+), `MainActivity.startRestore()` now proceeds to
`RoleManager.createRequestRoleIntent(ROLE_SMS)` regardless of what
`Telephony.Sms.getDefaultSmsPackage()` returns. The "no default package — running on
tablet?" toast no longer fires merely because the legacy API returned null; it only fires
on genuinely unsupported pre-Q devices (no telephony).

## Files Changed

### `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java`

**Lines changed: 411-435 (old) → 411-451 (new)**

Restructured `startRestore()` to add an explicit `else if (Build.VERSION.SDK_INT >= Q)`
branch that unconditionally proceeds to `requestDefaultSmsPackageChange()` (or shows the
first-use dialog), without consulting the legacy `Sms.getDefaultSmsPackage()`. The pre-Q
path retains the original legacy capture + non-empty gate + switch-back logic.

Before:
```java
} else {
    final String defaultSmsPackage = Sms.getDefaultSmsPackage(this);
    if (!TextUtils.isEmpty(defaultSmsPackage)) {
        preferences.setSmsDefaultPackage(defaultSmsPackage);
        ...requestDefaultSmsPackageChange() / showDialog()...
    } else {
        // BUG: fires on Q+ with null legacy value → restore never ran
        Toast.makeText(this, R.string.error_no_sms_default_package, LENGTH_LONG).show();
    }
}
```

After:
```java
} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    // Q+: always proceed to RoleManager — legacy value is irrelevant here
    if (preferences.hasSeenSmsDefaultPackageChangeDialog()) {
        requestDefaultSmsPackageChange();
    } else {
        showDialog(SMS_DEFAULT_PACKAGE_CHANGE);
    }
} else {
    // Pre-Q: capture legacy default for ACTION_CHANGE_DEFAULT switch-back
    final String defaultSmsPackage = Sms.getDefaultSmsPackage(this);
    if (!TextUtils.isEmpty(defaultSmsPackage)) {
        preferences.setSmsDefaultPackage(defaultSmsPackage);
        ...requestDefaultSmsPackageChange() / showDialog()...
    } else {
        // Genuinely unsupported pre-Q device (tablet / no telephony)
        Toast.makeText(this, R.string.error_no_sms_default_package, LENGTH_LONG).show();
    }
}
```

`requestDefaultSmsPackageChange()` (lines 469-481) is **unchanged** — it already has the
correct Q+ RoleManager path.

## Tests Added

### `app/src/test/java/com/zegoggles/smssync/activity/MainActivityRestoreTest.java`

New file. 3 `@Test` methods covering BUG-009 / U-041 AC-1 and AC-3:

| Test | AC | Description |
|------|----|-------------|
| `bug009_onQPlus_requestDefaultSmsPackageChange_startsRoleRequestIntent` | AC-1 | On API Q, with ROLE_SMS available but not held, `requestDefaultSmsPackageChange()` fires `startActivityForResult` with `REQUEST_CHANGE_DEFAULT_SMS_PACKAGE`. Verifies role request is produced. |
| `bug009_onQPlus_requestDefaultSmsPackageChange_doesNotShowErrorToast` | AC-3 | On API Q, the `error_no_sms_default_package` toast is NOT shown when the Q+ path executes. |
| `bug009_onQPlus_whenAlreadyRoleHolder_noIntentStarted` | Guard | When the app already holds ROLE_SMS (`isRoleHeld() == true`), no duplicate intent is issued. Confirms the guard condition in `requestDefaultSmsPackageChange()` is preserved. |

**Testing strategy note**: `requestDefaultSmsPackageChange()` is package-private (`void` method in
`com.zegoggles.smssync.activity`), accessible directly from the test class in the same package.
`MainActivity` is `@AndroidEntryPoint`; to avoid Hilt injection under plain Robolectric
(no `HiltTestApplication`), tests use `Robolectric.buildActivity(MainActivity.class).get()`
which creates the activity JVM object and attaches base context but does NOT call `onCreate()`
(bypasses the Hilt `OnContextAvailableListener → inject()` path). The `preferences` field
is injected via reflection. `ShadowRoleManager.addAvailableRole(ROLE_SMS)` is used to make
the SMS role available for the test, and `addHeldRole(ROLE_SMS)` to simulate already holding it.

## Capabilities Inventory (Retained)

Since this modifies an existing method rather than replacing a class, all capabilities are
accounted for:

| Capability | Status | Evidence |
|-----------|--------|---------|
| Already-default fast path: `isSmsBackupDefaultSmsApp()` → `startService()` | RETAINED | `MainActivity.java:415-416` |
| Q+: RoleManager role request via `requestDefaultSmsPackageChange()` | RETAINED + FIXED | `MainActivity.java:422-430`; was previously unreachable when legacy API returned null |
| Q+: first-use dialog `SMS_DEFAULT_PACKAGE_CHANGE` shown before `requestDefaultSmsPackageChange()` | RETAINED | `MainActivity.java:427-429` |
| Pre-Q: legacy package capture for switch-back | RETAINED | `MainActivity.java:431-447` |
| Pre-Q: `preferences.setSmsDefaultPackage()` | RETAINED | `MainActivity.java:434` |
| Pre-Q: toast on genuinely null legacy package | RETAINED | `MainActivity.java:440-442` |
| Q+: role released after restore via `SmsReceiver.disable()` in `restoreDefaultSmsProvider()` | RETAINED (method unchanged) | `MainActivity.java:485-493` |

## Integration Verification

- `startRestore()` is called from `doPerform(Actions.Restore)` (line 401) — unchanged.
- `requestDefaultSmsPackageChange()` is the same package-private method as before.
- `onActivityResult(REQUEST_CHANGE_DEFAULT_SMS_PACKAGE, ...)` handles the result and
  calls `startRestore()` again (line 233) — unchanged.

## Contract Adherence

No `integration_contracts` listed in the story frontmatter (field is `[]`). No CNTR-*
artifacts apply to this story.

## Build / Test Results

```
BUILD SUCCESSFUL in 2m 47s
69 actionable tasks: 19 executed, 50 up-to-date

> Task :app:assembleDebug       — SUCCESS
> Task :app:testDebugUnitTest   — SUCCESS
> Task :app:jacocoTestCoverageVerification — SUCCESS
```

Jacoco coverage gate (service*, mail*, auth* LINE >= 70%) passed without changes or exclusions.
The `activity` package is not gated; the new tests in `MainActivityRestoreTest` add line
coverage to `MainActivity.startRestore()` and `requestDefaultSmsPackageChange()` without
impacting the enforced packages.

## Authoritative Test Count (post-commit)

Per story instruction: `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`

**Result: 626**

(Pre-commit HEAD count: 623; +3 from `MainActivityRestoreTest`)

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|---------|
| AC-1: On Q+, role requested via RoleManager even when `getDefaultSmsPackage()` is null | PASS | `startRestore()` Q+ branch (line 420-429) proceeds to `requestDefaultSmsPackageChange()` unconditionally; test `bug009_onQPlus_requestDefaultSmsPackageChange_startsRoleRequestIntent` verifies role intent is issued |
| AC-2: Post-restore role release preserved; pre-Q switch-back preserved | PASS | `restoreDefaultSmsProvider()` unchanged (lines 483-493); pre-Q legacy capture retained in else branch |
| AC-3: Error toast only fires on genuinely unsupported cases, not null legacy API on Q+ | PASS | Toast moved inside pre-Q `else` branch; test `bug009_onQPlus_requestDefaultSmsPackageChange_doesNotShowErrorToast` asserts 0 toasts on Q+ path |
| AC-4: Build green; Robolectric test covers Q+ null-default path | PASS | BUILD SUCCESSFUL; 3 tests in `MainActivityRestoreTest` at `@Config(sdk = Q)` |
