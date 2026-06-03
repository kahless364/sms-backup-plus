# Solution Inventory

**Generated:** 2026-05-29
**Engagement:** 20260529-modernization

---

## Solution Information

- **Name:** SMS Backup+
- **Description:** Android application that backs up and restores SMS, MMS, and call logs to/from a Gmail/IMAP mailbox, with optional Google Calendar integration for call-log entries. Runs entirely on-device; no project-owned backend.
- **Total Projects:** 2 (one runtime module, one metadata/store-listing module)
- **Primary Technologies:** Java (Android), Gradle, IMAP (k-9 mail library), Square Otto event bus, Firebase JobDispatcher, AndroidX Preferences, Google Play Billing

---

## Project Registry

| ID  | Project Name       | Type              | Layer              | Size | Priority | Status      |
|-----|--------------------|-------------------|--------------------|------|----------|-------------|
| P01 | `app`              | Android App       | Frontend + Backend | L    | High     | Not Started |
| P02 | `metadata`         | Store Listing     | Peripheral/Docs    | S    | Low      | Not Started |

---

## Project Details

### P01: app

| Attribute          | Value |
|--------------------|-------|
| **Location**       | `app/` |
| **Type**           | Android Application (native) |
| **Layer**          | Frontend (settings UI, auth UI) + Backend (backup/restore engine, scheduling) |
| **Primary Language** | Java (source/target compatibility Java 8) |
| **Framework**      | Android SDK (compileSdk 29, minSdk 14, targetSdk 29) |
| **Build File**     | `app/build.gradle` |
| **Application ID** | `com.zegoggles.smssync` |
| **Version**        | 1.6.0-BETA2 (versionCode 1602) |
| **ABIs**           | `armeabi-v7a`, `arm64-v8a` |
| **Size Estimate**  | L (~100 Java source files, ~36 unit test files) |
| **Criticality**    | Core |
| **Priority**       | High |
| **Dependencies**   | None (this is the only runtime module) |
| **Dependents**     | None |
| **Notes**          | Lint warnings treated as errors (`warningsAsErrors true`); compiler built with `-Werror -Xlint:unchecked -Xlint:deprecation` for non-test code; release signing via `keystore.properties` (not committed). |

#### P01 Package Map

Source root: `app/src/main/java/com/zegoggles/smssync/`

