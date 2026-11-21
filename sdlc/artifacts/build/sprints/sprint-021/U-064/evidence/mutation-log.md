# U-064 mutation log

Protocol: U-064 plan section I4 (write-ahead entry, one edit with the Edit tool, one targeted run,
single-file checkout, hash and porcelain check, `restored` line). Paths are repository-relative.

Base commit: `de03e99b51a04d402b64cbd5c7f0d7d19e90f2a9`

| File | Base blob |
|------|-----------|
| `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` | `8a0fbb64fb171624591fbebafb818555f28efd58` |
| `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` | `d543927166b3c5e1da0aaa8ab584072a43bf7521` |

## Summary

| Id | AC | Failing tests / of | Verdict |
|----|----|--------------------|---------|
| M-N1 | AC-1 | 1 / 13 | DETECTED |
| M-N2 | AC-2, AC-3 | 2 / 19 | DETECTED |

Recovery note: if a session is lost while an entry says `APPLYING` and has no `restored` line, nothing else
runs (no gate, no review, no commit) until the file's hash has been compared with its base blob and, if it
differs, the file has been restored with `git checkout -- <file>`.

## M-N1 APPLYING 2026-11-21T03:03:27Z

- file: `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`
- hash-before: `8a0fbb64fb171624591fbebafb818555f28efd58` (equals base blob); `git diff --quiet HEAD -- app/src/main k9mail-vendored` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty
- edit: line 149 `enrolledCert.checkValidity();` replaced by `if (false) { enrolledCert.checkValidity(); }`
- class run: `PinnedCertificateSocketFactoryExpiryTest`, `PinnedCertificateSocketFactoryCreateSocketTest`, `PinnedCertificateSocketFactoryTest` in one invocation
- expected: 13 cases, 12 pass, 1 fail; the failing test is `handshake_expiredEnrolledCertificate_isRejectedAsExpired` (`java.lang.AssertionError`, "the handshake must be rejected, but it completed"); not a compile error, not a timeout
- run_exit: 1 (Gradle exit; a failing test is expected)
- cases: `evidence/mutations/M-N1-cases.txt` (`files=3 cases=13 pass=12 fail=1 error=0 skip=0`)
- failed: `PinnedCertificateSocketFactoryExpiryTest#handshake_expiredEnrolledCertificate_isRejectedAsExpired`, type `java.lang.AssertionError`, message "the handshake must be rejected, but it completed" (`evidence/mutations/M-N1-message.txt`)
- passed: the other 12 (4 CreateSocket, 8 trust-manager); compiled; no timeout
- expected: matches
- verdict: DETECTED
- restored: 2026-11-21T03:04:34Z hash-ok porcelain-empty (`git hash-object` = `8a0fbb64fb171624591fbebafb818555f28efd58`; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat HEAD -- app/src/main k9mail-vendored` empty)

## M-N2 APPLYING 2026-11-21T03:04:42Z

- file: `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`
- hash-before: `d543927166b3c5e1da0aaa8ab584072a43bf7521` (equals base blob); `git diff --quiet HEAD -- app/src/main k9mail-vendored` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty
- edit: lines 245 to 253 (the try/catch of `setSniViaSSLParameters`) replaced by its two body lines `params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));` and `socket.setSSLParameters(params);`; the guard at 239-243 and line 244 stay (same edit as U-063 M-A1c)
- class run: `DefaultTrustedSocketFactorySniTest` and `DefaultTrustedSocketFactoryJdkProviderTest` in one invocation
- expected: 19 cases, 17 pass, 2 fail; failing: `setSniViaSSLParameters_nameRejectedBySniHostName_doesNotThrowAndSkipsSNI` (Truth assertion, `escaped` lists all three names as `IllegalArgumentException`) and `createSocket_nameRejectedBySniHostName_doesNotThrow` (`java.lang.IllegalArgumentException`); the 12 existing SNI tests and the 5 JDK-provider tests pass
- run_exit: 1 (Gradle exit; failing tests are expected)
- cases: `evidence/mutations/M-N2-cases.txt` (`files=2 cases=19 pass=17 fail=2 error=0 skip=0`)
- failed: `DefaultTrustedSocketFactorySniTest#setSniViaSSLParameters_nameRejectedBySniHostName_doesNotThrowAndSkipsSNI` (`com.google.common.truth.AssertionErrorWithFacts`; the `escaped` value lists all three names, each with `java.lang.IllegalArgumentException`) and `DefaultTrustedSocketFactorySniTest#createSocket_nameRejectedBySniHostName_doesNotThrow` (`java.lang.IllegalArgumentException`) (`evidence/mutations/M-N2-message.txt`)
- passed: the 12 existing SNI tests and the 5 JDK-provider tests; compiled
- expected: matches
- verdict: DETECTED
- restored: 2026-11-21T03:05:43Z hash-ok porcelain-empty (`git hash-object` = `d543927166b3c5e1da0aaa8ab584072a43bf7521`; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat HEAD -- app/src/main k9mail-vendored` empty)
