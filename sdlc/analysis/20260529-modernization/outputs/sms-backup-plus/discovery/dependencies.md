# Dependency Analysis — sms-backup-plus

**Assessment ID:** 20260529-modernization
**Date:** 2026-05-29
**Analyst:** Developer Agent (claude-sonnet-4-6)
**Repo root:** `C:/Code/Android/sms-backup-plus` (all file paths below are relative to repo root)

> **Verification Note:** This is an Android/Gradle project. The standard Java/Gradle tooling
> (`mvn dependency:tree`, `gradle dependencies`) is not available in the analysis environment.
> All dependency data has been sourced by manual inspection of `build.gradle`, `app/build.gradle`,
> `gradle/wrapper/gradle-wrapper.properties`, `app/src/main/AndroidManifest.xml`, and
> all Java source `import` statements. Transitive dependency counts are estimated from
> known library dependency graphs; exact transitive counts require a running Gradle
> environment (`./gradlew :app:dependencies`). This limitation is noted in the
> Verification Evidence table.

---

## Summary

| Metric | Count |
|--------|-------|
| Direct Dependencies (production) | 7 |
| Direct Dependencies (test) | 4 + 1 annotation processor |
| Estimated Transitive Dependencies | ~60–80 (est.) |
| Total Direct + Test | 12 |
| Outdated (Major version behind) | 5 |
| Outdated (Minor version behind) | 2 |
| Deprecated / Archived | 3 |
| Known Vulnerable | 0 (no CVE scan available; see note) |
| Internal Modules | 10 |
| External Services | 4 |

### Health Rating: At Risk

The project carries three deprecated/archived libraries — one with no upstream replacement in the
declared repository ecosystem — and its build toolchain (AGP 4.1.3, Gradle 7.2, compileSdk 29)
lags the current Android standard by roughly four major cycles. jcenter() is still declared as a
repository source despite the service being shut down. The k9mail-library dependency is pinned to a
raw git SHA with no versioned release, creating an unreproducible supply chain.

---

## Canonical Dependency Findings

### DEP-001: jcenter() Repository Still Declared — Service Shut Down
- **Severity:** High
- **Confidence:** 1.00
- **Category:** Deprecated
- **Location:** `build.gradle` lines 5, 22, 25; `app/build.gradle` implicitly inherits via `allprojects`
- **Evidence:** JCenter (Bintray) shut down on 1 February 2022. Both `buildscript` and `allprojects` blocks
  still list `jcenter()` and `https://jcenter.bintray.com` as repository URLs. Any artifact that resolves
  only from JCenter will silently fail or resolve from a stale mirror. Gradle will currently succeed only
  because all artifacts happen to mirror on mavenCentral() or google(), but this is fragile and will break
  as mirrors age.
- **Remediation:** Remove all `jcenter()` and `https://jcenter.bintray.com` entries from `build.gradle`.
  Confirm each artifact resolves from `mavenCentral()`, `google()`, or `https://jitpack.io`.
- **Effort:** S

---

### DEP-002: firebase-jobdispatcher 0.8.6 — Deprecated, Unofficial Repository
- **Severity:** Critical
- **Confidence:** 1.00
- **Category:** Deprecated
- **Location:** `app/build.gradle` line 60; `build.gradle` line 27 (scijava repo workaround)
- **Evidence:** Firebase JobDispatcher was officially deprecated by Google in 2019 when WorkManager
  (Jetpack) became the stable recommended replacement. The library has had no updates since v0.8.6
  (2018-01-24). The build comment in `build.gradle` (`// This is the only repo that seems to be hosting
  "com.firebase:firebase-jobdispatcher" in 2024`) explicitly acknowledges reliance on a non-official
  mirror (`https://maven.scijava.org/content/repositories/public/`). This creates a supply-chain risk:
  the artifact is not served from a Google-controlled endpoint and the upstream repo owner could remove or
  alter it at any time. The library also requires Google Play Services (`GooglePlayDriver`) and has a
  manual fallback (`AlarmManagerDriver`) — both code paths remain in production source.
- **Remediation:** Migrate job scheduling to `androidx.work:work-runtime` (WorkManager). `SmsJobService`
  (extends `com.firebase.jobdispatcher.JobService`) must be rewritten as a `ListenableWorker` or
  `Worker`. `BackupJobs.java` and `AlarmManagerDriver.java` must be rewritten. This is a medium-effort
  refactor touching `service/BackupJobs.java`, `service/SmsJobService.java`,
  `service/AlarmManagerDriver.java`, and `AndroidManifest.xml`.
