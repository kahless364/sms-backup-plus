---
artifact_type: build-review-security
story_id: consolidated-2026-06
sprint: sprint-001..005
reviewer: Security Reviewer
date: 2026-06-04
verdict: PASS-WITH-FINDINGS
---

## Security Review: Consolidated Sprint-001..005 Modernization

### Summary

The modernization sprint series has eliminated the primary trust-all TLS bypass (CWE-295) and replaced plaintext credential storage with EncryptedSharedPreferences (CWE-312). No hardcoded private secrets were found. Two medium-severity findings and one low-severity finding require attention; no Critical or High severity blockers were identified.

### Verdict: PASS-WITH-FINDINGS

No Critical or High severity findings. The story series may advance. The medium-severity findings should be resolved in the next iteration.

---

### Findings

#### Critical (immediate fix required)

None.

#### High (fix before release)

None.

#### Medium (fix in next iteration)

**M-001 — Plaintext OAuth token placed in Activity Intent extra (legacy AccountManager path)**
- File: `app/src/main/java/com/zegoggles/smssync/activity/auth/AccountManagerAuthActivity.java:148-153`
- Severity: Medium
- OWASP: A07:2021 Identification and Authentication Failures / CWE-312
- Details: `useToken(Account account, String token)` at line 148 logs `"obtained token for … from AccountManager"` at `Log.d` level (no credential value — OK), but then puts the raw OAuth2 access token into an Intent extra (`EXTRA_TOKEN`) at line 151, which is delivered to `MainActivity` via `setResult(RESULT_OK, result)`. Android Intent extras are inspectable by any privileged process with READ_LOGS or a debuggable build and can be captured in `ActivityManager` history on some OEM firmwares. The token string is never masked here. This path is legacy (AccountManager-based OAuth) but is still reachable.
- Remediation: (1) Do not carry the raw token through Intent extras. Persist it immediately in `AuthPreferences.setOauth2Token()` inside `AccountManagerAuthActivity.useToken()` before calling `setResult`, and pass only the account name to `MainActivity`. (2) Alternatively, zero-out the token string from the Intent before `setResult` if the current call-site coupling cannot be changed yet. The `OAuth2Client`/webflow path does not exhibit this issue.

**M-002 — `PlaintextSharedPrefsSecretStore` reachable in production build when Keystore fails**
- File: `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java:102-135`, `PlaintextSharedPrefsSecretStore.java`
- Severity: Medium
- OWASP: A02:2021 Cryptographic Failures / CWE-312
- Details: `buildEncryptedStoreSafe(Context)` at `AuthPreferences.java:133` returns `new EncryptedPrefsSecretStore(context)`, which defers Keystore access until first use. The single-arg `AuthPreferences` constructor Javadoc (lines 89-101) states: "If EncryptedSharedPreferences construction fails … the constructor logs a warning and falls back to an InMemorySecretStore." However, `buildEncryptedStoreSafe()` at line 133 returns only `EncryptedPrefsSecretStore` — the documented fallback to `InMemorySecretStore` is not implemented in the code as written. The `EncryptedPrefsSecretStore.get()` at line 122 catches all `Exception` and returns `null` on decryption failure (which is correct for reads), but `EncryptedPrefsSecretStore.migrateFromPlaintext()` at line 230-235 catches `RuntimeException` from a failed `getEncrypted()` call, logs a warning, and returns — **leaving plaintext credentials un-migrated in `credentials.xml`**. On subsequent launches, the idempotency check at line 207 (`encrypted != null`) will be false if the Keystore is transiently unavailable, so the migration will be re-attempted — but until that re-attempt succeeds, the legacy plaintext keys remain on disk. The Keystore failure scenario is documented as Robolectric-only, but Android Keystore can also fail transiently on real devices (e.g., after factory reset, on first boot, or in Work Profile provisioning). The result is a silent fallback to unencrypted credentials rather than a hard failure.
- Remediation: (1) Log a warning (already done at line 232) and additionally emit a user-visible notification or toast that credential security is degraded. (2) Consider treating a Keystore failure as a fatal error for the migration path and blocking backup until resolved, rather than silently proceeding with plaintext credentials. (3) The Javadoc on `AuthPreferences(Context)` should be corrected to match the actual behavior (no InMemorySecretStore fallback; plaintext remains until Keystore is available).

#### Low (informational)

