---
artifact_id: 20260529-modernization
step: 1.2
title: File Inventory — SMS Backup+
generated: 2026-05-29T00:00:00Z
subject: sms-backup-plus
source_root: app/src
---

# File Inventory — SMS Backup+

**Engagement:** 20260529-modernization  
**Step:** 1.2 — File Inventory  
**Generated:** 2026-05-29  
**Subject:** `app/` module (`com.zegoggles.smssync`) — P01 only (P02 `metadata` is store-listing assets, out of scope for runtime analysis)

---

## File Inventory Summary

### Overview

| Metric | Count |
|--------|-------|
| Total Tracked Files (repo) | 286 |
| Production Java Source Files | 107 |
| Test Java Files | 36 |
| Android Resource Files (XML) | 45 |
| Image Assets (PNG/SVG/Sketch, metadata module) | 49 |
| Configuration / Build Files | 11 |
| Documentation Files (.md, CHANGES, COPYING, etc.) | 10 |
| Metadata Module Files (P02, peripheral) | 76 |

> **Counting method:** `git ls-files | sed 's/.*\.//' | sort | uniq -c` — lists all tracked files by extension. Run 2026-05-29. Excludes `.git/` and build outputs (not tracked). See Verification Evidence below.

---

### Source Code by Language

| Language | Files | Total Lines | Lines of Code | % of Total LOC |
|----------|-------|-------------|---------------|----------------|
| Java (production) | 107 | 10,635 | 8,428 | 74.4% |
| Java (test) | 36 | 3,550 | 2,896 | 25.6% |
| **Java Total** | **143** | **14,185** | **11,324** | **100%** |
| XML (Android resources) | 45 | 7,915 | — | — |
| Gradle (build scripts) | 4 | ~140 | — | — |

> Note: No Kotlin, no scripting languages, no generated code in source tree.

---

### Lines of Code Breakdown — Production Java

| Category | Lines | % |
|----------|-------|---|
| Code | 8,428 | 79.2% |
| Comments | 734 | 6.9% |
| Blank | 1,473 | 13.9% |
| **Total** | **10,635** | **100%** |

> **Counting method:** `find app/src/main -name "*.java" | xargs awk` with inline blank/comment/code classifier. Blank = empty or whitespace-only line; Comment = lines starting with `//`, `/*`, or `*` (block comment continuation). Run 2026-05-29.

### Lines of Code Breakdown — Test Java

| Category | Lines | % |
|----------|-------|---|
| Code | 2,896 | 81.6% |
| Comments | 14 | 0.4% |
| Blank | 640 | 18.0% |
| **Total** | **3,550** | **100%** |

---

### Test Coverage Indicators

| Metric | Value |
|--------|-------|
| Test Files | 36 |
| Production Source Files | 107 |
| Ratio (Test:Source files) | 0.34 : 1 |
| Test LOC | 2,896 |
| Production LOC | 8,428 |
| Ratio (Test LOC : Prod LOC) | 0.34 : 1 |
| Packages with test coverage | 10 of 19 production packages |
| Packages with no tests | 9 (see Anomalies) |

> Industry norm for mature Java/Android projects is 0.5 : 1 to 1 : 1 test-to-source ratio by LOC. SMS Backup+ sits below that at 0.34 : 1. However, coverage is strongest in the highest-risk packages (`service`, `mail`), which mitigates the overall ratio gap.

---

## File Distribution by Directory