- **Effort:** M

---

### DEP-003: com.squareup:otto 1.3.8 — Archived, No Updates Since 2016
- **Severity:** High
- **Confidence:** 1.00
- **Category:** Deprecated
- **Location:** `app/build.gradle` line 57
- **Evidence:** Square officially archived the Otto event bus in 2016 and recommends RxJava or
  Greenrobot EventBus as replacements. The library has not received updates since version 1.3.8
  (released 2014). It is pervasively used: `import com.squareup.otto.Subscribe` appears in 12 source
  files spanning `App.java`, `MainActivity`, `Dialogs`, `AdvancedSettings`, `MainSettings`,
  `OAuth2WebAuthActivity`, `BackupTask`, `RestoreTask`, `SmsBackupService`, `SmsRestoreService`,
  `SmsJobService`, and `StatusPreference`. `@Produce` is used in `SmsBackupService` and
  `SmsRestoreService`.
- **Remediation:** Replace with Greenrobot EventBus 3.x (`org.greenrobot:eventbus`) which is a
  near-drop-in replacement for Otto (different annotations but same pattern), or refactor to use
  LiveData / StateFlow if Kotlin migration is underway. Estimated 12 files require annotation
  changes.
- **Effort:** M

---

### DEP-004: com.github.jberkel.k-9:k9mail-library pinned to Git SHA
- **Severity:** High
- **Confidence:** 1.00
- **Category:** Outdated
- **Location:** `app/build.gradle` line 58
- **Evidence:** Declared as `com.github.jberkel.k-9:k9mail-library:eaf689025e` — a raw commit SHA
  resolved via JitPack. This is a fork of the k-9 mail library maintained by the same author as this
  project. Problems: (1) SHA-pinned JitPack dependencies are not reproducible across clean environments
  if JitPack evicts the build cache; (2) there is no semantic version, so security advisories cannot
  reference this artifact; (3) the upstream k-9 Mail project (now Thunderbird for Android) has released
  many versions since this SHA was committed, including security-relevant IMAP fixes. The library
  provides all IMAP store functionality (`com.fsck.k9.mail.*`) — it is a core dependency with no
  alternative in the current codebase.
- **Remediation:** Two options: (a) Vendor the library (copy source into the repo) to guarantee
  reproducibility and add it as an `:app` module, or (b) upgrade to the latest published k-9
  library version or the Thunderbird equivalent if an API-compatible release exists. Option (a) is
  lower risk for short-term stability; option (b) is better long-term.
- **Effort:** L

---

### DEP-005: Android Gradle Plugin 4.1.3 — Multiple Major Versions Behind
- **Severity:** High
- **Confidence:** 1.00
- **Category:** Outdated
- **Location:** `build.gradle` line 8
- **Evidence:** AGP 4.1.3 was released in 2021. Current stable is AGP 8.x (as of early 2025).
  AGP 4.x does not support compileSdk 34+, newer R8 features, Gradle Configuration Cache, or the
  modern DSL used by AGP 7+/8+. Remaining on AGP 4.x also blocks upgrading Gradle past 7.x and
  blocks adoption of the new declarative Gradle DSL. AGP 4.x support from Google is long ended.
- **Remediation:** Upgrade AGP incrementally: 4.1.3 → 7.4.x → 8.x. Each major AGP upgrade
  requires a corresponding Gradle wrapper upgrade and may require DSL changes. Reference
  the AGP upgrade assistant (`./gradlew generateUpgradeReport`). Pair with compileSdk/targetSdk
  bump (see DEP-007).
- **Effort:** M

---

### DEP-006: Gradle Wrapper 7.2 — Outdated
- **Severity:** Medium
- **Confidence:** 1.00
- **Category:** Outdated
- **Location:** `gradle/wrapper/gradle-wrapper.properties` line 3
- **Evidence:** Gradle 7.2 was released September 2021. Current stable is Gradle 8.x. Gradle 7.2
  lacks performance improvements (configuration cache GA, build cache improvements), toolchain
  support improvements, and is incompatible with AGP 8.x. Note: AGP 4.1.3 requires Gradle 6.5–7.x,
  so the wrapper version is consistent with the current AGP but will need to be bumped alongside AGP.
