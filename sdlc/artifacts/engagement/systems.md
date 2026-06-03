# System Inventory

## Overview

SMS Backup+ is a single-module Android application (`com.zegoggles.smssync`) that
backs up and restores SMS, MMS, and call logs to/from a Gmail/IMAP mailbox, with
optional Google Calendar integration for call logs. There is no server-side
component owned by the project — it integrates with Google/IMAP services directly
from the device.

## Modules

| Module | Path | Purpose |
|--------|------|---------|
| `app` | `app/` | The Android application (all runtime code, resources, tests) |
| `metadata` | `metadata/` | Fastlane/Play Store listing metadata and assets |

## Application Architecture (package map)

Source root: `app/src/main/java/com/zegoggles/smssync/`

| Package | Responsibility |
|---------|---------------|
| `activity` | UI — settings screens, authorization flow, status display |
| `auth` | OAuth2 client, token storage, and token refresh (`OAuth2Client`, `OAuth2Token`, `TokenRefresher`) |
| `mail` | IMAP backup store, message conversion (SMS/MMS/calllog → email), attachment handling (`BackupImapStore`, `MessageConverter`, `MessageGenerator`, `MmsSupport`, `DataType`) |
| `calendar` | Google Calendar integration for call-log entries |
| `contacts` | Contact lookup / phone-number → name resolution (`PersonLookup`, `PersonRecord`) |
| `preferences` | App settings, stored auth preferences, backup configuration |
| `receiver` | Broadcast receivers (incoming SMS, connectivity, boot) that trigger backups |
| `service` | Backup/restore engine — services, job scheduling, cursors, query building (`SmsBackupService`, `SmsRestoreService`, `BackupTask`, `RestoreTask`, `BackupJobs`, `CalendarSyncer`) |
| `tasks` | Async task helpers |
| `compat` | API-level compatibility shims |
| `utils` | Shared utilities |

## Architecture Layers

1. **Triggers** — `receiver/` (SMS arrival, connectivity, boot) and scheduled jobs
   (`service/BackupJobs`, `SmsJobService`, `AlarmManagerDriver`) decide when to run.
2. **Orchestration** — `service/` services drive `BackupTask` / `RestoreTask` using
   `BackupConfig` / `RestoreConfig` and a state machine (`service/state`).
3. **Data access** — `service/BackupItemsFetcher`, `BackupCursors`,
   `BackupQueryBuilder` read SMS/MMS/call-log content providers.
4. **Conversion** — `mail/MessageConverter` + `MessageGenerator` turn device records
   into RFC-822 email messages (and back for restore).
5. **Transport** — `mail/BackupImapStore` (built on the k-9 mail library) talks IMAP;
   `auth/` provides OAuth2 tokens where applicable.
6. **Eventing** — Otto event bus communicates progress/state to the UI.

## Key Technologies

- **Event bus:** Square Otto
- **Email/IMAP:** k-9 mail library (`com.github.jberkel.k-9:k9mail-library`)
- **Background scheduling:** Firebase JobDispatcher + AlarmManager fallback
- **Billing:** Google Play Billing (in-app donations)
- **Testing:** JUnit 4, Robolectric, Truth, Mockito

## Dev Commands

| Task | Command |
|------|---------|
| Build debug APK | `./gradlew assembleDebug` |
| Install to device | `adb install app/build/outputs/apk/app-debug.apk` |
| Unit tests | `./gradlew test` |
| Lint (warnings are errors) | `./gradlew lint` |

See [code-location.md](code-location.md) for full stack, SDK levels, and build constraints.
