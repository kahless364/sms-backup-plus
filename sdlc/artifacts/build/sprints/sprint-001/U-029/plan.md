---
artifact_type: plan
story_id: U-029
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# Plan: U-029 — ContactsPort + People API Adapter

## Approach

Introduce `ContactsPort` as a synchronous, single-method, never-throw interface in the `auth/` package. Refactor `OAuth2Client` to delegate account-email resolution to the port rather than the inline GData Contacts SAX call. Wire in `PeopleApiContactsAdapter` as the default (People API via raw `HttpsURLConnection` + `org.json`) and `GDataContactsAdapter` as the transitional fallback.

## Sequence

1. Create `ContactsPort.java` interface — `String resolveAccountEmail(String accessToken)`.
2. Create `PeopleApiContactsAdapter.java` — HTTP GET to `people.googleapis.com/v1/people/me?personFields=emailAddresses`, parse primary email from JSON, never throw.
3. Create `GDataContactsAdapter.java` — verbatim transplant of `getUsernameFromContacts` body + `FeedHandler` SAX inner class from OAuth2Client; transitional fallback.
4. Refactor `OAuth2Client.java`:
   - Remove `CONTACTS_SCOPE`, `CONTACTS_URL`, `getUsernameFromContacts`, `extractEmail`, `FeedHandler`, and all SAX imports.
   - Add `EMAIL_SCOPE`, `OPENID_SCOPE` constants; update `DEFAULT_SCOPE`.
   - Add `contactsPort` field + two-constructor pattern (primary two-arg; convenience single-arg delegates to `new PeopleApiContactsAdapter()`).
   - Replace `getUsernameFromContacts(token)` call with `contactsPort.resolveAccountEmail(token.accessToken)`.
   - Gate `Log.d(TAG, "got token ..., username="+username)` behind `BuildConfig.DEBUG`.
5. Update `OAuth2ClientTest.java` (replace U-006 scope test that asserted m8/feeds was present; add U-029 tests per AC-10).
6. Create `PeopleApiContactsAdapterTest.java` — JSON parsing unit tests (AC-10d/e/f/g).
7. Create `GDataContactsAdapterTest.java` — SAX parsing tests via reflection; needed for auth-package 70% coverage gate.

## Key Decisions

- No new library dependency: raw `HttpsURLConnection` + Android platform `org.json` (mirrors existing OAuth2Client style).
- Two-constructor Humble Object idiom: keeps all 5 existing construction sites source-compatible.
- GDataContactsAdapter is transitional and intentionally contains the m8/feeds URL; it is the holding place for the legacy path and is scheduled for deletion after People API verification.
- The m8/feeds reference remaining in GDataContactsAdapter is a documented spec tension with AC-5 (see qa-results.md).