```
app/src/
  main/
    AndroidManifest.xml                         [207 lines — 1 file]
    java/com/zegoggles/smssync/
      (root)                                    [3 files: App.java, Consts.java, MmsConsts.java]
      activity/                                 [6 files — MainActivity.java is the largest at 499 lines]
        auth/                                   [3 files]
        donation/                               [3 files]
        events/                                 [9 files — all tiny event POJO classes]
        fragments/                              [4 files]
      auth/                                     [4 files]
      calendar/                                 [3 files]
      compat/                                   [6 files including package-info.java]
      contacts/                                 [4 files]
      mail/                                     [14 files — 2nd largest package by LOC]
      preferences/                              [8 files]
      receiver/                                 [4 files]
      service/                                  [17 files — largest package]
        exception/                              [8 files — typed exception hierarchy]
        state/                                  [4 files — state machine value objects]
      tasks/                                    [1 file]
      utils/                                    [6 files]
    res/
      color/                                    [3 files]
      drawable/                                 [2 files]
      drawable-anydpi/                          [4 files]
      drawable-anydpi-v26/                      [1 file]
      layout/                                   [3 files]
      menu/                                     [1 file]
      values/                                   [5 files: arrays, defaults, keys, strings, styles]
      values-ca/ through values-zh-rTW/         [24 locale dirs × 1 strings.xml each = 24 files]
      xml/                                      [2 files: backup_descriptor.xml, preferences.xml]
  test/
    java/com/zegoggles/smssync/
      (root)                                    [1 file: AppTest.java]
      activity/                                 [1 file]
        donation/                               [2 files]
      auth/                                     [2 files]
      calendar/                                 [1 file]
      compat/                                   [1 file]
      contacts/                                 [2 files]
      mail/                                     [9 files]
      preferences/                              [2 files]
      receiver/                                 [2 files]
      service/                                  [12 files]
        state/                                  [1 file]

metadata/                                       [76 files — store listing PNGs, SVG, Sketch, text]
gradle/wrapper/                                 [2 files]
build.gradle, settings.gradle, gradle.properties, etc.  [~8 root config files]
```

---

## Large Files Requiring Review

Files at or approaching the 500-line threshold, or with notable complexity concentration:

