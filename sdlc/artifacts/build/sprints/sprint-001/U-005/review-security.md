---
artifact_type: security-review
story_id: "U-005"
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# Security Review: U-005

## Summary

This story resolves one known CVE and introduces no new security surface.

## Findings

### RESOLVED: CVE-2020-15250 (JUnit 4.12 -> 4.13.2)
JUnit 4.12 had a potential information disclosure vulnerability (CVE-2020-15250) where
temporary files created during test execution could be read by other users on the same
system. Upgrading to 4.13.2 resolves this advisory. This affects only the test execution
environment, not the production APK.

### PASS: mockito-all removal
`mockito-all:1.10.17` was a 2015-era fat jar bundling old versions of ASM, ByteBuddy,
and Objenesis. Replacing with `mockito-core:5.14.2` pulls these as separate dependencies
at current versions, eliminating transitive vulnerabilities in the test classpath.
`mockito-all` does not ship in the production APK (testImplementation scope).

### PASS: No new production dependencies
All changes are in `testImplementation` scope. None of these dependencies are included
in the release APK. The production dependency graph is unchanged.

### PASS: MessageConverter.java charset fix
The `IOUtils.toString(is, StandardCharsets.UTF_8)` change is a correctness fix, not a
security change. No data validation or authentication logic is affected.

### PASS: Robolectric 4.12.2
Robolectric is a test framework dependency only. It is not included in the release APK.
Robolectric 4.12.x is the current stable series with active maintenance.

### PASS: Truth 1.4.4, auto-service 1.0
Test-only dependencies. No production surface.

## No security findings requiring remediation.
