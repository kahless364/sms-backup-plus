---
id: "REQ-MODERNIZATION-001"
title: "Restore Google Play Eligibility via SDK and Build Uplift"
artifact_type: requirement
domain: modernization
status: approved
type: functional
priority: critical
epic: "EPIC-MODERNIZATION-001"
source_analysis: "sdlc/analysis/20260529-modernization/promoted/phase0-shippable-secure-context.md"
related_stories: []
change_records: []
---

# REQ-MODERNIZATION-001: Restore Google Play Eligibility via SDK and Build Uplift

## Overview

The application must be raised from `compileSdkVersion`/`targetSdkVersion` 29 to SDK 35, with a staged Android Gradle Plugin (AGP) and Gradle wrapper upgrade, all required API-31+-through-34+ behavioral conformance fixes co-landed atomically, and the defunct JCenter repository declarations removed — producing a signed Android App Bundle (AAB) that passes Google Play pre-launch review at targetSdk 35 (Gate G0 / Milestone M0).

## Context

`app/build.gradle` lines 10 and 15 declare `compileSdkVersion 29` and `targetSdkVersion 29`. Google Play has enforced a minimum `targetSdk` of API 34 for all app updates since August 2024; the current value sits five API levels below that floor. As a direct consequence, **no update of any kind — including critical security fixes — can reach users through Google Play until this requirement is satisfied.** This is the unconditional distribution gate (gaps.md TECH-001 / DEP-001): every other gap closure in the modernization plan inherits the same block.

The impact is not hypothetical. A maintainer who fixes the active MITM-exploitable trust-all TLS path (SEC-001) and submits an AAB today will receive a Play policy rejection before any user sees the fix. The gate must close first.

Raising `targetSdkVersion` from 29 to 35 activates Android's per-API behavioral enforcement retroactively across API levels 30–34. Three categories of conformance work must co-land inside the same build sweep — not deferred to follow-on commits — or the raise itself introduces new regressions on modern devices:

**1. PendingIntent mutability (API 31+ / Android 12).** Two `PendingIntent` constructions use `FLAG_UPDATE_CURRENT` without `FLAG_IMMUTABLE` or `FLAG_MUTABLE`: `service/AlarmManagerDriver.java:127` and `service/ServiceBase.java:204`. This is silent at targetSdk 29 but throws `IllegalArgumentException` at runtime on API 31+ the instant targetSdk is raised (gaps.md QUAL-001/CAP-002). The fix is a two-line change that must co-land with the SDK raise or the scheduling path hard-crashes on any device running Android 12+.

**2. Manifest receiver export declarations (API 31+ / Android 12).** All six intent-filtered `<receiver>` elements in `app/src/main/AndroidManifest.xml` lack explicit `android:exported` declarations: `SmsBroadcastReceiver` (line 131), `BootReceiver` (line 139), `PackageReplacedReceiver` (line 145), `BackupBroadcastReceiver` (line 151), `.compat.SmsReceiver` (line 159, `SMS_DELIVER` filter, `BROADCAST_SMS` permission), and `.compat.MmsReceiver` (line 167, `WAP_PUSH_DELIVER` filter, `BROADCAST_WAP_PUSH` permission). Android 12+ requires explicit export intent for every receiver; absence is a build error under AGP 7+. (The `.compat.HeadlessSmsSendService` at line 190 is a `<service>`, already `exported="true"`, and is out of scope.)

**3. Foreground-service type and notification permission (API 33–34 / Android 13–14).** API 34 requires foreground services to declare `android:foregroundServiceType`; API 33 requires `POST_NOTIFICATIONS` at runtime before posting progress notifications.

Additionally, `build.gradle` lines 4, 21, and 25 reference `jcenter()` and `https://jcenter.bintray.com`. JCenter shut down in February 2022. Its continued presence means every Gradle resolution attempt contacts a sunset CDN first — introducing latent build non-determinism with no upside. Removal is approximately a 30-minute fix (gaps.md TECH-007/DEP-006) and co-lands in this sweep.

The AGP upgrade must be staged through an intermediate version (4.1.3 → 7.4.x → 8.x) to avoid accumulating too many breaking changes in a single step; the Gradle wrapper must be raised to 8.x to match (target-state.md ADR-006). The minSdk raise from 14 to 21 co-lands with the targetSdk raise, eliminating the Pre-API-21 compatibility surface from the active codebase.

## Requirements

### SDK and Build Toolchain Uplift

