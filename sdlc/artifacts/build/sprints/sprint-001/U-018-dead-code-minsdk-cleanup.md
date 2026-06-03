---
type: story
status: done
sprint: "000001"
artifact_type: user-story
priority: medium
complexity: low
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-006
design_docs:
  - DES-MODERNIZATION-006
integration_contracts: []
dependencies:
  - U-006
  - U-003
change_records: []
platforms: []
tags:
  - dead-code
  - minsdk
  - cleanup
  - calendar
  - status-preference
gate_additions: []
id: U-018
title: 'Dead-Code Removal and minSdk Cleanup: delete CalendarAccessorPre40, collapse sub-21 Build.VERSION guards, implement StatusPreference rotation state'
pipeline: ''
domain: modernization
requirement_source: authored
---

# U-018: Dead-Code Removal and minSdk Cleanup — delete CalendarAccessorPre40, collapse sub-21 Build.VERSION guards, implement StatusPreference rotation state

## Story

As a maintainer of the SMS Backup+ codebase,
I want to delete `CalendarAccessorPre40.java` and its dead pre-ICS factory branch, collapse the two always-true inline `Build.VERSION` guards in `DataType.java` and `TokenRefresher.java`, verify `minSdkVersion 21` is set, and implement the stubbed `StatusPreference` instance-state save/restore using `Preference.BaseSavedState`,
so that provably-dead pre-21 compatibility shims no longer burden code readers and future migration units, and the settings screen no longer flashes to idle on screen rotation while a backup or restore is in progress.

## Acceptance Criteria

- [ ] AC-1: `calendar/CalendarAccessorPre40.java` is deleted in its entirety. The `if (sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH)` branch in `CalendarAccessor.Get.instance(ContentResolver)` (`calendar/CalendarAccessor.java`) is removed, along with the `sdkVersion` local variable, the `Build.VERSION.SDK_INT` assignment, and the `Build.VERSION_CODES` import. The `Get.instance` method signature (`public static CalendarAccessor instance(ContentResolver resolver)`) is preserved unchanged so that `BackupTask.java` (call site 1) and `activity/fragments/AdvancedSettings.java` (call site 2) require no source edits and compile without modification against the collapsed factory.
- [ ] AC-2: `app/build.gradle` `defaultConfig` block declares `minSdkVersion 21`. No `build.gradle` file in the project contains `minSdkVersion 14` or any value below `21`. Verified by `grep -r "minSdkVersion" .` returning only `21`.
- [ ] AC-3: The always-true `SDK_INT >= JELLY_BEAN` guard in `mail/DataType.java:21` is collapsed — the `>= 16` arm is retained as unconditional code and the dead `else` branch is deleted. The always-true `SDK_INT >= 14` guard in `auth/TokenRefresher.java:82` is collapsed — the `>= 14` arm is retained as unconditional code and the dead `else` branch is deleted. Neither file's behavior on API 21+ changes in any way.
- [ ] AC-4: The KITKAT (API 19) guards in `SmsRestoreService.java:67,163`, `MainActivity.java:365`, and `SmsReceiver.java:50` are left entirely untouched. `preferences/AuthMode.java`'s `XOAUTH` constant is left entirely untouched. `calendar/CalendarAccessor.java`'s interface declaration (`enableSync`, `addEntry`, `getCalendars` methods at approximately lines 17–37) is left entirely untouched.
- [ ] AC-5: `activity/StatusPreference.java` implements `onSaveInstanceState()` and `onRestoreInstanceState(Parcelable)` using an inner `SavedState extends Preference.BaseSavedState` class. `SavedState` carries: `CharSequence statusText`; `int statusColor` (sourced from a new `currentStatusColor` tracking field updated at every `setTextColor` call); `CharSequence detailsText`; `int progress`, `int max`, `boolean indeterminate` (from `progressBar`); and `int iconKind` (ordinal sourced from a new `currentIconKind` tracking field — four discriminator values: IDLE, DONE, ERROR, SYNCING — updated at every `setImageDrawable` call). `SavedState` implements `writeToParcel` and provides a `Parcelable.Creator<SavedState>` named `CREATOR`.
- [ ] AC-6: `StatusPreference.onBindViewHolder` applies the `restoredState` snapshot — setting statusLabel text and color, syncDetailsLabel text, progressBar progress/max/indeterminate, and the status icon drawable resolved from the `iconKind` discriminator — *instead of* calling `idle()` when `restoredState != null`, then clears `restoredState` to null. When `restoredState` is null (first bind, no rotation) the method calls `idle()` as today. The `restoredState` field is an instance field of type `SavedState` initialized to null.
- [ ] AC-7: `grep -rn "CalendarAccessorPre40" app/src` returns zero matches (no production source, no test source, no import, no comment reference to the class name).
- [ ] AC-8: The full Robolectric unit-test suite (`./gradlew test`) passes after all changes. No previously-passing test is broken. `CalendarAccessorPost40Test` and `CalendarSyncerTest` (which mocks the `CalendarAccessor` interface) both pass unmodified.
- [ ] AC-9: A Robolectric test for `StatusPreference` instance-state round-trip is added (new test method or new test class in `app/src/test/`). The test: (a) constructs a `StatusPreference`, drives it into a non-idle state by invoking `backupStateChanged` or `restoreStateChanged` with a non-idle `BackupState`/`RestoreState`; (b) calls `onSaveInstanceState()` and writes the returned `Parcelable` to a `Parcel`, then reads it back via `SavedState.CREATOR.createFromParcel` to exercise the full `Parcelable` round-trip; (c) constructs a fresh `StatusPreference`, calls `onRestoreInstanceState(restoredParcelable)` then simulates `onBindViewHolder` by calling it or asserting the field state; (d) asserts that statusLabel text, statusColor, detailsText, progress, max, indeterminate, and iconKind match the pre-rotation values; (e) asserts `idle()` was not applied over the restored values (status label text is not the idle-state string).

