---
artifact_type: review-security
story_id: "U-050"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-06-23"
blockers: 0
warnings: 0
---

# Security Review: U-050 — Decompose MainActivity, Inject Worker Engine Object Graph

## Review Summary

U-050 introduces `WorkerEngineFactory` (a Hilt `@Singleton` factory for per-run
`PersonLookup`/`MessageConverter`/`TokenRefresher`/`CalendarSyncer`), extracts four SMS-role
methods from `MainActivity` to `SmsDefaultRoleHelper`, and removes stale KDoc in
`WorkManagerScheduler`. From a security standpoint this story is neutral: the new factory
consolidates previously scattered `new` calls without widening the credential exposure surface,
the `SmsDefaultRoleHelper` extraction is a pure refactor preserving BUG-009/U-041 semantics,
and no new exported components or permission changes appear in either production code or the
manifest.

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

None.

### Low (informational)

**`WorkerEngineFactory` reads `AuthPreferences.userEmail` and `AuthPreferences.oAuth2ClientId`
at factory-method call time** — `createMessageConverter()` calls `authPreferences.userEmail`
and `createTokenRefresher()` calls `authPreferences.oAuth2ClientId`. These are read from
`EncryptedSharedPreferences` (via `SecretStore`) at the point `WorkerEngineFactory` methods are
invoked in `doWork()`. This is the same point at which they were previously read in the hand-new
construction paths inside the workers — no new read-window is introduced. No credential value is
stored in the `WorkerEngineFactory` instance itself (it holds only `Context`, `Preferences`, and
`AuthPreferences` references, not decoded values). No credential is logged.

---

## Detailed Security Verification

### 1. WorkerEngineFactory — credential access and logging

Read `WorkerEngineFactory.kt` in full (128 lines).

**Verified:**
- The factory holds three `@Inject`-supplied fields: `@ApplicationContext context`,
  `preferences: Preferences`, and `authPreferences: AuthPreferences`. None of these
  fields contain raw credential bytes — they are accessor objects backed by
  `EncryptedSharedPreferences` / `SecretStore`.
- `createPersonLookup()`: constructs `PersonLookup(context.contentResolver)`. No credential
  access.
- `createMessageConverter(personLookup, contactAccessor)`: reads `authPreferences.userEmail`
  to pass as the sender email to `MessageConverter`. This is the same read that previously
  occurred inline in `BackupWorker.fetchAndBackupItems()` and `RestoreWorker.doWork()`.
  Unchanged exposure surface; `userEmail` is the Gmail/IMAP account address, not an OAuth
  token or password.
- `createTokenRefresher()`: constructs `OAuth2Client(authPreferences.oAuth2ClientId)` and
  passes `authPreferences` to `TokenRefresher`. `oAuth2ClientId` is the app's OAuth client
  identifier (public, registered with Google) — not a user secret. This mirrors the
  pre-refactor path exactly.
- `createCalendarSyncerIfEnabled(personLookup)`: reads `preferences.isCallLogCalendarSyncEnabled`
  and `preferences.callLogCalendarId`. These are calendar configuration values, not credentials.
- **No `Log.*` statements** in `WorkerEngineFactory.kt`. Confirmed by reading the file: zero
  log calls. No credential is observable in logcat from this class.
- The factory is `@Singleton` — the `authPreferences` reference it holds will always reflect
  the current `AuthPreferences` state at the time a factory method is called (it is a reference
  to a live accessor, not a snapshot). This is correct security behavior: each backup/restore
  run reads the current token/client-id, so a re-authentication between runs is automatically
  picked up.

### 2. BackupWorker and RestoreWorker — engineFactory injection, no new credential surface

Read `BackupWorker.kt` lines 234–244 and `RestoreWorker.kt` lines 185–190 (the call sites
of the factory methods).

**Verified:**
- Five hand-`new` calls replaced by `engineFactory.create*()` calls. The security-relevant
  observations are:
  - `contactAccessor` is now an injected `@Singleton` field rather than `new ContactAccessor()`
    in-method. `ContactAccessor` has a zero-arg constructor with no credential dependencies.
  - `TokenRefresher` and `MessageConverter` creation is now in `WorkerEngineFactory`
    (see §1 above). The point of credential-read is identical to before; only the call
    site location changed.
