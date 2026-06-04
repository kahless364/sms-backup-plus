---
artifact_type: security-review
story_id: U-030
verdict: PASS
agent: Developer
timestamp: "2026-06-04"
---

# Security Review: U-030

## Summary

No security-relevant changes. This story is purely an exception-boundary narrowing: it removes k-9 type leakage from service.* without altering any authentication logic, credential handling, or TLS configuration.

## Analysis

### Exception wrapping

The `new MailException(e)` wrapping at both translation sites (constructor + converter) preserves the original `MessagingException` as `getCause()`. This is required for diagnostic output (`State.getDetailedErrorMessage()`) and does NOT expose k-9 internals to external callers — `MailException` is app-owned and `getCause()` is only used in internal error logging.

### Auth-escalation paths unchanged

The XOAuth2/AuthenticationFailed exception translation in K9MailTransport adapter methods (lines 152-247) is byte-for-byte unchanged. The subtype-first catch ordering is preserved. No security regression in auth flows.

### No credential exposure

No new logging of exception messages or causes was added. The existing diagnostic path via `State.getDetailedErrorMessage()` is unchanged.

## Issues

None.