### Integration Criteria

Not applicable. This story introduces no new cross-component boundary. `CalendarAccessor.Get.instance(ContentResolver)` preserves its signature; the two call sites compile unchanged. `StatusPreference.SavedState` is internal to `StatusPreference` and rides the standard `Preference` lifecycle protocol. No registry, dispatch map, or module boundary is modified.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/calendar/CalendarAccessorPre40.java` | 125-line pre-Android-4.0 calendar accessor writing to the legacy `content://com.android.calendar` provider; constructed only inside the dead `< ICE_CREAM_SANDWICH` factory branch | Delete file entirely |
| `app/src/main/java/com/zegoggles/smssync/calendar/CalendarAccessor.java` | `Get.instance` contains a `if (sdkVersion < ICE_CREAM_SANDWICH)` branch constructing `CalendarAccessorPre40`; the `else` constructs `CalendarAccessorPost40`; also declares the live `CalendarAccessor` interface | Remove the dead `if/else` dispatch: delete the `sdkVersion` local, the `Build` assignment, and the `Pre40` then-branch; collapse to unconditional `calendarAccessor = new CalendarAccessorPost40(resolver)`. Interface (lines 17–37) is untouched |
| `app/src/main/java/com/zegoggles/smssync/mail/DataType.java` | Line 21 guards behavior behind `if (SDK_INT >= JELLY_BEAN)` — always true at minSdk 21 | Remove the `else` branch; keep the `>= 16` arm as unconditional code; delete the condition itself |
| `app/src/main/java/com/zegoggles/smssync/auth/TokenRefresher.java` | Line 82 guards behavior behind `if (SDK_INT >= 14)` — always true at minSdk 21 | Remove the `else` branch; keep the `>= 14` arm as unconditional code; delete the condition itself |
| `app/build.gradle` | `defaultConfig { minSdkVersion 14 }` at line 14 | Change to `minSdkVersion 21` |
| `app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java` | `onSaveInstanceState()` and `onRestoreInstanceState(Parcelable)` are stubbed with `// TODO implement` comments; `onBindViewHolder` always calls `idle()` on first bind after rotation, causing a visible state flash | Implement both methods with a `SavedState extends Preference.BaseSavedState` inner class; add `currentStatusColor` and `currentIconKind` tracking fields; update `onBindViewHolder` to apply restored state instead of `idle()` when `restoredState != null` |
| `app/src/test/java/com/zegoggles/smssync/activity/StatusPreferenceTest.java` (new) | Does not exist | New Robolectric test class covering the `SavedState` Parcel round-trip and `onBindViewHolder` restored-state path (AC-9) |

