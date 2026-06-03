---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-001
related_stories: []
related_design_docs: []
integration_contracts: []
change_records: []
type: ''
id: DES-MODERNIZATION-001
title: ''
domain: modernization
---

# DES-MODERNIZATION-001: Restore Google Play Eligibility via SDK and Build Uplift

## Overview

This design specifies the **system architecture** and **integration design** for raising SMS Backup+ from `compileSdk`/`targetSdk` 29 to 35 (`minSdk` 14 → 21) through a **staged Android Gradle Plugin (AGP) and Gradle wrapper upgrade**, co-landing every API-31-through-34 behavioral-conformance fix atomically, and removing the defunct JCenter/Bintray repository declarations — producing a release-signed Android App Bundle (AAB) that passes Google Play pre-launch policy review at `targetSdk 35`. This is **Migration Unit MU-001 / Gate G0 / Milestone M0** of the modernization roadmap and the realization of `target-state.md` **ADR-006**.

It is deliberately a **build-and-conformance** design, not a behavior-redesign. Sections enabled: **System Architecture**, **Integration Design**, **Design Validation**, **Integration Contracts**. Data-model, API-design, and component-decomposition sections are intentionally **OFF** — this requirement changes the platform substrate and manifest surface, not the domain model, public API shape, or component boundaries (those are owned by DES-MODERNIZATION-005 WorkManager, DES-MODERNIZATION-003 transport security, DES-MODERNIZATION-007 Hilt, etc.).

It traces wholly to **REQ-MODERNIZATION-001** (all seven requirement clusters, AC-1 through AC-30).

## Context

`app/build.gradle:10,15` declare `compileSdkVersion 29` / `targetSdkVersion 29` (verified by direct read this session). Google Play has enforced a `targetSdk` floor of API 34 for all app *updates* since August 2024; the app sits five API levels below the floor. The consequence is binary and unconditional: **no update of any kind — including the active Critical trust-all-TLS security fix (SEC-001) — can reach existing users through Google Play until this gate closes** (REQ-MODERNIZATION-001 §Context; gaps.md TECH-001/DEP-001). Roughly 80% of the modernization roadmap's business value is gated behind this single change, because Play distribution is the delivery mechanism for every subsequent improvement.

Raising `targetSdk` is not a one-line edit. The raise **retroactively activates** Android's per-API behavioral enforcement for API levels 30–34. Three categories of latent non-conformance become hard failures the instant `targetSdk` crosses each threshold, and a build cannot even assemble under AGP 7+ with some of them present:

1. **PendingIntent mutability (API 31+).** Two construction sites use `FLAG_UPDATE_CURRENT` with neither `FLAG_IMMUTABLE` nor `FLAG_MUTABLE` — verified by direct read: `AlarmManagerDriver.java:127` (`PendingIntent.getService(ctx, 0, intent, FLAG_UPDATE_CURRENT)`) and `ServiceBase.java:201–205` (`PendingIntent.getActivity(..., FLAG_UPDATE_CURRENT)`, the `FLAG_UPDATE_CURRENT` token on line 204). Silent at `targetSdk 29`; throws `IllegalArgumentException` at construction on API 31+ the instant `targetSdk` is raised.
2. **Manifest receiver export (API 31+).** All six intent-filtered app `<receiver>` elements lack explicit `android:exported` — verified at `AndroidManifest.xml:131` (`SmsBroadcastReceiver`), `:139` (`BootReceiver`), `:145` (`PackageReplacedReceiver`), `:151` (`BackupBroadcastReceiver`, which additionally carries `tools:ignore="ExportedReceiver"`), `:159` (`.compat.SmsReceiver`, `SMS_DELIVER` filter, `BROADCAST_SMS` permission) and `:167` (`.compat.MmsReceiver`, `WAP_PUSH_DELIVER` filter, `BROADCAST_WAP_PUSH` permission). Absence is a **manifest-merger/build error** under AGP 7+, independent of runtime. (The `.compat.HeadlessSmsSendService` at `:190` is a `<service>`, not a receiver, and already declares `android:exported="true"` — out of scope.)
3. **Foreground-service type + notification permission (API 33–34).** `AndroidManifest.xml:123–124` declare `SmsBackupService`/`SmsRestoreService` `<service>` elements with no `android:foregroundServiceType`; `:80` declares `FOREGROUND_SERVICE` but there is no `FOREGROUND_SERVICE_DATA_SYNC` and no `POST_NOTIFICATIONS`. API 34 requires an FGS type; API 33 requires runtime `POST_NOTIFICATIONS` before posting progress notifications.

Additionally, `build.gradle` references `jcenter()` at lines 4 and 21 and `https://jcenter.bintray.com` at line 25 (verified). JCenter shut down in February 2022; each Gradle resolution contacts a sunset CDN first — latent non-determinism with zero upside.

The build toolchain itself blocks the raise: `build.gradle:8` pins AGP `4.1.3` and `gradle-wrapper.properties` pins Gradle `7.2` — neither can compile `compileSdk 35`. `app/build.gradle:66` pins Robolectric `4.3.1`, which caps test execution at SDK 29 and shares `app/build.gradle` with the SDK declarations, making the Robolectric upgrade a **hard build-graph co-requisite** (owned by DES-MODERNIZATION-003 under REQ-MODERNIZATION-002).

This design is the **baseline every other modernization DES compiles against.** Until it lands, no other story can be built, tested, or shipped. That singular property dictates its architecture: maximize the probability of a clean, characterization-testable, reversible upgrade by **staging** the toolchain jump rather than attempting it as one diff.

## Design

### Staged Toolchain Uplift — Three Independently-Buildable AGP Stages

The jump from AGP 4.1.3 to 8.x spans four major versions (4.1 → 7.0 → 7.4 → 8.0+), each introducing breaking changes across the build DSL, the variant API, namespace handling, lint configuration, and the Kotlin/AGP/Gradle compatibility matrix. Attempting it as a single diff compounds an unbounded breaking-change surface that cannot be characterization-tested at intermediate states. The design mandates **three independently-compilable commits** (REQ-MODERNIZATION-001 AC-6; Constraint 4; ADR-006):