- **Remediation:** Upgrade Gradle wrapper to 8.x as part of the AGP upgrade path described in DEP-005.
  Update `gradle/wrapper/gradle-wrapper.properties` `distributionUrl`.
- **Effort:** S (bundled with DEP-005)

---

### DEP-007: compileSdk / targetSdk 29 — Severely Outdated, Google Play Policy Violation Risk
- **Severity:** Critical
- **Confidence:** 1.00
- **Category:** Outdated
- **Location:** `app/build.gradle` lines 10, 15
- **Evidence:** compileSdk 29 corresponds to Android 10 (released 2019). Google Play Store has
  required targetSdk 33+ since August 2023 and will require targetSdk 34+ for new submissions and
  updates. An app submitted to Google Play today with targetSdk 29 would be rejected. Additionally,
  minSdk 14 (Android 4.0 ICS) is far below the current Google Play minimum recommendation (minSdk 21)
  and below Jetpack's practical minimum. Staying on compileSdk 29 also prevents use of any API
  introduced after Android 10.
- **Remediation:** Bump `compileSdkVersion` and `targetSdkVersion` to 34 (minimum to pass Play Store
  policy as of 2024) or 35. Update `buildToolsVersion` accordingly. Audit `AndroidManifest.xml`
  permission declarations — several are deprecated or require updated attributes on API 31+
  (e.g., `android:exported` must be explicit on all components). minSdk should be raised to at least 21.
- **Effort:** M

---

### DEP-008: com.android.billingclient:billing 2.1.0 — Four Major Versions Behind
- **Severity:** Medium
- **Confidence:** 1.00
- **Category:** Outdated
- **Location:** `app/build.gradle` line 59
- **Evidence:** billing 2.1.0 was released in 2019. Current stable is billing 7.x (2024).
  Google has deprecated the `SkuDetails` API (used extensively in `DonationActivity.java`,
  `DonationListFragment.java`, `Sku.java`) in favour of the `ProductDetails` API introduced in
  billing 5.0. Google Play will eventually stop supporting the old billing API. Major breaking
  API changes occurred at billing 5.0 (querySkuDetailsAsync → queryProductDetailsAsync).
- **Remediation:** Upgrade to `com.android.billingclient:billing:7.x`. Rewrite `DonationActivity.java`
  to use `queryProductDetailsAsync` and `ProductDetails` in place of `querySkuDetailsAsync` and
  `SkuDetails`. This is isolated to the donation subsystem (3 files).
- **Effort:** M

---

### DEP-009: androidx.annotation / androidx.preference / androidx.core Versions Outdated
- **Severity:** Low
- **Confidence:** 0.90
- **Category:** Outdated
- **Location:** `app/build.gradle` lines 61–63
- **Evidence:**
  - `androidx.annotation:annotation:1.1.0` — current is 1.9.x
  - `androidx.preference:preference:1.1.0` — current is 1.2.x
  - `androidx.core:core-role:1.0.0-beta01` — this is a beta artifact; current stable is 1.1.x
  These are minor version gaps; no breaking API changes are expected, but staying on 1.1.0 of
  preference misses bug fixes and the `core-role` beta01 reference is unusual for a production build.
- **Remediation:** Bump to current stable versions. The `core-role` beta01 declaration should be
  resolved to a stable release.
- **Effort:** S

---

### DEP-010: Test Dependencies Outdated
- **Severity:** Low
- **Confidence:** 0.90
- **Category:** Outdated
- **Location:** `app/build.gradle` lines 65–70
- **Evidence:**
  - `junit:junit:4.12` — current is 4.13.2; 4.12 has a known vulnerability (CVE-2020-15250, rule
    exposure via temp file). Should upgrade even in test scope.
  - `org.robolectric:robolectric:4.3.1` — current is 4.12.x; 4.3.1 only supports up to
    SDK 29, blocking any targetSdk bump.
  - `com.google.truth:truth:0.39` — current is 1.4.x (API-stable upgrade)
  - `org.mockito:mockito-all:1.10.17` — `mockito-all` is an uber-jar deprecated in favour of
    `mockito-core`; current mockito-core is 5.x. Version 1.10.17 is from 2015.
  - `com.google.auto.service:auto-service:1.0-rc4` — current stable is 1.1.1
