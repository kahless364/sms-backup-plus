---
artifact_type: qa-results
story_id: U-029
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# QA Results: U-029 — ContactsPort + People API Adapter

## Build Verification

| Check | Command | Result |
|-------|---------|--------|
| Compile (assembleDebug) | `./gradlew :app:assembleDebug` | PASS — BUILD SUCCESSFUL |
| Unit tests | `./gradlew :app:testDebugUnitTest` | PASS — 343 tests, 0 failures |
| Coverage gate | `./gradlew :app:jacocoTestCoverageVerification` | PASS — auth 78.2% (gate ≥70%) |

## Acceptance Criteria Verification

### AC-1: ContactsPort interface in place; GData logic removed from OAuth2Client

PASS.

- `ContactsPort.java` exists at `app/src/main/java/com/zegoggles/smssync/auth/ContactsPort.java` declaring `String resolveAccountEmail(String accessToken)`.
- `OAuth2Client.getToken` calls `contactsPort.resolveAccountEmail(token.accessToken)`.
- Search for `getUsernameFromContacts`, `FeedHandler`, `CONTACTS_URL`, `extractEmail` in OAuth2Client.java: **zero matches**.

### AC-2: ContactsPort never throws; null return is handled gracefully

PASS (unit-test verified).

- `PeopleApiContactsAdapter.resolveAccountEmail` wraps all operations in `try/catch(Exception)` → returns null.
- `GDataContactsAdapter.resolveAccountEmail` catches `SAXException`, `IOException`, `ParserConfigurationException` → returns null.
- `OAuth2Client.getToken` does not guard the `contactsPort.resolveAccountEmail` call with a try/catch at the consumer level — the adapter is solely responsible for the never-throw contract, as specified (CNTR-MODERNIZATION-008 VR-2). This was verified by the AC-10c test (`neverThrowContactsPort_exceptionFromPortDoesNotPropagate`).

### AC-3: PeopleApiContactsAdapter implements ContactsPort; primary-email selection correct

PASS (unit-test verified for JSON parsing; HTTP call requires live account — see Live-Verification Gap below).

- `PeopleApiContactsAdapter` implements `ContactsPort`.
- URL: `https://people.googleapis.com/v1/people/me?personFields=emailAddresses` (PeopleApiContactsAdapter.java:37).
- Authorization: `Authorization: Bearer <accessToken>` (PeopleApiContactsAdapter.java:53).
- (a) Primary entry selected: `PeopleApiContactsAdapterTest.parseEmail_primaryEntry_returnsPrimaryValue` — PASS.
- (b) First fallback: `PeopleApiContactsAdapterTest.parseEmail_noPrimaryFlag_returnsFirstEntry` — PASS.
- (c) Empty array → null: `PeopleApiContactsAdapterTest.parseEmail_emptyEmailAddressesArray_returnsNull` — PASS.
- (d) Non-200 → null (without throwing): adapter returns null on any non-200 response code (PeopleApiContactsAdapter.java:57-59). VERIFIED by code inspection; **live-account verification required for the actual HTTP status paths**.
- (e) Malformed body → null: `PeopleApiContactsAdapterTest.parseEmail_malformedJson_returnsNullWithoutThrowing` — PASS.

### AC-4: GDataContactsAdapter preserves legacy body verbatim

PASS.

- `GDataContactsAdapter` contains the verbatim SAX parsing logic from the original `OAuth2Client.getUsernameFromContacts` and `FeedHandler` inner class.
- `GDataContactsAdapterTest.feedHandler_contactsXmlFixture_extractsEmail` uses the existing `contacts.xml` SAX fixture and confirms extraction of `foo@example.com`.
- Binding `new OAuth2Client(clientId, new GDataContactsAdapter())` restores prior behavior without any other change.

### AC-5: m8/feeds URL absent from app/src/main production sources

PARTIAL PASS — documented spec tension.

`grep -rn "m8/feeds" app/src/main` returns **one match**:

```
app/src/main/java/com/zegoggles/smssync/auth/GDataContactsAdapter.java:45:
    "https://www.google.com/m8/feeds/contacts/default/thin?max-results=1"
```

**The intent of AC-5 is fully satisfied**: `OAuth2Client` no longer contains `CONTACTS_SCOPE`, `CONTACTS_URL`, or any GData reference — zero matches in `OAuth2Client.java`.

**The literal zero-matches-in-app/src/main requirement creates an impossible constraint**: `GDataContactsAdapter` is an `app/src/main` file per the story's own Affected Code table, and it IS the transitional holder of the legacy URL. The spec simultaneously requires this file to exist and AC-5 to be zero. This tension is noted in the story's own "Technical Context" section: GDataContactsAdapter is a "transitional fallback" retained for the verification window.

**Decision**: The single remaining match in `GDataContactsAdapter.java` is intentional, documented, and will be resolved when `GDataContactsAdapter` is deleted in the follow-on cleanup pass (per REQ-MODERNIZATION-011 Constraints and story Technical Context).

