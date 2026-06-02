---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-011
related_stories: []
related_design_docs: []
integration_contracts:
  - CNTR-MODERNIZATION-008
change_records: []
type: ''
id: DES-MODERNIZATION-011
title: ''
domain: modernization
---

# DES-MODERNIZATION-011: People API Contacts — `ContactsPort` & Account-Username Resolution

## Overview

This design replaces the single deprecated GData Contacts `m8/feeds/` call that
`auth/OAuth2Client.java` uses to resolve the signed-in Google account's email address,
moving it behind an app-owned `ContactsPort` whose default adapter calls the Google
**People API** (`people.get` on `people/me`). It also narrows the OAuth scope requested
during authorization from the soon-to-be-rejected `https://www.google.com/m8/feeds/`
contacts scope to the People-API-supported `…/auth/userinfo.email` scope (with
`openid`), while preserving the Gmail scope that backup/restore depends on. It traces
**REQ-MODERNIZATION-011** and implements **MU-010** and **target-state.md ADR-004**
(`ContactsPort` as one of the Hexagonal ports).

This is an **elective (M4 / Phase 3)** change. It touches a *convenience* path —
display of the account email after OAuth sign-in — not the core backup/restore engine.
It is deliberately scoped to a contained adapter swap plus a scope change, and is
reversible by binding the legacy adapter back (see Design Validation).

## Context

Verified against source this session — `auth/OAuth2Client.java` (read in full, 289 lines):

- The class declares two contacts couplings as constants:
  - `CONTACTS_SCOPE = "https://www.google.com/m8/feeds/"` (line 104), folded into
    `DEFAULT_SCOPE = GMAIL_SCOPE + " " + CONTACTS_SCOPE` (line 105) and sent on the
    authorization request via `requestUrl()` (line 133).
  - `CONTACTS_URL = "https://www.google.com/m8/feeds/contacts/default/thin?max-results=1"`
    (line 107), fetched by the private `getUsernameFromContacts(OAuth2Token)` (line 212).
- `getUsernameFromContacts` opens an `HttpsURLConnection` to `CONTACTS_URL` with the
  bearer access token, then SAX-parses the Atom feed via the inner `FeedHandler`
  (lines 247–288) to pull the `<author><email>` value.
- It is invoked from exactly one place: `getToken(String code)` at line 144, on the
  authorization-code exchange. The resolved email becomes the `username` field of the
  returned `OAuth2Token` (line 147) and is also written to the debug log at line 145
  (`Log.d(TAG, "got token " + … + ", username=" + username)`).
- `refreshToken(String)` (line 154) does **not** resolve the username — only the
  initial `getToken` does. This is important: the People API call is on the *sign-in*
  path only, not the per-refresh path.

The GData Contacts API is deprecated by Google and the `m8/feeds/` scope is being
withdrawn; new OAuth consent for that scope already fails for many configurations, so
this call is a **latent breakage** that silently degrades the post-sign-in display
(the account email comes back `null`, and the user simply sees no email).

The legacy local-contacts path is a **separate concern that this design does not
touch**. `contacts/ContactAccessor.java`, `contacts/ContactGroup*`, `contacts/Group.java`
and `mail/PersonLookup.java` resolve *device* contacts via `ContentResolver`
(`ContactsContract`) for message-sender display during backup. They have nothing to do
with the GData `m8/feeds/` network call and are explicitly preserved (MU-010
"preserved_local_lookup"; target-state.md §Data ownership). The name `ContactsPort` in
this design refers strictly to the **account-username / remote-contacts resolution**
seam in the auth flow, not to the local `ContactAccessor`.

---

## System Architecture

### Design Decision 1 — Introduce `ContactsPort` to abstract account/username resolution

**Anchor:** `DES-MODERNIZATION-011#contactsport-abstraction`

`OAuth2Client` today is both an OAuth client *and* a GData feed parser — the
`FeedHandler` inner class (42 lines of XML SAX handling) is contacts-API-specific
logic embedded in the auth client. We introduce an app-owned port to separate "exchange
an authorization code for tokens" (OAuth client responsibility) from "resolve the
account identity for a token" (contacts responsibility):

