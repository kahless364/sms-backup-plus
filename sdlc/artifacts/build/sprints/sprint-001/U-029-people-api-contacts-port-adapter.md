---
type: story
status: done
sprint: "000001"
artifact_type: user-story
priority: low
complexity: medium
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-011
design_docs:
  - DES-MODERNIZATION-011
integration_contracts:
  - CNTR-MODERNIZATION-008
dependencies:
  - U-003
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-029
title: Replace deprecated GData contacts call with ContactsPort + People API adapter
pipeline: ''
domain: modernization
---

# U-029: Replace deprecated GData contacts call with ContactsPort + People API adapter

## Story

As a Google account holder using SMS Backup+,
I want the OAuth sign-in flow to resolve my account email address via the supported Google People API rather than the deprecated GData Contacts feed,
so that my account email is displayed correctly after sign-in and the app does not request a withdrawn OAuth scope that blocks authorization for new installs.

## Acceptance Criteria

- [ ] **AC-1: ContactsPort interface in place; GData logic removed from OAuth2Client.**
  Given the `app/src/main/java/com/zegoggles/smssync/auth/` package,
  when the story is merged,
  then a `ContactsPort.java` interface exists declaring exactly `String resolveAccountEmail(String accessToken)`, `OAuth2Client.getToken(String code)` delegates to `contactsPort.resolveAccountEmail(token.accessToken)` and assigns the result to `OAuth2Token.userName`, and `OAuth2Client` no longer contains any of the following: the `getUsernameFromContacts` method, the `extractEmail` method, the `FeedHandler` inner class, or the `CONTACTS_URL` constant.

- [ ] **AC-2: ContactsPort never throws; null return is handled gracefully.**
  Given a `ContactsPort` implementation that throws any `RuntimeException` or checked exception for any failure mode (network error, 401, 403, malformed JSON, empty `emailAddresses` array),
  when `OAuth2Client.getToken(String code)` calls `contactsPort.resolveAccountEmail(accessToken)`,
  then no exception propagates out of `getToken`; the method returns a valid `OAuth2Token` whose `userName` is `null`, and no crash or uncaught exception is reported in the test run — satisfying CNTR-MODERNIZATION-008 VR-2 and VR-3.

- [ ] **AC-3: PeopleApiContactsAdapter implements ContactsPort; primary-email selection is correct.**
  Given a valid bearer access token minted under `https://www.googleapis.com/auth/userinfo.email` and `openid` scopes,
  when `PeopleApiContactsAdapter.resolveAccountEmail(accessToken)` is invoked,
  then it issues `GET https://people.googleapis.com/v1/people/me?personFields=emailAddresses` with `Authorization: Bearer <accessToken>`, and:
    (a) returns the `value` of the `emailAddresses` entry where `metadata.primary == true` when one is present,
    (b) returns the first `emailAddresses[0].value` when no entry carries `metadata.primary == true`,
    (c) returns `null` when the `emailAddresses` array is absent or empty,
    (d) returns `null` (without throwing) when the HTTP response status is non-200,
    (e) returns `null` (without throwing) when the response body is not valid JSON or cannot be parsed — satisfying CNTR-MODERNIZATION-008 VR-1, VR-2, and VR-6 (a non-null return is never an empty string).

- [ ] **AC-4: GDataContactsAdapter preserves the legacy body verbatim as a reversibility fallback.**
  Given the `GDataContactsAdapter` class in the `auth/` package,
  when its `resolveAccountEmail(accessToken)` method is called,
  then it performs the same SAX-parse of the GData Atom feed that `getUsernameFromContacts` performed in `OAuth2Client` (same URL, same bearer header, same `<author><email>` extraction, same null-on-exception behavior), and binding `GDataContactsAdapter` in place of `PeopleApiContactsAdapter` causes `OAuth2Client.getToken` to resume the previous resolution behavior without any other change.

- [ ] **AC-5: The `m8/feeds` URL string is absent from `app/src/main` production sources.**
  Given the repository at the merged commit,
  when the command `grep -rn "m8/feeds" app/src/main` is executed,
  then it returns zero matches — satisfying REQ-MODERNIZATION-011 AC-4 and DES-MODERNIZATION-011 Design Validation clause 1.
  (The string may still appear transiently in `app/src/test/resources/contacts.xml` only during the People API verification window; that file must be deleted before `GDataContactsAdapter` is deleted in the follow-on cleanup pass.)