| Stage | AGP | Gradle wrapper | compile/target SDK | What this stage de-risks |
|-------|-----|----------------|--------------------|--------------------------|
| S1 | **4.1.3 → 7.4.x** | 7.2 → 7.5.x (7.4 requires Gradle ≥ 7.3.3; 7.5.x for headroom) | leave 29, raise minSdk → 21 | Absorbs the largest DSL break: `lintOptions{}`→`lint{}`, `package` attr → `namespace`, variant-API removals, `buildToolsVersion` auto-resolution. minSdk raise here (not at S3) isolates compat-surface fallout from SDK-raise fallout. |
| S2 | **7.4.x → 8.x** (latest 8.x paired with SDK 35) | 7.5.x → 8.x (AGP 8 requires Gradle ≥ 8.0 and JDK 17) | leave 29 | Absorbs the AGP-8 jumps: mandatory `namespace`, JDK 17 toolchain, R8 default changes, removed deprecated APIs. SDK held at 29 so this stage proves the *toolchain* in isolation. |
| S3 | 8.x (unchanged) | 8.x (unchanged) | **29 → 35** + co-landed conformance fixes + jcenter removal | The behavioral raise. All API-31–34 conformance (PendingIntent mutability, receiver export, FGS type, POST_NOTIFICATIONS) co-lands here in the *same commit as* `targetSdkVersion 35`, satisfying AC-13's "no intermediate state where targetSdk is 35 and a site lacks a flag." |

**Stage ordering rationale (explicit):** The toolchain (S1, S2) is proven *before* the behavioral raise (S3). Inverting this — raising SDK on the old toolchain — is impossible (AGP 4.1.3 cannot compile SDK 35) and conflates two failure domains. Holding SDK at 29 through S2 means any S2 breakage is unambiguously a toolchain regression, not a behavioral one. The minSdk 14→21 raise is placed at **S1** rather than S3 because it is orthogonal to the targetSdk raise and to the AGP-8 jump; isolating it prevents a three-way-entangled diff at S3. Per Constraint 3, the raise *creates* dead code (`CalendarAccessorPre40`, the `CalendarAccessor.Get` factory branch, inline pre-21 guards such as `ServiceBase.isConnectedViaWifi_pre_SDK21` at lines 216–222) but **does not delete it** — dead-code removal is MU-011's scope. This design's boundary ends at the version bump that strands the code.

**`warningsAsErrors` / `-Werror` brittleness (Constraint 2; verified at `app/build.gradle:41,75`):** `warningsAsErrors true` and `-Werror -Xlint:deprecation` will fail the build the moment a new deprecation surfaces during the AGP/SDK sweep, and many *will* surface (e.g., `NotificationCompat.Builder(Context)` already `@SuppressWarnings("deprecation")` at `ServiceBase.java:186`; `getNetworkInfo`/`getAllNetworks` at `:220,227`). The design requires a **`lint-baseline.xml` committed before or with S1**, capturing the pre-existing warning set, with the policy: **each subsequent stage may only remove baseline entries, never add.** Full `-Werror` with an empty baseline must be restored before AC closure. This makes deprecation debt a monotonically-decreasing fitness function across the three stages rather than a wall at S3.

### API-31+ PendingIntent Mutability — Co-Landed FLAG_IMMUTABLE at Both Sites

Both sites take `| PendingIntent.FLAG_IMMUTABLE` bitwise-ORed with the existing `FLAG_UPDATE_CURRENT`, **co-landed in S3's `targetSdkVersion 35` commit** (REQ-MODERNIZATION-001 AC-10/11/13):

- `AlarmManagerDriver.java:127` — `PendingIntent.getService(ctx, 0, intent, FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)`. Both PendingIntents here are **send-only triggers** the app fires at itself (an `Intent` targeting `SmsBackupService` with an action; lines 124–125); nothing external mutates the intent extras, so `FLAG_IMMUTABLE` is behaviorally correct, not merely a compliance token.
- `ServiceBase.java:204` — the `getActivity(...)` constructing a `MainActivity` content-intent for the foreground notification; same `| FLAG_IMMUTABLE` addition. This intent is launched by the user tapping the notification and carries no mutable payload, so immutable is correct.

**Timing is a hard constraint, not a preference.** The fix is a no-op at `targetSdk 29` (so landing it earlier is pure churn) and converts a latent contract violation into a hard `IllegalArgumentException` on API 31+ the instant the raise lands (so landing it later means a shipped crash). AC-13 forbids any intermediate commit where `targetSdk == 35` and either site lacks a mutability flag. **Interim-status note (Constraint 5 / ADR-006):** `AlarmManagerDriver.java` is deleted in full by MU-005 (WorkManager migration — the class implements firebase `Driver`/`JobValidator`, confirmed by its imports at lines 24–29). The FLAG_IMMUTABLE fix to `:127` is an explicit interim measure that must remain and keep passing its API-31+ Robolectric test (AC-14) until MU-005 removes the file. This design does not remove it.

**Scope verification (PendingIntent sites) — grep-confirmed this session.** REQ-MODERNIZATION-001 AC-12 requires that *no* remaining `PendingIntent.get*` call in `app/src/main/java/` lacks a mutability flag. I ran `grep "PendingIntent\.get"` across `app/src/main/java` this session and confirmed **exactly two production construction sites**: `AlarmManagerDriver.java:127` (`getService`) and `ServiceBase.java:201` (`getActivity`, with the `FLAG_UPDATE_CURRENT` token on line 204). No third production site exists. Scope verified via grep. The S3 story should re-run the grep as a regression guard (a future cherry-pick could introduce a new site), but the current enumeration is complete and confirmed.

**Test co-update required (verified-now).** A pre-existing characterization test asserts the *current* flag value: `AlarmManagerDriverTest.java:97` reads `assertThat(shadowPendingIntent.getFlags()).isEqualTo(FLAG_UPDATE_CURRENT)` (confirmed by grep this session). The moment S3 ORs in `FLAG_IMMUTABLE`, this assertion fails (the shadow flags become `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`). The S3 commit MUST update `AlarmManagerDriverTest:97` to assert the combined flag, and the AC-14 API-31+ scheduling test (authored under REQ-MODERNIZATION-002) must pass. This test-coupling is the kind of thing a `-Werror`/CI run catches late; it is called out here so it lands in the same commit. (`AlarmManagerDriverTest` is retired with the file in MU-005, but it must stay green through the MU-001 interim.)