```
public interface ContactsPort {
    /**
     * Resolve the signed-in Google account's primary email address for the
     * given access token. Returns null if resolution fails for any reason
     * (network, auth, parse) — resolution is best-effort and MUST NOT throw
     * into the auth flow (preserves current getUsernameFromContacts semantics).
     */
    String resolveAccountEmail(String accessToken);
}
```

Two implementations, selected by binding:

| Adapter | Mechanism | Status |
|---------|-----------|--------|
| `PeopleApiContactsAdapter` | `GET https://people.googleapis.com/v1/people/me?personFields=emailAddresses` with `Authorization: Bearer <token>`; parse JSON `emailAddresses[].value` (prefer `metadata.primary == true`) | **New default** |
| `GDataContactsAdapter` | the existing `getUsernameFromContacts` body + `FeedHandler`, extracted verbatim | **Legacy fallback, behind a binding flip; deleted once People API is verified** |

`OAuth2Client.getToken` no longer parses contacts. It depends on `ContactsPort`:

```
String username = contactsPort.resolveAccountEmail(token.accessToken);
```

`getUsernameFromContacts`, `extractEmail`, `FeedHandler`, and `CONTACTS_URL` are
**removed from `OAuth2Client`** when the GData adapter is deleted (AC-1, AC-4). They
move into `GDataContactsAdapter` only transiently for the verification window.

**Behavioral contract preserved (verified against source):** `resolveAccountEmail`
returns `null` on any failure rather than throwing — `getToken` must continue to return
a valid `OAuth2Token` even when email resolution fails, exactly as today (the current
method catches `SAXException`/`IOException`/`ParserConfigurationException` and returns
`null`, lines 227–236). The People API adapter swaps SAX for JSON parsing and `IOException`
for the same null-on-failure discipline; no `com.google.api.*` SDK type crosses the port.

### Design Decision 2 — People API adapter replacing the GData call

**Anchor:** `DES-MODERNIZATION-011#people-api-adapter`

The People API replacement is intentionally **lightweight HTTPS + JSON**, mirroring the
existing class's hand-rolled `HttpsURLConnection` + manual-parse style (lines 176–188,
214–225) rather than pulling in the heavyweight `google-api-services-people` client SDK.
Rationale (trade-off explicit):

- The existing class already does raw `HttpsURLConnection` to Google endpoints and parses
  the body by hand. A single `people.get` call needs the same shape — one GET, one small
  JSON body. Adding the People client SDK (+ its `google-http-client`, `guava`,
  `google-oauth-client` transitive tree) for one field read is a Golden-Hammer cost the
  no-backend, single-maintainer app should not pay (target-state.md Technology Principle:
  no new mandatory surface; minimize deps).
- JSON parsing uses `org.json` (already on the Android platform) — no new dependency,
  parallel to the project's existing `OAuth2Token.fromJSON` (verified: `OAuth2Token`
  parses the token endpoint's JSON; the adapter follows the same idiom).

Endpoint and field selection:
- Request: `GET https://people.googleapis.com/v1/people/me?personFields=emailAddresses`
- Response: choose `emailAddresses[]` where `metadata.primary == true`; fall back to the
  first entry; return `null` if none.

This satisfies AC-2 (People API adapter implements the port; username/email resolution
works end-to-end).

### Design Decision 3 — OAuth scope update for the People API

**Anchor:** `DES-MODERNIZATION-011#oauth-scope-update`

`people.get` for the signed-in user's own email requires the
`https://www.googleapis.com/auth/userinfo.email` scope (plus `openid`); it does **not**
require — and must not request — the withdrawn `m8/feeds/` contacts scope. The change
to `OAuth2Client`:

| Constant (current) | Current value (verified) | Target |
|--------------------|--------------------------|--------|
| `CONTACTS_SCOPE` (line 104) | `https://www.google.com/m8/feeds/` | **remove** (replaced by the two below) |
| — | — | `EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email"` (new) |
| — | — | `OPENID_SCOPE = "openid"` (new) |
| `GMAIL_SCOPE` (line 103) | `https://mail.google.com/` | **unchanged** — backup/restore depends on it |
| `DEFAULT_SCOPE` (line 105) | `GMAIL_SCOPE + " " + CONTACTS_SCOPE` | `GMAIL_SCOPE + " " + EMAIL_SCOPE + " " + OPENID_SCOPE` |

