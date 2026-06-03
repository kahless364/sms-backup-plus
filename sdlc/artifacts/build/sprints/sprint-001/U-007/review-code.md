---
artifact_type: code-review
story_id: "U-007"
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# Code Review: U-007

## Review Summary

The rewrite of `AuthPreferences.migrate()` is correct, minimal, and faithful to
DES-MODERNIZATION-002 Decision 6. All acceptance criteria are satisfied.

## AuthPreferences.java

### migrate() — Overall Structure

The method follows the exact structure specified in DES-MODERNIZATION-002 Decision 6:
1. `useXOAuth()` early return guard — correct, first statement
2. Local variable `protocol` read via `getServerProtocol()` — one read, no double-read bug
3. `wasLegacyDowngradeProtocol` flag — correct boolean, covers `+ssl` and `+tls`
4. Single editor open via `preferences.edit()`
5. Trust-all clearing branch gated on `getBoolean(SERVER_TRUST_ALL_CERTIFICATES, false)` — correct default
6. Protocol normalization branch — uses `protocol + "+"` (not `getServerProtocol() + "+"` as in old code, avoiding a second prefs read)
7. `edit.apply()` — correct, no `commit()` in the method body

### TRANSPORT_SECURITY_NOTICE_PENDING Constant

- Key is `"transport_security_notice_pending"` — matches DES-002 Technical Context exactly
- Visibility: package-private (`static final`, no access modifier) — appropriate; tests in same package can reference it without string literals
- Javadoc correctly describes the producer/consumer relationship and references the analogous `sms_default_package_change_seen` pattern at `Preferences.java:247-253`

### markTransportSecurityNoticePending() Helper

- `private static` — correct; operates only on the passed Editor parameter
- Called in both branches that set notice: stale-clearing branch and legacy-protocol branch
- Idempotent: if both conditions hold (stale true + legacy protocol), the second call writes the same value — no harm

### AC-7 Verification: No .commit() in migrate()

Grep confirmed zero `.commit()` calls in the rewritten `migrate()` body. The method uses `edit.apply()`. The other `.commit()` calls in the file (lines 98, 102, 105, 113, 118, 130, 134) are in other methods (`setOauth2Token`, `clearOauth2Data`, `setImapPassword`, `setImapUser`) which are out of scope for this story.

### Idempotency

The rewrite is idempotent:
- If `SERVER_TRUST_ALL_CERTIFICATES` was already `false` and protocol is not `+ssl`/`+tls`, `migrate()` calls `edit.apply()` with no writes (safe no-op)
- If the process is killed after `apply()` is queued but before flush completes, re-executing `migrate()` on the next launch produces the same result (ARCH-017 rationale satisfied)

## AuthPreferencesTest.java

### @Ignore Removal

`migrate_legacySslTls_doesNotEnableTrustAll` — `@Ignore` successfully removed. The test now runs and passes as the executable AC-1 acceptance criterion.

### Updated migrate_explicitTrustAll_isPreserved

The name is preserved (describes the INPUT condition: user had explicit trust_all=true). The assertion was correctly updated from `isTrue()` to `isFalse()` (stale value cleared) with the addition of a notice-pending assertion. The updated comment block explains the behavioral change from the characterization baseline.

### Test Coverage

17 test methods in `AuthPreferencesTest`:
- All branches of migrate() are covered
- Both legacy protocol values (+ssl, +tls) have dedicated tests
- Parametrized tests for non-legacy protocols and unaffected users
- Early-return (OAuth2) path tested
- Both individual conditions AND combined conditions tested for notice-pending

### assertWithMessage Usage

The parametrized test loops use `assertWithMessage("context...")` from Truth 1.4 instead of the deprecated `.named()` chain. This produces readable failure messages.

## Findings

No defects found. The implementation conforms to DES-MODERNIZATION-002 Decision 6 exactly.

The only `.commit()` calls remaining in AuthPreferences.java are in unrelated methods
(setOauth2Token, clearOauth2Data, setImapPassword, setImapUser) — these are outside U-007's
scope and are not regressions.
