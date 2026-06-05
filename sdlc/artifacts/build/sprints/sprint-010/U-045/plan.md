---
artifact_type: plan
story_id: U-045
verdict: PASS
agent: Developer
timestamp: "2026-06-05"
---

# Plan: U-045 — Quiet conscrypt SNI fallback in DefaultTrustedSocketFactory (fix BUG-012)

## Root Cause

`DefaultTrustedSocketFactory.setHostnameViaReflection` (line 201 in the original file) calls:

```java
socket.getClass().getMethod("setHostname", String.class).invoke(socket, hostname);
```

On Android API 35+ the conscrypt socket class is `com.android.org.conscrypt.Java8EngineSocket`, which
does not expose a `setHostname(String)` method. The catch block previously logged at `Log.e` (ERROR)
with the full exception, producing a `NoSuchMethodException` ERROR stack trace on every IMAP connect.

## Fix

Two-part change to `DefaultTrustedSocketFactory.java` in `:k9mail-vendored`:

1. **Primary SNI path (API 24+)**: In `setSniHost`, add a leading branch guarded by
   `Build.VERSION.SDK_INT >= Build.VERSION_CODES.N` that calls the new
   `setSniViaSSLParameters(socket, hostname)` helper, which uses the public
   `SSLSocket.getSSLParameters()` / `SSLParameters.setServerNames(Collections.singletonList(new SNIHostName(host)))` /
   `SSLSocket.setSSLParameters(params)` API. This is the supported path on API 24+ and
   eliminates any reflective call on modern platforms.

2. **Quiet reflective fallback**: The existing `setHostnameViaReflection` catch block is
   changed from `Log.e` to `Log.d` so that on platforms where the method is absent the
   failure is a DEBUG note, not an ERROR stack trace. The reflective path is still reached
   only on API < 17 where neither SSLCertificateSocketFactory nor the SSLParameters API
   is used; on API 17–23 the `SSLCertificateSocketFactory` path handles SNI; on API 24+
   the SSLParameters path is used.

## New imports added

- `javax.net.ssl.SNIHostName`
- `javax.net.ssl.SSLParameters`

## Method extracted for testability

`setSniViaSSLParameters(SSLSocket socket, String hostname)` — public static; allows unit tests
in the app module to mock an SSLSocket and assert correct `setSSLParameters` behavior without
requiring a real conscrypt stack.

## Test

`app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java`
(4 @Test methods):
- Verifies SSLParameters is set with the correct SNIHostName on the socket
- Verifies different hostname is propagated correctly
- Verifies the reflective fallback with an absent setHostname method does NOT throw
- Verifies the reflective fallback does NOT call setSSLParameters (no state change)