Raise `compileSdkVersion` and `targetSdkVersion` to 35, `minSdkVersion` to 21, and `buildToolsVersion` to the version paired with SDK 35 in `app/build.gradle`. Upgrade the AGP via the staged path 4.1.3 → 7.4.x → 8.x (three independently-buildable commits) and upgrade the Gradle wrapper to 8.x to match. `./gradlew assembleRelease` must exit 0 on a clean checkout using only `google()`, `mavenCentral()`, and `https://jitpack.io` as repository sources.

#### Acceptance Criteria

1. `app/build.gradle` declares `compileSdkVersion 35` and `targetSdkVersion 35`. Verified by static inspection of the file post-merge; `grep "compileSdkVersion\|targetSdkVersion" app/build.gradle` returns only lines containing `35`.
2. `app/build.gradle` declares `minSdkVersion 21`. Verified by static inspection; no reference to `minSdkVersion 14` or any value below 21 remains.
3. The root `build.gradle` classpath entry declares AGP 8.x (`com.android.tools.build:gradle:8.*`). No AGP version below 7.4 appears in any build file. Verified by `grep -r "com.android.tools.build:gradle" .` returning only 8.x coordinates.
4. `gradle/wrapper/gradle-wrapper.properties` `distributionUrl` references a Gradle 8.x distribution. Verified by static inspection.
5. `./gradlew assembleRelease` exits 0 on a clean checkout with a cleared Gradle cache, using only `google()`, `mavenCentral()`, and `https://jitpack.io` repository declarations. Verified by CI log.
6. The version-control history (or PR commits) contains three identifiable AGP stage commits — AGP 4.1.3, then 7.4.x, then 8.x — each independently compilable to an unsigned APK/AAB without errors. Verified by PR commit history.

---

### JCenter Repository Declarations Removed

Delete all three JCenter and Bintray repository entries from `build.gradle`: `jcenter()` in the `buildscript` block (line 4), `jcenter()` in the `allprojects` block (line 21), and the literal `https://jcenter.bintray.com` URL at line 25. After removal, a clean build must resolve all declared dependencies without contacting any JCenter endpoint.

#### Acceptance Criteria

7. The string `jcenter` does not appear anywhere in `build.gradle`, `app/build.gradle`, `settings.gradle`, or any file under `gradle/`. Verified by `grep -ri "jcenter" build.gradle app/build.gradle settings.gradle gradle/` returning no output.
8. The string `jcenter.bintray.com` does not appear in any `*.gradle` or `*.properties` file. Verified by `grep -r "jcenter.bintray.com"` returning no output.
9. A clean `./gradlew dependencies` run with a cleared Gradle cache resolves all declared dependencies to `google()`, `mavenCentral()`, or `https://jitpack.io` without "Could not resolve" errors. Verified by CI build log with `--refresh-dependencies` flag and a pre-cleared cache.

---

### FLAG_IMMUTABLE Co-Landed on Both PendingIntent Sites

Add `PendingIntent.FLAG_IMMUTABLE` (bitwise-ORed with any existing flags) to both `PendingIntent` construction sites. This change must land in the same commit or the same PR as the `targetSdkVersion` raise — not before (it is a no-op at targetSdk 29 and constitutes unnecessary churn), and not after (the raise itself converts the latent issue into a hard `IllegalArgumentException` on the scheduling path on API 31+ devices).

- `service/AlarmManagerDriver.java:127`: add `| PendingIntent.FLAG_IMMUTABLE` to the flags argument.
- `service/ServiceBase.java:204`: add `| PendingIntent.FLAG_IMMUTABLE` to the flags argument.

Note: `AlarmManagerDriver.java` is slated for full deletion by the WorkManager migration (MU-005). The FLAG_IMMUTABLE fix here is an explicit interim measure. It must remain until MU-005 lands; MU-005 then removes the file entirely.

#### Acceptance Criteria

10. `service/AlarmManagerDriver.java` at the `PendingIntent` construction site (currently line 127) includes `FLAG_IMMUTABLE` in the flags argument. Verified by `grep -n "FLAG_IMMUTABLE" app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java` returning a match at the relevant line.
11. `service/ServiceBase.java` at the `PendingIntent` construction site (currently line 204) includes `FLAG_IMMUTABLE` in the flags argument. Verified by `grep -n "FLAG_IMMUTABLE" app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` returning a match at the relevant line.
12. No remaining `PendingIntent` construction call in `app/src/main/java/` is missing both `FLAG_IMMUTABLE` and `FLAG_MUTABLE`. Verified by `grep -rn "PendingIntent\.get" app/src/main/java/` followed by inspection confirming every construction site carries a mutability flag.
13. The git commit that sets `targetSdkVersion 35` is the same commit as, or is preceded within the same PR by, the commit adding `FLAG_IMMUTABLE` to both sites. No intermediate commit state exists where `targetSdkVersion` is 35 and either site lacks a mutability flag. Verified by PR commit history.
14. On a Robolectric test run targeting an emulated API 31+ SDK level, the backup scheduling path that invokes `AlarmManager` via `AlarmManagerDriver` completes without throwing `IllegalArgumentException`. Verified by test run output. (This test is authored under REQ-MODERNIZATION-002 coverage gate but must pass before REQ-MODERNIZATION-001 is closed.)

