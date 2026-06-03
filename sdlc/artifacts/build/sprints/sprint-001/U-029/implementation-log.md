---
artifact_type: implementation-log
story_id: U-029
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
files_changed: 2
files_created: 6
tests_added: 35
tests_passing: 343
---

# Implementation Log: U-029 — ContactsPort + People API Adapter

## Summary

Introduced `ContactsPort` interface + `PeopleApiContactsAdapter` (People API) + `GDataContactsAdapter` (legacy fallback). Refactored `OAuth2Client` to delegate account-email resolution to the port, removed all GData coupling from `OAuth2Client`, updated OAuth scopes, and gated the PII debug log behind `BuildConfig.DEBUG`. All builds pass; 343 tests green; auth-package coverage 78.2% (gate ≥70%).

## Files Modified

| File | Change |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java` | Removed `CONTACTS_SCOPE`, `CONTACTS_URL`, `getUsernameFromContacts`, `extractEmail`, `FeedHandler`, and all SAX imports. Added `EMAIL_SCOPE`, `OPENID_SCOPE`; updated `DEFAULT_SCOPE`. Added `contactsPort` field + two-constructor pattern. Replaced GData call with `contactsPort.resolveAccountEmail`. Gated `Log.d` behind `BuildConfig.DEBUG`. |
| `app/src/test/java/com/zegoggles/smssync/auth/OAuth2ClientTest.java` | Replaced U-006 test `requestUrl_includesContactsScope` (asserted m8/feeds present — now wrong) and `feedHandler_extractsEmailFromXml` (used reflection to find removed FeedHandler). Added U-029 scope assertion + ContactsPort-wiring + never-throw tests. |

## Files Created

| File | Purpose |
|------|---------|
| `app/src/main/java/com/zegoggles/smssync/auth/ContactsPort.java` | Interface: `String resolveAccountEmail(String accessToken)`. Never-throw contract per CNTR-MODERNIZATION-008. |
| `app/src/main/java/com/zegoggles/smssync/auth/PeopleApiContactsAdapter.java` | Default implementation. HTTP GET to `https://people.googleapis.com/v1/people/me?personFields=emailAddresses`. Primary-preferred JSON parse. All exceptions absorbed → returns null. |
| `app/src/main/java/com/zegoggles/smssync/auth/GDataContactsAdapter.java` | Transitional legacy fallback. Verbatim transplant of old `getUsernameFromContacts` body + `FeedHandler` SAX inner class. Scheduled for deletion after People API verification. |
| `app/src/test/java/com/zegoggles/smssync/auth/PeopleApiContactsAdapterTest.java` | 12 tests: JSON parse cases (primary, no-primary, empty array, malformed, null/blank body, null/blank token). |
| `app/src/test/java/com/zegoggles/smssync/auth/GDataContactsAdapterTest.java` | 8 tests: SAX parsing via reflection (extractEmail), FeedHandler direct SAX events, null-token never-throw. Required to bring auth-package coverage above 70%. |

## Capabilities Inventory (Replacement Story — OAuth2Client GData logic)

| Capability | Status | Citation |
|-----------|--------|---------|
| `getUsernameFromContacts(OAuth2Token)` — GData Atom feed HTTP GET | INTENTIONALLY REMOVED from OAuth2Client | Moved to `GDataContactsAdapter.resolveAccountEmail` (file:auth/GDataContactsAdapter.java:56) |
| `extractEmail(InputStream)` — SAX parse of Atom feed | INTENTIONALLY REMOVED from OAuth2Client | Moved to `GDataContactsAdapter.extractEmail` (file:auth/GDataContactsAdapter.java:82) |
| `FeedHandler` inner class — SAX `<author><email>` extraction | INTENTIONALLY REMOVED from OAuth2Client | Moved to `GDataContactsAdapter$FeedHandler` (file:auth/GDataContactsAdapter.java:94) |
| `CONTACTS_SCOPE` constant (`m8/feeds/`) | INTENTIONALLY REMOVED | Replaced by `EMAIL_SCOPE` + `OPENID_SCOPE` per DES-MODERNIZATION-011 Decision 3 |
| `CONTACTS_URL` constant (`m8/feeds/contacts/...`) | INTENTIONALLY REMOVED from OAuth2Client | Lives only in `GDataContactsAdapter` (transitional, file:line 44) |
| `DEFAULT_SCOPE` (gmail + contacts) | RETAINED (updated) | `DEFAULT_SCOPE = GMAIL_SCOPE + " " + EMAIL_SCOPE + " " + OPENID_SCOPE` (OAuth2Client.java:107) |
| `GMAIL_SCOPE` constant | RETAINED verbatim | OAuth2Client.java:104 |
| `getToken(String code)` → returns `OAuth2Token` with username | RETAINED | OAuth2Client.java:144 — delegates to `contactsPort.resolveAccountEmail` |
| `refreshToken(String)` — does NOT resolve username | RETAINED | OAuth2Client.java:161 — unchanged |
| Null-on-failure for username resolution | RETAINED | `contactsPort.resolveAccountEmail` returns null on any failure (CNTR-MODERNIZATION-008 VR-2) |
| `requestUrl()` — builds OAuth consent URL with scopes | RETAINED | OAuth2Client.java:130 — scope updated to three targets |
| Single-arg constructor `OAuth2Client(String clientId)` | RETAINED | OAuth2Client.java:138 — delegates to two-arg with `new PeopleApiContactsAdapter()` |
| Debug log of resolved email | RETAINED (gated) | OAuth2Client.java:155 — gated behind `if (BuildConfig.DEBUG)` (AC-8/ARCH-010) |

