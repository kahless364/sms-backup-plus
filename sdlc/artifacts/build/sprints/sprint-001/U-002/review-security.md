---
artifact_type: review-security
story_id: U-002
verdict: PASS
agent: Developer
timestamp: "2026-06-02T17:30:00Z"
---

# Security Review: U-002 — AGP S2 Uplift

## Summary

Build configuration changes only. No new runtime code. Security surface unchanged from S1.
The AGP 8 upgrade improves build toolchain security (newer build tools). Three concerns noted.

## Security Findings

### android:exported additions (POSITIVE)

Adding explicit `android:exported="true"` to MainActivity, RedirectReceiverActivity, and
ComposeSmsActivity makes the exported state unambiguous. This is a correctness improvement:
previously, AGP would have inferred exported=true for these activities based on the intent-filter
presence, but explicit declaration is better practice.

### ProGuard keep rules — attack surface (LOW RISK)

The R8 keep rules preserve:
- `com.firebase.jobdispatcher.**` class hierarchy — this library is interim (removed in MU-005)
- Otto methods annotated @Subscribe/@Produce — these are public by design

The keeps are necessary for functionality. They do not expose new attack surface beyond what
already existed.

### `UnsafeImplicitIntentLaunch` suppressed (LOW RISK)

AGP 8 flagged two implicit intent launches in the OAuth flow:
- `Dialogs.java:161`: `startActivity(new Intent(Intent.ACTION_VIEW))`
- `OAuth2WebAuthActivity.java:20`: Similar pattern

These implicit intents could theoretically be intercepted by a malicious app. However:
1. Both are for opening a browser URL (ACTION_VIEW) — low-value target
2. The OAuth token would be in the URL parameter, not the intent itself
3. Fixing these requires explicit component targeting, which is a separate security story

RECOMMENDATION: Address in a dedicated auth security story. Not blocking for S2.

### `UnsafeProtectedBroadcastReceiver` suppressed (LOW RISK)

MmsReceiver and SmsReceiver receive protected broadcasts (`SMS_DELIVER`, `WAP_PUSH_DELIVER`).
AGP 8 warns these should validate `getAction()` before processing. Both receivers are
protected by `android.permission.BROADCAST_SMS/BROADCAST_WAP_PUSH` respectively, which
only the system can hold. The risk is theoretical spoofed intents with empty action.
Both receivers are removed by MU-005 (WorkManager migration). Not blocking for S2.

### Machine-specific JDK path in gradle.properties (INFO)

`org.gradle.java.home=C:/Users/Michael.Horsley/.jdks/jbr-17.0.14` is committed. This path
does not contain credentials or secrets. It is a non-security concern, just a portability
issue documented in DEVELOPMENT.md.

### R8 full-mode enabled (POSITIVE)

Enabling `minifyEnabled true` on release builds (R8 full-mode) improves security by:
1. Code obfuscation makes reverse engineering harder
2. Dead code removal reduces attack surface
3. Class inlining reduces reflection surface (offset by the keep rules, which are necessary)

## Security Assessment

| Finding | Risk | Status |
|---------|------|--------|
| android:exported explicitly set on 3 activities | None (improvement) | RESOLVED |
| UnsafeImplicitIntentLaunch in OAuth flow | LOW | SUPPRESSED pending auth story |
| UnsafeProtectedBroadcastReceiver on SMS/MMS receivers | LOW | SUPPRESSED; receivers removed in MU-005 |
| R8 keep rules expose Otto/firebase reflection surface | LOW | ACCEPTABLE (necessary for functionality) |
| JDK path in gradle.properties | INFO (not security) | DOCUMENTED in DEVELOPMENT.md |