| Package | Key Files | Responsibility |
|---------|-----------|---------------|
| *(root)* | `App.java`, `Consts.java`, `MmsConsts.java` | Application entry point; global event bus (`Bus`) singleton; notification channel setup; broadcast receiver enable/disable |
| `activity` | `MainActivity.java`, `ThemeActivity.java`, `StatusPreference.java`, `Dialogs.java`, `AppPermission.java`, `PreferenceTitles.java` | Main settings screen (PreferenceFragmentCompat), permission requests, dialog management, theme switching |
| `activity/auth` | `OAuth2WebAuthActivity.java`, `AccountManagerAuthActivity.java`, `RedirectReceiverActivity.java` | OAuth2 web flow, AccountManager-based auth, OAuth2 redirect URI handling |
| `activity/donation` | `DonationActivity.java`, `DonationListFragment.java`, `Sku.java` | Google Play Billing in-app donation flow |
| `activity/events` | `AccountAddedEvent.java`, `AccountConnectionChangedEvent.java`, `AccountRemovedEvent.java`, `AutoBackupSettingsChangedEvent.java`, `FallbackAuthEvent.java`, `MissingPermissionsEvent.java`, `PerformAction.java`, `SettingsResetEvent.java`, `ThemeChangedEvent.java` | Otto event bus POJO event types for UI/service communication |
| `activity/fragments` | `MainSettings.java`, `AutoBackupSettings.java`, `AdvancedSettings.java`, `SMSBackupPreferenceFragment.java` | Preference screen fragments (AndroidX `PreferenceFragmentCompat`) |
| `auth` | `OAuth2Client.java`, `OAuth2Token.java`, `TokenRefresher.java`, `TokenRefreshException.java` | OAuth2 authorization code + token exchange/refresh (raw `HttpsURLConnection`; no HTTP client library); contacts API call to resolve Google account email |
| `calendar` | `CalendarAccessor.java`, `CalendarAccessorPost40.java`, `CalendarAccessorPre40.java` | Device Calendar provider read/write; API-level compatibility split (≥ API 14 vs. older) |
| `compat` | `SmsReceiver.java`, `MmsReceiver.java`, `ComposeSmsActivity.java`, `HeadlessSmsSendService.java`, `GooglePlayServices.java`, `package-info.java` | Default SMS app role stubs (required by Android KitKat+ to temporarily become default SMS app during restore); Google Play Services availability check |
| `contacts` | `ContactAccessor.java`, `ContactGroup.java`, `ContactGroupIds.java`, `Group.java` | Android Contacts provider read; contact-group filtering for selective backup |
| `mail` | `BackupImapStore.java`, `BackupStoreConfig.java`, `MessageConverter.java`, `MessageGenerator.java`, `MessageConverter.java`, `MmsSupport.java`, `CallFormatter.java`, `Headers.java`, `HeaderGenerator.java`, `Attachment.java`, `ConversionResult.java`, `DataType.java`, `PersonLookup.java`, `PersonRecord.java`, `AllTrustedSocketFactory.java` | SMS/MMS/call-log ↔ RFC-822 email conversion; IMAP store (wraps k-9 `ImapStore`); IMAP folder-per-data-type; custom IMAP search (UID SEARCH with X-header filter) |
| `preferences` | `Preferences.java`, `AuthPreferences.java`, `DataTypePreferences.java`, `AddressStyle.java`, `AuthMode.java`, `CallLogTypes.java`, `Defaults.java`, `MarkAsReadTypes.java` | `SharedPreferences` facade; all user-configurable settings; auth credential storage |
| `receiver` | `SmsBroadcastReceiver.java`, `BootReceiver.java`, `PackageReplacedReceiver.java`, `BackupBroadcastReceiver.java` | System broadcast receivers: incoming SMS trigger, boot-completed re-schedule, package-replaced re-schedule, third-party `com.zegoggles.smssync.BACKUP` broadcast |
| `service` | `SmsBackupService.java`, `SmsRestoreService.java`, `SmsJobService.java`, `ServiceBase.java`, `BackupTask.java`, `RestoreTask.java`, `BackupJobs.java`, `AlarmManagerDriver.java`, `BackupConfig.java`, `RestoreConfig.java`, `BackupCursors.java`, `BackupItemsFetcher.java`, `BackupQueryBuilder.java`, `BulkFetcher.java`, `CalendarSyncer.java`, `BackupType.java`, `CancelEvent.java` | Backup/restore engine; Firebase JobDispatcher scheduling with AlarmManager fallback driver; `AsyncTask`-based backup/restore workers; PowerManager/WifiManager wake locks; state machine |
| `service/exception` | `BackupDisabledException.java`, `ConnectivityException.java`, `LocalizableException.java`, `MissingPermissionException.java`, `NoConnectionException.java`, `RequiresLoginException.java`, `RequiresWifiException.java`, `SmsProviderNotWritableException.java` | Typed, localizable exception hierarchy for backup/restore failure modes |
| `service/state` | `State.java`, `BackupState.java`, `RestoreState.java`, `SmsSyncState.java` | Immutable value-object state machine: `SmsSyncState` enum → `State` → `BackupState`/`RestoreState`; posted on Otto bus for UI updates |
| `tasks` | `OAuth2CallbackTask.java` | `AsyncTask` wrapper for OAuth2 token exchange callback |
| `utils` | `AppLog.java`, `AppLog.java`, `BundleBuilder.java`, `Drawables.java`, `ListPreferenceHelper.java`, `Sanitizer.java`, `ThreadHelper.java` | File-based app log, Intent/Bundle builder helper, SMS thread ID helper |

#### P01 Key Third-Party Dependencies

