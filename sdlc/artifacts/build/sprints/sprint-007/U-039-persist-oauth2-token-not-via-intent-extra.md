---
type: bug
status: done
artifact_type: user-story
priority: medium
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-039
title: Persist OAuth2 token directly instead of via Intent extra (fix BUG-007)
pipeline: ''
domain: modernization
requirement_source: bug:BUG-007
sprint: '000007'
updated_at: '2026-06-05T04:45:50.525Z'
resolution: done
---

# U-039: Persist OAuth2 token directly, not via Intent extra

## Story
As a security-conscious user, I want my OAuth2 access token to never travel in an Activity Intent extra, so that a privileged process cannot read it from the result Intent.

## Source
Fixes **BUG-007** (security, medium) — `sdlc/artifacts/stories/BUG-007-oauth2-token-in-activity-intent-extra.md`.

## Acceptance Criteria
1. AC-1: `AccountManagerAuthActivity.useToken()` persists the token via `AuthPreferences.setOauth2Token(...)` (or equivalent) BEFORE `setResult`; the raw token is no longer placed in any Intent extra (`EXTRA_TOKEN` removed/unused).
2. AC-2: The AccountManager OAuth flow still results in a stored, usable token and the same downstream "account added" behavior; the webflow path is unchanged.
3. AC-3: `grep` confirms no token value is put into an Intent extra on this path.
4. AC-4: Build green (assembleDebug, testDebugUnitTest, jacoco ≥70%); relevant tests updated.

## Technical Notes
- File: `AccountManagerAuthActivity.java:148-153`. Move persistence into `useToken()` ahead of `setResult`; drop the token extra. MainActivity's consumer of the token extra (if any) is updated to read from AuthPreferences instead. Low complexity, legacy AccountManager path only.

## Estimation Guidance
Low.