---

### android:exported Declared on All Six Manifest Receivers

Add explicit `android:exported` attributes to all six intent-filtered `<receiver>` elements in `app/src/main/AndroidManifest.xml` that currently lack them. The correct value for each receiver is determined by whether it accepts intents from outside the application package:

- `SmsBroadcastReceiver` (line 131): `android:exported="false"` — receives the system `SMS_RECEIVED` implicit broadcast, but not a contract advertised to third parties.
- `BootReceiver` (line 139): `android:exported="false"` — receives `BOOT_COMPLETED` system broadcast; no third-party sending contract.
- `PackageReplacedReceiver` (line 145): `android:exported="false"` — receives `MY_PACKAGE_REPLACED`; no third-party sending contract.
- `BackupBroadcastReceiver` (line 151): `android:exported="true"` — exposes the documented public `com.zegoggles.smssync.BACKUP` broadcast contract that third-party automation apps (e.g., Tasker) may send. Setting this to `false` would silently break the public API and any user automation built on it. Any existing `tools:ignore="ExportedReceiver"` suppression on this element must be removed once the explicit attribute is declared.
- `.compat.SmsReceiver` (line 159): `android:exported="true"` — receives the system `SMS_DELIVER` broadcast (default-SMS-app delivery contract, guarded by the `BROADCAST_SMS` signature permission); must be reachable by the platform.
- `.compat.MmsReceiver` (line 167): `android:exported="true"` — receives the system `WAP_PUSH_DELIVER` broadcast (MMS delivery, guarded by the `BROADCAST_WAP_PUSH` signature permission); must be reachable by the platform.

> **Scope correction:** This requirement originally named only four receivers. The manifest contains six intent-filtered receivers; the two `.compat.*` receivers were omitted. Because a receiver with an intent-filter and no explicit `android:exported` is a hard manifest-merger error under AGP 7+, all six must be addressed or the build fails. Synced with DES-MODERNIZATION-001.

#### Acceptance Criteria

15. Each of the six intent-filtered `<receiver>` elements in `app/src/main/AndroidManifest.xml` carries an explicit `android:exported` attribute. Verified by static inspection of all six elements.
16. `SmsBroadcastReceiver`, `BootReceiver`, and `PackageReplacedReceiver` declare `android:exported="false"`. Verified by static inspection.
17. `BackupBroadcastReceiver`, `.compat.SmsReceiver`, and `.compat.MmsReceiver` declare `android:exported="true"`. Verified by static inspection of `AndroidManifest.xml`.
18. No `tools:ignore="ExportedReceiver"` annotation remains on any `<receiver>` element in `AndroidManifest.xml`. Verified by `grep "ExportedReceiver" app/src/main/AndroidManifest.xml` returning no output.
19. `./gradlew lint` produces zero violations of category `ExportedReceiver` for any of the six receivers. Verified by CI lint report.
20. `adb shell am broadcast -a com.zegoggles.smssync.BACKUP com.zegoggles.smssync` successfully triggers a backup intent (received by `BackupBroadcastReceiver`) on a test device or emulator running API 31+. Verified by logcat output confirming receipt. This verifies the public contract is not broken by the `exported` declaration.

---

### FOREGROUND_SERVICE_DATA_SYNC Type and POST_NOTIFICATIONS Permission

Add `android:foregroundServiceType="dataSync"` to the `<service>` element(s) for `SmsBackupService` and `SmsRestoreService` in `AndroidManifest.xml`. Add `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>` alongside the existing `FOREGROUND_SERVICE` permission. Declare `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` and request it at runtime using `ActivityCompat.requestPermissions` before the first backup or restore progress notification is posted on devices running API 33+. On devices below API 33, the runtime request must be suppressed — the code path must not call `requestPermissions` for `POST_NOTIFICATIONS` on pre-33 devices.

#### Acceptance Criteria

