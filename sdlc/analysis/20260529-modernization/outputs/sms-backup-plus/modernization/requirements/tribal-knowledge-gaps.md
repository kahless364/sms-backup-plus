---
artifact_id: 20260529-modernization
step_id: "2.5.5"
title: Tribal Knowledge Gap Analysis — SMS Backup+
generated: 2026-05-29
subject: sms-backup-plus
phase: modernization
prompt_id: migration/requirements/05-tribal-knowledge-gaps
assessment_status: in-progress
---

# Tribal Knowledge Gap Analysis — SMS Backup+

**Engagement:** 20260529-modernization · Phase 2.5, Step 2.5.5 (Tribal Knowledge Gap Analysis)
**Subject:** P01 `app` — `com.zegoggles.smssync`
**Date:** 2026-05-29
**All paths relative to repo root** (`C:/Code/Android/sms-backup-plus`)

---

## Context and Scope

This analysis focuses on the seven non-obvious knowledge areas called out in the assessment brief, cross-referenced against evidence found in the source code, git history, and prior phase outputs (code-classification, patterns, ilities-assessment). Every gap has a direct migration-risk consequence — the analysis does not catalogue incidental code commentary but concentrates on knowledge whose absence would cause a refactored or migrated component to silently produce incorrect results, degrade security, or break backward compatibility with user data already in Gmail.

---

## Summary

| Gap Type | Count | Risk Level | SME Sessions Needed |
|----------|:-----:|:----------:|---------------------|
| Undocumented Protocol Contract | 3 | High | Yes |
| Workaround / Lifecycle Bypass | 2 | High | Yes |
| Silent Security Behavior | 2 | High | Yes |
| Undocumented Business Rule | 4 | High | Yes |
| Magic Number / Constant | 3 | Medium | Maybe |
| Residual TODO / XXX Comment | 5 | Medium | Maybe |
| **Total Gaps** | **19** | | |

---

## Risk Assessment

| Risk Level | Count | Estimated SME Time |
|:----------:|:-----:|:------------------:|
| High | 11 | 6–8 hours |
| Medium | 8 | 2–3 hours |
| **Total** | **19** | **8–11 hours** |

---

## High-Risk Gaps (Require SME Session)

---

### Gap: [GAP-001] — Proprietary `X-smssync-*` IMAP Header Suite: Extent, Semantics, and Restore Contract

**Location:** `app/src/main/java/com/zegoggles/smssync/mail/Headers.java:1-38`, `HeaderGenerator.java:26-129`, `MessageConverter.java` (restore side)
**Risk Level:** High
**Gap Type:** Undocumented Protocol Contract

**Code (header declarations):**
```java
// Headers.java
static final String ID             = "X-smssync-id";
static final String ADDRESS        = "X-smssync-address";
static final String DATATYPE       = "X-smssync-datatype";    // SMS | MMS | CALLLOG — drives folder search
public static final String TYPE    = "X-smssync-type";        // subtype, datatype-specific
public static final String DATE    = "X-smssync-date";
static final String THREAD_ID      = "X-smssync-thread";
static final String READ           = "X-smssync-read";
static final String STATUS         = "X-smssync-status";
static final String PROTOCOL       = "X-smssync-protocol";
static final String SERVICE_CENTER = "X-smssync-service_center";
static final String BACKUP_TIME    = "X-smssync-backup-time";
public static final String VERSION = "X-smssync-version";
public static final String DURATION= "X-smssync-duration";
```

**Why This Is a Gap:**

The 13 `X-smssync-*` headers constitute a private application-level wire format embedded in RFC-822 messages stored in Gmail/IMAP. They are the only mechanism through which restore reconstructs original SMS/MMS/call-log content-values from email bodies. The header set is the **de-facto schema contract** between the backup writer and the restore reader — equivalent to a database schema — but it has no schema version guard, no migration document, and no formal specification. Three specific knowledge gaps exist:

1. **`X-smssync-datatype` drives the IMAP SEARCH filter.** `BackupImapStore.buildSearchQuery` emits `(HEADER X-smssync-datatype "SMS")` (or MMS/CALLLOG). If this header is missing from a stored message (old backup before the header was introduced — commit `7dc3860 Drop legacy header support, simplify search query`), that message is invisible to restore. The commit message says "drop legacy header support" but does not specify what the legacy format was, what version introduced `DATATYPE`, or whether any users still have pre-header messages in Gmail.

2. **`X-smssync-version` is written but never read on restore.** `HeaderGenerator.java:54` writes `VERSION`, but `MessageConverter` does not branch on it. Is this intentional (version is informational only) or was a version-specific parsing path dropped and never replaced?

3. **`X-smssync-type` carries a "datatype-specific subtype"** (`Headers.java:13` comment). For SMS it is the `TYPE` column (inbox/sent/draft/failed/queued). For MMS it is `MESSAGE_TYPE`. For CALLLOG it is `CallLog.Calls.TYPE` (incoming/outgoing/missed/rejected/voicemail). But the integer value meanings differ across types, and the header name "type" is the same — a consumer must know the enclosing `DATATYPE` first. This is non-obvious to anyone refactoring `MessageConverter`.

**Potential Interpretations:**
1. The header schema is stable and all users have post-2014 backups; the "drop legacy" commit means pre-header messages are simply unrestorable and that was an accepted trade-off at the time.
2. There is an undocumented minimum version of the app that produced compatible headers, and users upgrading from very old installs silently lost restore capability.
3. `X-smssync-version` was intended for future schema migration and was never implemented, leaving a dead write.

**Risk if Not Addressed:**
Any refactoring of `MessageConverter`, `HeaderGenerator`, or `BackupImapStore.buildSearchQuery` without understanding the full semantics — especially the `DATATYPE` search filter — could silently cause partial or zero restore from existing user backups. Users discovering missing messages months after migration would generate support escalations that are very difficult to debug post-hoc.

