---
artifact_type: plan
story_id: U-039
verdict: PASS
agent: Developer
timestamp: 2026-06-04
---

# Plan: U-039 — Persist OAuth2 token directly, not via Intent extra

## Problem
`AccountManagerAuthActivity.useToken()` placed the raw OAuth2 access token in a result
Intent extra (`EXTRA_TOKEN`) delivered to `MainActivity`. Intent extras are inspectable by
privileged processes, creating unnecessary exposure of a bearer token (BUG-007 / M-001).

## Fix Summary
1. Add an `AuthPreferences` field to `AccountManagerAuthActivity`; initialize in `onCreate()`.
2. In `useToken()`, call `authPreferences.setOauth2Token(account.name, token, null)` BEFORE
   `setResult()` — token is now durable before the Activity result is delivered.
3. Remove `putExtra(EXTRA_TOKEN, token)` from the result Intent; keep only `EXTRA_ACCOUNT`.
4. Deprecate the `EXTRA_TOKEN` constant with a note explaining it is no longer populated.
5. Update `MainActivity.handleAccountManagerAuth()` to no longer read `EXTRA_TOKEN`; instead
   confirm the token is in `authPreferences` via `hasOAuth2Tokens()` before emitting
   `AccountAdded`.
6. Remove the now-unused `EXTRA_TOKEN` static import from `MainActivity`.

## Files to Modify
- `AccountManagerAuthActivity.java` — `useToken()` + `EXTRA_TOKEN` deprecation
- `MainActivity.java` — `handleAccountManagerAuth()` + import removal

## Tests
- Add U-039 tests to `AuthPreferencesTest` covering:
  - Token persisted via `setOauth2Token()` and readable via `getOauth2Token()`
  - `hasOAuth2Tokens()` returns true after direct persistence
  - Consumer path documents no Intent extra needed
  - Token usable for XOAUTH2 authentication downstream

## Constraints
- Webflow path (OAuth2WebAuthActivity) untouched.
- AccountManager flow downstream behavior (account added, token stored) preserved.