- **Remediation:** Upgrade all test dependencies. Replace `mockito-all` with `mockito-core`.
  Robolectric upgrade is tied to targetSdk bump (DEP-007); must happen together.
- **Effort:** S

---

## External Package Dependencies

### By Category

| Category | Count | Key Packages |
|----------|-------|--------------|
| Email / IMAP | 1 | k9mail-library |
| Event Bus | 1 | otto |
| Job Scheduling | 1 | firebase-jobdispatcher |
| In-App Billing | 1 | billing |
| AndroidX / Jetpack | 3 | annotation, preference, core-role |
| Testing | 4 + 1 | junit, robolectric, truth, mockito-all, auto-service |
| Build Toolchain | 2 | AGP, Gradle wrapper |

### Full Inventory — Production Dependencies

| Package | Version | Latest (est.) | Type | License | Status |
|---------|---------|---------------|------|---------|--------|
| `com.squareup:otto` | 1.3.8 | 1.3.8 (archived) | Direct | Apache 2.0 | Archived/Deprecated |
| `com.github.jberkel.k-9:k9mail-library` | eaf689025e (SHA) | n/a (git SHA) | Direct | Apache 2.0 | Unpinned SHA / Risky |
| `com.android.billingclient:billing` | 2.1.0 | 7.x | Direct | Android SDK License | Outdated (Major) |
| `com.firebase:firebase-jobdispatcher` | 0.8.6 | 0.8.6 (deprecated) | Direct | Apache 2.0 | Deprecated |
| `androidx.annotation:annotation` | 1.1.0 | 1.9.x | Direct | Apache 2.0 | Outdated (Minor) |
| `androidx.preference:preference` | 1.1.0 | 1.2.x | Direct | Apache 2.0 | Outdated (Minor) |
| `androidx.core:core-role` | 1.0.0-beta01 | 1.1.x | Direct | Apache 2.0 | Beta in production |

### Full Inventory — Test Dependencies

| Package | Version | Latest (est.) | Type | License | Status |
|---------|---------|---------------|------|---------|--------|
| `junit:junit` | 4.12 | 4.13.2 | Direct (test) | EPL 1.0 | Outdated (CVE-2020-15250) |
| `org.robolectric:robolectric` | 4.3.1 | 4.12.x | Direct (test) | MIT | Outdated (Major) |
| `com.google.truth:truth` | 0.39 | 1.4.x | Direct (test) | Apache 2.0 | Outdated (Major) |
| `org.mockito:mockito-all` | 1.10.17 | n/a (deprecated artifact) | Direct (test) | MIT | Deprecated artifact |
| `com.google.auto.service:auto-service` | 1.0-rc4 | 1.1.1 | Annotation proc | Apache 2.0 | Outdated |

### Build Toolchain

| Component | Version | Latest (est.) | Status |
|-----------|---------|---------------|--------|
| Android Gradle Plugin | 4.1.3 | 8.x | Outdated (Major) |
| Gradle wrapper | 7.2 | 8.x | Outdated (Major) |
| compileSdk / targetSdk | 29 | 35 | Outdated (Critical) |
| buildTools | 29.0.2 | 34.x | Outdated |
| minSdk | 14 | 21 (recommended) | Below Google Play recommendation |

### Outdated Packages Summary

| Package | Current | Latest | Versions Behind | Breaking Changes |
|---------|---------|--------|-----------------|------------------|
| `com.android.billingclient:billing` | 2.1.0 | 7.x | 5 major | Yes (billing 5.0 API rewrite) |
| AGP | 4.1.3 | 8.x | 4 major | Yes |
| Gradle | 7.2 | 8.x | 1 major | Yes (some DSL) |
| `org.robolectric:robolectric` | 4.3.1 | 4.12.x | ~9 minor | Partial |
| `com.google.truth:truth` | 0.39 | 1.4.x | 1 major | Minor |
| `junit:junit` | 4.12 | 4.13.2 | patch | No |
| `androidx.annotation` | 1.1.0 | 1.9.x | ~8 minor | No |
| `androidx.preference` | 1.1.0 | 1.2.x | 1 minor | No |
| `com.google.auto.service` | 1.0-rc4 | 1.1.1 | 1 (rc→stable) | No |

### Deprecated / Unmaintained

