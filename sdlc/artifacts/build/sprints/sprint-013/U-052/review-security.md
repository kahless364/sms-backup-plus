---
artifact_type: review-security
story_id: "U-052"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-06-23"
blockers: 0
warnings: 1
---

# Security Review: U-052 — GreenMail IMAP Integration Tests; Remove Worker/Transport Coverage Exclusions

## Review Summary

U-052 introduces the GreenMail 2.1.3 embedded IMAP server as a `testImplementation`-only dependency and adds 26 integration tests covering `BackupImapStoreDelegate`, `BackupWorker`, and `RestoreWorker`. The primary security concern for this story is supply-chain integrity: did the new dependency land with proper SHA-256 verification, scoped only to test, and with no weakening of the dependency-verification gate? All three checks pass. The secondary concern — whether tests contain real credentials or weaken production security behavior — also passes. One low-severity informational warning is noted regarding `jersey-bom` being a transitive dependency (via GreenMail's Jakarta EE chain) that carries a POM-only entry with no runtime surface.

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

**[Low] `org.glassfish.jersey:jersey-bom:3.1.3` appears as a transitive POM-only dependency via GreenMail's Jakarta EE 9 chain.**

- File: `gradle/verification-metadata.xml` line 2789
- Nature: Jersey BOM (Bill of Materials) is a POM-only artifact; it declares no runtime classes and is consumed only as a Maven dependency-management BOM during Gradle resolution. It does not introduce any Jersey runtime code into the classpath.
- `jersey-bom` is entered in `verification-metadata.xml` with a specific SHA-256 on its POM file — it is not bypassed or trusted via wildcard.
- Jersey itself (the JAX-RS implementation) is NOT present in the resolved `testRuntimeClasspath`; only its BOM POM was pulled as a transitive metadata artifact during resolution.
- The SHA-256 value (`b7b821d85eb622aa8d2740081873c0401560e55daad41836ef2383984d80d201`) is recorded and verified.
- Assessment: Low severity. No runtime Jersey code is shipped or executed. The BOM POM is cryptographically pinned. Confirm via `./gradlew app:dependencies --configuration testRuntimeClasspath | grep jersey` at next CI run to document that no jersey JAR is on the runtime classpath.

### Security Checklist

- [x] No hardcoded credentials or secrets
- [x] GreenMail dependency scoped to `testImplementation` only — not shipped in the release APK
- [x] `verify-metadata=true` preserved in `gradle/verification-metadata.xml`
- [x] No `.*` wildcard trust rule added (only pre-existing `-javadoc.jar` and `-sources.jar` wildcards)
- [x] All new dependency artifacts have individual SHA-256 entries (no bypass)
- [x] Integration test credentials are synthetic/local-only
- [x] No production TLS, credential, or permission code changed
- [x] Error messages don't leak internal details
- [x] `verify-signatures=false` is pre-existing and unchanged (GPG signatures not enforced project-wide — acceptable posture)

## Detailed Findings

### Supply-Chain Verification Gate (Critical Check — PASS)

**Dependency scope:** `app/build.gradle` line 163 declares `testImplementation 'com.icegreen:greenmail-junit4:2.1.3'`. The `testImplementation` configuration is excluded from the release APK and AAB by the Android Gradle Plugin. GreenMail, Angus Mail (Jakarta Mail RI), jakarta.mail-api, jakarta.activation-api, and org.eclipse.angus artifacts are not present in any `implementation`, `api`, or `runtimeOnly` configuration. Confirmed by grepping `app/build.gradle` for all non-test configuration declarations of `greenmail` or `icegreen` — zero results.

**SHA-256 verification entries — all present, no wildcards:**

| Artifact | SHA-256 Entry |
|---|---|
| `com.icegreen:greenmail:2.1.3` (JAR + POM) | `3e445fa4...` / `115f6071...` |
| `com.icegreen:greenmail-junit4:2.1.3` (JAR + POM) | `bfe381c1...` / `28249...` |
| `com.icegreen:greenmail-parent:2.1.3` (POM) | `0233a3f7...` |
| `jakarta.activation-api:2.1.3` (JAR + POM) | `01b176d7...` / `b25499...` |
| `jakarta.mail-api:2.1.3` (JAR + POM) | `8051b58d...` / `febe763e...` |
| `org.eclipse.angus:all:2.0.3` (POM) | `128ca3b6...` |
| `org.eclipse.angus:angus-activation:2.0.2` (JAR + POM) | `6dd3bcff...` / `75e5621a...` |
| `org.eclipse.angus:angus-activation-project:2.0.2` (POM) | `af9188a1...` |
| `org.eclipse.angus:jakarta.mail:2.0.3` (JAR + POM) | `efb94642...` / `67996e64...` |
| `org.slf4j:slf4j-api:1.7.36` (JAR + POM) | `d3ef575e...` / `fb046a9c...` |
| `org.slf4j:slf4j-parent:1.7.36` (POM) | `bb388d37...` |
| `org.eclipse.ee4j:project:1.0.8` (POM) | `0d0c7b6e...` |
| `org.eclipse.ee4j:project:1.0.9` (POM) | `825379934...` |
| `org.glassfish.jersey:jersey-bom:3.1.3` (POM only) | `b7b821d8...` |

Every artifact introduced by GreenMail's transitive closure has a specific SHA-256 entry. No entry uses `<trust file=".*" />` or any regex pattern that would bypass verification for these artifacts. The existing two wildcard trust entries (`.*-javadoc[.]jar` and `.*-sources[.]jar`) are unchanged and pre-date this story; they apply only to documentation and source JARs, not to implementation JARs.

**`verify-metadata=true` preserved:** The `<verify-metadata>true</verify-metadata>` flag at line 4 of `gradle/verification-metadata.xml` is unchanged. The dependency verification gate remains active for all non-source/non-javadoc artifacts.

### Integration Test Credentials — Synthetic Only (PASS)

`BackupImapStoreDelegateIntegrationTest.java` uses:
- `TEST_USER = "testuser"`, `TEST_PASS = "testpass"` — synthetic values created in-JVM by `greenMail.setUser(...)` for the embedded GreenMail server only.
- `storeUri = "imap://PLAIN:testuser:testpass@127.0.0.1:<random-port>"` — loopback address, random port, no external server.

These are not real credentials. They exist only for the duration of the JUnit test method, inside a GreenMail in-JVM IMAP server that never binds to an externally accessible interface. They do not appear anywhere in production source (`app/src/main/`). No `gmail.com`, `yahoo.com`, production OAuth token, or Google API key appears in the new test files.

`BackupWorkerIntegrationTest.kt` and `RestoreWorkerIntegrationTest.kt` use `authPreferences.setImapUser("backup@test.local")` and `"test@test.local"` respectively — synthetic addresses used only to prevent NPE in `MessageConverter` (which requires a non-null IMAP username to construct an `Address` object). These do not represent real credentials.

### Production TLS/Auth/Permission Behavior — Unchanged (PASS)

The production code changes in this story are limited to:
1. Removal of three class-name patterns from `jacocoFileFilter` in `app/build.gradle` — has no effect on runtime behavior.
2. Comment and annotation updates in `app/build.gradle` rule blocks — purely documentary.

No changes were made to `K9MailTransport`, `AuthPreferences`, `PinnedCertificateSocketFactory`, `DefaultTrustedSocketFactory`, or any credential storage or TLS configuration path. REQ-MODERNIZATION-002 (TLS MITM elimination) and its implementations in prior sprints are not touched.

The GreenMail integration tests exercise the `BackupImapStoreDelegate` with `DefaultTrustedSocketFactory` (plain IMAP, no TLS) against a loopback-only server. This is the correct pattern for IMAP integration tests in a CI environment and does not imply cleartext IMAP is used in production; production connections use TLS via the existing `K9MailTransport` configuration.

### `@VisibleForTesting` Seam (PASS — inherited from U-053)

`appendBatchAndUpdateWatermark` is referenced by the U-052 integration tests via `BackupWorkerIntegrationTest`. The method is `internal` (Kotlin module visibility) with `@VisibleForTesting(otherwise = PRIVATE)`. This is the standard Android pattern for test seams — it is not `public`, does not leak outside the module, and carries no credential or sensitive data. The method body logs only `DataType` enum value and a Unix epoch timestamp (milliseconds), not credentials. See U-053 review for detailed seam analysis.

## Files Reviewed

- `app/build.gradle` (GreenMail dependency declaration, jacocoFileFilter removals, rule comment updates)
- `gradle/verification-metadata.xml` (all new SHA-256 entries)
- `app/src/test/java/com/zegoggles/smssync/mail/transport/BackupImapStoreDelegateIntegrationTest.java` (full)
- `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerIntegrationTest.kt` (credential scan, transport doubles)
- `app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerIntegrationTest.kt` (credential scan, transport doubles)
- `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` (seam visibility, log content)
- Implementation log: `sdlc/artifacts/build/sprints/sprint-013/U-052/implementation-log.md`

## REQ Traceability

- **REQ-MODERNIZATION-003 AC-8 (supply-chain verification):** GreenMail 2.1.3 and all 13 transitive artifacts are SHA-256 pinned in `verification-metadata.xml`. Gate preserved.
- **REQ-MODERNIZATION-002:** No TLS hardening regression. `DefaultTrustedSocketFactory` is used in the test; no `AllTrustedSocketFactory` or `InsecureX509TrustManager` is introduced.
- **REQ-MODERNIZATION-009 (mail ACL):** GreenMail types (`com.icegreen.*`) are confined to `BackupImapStoreDelegateIntegrationTest.java` in the `mail.transport` test package, which is the only package permitted to import k-9 types. No GreenMail import appears in any other test package.