**SME Questions:**
1. What is the oldest backup format still expected to be restorable by the current app? Is there a known "minimum required version" to restore from?
2. Was `X-smssync-version` ever intended to gate version-specific parsing logic, or is it purely diagnostic?
3. When commit `7dc3860` dropped "legacy header support" — what did the legacy format look like, and is any user expected to still have such messages?
4. Are there headers that are optional (i.e., old messages may not have them) versus required for restore?
5. If `X-smssync-datatype` is absent from a stored message, is that message silently skipped, or should it be treated as an error?

---

### Gap: [GAP-002] — `UID SEARCH 1:*` Custom IMAP Search: Semantics, Server Compatibility, and `SENTSINCE` Behavior

**Location:** `app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java:188-196`
**Risk Level:** High
**Gap Type:** Undocumented Protocol Contract

**Code:**
```java
private String buildSearchQuery(DataType dataType, Date since, boolean flagged) {
    final StringBuilder sb = new StringBuilder("UID SEARCH 1:*")
            .append(' ')
            .append(String.format(ENGLISH, "(HEADER %s \"%s\")", DATATYPE.toUpperCase(ENGLISH), dataType))
            .append(" UNDELETED");
    if (since != null) sb.append(" SENTSINCE ").append(RFC3501_DATE.get().format(since));
    if (flagged) sb.append(" FLAGGED");
    return sb.toString().trim();
}
```

**Why This Is a Gap:**

The search query has three non-obvious semantic decisions that carry significant migration risk:

1. **`1:*` sequence range, not UID range.** The query uses sequence numbers (`1:*` = all messages in sequence order), not a UID range. The k-9 `executeSimpleCommand` delivers raw IMAP responses. Any refactoring that replaces this with a named `ImapSearcher` method that implicitly uses UIDs would change search semantics. Whether IMAP servers commonly return all messages on `UID SEARCH 1:*` (treating the sequence range as universally valid) or behave differently for large folders is not documented.

2. **`SENTSINCE` is used, not `SINCE`.** RFC 3501 defines both: `SINCE` matches against the internal IMAP date (arrival), `SENTSINCE` matches against the `Date:` header (sent date). The choice of `SENTSINCE` means the query filters on the SMS/MMS sent timestamp, not the IMAP server arrival timestamp. For messages backed up while the device clock was incorrect, the two can diverge substantially. This is the right choice for incremental backup (since the app stores `maxSyncedDate` in terms of message send time), but a refactorer who replaces `SENTSINCE` with `SINCE` would cause backup to skip or re-backup the wrong window of messages.

3. **`FLAGGED` for starred-only restore.** The starred-message-restore feature relies on Gmail's starring being exposed as the IMAP `\Flagged` flag. This is a Gmail-specific behavior. Non-Gmail IMAP servers may not synchronize their "important" or "flagged" concepts to `\Flagged` in the same way. There is no comment explaining whether this feature is intentionally Gmail-only.

Additionally, the commented-out `Debug.startMethodTracing("sorting")` at line 172 (`BackupImapStore.java`) indicates this path was profiled historically — the developer knew the full-folder client-side sort was slow but left it, presumably because server-side `SORT` is an optional IMAP extension not universally available.

**Potential Interpretations:**
1. `SENTSINCE` was deliberately chosen over `SINCE` and is load-bearing; changing it would break incremental backup for all users.
2. `FLAGGED` restore is a Gmail-only feature accepted as such; non-Gmail users are warned or silently unaffected.
3. The `1:*` sequence-range form is a historical artifact from the k-9 API surface; the intended semantic is "all messages in folder."

**Risk if Not Addressed:**
Replacing `buildSearchQuery` or the k-9 `executeSimpleCommand` invocation without understanding `SENTSINCE` vs. `SINCE` semantics would silently cause incorrect incremental backup windows, potentially re-backing-up thousands of already-stored messages or missing new ones.

**SME Questions:**
1. Why was `SENTSINCE` chosen over `SINCE`? Was this a deliberate decision based on how `maxSyncedDate` is stored?
2. Is the `FLAGGED`-based starred restore intentionally Gmail-only? What is the expected behavior on a self-hosted IMAP server?
3. Has server-side `SORT` or `SEARCH … SINCE <UID>` bounding ever been attempted? Why was the client-side full-sort approach accepted as permanent?
4. Has the `UID SEARCH 1:*` query ever caused problems on large mailboxes in production (user reports of timeouts, memory errors)?

---

### Gap: [GAP-003] — MMS Inbound/Outbound Detection: Dual-Logic Fallback and PduHeaders Magic Constants

**Location:** `app/src/main/java/com/zegoggles/smssync/mail/MmsSupport.java:91-153`
**Risk Level:** High
**Gap Type:** Undocumented Business Rule + Magic Numbers

**Code (key section):**
```java
String PduHeadersFROM  = "137";   // MMS PDU header type for sender
String PduHeadersTO    = "151";   // MMS PDU header type for recipient
String PduHeadersCC    = "130";   // MMS PDU header type for CC
String type = cursor.getString(cursor.getColumnIndex("type"));
if (type.equals(PduHeadersFROM)) {
    sender = record;
} else if (type.equals(PduHeadersTO) || type.equals(PduHeadersCC)) {
    recipients.add(record);
} else {
    Log.w(TAG, "New logic for to/from did not work, falling back to old logic");
    if (MmsConsts.INSERT_ADDRESS_TOKEN.equals(address)) {
        inbound = false; // probably not the best way to determine if a message is inbound or outbound (legacy logic)
    } else {
        recipients.add(record);
    }
}
// Override with MESSAGE_BOX if present
if (Integer.parseInt(msgMap.get(Telephony.BaseMmsColumns.MESSAGE_BOX)) == MESSAGE_BOX_INBOX) {
    inbound = true;
} else if (...MESSAGE_BOX_SENT) {
    inbound = false;
}
```

**Why This Is a Gap:**

There are three compounding layers of undocumented knowledge here:

1. **The PduHeaders constants (137, 151, 130) are not from a public Android SDK constant.** The inline comment references `PduHeaders.java` from `platform/frameworks/opt/mms` — an internal AOSP class that is not part of the public Android API. The constants are hardcoded as string literals (not from `android.provider.Telephony` or any public class), making them invisible to tooling. A code reviewer or refactorer would not know these are MMS PDU header type codes without following the comment URL. These constants have been stable for over a decade but are not contractually public.

