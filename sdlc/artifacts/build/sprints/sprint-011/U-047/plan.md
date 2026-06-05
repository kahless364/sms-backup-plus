---
artifact_type: plan
story_id: U-047
verdict: PASS
agent: SolutionArchitect
timestamp: "2026-06-05"
---

# Plan: U-047 — Guard SNIHostName against IP-literal/empty IMAP host

## Root Cause

`DefaultTrustedSocketFactory.java` line 214 (after U-045):

```java
params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));
```

`SNIHostName(String)` throws `IllegalArgumentException` when `hostname` is:
- An IPv6 literal (contains `:`) — RFC 6066 §3 explicitly forbids SNI for IP literals
- An empty string
- Any string with non-LDH ASCII characters

On Android, the runtime also throws for IPv4 dotted-decimal. Per RFC 6066 §3, SNI MUST NOT
be sent for IP literals. The constructor does not protect against these inputs, causing an
unhandled crash for users whose IMAP server is addressed by IP.

## Approach

### Fix: `DefaultTrustedSocketFactory.setSniViaSSLParameters`

Two-layer guard in `setSniViaSSLParameters`:

1. **Explicit IP/blank check** (`isIpOrBlankHostname` helper): returns early (skip SNI)
   if hostname is null, blank, IPv4 (four dotted octets), or IPv6 (contains `:`).
   Pure syntactic check — no DNS resolution.

2. **Try/catch safety net**: wraps `new SNIHostName(hostname)` to catch any
   `IllegalArgumentException` for malformed hostnames that pass the explicit check.

DNS hostnames (e.g. `imap.gmail.com`) pass through to `setServerNames` unchanged.

### Files to change

| File | Change |
|------|--------|
| `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` | Add `isIpOrBlankHostname()` helper + guard in `setSniViaSSLParameters` |
| `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java` | Add 6 new @Test cases for DNS/IPv4/IPv6/empty/blank |

### Constraints preserved

- No ERROR-level log (BUG-012 fix retained; all new log calls at DEBUG)
- Change confined to vendored module + its test
- No DNS resolution in IP detection
- Gmail / DNS hostname behavior identical
