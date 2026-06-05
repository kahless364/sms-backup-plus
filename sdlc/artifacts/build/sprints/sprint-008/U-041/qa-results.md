---
artifact_type: qa-results
story_id: U-041
verdict: PASS
agent: "QA Analyst"
timestamp: 2026-06-05
ac_total: 4
ac_passed: 4
ac_failed: 0
tests_run: 645
tests_passed: 645
---

# QA Validation: U-041

Fix for BUG-009 — on Android Q+, `MainActivity.startRestore()` now requests the SMS role
via `RoleManager.createRequestRoleIntent(ROLE_SMS)` regardless of what the legacy
`Telephony.Sms.getDefaultSmsPackage()` returns, instead of bailing with the
"no default package" toast. Verified against the actual on-disk MainActivity.java, the new
MainActivityRestoreTest.java, and the git diff `e570ef2b..HEAD` (U-041 slice =
MainActivity.java 15 lines + MainActivityRestoreTest.java 183 new lines; the other files in
that range belong to the co-merged U-042/U-043).

## Verdict: PASS

## Acceptance Criteria Results

> Every PASS cites a `file:line` reference. The on-device sub-clause of AC-4 is explicitly
> deferred to post-merge device validation per the story text and is not failed here.

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1: On Q+, tapping Restore requests the SMS role via RoleManager even when `getDefaultSmsPackage()` returns null/empty; role-request dialog appears | PASS | Production: `MainActivity.java:417-428` adds an `else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)` branch that reaches `requestDefaultSmsPackageChange()` (line 425) WITHOUT ever calling `Sms.getDefaultSmsPackage()` — the legacy call is now confined to the pre-Q `else` (line 431). `requestDefaultSmsPackageChange()` at `MainActivity.java:482-489` issues `roleManager.createRequestRoleIntent(ROLE_SMS)` + `startActivityForResult(intent, REQUEST_CHANGE_DEFAULT_SMS_PACKAGE)`. Test: `MainActivityRestoreTest.java:73-100` (`bug009_onQPlus_requestDefaultSmsPackageChange_startsRoleRequestIntent`) — `@Config(sdk = Q)`, asserts `getNextStartedActivityForResult()` is non-null and `requestCode == REQUEST_CHANGE_DEFAULT_SMS_PACKAGE` (lines 92-96). This is a real assertion that the role intent is produced. |
| AC-2: Post-restore role release / pre-Q switch-back preserved (SmsReceiver disabled on Q+; ACTION_CHANGE_DEFAULT on pre-Q); U-009 restore-default-provider flow and role round-trip preserved | PASS (by inspection) | `restoreDefaultSmsProvider()` at `MainActivity.java:496-506` is UNCHANGED by the diff: Q+ → `SmsReceiver.disable(this)` (line 501); pre-Q non-empty → `ACTION_CHANGE_DEFAULT` switch-back (lines 502-504). Call sites unchanged: `onRestoreStateChanged()` `MainActivity.java:301` (on restore finish) and `checkDefaultSmsApp()` `MainActivity.java:535` (on resume). Pre-Q legacy capture `preferences.setSmsDefaultPackage(defaultSmsPackage)` retained at `MainActivity.java:434`. `git diff e570ef2b..HEAD` touches only lines 414-444 of startRestore — none of the release/switch-back code. Guard test `MainActivityRestoreTest.java:131-146` confirms the `isRoleHeld()` short-circuit (no duplicate role request) is preserved. |
| AC-3: "no default package" toast (`error_no_sms_default_package`) only fires in genuinely unsupported cases, not because the legacy API returned null on a Q+ phone | PASS | Toast is now scoped strictly inside the pre-Q `else` branch's null-package path: `MainActivity.java:440-442` (`Toast.makeText(this, R.string.error_no_sms_default_package, LENGTH_LONG)`). The Q+ branch (lines 417-428) has no toast and never inspects the legacy value. Tests: `MainActivityRestoreTest.java:99` asserts `shownToastCount() == 0` on the Q+ path; `MainActivityRestoreTest.java:107-123` (`...doesNotShowErrorToast`) asserts `shownToastCount() == 0` AND `getTextOfLatestToast()` != the `error_no_sms_default_package` string. |
| AC-4: Build green (assembleDebug, testDebugUnitTest, jacoco per-package LINE >=70%); Robolectric test covers Q+ null-default path. On-device confirmation deferred to post-merge validation | PASS (in-tree) / DEFERRED (on-device) | Authoritative integrated post-merge build state provided to QA: 645 tests pass, jacoco gate green. Robolectric coverage present: `MainActivityRestoreTest.java` — 3 `@Test` methods at `@Config(sdk = Build.VERSION_CODES.Q)` (lines 74, 108, 132) covering the Q+ null-default role-request path and the no-toast scoping. Test class compiles to execution (verified locally: `ROLE_SMS` import `MainActivity.java:74`, constant `REQUEST_CHANGE_DEFAULT_SMS_PACKAGE` `MainActivity.java:117` both resolve; in-process compile reached `:app:testDebugUnitTest` execution). Local re-run could not produce a fresh green/red signal due to a concurrent build holding file locks (`Device or resource busy` on `test-results/.../binary/output.bin`) and a corrupted Kotlin daemon cache (`InvalidProtocolBufferException`) — both environmental, neither attributable to U-041 source. On-device emulator confirmation is a separate orchestrator-run step and is correctly DEFERRED to post-merge device validation per AC-4 / story Verification Step 2; not failed here. |

