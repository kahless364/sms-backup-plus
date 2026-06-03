---
type: story
status: done
sprint: '000001'
artifact_type: user-story
priority: critical
complexity: medium
parallel_eligible: true
iteration: 1
requirements:
  - REQ-MODERNIZATION-002
design_docs:
  - DES-MODERNIZATION-002
integration_contracts: []
dependencies:
  - U-006
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-007
title: Rewrite AuthPreferences.migrate() to eliminate silent trust-all write and clear stale downgrade
pipeline: ''
domain: modernization
requirement_source: authored
updated_at: '2026-06-02T22:34:47.616Z'
resolution: done
---

# U-007: Rewrite AuthPreferences.migrate() to Eliminate Silent Trust-All Write and Clear Stale Downgrade

## Story

As a developer maintaining the SMS Backup+ codebase,
I want `AuthPreferences.migrate()` rewritten so it can never write `SERVER_TRUST_ALL_CERTIFICATES = true` under any input, actively clears any stale `true` value left by the old migration, normalizes the `+ssl`/`+tls` protocol strings without a coupled security downgrade, and raises a one-time notice flag for every affected user,
so that the silent MITM-enabling migration path is eliminated on first launch of the fixed build, every previously-downgraded user has validated TLS restored immediately, and the notice mechanism informs affected users how to re-enroll a pinned certificate if their server requires it.

## Acceptance Criteria

- [ ] **AC-1 — No branch of migrate() writes SERVER_TRUST_ALL_CERTIFICATES = true**

  Given the rewritten `AuthPreferences.migrate()` method,
  when the method executes with `SERVER_PROTOCOL` set to any of the following stored values — `+ssl`, `+tls`, `ssl`, `tls`, the empty string, an arbitrary custom value such as `"starttls"`, or no value at all — and `useXOAuth()` returns `false`,
  then `SharedPreferences` never receives a `putBoolean(SERVER_TRUST_ALL_CERTIFICATES, true)` call on any execution path through `migrate()`; a unit test parametrized over all of the above protocol values asserts this by reading `prefs.getBoolean(SERVER_TRUST_ALL_CERTIFICATES, false)` after `migrate()` returns and confirming it is `false` in every case.

- [ ] **AC-2 — Stale SERVER_TRUST_ALL_CERTIFICATES = true is actively cleared to false**

  Given a device whose `SharedPreferences` contain `SERVER_TRUST_ALL_CERTIFICATES = true` as a result of the old `migrate()` having previously executed (regardless of the current `SERVER_PROTOCOL` value),
  when the rewritten `migrate()` executes,
  then `SharedPreferences` receives `putBoolean(SERVER_TRUST_ALL_CERTIFICATES, false)`, and after `migrate()` returns, `prefs.getBoolean(SERVER_TRUST_ALL_CERTIFICATES, false)` returns `false`; a unit test seeds the preference to `true` before calling `migrate()` and asserts the `false` value afterward.

- [ ] **AC-3 — transport_security_notice_pending is set to true for the affected cohort**

  Given a device that satisfies either or both of the following conditions: (a) the stored `SERVER_PROTOCOL` at the time `migrate()` runs is `+ssl` or `+tls`; (b) `SERVER_TRUST_ALL_CERTIFICATES` was `true` before `migrate()` ran,
  when the rewritten `migrate()` executes,
  then `SharedPreferences` receives `putBoolean("transport_security_notice_pending", true)` exactly once per execution, and after `migrate()` returns, `prefs.getBoolean("transport_security_notice_pending", false)` returns `true`; unit tests cover each sub-case independently (stale-true only, legacy-protocol only, and both simultaneously).

- [ ] **AC-4 — transport_security_notice_pending is NOT set for unaffected users**

  Given a device whose stored `SERVER_PROTOCOL` is any value other than `+ssl` or `+tls` (including `ssl+`, `tls+`, `ssl`, `tls`, an arbitrary custom value, or absent) AND whose `SERVER_TRUST_ALL_CERTIFICATES` is `false` or absent,
  when the rewritten `migrate()` executes,
  then `SharedPreferences` does not receive any `putBoolean("transport_security_notice_pending", true)` call, and `prefs.getBoolean("transport_security_notice_pending", false)` remains `false` after `migrate()` returns; a unit test parametrized over all listed unaffected protocol values asserts this.

