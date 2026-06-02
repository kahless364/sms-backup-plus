---
status: approved
artifact_type: interface-contract
consumers: []
related_requirements: []
related_design_docs: []
related_stories: []
change_records: []
id: CNTR-MODERNIZATION-008
title: ''
domain: modernization
contract_type: ''
producer: ''
---

# CNTR-MODERNIZATION-008: `ContactsPort` — Account-Email Resolution Seam

## Overview

This contract specifies `ContactsPort`, the app-owned service interface through which the
OAuth sign-in flow resolves the signed-in Google account's primary email address ("username")
from an access token. It is the binding seam that replaces the deprecated, inline GData
Contacts `m8/feeds/` call inside `auth/OAuth2Client.java` (DES-MODERNIZATION-011).

The producer side of the seam (the *adapter*) performs the remote resolution; the default
implementation, `PeopleApiContactsAdapter`, calls the Google People API
(`people.get` on `people/me`). The consumer side (`OAuth2Client.getToken`) calls the port,
maps the result onto `OAuth2Token.userName`, and must continue to return a valid token even
when resolution yields no email.

This contract governs **only** the remote account-username resolution boundary. It is
deliberately and explicitly disjoint from the **local device-contacts** path
(`contacts/ContactAccessor.java`, `mail/PersonLookup.java` via `ContentResolver` /
`ContactsContract`) — see [Validation Rules](#validation-rules), clause VR-5. The name
"Contacts" in `ContactsPort` is a historical artifact of the GData call it replaces; the
port resolves *the account's own email*, not device contact records.

Verified against source this session: `app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java`
(read lines 95–254) and `OAuth2Token.java` (read in full, 70 lines).

## Contract Boundary

| Side | Component | Responsibility |
|------|-----------|----------------|
| **Consumer** (calls the port) | `auth/OAuth2Client.java` — specifically `getToken(String code)` (verified `:139`/`:144`) | Invokes `resolveAccountEmail(accessToken)` after the token-endpoint exchange; assigns the result to `OAuth2Token.userName` (verified field name `userName`, `OAuth2Token.java:17`/`:147`); must not propagate any exception from the port. Injected per DES-MODERNIZATION-008 (Hilt) once the DI graph is stable, or via the convenience-constructor default before then (DES-MODERNIZATION-011 §Integration Design). |
| **Producer** (implements the port) | `PeopleApiContactsAdapter` (default — DES-MODERNIZATION-011 Decision 2); `GDataContactsAdapter` (transitional legacy fallback, deleted after People API verification) | Performs the remote lookup and returns the resolved email or `null`. Owns all transport, parsing, and SDK/library concerns entirely on its side of the boundary. |

The single current call site is `tasks/OAuth2CallbackTask.java:30` → `oauth2Client.getToken(code)`
(grep-verified in DES-MODERNIZATION-011 §Integration Design). The port has exactly one
consumer method and, at present, two adapters.

```
OAuth2Client.getToken(code)
        │  (token endpoint exchange → OAuth2Token: accessToken, refreshToken, …)
        ▼
   contactsPort.resolveAccountEmail(token.accessToken)   ◀── ContactsPort  [THIS CONTRACT]
        │
        ▼  (producer side — opaque to consumer)
   PeopleApiContactsAdapter | GDataContactsAdapter
        │
        ▼
   String email | null        ──►  OAuth2Token.userName  (may be null; best-effort)
```

## Contract Definition

### Interface

```java
package com.zegoggles.smssync.auth;   // co-located with OAuth2Client / OAuth2Token

/**
 * Resolves the signed-in Google account's primary email address ("username")
 * from an OAuth access token. The remote account-identity seam that replaces the
 * deprecated GData m8/feeds contacts call (DES-MODERNIZATION-011).
 *
 * Resolution is BEST-EFFORT: implementations return null on any failure and MUST
 * NOT throw into the caller (see contract clause VR-2). This interface is the only
 * type that crosses the boundary; no People API / GData SDK type may appear in its
 * signature (see clause VR-4).
 */
public interface ContactsPort {

    /**
     * Resolve the primary email address for the account that owns {@code accessToken}.
     *
     * @param accessToken a valid OAuth 2.0 bearer access token (the
     *                    {@code OAuth2Token.accessToken} value). Never null when
     *                    called from getToken; implementations treat null/blank as a
     *                    resolution failure and return null.
     * @return the account's primary email address, or {@code null} if resolution
     *         fails for ANY reason (network, HTTP error, auth/scope, empty result,
     *         or parse error). NEVER throws.
     */
    String resolveAccountEmail(String accessToken);
}
```

**Method:** `String resolveAccountEmail(String accessToken)` — single method; takes the
bearer access-token string; returns a non-empty email `String` on success or `null` on any
failure. Synchronous (mirrors the current synchronous `getUsernameFromContacts`, verified
`:212`); called off the main thread by `OAuth2CallbackTask`.

### Required OAuth scopes (precondition supplied by the consumer)

The producer cannot resolve the email unless the `accessToken` was minted under the correct
scopes. The scope set is owned by the consumer (`OAuth2Client.DEFAULT_SCOPE`, verified
`:105`) and is part of this contract's precondition:

| Scope | Value | Role |
|-------|-------|------|
| Email | `https://www.googleapis.com/auth/userinfo.email` | **Required** for People `people.get` to return the account's own email. |
| OpenID | `openid` | **Required** companion to `userinfo.email`. |
| Gmail | `https://mail.google.com/` | Preserved verbatim (backup/restore dependency, verified `GMAIL_SCOPE`:103). Not consumed by this port; listed because the token carries it. |

The deprecated `https://www.google.com/m8/feeds/` contacts scope (verified `CONTACTS_SCOPE`:104)
**must not** be requested. A token minted without `userinfo.email` (e.g. a refreshed token
from a pre-migration grant) is a valid input to this contract — the producer simply returns
`null`, honoring VR-2 (DES-MODERNIZATION-011 Migration note).

## Versioning

- **Current version:** v1.
- **Breaking change policy:** changing the method signature, return type, or the
  never-throw / null-on-failure semantics (VR-1, VR-2) is breaking and requires a contract
  revision plus re-approval. Introducing an external SDK type into the signature (VR-4) is
  breaking by definition.
- **Backward compatibility:** adapters may freely change their transport, endpoint, parsing
  strategy, library choices, and the scopes they need *internally* without a contract
  revision, provided the externally observable behavior (returns the primary email on
  success, `null` on failure, never throws, no SDK type crosses the boundary) is unchanged.
  Swapping `PeopleApiContactsAdapter` for `GDataContactsAdapter` is a compatible binding
  flip — that interchangeability is the contract's purpose (DES-MODERNIZATION-011 §Reversibility).

## Validation Rules

| ID | Rule | Owner |
|----|------|-------|
| **VR-1** | `resolveAccountEmail` returns the account's **primary** email when available; if no entry is flagged primary, the first available email; `null` if none. | Producer |
| **VR-2** | **Never-throw / null-on-failure.** The method MUST NOT throw for any reason — network, non-200 HTTP, auth/scope rejection, empty result, or parse error are all returned as `null`. This preserves the exact semantics of the replaced `getUsernameFromContacts`, which catches `SAXException`/`IOException`/`ParserConfigurationException` and returns `null` (verified `OAuth2Client.java:227–236`). | Producer |
| **VR-3** | The consumer MUST tolerate a `null` return: `getToken` continues to build and return a valid `OAuth2Token` with `userName == null` (verified the result flows to the `username` argument at `:147`). Email resolution failure is never a sign-in failure. | Consumer |
| **VR-4** | **No external SDK type crosses the boundary.** Inputs and outputs are plain `String`. No `com.google.api.*` People client type, no GData/Atom/SAX type, no `org.json` type may appear in the port signature or leak through it. Transport and parsing are wholly internal to the adapter. | Both |
| **VR-5** | This port is **disjoint from local device contacts.** It MUST NOT read `ContactsContract` / use `ContentResolver`, and the local path (`ContactAccessor`, `PersonLookup`) MUST NOT be routed through this port. They are unrelated seams (DES-MODERNIZATION-011 §Context, AC-5). | Both |
| **VR-6** | A non-`null` return is a non-empty, syntactically email-shaped `String` (the producer must not return empty string in lieu of `null`). | Producer |

## Error Handling

All failure modes collapse to a single observable outcome at the boundary: **return `null`**.
There is no error channel, no exception, and no error object across this seam — by design
(VR-2). Adapters SHOULD log the cause internally (the legacy path logs at `OAuth2Client.java:222`,
`:228`, `:231`, `:234`) but MUST NOT surface it to the consumer.

Caution: the consumer logs the resolved username at `OAuth2Client.java:145`
(`Log.d(TAG, "got token …, username="+username)`). The resolved value is account PII;
ARCH-010 / MU-010 require gating that log behind `BuildConfig.DEBUG` in the implementing
story (DES-MODERNIZATION-011 §Risks). This is a consumer-side logging concern, not a port
signature concern, but is noted here because the port's return value is the PII in question.

## Example Payloads

The port's own signature carries only `String`s. The example below shows the full round
trip, with the People API request/response that the **producer** (`PeopleApiContactsAdapter`)
performs internally — none of which crosses the boundary (VR-4).

**Consumer call (the only thing that crosses the boundary):**
```java
// inside OAuth2Client.getToken, after the token exchange:
String username = contactsPort.resolveAccountEmail(token.accessToken);
// username == "ada@example.com"   (success)
// username == null                (any failure — token still returned)
```

**Producer-internal HTTP (PeopleApiContactsAdapter — illustrative, not part of the boundary):**
```
GET https://people.googleapis.com/v1/people/me?personFields=emailAddresses
Authorization: Bearer ya29.a0Af…              (the accessToken arg)
```
```json
// 200 OK response body parsed internally with org.json:
{
  "resourceName": "people/c1234567890",
  "emailAddresses": [
    { "metadata": { "primary": true, "source": { "type": "ACCOUNT" } },
      "value": "ada@example.com" },
    { "metadata": { "primary": false }, "value": "ada.alt@example.com" }
  ]
}
// adapter selects metadata.primary == true → "ada@example.com"  (VR-1)
```

**Failure example (boundary view):**
```
GET people/me  → 401 (token lacks userinfo.email; stale pre-migration grant)
PeopleApiContactsAdapter logs the cause, returns null.        (VR-2)
OAuth2Client.getToken returns OAuth2Token{…, userName=null}.  (VR-3)
```

## Dependencies

- **DES-MODERNIZATION-011** — design that defines this port, the People API adapter, and the
  scope change (`auth/OAuth2Client.java` modifications).
- **REQ-MODERNIZATION-011** — requirement (AC-1 mandates the port; AC-4 the GData removal).
- **DES-MODERNIZATION-008 (Hilt)** — *soft* dependency: provides the injection mechanism for
  the adapter when the DI graph is stable. Not a precondition; the convenience-constructor
  default works without it (DES-MODERNIZATION-011 §Integration Design).
- External (not a project-owned contract): Google People API `people.get` on `people/me`
  with `personFields=emailAddresses`. Owned by Google; documented in DES-MODERNIZATION-011,
  not as a CNTR. It lives entirely on the producer's internal side of this boundary.

## Notes

NOT finalized — draft for create-contracts Step 6 review. All cited source facts
(`CONTACTS_SCOPE`:104, `DEFAULT_SCOPE`:105, `CONTACTS_URL`:107, `getToken`:139/144,
null-on-failure catches:227–236, `getUsernameFromContacts`:212, `OAuth2Token.userName`:17/147)
verified against source this session.
