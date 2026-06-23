# Findings — Build & Toolchain (sms-backup-plus)

**Dimension:** Build & Toolchain · **Score: 87/100 · Grade: B+**
*Justification:* Current, supported core toolchain with dependency verification and a real coverage gate; held back only by deliberately deferred items (Kotlin 1.9/kapt/JVM-8, AGP-8 opt-outs, a 2015-vintage vendored mail library, an alpha crypto dep).

## Strengths
- **Current core toolchain** — AGP `8.7.3` (`app/build.gradle:7`), Gradle `8.14.1` (`gradle/wrapper/gradle-wrapper.properties:3`), JDK 17, `compileSdk`/`targetSdk` `35` (`app/build.gradle:21,25`). All supported.
- **Supply-chain hardening** — `gradle/verification-metadata.xml` with `verify-metadata=true` and ~478 pinned SHA-256 components; jcenter fully removed (repos = google/mavenCentral/jitpack).
- **Strict compiler/lint posture** — `warningsAsErrors true` (`app/build.gradle:65`), `-Werror -Xlint:unchecked` (`:185`); R8 shrinking on release (`:51`).
- **Real coverage gate** — `jacocoTestCoverageVerification` per-package LINE ≥70% with a documented false-green fix (`app/build.gradle:~321-361`).

## Weaknesses / Risks
- **BT-001 [Medium] Kotlin 1.9.25 + kapt + JVM target 8** (`app/build.gradle:7,90,112,184`; `gradle.properties:30`) — a generation behind Kotlin 2.x; kapt is maintenance-mode (KSP is the modern path); obsolete JVM-8 target survives only via `-Xlint:-options` suppression. Forward-compat fragility.
- **BT-002 [Medium] AGP-8 default opt-outs deferred** — `nonTransitiveRClass=false`, `nonFinalResIds=false`, `enableJetifier=true` (`gradle.properties:17,22-26`) re-enable legacy behavior removed/flipped in future AGP.
- **BT-003 [Medium] Vendored k-9 frozen at a 2015 commit** (`k9mail-vendored/build.gradle:41,68-74`) — `apache-mime4j 0.7.2`, `commons-io 2.4`, `org.apache.http.legacy`; unmaintained, no upstream security fixes. Largest unpatched-dependency surface.
- **BT-004 [Low] `security-crypto:1.1.0-alpha06`** (`app/build.gradle:137`) — alpha dependency on the credential-encryption critical path.
- **BT-005 [Low] Dead `jitpack.io` repo** still declared (`build.gradle:29`) after the only consumer (k-9) was vendored.
- **BT-006 [Low] CI does not explicitly enforce verification/coverage** — runs `jacocoTestReport` (gate finalizes off it) but no explicit `jacocoTestCoverageVerification` step or `--write-verification-metadata`; lint baseline can mask regressions.

## Improvement Items
1. Plan Kotlin 2.x + KSP migration; raise JVM target to 17 (removes obsolete-source suppressions + maintenance-mode kapt). *(L effort, med impact)*
2. Refresh/replace the vendored k-9 deps (`commons-io`, `apache-mime4j`, drop `org.apache.http.legacy`). *(M-L, med)*
3. Flip AGP-8 deferrals to defaults; drop `enableJetifier` + dead jitpack repo; move `security-crypto` off alpha. *(S-M, low-med)*
4. Add an explicit `jacocoTestCoverageVerification` step to CI. *(S, low)*