`requestUrl()` (line 130) already injects `DEFAULT_SCOPE` into the `scope` query
parameter (line 133); changing the constant is sufficient — no call-site change there.
This satisfies AC-3 (scopes updated; authorization flow succeeds with the new scopes).

> **Migration note (existing users):** the access/refresh tokens of users who already
> authorized under the old scope set do not carry `userinfo.email`. Their first
> `getToken` after upgrade is a fresh authorization (new consent) and will carry the
> new scope. A refreshed token from an old grant will not be able to call People with
> `userinfo.email`; `resolveAccountEmail` returns `null` in that case (best-effort, no
> crash) until the user re-consents. Because email display is a convenience, this
> degradation is acceptable and matches the current null-on-failure behavior. This is
> called out for the implementing story; it is not a blocker.

### ADR — People API vs. continued GData

**Status:** Accepted (target). Implements target-state.md ADR-004 (`ContactsPort`).

**Context:** GData Contacts `m8/feeds/` is deprecated and its OAuth scope is being
withdrawn (endpoints.md "Google Contacts API"; REQ-MODERNIZATION-011 Context). The one
in-app consumer is account-email resolution after OAuth sign-in (`OAuth2Client.java:144`).

**Decision:** Resolve the account email via the supported People API (`people.get` on
`people/me`, `personFields=emailAddresses`) behind an app-owned `ContactsPort`; request
`userinfo.email` + `openid` instead of the `m8/feeds/` scope; keep `mail.google.com`.
Implement the adapter with raw `HttpsURLConnection` + `org.json` (no People client SDK).

**Alternatives rejected:**
- *Keep GData:* the endpoint and scope are being withdrawn — this is the latent breakage
  the requirement exists to remove.
- *Drop email resolution entirely:* the username is stored on `OAuth2Token` and shown to
  the user; removing it is a visible regression for no benefit.
- *Use Google Sign-In / GoogleSignInAccount for the email:* would change the entire auth
  mechanism (the app uses a hand-rolled installed-app OAuth flow, verified lines 130–209)
  — out of scope and far larger blast radius than warranted for one field.
- *Add the `google-api-services-people` SDK:* heavyweight transitive tree for a single
  field read; rejected per Technology Principle (minimize deps).

**Consequences:** (+) Removes the deprecated dependency and its withdrawn scope; isolates
future contacts change behind a port; no new library. (−) Email display degrades to
`null` for users on a stale grant until they re-consent (acceptable; best-effort).

### Traceability

| AC (REQ-MODERNIZATION-011) | Design element |
|---------------------------|----------------|
| AC-1 `ContactsPort` abstracts resolution; GData removed from `OAuth2Client` | Decision 1 (port + `OAuth2Client.getToken` depends on it; `getUsernameFromContacts`/`FeedHandler`/`CONTACTS_URL` removed) |
| AC-2 People API adapter; resolution end-to-end | Decision 2 (`PeopleApiContactsAdapter`) |
| AC-3 scopes updated; auth flow succeeds | Decision 3 (`DEFAULT_SCOPE` → `userinfo.email` + `openid`) |
| AC-4 no `m8/feeds/` reference remains (grep) | Decisions 1+3 (remove `CONTACTS_SCOPE`, `CONTACTS_URL`, `FeedHandler`) — see Design Validation grep |
| AC-5 contacts/calendar otherwise unaffected | Local `ContactAccessor`/`PersonLookup` untouched (preserved); calendar untouched; `GMAIL_SCOPE` unchanged |

---

## Integration Design

### Dependency on DES-MODERNIZATION-001 (SDK / AGP uplift)