**L-001 — Public OAuth2 client ID and Google Backup API key committed in `keys.xml`**
- File: `app/src/main/res/values/keys.xml:3-4`
- Severity: Low
- OWASP: A02:2021 Cryptographic Failures / CWE-798
- Details: `keys.xml` contains two long-lived values: `oauth2_client_id` (`959061759285-uqnem9qtjr97856o7b6pkuek0ref5dnd.apps.googleusercontent.com`) and `backup_api_key` (`AEdPqrEAAAAI15zt2oJAvxMu4s5SaHisDyYsduKd2jq_-XnAug`). This file has been in the repository since the "Move into keys" commit, well before the modernization sprint series, and both values are bundled in the release APK anyway (they are not secrets in the traditional sense). For Google OAuth2 installed-app flows there is intentionally no `client_secret` — none was found. The `backup_api_key` is for the Android Backup Transport, which has access controls enforced server-side. This is a low-severity informational finding: these are not private keys but they should be rotated if the project transitions to a closed-source or commercial distribution model. This finding does **not** represent a regression introduced by the modernization sprints.
- Remediation: (1) Accept as-is for the current open-source distribution model. (2) If the project moves to closed-source, rotate both values and store them outside version control (e.g., in a `keys.properties` file excluded from git, injected via CI). No immediate action required.

---

### Detailed Analysis by Security Surface

#### 1. TLS Trust and Certificate Pinning

**Verdict: Sound.**

`TlsTrustPolicy` (enum, 2 values — `SYSTEM_VALIDATED` and `PINNED_CERTIFICATE`) has no trust-all constant. The class Javadoc explicitly prohibits adding one (`CNTR-MODERNIZATION-001`). Verified at `TlsTrustPolicy.java:29-43`.

`AllTrustedSocketFactory.java` does not exist on disk — confirmed by glob returning no results. The delete is complete.

`PinnedCertificateSocketFactory` (`PinnedCertificateSocketFactory.java:117-168`) implements `X509TrustManager.checkServerTrusted()` correctly:
- Empty chain rejected with `CertificateException` (line 128-131).
- SHA-256 fingerprint comparison via `Arrays.equals()` (line 141) — constant-time for equal-length arrays.
- `enrolledCert.checkValidity()` enforces not-before/not-after window (line 149).
- `getAcceptedIssuers()` returns a one-element non-null array (line 153-156) — compliant.

`K9MailTransport.buildSocketFactory()` (`K9MailTransport.java:135-148`) maps `SYSTEM_VALIDATED` to `DefaultTrustedSocketFactory` and `PINNED_CERTIFICATE` to `PinnedCertificateSocketFactory`. The `PINNED_CERTIFICATE` path requires a non-null `pinnedCert` (null guard at line 139-142). There is no `isTrustAllCertificates` code path that influences socket factory selection — the old boolean was only read to decide whether to display a migration notice, and the migration code at `AuthPreferences.java:379-384` actively clears any stale `true` value and never writes `true`.

`PinCertificateEnrollmentFlow` uses `EnrollmentCaptureTrustManager` — an intentional trust-any manager scoped to the display-only TLS handshake for certificate capture. This is the correct design pattern: the ephemeral trust manager is used only to retrieve the certificate for user review; it is local to `fetchLeafCert()` and never assigned to the `PinnedCertStore`. The cert store write at `storeCertOnConfirm()` (line 447-457) is triggered only from the explicit user confirmation callback. This design is sound.

`PinnedCertStore` stores certificates as Base64 DER in `MODE_PRIVATE` shared preferences (`pinned_certs`), excluded from Auto-Backup via `backup_descriptor.xml:7`. No information leak in log statements — the cert key used in log messages is `host:port` (non-sensitive).

The `isTrustAllCertificates()` method still exists in `AuthPreferences.java:277-279` and reads `SERVER_TRUST_ALL_CERTIFICATES`. Grep confirms this method is no longer called from any transport or socket factory path — it is a dead read that could be removed but poses no active risk.

#### 2. Credentials at Rest and Migration

**Verdict: Largely sound; one medium-severity gap in Keystore-failure handling (M-002).**

`EncryptedPrefsSecretStore` uses `MasterKey.Builder` with `AES256_GCM` and `setRequestStrongBoxBacked(true)` (best-effort StrongBox, fallback to TEE/software). The backing file `"credentials"` matches the `backup_descriptor.xml` exclude rule (`credentials.xml`) so auto-backup of encrypted credentials is correctly prevented.

Lazy initialization via double-checked locking on `volatile SharedPreferences encrypted` (lines 83-108) is thread-safe and correct.

`migrateFromPlaintext()` step ordering follows the rollback-safe protocol:
1. Fast-path idempotency check on already-open `encrypted` (line 207).
2. Reads plaintext values into memory from a raw SharedPreferences handle **before** calling `getEncrypted()` (lines 215-220). This is the critical Option-A ordering step.
3. Writes ciphertext + migration marker + `commit()` synchronously (lines 246-251).
4. Clears legacy plaintext only after step-3 commit (lines 254-258).

