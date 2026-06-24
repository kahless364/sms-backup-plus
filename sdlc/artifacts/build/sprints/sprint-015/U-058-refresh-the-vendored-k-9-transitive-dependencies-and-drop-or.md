---
type: story
status: done
artifact_type: user-story
priority: medium
complexity: high
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-058
title: Refresh the vendored k-9 transitive dependencies and drop org.apache.http.legacy
pipeline: ''
domain: modernization
resolution: done
requirement_source: assessment:20260623-post-migration-assessment#BT-003
sprint: '000015'
---

# U-058: Refresh the vendored k-9 transitive dependencies and drop org.apache.http.legacy

## Story
As a maintainer of the SMS Backup+ codebase, I want `commons-io` and `apache-mime4j` inside `:k9mail-vendored` bumped to maintained versions and the `org.apache.http.legacy` uses-library removed, so that the largest unpatched-dependency surface in the project receives upstream security fixes without re-tracking the upstream k-9 repository.

## Source
Derived from assessment 20260623-post-migration-assessment, finding BT-003. See `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/build-toolchain.md`.

## Acceptance Criteria
- [ ] AC-1: `k9mail-vendored/build.gradle:41` (and any companion version variable) is updated to declare `commons-io` at a maintained version (minimum `2.15.0` or the latest stable release at implementation time, current is `2.4`); the vendored jar or Maven coordinate is updated accordingly.
- [ ] AC-2: `k9mail-vendored/build.gradle:68-74` (apache-mime4j dependency block) is updated to declare `apache-mime4j` at a maintained version (minimum `0.8.x`, current is `0.7.2`); if multiple mime4j sub-artifacts are declared (`mime4j-core`, `mime4j-dom`, etc.) all must be bumped to the same new version.
- [ ] AC-3: `org.apache.http.legacy` is removed from the `:k9mail-vendored` module's `uses-library` declarations (or any `useLibrary 'org.apache.http.legacy'` in `build.gradle`); no code in `:k9mail-vendored` imports from `org.apache.http.*` packages that require the legacy library; any such imports are replaced with `java.net.*` or `okhttp`/`HttpURLConnection` equivalents if they exist in the vendored code, or the relevant k-9 source files are updated.
- [ ] AC-4: All mail transport tests pass: `./gradlew :app:testDebugUnitTest` — `K9MailTransportCreateOpenTest`, `K9MailTransportSslTest`, and all other `mail/**` tests continue to pass with the updated transitive deps; the ACL boundary (zero `com.fsck.k9.*` imports outside `K9MailTransport.java`) is unbroken.
- [ ] AC-5: Build green: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` — `gradle/verification-metadata.xml` is updated with SHA-256s for the new dependency versions; supply-chain verification passes.

## Affected Code
| File | Current Behavior | Change |
|------|-----------------|--------|
| `k9mail-vendored/build.gradle:41` | `commons-io:commons-io:2.4` (2012 vintage) | Bump to `2.15.x` or latest stable |
| `k9mail-vendored/build.gradle:68-74` | `apache-mime4j:0.7.2` (2015 vintage) | Bump to `0.8.x` or latest stable; update all sub-artifacts |
| `k9mail-vendored/build.gradle` (`useLibrary` or `uses-library` block) | `useLibrary 'org.apache.http.legacy'` present | Remove; ensure no k-9 source imports depend on it |
| `k9mail-vendored/src/main/java/...` (any `org.apache.http.*` imports) | Uses legacy Apache HTTP client classes requiring `org.apache.http.legacy` | Replace with `java.net.HttpURLConnection` or remove if unused |
| `gradle/verification-metadata.xml` | SHA-256 pinned for old dep versions | Update for new versions |

## Existing Behavior to Preserve
- MailTransport ACL boundary: zero `com.fsck.k9.*` imports in `service.*`; all k-9 types remain confined to `K9MailTransport.java`. Bumping deps inside `:k9mail-vendored` must not loosen this boundary.
- Do NOT re-track upstream k-9 — the module is pinned at a 2015 commit on purpose. Only the transitive Maven dependencies (`commons-io`, `apache-mime4j`) are updated; the vendored k-9 Java source files themselves are not replaced with upstream versions.
- All IMAP/SMTP transport behavior tested by `K9MailTransportCreateOpenTest` and related tests must survive; the `commons-io` and `mime4j` APIs used by the vendored code must remain compatible with the new versions (check for breaking API changes in the changelog before bumping).
- `BackupImapStoreDelegate` composition-based design (`K9MailTransport.java:414`) must be preserved unchanged.

## Verification Steps
1. Run `./gradlew :k9mail-vendored:dependencies | grep -E "commons-io|mime4j"` — confirm resolved versions are the new bumped versions with no fallback to `2.4`/`0.7.2`.
2. Run `./gradlew :app:assembleDebug` — no compilation errors from changed API signatures in `commons-io` or `mime4j`.
3. Run `./gradlew :app:testDebugUnitTest` — all `mail/**` tests pass.
4. Search for `org.apache.http.legacy` in all `build.gradle` files — expect zero results.
5. Search for `import org.apache.http.` in `:k9mail-vendored` sources — expect zero results.
6. Run `./gradlew :app:jacocoTestCoverageVerification` — gate passes with `mail*` package still at LINE ≥ 70%.

## Technical Context
- BT-003 root cause: the `:k9mail-vendored` module was created by pinning the k-9 source at a 2015 commit to eliminate the jitpack dependency. The transitive Maven deps (`commons-io 2.4`, `apache-mime4j 0.7.2`) were frozen at the same vintage; they have received security patches in subsequent years but the vendored module never picked them up.
- `commons-io 2.4` (2012) has had multiple CVEs fixed in later releases; `apache-mime4j 0.7.2` (2015) has had bugfixes and security-relevant updates in `0.8.x`. Neither is on a critical vulnerability path for this app's usage pattern, but both are unmaintained at their current versions.
- `org.apache.http.legacy` is a compatibility shim for the deprecated Apache HTTP client that was removed from Android in API 28; targeting API 35 with `useLibrary 'org.apache.http.legacy'` pulls in unnecessary legacy code. Removing it requires confirming no vendored k-9 source uses `org.apache.http.*` classes.
- This is an M-L effort primarily due to API compatibility checks; the actual code change may be small if `commons-io` and `mime4j` maintain backward-compatible APIs (which they do in most cases within major version).

## Notes
- Sprint D (Toolchain Currency). Medium-to-large effort.
- Check the `commons-io` 2.4 → 2.15 and `apache-mime4j` 0.7.2 → 0.8.x changelogs for breaking API changes before implementing; document any required source changes in the commit message.
- Do not attempt to re-vendor from upstream k-9 — only the Maven transitive deps are in scope.