REQ-MODERNIZATION-011 Constraints make this **depend on REQ-MODERNIZATION-001**
(SDK/AGP). The People API endpoint and modern TLS defaults assume the uplifted
`compileSdk`/`targetSdk 35` + AGP 8.x baseline (DES-001, MU-001 is the gate that blocks
this unit per migration-units.md Dependency Matrix: MU-010 depends on MU-007, which
depends on MU-005/006, all gated by MU-001). Practically: this change must land on the
post-uplift toolchain so `HttpsURLConnection` to `people.googleapis.com` runs against
the modern platform TLS stack and so `warningsAsErrors`/`-Werror` (DES-001) does not
trip on the removed/added code.

### Relationship to DES-MODERNIZATION-008 (Hilt) — soft, not hard

REQ-MODERNIZATION-011 "relates to REQ-MODERNIZATION-008 (Hilt) for adapter injection,
but does not hard-depend on it." Verified against `OAuth2Client.java`: the class is
constructed with a single `clientId` argument (`OAuth2Client(String clientId)`, line
123) — there is no DI container today. Two integration shapes, both valid:

- **Without Hilt (constructor injection, default if this lands before MU-007):** add a
  second constructor parameter `OAuth2Client(String clientId, ContactsPort contactsPort)`
  and a convenience `OAuth2Client(String clientId)` that defaults to
  `new PeopleApiContactsAdapter()`. This is the existing Humble-Object dual-constructor
  idiom the codebase already uses (target-state.md ADR-008 context) — no Hilt required.
- **With Hilt (if MU-007 has landed):** bind `ContactsPort → PeopleApiContactsAdapter`
  in a Hilt module and constructor-inject it; drop the convenience constructor. DES-008
  enumerates "+ the four ports (… Contacts/CalendarPort)" as injected collaborators, so
  this slots into that graph cleanly.

The design does **not** require Hilt to be present; the port boundary is what makes the
binding flip work either way. This matches MU-010's "injected via Hilt; eased once DI
graph stable" (an *easing*, not a precondition).

### Consumer integration points (scope)

`OAuth2Client`'s public surface that callers touch:
- `requestUrl()` — builds the authorization URL with `DEFAULT_SCOPE`. Changing the scope
  constant changes the consent screen; **no consumer signature changes.**
- `getToken(String code)` — returns an `OAuth2Token` whose `username` is the resolved
  email. Signature unchanged; behavior (null username on resolution failure) preserved.
- `refreshToken(String)` — unchanged; does not resolve email (verified line 154–163).

**Scope verified via grep this session** (`OAuth2Client`, `requestUrl`, `getToken`,
`getUsernameFromContacts` across `app/src`). The full consumer set, with the relevant
call surface:

| Consumer | What it calls | Affected by this change? |
|----------|---------------|--------------------------|
| `tasks/OAuth2CallbackTask.java:30` | `oauth2Client.getToken(code)` | **Yes — the sole `getToken` (username-resolution) call site.** Receives the `OAuth2Token` whose `username` is the resolved email. Behavior preserved (username may be null). |
| `activity/MainActivity.java:139` | `oauth2Client.requestUrl()` | **Yes — the sole `requestUrl` call site.** The consent screen reflects the new scopes; no signature change. |
| `activity/MainActivity.java:138` | `new OAuth2Client(clientId)` | Construction site — affected only if the constructor signature changes (see below). |
| `preferences/AuthPreferences.java:111` | `new OAuth2Client(...)` → `TokenRefresher(...).invalidateToken` | Construction site; uses refresh/invalidate path only — no username resolution. |
| `service/BackupTask.java:87` | `new OAuth2Client(...)` → `TokenRefresher` | Construction site; refresh path only. |
| `service/SmsRestoreService.java:103` | `new OAuth2Client(...)` → `TokenRefresher` | Construction site; refresh path only. |
| `auth/TokenRefresher.java:129` | `oauth2Client.refreshToken(...)` | Refresh path — **does not** resolve username (verified). Unaffected. |
| `activity/auth/RedirectReceiverActivity.java:37` | `OAuth2Client.REDIRECT_URL` (static) | Static constant read only. Unaffected. |

