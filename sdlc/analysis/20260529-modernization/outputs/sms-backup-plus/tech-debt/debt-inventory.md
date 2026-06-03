# Technical Debt Inventory — SMS Backup+

**Generated:** 2026-05-29T00:00:00Z  
**Engagement:** 20260529-modernization  
**Repository:** sms-backup-plus  
**Scope:** P01 `app` module only (107 production Java files, ~9,162 LOC; 36 test files, ~2,910 LOC)

---

## Verification Evidence

| Metric | Method Used | Timestamp | Raw Count |
|--------|-------------|-----------|-----------|
| TODO/FIXME/HACK markers | `Grep` pattern search across `app/src` | 2026-05-29 | 6 markers |
| Large files (>300 LOC) | PowerShell `Get-ChildItem` + `Measure-Object` line count | 2026-05-29 | 6 files |
| `@SuppressWarnings` / `@SuppressLint` | `Grep` pattern search | 2026-05-29 | 29 occurrences |
| `extends AsyncTask` usages | `Grep` pattern search | 2026-05-29 | 3 classes |
| `import com.squareup.otto` | `Grep` pattern search | 2026-05-29 | 11 files |
| `import com.firebase.jobdispatcher` | `Grep` pattern search | 2026-05-29 | 6 files |
| Deprecated API calls (`getNetworkInfo`, `NetworkInfo`) | `Grep` pattern search | 2026-05-29 | 4 production call-sites |
| `jcenter()` repository usage | Read `build.gradle` | 2026-05-29 | 3 declarations |

*Note: `library/scripts/verification/search-patterns.py`, `count-complexity.py`, `cloc`, and `radon` are not installed in this environment. All metrics were obtained via Glob/Grep manual analysis. Counts are exact from grep output.*

---

## Summary

| Category | Items | Effort (est.) | Impact |
|----------|-------|---------------|--------|
| Code Debt | 8 | M–L | High |
| Architecture Debt | 5 | L | Critical |
| Test Debt | 4 | M | High |
| Documentation Debt | 3 | S | Low |
| Dependency Debt | 6 | M–L | Critical |
| Infrastructure Debt | 4 | S–M | High |
| Design Debt | 2 | L | High |

### Overall Debt Rating: High

The codebase carries a cluster of **modernization-blocking** dependency and infrastructure debt items (targetSdk 29, deprecated firebase-jobdispatcher, archived Otto, jcenter shutdown) that must be resolved before the app can be published to the Play Store or safely maintained. Beyond these blockers, architectural debt from `AsyncTask` usage and deep Otto coupling represents the second tier of urgency.

---

## Code Debt Items

### High-Impact Items

| ID | Issue | Location | Type | Effort | Severity |
|----|-------|----------|------|--------|----------|
| CD-001 | `AsyncTask` used for backup/restore workers | `service/BackupTask.java`, `service/RestoreTask.java`, `tasks/OAuth2CallbackTask.java` | Deprecated API | M | High |
| CD-002 | `getNetworkInfo()` / `getActiveNetworkInfo()` deprecated API | `service/ServiceBase.java:220–229`, `service/SmsBackupService.java:179` | Deprecated API | S | High |
| CD-003 | `AllTrustedSocketFactory` disables all TLS certificate validation | `mail/AllTrustedSocketFactory.java` | Security smell | S | High |
| CD-004 | `minifyEnabled false` in release build | `app/build.gradle:33` | Build config smell | S | Medium |
| CD-005 | Raw `HttpsURLConnection` for OAuth2 and Contacts API; manual JSON/XML parsing | `auth/OAuth2Client.java` | Primitive obsession / DIY HTTP | M | Medium |
| CD-006 | `PendingIntent` created without `FLAG_IMMUTABLE` | `service/AlarmManagerDriver.java:127`, `service/ServiceBase.java:201` | Deprecated API (Android 12+) | S | High |
| CD-007 | `@SuppressWarnings("deprecation")` used 12 times to silence compiler warnings mandated by `-Werror` | Multiple files (see evidence) | Warning suppression debt | S | Medium |
| CD-008 | Unimplemented `onSaveInstanceState` / `onRestoreInstanceState` in `StatusPreference` | `activity/StatusPreference.java:132–139` | Stub / TODO debt | S | Low |

---

### CD-001: AsyncTask Usage for Core Workers

- **Severity:** High
- **Confidence:** 1.00
- **Category:** Code Debt
- **Location:** `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java:50`, `service/RestoreTask.java:48`, `tasks/OAuth2CallbackTask.java:15`
- **Evidence:**
  ```java
  class BackupTask extends AsyncTask<BackupConfig, BackupState, BackupState> {
  class RestoreTask extends AsyncTask<RestoreConfig, RestoreState, RestoreState> {
  public class OAuth2CallbackTask extends AsyncTask<String, Void, OAuth2Token> {
  ```
  `AsyncTask` was deprecated in API level 30 (Android 11) and is scheduled for removal. All three classes carry `@SuppressLint("StaticFieldLeak")` to silence the associated memory-leak warning, acknowledging the problem without fixing it.