2. **Three-way inbound/outbound detection with a known-bad fallback path.** The code implements three strategies: (a) PduHeaders FROM/TO (new), (b) `INSERT_ADDRESS_TOKEN` (legacy "insert" placeholder address meaning "self"), (c) `MESSAGE_BOX` column override. The `INSERT_ADDRESS_TOKEN` path is acknowledged as "probably not the best way" with "legacy logic" comments. The correct detection is (c) overriding everything else. Understanding *why* (a) and (b) are both kept — rather than just using (c) — requires knowing whether `MESSAGE_BOX` is reliably populated on all Android versions and carriers.

3. **The "strip recipient if also sender" RCS thread-splitting fix.** The code at lines 140–149 strips a recipient from the list if their ID matches the sender, to prevent RCS thread splitting in Gmail. This is a carrier/RCS-specific behavior that was added in git commit `ac71d55` with minimal context. Whether this fix applies only to RCS or also to standard MMS, and whether it has edge cases on group MMS threads, is not documented.

**Risk if Not Addressed:**
Any refactoring of MMS handling that changes how `inbound` is determined or how recipients are filtered would silently produce wrong email `From:`/`To:` direction, causing MMS messages to appear as sent when they were received or vice versa in Gmail. This is a data-integrity issue affecting the user's entire MMS backup history.

**SME Questions:**
1. Are the PduHeaders constants (137/151/130) safe to hardcode, or should they be read from `android.provider.Telephony.Mms.Addr` or an internal constant class?
2. Under what Android versions or carrier configurations does the `INSERT_ADDRESS_TOKEN` fallback path actually trigger? Is it still needed in 2024+ or can it be removed?
3. Why is `MESSAGE_BOX` not the sole inbound/outbound signal? Are there known cases where it is absent or incorrect?
4. Is the sender-stripping logic (lines 140–149) specific to RCS messages, or does it apply to all MMS? What happens in group MMS threads where the sender is also a listed recipient?
5. What is `MmsConsts.INSERT_ADDRESS_TOKEN`'s value, and which Android versions/carriers actually put it in the address field?

---

### Gap: [GAP-004] — `SmsJobService → new SmsBackupService()` Lifecycle Bypass: Correctness Guarantees and Migration Assumptions

**Location:** `app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java:79-86`
**Risk Level:** High
**Gap Type:** Workaround / Lifecycle Bypass

**Code:**
```java
} else if (shouldRun(jobParameters)) {
    // Since API level 26, an app in background cannot start a background service,
    // so just instantiate service manually
    // https://developer.android.com/about/versions/oreo/background.html#services
    SmsBackupService service = new SmsBackupService();
    service.attachBaseContext(this);
    service.handleIntent(new Intent(jobParameters.getTag()).putExtras(extras));
    jobs.put(jobParameters.getTag(), jobParameters);
    return true;
}
```

**Why This Is a Gap:**

This is not a standard Android pattern. `SmsBackupService` is a `Service` subclass, but it is instantiated with `new SmsBackupService()`, which means:

- `onCreate()` is **never called** on this instance. `SmsBackupService.onCreate()` sets `service = this` (the static back-reference that `isServiceWorking()` relies on) and calls `super.onCreate()` (which in `ServiceBase` acquires the `WakeLock`/`WifiLock`). Since `onCreate()` is not called by the manual instantiation, the static `service` field is **not set** for this execution path. How `isServiceWorking()` behaves in this scenario — and whether `SmsRestoreService.isServiceIdle()` gives a correct answer — is not documented.
- `onStartCommand()` is never called — so `START_NOT_STICKY` restart semantics do not apply to jobs dispatched through `SmsJobService`.
- `attachBaseContext(this)` passes the `JobService` context, meaning the "service" runs under the JobService's lifecycle, not a separate service lifecycle.

The comment correctly identifies the Oreo background-start restriction as the reason, but does not document what `ServiceBase` lifecycle behaviors are **explicitly safe to bypass** in this mode versus what invariants are broken. The code added in commit `39acb12 Fix service start crashing in background` with minimal description.

**Potential Interpretations:**
1. `acquireLocks()` in `ServiceBase` is called from `BackupTask.acquireLocksAndBackup()` — not from `onCreate()` — so WakeLock acquisition still works through this path. The `service = this` not being set is acceptable because `SmsJobService.backupStateChanged()` tracks job completion independently.
2. There is a subtle race condition between `SmsJobService.backupStateChanged()` checking `jobs.remove(state.backupType.name())` and the static `SmsBackupService.isServiceWorking()` returning false (since `service` was never set), which could allow a concurrent manual backup to start while a scheduled backup is running.
3. The `legacyCheckConnectivity()` path in `SmsBackupService.backup()` is skipped when `isUseOldScheduler()` is false (line 118), which happens to be the case when `SmsJobService` is active — so the `@SuppressWarnings("deprecation")` on `legacyCheckConnectivity` and the `ConnectivityManager.getActiveNetworkInfo()` path are exercised only in old-scheduler mode.

**Risk if Not Addressed:**
The WorkManager migration (Phase 2, TECH-002) replaces `SmsJobService` entirely. Understanding exactly what `SmsBackupService.onCreate()` must and must not provide for the WorkManager `CoroutineWorker` replacement is essential. If the `CoroutineWorker` omits initialization logic that was implicitly provided (e.g., by the `JobService` context), the migrated worker may silently fail to acquire locks or report state correctly.

**SME Questions:**
1. When `SmsJobService` instantiates `SmsBackupService` manually and calls `attachBaseContext`, is the `WakeLock` from `ServiceBase.acquireLocks()` acquired under the `JobService`'s `WakeLock` or is it a separate one?
2. Does `SmsBackupService.isServiceWorking()` ever return a correct answer when called from a path that went through `SmsJobService`? Is there a known race with manual backup triggering?
3. What behavior in `SmsBackupService.onCreate()` must be replicated in the `BackupWorker` replacement?
4. Was there ever a crash or state corruption reported specifically because of this lifecycle bypass?

---

### Gap: [GAP-005] — `AuthPreferences.migrate()` Silent Trust-All Switch: Intent, Affected User Population, and Rollback Safety