- `TestableBackupWorkerFactory` and `TestableRestoreWorkerFactory` (test-only `WorkerFactory`
  subclasses) now supply `ContactAccessor()` and `WorkerEngineFactory(appContext, prefs,
  authPrefs)` directly. These factories are used only in Robolectric unit tests and never
  registered in the production `App.getWorkManagerConfiguration()` path — which exclusively
  uses the Hilt-injected `HiltWorkerFactory`.

### 3. SmsDefaultRoleHelper — SMS role negotiation security

Read `SmsDefaultRoleHelper.kt` in full (159 lines).

**Verified:**
- This is a pure structural extraction of four methods from `MainActivity.java`. The logic for
  SMS-default-role negotiation (`RoleManager.createRequestRoleIntent(ROLE_SMS)` on Q+,
  `ACTION_CHANGE_DEFAULT` intent on pre-Q) is **byte-for-byte identical** to the removed
  `MainActivity` methods. BUG-009/U-041 Q+ unconditional path preserved at lines 77–83.
- `SmsDefaultRoleHelper` is a Kotlin `object` (singleton with no mutable state), not a
  component registered with the system. It is not exported and not accessible to external apps.
- The `DialogDelegate` interface solely allows `MainActivity` to receive callbacks for dialog
  display (`showSmsDefaultPackageChangeDialog()`) and restore start (`startViewModelRestore()`).
  No credential or sensitive data flows through this interface.
- `requestDefaultSmsPackageChange(activity)` calls `SmsReceiver.enable(activity)` followed by
  `roleManager.createRequestRoleIntent(ROLE_SMS)` and `activity.startActivityForResult()`.
  These are OS-level role negotiation calls — same semantics as before the extraction.
- `restoreDefaultSmsProvider(activity, smsPackage)` on Q+ calls `SmsReceiver.disable(activity)`;
  on pre-Q sends `ACTION_CHANGE_DEFAULT` with `EXTRA_PACKAGE_NAME = smsPackage`. `smsPackage`
  is a package name string obtained from `Preferences.getSmsDefaultPackage()`, not a credential.

### 4. No new exported components

Confirmed from the manifest diff (only two `<service>` entries removed; no additions) and from
reading `SmsDefaultRoleHelper.kt` (Kotlin `object`, not an Android component). The `MainViewModel`
is also not a manifest-registered component. No new exported surface.

### 5. TLS and trust — no change

`WorkManagerScheduler.kt` changes are documentation-only (KDoc cleanup of stale "U-020 pending"
and "second adapter alongside LegacyScheduler" text). No executable code was modified in that
file. No TLS-related code touched.

---

## Security Checklist

- [x] No hardcoded credentials or secrets — `WorkerEngineFactory` reads credentials from
      `AuthPreferences` (backed by `EncryptedSharedPreferences`) at call time; no credential
      is captured into a field or logged.
- [x] Input validation on all user inputs — `SmsDefaultRoleHelper` processes system-provided
      package names and activity result codes. No new user-provided string is trusted beyond
      the pre-existing `onActivityResult` path.
- [x] Output encoding prevents injection attacks — no output encoding surface changed.
- [x] Authentication tokens handled securely — `TokenRefresher` creation path now goes through
      `WorkerEngineFactory.createTokenRefresher()` instead of inline `new TokenRefresher(...)`.
      Semantics identical; token never logged; `AuthPreferences` SecretStore remains
      `EncryptedSharedPreferences`.
- [x] Authorization checks on all protected resources — SMS role negotiation (Q+ RoleManager,
      pre-Q ACTION_CHANGE_DEFAULT) preserved verbatim in `SmsDefaultRoleHelper`.
- [x] Sensitive data encrypted at rest and in transit — no change to EncryptedSharedPreferences
      or TLS stack.
- [x] Error messages don't leak internal details — `WorkerEngineFactory` has no log statements.
      `SmsDefaultRoleHelper` logs only package names and role state (not credentials).
- [x] Dependencies have no known critical vulnerabilities — no dependency changes in U-050.