Process-kill safety: a kill between step 3 and step 4 leaves the marker in the encrypted store so the next launch hits the idempotency short-circuit at step 1 and does not clear the still-present plaintext — acceptable because the ciphertext copy is already durable.

Gap (M-002): if `getEncrypted()` throws at line 229-231, migration is skipped and plaintext credentials remain. The code logs a warning and intends to retry on the next launch, but there is no mechanism to notify the user that their credentials are stored in plaintext. On a real device with a transient Keystore failure this could leave credentials unencrypted indefinitely if the Keystore never recovers.

`CredentialMigrationGate` (`CredentialMigrationGate.java`) is correctly implemented:
- `prepare()` initializes the latch before the background task is submitted.
- `signalComplete()` sets `migrationComplete` (volatile write) before `countDown()`, establishing happens-before.
- `awaitIfNeeded()` skips on main thread (fail-open with log warning at line 158-160) to prevent ANR.
- 10-second timeout prevents indefinite blocking of worker threads.
- `AuthPreferences.getOauth2Token()`, `getOauth2RefreshToken()`, `getImapPassword()` all call `CredentialMigrationGate.awaitIfNeeded()` before reading from `secretStore`. No pre-migration credential read window exists for these three methods.

#### 3. Secrets in Logs

**Verdict: No credential values are logged in production builds.**

Grep of all `Log.*` calls matching credential-adjacent keywords confirmed:
- `OAuth2Client.java:183-184`: `token.getTokenForLogging()` is gated behind `BuildConfig.DEBUG`. `getTokenForLogging()` (`OAuth2Token.java:61-68`) replaces all characters of `accessToken` and `refreshToken` with `X` via `replaceAll(".", "X")`. Only metadata (`tokenType`, `expiresIn`, `userName`) is included in release log output — and even this is only visible under `BuildConfig.DEBUG`. Sound.
- `AccountManagerAuthActivity.java:148`: `Log.d(TAG, "obtained token for " + account + " from AccountManager")` — logs account name only, not the token value. Account name is a low-sensitivity identifier. Acceptable.
- `PeopleApiContactsAdapter.java:50`: logs `"null/blank accessToken"` — the message describes nullity, not the value. No credential exposed.
- `EncryptedPrefsSecretStore.java:126-127`: logs the key name (e.g., `"oauth2_token"`) and exception class name on decrypt failure. No credential value exposed.
- `EncryptedPrefsSecretStore.java:232-234`: logs Keystore failure cause via `e.getMessage()`. Exception messages from `GeneralSecurityException` do not contain credential material.
- All BackupWorker/RestoreWorker auth-related log lines: exception class names and status codes only, no credential values.

#### 4. PendingIntent Mutability and Exported Components

**Verdict: PendingIntent is correctly flagged; exported components are appropriately scoped.**

`ServiceBase.getPendingIntent()` (`ServiceBase.java:277-280`) uses `FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE`. `FLAG_IMMUTABLE` satisfies the Android 12+ (API 31) requirement for explicit mutability declaration. No mutable PendingIntents were found.

**Exported components:**
- `SmsBackupService`, `SmsRestoreService`: `android:exported="false"` (lines 131-135).
- `SmsBroadcastReceiver`: `android:exported="false"` with `android:permission="android.permission.BROADCAST_SMS"` — correctly protected by a system-level permission.
- `BootReceiver`, `PackageReplacedReceiver`: `android:exported="false"`.
- `BackupBroadcastReceiver`: `android:exported="true"` with the public `com.zegoggles.smssync.BACKUP` action (line 163-170). This is an intentional third-party integration surface. `BackupBroadcastReceiver.onReceive()` guards execution behind `preferences.isAllow3rdPartyIntegration()`. The receiver triggers `BackupScheduler.scheduleImmediate()` only — it does not transmit credentials, does not return data, and does not accept parameters from the caller. The attack surface is limited to triggering an immediate backup (which will fail if credentials are not set). Acceptable for an intentionally public receiver.
- `SmsReceiver`, `MmsReceiver`: `android:exported="true"` with `android:permission="android.permission.BROADCAST_SMS"` and `android.permission.BROADCAST_WAP_PUSH` respectively. Both are system-permission-protected.
- `RedirectReceiverActivity`: `android:exported="true"` with an intent filter for `com.zegoggles.smssync:/oauth2redirect`. This is the OAuth2 redirect URI receiver for the browser-based auth flow. The scheme is app-specific (`com.zegoggles.smssync`) and registered as the redirect URI in the OAuth2 client configuration. Intent data processed is the authorization code — not a secret (codes are single-use and short-lived per RFC 6749 §4.1.2). Acceptable.
- `ComposeSmsActivity`, `HeadlessSmsSendService`: SMS/MMS default-app components; exported with standard SMS schemes. Required for the "set as default SMS app" capability. Outside the scope of this review.
- `OAuth2WebAuthActivity`, `AccountManagerAuthActivity`: not `android:exported="true"` in the manifest — correct for activities that should not be directly invocable from outside the app.