- [ ] **AC-6: OAuth scopes updated; `requestUrl()` emits the three target scopes and not the withdrawn scope.**
  Given a call to `OAuth2Client.requestUrl()`,
  when the returned URL string is inspected,
  then the `scope` query parameter contains `https://mail.google.com/`, `https://www.googleapis.com/auth/userinfo.email`, and `openid`, and does NOT contain `https://www.google.com/m8/feeds/` — satisfying REQ-MODERNIZATION-011 AC-3 and DES-MODERNIZATION-011 Decision 3.

- [ ] **AC-7: Convenience constructor preserves source-compatibility at all five construction sites.**
  Given the five `new OAuth2Client(...)` call sites (`activity/MainActivity.java`, `auth/preferences/AuthPreferences.java`, `service/BackupTask.java`, `service/SmsRestoreService.java`, and `tasks/OAuth2CallbackTask.java`),
  when the story is compiled with no modifications to any of those five files,
  then the project compiles without error — meaning `OAuth2Client(String clientId)` remains a valid single-argument constructor that self-supplies `new PeopleApiContactsAdapter()` as its `contactsPort`.

- [ ] **AC-8: Debug log of resolved account email is gated behind BuildConfig.DEBUG (closes ARCH-010).**
  Given the log statement at `auth/OAuth2Client.java:145` (`Log.d(TAG, "got token …, username=" + username)`),
  when the app is built in release configuration (`BuildConfig.DEBUG == false`),
  then the log statement is not executed — the resolved email (account PII) is not written to logcat in production builds — satisfying CNTR-MODERNIZATION-008 Error Handling note and ARCH-010.

- [ ] **AC-9: Local device-contacts path is unmodified.**
  Given the files `contacts/ContactAccessor.java`, `contacts/ContactGroupIds.java`, `contacts/ContactGroupNames.java`, `contacts/Group.java`, and `mail/PersonLookup.java`,
  when `git diff` is run against the story branch,
  then none of those files appear in the diff — satisfying REQ-MODERNIZATION-011 AC-5, DES-MODERNIZATION-011 AC-5, and CNTR-MODERNIZATION-008 VR-5.

- [ ] **AC-10: Unit tests cover the ContactsPort contract, People API JSON parsing, and scope emission.**
  Given the test suite under `app/src/test/`,
  when `./gradlew testDebugUnitTest` completes,
  then the following test cases pass:
    (a) A `FakeContactsPort` returning a known email causes `OAuth2Client.getToken` to set `OAuth2Token.userName` to that email and not throw,
    (b) A `FakeContactsPort` returning `null` causes `OAuth2Client.getToken` to return a valid `OAuth2Token` with `userName == null` and not throw,
    (c) A `FakeContactsPort` throwing `RuntimeException` causes `OAuth2Client.getToken` to return a valid `OAuth2Token` with `userName == null` and not throw,
    (d) `PeopleApiContactsAdapter` JSON parsing: primary-preferred selection (two entries, one primary — returns primary value),
    (e) `PeopleApiContactsAdapter` JSON parsing: no primary flag — returns first entry value,
    (f) `PeopleApiContactsAdapter` JSON parsing: empty `emailAddresses` array — returns `null`,
    (g) `PeopleApiContactsAdapter` JSON parsing: malformed/empty body — returns `null` without throwing,
    (h) `OAuth2Client.requestUrl()` scope assertion: emitted URL contains the three target scope tokens and does not contain `m8/feeds`.

### Integration Criteria

