---
artifact_type: plan
story_id: U-027
verdict: PASS
---

# U-027 Implementation Plan: k-9 Dependency Unpin

## Objective

Replace the JitPack SHA pin `com.github.jberkel.k-9:k9mail-library:eaf689025e` with a
reproducible coordinate, record checksums in `gradle/verification-metadata.xml`.

## Coordinate Evaluation (ADR-009-A Preference Order)

### Path A — mavenCentral() (checked first)

Searched `https://search.maven.org/solrsearch/select?q=g:app.k9mail` and
`g:com.fsck.k9`. Both returned `numFound: 0`. No k9mail-library artifact exists
on mavenCentral as of 2026-06-03. **Path A not available.**

### Path B — JitPack tagged coordinate (checked second)

- Ran `git ls-remote --tags https://github.com/jberkel/k-9`
- Commit `eaf689025ec28d55e3b62f5ea3b1f185f410ee6b` is the HEAD of branch
  `sms-backup-plus-1.5.11` — NOT reachable from any tag
- All tagged releases (2.x, 3.x) on jberkel/k-9 main branch failed to build on
  JitPack (status: `Error` for 3.702, 3.701, 3.596, 3.601; `none` for newer tags)
- Confirmed JitPack artifact at current SHA is evicted:
  `curl -si https://jitpack.io/com/github/jberkel/k-9/eaf689025e/k9mail-library-eaf689025e.aar`
  → HTTP 404
- **Path B not available** (no tag at the exact commit; main branch tags incompatible
  or unbuilt)

### Path C — Vendor as :k9mail-vendored (selected)

- Cloned `https://github.com/jberkel/k-9.git --branch sms-backup-plus-1.5.11`
- Confirmed HEAD is `eaf689025ec28d55e3b62f5ea3b1f185f410ee6b`
- Copied `k9mail-library/src/main/` verbatim into `k9mail-vendored/src/main/`
- 116 Java source files; Apache-2.0 license confirmed
- **Path C selected**

## Changes

| File | Change |
|------|--------|
| `app/build.gradle:102` | `implementation 'com.github.jberkel.k-9:k9mail-library:eaf689025e'` → `implementation project(':k9mail-vendored')` |
| `settings.gradle` | Added `':k9mail-vendored'` to include list |
| `k9mail-vendored/` (new) | Gradle library module with vendored k-9 source at `eaf689025e` |
| `gradle/verification-metadata.xml` (new) | SHA-256 checksums for all resolved external artifacts |

## Permitted Modifications to Vendored Source

Per AC-5, modifications required for AGP 8 compatibility are permitted with documentation:

1. **Import migration**: `android.support.annotation.*` → `androidx.annotation.*` in 12
   source files. Pure annotation rename; AGP 8 / AndroidX requires this. No behavioral
   change. (Documented in NOTICE and build.gradle)

2. **Dependency scope**: `jcenter()` coordinates migrated to `mavenCentral()` equivalents
   at identical versions. jcenter shut down 2022; functionally identical artifacts.

3. **`useLibrary 'org.apache.http.legacy'`** added: required by webdav/ package which
   uses Apache HTTP Client. webdav/ is not used by SMS Backup+; no behavioral change.

## Test Strategy

All existing tests cover the k-9 API surface. No new tests needed for a dependency
coordinate swap (same API, same behavior). The JaCoCo 70% gate verifies no regressions.
