---
id: REQ-MODERNIZATION-011
title: Migrate Contacts Resolution to the People API
artifact_type: requirement
domain: modernization
status: approved
type: functional
priority: low
epic: EPIC-MODERNIZATION-004
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-001
  - REQ-MODERNIZATION-008
related_stories: []
related_design_docs: []
traces_to: []
change_records: []
notes: |
  Traceability: deprecated GData Contacts m8/feeds endpoint (endpoints.md); migration unit MU-010; source assessment 20260529-modernization. Elective (M4).
---

# Migrate Contacts Resolution to the People API

## Description

Replace the deprecated GData Contacts `m8/feeds/` username-resolution call with the
Google People API, behind an app-owned `ContactsPort`, including the required OAuth
scope changes.

## Context

`auth/OAuth2Client.java` resolves the signed-in Google account's username/email via the
legacy GData Contacts feed (`https://www.google.com/m8/feeds/contacts/default/thin`).
The GData Contacts API is deprecated and may already be failing for new authorizations;
the supported replacement is the People API, which requires updated OAuth scopes. A
`ContactsPort` + adapter isolates the call so the GData→People swap is a contained
adapter change, and so the contacts integration can be injected (relates to Hilt,
REQ-MODERNIZATION-008).

## Acceptance Criteria

1. A `ContactsPort` abstracts contact/username resolution; the GData call is removed
   from `OAuth2Client.java`.
2. A People API adapter implements the port; username/email resolution works end-to-end.
3. OAuth scopes are updated to those required by the People API; the authorization flow
   succeeds with the new scopes.
4. No reference to the GData `m8/feeds/` endpoint remains (verified by grep).
5. Existing OAuth contacts/calendar functionality is otherwise unaffected.

## Rationale

The GData Contacts feed is deprecated and a latent breakage; moving to the People API
behind a port restores a supported integration and isolates future change. Elective
because it affects only the contact-name-resolution convenience path, not core
backup/restore.

## Constraints

- Depends on REQ-MODERNIZATION-001 (SDK/AGP) and the OAuth scope change required by the People API.
- Relates to REQ-MODERNIZATION-008 (Hilt) for adapter injection, but does not hard-depend on it.
- Verify username resolution end-to-end with the new scopes before removing the GData path.

## Notes

Boundary: `auth/OAuth2Client.java`, `contacts/` package, new `ContactsPort` + adapter.
Traceability: GData m8 deprecation (endpoints.md); MU-010; source assessment 20260529-modernization.