| File | Lines | Concern |
|------|-------|---------|
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` | 499 | God-class risk: combines event-bus subscriber, OAuth2 orchestration, backup trigger UI, preference management, permission handling, and dialog dispatch in one class |
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java` | 386 | Preference fragment with inline business logic; difficult to test |
| `app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java` | 360 | Custom view + business logic in one class |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java` | 348 | `AsyncTask` subclass with all restore orchestration; `AsyncTask` is deprecated since API 30 |
| `app/src/main/java/com/zegoggles/smssync/activity/Dialogs.java` | 346 | Monolithic dialog factory with mixed concerns |
| `app/src/main/java/com/zegoggles/smssync/mail/MessageGenerator.java` | 322 | Core SMS/MMS-to-email conversion logic; complex branching |
| `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` | 318 | Foreground service orchestration mixed with state machine transitions |
| `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` | 307 | `AsyncTask` subclass with all backup orchestration; deprecated since API 30 |
| `app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java` | 307 | Single facade for all app preferences; grows with each new setting |
| `app/src/main/java/com/zegoggles/smssync/activity/donation/DonationActivity.java` | 304 | Billing client lifecycle mixed into activity |
| `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` | 289 | Auth credential storage mixed with preference key logic |
| `app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java` | 289 | Raw `HttpsURLConnection` OAuth2 flow; no retry, no modern auth library |
| `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` | 235 | WakeLock/WifiLock management plus foreground service lifecycle |
| `app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java` | 228 | IMAP store extension with custom search queries |
| `app/src/main/java/com/zegoggles/smssync/App.java` | 227 | Application singleton, Otto bus init, notification channel setup |

---

## Anomalies Identified

1. **`AppLog.java` listed twice in solution-inventory `utils` package** — Only one file exists at `app/src/main/java/com/zegoggles/smssync/utils/AppLog.java` (208 lines). The double-listing in the solution-inventory was a documentation copy-paste error; no actual duplication exists in the filesystem.

2. **`activity/events/` has 9 files but zero test coverage** — The entire Otto event-bus POJO layer (`AccountAddedEvent`, `PerformAction`, etc.) has no dedicated tests. These are simple data carriers so risk is low, but event contract regressions would be silent.

3. **`service/exception/` has 8 exception classes with no direct test coverage** — Exception construction and message localization are only exercised indirectly through `BackupTaskTest` and `RestoreTaskTest`. No unit tests verify exception message keys or inheritance.

4. **`tasks/` package contains a single file** (`OAuth2CallbackTask.java`, 1 file) with no test — A package for a single `AsyncTask` subclass suggests incomplete decomposition or a vestigial remnant of a planned tasks layer.

5. **`utils/` package has no test coverage** — `AppLog.java` (file-based log), `ThreadHelper.java` (SMS thread ID lookup), `Sanitizer.java`, and `BundleBuilder.java` are all untested. `ThreadHelper` in particular does ContentResolver I/O which could benefit from Robolectric tests.

6. **`CalendarAccessorPre40.java` serves API < 14** — The `minSdk` is 14, so this class is dead code for all supported devices. The API-level compatibility split in `CalendarAccessor` (`CalendarAccessorPre40` vs `CalendarAccessorPost40`) targets pre-ICS (API < 14), which is below `minSdk`.

7. **24 locale `values-*/strings.xml` files with no tooling for translation validation** — The repository has no linting configuration specific to translation completeness. Missing keys in locale files would only be caught at runtime.

8. **`AllTrustedSocketFactory.java` exists in production** — This class bypasses TLS certificate validation ("trust all hosts"), intended as a debug/fallback path. It is referenced from `BackupImapStore` and `AuthPreferences`. While it may be gated by a preference flag, the presence of a cert-validation bypass in production code is a security anomaly.

9. **`compat/SmsReceiver.java`, `compat/MmsReceiver.java`, `compat/ComposeSmsActivity.java`, `compat/HeadlessSmsSendService.java` are stub implementations required only when the app acts as default SMS app** — These stubs add Manifest components unconditionally but serve no functional purpose unless the user explicitly makes SMS Backup+ the default SMS app during restore.

10. **Firebase JobDispatcher (`com.firebase:firebase-jobdispatcher`) is deprecated and unmaintained** — The library has been superseded by `androidx.work` (WorkManager) since 2019. `BackupJobs.java`, `SmsJobService.java`, and `AlarmManagerDriver.java` are all tied to this deprecated scheduling infrastructure.

---

## Narrative Summary

### Codebase Composition

SMS Backup+ is a compact, single-module Android application with 107 production Java files totalling 10,635 lines and 36 test files totalling 3,550 lines. The codebase is 100% Java — there is no Kotlin, no scripting, and no generated source code in the tracked tree. The production-to-test line ratio of approximately 3:1 places the project below the industry norm of 1:1 to 2:1 for mature Android projects, though coverage is concentrated where it matters most: the `service` and `mail` packages, which contain the backup/restore engine and IMAP conversion logic respectively, together account for 31 production files and 21 of the 36 test files.

Resource files add another 45 XML files (7,915 lines) covering layouts, drawables, color definitions, a preferences XML tree, and 24 locale-specific `strings.xml` translations. These localisation files represent a meaningful maintenance surface: they cannot be validated for completeness without tooling, and any new preference key added in `values/strings.xml` must be manually propagated to 24 translations.

The metadata module (P02) contributes 76 files, almost entirely Play Store screenshot PNGs across 7 locales plus Fastlane/F-Droid metadata text. It has no bearing on runtime behaviour and is confirmed out of scope for modernization analysis.

### Notable Patterns

The `service` package (17 files) is the single largest package by file count, followed by `mail` (14 files) and `activity/events` (9 files). The `service` package size reflects the application's core complexity: backup and restore orchestration, job scheduling (Firebase JobDispatcher with AlarmManager fallback), state machine management, and the exception hierarchy all live here. The `mail` package covers the entire SMS/MMS/call-log-to-email conversion domain, which is architecturally appropriate but dense: `MessageGenerator.java` at 322 lines and `MessageConverter.java` at 215 lines handle format transformation, header generation, MMS attachment handling, and person-name lookup — multiple concerns that would benefit from decomposition.

`MainActivity.java` at 499 lines is a clear god-class candidate: it acts as an Otto event bus subscriber, an OAuth2 flow orchestrator, a backup/restore trigger dispatcher, and a permissions handler simultaneously. The activity layer in general (`AdvancedSettings.java` at 386 lines, `StatusPreference.java` at 360 lines, `Dialogs.java` at 346 lines) concentrates view logic and business logic in ways that resist unit testing and increase coupling.

### Implications for Modernization

Three technology choices in the current codebase have reached end-of-life or deprecated status and are primary modernization targets:

1. **`AsyncTask`** (`BackupTask`, `RestoreTask`, `OAuth2CallbackTask`) — deprecated in Android API 30 (Android 11) and removed in API 33 (Android 13). Replacement with `Kotlin Coroutines` or `java.util.concurrent` is required for API 33+ compatibility.

2. **Firebase JobDispatcher** — deprecated since 2019 in favour of `WorkManager`. `BackupJobs`, `SmsJobService`, and `AlarmManagerDriver` all depend on it. Migration to `WorkManager` would also consolidate the dual `GooglePlayDriver`/`AlarmManagerDriver` dispatch path.

3. **`AllTrustedSocketFactory`** — a production-present TLS certificate bypass. Modernization should evaluate whether this is reachable in production builds and, if so, replace it with proper certificate pinning or standard trust stores.

Secondary modernization targets include: raw `HttpsURLConnection` OAuth2 implementation in `OAuth2Client` (no token refresh safety, no modern auth library), the Square Otto event bus (archived since 2016; `LiveData`/`StateFlow` are idiomatic Android replacements), and the `CalendarAccessorPre40` dead-code shim for API < 14.

### Areas Requiring Further Investigation

The following areas warrant deeper review in subsequent assessment prompts:

- **`MainActivity.java` (499 lines)** — Responsibilities should be inventoried for decomposition. Architecture assessment should evaluate whether ViewModel + LiveData migration is feasible.
- **`AllTrustedSocketFactory` reachability** — The tech-debt assessment should trace all call paths to determine whether the cert bypass is reachable in release builds.
- **`service/exception` localization** — Exception message key mapping should be traced to confirm all keys are present in all 24 locale files.
- **`CalendarAccessorPre40` dead code** — Confirm it is unreachable given `minSdk 14` and recommend removal.
- **Test coverage gap for `utils/`, `tasks/`, `activity/events/`, `service/exception/`** — Determine whether the gap is acceptable risk or needs remediation as part of modernization.

---

## Recommendations

- **Prioritize `AsyncTask` migration** — With `targetSdk 29` already at Android 10, raising `targetSdk` to 33+ (required by Play Store policy) will make deprecated `AsyncTask` usage a build-time issue. Plan migration to `WorkManager` workers or coroutines before any SDK bump.
- **Migrate Firebase JobDispatcher to WorkManager** — `firebase-jobdispatcher` is archived. WorkManager also subsumes the `AlarmManagerDriver` fallback, reducing scheduling code surface.
- **Audit and gate `AllTrustedSocketFactory`** — Confirm whether the class is accessible in release builds. If it is, replace with standard TLS trust handling or explicit user-facing warnings.
- **Decompose `MainActivity`** — Extract OAuth2 orchestration into a dedicated ViewModel or use-case; extract backup/restore trigger logic into a controller class. The 499-line activity is the highest-priority refactoring candidate in the UI layer.
- **Add translation completeness tooling** — With 24 locale files, missing strings fail silently at runtime. A lint check or CI script to verify key completeness is strongly recommended.
- **Remove `CalendarAccessorPre40`** — The class targets API < 14, which is below `minSdk`. It is dead code and should be removed to reduce surface area.

---

## Verification Evidence

| Metric | Method Used | Timestamp | Exclusions |
|--------|-------------|-----------|------------|
| Total tracked file count | `git ls-files \| wc -l` | 2026-05-29 | `.git/`, build outputs (not tracked) |
| File counts by extension | `git ls-files \| sed 's/.*\.//' \| sort \| uniq -c` | 2026-05-29 | `.git/`, build outputs |
| Java production file count | `find app/src/main -name "*.java" \| wc -l` | 2026-05-29 | build/, .git/ |
| Java test file count | `find app/src/test -name "*.java" \| wc -l` | 2026-05-29 | build/, .git/ |
| Lines of code (production) | `find app/src/main -name "*.java" \| xargs awk` (inline blank/comment/code classifier) | 2026-05-29 | build/, .git/ |
| Lines of code (test) | `find app/src/test -name "*.java" \| xargs awk` (same classifier) | 2026-05-29 | build/, .git/ |
| Large files | `find app/src/main -name "*.java" -exec wc -l {} \; \| sort -rn` | 2026-05-29 | build/, .git/ |
| XML resource lines | `find app/src/main/res -name "*.xml" \| xargs awk 'END{print NR}'` | 2026-05-29 | build/, .git/ |
| Package distribution | `find ... \| sed ... \| sort \| uniq -c` | 2026-05-29 | build/, .git/ |
| Deprecated API usage | `find ... -exec grep -l "AsyncTask\|FirebaseJobDispatcher" {} \;` | 2026-05-29 | build/, .git/ |

> **Note on cloc:** `cloc` was not available in the environment. Line counting was performed using `awk`-based scripts producing equivalent blank/comment/code breakdowns. The verification script at `library/scripts/verification/count-loc.sh` was not used as it requires `cloc`. Manual counting via `awk` was applied as the documented fallback.
