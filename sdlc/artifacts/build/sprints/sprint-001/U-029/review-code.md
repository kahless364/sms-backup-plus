---
artifact_type: review-code
story_id: U-029
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# Code Review: U-029 — ContactsPort + People API Adapter

## Summary

Implementation is correct and follows project patterns. All ACs are satisfied at the code level. One spec tension (AC-5 literal grep vs. GDataContactsAdapter location) is documented rather than silently ignored.

## Review Findings

### ContactsPort.java

PASS. Interface is correctly defined with exactly one method per contract (CNTR-MODERNIZATION-008 §Contract Definition). Javadoc accurately describes the never-throw semantics and VR-4 (no SDK types cross the boundary).

### PeopleApiContactsAdapter.java

PASS. Uses `HttpsURLConnection` + `org.json` per DES-MODERNIZATION-011 Decision 2 (no People API SDK). `resolveAccountEmail` wraps all operations in a broad `catch(Exception)` which satisfies VR-2. `parseEmail` is package-private to allow direct unit testing of the parsing logic without requiring network. Two-pass primary-selection logic matches VR-1 exactly.

Minor note: `TextUtils.isEmpty(accessToken)` guard on entry returns null early for blank tokens (correct per VR-2), but the guard could also log a warning at WARNING level (it does). No issue.

### GDataContactsAdapter.java

PASS for its purpose. Is a verbatim transplant with appropriate deprecation notices. `FeedHandler.getEmail()` now returns `null` instead of empty string for the no-email case (the original returned `email.toString().trim()` which could be empty — this change improves VR-6 compliance and is correct). The CONTACTS_URL constant is the intentional m8/feeds URL holder for the verification window.

### OAuth2Client.java

PASS. All GData coupling removed (no `CONTACTS_SCOPE`, `CONTACTS_URL`, `getUsernameFromContacts`, `extractEmail`, `FeedHandler`). SAX imports removed. `BuildConfig.DEBUG` gate on the PII log at line 155 closes ARCH-010. Two-constructor pattern is clean and source-compatible. `contactsPort` null-guard in the two-arg constructor is correct.

### OAuth2ClientTest.java

PASS. The U-006 tests that were broken by the scope change (`requestUrl_includesContactsScope` asserted m8/feeds present; `feedHandler_extractsEmailFromXml` used reflection to find the removed FeedHandler) have been replaced with accurate tests. New U-029 tests cover scope assertion, port-wiring, and the never-throw VR-2 contract.

### PeopleApiContactsAdapterTest.java

PASS. All 12 test cases directly exercise `parseEmail` with JSON fixture strings, avoiding network. Covers primary selection (AC-10d), first-fallback (AC-10e), empty array (AC-10f), malformed/null/empty body (AC-10g), and null/blank token fast-path.

### GDataContactsAdapterTest.java

PASS. SAX parsing exercised via reflection on the package-private `extractEmail` method. FeedHandler inner class exercised by direct reflection-based SAX events. The `contacts.xml` fixture test includes a skip-guard if the fixture has been deleted (for when GDataContactsAdapter is eventually removed). 8 tests bring the auth-package coverage to 78.2%.

## Patterns Followed

- Raw `HttpsURLConnection` + `org.json`: follows the exact style of the existing `OAuth2Client` network code and `OAuth2Token.fromJSON`.
- `@RunWith(RobolectricTestRunner.class)`: consistent with all other test classes in the `auth` package.
- Truth assertions (`com.google.common.truth.Truth.assertThat`): consistent with test suite.
- No new library dependencies added.

## AC-5 Spec Tension

The single remaining `m8/feeds` occurrence in `GDataContactsAdapter.java:45` is documented as a known spec tension (not a defect). The story's Constraints section explicitly says "Verify username resolution end-to-end with the new scopes before removing the GData path" — GDataContactsAdapter must exist during this window. This file is scheduled for deletion per the story's Technical Context and must be deleted with contacts.xml in the follow-on cleanup pass.
