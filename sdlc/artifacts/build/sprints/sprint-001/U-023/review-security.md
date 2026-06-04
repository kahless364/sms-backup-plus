---
artifact_type: security-review
story_id: "U-023"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-04"
blockers: 0
warnings: 0
---

# Security Review: U-023

## Summary

This story is a pure dependency injection wiring change. It adds `@Inject` annotations to
constructors, moves manual `new` calls to factory methods, and refactors test setup. No new
data flows, network calls, cryptographic operations, permission requests, or secrets handling
is introduced.

## Security-Relevant Changes

### OAuth2Client construction (MainActivity, AuthPreferences)

The `new OAuth2Client(clientId)` calls in `MainActivity.onCreate()` and
`AuthPreferences.clearToken()` are unchanged in semantics — only the syntax changed from
short-name to fully-qualified class name. The `clientId` source (resource string
`R.string.oauth2_client_id`) is unchanged. No security regression.

### TokenRefresher construction (AuthPreferences)

`AuthPreferences.clearToken()` constructs `TokenRefresher` transiently for a token
invalidation call. The construction is now fully-qualified but semantically identical.
The token being invalidated comes from existing `SharedPreferences` (encrypted via
SecretStore from U-011/U-012). No new exposure.

### CalendarSyncer Lazy construction

`CalendarSyncer` is only instantiated when `preferences.isCallLogCalendarSyncEnabled()`
returns true. This runtime guard was preserved exactly. No change in when CalendarSyncer
is constructed or what data it accesses.

### No hardcoded credentials

No secrets, API keys, or credentials introduced. The `oauth2ClientId` continues to be
sourced from `R.string.oauth2_client_id` (build-time resource).

## Findings

No security blockers or warnings found.

## Phase Completion Report
---
story_id: "U-023"
phase: "security-review"
verdict: "PASS"
artifact_path: "C:/Code/Android/sms-backup-plus/.claude/worktrees/agent-a49fc73b335a851bf/sdlc/artifacts/build/sprints/sprint-001/U-023/review-security.md"
story_status: "in-progress"
current_build_phase: "validation"
blockers: 0
warnings: 0
errors: []
---
