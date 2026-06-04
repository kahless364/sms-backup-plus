---
artifact_type: review-security
story_id: U-032
verdict: PASS
agent: Developer
timestamp: "2026-06-04T00:00:00Z"
---

# Security Review: U-032 — Complete Hilt Service Injection

## Summary

This story applies DI infrastructure changes only (Hilt annotations, field renames, shim removal,
test migration). No new network calls, data storage, authentication flows, or permission requests
are introduced. Security posture is unchanged.

## Findings

### No new attack surface

`@AndroidEntryPoint` annotation instructs Hilt to inject field dependencies at `onCreate()`. The
injected types (`Preferences`, `AuthPreferences`) are the same singleton instances previously
obtained via null-check fallback construction. No new components, services, or receivers are
exported. The AndroidManifest.xml is not modified by this story.

### Dependency injection doesn't widen scope

`ServiceBase.preferences` and `ServiceBase.authPreferences` are package-private fields. The Hilt
member injector sets these fields before `onCreate()` body runs. The field access modifier (package-
private) is appropriate — test code in the same package can set them in tests, production code
uses `getPreferences()`/`getAuthPreferences()` accessors.

### CalendarSyncer @Inject removal: no security impact

Removing `@Inject` from CalendarSyncer's constructor prevents an unintended Hilt-managed
CalendarSyncer lifecycle. CalendarSyncer is now exclusively manually constructed, which is the
intended pattern. No credentials or sensitive data flow through CalendarSyncer's constructor.

### No credentials, secrets, or sensitive data in changes

No hardcoded credentials, API keys, tokens, or PII are introduced. No new file I/O, network
operations, or IPC mechanisms are added.

### Test-only anonymous subclass: no production impact

The `@Override public void onCreate()` no-op override exists only in test source (`src/test/`).
It cannot be shipped to production and has no security impact.
