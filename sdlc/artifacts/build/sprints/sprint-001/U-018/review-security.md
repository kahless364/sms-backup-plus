---
artifact_type: review-security
story_id: "U-018"
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# Security Review: U-018

## Summary

No new security-relevant code was introduced. The changes are deletions of provably-dead code and
implementation of an Android platform UI lifecycle method (`onSaveInstanceState`/
`onRestoreInstanceState`).

## Findings

**`SavedState` Parcelable:** The `SavedState` inner class serialises UI display values only
(`statusText`, `statusColor`, `detailsText`, `progress`, `max`, `indeterminate`, `iconKind`). None
of these fields carry credentials, tokens, account names, or sensitive data. The data is stored in
the standard Android instance-state bundle (same as any `View.onSaveInstanceState`), which is not
persisted to disk and is not accessible cross-process.

**`TokenRefresher.java` collapse:** Removing the dead `getAuthTokenPreApi14` method eliminates dead
code that used a deprecated 5-arg `AccountManager.getAuthToken` call. The live 6-arg form is the
correct and supported API. No security regression.

**Scope boundaries respected:** `AuthMode.XOAUTH` is untouched. No auth-related logic was modified
beyond removing the unreachable pre-14 dispatch.

No security issues identified.
