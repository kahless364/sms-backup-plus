---
artifact_type: review-code
story_id: "U-027"
verdict: PASS
reviewer: "Developer (self-review per sprint execution)"
timestamp: "2026-06-03"
---

# Code Review: U-027

## Summary

The implementation correctly applies Path C (vendor) for the k-9 dependency unpin.
All changes are scoped to the Gradle build layer and the new vendored module.

## Findings

### PASS — app/build.gradle

- Old SHA pin correctly replaced with `project(':k9mail-vendored')`
- Comment documents the rationale (eviction confirmed, Path A/B evaluated)
- No functional reference to `eaf689025e` remains; comment reference is appropriate

### PASS — settings.gradle

- `:k9mail-vendored` added correctly to include list
- Single-line change, correct syntax

### PASS — k9mail-vendored/build.gradle

- Asset-capture comment at top is correct and complete per AC-5
- `namespace 'com.fsck.k9.mail'` correctly declared (AGP 8 requirement)
- `useLibrary 'org.apache.http.legacy'` correctly added for webdav/
- Dependency scopes: `api` for deps directly imported by `app/src` (commons-io,
  apache-mime4j, androidx.annotation); `implementation` for internal-only deps (jzlib,
  jutf7). This is semantically correct.
- All permitted modifications documented inline.

### PASS — k9mail-vendored/LICENSE

- Apache 2.0 full text, verbatim from upstream repo. Correct.

### PASS — k9mail-vendored/NOTICE

- Contains upstream repository URL, commit SHA `eaf689025ec28d55e3b62f5ea3b1f185f410ee6b`,
  capture date 2026-06-03, and the required statement about asset capture.
- Documents the 12 import migrations (annotation migration) per AC-5 requirement.

### PASS — k9mail-vendored/src/main/java/

- 116 Java files copied verbatim from upstream
- Import migration (`android.support.annotation.*` → `androidx.annotation.*`) is the
  only permitted modification; a `sed` replace was used (no logic change)
- Class names, method signatures, package names all unchanged

### PASS — gradle/verification-metadata.xml

- File created with SHA-256 checksums for all external artifacts
- Contains entries for: commons-io, apache-mime4j-core/dom, jutf7, jzlib,
  androidx.annotation, aapt2, and all other build/runtime dependencies
- Generated via `--write-verification-metadata sha256 :app:assembleDebug`
  to capture build toolchain artifacts (aapt2) as well as runtime deps

## Issues

None. No blocking findings.

## Verdict

PASS — implementation is correct and complete for Path C vendoring.