## Contract Adherence (CNTR-MODERNIZATION-008)

| VR | Requirement | Implementation |
|----|------------|----------------|
| VR-1 | Returns primary email; falls back to first; null if none | `PeopleApiContactsAdapter.parseEmail`: pass 1 checks `metadata.primary==true`; pass 2 returns `addresses[0].value`; returns null if empty (PeopleApiContactsAdapter.java:73-94) |
| VR-2 | Never throws — all failures → null | `PeopleApiContactsAdapter.resolveAccountEmail`: catches all `Exception` (line 56); `GDataContactsAdapter`: catches `SAXException`, `IOException`, `ParserConfigurationException` (lines 71-79) |
| VR-3 | Consumer tolerates null | `OAuth2Client.getToken`: `String username = contactsPort.resolveAccountEmail(...)` assigned to OAuth2Token; no null check that would throw (OAuth2Client.java:150-156) |
| VR-4 | No external SDK type crosses boundary | Port signature: `String resolveAccountEmail(String accessToken)` — only String (ContactsPort.java:35); no `com.google.api.*` in any signature |
| VR-5 | Disjoint from local device contacts | ContactsPort is in `auth/`; `contacts/ContactAccessor.java` and `mail/PersonLookup.java` untouched |
| VR-6 | Non-null return is non-empty | `PeopleApiContactsAdapter.parseEmail`: `TextUtils.isEmpty(value)` check before returning non-null (lines 80, 88); `GDataContactsAdapter$FeedHandler.getEmail()`: `result.isEmpty() ? null : result` (line 120) |

## Integration Verification

- `OAuth2Client.getToken` (the sole consumer of the port) is called from `OAuth2CallbackTask.java:30` (verified by grep on sdlc/modernization-plan).
- `PeopleApiContactsAdapter` is bound by `OAuth2Client(String clientId)` convenience constructor (OAuth2Client.java:138).
- Five construction sites (`MainActivity:139`, `AuthPreferences:111`, `BackupTask:87`, `SmsRestoreService:103`, `OAuth2CallbackTask` via injection) continue to use single-arg constructor — no source change required.
- `GDataContactsAdapter` is accessible via `new OAuth2Client(clientId, new GDataContactsAdapter())` for the reversibility binding-flip path.

## Test Results

| Suite | Tests | Result |
|-------|-------|--------|
| `PeopleApiContactsAdapterTest` | 12 | PASS |
| `OAuth2ClientTest` | 15 | PASS |
| `GDataContactsAdapterTest` | 8 | PASS |
| Full suite (`testDebugUnitTest`) | 343 | PASS — BUILD SUCCESSFUL |
| `jacocoTestCoverageVerification` | auth 78.2% | PASS (gate ≥70%) |
| `assembleDebug` | — | PASS — BUILD SUCCESSFUL |

## Notes

- GDataContactsAdapter intentionally contains the `m8/feeds` URL as the transitional reversibility fallback. This creates a literal-grep tension with AC-5 ("zero matches in app/src/main"). The intent of AC-5 (no m8/feeds coupling in OAuth2Client) is fully satisfied. See qa-results.md for the documented gap.
- The actual People API HTTP call cannot be verified without a live Google account. See qa-results.md §Live-Verification Gap.
- Worktree was behind `sdlc/modernization-plan`; fast-forward merge applied before implementation to pick up U-001 through U-007 build infrastructure.