- **Remediation:** Replace `BackupTask` and `RestoreTask` with Kotlin Coroutines (`CoroutineScope` scoped to the foreground service), or with a `ListenableFuture` / `Executor`-based approach if staying on Java. Replace `OAuth2CallbackTask` with an inline coroutine or `ExecutorService` call.
- **Effort:** M — three classes to rewrite; the `publishProgress` / `onProgressUpdate` contract must be re-expressed as `Flow` or callback-based emission; tests must be updated.

---

### CD-002: Deprecated `ConnectivityManager.getNetworkInfo()` API

- **Severity:** High
- **Confidence:** 1.00
- **Category:** Code Debt
- **Location:** `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java:216–229`, `service/SmsBackupService.java:179`
- **Evidence:**
  ```java
  // ServiceBase.java:216
  @SuppressWarnings("deprecation")
  private boolean isConnectedViaWifi_pre_SDK21() {
      return getConnectivityManager().getNetworkInfo(TYPE_WIFI) != null
          && getConnectivityManager().getNetworkInfo(TYPE_WIFI).isConnected();
  }
  // ServiceBase.java:224
  @SuppressWarnings("deprecation")
  private boolean isConnectedViaWifi_SDK21() {
      final android.net.NetworkInfo networkInfo = getConnectivityManager().getNetworkInfo(network);
  }
  // SmsBackupService.java:179
  android.net.NetworkInfo active = getConnectivityManager().getActiveNetworkInfo();
  ```
  `ConnectivityManager.getNetworkInfo()` and `getActiveNetworkInfo()` were deprecated in API 29 and removed in API 31. The pre-SDK21 path also hard-codes `TYPE_WIFI`, which was deprecated in API 28.
- **Remediation:** Replace with `ConnectivityManager.registerDefaultNetworkCallback()` or `NetworkCapabilities` API (available since API 23). Remove the pre-SDK21 code path entirely (minSdk is already 14 but the app targets API 29+, and deploying on API 21+ is the practical reality).
- **Effort:** S — isolated in two files; the `isConnectedViaWifi()` method and the legacy connectivity check are small but require API-level testing.

---

### CD-003: AllTrustedSocketFactory — Blanket TLS Bypass

- **Severity:** High
- **Confidence:** 1.00
- **Category:** Code Debt (Security Smell)
- **Location:** `app/src/main/java/com/zegoggles/smssync/mail/AllTrustedSocketFactory.java`
- **Evidence:**
  ```java
  @SuppressLint("TrustAllX509TrustManager")
  class AllTrustedSocketFactory implements TrustedSocketFactory {
      private static class InsecureX509TrustManager implements X509TrustManager {
          public void checkClientTrusted(...) {} // empty — trusts all
          public void checkServerTrusted(...) {} // empty — trusts all
          public X509Certificate[] getAcceptedIssuers() { return null; }
      }
  }
  ```
  When users enable "Trust all certificates" in settings, all IMAP TLS certificate validation is disabled, exposing credentials to MITM attacks. The file comment acknowledges the trust-all logic was deliberately removed from k-9 upstream.