| Package | Issue | Last Updated (est.) | Replacement |
|---------|-------|---------------------|-------------|
| `com.squareup:otto` | Officially archived by Square | 2016 (v1.3.8) | Greenrobot EventBus 3.x, or LiveData/Flow |
| `com.firebase:firebase-jobdispatcher` | Deprecated by Google 2019; unofficial mirror only | 2018 (v0.8.6) | `androidx.work:work-runtime` (WorkManager) |
| `org.mockito:mockito-all` | Deprecated uber-jar artifact; use `mockito-core` | ~2016 | `org.mockito:mockito-core:5.x` |

### License Summary

| License Type | Count | Packages | Concern |
|--------------|-------|----------|---------|
| Apache 2.0 | 9 | otto, k9mail-library, firebase-jobdispatcher, annotation, preference, core-role, truth, auto-service | None — compatible with project Apache 2.0 license |
| MIT | 1 | robolectric, mockito-all | None |
| EPL 1.0 | 1 | junit | Weak copyleft; test-only scope, not distributed — no concern |
| Android SDK License | 1 | billing | Google proprietary; standard Play app license |

> No GPL/LGPL dependencies were identified. License risk is low.

---

## Internal Module Dependencies

### Module Map

```
┌────────────────────────────────────────────────────────────────────┐
│  App.java  (Application entry point)                               │
│  Registers Otto Bus, initialises BackupJobs, configures K9MailLib  │
└───────────────┬────────────────────────────────────────────────────┘
                │
        ┌───────▼──────────────────────────────────────────────┐
        │  activity.*  (UI layer)                               │
        │  MainActivity, ThemeActivity, Dialogs                 │
        │  auth/*, donation/*, fragments/*                      │
        └───────┬──────────────────────────────────────────────┘
                │ Otto events
        ┌───────▼──────────────────────────────────────────────┐
        │  service.*  (Background work)                         │
        │  SmsBackupService, SmsRestoreService, SmsJobService   │
        │  BackupJobs, AlarmManagerDriver                       │
        │  BackupTask, RestoreTask, BackupState, RestoreState   │
        └───────┬──────────────────────────────────────────────┘
                │
    ┌───────────┼────────────────────┐
    ▼           ▼                    ▼
┌────────┐  ┌────────┐         ┌──────────────┐
│ mail.* │  │ prefs.*│         │  receiver.*  │
│IMAP ops│  │ Auth,  │         │ Boot,SMS,MMS │
│Converters│ │ DataType│       │ Broadcast    │
└────────┘  └────────┘         └──────────────┘
    │            │
    ▼            ▼
┌────────────────────────────────┐
│  contacts.*, calendar.*        │
│  (Contact/Calendar accessors)  │
└────────────────────────────────┘
    │
    ▼
┌───────────────────┐
│  utils.*          │
│  (AppLog,Drawables│
│  ThreadHelper etc)│
└───────────────────┘
```

### Module Inventory

| Module (package suffix) | Purpose | Key External Dependency |
|-------------------------|---------|------------------------|
| `App.java` / root | Application bootstrap, Otto bus, GCM check | otto, k9mail-library |
| `activity.*` | UI activities and preference fragments | otto, billing |
| `activity.auth.*` | OAuth2 / AccountManager authentication | (Android SDK) |
| `activity.donation.*` | In-app donation via Google Play Billing | billing |
| `service.*` | Background backup/restore orchestration | firebase-jobdispatcher, otto, k9mail-library |
| `service.state.*` | Backup/restore state machines | k9mail-library |
| `mail.*` | IMAP store, message conversion, MMS | k9mail-library |
| `preferences.*` | App preferences, auth preferences | androidx.preference |
| `contacts.*` | Contact group/person lookup | (Android SDK) |
| `calendar.*` | Calendar read/write | (Android SDK) |
| `receiver.*` | Broadcast receivers (SMS, boot, package) | (Android SDK) |
| `compat.*` | SMS/MMS compatibility shims | (Android SDK) |
| `auth.*` | OAuth2 token refresh | (Android SDK) |
| `utils.*` | Logging, drawables, thread helpers | androidx.annotation, androidx.core |
| `tasks.*` | OAuth2 callback async task | (Android SDK) |

### Dependency Metrics (estimated)