| Dependency | Version | Role |
|------------|---------|------|
| `com.squareup:otto` | 1.3.8 | Event bus — decouples service state changes from UI updates |
| `com.github.jberkel.k-9:k9mail-library` | eaf689025e (git SHA) | IMAP client (`ImapStore`, `ImapFolder`, `ImapMessage`, `MessagingException`) |
| `com.android.billingclient:billing` | 2.1.0 | Google Play Billing for in-app donations |
| `com.firebase:firebase-jobdispatcher` | 0.8.6 | Background job scheduling (GCM-backed primary, `AlarmManagerDriver` fallback) |
| `androidx.annotation:annotation` | 1.1.0 | `@NonNull`, `@Nullable`, `@RequiresApi` annotations |
| `androidx.preference:preference` | 1.1.0 | `PreferenceFragmentCompat`, `PreferenceScreen` (AndroidX settings UI) |
| `androidx.core:core-role` | 1.0.0-beta01 | `RoleManagerCompat.ROLE_SMS` — default SMS app role (Android Q+) |

#### P01 Test Stack

| Library | Version | Role |
|---------|---------|------|
| JUnit 4 | 4.12 | Test runner and assertions |
| Robolectric | 4.3.1 | Android framework stubs for JVM unit tests |
| Google Truth | 0.39 | Fluent assertion library |
| Mockito | 1.10.17 (mockito-all) | Mocking |
| `com.google.auto.service:auto-service` | 1.0-rc4 | Robolectric service annotation processor |

Test root: `app/src/test/java/com/zegoggles/smssync/`  
36 test files present, covering all major packages.

#### P01 Android Manifest Components

| Component Type | Name | Purpose |
|----------------|------|---------|
| Activity | `.activity.MainActivity` | LAUNCHER — main settings + backup/restore trigger |
| Activity | `.activity.auth.OAuth2WebAuthActivity` | OAuth2 browser-based auth |
| Activity | `.activity.auth.RedirectReceiverActivity` | OAuth2 redirect URI handler (`com.zegoggles.smssync:/oauth2redirect`) |
| Activity | `.activity.auth.AccountManagerAuthActivity` | AccountManager-based token retrieval |
| Activity | `.activity.donation.DonationActivity` | In-app donation |
| Activity | `.compat.ComposeSmsActivity` | Default SMS app stub (compose SMS/MMS) |
| Service | `.service.SmsBackupService` | Foreground service driving backup |
| Service | `.service.SmsRestoreService` | Foreground service driving restore |
| Service | `.service.SmsJobService` | Firebase `JobService` entry point for scheduled jobs |
| Service | `.compat.HeadlessSmsSendService` | Default SMS app stub (respond-via-message) |
| Receiver | `.receiver.SmsBroadcastReceiver` | `SMS_RECEIVED` → schedule incoming backup |
| Receiver | `.receiver.BootReceiver` | `BOOT_COMPLETED` → reschedule backup jobs |
| Receiver | `.receiver.PackageReplacedReceiver` | `MY_PACKAGE_REPLACED` → reschedule backup jobs |
| Receiver | `.receiver.BackupBroadcastReceiver` | `com.zegoggles.smssync.BACKUP` — third-party integration |
| Receiver | `.compat.SmsReceiver` | Default SMS app stub (`SMS_DELIVER`) |
| Receiver | `.compat.MmsReceiver` | Default SMS app stub (`WAP_PUSH_DELIVER`) |

#### P01 Architectural Patterns