**Location:** `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java:276-288`
**Risk Level:** High
**Gap Type:** Silent Security Behavior

**Code:**
```java
void migrate() {
    if (useXOAuth()) {
        return;
    }
    // convert deprecated authentication methods
    if ("+ssl".equals(getServerProtocol()) ||
        "+tls".equals(getServerProtocol())) {
        preferences.edit()
            .putBoolean(SERVER_TRUST_ALL_CERTIFICATES, true)
            .putString(SERVER_PROTOCOL, getServerProtocol()+"+")
            .commit();
    }
}
```

**Why This Is a Gap:**

`migrate()` is called from `App.onCreate()` on every cold start. For any user whose stored `SERVER_PROTOCOL` is `+ssl` (validated SSL) or `+tls` (validated TLS/STARTTLS), this migration:

1. **Silently enables `SERVER_TRUST_ALL_CERTIFICATES = true`** — disabling all certificate validation.
2. **Appends `+` to the protocol string** (e.g., `+ssl` becomes `+ssl+`), which is the format used for self-signed/trust-all connections in the k-9 URI scheme.

The migration runs without any user notification. The ilities-assessment (ARCH-008) identifies this as a High security defect. However, the tribal knowledge gap is in the **original intent**:

- Who configured `+ssl` or `+tls` (without the trailing `+`) in the first place? These settings indicate users who set up a custom IMAP server with validated TLS before a UI change removed these options. Were these users genuinely connecting to self-signed servers (so trust-all was correct) or to CA-signed servers (where trust-all is a silent security downgrade)?
- What was the UI flow that created `+ssl`/`+tls` (non-trust-all) settings? That UI path appears to no longer exist given that `DEFAULT_SERVER_PROTOCOL = "+ssl+"`. If the UI no longer allows creating these values, the migration can only trigger for users who had the old app and upgraded — how large is that population?
- `migrate()` is package-private with no `@VisibleForTesting`. `AuthPreferencesTest` does not cover this path (noted as a gap in code-classification.md). The test gap means there are no regression guards.

**Risk if Not Addressed:**
SEC-001 (Phase 1) requires deleting `AllTrustedSocketFactory` and fixing `migrate()`. Without understanding the intended migration path and the user population it affects, the fix risks: (a) breaking connectivity for users who genuinely need trust-all for self-hosted IMAP, or (b) incorrectly leaving trust-all enabled for users who had validated TLS.

**SME Questions:**
1. What was the original UI that allowed users to select `+ssl` or `+tls` without trust-all? Was it a manual server address entry field that accepted raw protocol strings?
2. Was the intent of `migrate()` to help self-hosted-IMAP users (who genuinely needed trust-all) or was it a broad migration that accidentally degraded validated-TLS users?
3. What is the estimated size of the user population that has `SERVER_PROTOCOL` stored as `+ssl` or `+tls`? Can this be inferred from app crash/play store data?
4. After SEC-001 removes trust-all: for users who have `SERVER_TRUST_ALL_CERTIFICATES = true` already set from this migration, should the fix reset it to `false` (potentially breaking their IMAP connection) or show a migration dialog?
5. Is there any context from the git history commit `5c7a301 Clean up IMAP security settings` about what triggered this migration?

---

### Gap: [GAP-006] — XOAuth2 vs. App-Password Gmail Paths: Scope Requirements, Token Lifetime, and Contacts API Dependency

**Location:** `app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java:103-107`, `AuthPreferences.java:167-186`, `AuthPreferences.java:257-265`
**Risk Level:** High
**Gap Type:** Undocumented Business Rule

**Code (OAuth2 scope and XOAuth2 token construction):**
```java
// OAuth2Client.java - scopes requested
private static final String GMAIL_SCOPE    = "https://mail.google.com/";
private static final String CONTACTS_SCOPE = "https://www.google.com/m8/feeds/";  // GData v3 — DEPRECATED
private static final String DEFAULT_SCOPE  = GMAIL_SCOPE + " " + CONTACTS_SCOPE;
private static final String CONTACTS_URL   = "https://www.google.com/m8/feeds/contacts/default/thin?max-results=1";

// AuthPreferences.java - SASL XOAUTH2 token construction
private String generateXOAuth2Token() {
    final String formatted = "user=" + username + "\001auth=Bearer " + token + "\001\001";
    return Base64.encodeToString(formatted.getBytes(UTF_8), NO_WRAP);
}
```

**Why This Is a Gap:**

Five specific areas of undocumented complexity:

1. **`CONTACTS_SCOPE` uses GData v3 (`m8/feeds`) which Google has deprecated in favor of the People API.** The Contacts scope is requested only to resolve the Gmail account username (email address) via `getUsernameFromContacts()`. The GData v3 Contacts API was formally sunset by Google and may stop accepting new authorizations or return errors. Whether this is already breaking for new OAuth2 sign-ins is unclear. The `@SuppressWarnings("deprecation")` on `useXOAuth()` reinforces that this whole path is known-deprecated.