- [ ] **IC-1:** `ContactsPort.java` is a Java interface (not a class) in `app/src/main/java/com/zegoggles/smssync/auth/` declaring exactly one method: `String resolveAccountEmail(String accessToken)`.
- [ ] **IC-2:** `PeopleApiContactsAdapter` implements `ContactsPort` and is the adapter bound by `OAuth2Client(String clientId)` (the convenience constructor). No external People API SDK type appears in the class's public or package-visible signature.
- [ ] **IC-3:** `GDataContactsAdapter` implements `ContactsPort` and is bound by the two-argument `OAuth2Client(String clientId, ContactsPort contactsPort)` constructor when explicitly provided — confirming the binding-flip path is operational.
- [ ] **IC-4:** `OAuth2Client(String clientId, ContactsPort contactsPort)` is the primary constructor; `OAuth2Client(String clientId)` is a convenience constructor that delegates to it with `new PeopleApiContactsAdapter()`. All five construction sites in the existing codebase continue to call the single-argument form and compile without modification.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java` | Contains `CONTACTS_SCOPE` (line 104), `DEFAULT_SCOPE` (line 105), `CONTACTS_URL` (line 107), `getUsernameFromContacts(OAuth2Token)` (line 212), `extractEmail` helper, `FeedHandler` SAX inner class (lines 247-288), and single-arg constructor. Calls `getUsernameFromContacts` from `getToken` at line 144. Logs resolved email unconditionally at line 145. | Remove `CONTACTS_SCOPE`, `CONTACTS_URL`, `getUsernameFromContacts`, `extractEmail`, `FeedHandler`. Add `EMAIL_SCOPE`, `OPENID_SCOPE` constants. Update `DEFAULT_SCOPE`. Add `contactsPort` field. Add two-arg constructor; keep single-arg as convenience. Replace the `getUsernameFromContacts` call with `contactsPort.resolveAccountEmail(token.accessToken)`. Gate line-145 log behind `BuildConfig.DEBUG`. |
| `app/src/main/java/com/zegoggles/smssync/auth/ContactsPort.java` | Does not exist. | Create: `public interface ContactsPort { String resolveAccountEmail(String accessToken); }` |
| `app/src/main/java/com/zegoggles/smssync/auth/PeopleApiContactsAdapter.java` | Does not exist. | Create: implements `ContactsPort`. Raw `HttpsURLConnection` GET to `https://people.googleapis.com/v1/people/me?personFields=emailAddresses`. Parse `org.json`. Return primary email / first email / `null`. Catch all exceptions; return `null`; never throw. |
| `app/src/main/java/com/zegoggles/smssync/auth/GDataContactsAdapter.java` | Does not exist. | Create: implements `ContactsPort`. Verbatim transplant of existing `getUsernameFromContacts` body + `FeedHandler` inner SAX class from `OAuth2Client`. Transitional fallback — deleted after People API is verified end-to-end. |
| `app/src/test/resources/contacts.xml` | SAX test fixture for the legacy GData Atom feed (`m8/feeds` strings at lines 8-28). | Retained during the verification window. Deleted when `GDataContactsAdapter` is deleted (follow-on pass, not this story). |
| `app/src/test/java/.../auth/OAuth2ClientTest.java` | Does not exist (MU-002 TD-003: no test). | Create: ContactsPort fake tests (AC-10a/b/c), `requestUrl` scope assertion (AC-10h). |
| `app/src/test/java/.../auth/PeopleApiContactsAdapterTest.java` | Does not exist. | Create: JSON parsing unit tests (AC-10d/e/f/g) using a fixture body. |

## Existing Behavior to Preserve

- `OAuth2Client.getToken(String code)` returns a valid `OAuth2Token` even when account-email resolution fails; `OAuth2Token.userName` may be `null` — this null-on-failure semantic is the existing behavior from `getUsernameFromContacts` (catches `SAXException`/`IOException`/`ParserConfigurationException`, returns null, lines 227-236) and must be preserved exactly.
- `OAuth2Client.refreshToken(String refreshToken)` does not resolve the username — it must remain unchanged and must not call `contactsPort` (verified: refresh path, line 154, does not call `getUsernameFromContacts`).
- `OAuth2Client.requestUrl()` continues to include `GMAIL_SCOPE` (`https://mail.google.com/`) in the emitted `scope` parameter. This scope is required by backup/restore and must not be removed.
- `GMAIL_SCOPE` constant value `"https://mail.google.com/"` is unchanged.
- `contacts/ContactAccessor.java` and `mail/PersonLookup.java` (local device-contacts resolution via `ContentResolver`/`ContactsContract`) are untouched; they have no dependency on the GData call or the new port.
- All five `OAuth2Client` construction sites (`MainActivity`, `AuthPreferences`, `BackupTask`, `SmsRestoreService`, `OAuth2CallbackTask`) compile unchanged with the single-arg constructor.
- `TokenRefresher` behavior is fully preserved; it uses only `refreshToken` and `invalidateToken`, neither of which is modified.

