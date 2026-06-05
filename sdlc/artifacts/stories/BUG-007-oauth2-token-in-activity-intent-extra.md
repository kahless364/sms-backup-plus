---
type: bug
status: ready
artifact_type: bug
severity: medium
priority: medium
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-029
platforms: []
tags: []
id: BUG-007
title: Raw OAuth2 token passed via Activity Intent extra (legacy AccountManager path)
domain: build
origin: code-review
---

# BUG-007: Raw OAuth2 token in Activity Intent extra (legacy path)

## Description
In the legacy AccountManager-based OAuth path, `AccountManagerAuthActivity.useToken()` puts the raw access token into an Intent extra (`EXTRA_TOKEN`) delivered to `MainActivity` via `setResult`. Intent extras are inspectable by privileged processes — needless exposure of a bearer token. The webflow OAuth path is clean.

## Steps to Reproduce
1. Use the AccountManager OAuth flow.
2. Inspect the result Intent extras returned to `MainActivity`.
3. Observe the raw access token present in `EXTRA_TOKEN`.

## Expected Behavior
The token is persisted directly (e.g., `authPreferences.setOauth2Token(...)`) inside `useToken()` before `setResult`, so it never travels in an Intent extra.

## Actual Behavior
`AccountManagerAuthActivity.java:148-153` places the raw token into the result Intent.

## Evidence
- `AccountManagerAuthActivity.java:148-153` (`useToken()` → `EXTRA_TOKEN`).
- Consolidated security review: `sdlc/artifacts/build/reviews/consolidated-2026-06/security-review.md` finding M-001.

## Acceptance Criteria
- [ ] AC-1: The raw OAuth2 token is no longer placed in any Intent extra; persisted via `AuthPreferences` inside `useToken()` before `setResult`.
- [ ] AC-2: Downstream behavior (account added / token stored) preserved for the AccountManager path.
- [ ] AC-3: Build green; relevant tests updated.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `AccountManagerAuthActivity.java:148-153` | raw token in result Intent extra | store via AuthPreferences before setResult; drop the token extra |

## Existing Behavior to Preserve
- AccountManager OAuth flow still results in a stored, usable token; webflow path unchanged.

## Verification Steps
1. Confirm no token extra in the result Intent; confirm token stored and usable.

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context
- Affects only the legacy AccountManager path. Low complexity. Backlog (security medium).

## Supporting Documentation
- `sdlc/artifacts/build/reviews/consolidated-2026-06/security-review.md` (M-001)

## Notes
Backlog (security medium) — not in the immediate B-1/B-2/B-3 fix batch.