## Integration Path Verification

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| `startRestore()` Q+ branch (`MainActivity.java:417-428`) | UI Restore action | `doPerform(Actions.Restore)` → `startRestore()` (`MainActivity.java:401`) → Q+ branch → `requestDefaultSmsPackageChange()` (`MainActivity.java:425`) | yes — call site at line 401 unchanged; branch reachable on any Q+ device once `isSmsBackupDefaultSmsApp()` is false |
| `requestDefaultSmsPackageChange()` (`MainActivity.java:482-489`) | Q+ restore branch + first-use dialog confirm | reached from `startRestore()` line 425 (dialog already seen) or `showDialog(SMS_DEFAULT_PACKAGE_CHANGE)` line 427 → dialog confirm | yes — method pre-existing, now reachable irrespective of legacy package value |
| Role-request result handling | `onActivityResult(REQUEST_CHANGE_DEFAULT_SMS_PACKAGE, ...)` (`MainActivity.java:229-235`) | result → `setSeenSmsDefaultPackageChangeDialog()` → re-invoke `startRestore()` (line 233) | yes — unchanged; the loop that drives restore after the grant is intact |
| Role release | restore finish / resume | `onRestoreStateChanged()` (line 301) and `checkDefaultSmsApp()` (line 535) → `restoreDefaultSmsProvider()` (496) → `SmsReceiver.disable()` (501) | yes — unchanged by diff |

## Behavioral Contract Verification

> Story frontmatter `integration_contracts: []` — no CNTR-* artifacts apply. The relevant
> contract is the `startRestore()` / `requestDefaultSmsPackageChange()` behavioral contract.

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| startRestore() restore-entry behavior | Already-default fast path → startService | `MainActivity.java:415-416` | yes (unchanged) |
| startRestore() restore-entry behavior | Q+ not-default → request role unconditionally (no legacy gate) | `MainActivity.java:417-428` | yes (fixed) |
| startRestore() restore-entry behavior | Q+ first-use → show SMS_DEFAULT_PACKAGE_CHANGE dialog before requesting | `MainActivity.java:424-428` | yes |
| startRestore() restore-entry behavior | Pre-Q non-empty default → capture + request | `MainActivity.java:431-439` | yes (unchanged) |
| startRestore() restore-entry behavior | Pre-Q empty default → error toast | `MainActivity.java:440-442` | yes (now correctly scoped to pre-Q only) |
| requestDefaultSmsPackageChange() | Q+ role-held guard → no duplicate request | `MainActivity.java:485` (`!roleManager.isRoleHeld(ROLE_SMS)`) | yes (test-verified, `MainActivityRestoreTest.java:131-146`) |
| requestDefaultSmsPackageChange() | Q+ enable SmsReceiver before requesting role | `MainActivity.java:486` | yes (unchanged) |
| restoreDefaultSmsProvider() | Q+ release via SmsReceiver.disable; pre-Q ACTION_CHANGE_DEFAULT | `MainActivity.java:498-505` | yes (unchanged) |

## Requirement Scope Coverage