## Verification Steps

1. **AC-5 grep gate:** From the repository root, run `grep -rn "m8/feeds" app/src/main`. Confirm zero output lines.
2. **AC-6 scope check (unit):** Run `./gradlew testDebugUnitTest`. Confirm test class `OAuth2ClientTest` passes the `requestUrl_emitsTargetScopesAndNotM8Feeds` test (or equivalent), which asserts the URL string contains `mail.google.com`, `userinfo.email`, and `openid`, and does not contain `m8/feeds`.
3. **AC-2 never-throw (unit):** Confirm the three `FakeContactsPort` test variants (returns email, returns null, throws) all pass — `getToken` returns a non-null `OAuth2Token` in all three cases and no exception escapes.
4. **AC-3 People API JSON parsing (unit):** Confirm all five `PeopleApiContactsAdapterTest` JSON-parse cases pass (primary selection, first-fallback, empty array, malformed body, null/blank token).
5. **AC-7 compilation:** Confirm the project compiles with `./gradlew assembleDebug` without modifications to `MainActivity.java`, `AuthPreferences.java`, `BackupTask.java`, `SmsRestoreService.java`, or `OAuth2CallbackTask.java`.
6. **AC-8 debug log gate:** Open `OAuth2Client.java` and verify the log statement at (formerly) line 145 is wrapped in `if (BuildConfig.DEBUG) { … }` or an equivalent conditional that is false in release builds.
7. **AC-1 structural check:** Confirm that searching `OAuth2Client.java` for `getUsernameFromContacts`, `FeedHandler`, `CONTACTS_URL`, and `extractEmail` yields zero results.
8. **AC-4 GDataContactsAdapter reversibility:** Instantiate `OAuth2Client` with `new OAuth2Client(clientId, new GDataContactsAdapter())` in a test; assert that `resolveAccountEmail` produces the same result as the old `getUsernameFromContacts` against the existing `contacts.xml` SAX fixture.
9. **AC-9 local contacts unchanged:** Run `git diff HEAD~1 -- app/src/main/java/com/zegoggles/smssync/contacts/ app/src/main/java/com/zegoggles/smssync/mail/PersonLookup.java` and confirm no output (no changes to those files).
10. **End-to-end (manual, pre-GDataContactsAdapter deletion):** On a physical device or emulator with a Google account, complete the OAuth sign-in flow. Confirm the account email appears in the app's authorized-account display. Revoke the token via Google account settings and re-authorize; confirm email still resolves. After confirming, bind `GDataContactsAdapter` instead and confirm the prior behavior is identical (reversibility check per REQ-MODERNIZATION-011 Constraints).

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java) | All implementation: `ContactsPort` interface, `PeopleApiContactsAdapter`, `GDataContactsAdapter`, `OAuth2Client` refactor, unit tests | Developer |

## Technical Context