#### 5. WorkManager Data

**Verdict: No credentials in WorkManager input/output Data.**

`BackupWorker` input is received via `WorkerParameters`. Grep of all `workDataOf(...)` calls in `BackupWorker.kt` and `RestoreWorker.kt` confirms output data contains only: `KEY_FAILURE_REASON` (short string category like `"auth_failed"`), `PROGRESS_KEY_STATE`, `PROGRESS_KEY_BACKED_UP`, `PROGRESS_KEY_ITEMS_TO_SYNC` — all non-sensitive progress/status values. No token, password, or URI is placed in WorkManager Data.

#### 6. OAuth2 Client

**Verdict: Sound.**

`OAuth2Client` uses `HttpsURLConnection` (HTTPS enforced by the class type — any non-HTTPS URL would fail the cast to `HttpsURLConnection` at line 216). The token endpoint `https://www.googleapis.com/oauth2/v3/token` is hardcoded HTTPS. No cleartext fallback. The `postTokenEndpoint()` method does not disable hostname verification or certificate validation — default Android system TLS is used for the Google token endpoint.

No `client_secret` is present anywhere in the codebase (confirmed by grep returning no results) — correct for the installed-app OAuth2 flow.

---

### Security Checklist

- [x] No hardcoded private credentials or secrets (OAuth2 client ID is public; no client_secret present)
- [x] Input validation on all user inputs (IMAP folder validation in K9MailTransport:310-313; server address parsing with defaults)
- [x] Output encoding prevents injection attacks (IMAP search query uses format string with datatype enum value — no user-controlled injection; URI components URL-encoded in AuthPreferences:346-352)
- [x] Authentication tokens handled securely (EncryptedSharedPreferences; debug-only logging with masking)
- [x] Authorization checks on all protected resources (BackupBroadcastReceiver guarded by allow3rdParty flag; exported SMS receivers protected by system permissions)
- [x] Sensitive data encrypted at rest (EncryptedSharedPreferences with AES-256-GCM; credentials.xml excluded from Auto-Backup)
- [x] Pinned cert store excluded from Auto-Backup (backup_descriptor.xml:7)
- [x] Error messages do not leak internal credential details (exception class names only in logs; no stack traces contain credential values)
- [x] PendingIntent uses FLAG_IMMUTABLE (ServiceBase.java:280)
- [x] No trust-all TrustManager on any data connection path (AllTrustedSocketFactory deleted; TlsTrustPolicy has no trust-all constant)
- [x] No SERVER_TRUST_ALL_CERTIFICATES=true writes (migrate() only clears stale true; never writes true)
- [x] OAuth2 token logging gated behind BuildConfig.DEBUG with masking (OAuth2Token.getTokenForLogging())
- [ ] **[OPEN]** OAuth2 token not placed in Activity Intent extra (M-001 — AccountManagerAuthActivity legacy path)
- [ ] **[OPEN]** Keystore failure during migration results in silent plaintext persistence with no user notification (M-002)
- [ ] **[OPEN]** `isTrustAllCertificates()` is a dead method that should be removed to reduce confusion (Low)
- [x] Dependencies: no new vulnerable dependencies introduced by the modernization (androidx.security:security-crypto:1.1.0-alpha06 is the current alpha; no known CVEs against this version as of June 2026)

---

### Prioritized Remediation List

| Priority | Finding | Effort | Risk if Deferred |
|----------|---------|--------|-----------------|
| 1 | M-001: Raw OAuth token in Activity Intent extra (AccountManagerAuthActivity legacy path) | Low — move `setOauth2Token()` call into `useToken()` before `setResult` | Token capture by privileged process during account linking; one-time risk per auth event |
| 2 | M-002: Silent plaintext credential persistence on Keystore failure | Medium — add user-visible notification + document degraded-security state | Credentials remain unencrypted on devices with transient Keystore failures; user unaware |
| 3 | L-001 (informational): Dead `isTrustAllCertificates()` method and stale keys.xml values | Low — remove dead method; defer key rotation to commercial release | No active risk; dead code confusion only |

---

### Notes on Scope

This review covers production code in `app/src/main/java/` and `app/src/main/res/`. Test code in `app/src/test/` and `app/src/androidTest/` was not reviewed for security findings (test code by definition may use insecure fakes). The `k9mail-vendored/` module was not in scope for this review as it was not modified by the sprint series. The `PlaintextSharedPrefsSecretStore` class is package-private (`class`, not `public class`) and carries a `@Deprecated` annotation — its use is limited by package visibility, which reduces (but does not eliminate) the risk of accidental production use.