1. **Event Bus (Square Otto)** — `App.bus` is a process-global singleton. Services post `BackupState`/`RestoreState` objects; `MainActivity` and `SmsJobService` subscribe. `@Produce` annotations allow late subscribers to receive the last-known state.
2. **AsyncTask-based workers** — `BackupTask extends AsyncTask<BackupConfig, BackupState, BackupState>` and `RestoreTask extends AsyncTask<RestoreConfig, RestoreState, RestoreState>`. Both are created inside foreground services and post progress via the event bus.
3. **State machine (immutable value objects)** — `SmsSyncState` enum drives `State.transition(SmsSyncState, Exception)` to produce new immutable state instances; mutable state is never shared across threads.
4. **Firebase JobDispatcher with AlarmManager fallback** — `BackupJobs` selects `GooglePlayDriver` or `AlarmManagerDriver` at construction time based on `Preferences.isUseOldScheduler()`. Both implement the same `Driver` interface, so the scheduling abstraction is transparent to callers.
5. **Content provider cursors as data source** — `BackupItemsFetcher` + `BackupQueryBuilder` query `Telephony.Sms`, `Telephony.Mms`, and `CallLog.Calls` via `ContentResolver`. Cursors are wrapped in `BackupCursors` for unified iteration.
6. **k-9 mail library for IMAP** — `BackupImapStore extends ImapStore`; `BackupFolder extends ImapFolder`. IMAP folders are named per `DataType` and lazily opened; custom UID SEARCH queries use a proprietary `X-DataType:` header.
7. **SharedPreferences facade** — `Preferences` and `AuthPreferences` provide typed accessors over `PreferenceManager.getDefaultSharedPreferences()`. All keys are declared as `enum Keys` entries.
8. **Compatibility shims** — `AlarmManagerDriver` (scheduling), `CalendarAccessorPost40` vs. `CalendarAccessorPre40` (calendar API), and the `compat` package (default-SMS-app role) isolate API-level differences.
9. **Foreground service + WakeLock/WifiLock** — `ServiceBase.acquireLocks()` / `releaseLocks()` holds a `PARTIAL_WAKE_LOCK` (10 min) and an optional `WIFI_MODE_FULL_HIGH_PERF` lock during backup I/O.

#### P01 Data Types

Three data types are supported, each controlled by a `DataType` enum entry carrying its own folder-name preference key, backup-enabled flag, restore-enabled flag, max-synced-date preference key, and required Android permissions:

| DataType | Default IMAP folder | Backup default | Restore default |
|----------|---------------------|----------------|-----------------|
| `SMS` | `SMS` | enabled | enabled |
| `MMS` | `SMS` (shared) | enabled | disabled |
| `CALLLOG` | `Call log` | disabled | enabled |

#### P01 External API Touchpoints

| External Service | Protocol/Library | Used For |
|-----------------|-----------------|---------|
| IMAP server (default: `imap.gmail.com:993`) | k-9 `ImapStore` (IMAPS) | Backup storage and restore retrieval |
| Google OAuth2 (`accounts.google.com`, `googleapis.com/oauth2/v3/token`) | Raw `HttpsURLConnection` | Auth code exchange, token refresh |
| Google Contacts (`google.com/m8/feeds/`) | `HttpsURLConnection` + SAX XML | Username resolution after OAuth2 |
| Android Calendar provider (`content://com.android.calendar/`) | `ContentResolver` | Call-log calendar sync |
| Android SMS/MMS provider (`content://sms/`, `content://mms/`) | `ContentResolver` | Data source for backup; write target for restore |
| Android Call Log provider (`content://call_log/calls`) | `ContentResolver` | Data source for backup; write target for restore |
| Google Play Billing | `BillingClient` | In-app donations |

---

### P02: metadata

| Attribute          | Value |
|--------------------|-------|
| **Location**       | `metadata/` |
| **Type**           | Store listing / CI metadata |
| **Layer**          | Peripheral — distribution support only |
| **Primary Language** | N/A (images, text, Gradle stub) |
| **Framework**      | Fastlane-compatible layout (Play Store + F-Droid) |
| **Build File**     | `metadata/build.gradle` |
| **Size Estimate**  | S |
| **Criticality**    | Peripheral |
| **Priority**       | Low |
| **Dependencies**   | None |
| **Dependents**     | None |
| **Notes**          | Contains Play Store screenshots (localized: `da`, `de`, `en`, …), app icon assets (SVG, PNG), Sketch source, and an F-Droid metadata text file. Not part of the runtime build. |

---

## Dependency Map