**Constructor-shape implication (verified):** `new OAuth2Client(...)` appears at **5
call sites** (MainActivity:138, AuthPreferences:111, BackupTask:87,
SmsRestoreService:103, and OAuth2CallbackTask holds an injected instance). If the
no-Hilt path adds a `ContactsPort` parameter, the convenience `OAuth2Client(String
clientId)` constructor **must be retained** so these 5 sites compile unchanged — only
the construction in the OAuth callback flow (the one that actually calls `getToken`)
needs the explicit adapter. This is why the design defaults to a convenience constructor
that self-supplies `new PeopleApiContactsAdapter()`: it keeps all 5 sites source-compatible.

### Existing OAuth contacts/calendar flow must keep working (AC-5)

- **Gmail backup/restore:** unaffected — `GMAIL_SCOPE = https://mail.google.com/` is
  retained verbatim in `DEFAULT_SCOPE` (verified line 103, 105).
- **Calendar sync:** `service/CalendarSyncer.java` uses the device calendar provider /
  Google account (endpoints.md) — it does not depend on the `m8/feeds/` scope or on
  `OAuth2Client`'s contacts call, so removing that scope does not affect it.
- **Local device contacts:** `contacts/ContactAccessor.java` + `mail/PersonLookup.java`
  use `ContentResolver`/`ContactsContract` (not OAuth, not GData) — untouched.

### Integration flow (target)

```
OAuth sign-in (authorization code)
        │
        ▼
OAuth2Client.getToken(code)
        │  POST token endpoint  → OAuth2Token (access/refresh)
        │
        ├─ contactsPort.resolveAccountEmail(token.accessToken)   ◀── ContactsPort (port)
        │        │
        │        ▼
        │   PeopleApiContactsAdapter (adapter)
        │        GET people.googleapis.com/v1/people/me?personFields=emailAddresses
        │        Authorization: Bearer <accessToken>
        │        parse JSON emailAddresses[] (primary preferred) → email | null
        │
        ▼
OAuth2Token(access, type, refresh, expires, username=email)   ← username may be null (best-effort)
```

---

## Design Validation

A reviewer/QA can confirm this design is correctly implemented by:

1. **`ContactsPort` in place + GData removed from `OAuth2Client` (AC-1, AC-4):**
   - `app/src/main/java/com/zegoggles/smssync/auth/ContactsPort.java` (or
     `contacts/ContactsPort.java`) exists and declares `String resolveAccountEmail(String accessToken)`.
   - `OAuth2Client.getToken` calls `contactsPort.resolveAccountEmail(...)` and no longer
     contains `getUsernameFromContacts`, `extractEmail`, the `FeedHandler` inner class,
     or the `CONTACTS_URL` constant.
   - **Grep gate (AC-4):** `grep -rn "m8/feeds" app/src/main` returns **zero** matches
     after the change (verified this session: the only production matches today are
     `OAuth2Client.java:104` and `:107`). **Important — scope the grep to `app/src/main`,
     not all of `app/src`:** `app/src/test/resources/contacts.xml` contains `m8/feeds`
     strings (the SAX fixture for the legacy feed) at lines 8–28. That fixture must be
     **deleted alongside the GData adapter** (it tests nothing once `FeedHandler` is gone);
     if retained transiently for the legacy `GDataContactsAdapter` verification window, it
     is exempt from the AC-4 production-source gate but must be removed before the legacy
     adapter is deleted. Documentation under `sdlc/` is out of scope for AC-4.
2. **People API resolves username end-to-end with the new scopes (AC-2):**
   - `PeopleApiContactsAdapter` issues `GET people.googleapis.com/v1/people/me?personFields=emailAddresses`
     with the bearer token and returns the primary email (or first, or `null`).
   - End-to-end check: a fresh OAuth sign-in yields an `OAuth2Token` whose `username` is
     the signed-in account's email; a forced failure (revoke/garbage token) yields
     `username == null` and `getToken` still returns a valid token (no throw) — pinning
     the preserved best-effort contract.
3. **Authorization succeeds with updated scopes (AC-3):**
   - `requestUrl()` emits a `scope` parameter containing `https://mail.google.com/`,
     `https://www.googleapis.com/auth/userinfo.email`, and `openid`, and **not**
     `https://www.google.com/m8/feeds/`.
   - Manual: the Google consent screen shows the new scopes and grants successfully.