| Module | Afferent Coupling (In) | Efferent Coupling (Out) | Instability |
|--------|------------------------|------------------------|-------------|
| `App.java` | 0 (root) | 4 (service, prefs, receiver, bus) | 1.0 (unstable) |
| `mail.*` | 4 (service, task, state, App) | 1 (k9mail-library) | 0.2 (stable) |
| `service.*` | 2 (App, receiver) | 5 (mail, prefs, contacts, jobdispatcher, otto) | 0.7 |
| `activity.*` | 0 | 4 (service, prefs, auth, otto) | 1.0 |
| `preferences.*` | 5 (service, activity, auth, App, calendar) | 1 (androidx.preference) | 0.2 (stable) |
| `utils.*` | many | 2 (annotation, appcompat) | 0.3 |

### Circular Dependencies

No circular dependencies were detected in the package-level import graph. All observable
imports flow in one direction: `activity → service → mail` and `activity → preferences`.
The `App.java` class is referenced from many packages as a static utility (bus `post`/`register`)
but this does not create package-level cycles.

---

## External Service Dependencies

| Service | Type | Purpose | Criticality | Fallback |
|---------|------|---------|-------------|----------|
| IMAP server (Gmail or custom) | Network/IMAP | Backup destination; message read/write | Critical | None (app function fails entirely) |
| Google OAuth2 / AccountManager | Auth API | Token issuance for XOAuth2 / account binding | High | IMAP plain-text password (fallback auth mode) |
| Google Play Services (GCM/FCM) | OS service | Job scheduling via `firebase-jobdispatcher` | Medium | `AlarmManagerDriver` fallback is present |
| Google Play Billing | SDK | In-app donation purchase flow | Low | App works without donation; gracefully degraded |
| Google Calendar API | ContentProvider | Writing call log entries to calendar | Low | Disabled if permission not granted |
| Google Backup API | Android backup | App data backup metadata key | Low | No backup if key missing |

### Integration Points

| Service | Called From | Auth Method | Error Handling |
|---------|-------------|-------------|----------------|
| IMAP server | `mail/BackupImapStore.java` | XOAuth2 / plain password | `MessagingException` hierarchy; retried via BackupTask |
| Google OAuth2 | `auth/OAuth2Client.java`, `auth/TokenRefresher.java` | Android AccountManager + OAuth2 | `TokenRefreshException`; UI prompt on failure |
| Google Play Services | `service/BackupJobs.java` via `GooglePlayDriver` | n/a (OS-level) | Falls back to `AlarmManagerDriver` if unavailable |
| Google Play Billing | `activity/donation/DonationActivity.java` | BillingClient SDK | `BillingResult` error codes; silently fails |

### Failure Impact

| Service | If Unavailable | Timeout Configured | Retry Logic |
|---------|----------------|-------------------|-------------|
| IMAP server | Backup/restore impossible | No explicit timeout in code | Exponential backoff via firebase-jobdispatcher |
| Google OAuth2 | Cannot authenticate; UI falls back to password entry | No explicit timeout | Manual retry only |
| Google Play Services | Job scheduling falls back to AlarmManager | n/a | AlarmManager reschedule on boot |
| Google Play Billing | Donation flow unavailable; app still fully functional | No | No retry |

---

## Dependency Risks

### High Risk Items

| ID | Risk | Description | Affected Dependencies | Severity |
|----|------|-------------|----------------------|----------|
| DEP-001 | jcenter() shutdown | Build will silently fail for any artifact not mirrored elsewhere | `build.gradle` | High |
| DEP-002 | firebase-jobdispatcher unofficial mirror | Unofficial Maven repo; supply-chain integrity not verifiable | `firebase-jobdispatcher` | Critical |
| DEP-003 | Otto archived | No security patches; deprecated by vendor | `otto` | High |
| DEP-004 | k9mail SHA pin | Unreproducible builds; no version tag | `k9mail-library` | High |
| DEP-007 | targetSdk 29 | Google Play policy violation; app cannot be submitted/updated | `app/build.gradle` | Critical |

### Concentration Risk

| Package | Used By (files) | Risk Level |
|---------|----------------|------------|
| `k9mail-library` | 15+ source files across `mail/*`, `service/*`, `preferences/*` | Critical — entire IMAP layer depends on this single SHA-pinned library |
| `com.squareup:otto` | 12 source files | High — bus is used throughout all layers |
| `firebase-jobdispatcher` | 3 source files (`BackupJobs`, `SmsJobService`, `AlarmManagerDriver`) | High — all background scheduling goes through this path |

### Version Conflicts