- **Port boundary semantics (CNTR-MODERNIZATION-008):** `ContactsPort` is a synchronous, single-method interface. It is called off the main thread by `OAuth2CallbackTask`. The never-throw / null-on-failure contract (VR-2) mirrors the existing `getUsernameFromContacts` exception-absorption behavior exactly (lines 227-236 of the current `OAuth2Client.java`). Any deviation from this semantic — including returning an empty string instead of `null` (VR-6) or allowing `ContentResolver` access through this port (VR-5) — is a contract violation.
- **No new library dependency:** `PeopleApiContactsAdapter` uses `javax.net.ssl.HttpsURLConnection` (already used by `OAuth2Client`) and `org.json.JSONObject`/`JSONArray` (Android platform, already used by `OAuth2Token.fromJSON`). The `google-api-services-people` client SDK must NOT be added (DES-MODERNIZATION-011 Decision 2; Technology Principle: minimize deps).
- **Scope change impact on existing users:** Users with a token minted under the old scope set (including `m8/feeds/`) will not carry `userinfo.email` on their refresh token. When they re-authorize (fresh consent), they will receive the new scopes. Until then, `resolveAccountEmail` returns `null` on their first call, which is acceptable (best-effort, email display is a convenience). This is documented in DES-MODERNIZATION-011 §Migration Note and requires no code branch — the existing null-tolerance in `getToken` handles it.
- **Constructor injection shape (pre-Hilt):** This story implements the dual-constructor Humble-Object idiom: `OAuth2Client(String clientId, ContactsPort contactsPort)` is the testable form; `OAuth2Client(String clientId)` self-supplies `new PeopleApiContactsAdapter()` and exists solely to keep the 5 existing construction sites source-compatible. When U-022 (Hilt) is complete, the convenience constructor may be dropped in favour of `@Inject`-annotated constructor injection — but that transition is out of scope here.
- **GDataContactsAdapter is transitional:** It is created as a reversibility fallback per REQ-MODERNIZATION-011 Constraints ("Verify username resolution end-to-end with the new scopes before removing the GData path"). It must not be used in production code other than the explicit binding-flip path. It is scheduled for deletion alongside `contacts.xml` once end-to-end People API verification is confirmed — that deletion is a follow-on task, not part of this story.
- **ARCH-010 / PII log:** The log at `OAuth2Client.java:145` emits the resolved account email. This is PII. Gating it behind `BuildConfig.DEBUG` closes ARCH-010 and is explicitly called out in CNTR-MODERNIZATION-008 §Error Handling. This is a one-line change but must not be omitted.
- **People API primary-email selection:** The adapter must prefer the entry where `emailAddresses[N].metadata.primary == true`. If the JSON field `primary` is absent or `false` for all entries, fall back to index 0. If `emailAddresses` is absent or empty, return `null`. The returned string must be non-empty (a non-null return that is an empty string violates VR-6).
- **Dependency:** This story depends on U-003 (SDK/AGP uplift to `compileSdk`/`targetSdk 35` + AGP 8.x) because the `people.googleapis.com` call must run against the modern platform TLS stack, and the `warningsAsErrors`/`-Werror` flag introduced by DES-MODERNIZATION-001 must not trip on the new/removed code. Do not merge this story before U-003 is merged. The soft dependency on U-022 (Hilt) means if Hilt lands first, the convenience constructor can be omitted and replaced with `@Inject`; if it has not landed, the convenience constructor is required.

## Supporting Documentation

- REQ-MODERNIZATION-011 — requirement governing this story (5 ACs, all satisfied here)
- DES-MODERNIZATION-011 — design: `ContactsPort` abstraction (Decision 1), `PeopleApiContactsAdapter` (Decision 2), scope update (Decision 3), Integration Design (constructor shapes, 5 call sites, consumer table), Design Validation (grep gate, test expectations)
- CNTR-MODERNIZATION-008 — `ContactsPort` interface contract: VR-1 through VR-6, never-throw semantics, PII log note

## Integration Contract References

- CNTR-MODERNIZATION-008#contract-definition — `String resolveAccountEmail(String accessToken)` method signature, never-throw semantics (VR-2), null-on-failure (VR-2/VR-3), no external SDK type crossing the boundary (VR-4), disjoint from local device contacts (VR-5), non-empty non-null return (VR-6)
- CNTR-MODERNIZATION-008#validation-rules — full VR-1 through VR-6 checklist; all rules are verified by AC-2, AC-3, AC-9, and IC-2
- CNTR-MODERNIZATION-008#error-handling — adapter SHOULD log failure cause internally (not surface to consumer); consumer log of PII must be gated behind `BuildConfig.DEBUG` (closes ARCH-010)

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

NOT finalized — draft for create-stories Step 4 review. All source facts verified against `auth/OAuth2Client.java` (289 lines, read in full per DES-MODERNIZATION-011 §Artifacts Consulted): `CONTACTS_SCOPE`:104, `DEFAULT_SCOPE`:105, `CONTACTS_URL`:107, `getToken`:139/144, `getUsernameFromContacts`:212, `FeedHandler`:247-288, null-on-failure catches:227-236. Five `new OAuth2Client` construction sites enumerated per DES-MODERNIZATION-011 §Integration Design. Depends on U-003 (SDK/AGP); soft dependency on U-022 (Hilt injection — not a hard blocker).