4. **Calendar/contacts otherwise unaffected (AC-5):**
   - `contacts/ContactAccessor.java`, `contacts/ContactGroup*.java`, `contacts/Group.java`,
     `mail/PersonLookup.java`, and `service/CalendarSyncer.java` are unchanged by this
     story (git diff touches only `auth/` + the new port/adapter files).
   - `GMAIL_SCOPE` constant value is unchanged; backup/restore smoke test passes.
5. **Reversibility:** if People API verification fails in the field, re-binding
   `GDataContactsAdapter` (the extracted legacy body) restores prior behavior without
   touching `OAuth2Client.getToken`. The legacy adapter is deleted only after People API
   is confirmed (REQ Constraints: "Verify username resolution end-to-end with the new
   scopes before removing the GData path").

### Test coverage expectation

Verified this session: **there is no existing test for `OAuth2Client`** (migration-units
MU-002 lists `auth/OAuth2Client.java` as a *backfill target* — "NEW TEST: TD-003, no
test exists"). The contacts package has tests (`ContactAccessorTest.java`,
`ContactGroupsTest.java`) but those cover the unrelated local `ContactsContract` path.
The implementing story therefore must **add** tests:
- `ContactsPort` fake returning a known email / `null`, asserting `getToken` maps it onto
  `OAuth2Token.username` and never throws on `null`.
- `PeopleApiContactsAdapter` JSON-parse unit test (primary-preferred selection; empty
  `emailAddresses`; malformed body → `null`) using a fixture body, paralleling the
  existing `contacts.xml` test-resource idiom.
- `requestUrl()` assertion that the emitted `scope` contains the three target scopes and
  not `m8/feeds/`.

---

## Integration Contracts

> This design introduces one new cross-component boundary: the engine/auth layer calling
> a remote Google API (People) through an app-owned port. Per the contract gate, a
> CNTR-* artifact must be created before any story referencing this design is
> sprint-planned.

| Boundary | Producer | Consumer(s) | Contract Type | CNTR Artifact | Status |
|----------|----------|-------------|---------------|---------------|--------|
| `OAuth2Client` → `ContactsPort` (account-email resolution) | `OAuth2Client.getToken` | `PeopleApiContactsAdapter` (default), `GDataContactsAdapter` (legacy) | service (app-owned interface) | CNTR-MODERNIZATION-ContactsPort (needed) | needed |
| `PeopleApiContactsAdapter` → Google People API | adapter | `people.googleapis.com/v1/people/me` | api (external) | documented in this DES (no project-owned CNTR; external API) | n/a |

### Contracts Needed (pre-sprint gate)

- [ ] **CNTR needed:** `ContactsPort` service contract — method
  `String resolveAccountEmail(String accessToken)`; semantics: best-effort, returns
  `null` on any failure (network/auth/parse), **never throws** into the auth flow; no
  external SDK type crosses the boundary. This must be created via `/amp:create-contracts`
  and reach `approved` status before a story implementing this design is sprint-planned.

The external People API surface (`people.get`, `personFields=emailAddresses`,
`userinfo.email`+`openid` scopes) is owned by Google and is documented here rather than
as a project CNTR; the binding interface the project *does* own is `ContactsPort`, which
is the contract that gates this work.

## Trade-offs

- **Raw HTTPS + `org.json` vs. People client SDK:** chosen raw to avoid a heavy
  transitive dependency for one field read; cost is hand-written JSON parsing (mitigated
  by the existing `OAuth2Token.fromJSON` idiom and a focused parse test).
- **Port for a single call site:** introduces an interface + two adapters for one
  consumer. Justified by (a) the requirement mandating the port (AC-1), (b) reversibility
  during People-API verification, and (c) alignment with target-state.md ADR-004's
  Hexagonal boundary set.
- **Best-effort null on stale grants:** existing users on the old scope see `null` email
  until re-consent. Accepted because email display is a convenience and the prior code
  already returned `null` on failure.

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| People API requires a scope/consent the implementer mis-specifies, breaking sign-in | Medium | Medium | Verify the consent flow end-to-end on a real account before deleting the GData adapter (REQ Constraint); keep `GDataContactsAdapter` bound until confirmed |
| Removing `m8/feeds/` scope affects an undiscovered consumer | Low | Medium | Grep `app/src` for `m8/feeds`/`CONTACTS_SCOPE` at build time (only `OAuth2Client.java` matched this session); confirm no other reader |
| Existing users on stale grants see no email until re-consent | Medium | Low | Document one-time degradation; best-effort `null` preserves no-crash behavior |
| Account-email PII logged at `OAuth2Client.java:145` | Known (ARCH-010) | Low | MU-010 pairs this with gating the debug log behind `BuildConfig.DEBUG`; recommend the implementing story close ARCH-010 here (do not log the resolved email in release builds) |
| Lands before SDK uplift (DES-001) and trips `-Werror` / TLS defaults | Low | Medium | Sequence after MU-001 (gate) per migration-units Dependency Matrix |

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| REQ-MODERNIZATION-011 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-011-migrate-contacts-to-people-api.md | Requirement + 5 ACs governing this design |
| MU-010 (migration unit) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md | Unit scope: ContactsPort + PeopleApiContactsAdapter; preserved local lookup; dependencies |
| Target State (ADR-004) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/target-state.md | ContactsPort as Hexagonal port; integration architecture; constraints |
| Endpoints (engagement) | sdlc/artifacts/engagement/endpoints.md | GData m8/feeds deprecation; OAuth2 scopes; Gmail policy |
| DES-MODERNIZATION-001 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-001-restore-play-store-eligibility-design.md | SDK/AGP uplift dependency |
| DES-MODERNIZATION-008 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-008-hilt-di-design.md | Hilt port-injection relationship (soft dep) |
| DES-MODERNIZATION-009 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-009-mail-acl-k9-unpin-design.md | Reference for the port/ACL design idiom in this domain |
| OAuth2Client.java | app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java | Source being modified (read in full): CONTACTS_SCOPE:104, DEFAULT_SCOPE:105, CONTACTS_URL:107, requestUrl:130, getToken:139/144, getUsernameFromContacts:212, FeedHandler:247 |
| OAuth2Token.java | app/src/main/java/com/zegoggles/smssync/auth/OAuth2Token.java | username field carrier; fromJSON idiom |
| TokenRefresher.java | app/src/main/java/com/zegoggles/smssync/auth/TokenRefresher.java | Confirms refresh path does not resolve username |
| ContactAccessor.java | app/src/main/java/com/zegoggles/smssync/contacts/ContactAccessor.java | Local ContentResolver path — confirmed unrelated/preserved |
| PersonLookup.java | app/src/main/java/com/zegoggles/smssync/mail/PersonLookup.java | Local contact-name lookup — confirmed unrelated/preserved |
| OAuth2CallbackTask.java | app/src/main/java/com/zegoggles/smssync/tasks/OAuth2CallbackTask.java | Sole `getToken` call site (grep-verified) |
| MainActivity.java | app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java | Sole `requestUrl` + a `new OAuth2Client` call site (grep-verified) |
| contacts.xml (test fixture) | app/src/test/resources/contacts.xml | Legacy GData SAX feed fixture (m8/feeds at lines 8–28) — to be deleted with the GData adapter |

## Notes

Scope verified via grep this session. `OAuth2Client.java`, `OAuth2Token.java`,
`TokenRefresher.java`, and the `contacts/` package were read in full. The GData coupling
is confined to `OAuth2Client.java` (constants `CONTACTS_SCOPE`:104 / `CONTACTS_URL`:107,
private `getUsernameFromContacts`:212, inner `FeedHandler`:247). The only `getToken`
call site is `OAuth2CallbackTask.java:30`; the only `requestUrl` call site is
`MainActivity.java:139`; `new OAuth2Client` appears at 5 sites — all enumerated in
Integration Design. The `m8/feeds` string in production code is exactly the two
`OAuth2Client` constants (the other matches are in `app/src/test/resources/contacts.xml`,
a test fixture, and in sdlc/ documentation — both outside the AC-4 production-grep scope).
NOT finalized — draft for create-design Step 5 review.