- [ ] **AC-5 — Protocol normalization is preserved without a coupled security change**

  Given a device whose stored `SERVER_PROTOCOL` is `+ssl` or `+tls`,
  when the rewritten `migrate()` executes,
  then `SERVER_PROTOCOL` is updated to `+ssl+` or `+tls+` respectively (appending `+` to the existing value, preserving the existing normalization logic), and `SERVER_TRUST_ALL_CERTIFICATES` is not written as `true`; a unit test seeds each legacy protocol value, calls `migrate()`, and asserts both the normalized protocol value and the absence of a `true` trust-all write.

- [ ] **AC-6 — useXOAuth() true causes early return with no preference writes**

  Given a device for which `useXOAuth()` returns `true`, regardless of the stored `SERVER_PROTOCOL` or `SERVER_TRUST_ALL_CERTIFICATES` values,
  when the rewritten `migrate()` executes,
  then `migrate()` returns immediately without writing any preference key; a unit test asserts that the `SharedPreferences.Editor` receives zero `put*` calls when `useXOAuth()` is `true`.

- [ ] **AC-7 — migrate() uses apply(), not commit()**

  Given the rewritten `migrate()` implementation,
  when the implementation is read by a developer,
  then every `SharedPreferences.Editor` flush call in `migrate()` uses `apply()`, not `commit()`; a code review or static assertion confirms the absence of `.commit()` in the method body; the commit message or PR description references DES-MODERNIZATION-002 Decision 6 ("ARCH-017: apply(), not commit(), for a launch-path write").

- [ ] **AC-8 — U-006 characterization tests are green on master before this story's migrate() changes are merged (hard gate)**

  Given the three `migrate()` characterization test cases backfilled by U-006 in `app/src/test/java/com/zegoggles/smssync/preferences/AuthPreferencesTest.java`,
  when the test suite is executed against the unmodified production `AuthPreferences.migrate()` on the master branch,
  then all three characterization test cases pass and the CI run is green; a pull request that modifies `AuthPreferences.migrate()` is not merged until this CI green state is confirmed on master; the PR description must reference the U-006 characterization tests by name and link the passing CI run.