No intra-project version conflicts were detected in the direct dependency declarations.
However, transitive conflicts are likely given the age delta between declared versions and
current releases. Specifically:
- `firebase-jobdispatcher:0.8.6` transitively depends on `com.google.android.gms:play-services-gcm`
  which has not been updated and may conflict with other Google Play Services components.
- `robolectric:4.3.1` bundles its own bouncycastle (`bcprov`) which conflicts with bouncycastle
  brought in by k9mail-library — this is explicitly noted in `gradle.properties`:
  `android.jetifier.blacklist=.*bcprov.*`.

---

## Internal Module Dependency Tree (Condensed)

```
:app
├── com.squareup:otto:1.3.8
│   └── (no declared transitive deps)
├── com.github.jberkel.k-9:k9mail-library:eaf689025e  [JitPack SHA]
│   ├── org.bouncycastle:bcprov-jdk15on (transitive, exact version unknown)
│   ├── com.sun.mail:android-mail (transitive)
│   └── ... (other JavaMail / crypto transitive deps)
├── com.android.billingclient:billing:2.1.0
│   └── com.google.android.gms:play-services-base (transitive)
├── com.firebase:firebase-jobdispatcher:0.8.6
│   └── com.google.android.gms:play-services-gcm (transitive, deprecated)
├── androidx.annotation:annotation:1.1.0
├── androidx.preference:preference:1.1.0
│   ├── androidx.fragment:fragment (transitive)
│   ├── androidx.appcompat:appcompat (transitive)
│   └── androidx.recyclerview:recyclerview (transitive)
└── androidx.core:core-role:1.0.0-beta01
    └── androidx.core:core (transitive)

Test dependencies:
├── junit:junit:4.12
├── org.robolectric:robolectric:4.3.1
│   ├── com.google.truth:truth (transitive subset)
│   ├── org.bouncycastle:bcprov-jdk15on  [CONFLICT with k9mail — mitigated by jetifier blacklist]
│   └── ... android SDK stubs, shadows
├── com.google.truth:truth:0.39
└── org.mockito:mockito-all:1.10.17
```

---

## Narrative Summary

### Dependency Landscape

SMS Backup+ is a lean Android application with only seven direct production dependencies, which
is appropriate for its scope. The dependency graph is shallow: the most-used library (k9mail-library)
is a single dependency that supplies the entire IMAP layer, and all other libraries are narrow
utilities (event bus, billing, job dispatcher, androidx). The project does not exhibit "npm
install everything" syndrome — every declared dependency has a clear functional purpose.

However, the quality of those seven dependencies is poor in aggregate. Three are deprecated or
archived by their original maintainers, one is pinned to a raw git SHA (making it effectively
untraceable from a security standpoint), and one is sourced from a third-party Maven mirror
because no official host remains. The implicit "eighth dependency" — the build toolchain — is
similarly stale: AGP 4.1.3, Gradle 7.2, and compileSdk/targetSdk 29 are all multiple major
versions behind current stable, and the combination of these places the project in a position
where it cannot be submitted to the Google Play Store without remediation.

### Health Assessment Rationale

The At Risk rating is driven by three compounding factors. First, the project depends on
`firebase-jobdispatcher` via an unofficial scijava Maven mirror — a supply-chain risk that has
no parallel in well-maintained projects and that the build file itself acknowledges as a workaround.
Second, targetSdk 29 is a hard blocker for Google Play distribution, meaning the app cannot receive
updates through the primary distribution channel. Third, jcenter() is declared as a repository
source despite the service being shut down in 2022. Two of these three issues (targetSdk and
firebase-jobdispatcher) require non-trivial refactoring rather than a simple version bump.

### Risk Analysis

The most consequential single-point-of-failure is the k9mail-library: all IMAP backup and restore
functionality runs through this library's `ImapStore`, `ImapFolder`, `ImapMessage`, and related
classes, which are referenced across 15+ source files. If the JitPack build cache for SHA
`eaf689025e` is evicted, a clean build in a CI environment will fail with no available fallback.
The firebase-jobdispatcher supply-chain risk is secondary but meaningful — a malicious or accidental
artifact replacement at `maven.scijava.org` would be incorporated into the next build without
any checksum warning, since Gradle does not verify artifact provenance beyond the configured
repositories.

The Otto event bus permeates the architecture (12 files, all layers). Its archived status means
no security patches will ever be issued; if a vulnerability is found in otto's byte-code
manipulation, the project has no upstream to turn to. Replacing otto is a medium-effort refactor
but a necessary one given the project's Apache 2.0 license and the responsibility to avoid
distributing known-vulnerable code.