```
┌──────────────────────────────────────────────────────────────────┐
│                     SINGLE ANDROID APPLICATION                    │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │  P01: app  (com.zegoggles.smssync)                          │  │
│  │                                                               │  │
│  │  UI Layer                                                     │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │  │
│  │  │ MainActivity │  │ Auth          │  │ Donation          │   │  │
│  │  │ (settings)   │  │ Activities   │  │ Activity          │   │  │
│  │  └──────┬───────┘  └──────┬───────┘  └──────────────────┘   │  │
│  │         │   Otto Bus       │                                   │  │
│  │  ┌──────▼───────────────────────────────────────────────┐    │  │
│  │  │  App.bus  (Square Otto singleton)                     │    │  │
│  │  └──────┬──────────────────────────────────────┬─────────┘    │  │
│  │         │ BackupState / RestoreState events     │              │  │
│  │  Service Layer                                  │              │  │
│  │  ┌──────▼──────────┐  ┌─────────────────────┐  │              │  │
│  │  │ SmsBackupService│  │ SmsRestoreService    │  │              │  │
│  │  │  (ServiceBase)  │  │  (ServiceBase)       │  │              │  │
│  │  └──────┬──────────┘  └──────────┬───────────┘  │              │  │
│  │         │ AsyncTask               │               │              │  │
│  │  ┌──────▼──────────┐  ┌──────────▼───────────┐  │              │  │
│  │  │   BackupTask    │  │    RestoreTask         │  │              │  │
│  │  └──────┬──────────┘  └──────────┬────────────┘  │              │  │
│  │  Scheduling Layer                 │               │              │  │
│  │  ┌───────────────────────────┐    │               │              │  │
│  │  │ BackupJobs                │    │               │              │  │
│  │  │ (FirebaseJobDispatcher)   │    │               │              │  │
│  │  │   ├─ GooglePlayDriver     │    │               │              │  │
│  │  │   └─ AlarmManagerDriver   │    │               │              │  │
│  │  └───────────────────────────┘    │               │              │  │
│  │  ┌──────────────────────────────────────────────┐ │              │  │
│  │  │  Data / Mail Layer                           │ │              │  │
│  │  │  BackupImapStore (k-9 ImapStore)             │ │              │  │
│  │  │  MessageConverter / MessageGenerator         │ │              │  │
│  │  │  BackupItemsFetcher / ContentResolver cursors│ │              │  │
│  │  │  OAuth2Client / TokenRefresher               │ │              │  │
│  │  └──────────────────────────────────────────────┘ │              │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  P02: metadata (store listing assets — not in runtime build)       │
└──────────────────────────────────────────────────────────────────┘

External dependencies (network):
  app ──IMAPS──▶  imap.gmail.com (or custom IMAP server)
  app ──HTTPS──▶  accounts.google.com  (OAuth2 auth)
  app ──HTTPS──▶  googleapis.com/oauth2/v3/token  (token exchange/refresh)
  app ──HTTPS──▶  google.com/m8/feeds/  (Contacts API — username resolution)
  app ──IPC────▶  Android system ContentProviders
                    content://sms/, content://mms/  (SMS/MMS)
                    content://call_log/calls  (Call Log)
                    content://com.android.calendar/  (Calendar)
  app ──IPC────▶  Google Play Billing  (in-app donations)
```

---

## Analysis Phases

### Phase 1: Foundation (Analyze First)

| Project | Rationale |
|---------|-----------|
| P01 `app` | Single core project; contains all runtime logic — must be understood before any other analysis |

### Phase 2: Peripheral

| Project | Rationale | Depends On |
|---------|-----------|------------|
| P02 `metadata` | Store listing only; no code; low analytical value for a modernization engagement | Phase 1 context helpful but not required |

---

## Scope Estimate

| Metric | Value |
|--------|-------|
| Total Projects | 2 |
| Core Projects | 1 (`app`) |
| Java Source Files | ~100 (production); ~36 (test) |
| Test Coverage | Robolectric unit tests present for all major packages |
| Localisations | 22 language `values-*` directories |
| Estimated Analysis Effort | 3–5 days (full SDLC assessment of P01) |
| Recommended Approach | Focused — analyse P01 in full; treat P02 as out of scope |

---

## Analysis Configuration

For this engagement, analyse:
- [x] Core projects only (P01 `app` — focused)
- [ ] All projects (comprehensive)

Analysis depth:
- [x] Full analysis (all prompts)

---

## Next Steps

1. Confirm P01 (`app`) as the sole subject of full analysis.
2. Proceed to code-quality and architecture analysis prompts for P01.
3. P02 (`metadata`) requires no further analysis for a modernization engagement.