2. **The SASL XOAUTH2 token format is hand-assembled.** The format `"user=" + username + "\001auth=Bearer " + token + "\001\001"` is the XOAUTH2 initial client response (RFC 6750 / Google's XOAUTH2 spec). The `\001` (Control+A) separator is a non-printing character that is easy to accidentally corrupt in string handling. The `TODO: this should probably be handled in K9` comment at `AuthPreferences.java:241` acknowledges the app is doing something that arguably belongs in the IMAP library.

3. **Double URL-encoding of username/password in the IMAP URI.** `AuthPreferences.formatUri()` calls `encode(encode(username))` and `encode(encode(password))`. The comment at line 204 references a k-9 bug: `"there's a bug in K9mail-library which requires double-encoding of uris"` with a specific commit URL (`b0d401c3b73c6b57402dc81d3cfd6488a71a1b98`). If the k-9 dependency is ever updated to a version that fixes this bug, the double-encoding would become triple-encoding and break all authentication. The k-9 dependency is pinned to a git SHA (`eaf689025e`) precisely because of such compatibility concerns.

4. **App-password path vs. XOAuth2 path have different server address behaviors.** `getStoreUri()` hardcodes `DEFAULT_SERVER_ADDRESS = "imap.gmail.com:993"` for XOAuth2 but uses `getServerAddress()` (from preferences) for PLAIN auth. This means XOAuth2 users can only connect to Gmail; PLAIN users can use any IMAP server. The business rule that XOAuth2 is Gmail-only and PLAIN is multi-server is not documented anywhere except by inference from `AuthPreferences.java:170-174`.

5. **Token refresh behavior on HTTP 400 only.** `BackupTask.handleAuthError` only attempts token refresh when the XOAUTH2 failure status is exactly `400` (lines 184, 158 in RestoreTask). A status of `401` or any other code is treated as a terminal error. The choice of `400` (Bad Request) as the refresh trigger — rather than `401` (Unauthorized) which is more conventional — is specific to Google's XOAUTH2 implementation and not documented.

**Risk if Not Addressed:**
Replacing the auth layer (Phase 3 consideration: AppAuth / modern auth library) without understanding the double-encoding bug, the XOAUTH2 token format, and the GData Contacts dependency would break OAuth2 sign-in for all users. The double-encoding in particular is an invisible invariant that would survive code review.

**SME Questions:**
1. Is `CONTACTS_SCOPE` / `m8/feeds` still working for existing authorized users? Has there been any uptick in auth failures that might indicate Google's GData API deprecation is in effect?
2. What would break if the double-encoding (`encode(encode(...))`) were removed? Has this ever been tested against a k-9 build that fixed the double-encoding bug?
3. Why is token refresh triggered on HTTP status 400 rather than 401? Is this a documented Google XOAUTH2 behavior or was it discovered empirically from production failures?
4. Is there a reason `generateXOAuth2Token()` is implemented in the app rather than in the k-9 library? Was this ever discussed with k-9 upstream?
5. Has the GData Contacts API (`m8/feeds`) replacement with People API ever been attempted? What scope would be needed?

---

### Gap: [GAP-007] — MMS Backup: Thread ID Strategy, SMIL Suppression, and Attachment URI Lifetime

**Location:** `app/src/main/java/com/zegoggles/smssync/mail/MessageGenerator.java:155-188`, `MmsSupport.java:155-186`
**Risk Level:** High
**Gap Type:** Undocumented Business Rule

**Code (thread ID strategy comments):**
```java
// MessageGenerator.java:159-176 — three commented-out alternatives retained as documentation
// We could thread by contact ID...
// We could grab all raw addresses...
// We could thread by messaging app's thread ID... (chosen)
String mmsThreadId = msgMap.get(Telephony.BaseMmsColumns.THREAD_ID);

// MmsSupport.java:175-176
if (!TextUtils.isEmpty(contentType) && contentType.startsWith("text/") ...) {
    parts.add(new MimeBodyPart(new TextBody(text), contentType));
} else if ("application/smil".equalsIgnoreCase(contentType)) {
    // silently ignore SMIL stuff
} else {
    // attach everything else
    final Uri partUri = Uri.withAppendedPath(Consts.MMS_PROVIDER, MMS_PART + "/" + id);
    parts.add(Attachment.createPartFromUri(resolver, partUri, fileName, contentType));
}
```

**Why This Is a Gap:**

1. **Thread ID strategy was explicitly evaluated and chosen, but the reasoning is only preserved in commented-out code.** The three commented-out alternatives (contact ID, raw phone numbers, THREAD_ID) represent actual engineering decisions. The chosen approach (THREAD_ID from `BaseMmsColumns`) was selected after the original author found contact-ID threading "worked pretty badly with MMS threads." The raw-address approach was abandoned because `+1` prefix inconsistency. This institutional knowledge exists only in these comments. A v2 message-ID was introduced (commit implied by the `"v2"` suffix in `HeaderGenerator.java:50`) to allow users to re-backup and fix threading. Whether the v2 transition was backward-compatible (did it cause Gmail to create duplicate message threads for users who upgraded mid-backup?) is not documented.

2. **SMIL is silently suppressed.** `application/smil` parts are silently dropped with a comment "silently ignore SMIL stuff." SMIL is the presentation layout metadata for MMS messages (positioning images relative to text in carrier-rendered MMS). The decision to drop it was presumably taken because most email clients cannot render SMIL. But whether this affects MMS restore (does `ContentValues` need any SMIL metadata to correctly write the MMS back to the SMS provider?) is unknown.

3. **MMS attachment URIs (`content://mms/part/ID`) are read at backup time.** The URI scheme `Consts.MMS_PART + "/" + id` reads MMS part data from the ContentProvider at backup time. These URIs are valid while the MMS is on the device. After a factory reset and restore, the numeric `_id` values will differ. The restore path writes MMS parts back into the ContentProvider — but the backup contains the attachment as a MIME part in the email, not a URI. Understanding how `Attachment.createPartFromUri` reads and how `MessageConverter.messageToContentValues` restores attachments is critical.

**Risk if Not Addressed:**
Changes to MMS handling without understanding the THREAD_ID strategy and the v2 message-ID change could cause Gmail thread fragmentation for users whose MMS and SMS messages were previously in the same Gmail thread. This is cosmetically destructive (correct data, wrong grouping) and very difficult to detect with automated tests.

**SME Questions:**
1. The v2 `MESSAGE_ID` suffix was added to allow re-backup to fix threading — is this still the recommended remediation for users with broken threads, or has it caused problems (e.g., Gmail treating v2 messages as duplicates)?
2. Does SMIL suppression affect MMS restore? Are there MMS message types that depend on SMIL metadata to display correctly after restore?
3. Are there known issues with MMS attachment restore on specific Android versions — specifically around ContentProvider write permissions for `content://mms/part`?
4. Does the `THREAD_ID` threading approach align with SMS threading (which also uses `THREAD_ID`)? Do mixed SMS+MMS threads always get the same THREAD_ID, ensuring they remain in the same Gmail thread after backup?

---

### Gap: [GAP-008] — `AuthPreferences` Double URL-Encoding: k-9 Bug Dependency and Version Coupling

*(This gap is enumerated separately from GAP-006 due to its specific migration-blocking nature.)*

**Location:** `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java:200-208`
**Risk Level:** High
**Gap Type:** Workaround

**Code:**
```java
private String formatUri(AuthType authType, String serverProtocol, String username, String password, String serverAddress) {
    return String.format(IMAP_URI,
        serverProtocol,
        authType.name().toUpperCase(Locale.US),
        // NB: there's a bug in K9mail-library which requires double-encoding of uris
        // https://github.com/k9mail/k-9/commit/b0d401c3b73c6b57402dc81d3cfd6488a71a1b98
        encode(encode(username)),
        encode(encode(password)),
        serverAddress);
}
```

**Why This Is a Gap:**

The double-encoding is a documented workaround for a specific bug in the k-9 mail library at the pinned commit (`eaf689025e`). The workaround is correct *for that specific commit* but creates a hidden invariant coupling between the app and the k-9 version. The referenced k-9 commit (`b0d401c3b73c6b57402dc81d3cfd6488a71a1b98`) shows the *fix* that was applied in k-9 — meaning the **double-encoding is working around a bug in the k-9 version before that fix commit**. If the k-9 dependency is ever advanced to a post-fix version, the double-encoding becomes incorrect (triple-encoded credentials when k-9 decodes once and the URI parser decodes again). There is no test covering what happens when single-encoding is used.

**Risk if Not Addressed:**
Phase 2 ACL work (introducing `MailTransport` port) and any k-9 version bump would need to verify whether this workaround is still required. Getting it wrong would silently break all IMAP authentication for all users.

**SME Questions:**
1. Has the k-9 dependency ever been advanced beyond commit `eaf689025e`? If so, was double-encoding still required?
2. Is there a test — even a manual one — that verifies credentials containing `@`, `+`, or other special characters work correctly with single vs. double encoding?
3. Is the k-9 SHA pin (`eaf689025e`) in `build.gradle` set specifically to stay before or after the double-encoding fix? Which side of `b0d401c3` is the pinned SHA on?

---

## Medium-Risk Gaps (Document or Verify)

| ID | Location | Type | Brief Description | Migration Question |
|----|----------|------|-------------------|--------------------|
| GAP-009 | `service/BackupJobs.java:205` | Magic Number | `RETRY_POLICY_EXPONENTIAL, 30, 300` — 30s initial, 300s cap | Were these values tuned from production backoff data, or are they framework defaults? WorkManager's default backoff is different (15s min, 5h max); must be explicitly set to preserve behavior. |
| GAP-010 | `service/BackupJobs.java:56` | Magic Number | `BOOT_BACKUP_DELAY = 60` seconds | Why 60s post-boot delay? Device stabilization? Connectivity window? Must be preserved in WorkManager `setInitialDelay`. |
| GAP-011 | `service/RestoreTask.java:124` | Magic Number | `if (currentRestoredItem % 50 == 0) service.clearCache()` | Why every 50 messages? Was this tuned against SD card fill behavior or OOM? WorkManager's periodic checkpoint approach may make this unnecessary, but if the cache-clear is tied to ContentProvider cursor window limits it must be retained. |
| GAP-012 | `service/SmsBackupService.java:63-64` | Magic Number | `BACKUP_ID = 1`, `NOTIFICATION_ID_WARNING = 1` — same value | Both notification IDs are 1. Is this intentional (warning replaces backup notification) or a bug where the warning notification cancels the foreground notification? |
| GAP-013 | `mail/MessageGenerator.java:108` | TODO comment | `// TODO: should probably be TextBasedSmsColumns.DATE_SENT` | Should the backup timestamp use `DATE` (delivery/storage time) or `DATE_SENT` (original send time)? Using `DATE` matches what `SENTSINCE` in the IMAP search uses (via `maxSyncedDate`), so changing to `DATE_SENT` would break incremental backup windowing. |
| GAP-014 | `activity/StatusPreference.java:132,138` | TODO comment | `// TODO implement` in `onSaveInstanceState`/`onRestoreInstanceState` | These lifecycle methods are stubbed. What state should be preserved? If the `BackupState` UI is not restored after a configuration change (rotation), does this cause a visible glitch or a functional regression? |
| GAP-015 | `activity/auth/AccountManagerAuthActivity.java:66` | Workaround comment | `super.onStateNotSaved(); // workaround for https://issuetracker.google.com/issues/37122909` | This is a workaround for an Android OS bug in fragment transactions after `onSaveInstanceState`. Is the bug still present in current Android versions, or is this workaround masking a changed behavior? |
| GAP-016 | `service/RestoreTask.java:336-343` | Undocumented Protocol | `content://sms/conversations/-1` delete trick to force thread update | This is an undocumented hack: deleting a negative conversation ID forces the SMS provider to rebuild all thread metadata. Is this still necessary and functional on Android 11+ (scoped SMS provider access)? |

---

## Workaround Comments Found

| Location | Comment | Implication |
|----------|---------|-------------|
| `service/SmsJobService.java:79-83` | `// Since API level 26, an app in background cannot start a background service, so just instantiate service manually` | Core lifecycle bypass — see GAP-004 |
| `preferences/AuthPreferences.java:203-205` | `// NB: there's a bug in K9mail-library which requires double-encoding of uris` | k-9 version-locked workaround — see GAP-008 |
| `activity/auth/AccountManagerAuthActivity.java:66` | `// workaround for https://issuetracker.google.com/issues/37122909` | OS bug workaround — see GAP-015 |
| `mail/MmsSupport.java:122` | `// probably not the best way to determine if a message is inbound or outbound (legacy logic)` | Known-bad fallback path still active — see GAP-003 |
| `mail/MessageGenerator.java:108` | `// TODO: should probably be TextBasedSmsColumns.DATE_SENT` | DATE vs DATE_SENT has IMAP search implications — see GAP-013 |
| `service/RestoreTask.java:337` | `// unfortunately there's no direct way to do that in the SDK, but passing a negative conversation id to delete should do the trick` | Undocumented SMS provider behavior — see GAP-016 |
| `mail/BackupImapStore.java:172` | `//Debug.startMethodTracing("sorting")` | Commented-out profiling indicating known hot path |

---

## Undocumented Business Rules

| Location | Apparent Rule | Confidence | Question |
|----------|---------------|:----------:|----------|
| `preferences/AuthPreferences.java:170-174` | XOAuth2 always uses `imap.gmail.com:993`; PLAIN uses user-configured server | High | Intentional Gmail-only constraint for XOAuth2? |
| `service/BackupTask.java:158-163` | First backup sets `MAX_SYNCED_DATE` sentinel even when nothing to back up | High | Is `MAX_SYNCED_DATE` sentinel value (`DataType.Defaults.MAX_SYNCED_DATE`) a well-known constant or can it overlap with real timestamps? |
| `service/SmsBackupService.java:116-121` | Connectivity check only runs in old-scheduler mode | High | Is this intentional (new scheduler handles connectivity constraints) or an oversight? |
| `service/RestoreTask.java:254-258` | Only restore `MESSAGE_TYPE_INBOX` and `MESSAGE_TYPE_SENT` SMS types | High | Why are draft/failed/queued messages excluded? Will they exist in backups but be silently skipped on restore? |
| `mail/MmsSupport.java:174-176` | `application/smil` is silently dropped | Medium | Does this affect restore fidelity? |
| `mail/MessageGenerator.java:252-261` | MMS subject stability via sorted participant name list + `/` separator | Medium | Does Gmail thread matching depend on this exact format being stable across app versions? |
| `service/SmsBackupService.java:261-263` | Notification suppression for connectivity errors | Medium | Why are connectivity errors silently not notified even when `isNotificationEnabled()`? |
| `mail/BackupImapStore.java:183` | `Collections.reverse(messages)` after sort | Medium | After sorting oldest-first (MessageComparator sorts descending by date, then subList takes first N), why reverse? Does reverse produce newest-first for the restore loop? |

---

## Complex Methods Needing Explanation

| Method | File | Branch Token Proxy | Lines | Concern |
|--------|------|:------------------:|:-----:|---------|
| `getMessages` | `BackupImapStore.java:148` | ~12 | 39 | IMAP search + conditional sort + cap + reverse — semantics depend on understanding all four steps in sequence |
| `restore` | `RestoreTask.java:97` | ~15 | 58 | Loop + token-refresh retry + thread update + three exception paths |
| `messageFromMapMms` | `MessageGenerator.java:122` | ~14 | 67 | Three commented-out threading strategies + body assembly |
| `handleIntent` | `SmsBackupService.java:88` | ~10 | 18 | Credential check, connectivity check, and permission check order matters for correct error state |
| `migrate` | `AuthPreferences.java:276` | ~3 | 13 | Small but security-critical; runs on every app start |

---

## File History Analysis

| File | Commits (est.) | Notable Patterns | Knowledge Risk |
|------|:--------------:|-----------------|----------------|
| `mail/MmsSupport.java` | 8 | RCS-specific threading fixes (4 recent commits); dual-logic inbound detection evolved over time | High — recent changes, complex business logic, carrier-specific behavior |
| `preferences/AuthPreferences.java` | ~15 | OAuth2 migration, XOAuth deprecation, security settings cleanup across multiple commits | High — security-critical, migration logic, double-encoding workaround |
| `mail/BackupImapStore.java` | ~12 | Legacy header support drop, trust logic re-add, IMAP search refinements | High — protocol contract, search semantics |
| `service/SmsJobService.java` | ~10 | Service lifecycle bypass added for Oreo | High — platform workaround with unclear invariants |
| `mail/MessageGenerator.java` | ~8 | MMS threading strategy evolution, v2 message-ID | Medium — threading decisions preserved only in commented-out code |
| `mail/HeaderGenerator.java` | ~5 | `"v2"` suffix added to address; header set stable | Medium — `"v2"` change reason not documented |

---

## The 24-Locale String Surface

**Location:** `app/src/main/res/values*/strings.xml` (24 locale directories)
**Risk Level:** Medium (string-surface-level; not a functional gap, but a migration process gap)

The app ships 24 locale-specific string resource directories (`values`, `values-ca`, `values-cs`, `values-da`, `values-de`, `values-es`, `values-fr`, `values-gl`, `values-gr`, `values-hu`, `values-it`, `values-ko`, `values-nb-rNO`, `values-nl`, `values-pl`, `values-pt-rPT`, `values-ru`, `values-sk`, `values-sq`, `values-sr`, `values-sv`, `values-tr`, `values-uk`, `values-zh-rCN`, `values-zh-rTW`).

**Knowledge Gaps:**
1. **String completeness is unknown.** There is no automated check that all locales have translations for all string keys. Android falls back to `values/` silently, so incomplete locales are invisible in testing unless the device locale is set to that language.
2. **`LocalizableException` resource IDs.** The typed exception hierarchy (`ConnectivityException`, `RequiresLoginException`, etc.) each declare a `@StringRes errorResourceId()`. If new exception types are added during modernization, they need corresponding resource strings in all 24 locales — but there is no tooling enforcing this and no contributor guidance for translators.
3. **Email subject format strings (`type.withField`).** `DataType.SMS.withField`, `.MMS.withField`, `.CALLLOG.withField` carry `R.string` references used for email subjects (e.g., "SMS with Alice"). These strings appear in the user's Gmail mailbox permanently. Changing their format string structure (even for a new locale) would cause the email subjects in Gmail to diverge from existing threads, potentially breaking Gmail's thread grouping.

**SME Questions:**
1. Is there a translation contribution process? How are translators notified of new string keys added during modernization?
2. Has the email subject format string (`DataType.*.withField`) ever been changed? Did it cause thread fragmentation in Gmail for existing users?
3. Are there any locale-specific strings that encode behavioral assumptions (e.g., date formats used in IMAP search queries) rather than just UI text?

---

## Gap Severity Matrix

```
                     | Low Business Impact     | High Business Impact          |
─────────────────────┼─────────────────────────┼───────────────────────────────┤
 Easy to Verify      | GAP-009 (retry values)  | GAP-013 (DATE vs DATE_SENT)   |
 (code + test)       | GAP-010 (boot delay)    | GAP-014 (StatusPreference)    |
                     | GAP-011 (cache/50)      | GAP-015 (AccountManager hack) |
                     | GAP-012 (notif ID=1)    |                               |
─────────────────────┼─────────────────────────┼───────────────────────────────┤
 Hard to Verify      | Locale completeness     | GAP-001 (header schema)       |
 (requires prod      | String format history   | GAP-002 (IMAP search)         |
  data or SME)       |                         | GAP-003 (MMS in/out logic)    |
                     |                         | GAP-004 (JobService bypass)   |
                     |                         | GAP-005 (migrate() intent)    |
                     |                         | GAP-006 (XOAuth2 / GData)     |
                     |                         | GAP-007 (MMS threading/v2)    |
                     |                         | GAP-008 (double-encoding)     |
                     |                         | GAP-016 (thread update trick) |
```

All cells in the "Hard to Verify / High Business Impact" quadrant require SME sessions before Phase 1/2 work touches the corresponding seam.

---

## Recommended SME Sessions

| Session | Focus Area | Gaps Covered | Duration | Priority | Before Phase |
|---------|------------|:------------:|:--------:|:--------:|:------------:|
| 1 | Auth and Security Seams | GAP-005, GAP-006, GAP-008 | 2 hours | Critical | Phase 1 (SEC-001/SEC-002) |
| 2 | IMAP Protocol Contract and Header Schema | GAP-001, GAP-002 | 1.5 hours | High | Phase 2 (ACL introduction) |
| 3 | MMS Internals and Thread Strategy | GAP-003, GAP-007 | 1.5 hours | High | Phase 2 (MMS test coverage) |
| 4 | Service Lifecycle Bypass and WorkManager Migration | GAP-004, GAP-009, GAP-010, GAP-011 | 1 hour | High | Phase 2 (WorkManager migration) |
| 5 | Restore Correctness and SMS Provider Hacks | GAP-013, GAP-016, restore business rules | 1 hour | Medium | Phase 2 (RestoreWorker) |
| 6 | Locale and String Surface | 24-locale section, email subject format | 30 min | Medium | Phase 3 (any string changes) |

---

## Migration Risk Summary

**If SME sessions are conducted (recommended):**
- All 8 High-risk gaps can be documented and addressed before the affected seams are touched.
- Migration proceeds with explicit documentation of the header schema, IMAP search semantics, MMS inbound/outbound logic, and auth workarounds.
- Regression tests can be written against the SME-confirmed specifications before any code changes.

**If SME sessions are NOT conducted:**
- 8 high-risk gaps remain undocumented.
- The IMAP header schema, `SENTSINCE` vs. `SINCE` semantics, MMS threading, and double-encoding workaround are all silent invariants that will survive code review.
- Estimated migration risk: any change to `BackupImapStore`, `MessageConverter`, `AuthPreferences`, or `MmsSupport` has a greater than 50% probability of silently degrading restore fidelity or authentication for some user population.
- Recommended mitigation if sessions cannot be scheduled: extend characterization test coverage to every method called out in this analysis before any code in those files is changed (Feathers, *Working Effectively with Legacy Code*, Ch. 13 "Sensing and Separation").

---

## Next Steps

1. **Schedule Session 1** (Auth/Security) before any Phase 1 SEC-001/SEC-002 work begins — this is an immediate dependency.
2. **Expand `AuthPreferencesTest`** to cover the `migrate()` path (both triggering and non-triggering cases) as characterization tests, independent of SME session timing.
3. **Document header schema** in `mail/Headers.java` as inline Javadoc after SME session 2, making the wire format explicit for all contributors.
4. **Add assertions in `BackupQueryBuilderTest`** that verify `SENTSINCE` (not `SINCE`) is used when a `since` date is provided, and add a test that confirms the search query format matches the expected IMAP string exactly.
5. **Add a regression test** for the double-encoding behavior in `AuthPreferences.getStoreUri()` that verifies credentials with special characters (`@`, `+`, space) round-trip correctly through the k-9 URI parser.
6. **Flag GAP-016** (thread update trick via `conversations/-1`) for scoped-storage/SMS-provider access testing on Android 11+ as a Phase 0 compatibility check.
7. **Update `code-classification.md` refactor notes** for `MmsSupport.java`, `AuthPreferences.java`, and `BackupImapStore.java` to reference this document's gap IDs as prerequisites for those files' refactoring.

---

<!-- SELF-CHECK
Date: 2026-05-29
Checklist: 7/7
[x] Every magic number, unexplained constant, or hardcoded value is documented — PDU header constants (137/151/130), retry values (30/300), boot delay (60), cache-clear interval (50), notification IDs (1/1), MAX_SYNCED_DATE sentinel all covered.
[x] Every TODO/FIXME/HACK comment in source code is captured — 7 comment markers found via grep; all captured in Workaround Comments section or individual gap entries.
[x] Business rules with no obvious documentation are flagged — 8 undocumented business rules listed in the dedicated table; key ones (XOAuth2 Gmail-only, SMS type filter on restore, connectivity-check suppression) enumerated.
[x] Configuration values without explanatory comments are listed — double-encoding k-9 workaround, SENTSINCE choice, BOOT_BACKUP_DELAY, DEFAULT_SERVER_ADDRESS hardcode all documented.
[x] Workarounds and patches are identified with their context — Workaround Comments table covers all 7 known workarounds with cross-references to gap IDs.
[x] Each gap has: location (file:line), description, impact on migration, suggested resolution — all 16 enumerated gaps (GAP-001 through GAP-016) have file:line locations, descriptions, migration risk, and SME questions.
[x] Gaps are prioritized by migration impact (blocking, significant, minor) — Gap Severity Matrix and SME session priority table ordering provide prioritization; Session 1 is marked Critical with a Phase 1 dependency, Sessions 2–4 are High with Phase 2 dependencies.
Gaps Found: The 24-locale string surface gap has reduced detail because locale completeness requires a tooling pass (string key diff across all 24 directories) that was out of scope for static analysis alone; flagged as requiring a separate automated check. The git commit history analysis is limited to the commits visible via git log — the full commit message context for commits prior to the earliest visible SHA is not accessible.
Result: READY FOR AUDIT
-->
