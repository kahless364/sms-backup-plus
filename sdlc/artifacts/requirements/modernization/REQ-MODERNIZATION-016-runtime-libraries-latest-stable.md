---
status: approved
type: nfr
artifact_type: requirement
related_requirements:
  - REQ-MODERNIZATION-015
related_stories: []
related_design_docs: []
traces_to:
  - EPIC-MODERNIZATION-006
change_records: []
id: REQ-MODERNIZATION-016
title: Runtime libraries on latest stable (WorkManager, Lifecycle, coroutines, preference, annotation, core-role)
domain: modernization
epic: EPIC-MODERNIZATION-006
priority: medium
notes: |
  Version figures are a 2026-11-12 snapshot, not targets. REQ-MODERNIZATION-015 (toolchain and
  minSdk 23) may need to land first if a library's minimum requires it. The retro file for
  sprint-017 was not searched successfully for BUG-005/009/016 references (no matches), so
  bug IDs are cited from the dispatch and the manifest comment only.
  Related contracts (from the draft): CNTR-MODERNIZATION-003, CNTR-MODERNIZATION-004.
updated_at: '2026-11-12T16:48:41.089Z'
---

# REQ-MODERNIZATION-016: Runtime libraries on latest stable

## Description

Bring every runtime library named below to the latest stable release, re-resolved on the date the
implementing story is planned. Remove the one remaining pre-release runtime dependency
(`androidx.core:core-role:1.0.0-beta01`). Confirm, without changing, the runtime libraries that are
already current. Preserve all user-visible backup, restore, scheduling, and default-SMS-role behavior.

"Latest stable" is the newest non-pre-release (no alpha/beta/rc) version on Google Maven or Maven
Central on the planning date; the planning date and source are recorded in the story. The figures
in the table are a 2026-11-12 snapshot, not frozen targets.

## Context

The build runs on JDK 25 (Gradle 9.5.1 / AGP 9.0.1) with 697 unit tests (2 skipped). The runtime
libraries are several minor versions behind (`app/build.gradle` lines 118-157;
`k9mail-vendored/build.gradle` line 114). These libraries carry load-bearing behavior:

- `BackupWorker.kt` and `RestoreWorker.kt` call `setForeground()` with
  `FOREGROUND_SERVICE_TYPE_DATA_SYNC`. `app/src/main/AndroidManifest.xml` (lines 141-144) overrides
  `androidx.work.impl.foreground.SystemForegroundService` with `foregroundServiceType="dataSync"`
  using `tools:replace` (BUG-016). A WorkManager upgrade can change the library's own manifest
  entry, so the merged manifest must be re-checked.
- The scheduler contract CNTR-MODERNIZATION-004, the secret-store contract CNTR-MODERNIZATION-003
  (EncryptedSharedPreferences), the default-SMS-role flow through core-role (BUG-009), and the
  `MainViewModel` lifecycle flows (BUG-005, no leak) all depend on these libraries.

## Requirements

### Version table (snapshot 2026-11-12)

| Coordinate | Current | Latest stable (snapshot) | Location |
|---|---|---|---|
| androidx.work:work-runtime-ktx / work-testing | 2.9.1 | 2.12.0 | `app/build.gradle` |
| androidx.lifecycle:lifecycle-runtime-ktx / lifecycle-viewmodel-ktx | 2.6.2 | 2.11.0 | `app/build.gradle` |
| org.jetbrains.kotlinx:kotlinx-coroutines-core / -android / -test | 1.7.3 | 1.11.0 | `app/build.gradle` |
| androidx.preference:preference | 1.1.0 | 1.2.1 | `app/build.gradle` |
| androidx.annotation:annotation | 1.1.0 (app), 1.7.1 (vendored) | 1.11.0 | `app/build.gradle`, `k9mail-vendored/build.gradle` |
| androidx.core:core-role | 1.0.0-beta01 | 1.1.0 (leaves pre-release) | `app/build.gradle` |

Already current, to be confirmed and not changed unless the planning-date re-resolution shows
otherwise: `androidx.security:security-crypto` 1.1.0, `junit:junit` 4.13.2, `jutf7` 1.0.0
(the vendored mail stack is retired by a separate requirement). The story must check
security-crypto's maintenance status at planning time and record the finding; this requirement
does not assert that the library is deprecated.

### Acceptance Criteria

- [ ] AC-1: For each coordinate in the table, the version in `app/build.gradle` and
  `k9mail-vendored/build.gradle` equals the latest stable version re-resolved on the story's
  planning date. The story records the date and source per coordinate.
- [ ] AC-2: No runtime or test dependency in `app/build.gradle` or `k9mail-vendored/build.gradle`
  has a version containing `alpha`, `beta`, or `rc`.
  Verified by: `grep -nE "alpha|beta|rc[0-9]" app/build.gradle k9mail-vendored/build.gradle`
  returns zero dependency lines.