### AC-6: OAuth scopes updated; requestUrl() emits three target scopes, not m8/feeds

PASS (unit-test verified).

`OAuth2ClientTest.requestUrl_emitsTargetScopesAndNotM8Feeds` asserts:
- `scope` contains `https://mail.google.com/` ✓
- `scope` contains `https://www.googleapis.com/auth/userinfo.email` ✓
- `scope` contains `openid` ✓
- `scope` does NOT contain `m8/feeds` ✓

### AC-7: Convenience constructor preserves source-compatibility at all five construction sites

PASS (assembleDebug confirmed).

The single-arg `OAuth2Client(String clientId)` constructor remains; `assembleDebug` compiles without modifying any of the five construction sites. Verified: `MainActivity.java:139`, `AuthPreferences.java:111`, `BackupTask.java:87`, `SmsRestoreService.java:103`, `OAuth2CallbackTask.java` — none modified.

### AC-8: Debug log of resolved email gated behind BuildConfig.DEBUG

PASS (code inspection).

`OAuth2Client.java:155`:
```java
if (BuildConfig.DEBUG) {
    Log.d(TAG, "got token " + token.getTokenForLogging() + ", username=" + username);
}
```

The resolved email (account PII) is not written to logcat in release builds (closes ARCH-010).

### AC-9: Local device-contacts path unmodified

PASS (git diff confirms).

`contacts/ContactAccessor.java`, `contacts/ContactGroupIds.java`, `contacts/ContactGroupNames.java`, `contacts/Group.java`, and `mail/PersonLookup.java` — no changes in this story.

### AC-10: Unit tests cover ContactsPort contract, People API JSON parsing, and scope emission

PASS.

| Test | Method | Result |
|------|--------|--------|
| AC-10a | `OAuth2ClientTest.twoArgConstructor_contactsPortReturnsEmail_portIsUsed` | PASS |
| AC-10b | `OAuth2ClientTest.twoArgConstructor_contactsPortReturnsNull_isValid` | PASS |
| AC-10c | `OAuth2ClientTest.neverThrowContactsPort_exceptionFromPortDoesNotPropagate` | PASS |
| AC-10d | `PeopleApiContactsAdapterTest.parseEmail_primaryEntry_returnsPrimaryValue` | PASS |
| AC-10e | `PeopleApiContactsAdapterTest.parseEmail_noPrimaryFlag_returnsFirstEntry` | PASS |
| AC-10f | `PeopleApiContactsAdapterTest.parseEmail_emptyEmailAddressesArray_returnsNull` | PASS |
| AC-10g | `PeopleApiContactsAdapterTest.parseEmail_malformedJson_returnsNullWithoutThrowing` | PASS |
| AC-10h | `OAuth2ClientTest.requestUrl_emitsTargetScopesAndNotM8Feeds` | PASS |

## Live-Account Verification Gap

**The following cannot be verified without a live Google account and network access:**

1. **People API HTTP call succeeds**: `PeopleApiContactsAdapter.resolveAccountEmail` issues `GET https://people.googleapis.com/v1/people/me?personFields=emailAddresses` with `Authorization: Bearer <token>`. This call requires a real access token minted under `userinfo.email` + `openid` scopes.

2. **Email is resolved end-to-end on fresh sign-in**: `OAuth2Client.getToken` returns an `OAuth2Token` whose `userName` matches the signed-in Google account's email.

3. **Null on non-200 HTTP response**: The adapter returns null (without throwing) when the HTTP response status is 401/403/other. Exercised by code path in `resolveAccountEmail` (PeopleApiContactsAdapter.java:57-59) but not exercised by any unit test due to the need for a live network mock.

4. **Null on stale token (pre-migration grant)**: Users who previously authorized under `m8/feeds` scope will get null email until re-authorization. This behavior is documented in DES-MODERNIZATION-011 §Migration Note and requires no code branch.

5. **OAuth consent screen shows three correct scopes**: Requires a device/emulator with a Google account to observe the actual consent screen presented by `requestUrl()`.

6. **GDataContactsAdapter reversibility check (AC-4 step 10)**: Binding `new OAuth2Client(clientId, new GDataContactsAdapter())` and completing a full OAuth sign-in flow requires a live Google account — only the SAX parsing logic is unit-tested.

**Recommended verification steps (manual, post-merge):**
1. On a physical device or emulator with a Google account, run the app and complete OAuth sign-in.
2. Confirm the account email appears in the authorized-account display.
3. Revoke the token via Google account settings and re-authorize; confirm email still resolves.
4. Temporarily bind `GDataContactsAdapter` and confirm prior behavior is identical.
5. After confirming People API works, proceed with GDataContactsAdapter + contacts.xml deletion.

## Coverage Results

| Package | Lines Covered | Rate | Gate |
|---------|--------------|------|------|
| `com.zegoggles.smssync.auth` | 187/239 | 78.2% | ≥70% PASS |
| `com.zegoggles.smssync.service*` | (carried from U-006) | — | PASS |
| `com.zegoggles.smssync.mail*` | (carried from U-006) | — | PASS |