- [ ] **AC-9 — Already-validated TLS users, OAuth2 users, and users with no migration trigger experience no behavior change**

  Given a device whose `SERVER_PROTOCOL` is not `+ssl` or `+tls` (e.g., already-normalized values such as `ssl+`, `tls+`, plain `ssl`, plain `tls`, or custom values) AND whose `SERVER_TRUST_ALL_CERTIFICATES` is `false` or absent, and for which `useXOAuth()` returns `false`,
  when the rewritten `migrate()` executes,
  then the `SERVER_TRUST_ALL_CERTIFICATES` value is not altered, the `SERVER_PROTOCOL` value is not altered, no notice flag is written, and the existing `AuthPreferencesTest` cases covering these paths pass without regression; a developer running `./gradlew :app:testDebugUnitTest` from the repo root sees all pre-existing test cases green.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` | `migrate()` (lines 276-288) unconditionally writes `SERVER_TRUST_ALL_CERTIFICATES = true` for any `SERVER_PROTOCOL` matching `+ssl` or `+tls`, normalizes the protocol string, and calls `.commit()` | Rewrite: remove trust-all write; add stale-`true` clearing write; add `transport_security_notice_pending` flag for affected cohort; change `.commit()` to `.apply()` |
| `app/src/test/java/com/zegoggles/smssync/preferences/AuthPreferencesTest.java` | Contains U-006 characterization tests pinning the current (broken) behavior of `migrate()`; no tests for the corrected behavior | Add new unit tests for AC-1 through AC-9 covering every branch of the rewritten method |

## Existing Behavior to Preserve

- The `useXOAuth()` early-return guard at the top of `migrate()` must remain the first executed statement; OAuth2-configured users must not be affected by this story's changes.
- Protocol normalization: a stored value of `+ssl` must become `+ssl+`; a stored value of `+tls` must become `+tls+`; these string transformations are correct behavior and must survive the rewrite.
- All other `AuthPreferences` methods — `getServerProtocol()`, `isTrustAllCertificates()`, `getCredentials()`, `getServerAddress()`, `getServerPort()` — must be untouched and must behave identically before and after this story.
- `Preferences.migrate()` (`preferences/Preferences.java:294-296`) calls `new AuthPreferences(context).migrate()` on every `App.onCreate()`; this call chain must continue to compile and execute without error after the rewrite.
- The existing `BackupImapStoreTest` suite must remain green; this story does not touch `BackupImapStore.java` or any test outside `AuthPreferencesTest.java`.

## Verification Steps

1. **AC-1 (no trust-all write):** Run `./gradlew :app:testDebugUnitTest --tests "*AuthPreferencesTest*"` from the repo root. Confirm that the parametrized test covering `SERVER_PROTOCOL` values `["+ssl", "+tls", "ssl", "tls", "", "starttls", null]` passes for each value and that the test body asserts `prefs.getBoolean(AuthPreferences.SERVER_TRUST_ALL_CERTIFICATES, false) == false` after each `migrate()` call.

2. **AC-2 (stale-true cleared):** In the `AuthPreferencesTest` test for stale-true clearing, verify the test seeds `prefs.edit().putBoolean(SERVER_TRUST_ALL_CERTIFICATES, true).apply()` before calling `migrate()`, then asserts `prefs.getBoolean(SERVER_TRUST_ALL_CERTIFICATES, false) == false` afterward. Run the test and confirm it passes.

3. **AC-3 (notice flag set for affected cohort):** Run the three sub-case tests: (a) `SERVER_PROTOCOL = "+ssl"`, trust-all initially `false`; (b) `SERVER_PROTOCOL = "ssl+"` (unaffected protocol), trust-all initially `true`; (c) `SERVER_PROTOCOL = "+tls"`, trust-all initially `true`. For (a) and (b) and (c), assert `prefs.getBoolean("transport_security_notice_pending", false) == true` after `migrate()`. Confirm all three pass.

4. **AC-4 (notice flag NOT set for unaffected users):** Run the parametrized unaffected-user test covering `SERVER_PROTOCOL` values `["ssl+", "tls+", "ssl", "tls", "starttls", "", null]` with trust-all initially `false`. Assert `prefs.getBoolean("transport_security_notice_pending", false) == false` for every value. Confirm all pass.

5. **AC-5 (protocol normalization preserved):** Seed `SERVER_PROTOCOL = "+ssl"`, call `migrate()`, read `prefs.getString(SERVER_PROTOCOL, null)` and confirm it equals `"+ssl+"`. Repeat with `"+tls"` expecting `"+tls+"`. Confirm trust-all remains `false` in both cases.

6. **AC-6 (OAuth2 early return):** Seed any `SERVER_PROTOCOL` and `SERVER_TRUST_ALL_CERTIFICATES = true`, set up the mock/stub so `useXOAuth()` returns `true`, call `migrate()`, and use a `SharedPreferences.Editor` spy or shadow to assert zero `put*` calls were recorded. Confirm the test passes.

7. **AC-7 (apply not commit):** Open `AuthPreferences.java` and search for `.commit()` within the `migrate()` method body (IDE search or `grep -n "\.commit()" app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` from the repo root). Confirm zero matches. Confirm `.apply()` appears at the flush site.

8. **AC-8 (U-006 gate):** Before creating the PR that modifies `migrate()`, open the CI run for the current master HEAD commit. Navigate to the `testDebugUnitTest` job. Confirm the three `AuthPreferencesTest.migrate*` characterization tests added by U-006 are listed as passed. Paste the CI run URL into the PR description.

9. **AC-9 (no regression for unaffected users):** Run `./gradlew :app:testDebugUnitTest` from the repo root against the full test suite on the feature branch. Confirm zero test failures. Pay particular attention to any pre-existing `AuthPreferencesTest` cases and the full `BackupImapStoreTest` suite.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java) | Rewrite `AuthPreferences.migrate()` body; author new unit tests in `AuthPreferencesTest.java` | Developer |

## Technical Context

- **The broken code being replaced** is at `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` lines 276-288. The current body is reproduced verbatim in DES-MODERNIZATION-002 §Context. The critical defect is the unconditional `putBoolean(SERVER_TRUST_ALL_CERTIFICATES, true)` write — it enables the trust-all MITM path for every user who ever configured `+ssl` or `+tls`, without notice or consent.

- **The replacement body** is specified exactly in DES-MODERNIZATION-002 Decision 6. The developer must implement it as written. The structure is: (1) `useXOAuth()` early return guard; (2) read `SERVER_PROTOCOL` into a local variable; (3) determine `wasLegacyDowngradeProtocol` flag; (4) open `SharedPreferences.Editor`; (5) if `prefs.getBoolean(SERVER_TRUST_ALL_CERTIFICATES, false)` is `true`, write `false` and mark notice pending; (6) if `wasLegacyDowngradeProtocol`, write the normalized protocol and mark notice pending; (7) call `edit.apply()` — never `edit.commit()`.

- **The notice flag key** is `"transport_security_notice_pending"`. It is a boolean in default `SharedPreferences`. It is not presented to the user by this story; this story only writes the flag. The presentation layer (a subsequent story) reads the flag, shows the notice once, and writes `"transport_security_notice_shown" = true`. The analogous existing pattern is at `Preferences.java:247-253` (`sms_default_package_change_seen`).

- **`SERVER_TRUST_ALL_CERTIFICATES` key** is defined at `AuthPreferences.java:48` as the string constant `"server_trust_all_certificates"`. The developer must use the existing constant, not a string literal.

- **`SERVER_PROTOCOL` key** is accessed via `getServerProtocol()` which reads from `preferences` using the key at `AuthPreferences.java:42`. The developer must use `getServerProtocol()` to read the current value, not read the raw key directly, to maintain consistency with the rest of the class.

- **Sequencing gate (AC-8 is a hard merge gate):** This story may be developed in parallel with U-006 from day one but its migrate()-modifying commit must not be merged to master until U-006's three characterization tests are confirmed green on master CI. The developer must check CI on master before raising the merge/PR. This is not advisory — it is a hard process constraint from REQ-MODERNIZATION-002 AC-7, REQ Constraint 2, and DES-MODERNIZATION-002 §Design Validation.

- **This story's scope is strictly `migrate()` only.** It does not touch `isTrustAllCertificates()` (line 196), `BackupImapStore.java`, `ServiceBase.java`, or any UI. Those changes belong to U-008, U-009, and U-010. Touching those files in this story is out of scope and a constraint violation (REQ Constraint 4 coordination order: MU-003 first within `AuthPreferences.java`).

- **`apply()` vs `commit()` rationale:** The `migrate()` method runs on the main thread via `App.onCreate()`. `commit()` performs a synchronous `fsync` which blocks the main thread during every cold start. `apply()` is asynchronous and safe here because the write is idempotent — if the process is killed before `apply()` flushes, `migrate()` will re-execute on the next launch and produce the same result (DES-MODERNIZATION-002 Decision 6 Trade-offs; ARCH-017).

- **Test infrastructure:** U-006 (depends-on) will have already upgraded the Robolectric shadow to 4.12+ and confirmed `AuthPreferencesTest` compiles and runs. This story's new tests are written in the same file, same test class, and use the same `RobolectricTestRunner`/`@Config` established by U-006. No new test infrastructure setup is needed.

## Supporting Documentation

- REQ-MODERNIZATION-002 §Acceptance Criteria AC-5, AC-6, AC-7, AC-9, AC-10 — the requirement ACs this story directly satisfies
- REQ-MODERNIZATION-002 §Context §Active Vulnerability: Silent Security-Downgrade Migration — the ARCH-008 vulnerability description
- REQ-MODERNIZATION-002 §Constraints 2 and 4 — sequencing gates and AuthPreferences.java edit ordering
- DES-MODERNIZATION-002 §Design Decision 6 — the exact replacement method body, key properties, and AC-to-design-property mapping
- DES-MODERNIZATION-002 §Integration Design §Migration UX for already-downgraded users — the three-step UX path for the stale-`true` cohort
- DES-MODERNIZATION-002 §Trade-offs — rationale for `apply()` vs `commit()` and fail-closed behavior

## Integration Contract References

No integration contracts govern this story's scope. This story modifies only the `migrate()` method body inside `AuthPreferences.java` and its corresponding unit tests. The socket-factory boundary (CNTR-MODERNIZATION-TLSFACTORY-001) and pinned-cert enrollment boundary (CNTR-MODERNIZATION-PINENROLL-001) are consumed by U-008 and U-009 respectively, not by this story.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Not finalized. The artifact_path for this story is `sdlc/artifacts/stories/U-007-modernization-tls-migrate-rewrite-notice.md`. This story runs parallel to U-008 through U-010 from day one but its migrate()-modifying commit is gated on U-006's characterization tests being green on master CI (AC-8 hard gate).