21. The `SmsBackupService` `<service>` element in `AndroidManifest.xml` includes `android:foregroundServiceType="dataSync"`. Verified by static inspection.
22. The `SmsRestoreService` `<service>` element in `AndroidManifest.xml` includes `android:foregroundServiceType="dataSync"`. Verified by static inspection.
23. `AndroidManifest.xml` contains `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>`. Verified by `grep "FOREGROUND_SERVICE_DATA_SYNC" app/src/main/AndroidManifest.xml` returning a match.
24. `AndroidManifest.xml` contains `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`. Verified by `grep "POST_NOTIFICATIONS" app/src/main/AndroidManifest.xml` returning a match.
25. On an API 33+ AVD, the app requests `POST_NOTIFICATIONS` at runtime before the first backup/restore progress notification is posted. Verified by an instrumentation test or documented manual smoke on an API 33+ AVD confirming the permission dialog appears on first backup initiation.
26. On an API 32 AVD, the app does not call `requestPermissions` for `POST_NOTIFICATIONS` and does not crash. Verified by instrumentation test or documented manual smoke on an API 32 AVD confirming no permission dialog for `POST_NOTIFICATIONS` and no `RuntimeException`.
27. `./gradlew lint` produces zero violations related to `ForegroundServiceType` or `MissingPermission` for notification posting. Verified by CI lint report.

---

### Signed AAB at targetSdk 35 Passes Play Pre-Launch Review (Gate G0)

A release-signed Android App Bundle built from the codebase satisfying all prior acceptance criteria must be submitted to Google Play Console and pass the automated pre-launch policy check without any rejection on the targetSdk dimension. This is the terminal acceptance condition for REQ-MODERNIZATION-001 and constitutes Gate G0 / Milestone M0 of the modernization roadmap.

#### Acceptance Criteria

28. `./gradlew bundleRelease` produces a signed AAB file. Verified by presence of the AAB artifact and a non-zero file size.
29. `aapt2 dump badging <path-to-release.aab> | grep targetSdkVersion` outputs `targetSdkVersion:'35'`. Verified by command output.
30. The AAB is submitted to the Google Play Console internal testing track. The submission is accepted (no policy-rejection email or console error regarding `targetSdkVersion`, `android:exported`, foreground-service type, or `PendingIntent` mutability). Verified by Play Console submission status showing no policy violation on any of these dimensions.

## Rationale

Google Play's enforced targetSdk floor is a hard binary gate: an app below the floor cannot receive updates through the primary distribution channel, regardless of the quality or urgency of the changes. For SMS Backup+ — an open-source privacy application whose entire value proposition is protecting users' message data — the inability to ship security fixes is not an inconvenience; it is a direct, ongoing harm. The trust-all TLS path (SEC-001) exposes every user's SMS, MMS, and call-log content to a network-adjacent attacker. That exposure cannot be closed for existing users until targetSdk ≥ 34.

The FLAG_IMMUTABLE and jcenter items are included in this requirement — rather than separate tickets with independent timelines — because their timing constraints are absolute and co-dependent with the SDK raise. FLAG_IMMUTABLE must co-land because the raise is the precise event that converts a latent API contract violation into a hard crash: a raise without the fix is strictly worse than no raise. JCenter removal co-lands because the build-file scope is shared, the effort is minimal, and each day of deferral is another day of non-deterministic resolution against a sunset CDN.

The ~80% front-loading of business value in the modernization roadmap (modernization-plan.md §Remediation Roadmap) is a structural consequence of this topology: Play distribution is the delivery mechanism for every subsequent improvement. Closing this gate is the lever that makes all other work reachable by users.

## Verification Method

**Static / CI-automated (no device required):** Acceptance criteria 1–9, 10–13, 15–19, 21–24, 28–29 are verifiable by grep, static file inspection, lint report, and `aapt2 dump` in CI without device execution.

**Dynamic / device-required:** Criteria 14, 20, 25, 26 require execution against specific API-level AVDs. These must run in a CI matrix with the target API-level system image or be executed as documented manual smoke steps on the named API levels before the requirement is marked closed.

**Distribution gate:** Criterion 30 requires a Google Play Console submission. This is a one-time gate check and constitutes the definitive pass/fail signal for Gate G0.

## Constraints