### Implications for Modernization Assessment

The dependency health directly shapes the modernization effort. The targetSdk/compileSdk gap
(DEP-007) is an unconditional blocker for Google Play distribution and must be addressed first
as a prerequisite for all other work. The firebase-jobdispatcher → WorkManager migration (DEP-002)
is likely the most structurally impactful change because WorkManager imposes a different job
lifecycle model than the FirebaseJobDispatcher API. The otto → EventBus/LiveData migration (DEP-003)
is broad (12 files) but mechanically straightforward. Together, these three efforts constitute
the minimum viable modernization to restore Play Store distribution eligibility and eliminate
critical supply-chain exposure.

---

## Recommendations

### Immediate Actions (do first — blockers)

1. **Remove jcenter()** from all repository blocks in `build.gradle` (DEP-001, effort S).
2. **Bump targetSdk to 34** and update all manifest `android:exported` attributes on components
   (DEP-007, effort M) — this is a Google Play Store prerequisite.
3. **Resolve firebase-jobdispatcher supply chain risk**: either vendor the 0.8.6 artifact
   locally or begin the WorkManager migration (DEP-002, effort M).

### Short-term (1–3 months)

1. **Migrate firebase-jobdispatcher to WorkManager** (DEP-002, effort M): rewrite
   `service/BackupJobs.java`, `service/SmsJobService.java`, and `service/AlarmManagerDriver.java`.
2. **Upgrade AGP to 7.4 then 8.x** with matching Gradle wrapper upgrades (DEP-005/DEP-006, effort M).
   The incremental path reduces risk: 4.1.3 → 7.4.x → 8.x.
3. **Upgrade Play Billing to 7.x** and rewrite donation flow to use `ProductDetails` API (DEP-008, effort M).
4. **Replace otto** with Greenrobot EventBus 3.x or LiveData (DEP-003, effort M).
5. **Upgrade all test dependencies** including Robolectric (tied to targetSdk bump) (DEP-010, effort S).
6. **Resolve k9mail-library SHA pin**: vendor or find a versioned release (DEP-004, effort L).

### Ongoing Practices

- [ ] Enable Dependabot or Renovate Bot for automated Android/Gradle dependency PRs.
- [ ] Add Gradle dependency verification (`gradle/verification-metadata.xml`) to detect supply-chain
      tampering on `maven.scijava.org` artifacts.
- [ ] Define a policy: no SHA-pinned JitPack dependencies in production; require versioned releases.
- [ ] Run `./gradlew :app:dependencies` in CI to detect transitive conflicts on every PR.
- [ ] Add OWASP Dependency-Check Gradle plugin (`org.owasp.dependencycheck`) to the build for
      ongoing CVE scanning.
- [ ] Track Google Play minimum targetSdk requirements annually and update before the enforcement date.

---

## Verification Evidence

| Metric | Method Used | Timestamp | Notes |
|--------|-------------|-----------|-------|
| Direct production deps (7) | Manual read of `app/build.gradle` `dependencies {}` block | 2026-05-29 | Deterministic; all 7 entries confirmed |
| Direct test deps (5) | Manual read of `app/build.gradle` `testImplementation` / `testAnnotationProcessor` | 2026-05-29 | Deterministic |
| Build toolchain versions | Manual read of `build.gradle` + `gradle/wrapper/gradle-wrapper.properties` | 2026-05-29 | Deterministic |
| Import usage (which src files use each lib) | `Grep` against `app/src/main/java/**/*.java` on `^import com.(squareup|firebase|android.billingclient|fsck)` | 2026-05-29 | Full result set reviewed |
| Transitive deps (est. 60–80) | Estimated from known library graphs; NOT verified by `./gradlew :app:dependencies` | 2026-05-29 | Tooling not available in analysis environment; count is approximation |
| Vulnerability scan | Not performed — no `./gradlew dependencyCheckAnalyze` or `ossindex` available | 2026-05-29 | Recommendation: run OWASP Dependency-Check; CVE-2020-15250 in junit:4.12 noted from known advisories |
| Latest versions | Based on analyst knowledge (cutoff Aug 2025); not verified against live Maven Central | 2026-05-29 | Confirm with `./gradlew :app:dependencies --refresh-dependencies` |
