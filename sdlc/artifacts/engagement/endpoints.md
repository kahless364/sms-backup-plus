# Endpoints & Connection Information

SMS Backup+ runs entirely on-device. It has no project-owned backend; all external
endpoints belong to Google or to the user-configured IMAP provider. No secrets are
stored in this repo — OAuth client IDs and signing keys are supplied at build/run time.

## Mail / Backup Transport (IMAP)

| Field | Value |
|-------|-------|
| Default provider | Gmail (`imap.gmail.com`, IMAPS / port 993) |
| Custom servers | Any IMAP server (user-configurable in app settings) |
| Auth (current) | IMAP login with an account "app password" (see note below) |
| Library | k-9 mail library, via `mail/BackupImapStore.java` |
| Mailbox scope | `https://mail.google.com/` (full mailbox access) |

> **Policy note:** Since Google's June 2019 API policy change (sensitivity scopes),
> XOAuth2 can no longer be used to write messages into Gmail via IMAP. The app now
> requires a generated Gmail **app password** for the backup/restore IMAP connection.

## Google OAuth2 (used for contacts + calendar)

Defined in `app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java`:

| Endpoint | URL |
|----------|-----|
| Authorization | `https://accounts.google.com/o/oauth2/auth` |
| Token (exchange + refresh) | `https://www.googleapis.com/oauth2/v3/token` |
| Redirect URI | `com.zegoggles.smssync:/oauth2redirect` (custom scheme / native app flow) |

OAuth2 reference docs the implementation follows:
- https://developers.google.com/identity/protocols/OAuth2InstalledApp
- https://developers.google.com/identity/protocols/OAuth2UserAgent

## Google Contacts API

| Purpose | Endpoint |
|---------|----------|
| Contact name lookup (phone → name) | `https://www.google.com/m8/feeds/` |
| Thin contacts feed | `https://www.google.com/m8/feeds/contacts/default/thin` |

Used by `contacts/PersonLookup` to match phone numbers to contact names.

## Google Calendar

Call-log entries can be written into a user-selected Google Calendar via
`service/CalendarSyncer.java` (uses the device calendar provider / Google account).

## Credentials & Configuration

| Item | Where it comes from |
|------|--------------------|
| OAuth client ID | Injected at construction (`OAuth2Client(String clientId)`) — not hardcoded in repo |
| Gmail app password | Entered by the end user at runtime |
| IMAP host/port/user | User-configurable in app preferences (`preferences/`) |
| Release signing key | `keystore.properties` at repo root (not committed) |

## Google Play

| Purpose | Service |
|---------|---------|
| In-app donations | Google Play Billing (`com.android.billingclient:billing`) |
| Distribution | Google Play Store + F-Droid |
