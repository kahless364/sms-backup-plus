# U-058 Implementation Log — Refresh vendored k-9 deps (BT-003)
**Tier A achieved.** commit on worktree branch (merged).
## Dependency bumps (k9mail-vendored/build.gradle)
- commons-io 2.4 → **2.16.1** (no source change)
- apache-mime4j-core/dom 0.7.2 → **0.8.11**
## mime4j 0.8.x source adaptations (5 files)
- `Address.java`: `AddressBuilder` → `LenientAddressParser` (renamed); removed now-unthrown `MimeException` catch.
- `internet/EncoderUtil.java`: `CharsetUtil.{UTF_8,US_ASCII,ISO_8859_1}` → `java.nio.charset.StandardCharsets.*`.
- `internet/MimeMessage.java` + `message/MessageHeaderParser.java`: `new MimeConfig(); set*()` → `MimeConfig.custom().set*().build()` (now immutable).
- `internet/MimeUtility.java`: `QuotedPrintableInputStream.close()` no longer declares `throws IOException`.
## Dropping org.apache.http.legacy
- grep confirmed zero production references to webdav/`org.apache.http` from `app/`.
- Excluded the vendored `store/webdav/**` package + `WebDavTransport.java` from the source set; removed webdav branches in `Transport.java`/`RemoteStore.java`.
- `ssl/TrustManagerFactory.java`: replaced legacy `StrictHostnameVerifier` with `HttpsURLConnection.getDefaultHostnameVerifier()` + a `MinimalSslSession` shim supplying the real peer cert chain — same RFC 2818/6125 hostname verification (confirmed equivalent in the sprint-015 security review; conscrypt OkHostnameVerifier reads only getPeerCertificates()).
- Removed `useLibrary 'org.apache.http.legacy'`.
## Supply chain
- `gradle/verification-metadata.xml` updated with specific SHA-256 for the new commons-io/mime4j artifacts (no wildcard; verify-metadata=true).
## Result
`./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` — BUILD SUCCESSFUL. 697 tests pass. ACL boundary (0 `com.fsck.k9` imports in `service.*`) intact. No deferrals.