## Existing Behavior to Preserve

- `CalendarAccessor.Get.instance(ContentResolver resolver)` method signature is preserved exactly; `BackupTask.java` and `AdvancedSettings.java` call sites compile against it without any source modification.
- `CalendarAccessor` interface methods (`enableSync`, `addEntry`, `getCalendars`) and their signatures are unchanged; `CalendarSyncer.java` and `CalendarSyncerTest.java` continue to bind to the interface without modification.
- `CalendarAccessorPost40.java` source is unchanged byte-for-byte; its `addEntry`, `getCalendars`, and `enableSync` implementations are the only surviving paths through the calendar subsystem and must not be touched.
- On API 21+, all calendar sync behavior (add entry, get calendars, enable sync) is identical to the pre-change behavior; the factory previously selected `CalendarAccessorPost40` unconditionally at runtime on all installed devices, and it continues to do so.
- `mail/DataType.java` behavior on the `>= JELLY_BEAN` arm and `auth/TokenRefresher.java` behavior on the `>= 14` arm are semantically identical after the collapse — only the dead `else` branches are removed.
- `StatusPreference` renders `idle()` on first bind when there is no saved state — the no-rotation path is unchanged.
- `StatusPreference` responds to `backupStateChanged` and `restoreStateChanged` calls identically to today during an active backup/restore session; only the rotation/configuration-change path changes (it no longer flashes to idle).
- The KITKAT guards at `SmsRestoreService.java:67,163`, `MainActivity.java:365`, and `SmsReceiver.java:50` are left entirely untouched.
- `preferences/AuthMode.XOAUTH` is not removed or deprecated further.
- `warningsAsErrors true` and `-Werror -Xlint:deprecation` (from `app/build.gradle:41` and `:75`) must remain satisfied after the changes; collapsing the dead `else` arms removes, rather than introduces, dead-code warnings.

## Verification Steps