### Manifest Conformance — Receiver Export, FGS Type, Notification Permission

All manifest changes co-land in **S3** (they are build errors under the AGP 7+ that S1 introduces, so strictly speaking the receiver-export fix may be forced as early as S1's AGP 7.4; the design permits it at S1 if the merger demands it, but the *value* assignment is identical and is specified here). Verified current state and target state:

| Element (verified line) | Current | Target | Export rationale (REQ AC-15..18) |
|---|---|---|---|
| `SmsBroadcastReceiver` (`:131`) | no `exported` | `android:exported="false"` | Receives system `SMS_RECEIVED` implicit broadcast; not a third-party contract. Already guarded by `android:permission="android.permission.BROADCAST_SMS"`. |
| `BootReceiver` (`:139`) | no `exported` | `android:exported="false"` | `BOOT_COMPLETED` system broadcast only. |
| `PackageReplacedReceiver` (`:145`) | no `exported` | `android:exported="false"` | `MY_PACKAGE_REPLACED` system broadcast only. |
| `BackupBroadcastReceiver` (`:151`) | no `exported`, has `tools:ignore="ExportedReceiver"` | `android:exported="true"`, **remove** `tools:ignore` | Exposes the documented public `com.zegoggles.smssync.BACKUP` contract that third-party automation (Tasker etc.) sends. Setting `false` silently breaks the public API. AC-18 requires the `tools:ignore` be removed once the explicit attribute exists; AC-20 verifies the contract still fires via `adb shell am broadcast`. |
| `.compat.SmsReceiver` (`:159`) | no `exported` | `android:exported="true"` | Receives the system `SMS_DELIVER` broadcast (default-SMS-app role); the system, not the app, is the sender, so the receiver must be exported to be reachable. Already guarded by `android:permission="android.permission.BROADCAST_SMS"`. Omitted from the original design and requirement (audit-corrected). |
| `.compat.MmsReceiver` (`:167`) | no `exported` | `android:exported="true"` | Receives the system `WAP_PUSH_DELIVER` broadcast (default-SMS-app MMS path); system-sent, so exported is required. Already guarded by `android:permission="android.permission.BROADCAST_WAP_PUSH"`. Omitted from the original design and requirement (audit-corrected). |

**Scope correction (receiver count).** The full set is **six** intent-filtered receivers, not four. `.compat.SmsReceiver` (`:159`) and `.compat.MmsReceiver` (`:167`) were omitted from the original design body and from REQ-MODERNIZATION-001 (both name only four). Because a receiver with an intent-filter and no explicit `android:exported` is a **hard manifest-merger error under AGP 7+**, implementing only four would fail this design's own AC-5 (`assembleRelease` exit 0) the moment S1 introduces AGP 7.4. This note **corrects REQ-MODERNIZATION-001's scope**: the implementing story MUST set explicit `android:exported` on all six receivers — `SmsBroadcastReceiver=false`, `BootReceiver=false`, `PackageReplacedReceiver=false`, `BackupBroadcastReceiver=true`, `.compat.SmsReceiver=true`, `.compat.MmsReceiver=true`. The two compat receivers are `true` because they receive system-originated broadcasts (`SMS_DELIVER`/`WAP_PUSH_DELIVER`) and must be reachable by the platform.

**Foreground-service conformance (REQ AC-21..24):**
- Add `android:foregroundServiceType="dataSync"` to both `SmsBackupService` (`:123`) and `SmsRestoreService` (`:124`) `<service>` elements. `dataSync` is the correct type: both services perform network-bound bulk data transfer (IMAP backup/restore), which is exactly the `dataSync` category's definition.
- Add `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>` alongside the existing `FOREGROUND_SERVICE` at `:80`.
- Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`.

**POST_NOTIFICATIONS runtime request (REQ AC-25/26) — version-gated, the one piece of new code logic in this design.** On API 33+, the app must call `ActivityCompat.requestPermissions(..., POST_NOTIFICATIONS)` before the first backup/restore progress notification is posted; on API ≤ 32 the code path must **not** call `requestPermissions` for `POST_NOTIFICATIONS` and must not crash. The integration point is the user-initiated backup/restore entry (the manual trigger in `MainActivity`, before the service posts via `ServiceBase.createNotification`/`getNotifier`, verified at `ServiceBase.java:174,187`). The guard is the standard `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` check — the same idiom already used at `ServiceBase.java:208`. This is the only behavioral addition; everything else in this design is version metadata, manifest attributes, and a two-site flag OR.

**Manifest scope note.** The `SmsJobService` `<service>` with the firebase `ACTION_EXECUTE` intent-filter (`:125–129`) is **out of scope here** — it is removed by MU-005's WorkManager migration. This design leaves it untouched; it remains `android:exported="false"` (already explicit at `:125`) and conformant.

### JCenter / Bintray Repository Removal

Delete all three sunset-CDN references (verified): `jcenter()` at `build.gradle:4` (buildscript), `jcenter()` at `:21` (allprojects), and `maven { url "https://jcenter.bintray.com" }` at `:25`. After removal the repository set is `google()`, `mavenCentral()`, `maven { url "https://jitpack.io" }`, and the existing `maven { url "https://maven.google.com" }` (redundant with `google()` but harmless). Co-lands in **S3** (REQ AC-7/8/9).

**Dependency-resolution risk this exposes (scope-verified caveat).** `build.gradle:26–27` documents that `https://maven.scijava.org/...` is "the only repo that seems to be hosting `com.firebase:firebase-jobdispatcher` in 2024." That scijava mirror is **separate from JCenter and is NOT removed by this requirement** (REQ-MODERNIZATION-001 scopes only `jcenter` and `jcenter.bintray.com`; AC-7/8 grep for `jcenter` specifically). Removing jcenter must therefore **not** also remove the scijava mirror, or `firebase-jobdispatcher:0.8.6` (`app/build.gradle:60`, verified) becomes unresolvable and the S3 build fails AC-5. The scijava mirror dies with firebase-jobdispatcher under MU-005, not here. The implementing story MUST verify post-removal that `firebase-jobdispatcher` still resolves (AC-9: `./gradlew dependencies --refresh-dependencies` on a cleared cache). This is the single most likely way an over-eager jcenter cleanup breaks the build, and it is called out so the Code Reviewer can gate on it.

### Robolectric Co-Requisite Coupling (cross-design dependency)

`app/build.gradle:66` pins `robolectric:4.3.1` (verified), which cannot run against `compileSdk 35`. Because the SDK declarations and the test dependency **share `app/build.gradle`**, the build graph forces them to move together: the moment S3 sets `compileSdk 35`, the existing test suite stops compiling/running unless Robolectric is simultaneously raised to 4.12.x. Per Constraint 1, neither ships independently. The Robolectric upgrade is owned by **DES-MODERNIZATION-003 under REQ-MODERNIZATION-002** but is a **hard co-requisite of S3** — they are one atomic build sweep. The companion `mockito-all:1.10.17` → `mockito-core 5.x` and `junit 4.12` (CVE-2020-15250) → `4.13.2` upgrades ride the same sweep (verified at `app/build.gradle:65,68`). This design declares the coupling; it does not specify the Robolectric implementation (that is the body of DES-MODERNIZATION-003).

## Architecture

### System view — what physically changes

There is no server tier (systems.md confirms an on-device monolith). "System architecture" here is the **build/distribution pipeline** plus the **manifest declaration surface** — the two things this requirement mutates. The runtime component graph (activity / service / mail / auth / preferences) is **unchanged** by this design.

```
                         MU-001 / Gate G0 BUILD PIPELINE (target)
  +------------------------------------------------------------------------------+
  |  Source (GitHub, single ':app' module + ':metadata')                         |
  |     gradle-wrapper.properties : Gradle 7.2 --S1--> 7.5.x --S2--> 8.x         |
  |     build.gradle  : AGP 4.1.3 --S1--> 7.4.x --S2--> 8.x ; jcenter x3 --S3--> X|
  |     app/build.gradle : compile/target 29 --S3--> 35 ; minSdk 14 --S1--> 21   |
  |     app/build.gradle : robolectric 4.3.1 --S3 (co-req DES-003)--> 4.12.x      |
  |     lint-baseline.xml : committed @S1, entries only REMOVED through S3         |
  +------------------------------------------------------------------------------+
        | ./gradlew assembleRelease (exit 0, repos: google/mavenCentral/jitpack
        |                            + scijava-for-jobdispatcher-until-MU-005)
        v
  +------------------------------------------------------------------------------+
  |  AndroidManifest.xml conformance (S3)                                         |
  |   6 receivers --> android:exported explicit (3x false; Backup/Sms/Mms=true)  |
  |   BackupBroadcastReceiver --> drop tools:ignore="ExportedReceiver"            |
  |   SmsBackup/RestoreService <service> --> android:foregroundServiceType=dataSync|
  |   <uses-permission> --> +FOREGROUND_SERVICE_DATA_SYNC  +POST_NOTIFICATIONS    |
  |   (SmsJobService ACTION_EXECUTE filter: untouched here, removed by MU-005)    |
  +------------------------------------------------------------------------------+
        |
        v
  +------------------------------------------------------------------------------+
  |  Co-landed source conformance (S3, same commit as targetSdk 35)              |
  |   AlarmManagerDriver.java:127  getService(... FLAG_UPDATE_CURRENT|IMMUTABLE)  |
  |   ServiceBase.java:204         getActivity(... FLAG_UPDATE_CURRENT|IMMUTABLE) |
  |   POST_NOTIFICATIONS runtime request, gated SDK_INT >= TIRAMISU (33)          |
  |   AlarmManagerDriverTest:97    flag assertion updated to match the OR         |
  +------------------------------------------------------------------------------+
        | ./gradlew bundleRelease (signed via keystore.properties)
        v
  +------------------------------------------------------------------------------+
  |  release.aab -- aapt2 dump badging > targetSdkVersion:'35'                    |
  |             -- Google Play Console internal testing track > pre-launch PASS   |
  |                (no rejection on targetSdk / exported / FGS type / PI mutability)|
  +------------------------------------------------------------------------------+
                              = Gate G0 / Milestone M0
```

### Why staged, not big-bang (trade-off, explicit)

| Approach | Pro | Con | Decision |
|----------|-----|-----|----------|
| Single AGP 4.1.3 → 8.x + SDK 35 diff | Fewest commits | Unbounded compounded breaking-change surface; uncharacterizable intermediate; one failure indistinguishable from another; no reversible checkpoint | **Rejected** (Constraint 4) |
| Three staged commits (this design) | Each stage independently compilable + characterization-testable; bisectable; reversible to last green stage; isolates toolchain failures from behavioral failures | More commits; reviewer must verify intermediate compilability | **Accepted** (ADR-MOD-001-A) |
| Stage SDK *inside* the AGP stages (raise SDK at each AGP bump) | Spreads behavioral conformance | AGP 4.1.3/7.x cannot build SDK 35 anyway; entangles toolchain + behavioral domains the staging exists to separate | **Rejected** |

### ADR-MOD-001-A: Stage the AGP/Gradle/SDK uplift as three independently-buildable commits

**Status:** Accepted (realizes target-state ADR-006).
**Context:** AGP 4.1.3 → 8.x spans four majors with breaking DSL/variant/namespace/JDK changes; `compileSdk 35` is unbuildable below AGP 8 + Gradle 8 + JDK 17; `warningsAsErrors`/`-Werror` weaponize every new deprecation; Robolectric 4.3.1 caps tests at SDK 29 and shares the build file (verified `app/build.gradle:41,66,75`; `build.gradle:8`; `gradle-wrapper.properties` Gradle 7.2).
**Decision:** S1 = AGP 7.4.x + Gradle 7.5.x + minSdk 21 (SDK held 29); S2 = AGP 8.x + Gradle 8.x + JDK 17 (SDK held 29); S3 = SDK 35 + all API-31–34 conformance co-landed in the `targetSdk 35` commit + jcenter removal + (co-req) Robolectric 4.12.x. `lint-baseline.xml` committed at/before S1, entries only removed thereafter; empty baseline + full `-Werror` restored before AC closure.
**Consequences:** (+) Each stage is bisectable, reversible, and characterization-testable; toolchain failures are isolated from behavioral failures; AC-6/AC-13 are structurally satisfiable. (−) More commits and reviewer effort; the scijava-jobdispatcher resolution path must be preserved through jcenter removal (called out as the primary build-break risk). (−) Hard coupling to DES-MODERNIZATION-003 at S3 — they ship as one sweep, not independently.

### ADR-MOD-001-B: Co-land all API-31–34 conformance inside the targetSdk-35 commit (atomic raise)

**Status:** Accepted.
**Context:** The raise retroactively activates per-API enforcement; PendingIntent mutability (`AlarmManagerDriver:127`, `ServiceBase:204`), receiver export (all six intent-filtered receivers), FGS type, and POST_NOTIFICATIONS each become hard failures at their threshold. AC-13 forbids any intermediate state where `targetSdk == 35` and a mutability flag is absent.
**Decision:** Every conformance fix lands in the **same commit** that sets `targetSdkVersion 35` (S3). No conformance fix lands earlier (no-op churn) or later (shipped crash). POST_NOTIFICATIONS is requested at runtime gated on `SDK_INT >= 33`, suppressed below.
**Consequences:** (+) No regressed intermediate state can ship; satisfies AC-10..27 atomically. (−) S3 is a larger single commit — mitigated by S1/S2 having already de-risked the toolchain, so S3's diff is conformance-only, not toolchain+conformance.

## Components

Component decomposition is **out of scope** for this design (section OFF). MU-001 changes build configuration, manifest declarations, two flag arguments, and one version-gated permission request. It introduces **no new runtime component, no new module, no new package, and no new public/internal interface.** The runtime component graph is owned by the substrate-swap designs (DES-MODERNIZATION-005, DES-MODERNIZATION-006, DES-MODERNIZATION-007). The single sliver of new *code* (the SDK-gated `POST_NOTIFICATIONS` request) attaches to the existing user-initiated backup/restore entry point and the existing notification path (`ServiceBase.java:174,187`); it creates no new component.

## Interfaces

No new application interface is introduced. Two **externally-visible contracts are explicitly preserved** (not created) by this design:

- **`com.zegoggles.smssync.BACKUP` broadcast (public).** `BackupBroadcastReceiver` (`AndroidManifest.xml:151`) keeps `android:exported="true"`; third-party automation (Tasker) continues to trigger backups. AC-20 verifies via `adb shell am broadcast`. Setting this `false` is a silent breaking change to a documented public contract — prohibited.
- **`android:foregroundServiceType="dataSync"` + permission declarations.** These are platform-facing manifest contracts, not app APIs; they are additive metadata.

The build/distribution "interface" is the Gradle task surface (`assembleRelease`, `bundleRelease`, `lint`, `dependencies`) and `aapt2 dump badging` — all standard, none redefined.

## Integration Design

### Integration point 1 — Google Play distribution (the terminal gate)

The AAB integrates with Google Play Console's automated **pre-launch policy check** on the internal testing track. The integration contract is implicit and Play-owned: the bundle's `targetSdkVersion` (readable via `aapt2 dump badging`, AC-29) must be ≥ 34, **every one of the six intent-filtered `<receiver>` elements** must declare `android:exported` (else the bundle does not assemble at all under AGP 7+, well before Play sees it), foreground services must declare a type, and PendingIntents must declare mutability — or the submission is rejected on that dimension (AC-30). This design's entire purpose is to satisfy that contract. There is no project-owned counterparty to negotiate with; the contract is unilateral platform policy, which is precisely why it is modeled as a **gate**, not a CNTR artifact (no two project components are agreeing on an interface).

**Submission path:** `bundleRelease` (signed via `keystore.properties` at repo root, verified present-or-unsigned logic at `app/build.gradle:3–7,35`) → upload to internal testing track → automated pre-launch review → PASS = Gate G0 cleared = M0. This is a one-time gate signal, not a recurring CI step.

### Integration point 2 — Build dependency repositories

Post-jcenter-removal, dependency resolution integrates with: `google()` (AndroidX, AGP, build-tools), `mavenCentral()` (otto, billing, junit, robolectric, truth, mockito, auto-service — verified `app/build.gradle:57,59,65–70`), `https://jitpack.io` (k-9 SHA `eaf689025e`, `app/build.gradle:58`), and — **critically retained** — `https://maven.scijava.org/...` for `firebase-jobdispatcher:0.8.6` (`app/build.gradle:60`; `build.gradle:26–27`). AC-9 verifies clean resolution on a cleared cache with `--refresh-dependencies`. The integration risk is over-removal (deleting scijava with jcenter), addressed in the JCenter Removal section.

### Integration point 3 — This design as the baseline for every downstream DES

This is the load-bearing integration property of MU-001: **DES-MODERNIZATION-001 is the compile/test baseline that DES-MODERNIZATION-002 through DES-MODERNIZATION-011 are written against.** Concretely:
- **DES-MODERNIZATION-003 (transport security and Robolectric):** S3 hard co-requisite — Robolectric 4.12.x must land in the same sweep or the test suite breaks (Constraint 1). The two designs share `app/build.gradle` and are one atomic build sweep.
- **DES-MODERNIZATION-005 (WorkManager):** consumes the SDK-35/AGP-8 baseline; removes `AlarmManagerDriver.java` (taking the interim FLAG_IMMUTABLE fix with it), `SmsJobService` plus its `ACTION_EXECUTE` filter, and the scijava mirror plus `firebase-jobdispatcher`.
- **DES-MODERNIZATION-011 (dead-code sweep):** consumes the minSdk-21 boundary this design creates; deletes the pre-21 code stranded here (`CalendarAccessorPre40`, `CalendarAccessor.Get` branch, `ServiceBase.isConnectedViaWifi_pre_SDK21`).
- **All others** — DES-MODERNIZATION-004 (secrets), DES-MODERNIZATION-006 (eventing), DES-MODERNIZATION-007 (Hilt), DES-MODERNIZATION-008 (mail ACL), DES-MODERNIZATION-009 (Billing 7.x): none can build, test, or ship to users until Gate G0 clears.

The sequencing contract is therefore: **MU-001 first, S1 then S2 then S3, with the DES-MODERNIZATION-003 Robolectric upgrade co-landing at S3.** No downstream story may be sprint-planned ahead of S3 green.

### Integration sequencing diagram

```
  S1 (AGP7.4/Gradle7.5/minSdk21, SDK 29)  - green ->
  S2 (AGP8/Gradle8/JDK17, SDK 29)         - green ->
  S3 (SDK35 + conformance + jcenter X) + DES-003(Robolectric4.12) - green ->
        |
        +-> aapt2 badging = targetSdk 35 (AC-29)
        +-> Play pre-launch PASS = Gate G0 / M0 (AC-30)
        +-> UNBLOCKS: DES-MOD-004/005/006/007/008/009/011 (none shippable before this)
```

## Design Validation

Every REQ-MODERNIZATION-001 acceptance criterion is traced to a verification mechanism. AC values are quoted from the requirement read in full this session; "verified-now" marks facts I confirmed by direct file read in this session.

| AC | Criterion (abbrev.) | Design coverage | Verification mechanism | Current state (verified-now) |
|----|---------------------|-----------------|------------------------|------------------------------|
| 1 | compile/target = 35 | S3 | `grep "compileSdkVersion\|targetSdkVersion" app/build.gradle` -> only `35` | `:10,:15` both `29` |
| 2 | minSdk = 21 | S1 | static inspection; no `minSdkVersion 14`/`<21` | `:14` = `14` |
| 3 | AGP 8.x classpath | S2 | `grep -r "com.android.tools.build:gradle"` -> only 8.x | `build.gradle:8` = `4.1.3` |
| 4 | Gradle wrapper 8.x | S2 | inspect `distributionUrl` | wrapper = `gradle-7.2-bin.zip` |
| 5 | `assembleRelease` exit 0, repos = google/mavenCentral/jitpack | S3 | CI log on cleared cache | n/a (depends on all stages) |
| 6 | 3 identifiable AGP stage commits | S1/S2/S3 | PR commit history, each independently compilable | n/a |
| 7 | no `jcenter` in gradle files | S3 | `grep -ri jcenter` -> empty | `build.gradle:4,21` present |
| 8 | no `jcenter.bintray.com` | S3 | `grep -r jcenter.bintray.com` -> empty | `build.gradle:25` present |
| 9 | clean resolve, no "Could not resolve" | S3 | `dependencies --refresh-dependencies`, cleared cache | scijava mirror must survive removal |
| 10 | `FLAG_IMMUTABLE` at AlarmManagerDriver | S3 | `grep -n FLAG_IMMUTABLE AlarmManagerDriver.java` | `:127` `FLAG_UPDATE_CURRENT` only |
| 11 | `FLAG_IMMUTABLE` at ServiceBase | S3 | `grep -n FLAG_IMMUTABLE ServiceBase.java` | `:204` `FLAG_UPDATE_CURRENT` only |
| 12 | no PendingIntent.get without a mutability flag | S3 | `grep -rn "PendingIntent\.get" app/src/main/java/` + inspect | **grep-confirmed this session: exactly 2 production sites (`AlarmManagerDriver:127`, `ServiceBase:201`); no third exists** |
| 13 | targetSdk-35 commit co-lands both flags | S3 (ADR-MOD-001-B) | PR history: no intermediate `targetSdk==35 AND flag-absent` state | n/a |
| 14 | API-31+ Robolectric scheduling test no `IllegalArgumentException` | S3 + DES-003 | test run output (authored under REQ-MOD-002); **also update `AlarmManagerDriverTest:97` flag assertion** | `AlarmManagerDriverTest:97` currently asserts `FLAG_UPDATE_CURRENT` — will break on the OR; co-update required |
| 15–18 | **All six** intent-filtered receivers explicit `exported`; `SmsBroadcastReceiver`/`BootReceiver`/`PackageReplacedReceiver`=false, `BackupBroadcastReceiver`/`.compat.SmsReceiver`/`.compat.MmsReceiver`=true; no `tools:ignore` | S3 | static inspection + `grep ExportedReceiver` -> empty | `:131,:139,:145,:159,:167` no `exported`; `:151` no `exported` + has `tools:ignore` (scope corrected — REQ-MOD-001 named only four) |
| 19 | `lint` zero `ExportedReceiver` | S3 | CI lint report | n/a |
| 20 | `am broadcast com.zegoggles.smssync.BACKUP` triggers backup on API 31+ | S3 (contract preserved) | logcat on API 31+ device | contract live at `:151–156` |
| 21–22 | `foregroundServiceType="dataSync"` on both services | S3 | static inspection | `:123,:124` no FGS type |
| 23 | `FOREGROUND_SERVICE_DATA_SYNC` permission | S3 | `grep FOREGROUND_SERVICE_DATA_SYNC` | only `FOREGROUND_SERVICE` at `:80` |
| 24 | `POST_NOTIFICATIONS` permission | S3 | `grep POST_NOTIFICATIONS` | absent |
| 25 | API 33+ requests POST_NOTIFICATIONS before first notification | S3 (gated request) | instrumentation/manual smoke on API 33+ AVD | absent |
| 26 | API 32 does NOT request, no crash | S3 (`SDK_INT >= 33` gate) | instrumentation/manual smoke on API 32 AVD | absent |
| 27 | `lint` zero ForegroundServiceType/MissingPermission | S3 | CI lint report | n/a |
| 28 | `bundleRelease` produces signed AAB | S3 | artifact presence + non-zero size | signing wired `app/build.gradle:22–35` |
| 29 | `aapt2 dump badging` -> `targetSdkVersion:'35'` | S3 | command output | would emit `29` today |
| 30 | Play pre-launch PASS, no targetSdk/exported/FGS/PI rejection | Gate G0 | Play Console submission status | n/a (terminal gate) |

**Coverage conclusion:** All 30 ACs are covered by an enabled section and assigned a stage + verification mechanism. The two device-required clusters (14/20/25/26) depend on an AVD matrix (API 31+, 33+, 32) or documented manual smoke per REQ §Verification Method — the design names the exact API levels. The one terminal gate (30) is a Play Console submission. AC-12's site enumeration was grep-confirmed this session (exactly two production sites; no third). The one test-coupling hazard surfaced by the migration-units cross-check — `AlarmManagerDriverTest:97`'s `FLAG_UPDATE_CURRENT` assertion breaking on the OR — is documented in the PendingIntent section and tied to AC-14.

**Conformance to approved standards/design.** This design realizes target-state.md **ADR-006** (compile/target 35, minSdk 21, staged AGP 4.1.3->7.4->8.x + Gradle 8, exported-explicit receivers, POST_NOTIFICATIONS + FOREGROUND_SERVICE_DATA_SYNC) without deviation. It respects all five REQ-MODERNIZATION-001 constraints (Robolectric co-req, warningsAsErrors baseline, minSdk dead-code boundary deferred to MU-011, staged AGP, AlarmManagerDriver interim status). No approved REQ/DES/standard is contradicted.

**ISO 25010 quality-attribute pass (Bass/Clements/Kazman lens).** *Compatibility/Portability* (the dominant driver): directly restored — `targetSdk 35` clears the Play floor; the staged path bounds the upgrade blast radius. *Reliability:* the atomic-conformance ADR-MOD-001-B prevents a regressed shippable intermediate (no `targetSdk==35`-without-mutability-flag state). *Security:* this gate is the precondition that lets the Critical trust-all-TLS fix reach users; `FLAG_IMMUTABLE` additionally closes a PendingIntent-tampering surface (immutable intents cannot be hijacked by a holder). *Maintainability:* jcenter removal eliminates a sunset-CDN non-determinism; `lint-baseline.xml` keeps deprecation debt a monotonically-decreasing fitness function. *Performance/Usability/Functional-Suitability:* not materially affected by a version/manifest change — explicitly evaluated and found not applicable.

## Integration Contracts

> Required for any design introducing cross-component boundaries.

**This design introduces NO new cross-component (CNTR-eligible) boundary, and therefore requires no CNTR-\* artifact.** Rationale: a CNTR contract governs an interface agreed between two *project-owned* components (producer/consumer). MU-001 changes build configuration, manifest metadata, two flag arguments, and one version-gated permission call. The boundaries it touches are:

| Boundary | Producer | Consumer(s) | Contract Type | CNTR Artifact | Status |
|----------|----------|-------------|---------------|---------------|--------|
| App AAB -> Google Play pre-launch policy | this app | Google Play (external platform) | platform-gate (unilateral) | — | **N/A — external unilateral gate, not a project contract** |
| Build -> dependency repositories | Gradle | google/mavenCentral/jitpack/scijava (external) | service | — | **N/A — external** |
| `com.zegoggles.smssync.BACKUP` broadcast | 3rd-party automation | `BackupBroadcastReceiver` | event (pre-existing) | — | **Preserved, not introduced — out of CNTR scope** |
| MU-001 build baseline -> downstream DES | this design | other designs | service (design-sequencing) | — | **N/A — design ordering, not a runtime interface** |

### Contracts Needed (pre-sprint gate)

- **None.** No `/amp:create-contracts` run is required before stories referencing this design can be sprint-planned. The public `BACKUP` broadcast is a pre-existing contract that this design *preserves* (verified by AC-20); it is not a new boundary. Should the implementing team choose to formalize the public `BACKUP` broadcast as a CNTR artifact for downstream protection, that is a *separate, optional* hardening action and is **not** a gating prerequisite for MU-001.

## Trade-offs

- **Staged (3 commits) vs. big-bang AGP/SDK jump** — see ADR-MOD-001-A. Accepted the extra commits + reviewer burden to gain bisectability, reversibility, and isolation of toolchain vs. behavioral failures. A big-bang diff cannot be characterization-tested at intermediate states (Constraint 4).
- **Co-land conformance vs. follow-on fixes** — see ADR-MOD-001-B. Accepted a larger S3 commit to guarantee no regressed intermediate state ships (AC-13). S1/S2 having de-risked the toolchain keeps S3 conformance-only.
- **`dataSync` FGS type** — chosen over alternatives (`shortService`, none) because backup/restore is precisely long-running network data transfer; `shortService` (≤ ~3 min, no special permission) would truncate large-mailbox runs and contradicts the existing 10-minute wakelock window at `ServiceBase.java:120`.
- **minSdk raise at S1 vs. S3** — placed at S1 to isolate compat-surface fallout from SDK-raise fallout, accepting that it strands dead code one full migration-unit before MU-011 deletes it. The stranded code is inert (provably unreachable below the new floor), so the cost is cosmetic until MU-011.
- **Retain scijava mirror through jcenter removal** — accepted a lingering single-purpose mirror (dies with MU-005) rather than risk an unresolvable `firebase-jobdispatcher` at S3. The alternative (remove both now) is out of scope and would break AC-5/AC-9.
- **`lint-baseline.xml` vs. relaxing `-Werror`** — chose a shrink-only baseline over disabling `warningsAsErrors`, keeping deprecation debt a monotonically-decreasing fitness function (Constraint 2) instead of losing the signal entirely.

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Over-eager jcenter cleanup also removes the scijava mirror -> `firebase-jobdispatcher` unresolvable, S3 build fails | Medium | High | Scope jcenter removal to the three verified lines only (`build.gradle:4,21,25`); AC-9 dependency check on cleared cache; called out for Code Reviewer gating. |
| `-Werror`/`warningsAsErrors` fails build mid-sweep on new deprecations (already-suppressed cases at `ServiceBase.java:186,215,225`) | High | Medium | `lint-baseline.xml` at S1; shrink-only policy; restore empty baseline + `-Werror` before AC closure (Constraint 2). |
| `AlarmManagerDriverTest:97` flag assertion breaks when FLAG_IMMUTABLE is OR'd in | High | Low | Co-update the assertion in the same S3 commit; surfaced by the migration-units cross-check; tied to AC-14. |
| AGP 8 requires JDK 17 — CI/dev toolchain mismatch at S2 | Medium | Medium | Pin JDK 17 in CI and document in the S2 commit; S2 isolates this so a JDK failure is unambiguous. |
| Robolectric 4.12.x (DES-003 co-req) introduces test-API breaks at S3, blocking AC-14 | Medium | Medium | S3 is one atomic sweep with DES-MODERNIZATION-003; characterization tests gate the sweep; do not declare AC-14 closed until the API-31+ scheduling test passes. |
| Setting `BackupBroadcastReceiver` exported=false by mistake silently breaks the public Tasker contract | Low | High | AC-17/AC-20 explicitly verify `exported="true"` + live `am broadcast`; called out in receiver table. |
| Play pre-launch rejects on a dimension outside the four anticipated (e.g., a new policy) | Low | Medium | Submit to internal testing track first (not production); treat any rejection as a re-open of Gate G0, not a partial pass. |

## Notes

**Audit-trail note.** Every load-bearing fact in this design was confirmed by direct Read and/or Grep of the actual files this session: `build.gradle`, `app/build.gradle`, `AndroidManifest.xml` (full, 207 lines), `gradle/wrapper/gradle-wrapper.properties`, `AlarmManagerDriver.java` (full), `ServiceBase.java` (full), `migration-units.md` MU-001 (full), plus `REQ-MODERNIZATION-001`, `target-state.md`, and `code-location.md` in full. The AC-12 two-site PendingIntent enumeration was grep-confirmed (exactly two production sites; `AlarmManagerDriver:127`, `ServiceBase:201`). The migration-units cross-check additionally surfaced a real test-coupling hazard not present in the requirement — `AlarmManagerDriverTest:97` asserts the current `FLAG_UPDATE_CURRENT` flag and will break when FLAG_IMMUTABLE is OR'd in — now documented against AC-14. No design claim rests on an unread file.

**Out-of-scope (owned elsewhere), recorded to prevent scope creep.** WorkManager migration and removal of `AlarmManagerDriver`, `SmsJobService`, firebase, and scijava are owned by MU-005 and DES-MODERNIZATION-005. The Robolectric, mockito, and junit upgrade bodies are owned by REQ-MODERNIZATION-002 and DES-MODERNIZATION-003 (co-landed at S3 but specified there). Pre-21 dead-code deletion is owned by MU-011. Trust-all-TLS removal and EncryptedSharedPreferences are owned by DES-MODERNIZATION-004 and DES-MODERNIZATION-007. Billing 7.x is owned by DES-MODERNIZATION-009. This design's boundary is exactly: SDK, AGP, and Gradle versions; manifest conformance; two FLAG_IMMUTABLE additions; one SDK-gated POST_NOTIFICATIONS request; and jcenter removal.

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| REQ-MODERNIZATION-001 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-001-restore-play-store-eligibility.md | The requirement this design realizes — all 30 ACs, 5 constraints, key-file-locations (read in full) |
| Target Architecture (ADR-006) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/target-state.md | Staged AGP path, SDK/minSdk decision, manifest conformance (read in full) |
| Migration units (MU-001) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md | MU-001 boundary/components/acceptance, Shared-File Allocation, dependency graph — read in full this session; surfaced the AlarmManagerDriverTest:97 flag-assertion coupling |
| Code Location (engagement) | sdlc/artifacts/engagement/code-location.md | Tech stack, build/test commands, current SDK levels, signing, constraints (read in full) |
| build.gradle (root) | build.gradle | Verified AGP 4.1.3 (:8), jcenter (:4,:21), jcenter.bintray.com (:25), scijava mirror (:26-27) |
| app/build.gradle | app/build.gradle | Verified compile/target 29 (:10,:15), minSdk 14 (:14), buildTools 29.0.2 (:11), warningsAsErrors (:41), robolectric 4.3.1 (:66), firebase-jobdispatcher (:60), -Werror (:75), deps |
| AndroidManifest.xml | app/src/main/AndroidManifest.xml | Verified all six intent-filtered receivers lacking exported (:131,:139,:145,:151,:159,:167), tools:ignore (:151), services lacking FGS type (:123,:124), FOREGROUND_SERVICE only (:80), SmsJobService ACTION_EXECUTE (:125-129); .compat.HeadlessSmsSendService (:190) already exported=true (out of scope) |
| gradle-wrapper.properties | gradle/wrapper/gradle-wrapper.properties | Verified Gradle 7.2 |
| AlarmManagerDriver.java | app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java | Verified PendingIntent.getService FLAG_UPDATE_CURRENT at :127; firebase Driver (MU-005 deletion target) |
| ServiceBase.java | app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java | Verified PendingIntent.getActivity FLAG_UPDATE_CURRENT at :201-205 (:204 token); SDK_INT gate idiom (:208); notification path (:174,:187) |
| AlarmManagerDriverTest.java | app/src/test/java/com/zegoggles/smssync/service/AlarmManagerDriverTest.java | Grep-confirmed :97 asserts getFlags() == FLAG_UPDATE_CURRENT — co-update required in S3 |
| PendingIntent.get grep | app/src/main/java | Grep-confirmed exactly 2 production construction sites (AC-12 scope) |
| settings.gradle | settings.gradle | Verified module set :app, :metadata |

## Verdict

**PASS** — System architecture, integration design, and design validation are complete and grounded in source verified by direct read/grep this session: `build.gradle`, `app/build.gradle`, `AndroidManifest.xml` (full), `gradle-wrapper.properties`, `AlarmManagerDriver.java` (full), `ServiceBase.java` (full), `migration-units.md` MU-001 (full), plus REQ-MODERNIZATION-001, target-state.md (ADR-006), and code-location.md in full. The staged uplift is specified as three independently-buildable stages with explicit ordering rationale and two ADRs (staging; atomic conformance co-land) carrying Context/Decision/Consequences and named rejected alternatives. All 30 REQ-MODERNIZATION-001 acceptance criteria are traced to a stage and a verification mechanism against the verified current state. The AC-12 two-site PendingIntent enumeration was grep-confirmed (no third site); the cross-check against migration-units surfaced a real test-coupling hazard (`AlarmManagerDriverTest:97`) now documented against AC-14. The primary build-break risk (over-removing the scijava mirror with jcenter) is called out explicitly for Code Reviewer gating. No CNTR artifact is required (no new project-owned cross-component boundary; the public BACKUP broadcast is preserved, not introduced). The design conforms to target-state ADR-006 and all five REQ constraints without deviation. Manifest receiver conformance covers all six intent-filtered receivers (audit-corrected; see Manifest Conformance §Scope correction), correcting REQ-MODERNIZATION-001's four-receiver scope so the implementing story does not reproduce the gap.