- [ ] AC-3: The story records the planning-date version and maintenance-status finding for
  security-crypto, JUnit, and jutf7, and states "unchanged" or the new version for each.
- [ ] AC-4: `gradle/verification-metadata.xml` covers every new artifact, with entries added only
  through Gradle's writer (`--write-verification-metadata`). Dependency verification remains
  enforced, and `./gradlew :app:assembleDebug` succeeds on JDK 25.
- [ ] AC-5: `./gradlew :app:testDebugUnitTest` completes with zero failures and no more than 2
  skipped tests. Any test removed or rewritten for coroutine-test API changes (for example
  `runTest` or `TestDispatcher` behavior) is listed in the story with the replacement that covers
  the same behavior.
- [ ] AC-6: `./gradlew :app:jacocoTestCoverageVerification` passes with no per-package floor
  lowered (receiver at or above 50%, other packages at or above 70%).
- [ ] AC-7: Merged-manifest equivalence. Before the upgrade and after it, the merged manifest
  (`app/build/intermediates/merged_manifests/debug/`) is captured. The
  `androidx.work.impl.foreground.SystemForegroundService` entry still has
  `android:foregroundServiceType="dataSync"`, and the story lists every other difference between
  the two merged manifests (components, permissions, providers) with a justification for each.
- [ ] AC-8: Scheduler contract. Every test covering CNTR-MODERNIZATION-004 passes unmodified, or is
  modified only for a documented library API change with identical asserted behavior.
- [ ] AC-9: On a device or emulator, the default-SMS-role request flow (core-role) completes as
  before: the role request is raised and the granted and denied outcomes are handled. Results are
  recorded in the story (BUG-009 regression check).
- [ ] AC-10: `MainViewModel` flows collected through `lifecycleScope` / `repeatOnLifecycle` still
  stop collecting when the activity is stopped, and the BUG-005 no-leak tests pass.
- [ ] AC-11: Preference screens open, display stored values, and persist edits after the upgrade,
  verified on a device or emulator with results recorded.
- [ ] AC-12: Data-at-rest compatibility (CNTR-MODERNIZATION-003). Credentials stored by the
  pre-upgrade build in EncryptedSharedPreferences are read successfully, without re-entry, by the
  post-upgrade build installed over it (`adb install -r`).
- [ ] AC-13: On-device round-trip on the upgraded debug APK. A manual backup completes and a
  manual restore completes. A scheduled (WorkManager) backup runs once. No `IllegalArgumentException`,
  `SecurityException`, or `ForegroundServiceStartNotAllowedException` appears in `adb logcat`
  during any of the three runs. Results (device, API level, log excerpt) are recorded in the story.

## Rationale

This requirement implements the "Runtime libraries on latest stable" item of
EPIC-MODERNIZATION-006 (success criteria 1, 7, 8, and 9). Older libraries miss security and
platform-compatibility fixes, and a pre-release dependency should not ship. The risk concentrates
in WorkManager, because the BUG-016 manifest override depends on the library's own manifest
contents. The acceptance criteria therefore require evidence from the merged manifest and from a
device, not only from unit tests.

## Constraints

- Build stays on JDK 25. Dependency verification stays enforced. No per-package JaCoCo floor is lowered.
- minSdk will be 23 once REQ-MODERNIZATION-015 lands. If a target library's minimum SDK or
  Kotlin version exceeds what the current build provides, this requirement is sequenced after
  REQ-MODERNIZATION-015 and the story records that dependency.
- Each library family (WorkManager, Lifecycle, coroutines, preference/annotation/core-role) should
  land as its own verified increment so any regression is attributable to one change.
- Coroutines 1.11.0 must be compatible with the Kotlin version in use; if not, the story records
  the constraint and resolves it against REQ-MODERNIZATION-015 rather than pinning silently.
- No user-visible behavior change, and no data loss in stored credentials or settings.

## Verification Method

1. Static checks: AC-1 and AC-2 greps and the recorded resolution evidence.
2. Gradle gates: `assembleDebug`, `testDebugUnitTest`, and `jacocoTestCoverageVerification` (AC-4 to AC-6).
3. Merged-manifest diff before and after (AC-7).
4. On-device checks (AC-9, AC-11, AC-12, AC-13) and contract and lifecycle test review (AC-8, AC-10).

## Notes

- `androidx.security:security-crypto` is left alone here. If planning-time review shows it is
  unmaintained, record it as a follow-up and do not widen this requirement.
- `work-testing` is a `testImplementation` dependency (`app/build.gradle` line 157) and must be
  bumped in lockstep with `work-runtime-ktx`.
