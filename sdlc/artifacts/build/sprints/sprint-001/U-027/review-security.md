---
artifact_type: review-security
story_id: "U-027"
verdict: PASS
reviewer: "Developer (self-review per sprint execution)"
timestamp: "2026-06-03"
---

# Security Review: U-027

## Summary

The k-9 dependency unpin moves from an evictable JitPack SHA artifact to a vendored
local module. The security posture improves: the build no longer depends on an external
service (JitPack) for a potentially evictable artifact.

## Supply Chain

- **Before**: `com.github.jberkel.k-9:k9mail-library:eaf689025e` from JitPack. Artifact
  was confirmed evicted (HTTP 404). SHA-based pins are not permanently cached by JitPack
  (unlike tags). Any attacker who could interpose on JitPack (MITM, rogue build, etc.)
  could have replaced the artifact.

- **After**: Local module `k9mail-vendored/` in the repository tree. Source code is
  committed, auditable, and content-addressed by the git repo's SHA tree. The external
  dependencies (apache-mime4j, commons-io, jutf7, jzlib) are now SHA-256 checksummed
  in `gradle/verification-metadata.xml`.

## Verification Metadata

- `gradle/verification-metadata.xml` contains SHA-256 checksums for all resolved
  external artifacts. This provides supply-chain integrity: if any transitive dependency
  is tampered with, the build will fail with a checksum mismatch.

## Source Integrity

- The vendored source is verbatim from `eaf689025ec28d55e3b62f5ea3b1f185f410ee6b` with
  the exception of 12 files that have `android.support.annotation` → `androidx.annotation`
  import migration (annotation rename, no behavioral change).
- The original source's Apache-2.0 license and NOTICE are preserved.

## Dependency Analysis

- `org.apache.james:apache-mime4j-core/dom:0.7.2`: MIME parsing library. Known version,
  checksum recorded. No known CVEs at 0.7.2 that affect this app's attack surface
  (the MIME parsing is for email body construction, not parsing untrusted input).
- `commons-io:2.4`: IO utility library. Known version, checksum recorded.
- `com.jcraft:jzlib:1.0.7`: zlib compression. Internal IMAP compression, not network-facing.
- `com.beetstra.jutf7:jutf7:1.0.0`: IMAP folder name encoding (UTF-7). Internal use only.

## No New Attack Surface

- The vendored code is the same k-9 IMAP library that was previously resolved from
  JitPack. No new functionality was added. No new network endpoints are introduced.
- The webdav/ package is present but SMS Backup+ does not use WebDAV; the code is
  inert.

## Verdict

PASS — supply chain posture improved by vendoring + verification-metadata.xml.