1. **AC-1 — Pre40 deleted, factory collapsed:** Run `ls app/src/main/java/com/zegoggles/smssync/calendar/CalendarAccessorPre40.java`; confirm the file does not exist. Open `calendar/CalendarAccessor.java` and confirm the `Get.instance` method body contains no `if (sdkVersion < ...)` branch, no `sdkVersion` local, no `ICE_CREAM_SANDWICH` reference, and no `CalendarAccessorPre40` reference. Confirm `Get.instance(ContentResolver resolver)` signature is present and the method returns a `CalendarAccessorPost40` unconditionally via the existing null-check cache.
2. **AC-2 — minSdkVersion 21:** Run `grep -r "minSdkVersion" .` from the repo root. Confirm every result shows `21`; no `14` appears.
3. **AC-3 — DataType and TokenRefresher guards collapsed:** Open `mail/DataType.java` and confirm line 21 (approximately) contains no `SDK_INT` comparison and no `else` block. Open `auth/TokenRefresher.java` and confirm the `SDK_INT >= 14` guard at approximately line 82 is absent and no `else` block remains.
4. **AC-4 — KITKAT/XOAUTH/interface untouched:** Run `grep -n "KITKAT" app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java`; confirm lines 67 and 163 still contain KITKAT guards. Run `grep -n "KITKAT" app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java`; confirm line 365 still contains a KITKAT guard. Run `grep -n "XOAUTH" app/src/main/java/com/zegoggles/smssync/preferences/AuthMode.java`; confirm the constant is present. Open `calendar/CalendarAccessor.java`; confirm the interface methods `enableSync`, `addEntry`, and `getCalendars` are present and unchanged.
5. **AC-5 and AC-6 — StatusPreference SavedState:** Open `activity/StatusPreference.java`. Confirm the inner class `SavedState extends Preference.BaseSavedState` is present with fields `statusText`, `statusColor`, `detailsText`, `progress`, `max`, `indeterminate`, `iconKind`. Confirm `onSaveInstanceState()` populates a `SavedState` from the tracking fields. Confirm `onRestoreInstanceState(Parcelable state)` stores the cast `SavedState` into `this.restoredState`. Confirm `onBindViewHolder` checks `restoredState != null` before calling `idle()` and applies the snapshot when present, then sets `restoredState = null`.
6. **AC-7 — Grep gate:** Run `grep -rn "CalendarAccessorPre40" app/src`. Confirm zero output.
7. **AC-8 — Full suite green:** Run `./gradlew test` from the repo root. Confirm exit code 0 with no test failures. Inspect the test report under `app/build/reports/tests/` and confirm `CalendarAccessorPost40Test` and `CalendarSyncerTest` both show all tests passing.
8. **AC-9 — StatusPreference rotation test:** Run `./gradlew test --tests "*.StatusPreferenceTest"`. Confirm the test class exists and all test methods pass. Confirm the test output shows a Parcel round-trip was exercised (the test does not merely call `onSaveInstanceState` and discard the result).
9. **End-to-end rotation (manual acceptance):** Install a debug build on an API 21+ device or emulator. Start a backup. While the backup row shows a non-idle state in the settings UI, rotate the device. Confirm the status row continues to display the backup state (progress bar, status text, icon) after rotation without flashing to the idle icon or clearing the status label.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android / Java | All changes: file deletion, `CalendarAccessor.java` branch collapse, `DataType.java` and `TokenRefresher.java` guard collapse, `app/build.gradle` minSdk edit, `StatusPreference.java` `SavedState` implementation, new `StatusPreferenceTest.java` | Developer |

## Technical Context

**Why `Get.instance` signature is preserved rather than inlining `CalendarAccessorPost40` at the two call sites:** DES-MODERNIZATION-006 §1 (ADR-006-2) explicitly defers that inlining to MU-007 (Hilt DI), where the accessor becomes constructor-injected and the static singleton is removed as part of the DI graph. Doing it here would (a) change two files outside the minimal dead-code scope, (b) duplicate work MU-007 must redo, and (c) risk the static-singleton identity semantics (`calendarAccessor` is cached across calls — both call sites currently share the same instance). The `Get.instance` signature is the contract; the factory body is the implementation detail.

**Why `onRestoreInstanceState` cannot paint views directly:** `StatusPreference` extends `androidx.preference.Preference`. The `Button`/`TextView`/`ProgressBar`/`ImageView` fields in `StatusPreference` are null until `onBindViewHolder` runs — `Preference` views are recycled by the `RecyclerView`-backed `PreferenceFragmentCompat` and bound lazily. Calling `statusLabel.setText(...)` in `onRestoreInstanceState` would NPE or be a no-op depending on bind ordering. The design stores the snapshot in `restoredState` and applies it in `onBindViewHolder`, which is the authoritative moment when views are guaranteed non-null. Clearing `restoredState` after the first bind prevents the snapshot from being re-applied on subsequent recycler rebinds.

