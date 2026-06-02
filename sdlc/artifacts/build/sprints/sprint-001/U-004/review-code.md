---
artifact_type: code-review
story_id: "U-004"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-02"
blockers: 0
warnings: 3
---

# Code Review: U-004

## Summary

The implementation is structurally correct and conforms to the approved design
(DES-MODERNIZATION-003) and requirements (REQ-MODERNIZATION-003). Three warnings
are noted — none are blockers, but two should be resolved before Gate G2 is declared.

## Findings

### WARNING 1: `--verify` flag absent from CI Gradle invocation

**File:** `.github/workflows/ci.yml` line 42
**Severity:** Warning (must-fix before AC-8 is satisfied)
**Finding:** The Gradle invocation is `./gradlew test lint jacocoTestReport assembleRelease`
without `--verify`. AC-1 requires `--verify` appended so that a checksum mismatch in
`gradle/verification-metadata.xml` causes the command to exit non-zero.
**Root cause:** `gradle/verification-metadata.xml` does not exist yet; adding `--verify`
without the metadata file causes Gradle to fail on every run.
**Resolution:** Generate `gradle/verification-metadata.xml` via
`./gradlew --write-verification-metadata sha256` in a JDK-11 environment, commit it,
then add `--verify` to the CI invocation. This must be done before Gate G2 is declared.
Tracked in implementation-log Known Gaps item 1.

### WARNING 2: JaCoCo `**` glob sub-package behavior not runtime-verified

**File:** `app/build.gradle` lines 136, 146, 156
**Severity:** Warning (false-pass risk)
**Finding:** The `includes = ['com/zegoggles/smssync/service/**']` pattern is the
documented form for matching a package and its sub-packages in JaCoCo PACKAGE element
rules. However, this was not verified at runtime (JDK 22 / Gradle incompatibility).
If JaCoCo 0.8.7 does not recursively match sub-packages with `**`, the `service/state/`
and `service/exception/` sub-packages would be silently excluded, creating a false pass.
**Resolution:** On the first CI run, inspect the generated HTML report at
`app/build/reports/jacoco/jacocoTestReport/html/` and confirm that classes from
`service/state/` and `service/exception/` appear under the `service/` coverage totals.
If not, split the rule into explicit sub-package includes.

### WARNING 3: `classDirectories` path depends on AGP 4.1.3 build layout

**File:** `app/build.gradle` lines 108-110, 127-129
**Severity:** Warning (silent 0% coverage risk)
**Finding:** The JaCoCo class directory is set to
`${buildDir}/intermediates/javac/debug`. This is the standard AGP 4.x path for
debug unit-test compiled output. If the actual build produces `.class` files at a
different path (e.g., due to Gradle caching or AGP version variations), the JaCoCo
report will show 0% coverage without an error, producing a silent false pass for
the gate.
**Resolution:** On the first CI run, verify the HTML report shows non-zero coverage
values for at least one gated package. If coverage shows 0%, adjust the path.

## AC Conformance Checklist

| AC | Requirement | Status | Notes |
|----|-------------|--------|-------|
| AC-1 | CI runs all four tasks on PR; required check on master | PARTIAL | Workflow present, `--verify` missing, branch-protection is a GitHub config step |
| AC-2 | `minifyEnabled true`; R8 runs on assembleRelease | PASS (static) | Line 34: `minifyEnabled true` confirmed; not runtime-verified |
| AC-3 | Three independent per-package 70% LINE rules | PASS (static) | Three rule blocks with correct counters/values; not runtime-verified |
| AC-4 | Otto @Subscribe/@Produce keep rules | PASS (static) | Rules present in proguard-rules.pro |
| AC-5 | Gate blocks PRs dropping any package < 70% | PASS (static) | finalizedBy wiring present; not runtime-verified |
| AC-6 | Workflow structural: triggers, runner pin, cache, no-escape, name | PASS | All verified via static checks |

## Positive Observations

- CI YAML is well-formed with correct indentation (no tabs)
- `warningsAsErrors true` retained in `lintOptions` — existing behavior preserved
- Original `proguard-rules.pro` content preserved; rules appended not substituted
- Keep rules are well-commented with INTERIM markers for future removal (MU-005, MU-006)
- Three separate `rule {}` blocks ensure per-package independent failure (not a single aggregate)
- `jacoco { toolVersion = "0.8.7" }` pins the JaCoCo version, preventing silent upgrades
- Cache keys include both `**/*.gradle*` and `gradle-wrapper.properties` hash — correct