- **Remediation:** Remove the blanket trust-all option and replace with a proper certificate pinning or a user-visible certificate trust dialog (Android's `KeyChain` APIs). At minimum add a prominent warning and require explicit per-connection confirmation.
- **Effort:** S — single class replacement; UX change in settings required.

---

### CD-004: `minifyEnabled false` in Release Build

- **Severity:** Medium
- **Confidence:** 1.00
- **Category:** Code Debt
- **Location:** `app/build.gradle:33`
- **Evidence:**
  ```groovy
  buildTypes {
      release {
          minifyEnabled false
          proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
      }
  }
  ```
  Code shrinking and obfuscation are disabled. The ProGuard rules file exists but is unused. This produces a larger APK and exposes internal class names. The unused `proguardFiles` declaration is misleading.
- **Remediation:** Enable `minifyEnabled true` and `shrinkResources true` (R8 is the default shrinker since AGP 3.4). Update/validate `proguard-rules.pro` to retain public API entry points.
- **Effort:** S — toggling the flag is trivial; validating that R8 does not break the k-9 library or Firebase dependencies requires a test build cycle.

---

### CD-005: Hand-Rolled HTTP Client for OAuth2 and Contacts API

- **Severity:** Medium
- **Confidence:** 0.95
- **Category:** Code Debt
- **Location:** `app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java`
- **Evidence:**
  The class manually constructs `HttpsURLConnection`, writes POST bodies by hand, parses JSON responses via `OAuth2Token.fromJSON` (custom), and parses the Contacts API response via SAX XML. There is no retry logic, no timeout configuration, and no connection pooling.
  ```java
  private HttpsURLConnection postTokenEndpoint(String payload) throws IOException {
      HttpsURLConnection connection = (HttpsURLConnection) new URL(TOKEN_URL).openConnection();
      connection.setDoOutput(true);
      ...
  }
  ```
  Additionally, the Google Contacts API (`/m8/feeds/`) used here is deprecated (Google deprecated the GData Contacts API in 2021); see `auth/OAuth2Client.java:107`.
- **Remediation:** Replace `HttpsURLConnection` with OkHttp (already available transitively via k-9) or a lightweight `HttpClient` wrapper. Replace the deprecated Contacts API call with the People API or Google Sign-In `GoogleSignInAccount.getEmail()`. Use the Google Auth Library or `GoogleAuthUtil` (via `AccountManager`) for OAuth2 token management instead of raw token exchange.
- **Effort:** M — OAuth2 client replacement touches auth flow end-to-end; requires careful testing of the token exchange, refresh, and fallback paths.

---

### CD-006: `PendingIntent` Missing `FLAG_IMMUTABLE`

- **Severity:** High
- **Confidence:** 1.00
- **Category:** Code Debt
- **Location:** `app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java:127`, `service/ServiceBase.java:201`
- **Evidence:**
  ```java
  // AlarmManagerDriver.java:127
  return PendingIntent.getService(ctx, 0, intent, FLAG_UPDATE_CURRENT);
  // ServiceBase.java:201
  return PendingIntent.getActivity(getApplicationContext(), 0, intent, FLAG_UPDATE_CURRENT);
  ```
  Android 12 (API 31) requires `PendingIntent.FLAG_IMMUTABLE` or `FLAG_MUTABLE` to be set explicitly. `FLAG_UPDATE_CURRENT` alone throws `IllegalArgumentException` at runtime on Android 12+. This is a **runtime crash** on modern Android versions when `targetSdk >= 31`.
- **Remediation:** Add `FLAG_IMMUTABLE` to both `PendingIntent` calls: `FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE`.
- **Effort:** S — two-line fix; however, it is blocked by the targetSdk upgrade (currently 29).

---

### CD-007: Widespread `@SuppressWarnings("deprecation")` to Satisfy `-Werror`

- **Severity:** Medium
- **Confidence:** 1.00
- **Category:** Code Debt
- **Location:** Multiple files (29 `@SuppressWarnings`/`@SuppressLint` occurrences; 12 are `"deprecation"`)
- **Evidence:**
  Key locations: `App.java:148`, `auth/TokenRefresher.java:89`, `preferences/AuthPreferences.java:127,152`, `service/ServiceBase.java:186,215,225`, `service/SmsBackupService.java:177,293`, `service/SmsRestoreService.java:161`, `activity/Dialogs.java:159,165,183,225`.
  The `-Werror` compiler flag means any unaddressed deprecation causes a build failure; the team routinely suppresses warnings rather than fixing the underlying deprecated API usage.
- **Remediation:** Replace deprecated APIs where possible (see CD-001, CD-002, CD-006) and remove `@SuppressWarnings`. For remaining legitimate suppressions (e.g. `versionCode` deprecation), accept them with documented justification.
- **Effort:** S — no code change beyond addressing the root APIs; tracking effort is part of other CD items.

---

### CD-008: Unimplemented State Persistence in `StatusPreference`

- **Severity:** Low
- **Confidence:** 1.00
- **Category:** Code Debt
- **Location:** `app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java:131–139`
- **Evidence:**
  ```java
  @Override
  public Parcelable onSaveInstanceState() {
      // TODO implement
      return super.onSaveInstanceState();
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {
      // TODO implement
      super.onRestoreInstanceState(state);
  }
  ```
  The status widget loses its visual state on configuration changes (rotation) because instance state is not saved. This creates a brief flash of incorrect state when the activity is recreated.
- **Remediation:** Implement `onSaveInstanceState` to persist the current `SmsSyncState` enum value and progress count; restore in `onRestoreInstanceState`.
- **Effort:** S — self-contained; well-understood Android pattern.

---

## Architecture Debt

| ID | Issue | Affected Components | Severity |
|----|-------|---------------------|----------|
| AD-001 | Square Otto event bus — archived, no longer maintained | All 11 subscriber/producer files | Critical |
| AD-002 | `SmsJobService` manually instantiates `SmsBackupService` (bypasses Android lifecycle) | `service/SmsJobService.java:82–84` | High |
| AD-003 | `App.bus` process-global singleton tightly couples UI to service layer | All packages | High |
| AD-004 | `BackupTask` / `RestoreTask` hold `@SuppressLint("StaticFieldLeak")` reference to `Service` context | `service/BackupTask.java:52`, `service/RestoreTask.java:55` | Medium |
| AD-005 | `CalendarAccessorPre40` dead compatibility shim (API 14+ already required) | `calendar/CalendarAccessorPre40.java`, `calendar/CalendarAccessor.java` | Low |

---

### AD-001: Archived Square Otto Event Bus

- **Severity:** Critical
- **Confidence:** 1.00
- **Category:** Architecture Debt
- **Location:** `app/src/main/java/com/zegoggles/smssync/App.java:59`, and 10 additional files importing `com.squareup.otto`
- **Evidence:**
  Square officially archived Otto in 2016 in favour of RxJava/Kotlin. The library receives no security patches, no compatibility updates, and no API 30+ validation. It currently works only because it operates over reflection and Android has not yet broken the reflection paths it uses. The library version is `1.3.8` — the final release, from 2014.
  ```java
  // App.java
  private static final Bus bus = new Bus();
  // 11 files subscribe/produce
  @Subscribe public void backupStateChanged(BackupState state) { ... }
  @Produce public BackupState produceLastState() { ... }
  ```
- **Remediation:** Replace Otto with either (a) Kotlin `StateFlow` + `SharedFlow` for structured state propagation between service and UI, or (b) `LocalBroadcastManager` (itself deprecated but actively maintained) as a low-disruption interim step. The `@Produce` semantic (late subscription receives last value) maps cleanly to `StateFlow`.
- **Effort:** L — 11 files import Otto; the `@Produce` pattern requires a different contract in Kotlin coroutines; `BackupState` and `RestoreState` must be exposed as `StateFlow` from their respective services. Test infrastructure for Otto subscriptions must be updated.

---

### AD-002: `SmsJobService` Manually Instantiates `SmsBackupService`

- **Severity:** High
- **Confidence:** 1.00
- **Category:** Architecture Debt
- **Location:** `app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java:82–84`
- **Evidence:**
  ```java
  // Since API level 26, an app in background cannot start a background service,
  // so just instantiate service manually
  SmsBackupService service = new SmsBackupService();
  service.attachBaseContext(this);
  service.handleIntent(new Intent(jobParameters.getTag()).putExtras(extras));
  ```
  This bypasses the Android service lifecycle completely. `SmsBackupService.onCreate()` is never called, which means `AppLog` is not initialized, `App.register(service)` is not called, and any future `onCreate` logic will be silently skipped. The comment attributes this workaround to an API 26 background service restriction, but the correct solution for that restriction is `startForegroundService()` or `WorkManager`.
- **Remediation:** Replace `SmsJobService` (Firebase `JobService`) with a `WorkManager` `Worker` that calls `startForegroundService(Intent)` → `SmsBackupService`, or restructure `SmsBackupService` into a `WorkManager` `CoroutineWorker` that owns the entire lifecycle.
- **Effort:** L — this is the entry point for all scheduled backups; requires migrating from Firebase JobDispatcher to WorkManager end-to-end.

---

### AD-003: Process-Global `App.bus` Singleton Coupling

- **Severity:** High
- **Confidence:** 0.90
- **Category:** Architecture Debt
- **Location:** `app/src/main/java/com/zegoggles/smssync/App.java:59`; consumers in `activity/`, `service/`
- **Evidence:**
  ```java
  private static final Bus bus = new Bus();
  public static void register(Object listener) { bus.register(listener); }
  public static void post(Object event) { bus.post(event); }
  ```
  There is no dependency injection or scoping. Any object can post to or subscribe from the global bus, and there is no compile-time safety on event types (Otto uses reflection). The `@Produce` pattern means subscribers can receive stale state from previous lifecycle instances, creating subtle ordering bugs. The bus singleton also makes unit testing difficult because test classes must manage `App.register/unregister` manually.
- **Remediation:** Introduce a scoped `ViewModel` per screen or a `StateFlow`-backed repository that services write to and activities observe. This aligns with the MVVM pattern recommended by Jetpack.
- **Effort:** L — architectural refactor spanning all UI and service classes; should be paired with AD-001 remediation.

---

### AD-004: `@SuppressLint("StaticFieldLeak")` Context Reference in AsyncTask Subclasses

- **Severity:** Medium
- **Confidence:** 1.00
- **Category:** Architecture Debt
- **Location:** `service/BackupTask.java:51–52`, `service/RestoreTask.java:54–55`
- **Evidence:**
  ```java
  @SuppressLint("StaticFieldLeak")
  private final SmsBackupService service;
  ```
  `BackupTask` holds a strong reference to `SmsBackupService`. If the `AsyncTask` outlives the service (e.g. on process death and restart), this reference prevents the service from being garbage collected. The `@SuppressLint` annotation silences the lint warning without fixing the underlying memory model.
- **Remediation:** Resolved as part of CD-001 (AsyncTask replacement). A `WorkManager` worker or coroutine-based worker tied to the service scope does not exhibit this pattern.
- **Effort:** S (dependent on CD-001).

---

### AD-005: `CalendarAccessorPre40` Dead Compatibility Shim

- **Severity:** Low (Informational)
- **Confidence:** 0.85
- **Category:** Architecture Debt
- **Location:** `app/src/main/java/com/zegoggles/smssync/calendar/CalendarAccessorPre40.java`, `calendar/CalendarAccessor.java`
- **Evidence:**
  `CalendarAccessorPre40` targets Android 2.x–3.x (API < 14). The app's `minSdk` is 14 (Android 4.0). No device on a supported Android version will execute this code path. It adds dead code and additional test surface.
- **Remediation:** Remove `CalendarAccessorPre40` and the factory method in `CalendarAccessor.Get.instance()`. Inline `CalendarAccessorPost40` as the sole implementation.
- **Effort:** S — two files to delete; update `CalendarAccessor.java` factory method.

---

## Test Debt

**Coverage Assessment:**
- Estimated Coverage: ~40–50% (manual estimate based on 36 test files for 107 production files, with no instrumented coverage data available)
- All major packages have at least one test file, but several critical paths have limited or no direct coverage.

**Critical Untested Paths:**
- `activity/MainActivity.java` (452 LOC, complex lifecycle) — no dedicated test file
- `activity/Dialogs.java` (314 LOC, 16 dialog types) — no dedicated test file
- `activity/fragments/AdvancedSettings.java` (332 LOC) — no dedicated test file
- `auth/OAuth2Client.java` (253 LOC) — no dedicated test file (network calls are not mocked in unit tests)
- `service/SmsJobService.java` — has a test file but the manual `SmsBackupService` instantiation path (AD-002) is not exercised with a real lifecycle

| ID | Gap | Location | Risk |
|----|-----|----------|------|
| TD-001 | No unit tests for `MainActivity` | `activity/MainActivity.java` | High — permission handling, OAuth callback, state transitions all untested |
| TD-002 | No unit tests for `Dialogs` | `activity/Dialogs.java` | Medium — 16 dialog types, any broken dialog silently fails user flows |
| TD-003 | No unit tests for `OAuth2Client` | `auth/OAuth2Client.java` | High — token exchange, refresh, error handling, Contacts API parsing all untested |
| TD-004 | Test infrastructure uses `mockito-all 1.10.17` (2015, includes Hamcrest 1.1); incompatible with modern Mockito 2+ APIs | `app/build.gradle:68` | Medium — upgrade to Mockito-core 4+ / 5+ is blocked by `mockito-all` shadow JAR conflicts |

---

### TD-001: No Test Coverage for `MainActivity`

- **Severity:** High
- **Confidence:** 0.90
- **Category:** Test Debt
- **Location:** `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` (452 LOC)
- **Evidence:** No `MainActivityTest.java` exists in `app/src/test/java/com/zegoggles/smssync/activity/`. `MainActivity` handles 6 permission request codes, 3 `onActivityResult` paths, 3 Otto event handlers, and 2 auth flows.
- **Remediation:** Add Robolectric tests for each `onActivityResult` branch, permission grant/deny paths, and backup start/restore start flows.
- **Effort:** M

---

### TD-003: No Test Coverage for `OAuth2Client`

- **Severity:** High
- **Confidence:** 0.90
- **Category:** Test Debt
- **Location:** `app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java` (253 LOC)
- **Evidence:** No `OAuth2ClientTest.java` exists. The class performs the token exchange, refresh, and Contacts API username resolution. Failure modes (non-200 responses, SAX parse errors, IO errors) are all untested. `TokenRefresherTest.java` exists but mocks out `OAuth2Client` entirely.
- **Remediation:** Add a mock HTTP server (OkHttp `MockWebServer` or WireMock) to test token exchange, refresh, 401 handling, and Contacts API parsing against fixture responses.
- **Effort:** M

---

## Explicit Debt Markers (TODOs/FIXMEs)

| Marker | Location | Text | Age (if known) |
|--------|----------|------|----------------|
| TODO | `preferences/AuthPreferences.java:241` | `TODO: this should probably be handled in K9` | Unknown (pre-2016) |
| TODO | `compat/GooglePlayServices.java:32` | `TODO: check signatures` | Unknown |
| TODO | `service/SmsBackupServiceTest.java:212` | `// TODO` (empty test body) | Unknown |
| TODO | `mail/MessageGenerator.java:108` | `TODO: should probably be TextBasedSmsColumns.DATE_SENT` | Unknown |
| TODO | `activity/StatusPreference.java:132` | `// TODO implement` (onSaveInstanceState) | Unknown |
| TODO | `activity/StatusPreference.java:138` | `// TODO implement` (onRestoreInstanceState) | Unknown |

**Total Markers Found:** 6

**Notable:** The TODO in `SmsBackupServiceTest.java:212` indicates an **empty test body** — a known test gap filed as a debt marker. The TODO in `MessageGenerator.java:108` regarding `DATE_SENT` vs `DATE` may cause incorrect timestamps on backed-up SMS messages sent by the user.

---

## Complexity Hotspots

| File | Lines | Concern |
|------|-------|---------|
| `activity/MainActivity.java` | 452 | Orchestrates auth, permissions, backup/restore launch, 5 Otto handlers, 6 dialog types — god-activity pattern |
| `activity/fragments/AdvancedSettings.java` | 332 | Large preference fragment with calendar, contact group, IMAP validation inline |
| `activity/Dialogs.java` | 314 | 16 dialog types enumerated and instantiated via switch; violates open/closed principle |
| `activity/StatusPreference.java` | 312 | Custom Preference widget that subscribes to Otto events; two unimplemented methods |
| `service/RestoreTask.java` | 307 | Dual AsyncTask + Otto subscriber; cursor management, dedup, thread update inline |
| `service/SmsBackupService.java` | 280 | Service + Otto subscriber/producer + notification management + scheduler interaction |
| `mail/MessageGenerator.java` | 272 | All three message type conversions in one class; `messageFromMapMms` has 10 lines of commented-out threading experiments |

---

## Dependency Debt

| Package | Version | Issue | Risk | Action |
|---------|---------|-------|------|--------|
| `com.squareup:otto` | 1.3.8 | **Archived 2016**; no updates, no security patches, no Kotlin support | Critical | Replace with Kotlin Flow / LiveData (see AD-001) |
| `com.firebase:firebase-jobdispatcher` | 0.8.6 | **Deprecated and archived**; Firebase recommends `WorkManager`; the only remaining Maven host is `maven.scijava.org` (a scientific computing repo, not Google's), which is unreliable for production builds | Critical | Migrate to `WorkManager` (see AD-002) |
| `compileSdkVersion 29`, `targetSdkVersion 29` | API 29 | **Play Store requires targetSdk ≥ 34 for new updates** (as of Aug 2024 policy deadline); the app cannot be updated on the Play Store without upgrading. Also blocks `FLAG_IMMUTABLE` fix (CD-006) and foreground service type declaration required since API 34. | Critical (distribution-blocking) | Upgrade `compileSdk` and `targetSdk` to 35; address all API-level breakages |
| `com.android.billingclient:billing` | 2.1.0 | Version 2.x reached end-of-support; current version is 7.x. Play Store will begin enforcing Billing Library ≥ 6.x for apps using in-app purchases. | High | Upgrade to billing 7.x; API surface changed significantly since 2.x |
| `org.mockito:mockito-all` | 1.10.17 | **Shadow JAR from 2015**; includes Hamcrest 1.1 which conflicts with JUnit 4.13+; does not support Java 8 argument matchers; blocks upgrade to Mockito 4+ / 5+ | Medium | Replace with `mockito-core:5.x` + `mockito-android:5.x` |
| `org.robolectric:robolectric` | 4.3.1 | Released 2020; current version is 4.13+; missing support for API 30–35 shadows; `ShadowNetworkInfo.newInstance` usage in tests is a removed API in later versions | Medium | Upgrade to Robolectric 4.13+ |
| `com.google.truth:truth` | 0.39 | Released 2018; current version is 1.4.x; `assertThat` extension APIs for newer types unavailable | Low | Upgrade to Truth 1.4.x |
| `jcenter()` repository | — | **JCenter shut down in 2022**; `build.gradle` still lists `jcenter()` as a primary repository in 3 places. Artifact resolution silently falls through to Maven Central or fails, making builds non-deterministic. | High (build stability) | Remove all `jcenter()` declarations; replace with `mavenCentral()` + `google()` |

### Dependency Debt — Special Note on `firebase-jobdispatcher`

The root `build.gradle` contains an explicit comment acknowledging the problem:

```groovy
//  This is the only repo that seems to be hosting "com.firebase:firebase-jobdispatcher" in 2024
maven { url "https://maven.scijava.org/content/repositories/public/" }
```

This is the clearest existing signal in the codebase that `firebase-jobdispatcher` is a **build-stability blocker**. A SciJava repository going offline would break all CI builds immediately.

---

## Infrastructure Debt

| ID | Issue | Location | Severity |
|----|-------|----------|----------|
| IF-001 | `jcenter()` repository referenced in 3 places after shutdown | `build.gradle:6,22,25` | High |
| IF-002 | `-Werror` / `warningsAsErrors true` creates brittle build; any upstream SDK deprecation breaks CI | `app/build.gradle:41,74-77` | Medium |
| IF-003 | `minifyEnabled false` in release; no code shrinking applied | `app/build.gradle:33` | Medium |
| IF-004 | No CI/CD configuration present in repository | Repo root | Medium |

### IF-001: Deprecated `jcenter()` Repository

- **Severity:** High
- **Confidence:** 1.00
- **Category:** Infrastructure Debt
- **Location:** `build.gradle:6`, `build.gradle:22`, `build.gradle:25`
- **Evidence:**
  ```groovy
  // build.gradle:6
  repositories {
      google()
      jcenter()
      mavenCentral()
  }
  // build.gradle:22
  repositories {
      jcenter()
      ...
      maven { url "https://jcenter.bintray.com" }
  }
  ```
  JCenter was shut down in May 2022. The `jcenter.bintray.com` domain resolves to a read-only mirror that JFrog plans to decomission. Build resolution already fails for some packages that were JCenter-only. `jcenter()` is listed before `mavenCentral()`, meaning resolution attempts to an offline service first on every build.
- **Remediation:** Remove all three `jcenter()` and `jcenter.bintray.com` references. Verify all dependencies resolve from `mavenCentral()`, `google()`, `jitpack.io`, or the SciJava mirror.
- **Effort:** S — build.gradle change + validation build.

### IF-002: `-Werror` and `warningsAsErrors true` Build Brittleness

- **Severity:** Medium
- **Confidence:** 0.90
- **Category:** Infrastructure Debt
- **Location:** `app/build.gradle:41`, `app/build.gradle:74–77`
- **Evidence:**
  ```groovy
  lintOptions { warningsAsErrors true }
  tasks.withType(JavaCompile) {
      if (!it.name.toLowerCase().contains('test')) {
          options.compilerArgs = ['-Werror', '-Xlint:unchecked', '-Xlint:deprecation']
      }
  }
  ```
  Every Android SDK upgrade that adds or changes a deprecated API call will immediately break the production build. The existing codebase responds by adding `@SuppressWarnings("deprecation")` suppressions (29 currently). This approach scales poorly and hides the actual technical debt rather than tracking it.
- **Remediation:** During the modernization, temporarily relax `-Werror` to `-Xlint:deprecation` (warning only) to allow incremental API upgrades. Restore `-Werror` once deprecated APIs are eliminated. Consider adopting Android Lint baselines (`lint-baseline.xml`) as a structured incremental debt tracking mechanism.
- **Effort:** S — config change plus lint baseline generation.

---

## Debt Heatmap

Files/areas with the highest concentration of debt items:

| Location | Debt Items | Recommendation |
|----------|------------|----------------|
| `service/BackupTask.java` | CD-001, CD-004 (indirect), AD-004 | Priority rewrite target — AsyncTask + context leak |
| `service/RestoreTask.java` | CD-001, CD-004 (indirect), AD-004 | Priority rewrite target — AsyncTask + context leak |
| `service/BackupJobs.java` + `service/SmsJobService.java` | DD (firebase-jobdispatcher), AD-002 | Replace with WorkManager as single unit |
| `service/ServiceBase.java` | CD-002, CD-006 (indirect), deprecated APIs | Refactor connectivity checks; fix PendingIntent flags |
| `service/SmsBackupService.java` | AD-003, CD-007, deprecated API | Structural refactor (ViewModel/StateFlow) |
| `auth/OAuth2Client.java` | CD-005, DD (deprecated Contacts API), TD-003 | Replace with Google Auth library + People API |
| `App.java` | AD-001, AD-003 | Replace Otto bus with structured Flow |
| `build.gradle` (root) | IF-001, DD (jcenter), DD (firebase-jobdispatcher) | Immediate repo cleanup |
| `app/build.gradle` | DD (targetSdk 29, billing 2.1.0), IF-002, IF-003 | Immediate SDK upgrade required |

---

## Prioritized Remediation

### Quick Wins (Low effort, high impact)

1. **Remove `jcenter()` from `build.gradle`** (IF-001) — prevents silent build failures; 30-minute fix.
2. **Add `FLAG_IMMUTABLE` to `PendingIntent` calls** (CD-006) — prevents runtime crashes on Android 12+; two-line fix.
3. **Upgrade `compileSdk` / `targetSdk` to 35** — unblocks Play Store distribution and enables CD-006 fix; required before any other modernization can ship.
4. **Remove `CalendarAccessorPre40`** (AD-005) — clean up dead code; deletes two files.
5. **Enable `minifyEnabled true`** (CD-004) — reduces APK size; low risk with R8 defaults.
6. **Implement `StatusPreference` state persistence** (CD-008) — resolves two TODO markers; self-contained.

### Strategic Items (Higher effort, foundational)

1. **Replace Firebase JobDispatcher with WorkManager** — resolves DD (firebase-jobdispatcher), AD-002; unblocks reliable scheduled backup on modern Android. This is the **single highest-priority architectural fix** because the current Maven host for firebase-jobdispatcher is unreliable.
2. **Replace Otto with Kotlin Flow / StateFlow** — resolves AD-001, AD-003; enables structured state management and testability; must be coordinated with `AsyncTask` replacement.
3. **Replace `AsyncTask` with coroutines** — resolves CD-001, AD-004; dependent on Kotlin introduction.
4. **Upgrade `targetSdk` to 35 and address all API breakages** — resolves DD (targetSdk), CD-006, CD-002 (can then use modern `ConnectivityManager` API); distribution-blocking.
5. **Upgrade `billing` to 7.x** — resolves donation flow breakage risk; isolated module upgrade.
6. **Replace `OAuth2Client` hand-rolled HTTP with Google Auth Library** — resolves CD-005, DD (deprecated Contacts API).

### Defer/Monitor

1. Upgrade Robolectric to 4.13+ — valuable but non-blocking; schedule with next major test refactor.
2. Upgrade `mockito-all` to `mockito-core 5.x` — needed for modern Mockito APIs; schedule with Kotlin migration.
3. Upgrade `truth` to 1.4.x — low priority; no functional impact.
4. Full MVVM / ViewModel adoption (AD-003 long-term) — valuable architectural direction but not blocking; schedule as Phase 3 of modernization roadmap.

---

## Recommended Debt Budget

Recommend allocating **40% of sprint capacity** to debt reduction during the modernization initiative:

- **Immediate focus (Sprints 1–2):** targetSdk upgrade to 35 + jcenter removal + PendingIntent fix + firebase-jobdispatcher → WorkManager migration. These are distribution-blocking or build-stability items.
- **Ongoing (Sprints 3–6):** Otto → Flow migration paired with AsyncTask → coroutine replacement; billing library upgrade.
- **Stabilization (Sprints 7+):** Test infrastructure upgrade (Robolectric, Mockito); OAuth2 client replacement; R8 enablement.

---

## Narrative Summary

### Debt Profile

SMS Backup+ carries a well-maintained but temporally-stranded codebase. The production code quality is reasonable for a ~2009-era Android app: exception hierarchies are typed and localizable, the state machine is immutable and testable, and the k-9 IMAP integration is encapsulated behind clean interfaces. However, the dependency layer has not kept pace with the Android ecosystem and has accumulated five years of abandonment debt. Three of the project's six core dependencies — Otto, firebase-jobdispatcher, and the jcenter repository — are either archived, effectively deprecated, or hosted on an unreliable server. The `targetSdk 29` is the most critical single debt item: the Play Store currently rejects app updates that do not target API 33+, which means the app **cannot ship a new release to the Play Store** until the SDK is upgraded.

### Root Causes

The debt pattern suggests a project that was actively maintained through approximately 2021 and has since received only break-fix maintenance. The comment in `build.gradle` ("This is the only repo that seems to be hosting firebase-jobdispatcher in 2024") is diagnostic: a developer encountered the problem, worked around it, and moved on without resolving the root cause. The `-Werror` / `warningsAsErrors` policy was well-intentioned but resulted in a culture of `@SuppressWarnings` accumulation rather than API modernization. The absence of CI/CD configuration means there is no automated enforcement gate for regressions.

### Impact Analysis

The debt has two distinct impact tiers. The first tier — **targetSdk 29, firebase-jobdispatcher, Otto** — is distribution-blocking and scheduler-reliability-blocking. The app cannot be submitted to the Play Store for updates, and the scheduler has a single-point-of-failure dependency on a scientific computing Maven mirror. The second tier — `AsyncTask`, deprecated connectivity APIs, hand-rolled OAuth2, missing `FLAG_IMMUTABLE` — represents correctness risks on modern Android versions (API 31+). Users on Android 12+ may experience crashes in the `AlarmManagerDriver` path. The `AllTrustedSocketFactory` is a security concern for users who enable "trust all certificates."

### Implications for Modernization

The debt profile strongly favors an **incremental modernization** over a full rewrite. The core backup/restore logic (`BackupTask`, `RestoreTask`, `MessageConverter`, `BackupImapStore`) is well-tested and relatively clean; it does not need replacement, only re-scaffolding (replace `AsyncTask` with coroutines, replace Otto with `StateFlow`). The high-value modernization sequence is: (1) unblock distribution by upgrading `targetSdk` and removing dead dependencies, (2) replace the scheduling layer with `WorkManager`, (3) introduce Kotlin and coroutines for the async layer, (4) replace Otto with `StateFlow`. Steps 1–2 are prerequisite for any Play Store submission; steps 3–4 are prerequisite for long-term maintainability. The rewrite-vs-refactor decision for the UI layer (Jetpack Compose vs. XML preferences) can be deferred until the service layer is stabilized.