**The `currentStatusColor` and `currentIconKind` tracking fields:** The existing code sets color and icon imperatively via live `Drawable`/`int` values in `setViewAttributes` and the `finished*`/`idle` helpers; the current state is not recorded anywhere serializable. The implementation must introduce these two fields and update them at every point that currently calls `statusLabel.setTextColor(...)` and `statusIcon.setImageDrawable(...)`. The `iconKind` discriminator maps to the four existing static drawables (`idle`, `done`, `error`, `syncing`); the resolver in `onBindViewHolder` calls `setImageDrawable` with the appropriate static based on the ordinal.

**Scope boundaries enforced by this story:**
- KITKAT guards (`SmsRestoreService.java:67,163`, `MainActivity.java:365`, `SmsReceiver.java:50`) are explicitly out of scope. These gate restore write-permission and default-SMS-app compat logic; their `else` branches carry live-looking restore logic that requires careful review beyond the scope of a low-risk cleanup unit.
- `preferences/AuthMode.XOAUTH` is live for legacy users and gated on MU-004 credential migration (REQ-MODERNIZATION-004). It must not be touched here.
- The `try/catch` wrapping `CalendarAccessorPost40` construction in `Get.instance` may be retained or removed — `new CalendarAccessorPost40(resolver)` throws no checked exception, so the `catch (Exception e) → IllegalStateException` is dead defensively, but removing it is cosmetic and not required by the acceptance criteria.

**Key file locations (all paths relative to repo root):**

| File | Lines of interest |
|------|------------------|
| `app/src/main/java/com/zegoggles/smssync/calendar/CalendarAccessorPre40.java` | Entire file — deletion target (125 lines) |
| `app/src/main/java/com/zegoggles/smssync/calendar/CalendarAccessor.java` | `:49-51` dead `if (sdkVersion < ICE_CREAM_SANDWICH)` branch; `:17-37` live interface (untouched) |
| `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` | `:78` call site 1: `CalendarAccessor.Get.instance(service.getContentResolver())` |
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java` | `:314` call site 2: `CalendarAccessor.Get.instance(getContext().getContentResolver())` |
| `app/src/main/java/com/zegoggles/smssync/mail/DataType.java` | `:21` always-true `SDK_INT >= JELLY_BEAN` guard |
| `app/src/main/java/com/zegoggles/smssync/auth/TokenRefresher.java` | `:82` always-true `SDK_INT >= 14` guard |
| `app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java` | `:131-140` stubbed `onSaveInstanceState`/`onRestoreInstanceState`; `:110-128` `onBindViewHolder` (add `restoredState` check here); `setViewAttributes` and `finished*`/`idle` helpers (add `currentStatusColor`/`currentIconKind` update points here) |
| `app/build.gradle` | `:14` `minSdkVersion 14` → `21` |

## Supporting Documentation

- `REQ-MODERNIZATION-006` §Acceptance Criteria (ACs 1–5), §Constraints, §Context
- `DES-MODERNIZATION-006` §1 (CalendarAccessorPre40 deletion and factory collapse, ADR-006-1, ADR-006-2), §2 (StatusPreference SavedState design pattern, critical sequencing note), §3 (guard collapse table, KITKAT/XOAUTH scope boundaries), §Integration Design (dependency on DES-MODERNIZATION-001, call-site preservation contract), §Design Validation (V1–V4 verification strategy)

## Integration Contract References

None. This story introduces no new cross-component boundary and changes no existing producer/consumer contract. The `CalendarAccessor` interface and all its consumers are unchanged. `StatusPreference.SavedState` is internal to `StatusPreference` and participates only in the standard `Preference` instance-state protocol. No `CNTR-*` artifact governs or is produced by this story.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Preconditions: U-006 Gate G2 green (characterization test baseline established so the full-suite green gate in AC-8 is meaningful) and U-003 minSdk 21 landed (so the Pre40 deadness is provably correct by construction, not merely safe in practice). Do not land this story before both preconditions are satisfied.