1. **Robolectric co-requisite (REQ-MODERNIZATION-002).** Robolectric 4.3.1 (`app/build.gradle:66`) caps test execution at SDK 29. It cannot compile or run tests against `compileSdkVersion 35`. The Robolectric upgrade to 4.12.x must land in the same build sweep as the SDK raise. Neither ships independently: raising targetSdk without upgrading Robolectric breaks the entire test suite; upgrading Robolectric without raising targetSdk delivers no value. This is a hard co-requisite enforced by the Gradle build graph — they share `app/build.gradle`.
2. **warningsAsErrors brittleness.** `warningsAsErrors true` (`app/build.gradle:41`) and `-Werror -Xlint:deprecation` (`app/build.gradle:75`) will fail the build as new deprecation warnings surface during the AGP and SDK upgrade. A `lint-baseline.xml` capturing the pre-existing warnings must be committed before or alongside the first AGP stage commit. Each subsequent stage must remove entries from the baseline (never add); the full `-Werror` policy with zero suppressed entries must be restored before this requirement is considered closed.
3. **minSdk 21 dead-code boundary.** Raising `minSdkVersion` to 21 renders `CalendarAccessorPre40.java`, the `CalendarAccessor.Get` factory branch, and several inline API-level guards provably unreachable. Their deletion is owned by the dead-code sweep requirement (MU-011). This requirement's scope ends at the minSdk raise that creates the dead code; the dead-branch cleanup is out of scope here.
4. **Staged AGP upgrade path.** The jump from AGP 4.1.3 to 8.x spans four major versions, each introducing breaking changes in the build DSL, variant API, and namespace handling. The migration must be staged as three independently-compilable commits: 4.1.3 → 7.4.x → 8.x. Jumping directly from 4.1.3 to 8.x is not permitted — the compounding breaking-change surface is not safely manageable as a single diff and cannot be characterization-tested at intermediate states.
5. **AlarmManagerDriver interim status.** `service/AlarmManagerDriver.java` is deleted in full by the WorkManager migration (MU-005 / REQ-MODERNIZATION-005). The FLAG_IMMUTABLE fix to line 127 is explicitly an interim measure. It must remain in place and passing its test until MU-005 lands; the deletion of the file is out of scope for this requirement.

## Notes

**Traceability**

| Source ID | Finding | Source artifact |
|-----------|---------|----------------|
| TECH-001 / DEBT-001 | targetSdk 29 blocks all Play distribution | `gaps.md` §DEP-001; `phase0-shippable-secure-context.md` Finding 1 |
| QUAL-001 / CAP-002 | FLAG_IMMUTABLE missing — latent crash on API 31+ | `gaps.md` §QUAL-001 |
| TECH-007 / DEP-006 | jcenter after 2022 shutdown — build non-determinism | `gaps.md` §DEP-006 |
| MU-001 | Build & Platform Uplift (the gate) | `migration-units.md` §MU-001 |
| Gate G0 / Milestone M0 | Play pre-launch review gate | `modernization-plan.md` §Remediation Roadmap — Immediate (0–30 days) |

Source assessment: `20260529-modernization`

**Key file locations (all paths relative to repo root)**

| File | Lines of interest |
|------|------------------|
| `app/build.gradle` | `:10` compileSdkVersion 29; `:14` minSdkVersion 14; `:15` targetSdkVersion 29; `:41` warningsAsErrors; `:66` robolectric 4.3.1; `:75` -Werror |
| `build.gradle` | `:4` jcenter() buildscript; `:8` AGP 4.1.3; `:21` jcenter() allprojects; `:25` jcenter.bintray.com |
| `gradle/wrapper/gradle-wrapper.properties` | distributionUrl (currently Gradle 7.2) |
| `app/src/main/AndroidManifest.xml` | `:131` SmsBroadcastReceiver; `:139` BootReceiver; `:145` PackageReplacedReceiver; `:151` BackupBroadcastReceiver; `:159` compat.SmsReceiver; `:167` compat.MmsReceiver |
| `service/AlarmManagerDriver.java` | `:127` PendingIntent FLAG_IMMUTABLE co-land site |
| `service/ServiceBase.java` | `:204` PendingIntent FLAG_IMMUTABLE co-land site |

**Downstream blocks resolved by this requirement.** Closing REQ-MODERNIZATION-001 unblocks: REQ-MODERNIZATION-002 (Test-Harness and Coverage Gate — co-requisite), REQ-MODERNIZATION-003 (Transport Security Hardening — characterization tests can run at targetSdk 35), REQ-MODERNIZATION-004 (Secret-at-Rest Hardening — Keystore ergonomics stable at minSdk 21), REQ-MODERNIZATION-005 (WorkManager migration — requires SDK 35 build baseline), REQ-MODERNIZATION-006 (Eventing migration), REQ-MODERNIZATION-007 (Hilt DI), REQ-MODERNIZATION-008 (Mail ACL and k-9 unpin), REQ-MODERNIZATION-009 (Play Billing 7.x uplift), and REQ-MODERNIZATION-011 (dead-code sweep). None of these can ship to users via Google Play until Gate G0 is cleared.