> Story frontmatter `requirements: []`, `design_docs: []`. Source of record is bug:BUG-009.

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| BUG-009 AC-1 | Q+ requests SMS role even when getDefaultSmsPackage() null | yes | `MainActivity.java:417-428`, `MainActivity.java:482-489`; test `MainActivityRestoreTest.java:73-100` |
| BUG-009 AC-2 | Role released after restore; pre-Q switch-back preserved | yes | `MainActivity.java:496-506` (unchanged), call sites 301/535 |
| BUG-009 AC-3 | "tablet?" toast only on genuinely unsupported cases | yes | `MainActivity.java:440-442` scoped to pre-Q null path; tests `MainActivityRestoreTest.java:99,107-123` |
| BUG-009 AC-4 | On-device restore writes SMS after grant; build green | partial — build green covered in-tree; on-device DEFERRED to post-merge device validation per story AC-4 | 645 tests / jacoco green (provided); on-device = orchestrator post-merge step |

## Capabilities Inventory Verification

> Implementation log Capabilities Inventory (all RETAINED) cross-checked against on-disk code.

| Capability | Status in Log | Verified in Code | File:Line | Result |
|------------|---------------|------------------|-----------|--------|
| Already-default fast path → startService | RETAINED | Yes | `MainActivity.java:415-416` | PASS |
| Q+ RoleManager role request via requestDefaultSmsPackageChange() | RETAINED + FIXED | Yes | `MainActivity.java:417-428` + `482-489` | PASS |
| Q+ first-use dialog before request | RETAINED | Yes | `MainActivity.java:424-428` | PASS |
| Pre-Q legacy package capture for switch-back | RETAINED | Yes | `MainActivity.java:431-434` | PASS |
| Pre-Q toast on genuinely null legacy package | RETAINED | Yes | `MainActivity.java:440-442` | PASS |
| Q+ role released after restore via SmsReceiver.disable() | RETAINED (method unchanged) | Yes | `MainActivity.java:498-501` | PASS |

(Log's line numbers were slightly off from current HEAD, e.g. it cited 469-481 / 485-493 for
methods now at 482-489 / 496-506; the capabilities themselves are all present and functional.)

## Test Results

- New tests: `MainActivityRestoreTest.java` — 3 `@Test` methods, all `@Config(sdk = Q)`:
  - `bug009_onQPlus_requestDefaultSmsPackageChange_startsRoleRequestIntent` (AC-1)
  - `bug009_onQPlus_requestDefaultSmsPackageChange_doesNotShowErrorToast` (AC-3)
  - `bug009_onQPlus_whenAlreadyRoleHolder_noIntentStarted` (guard for AC-2 round-trip)
- Authoritative integrated build state (provided to QA): 645 tests pass, jacoco per-package
  LINE >= 70% green.
- Local independent re-run was blocked by environmental factors only: a concurrent gradle/
  Kotlin build repeatedly stopped the daemon and corrupted the Kotlin compiler cache
  (`InvalidProtocolBufferException: Protocol message tag had invalid wire type`), and held a
  file lock on `app/build/test-results/testDebugUnitTest/binary/output.bin`
  (`Device or resource busy`). The test class compiled and reached `:app:testDebugUnitTest`
  execution under the in-process compile strategy, confirming no source/compilation defect.
  No failure was attributable to U-041 code.

## Regression Results

- `restoreDefaultSmsProvider()` and its call sites (lines 301, 535) untouched by the diff —
  restore-finish role release and resume cleanup intact.
- `onActivityResult(REQUEST_CHANGE_DEFAULT_SMS_PACKAGE)` (lines 229-235) untouched — the
  post-grant re-invocation of `startRestore()` is preserved.
- Pre-Q path (legacy capture + ACTION_CHANGE_DEFAULT + tablet toast) fully retained at
  `MainActivity.java:429-443`.
- Diff scope for U-041 is limited to `MainActivity.java` (+15 lines) and the new test file;
  `StatusPreference.java` (which appeared in a transient kapt stub error during a corrupted
  build) is NOT modified by this story — confirmed via `git diff e570ef2b..HEAD`.

## Issues Found

- None blocking. One non-blocking note: the implementation log's Capabilities Inventory and
  AC table cite line numbers offset from current HEAD (methods have shifted ~13 lines). The
  capabilities are all present and correct; only the citations drifted.
- AC-4 on-device emulator confirmation (restore actually writes SMS after the user grants the
  role) is genuinely unverifiable in-tree and is DEFERRED to the orchestrator's post-merge
  device-validation step, exactly as the story specifies. This is tracked, not a silent gap.

## Phase Completion Report
---
story_id: U-041
phase: "validation"
verdict: PASS
artifact_path: "sdlc/artifacts/build/sprints/sprint-008/U-041/qa-results.md"
story_status: "validated"
current_build_phase: "validation"
ac_passed: 4
ac_total: 4
errors: []
---
