---
type: story
status: done
sprint: '000001'
artifact_type: user-story
priority: medium
complexity: medium
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-009
design_docs:
  - DES-MODERNIZATION-009
integration_contracts: []
dependencies:
  - U-025
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-027
title: Unpin k-9 mail library from JitPack SHA to a reproducible coordinate (or vendor as :k9mail-vendored); record in verification-metadata.xml
pipeline: ''
domain: modernization
requirement_source: authored
updated_at: '2026-06-03T17:01:16.868Z'
resolution: done
---

# U-027: Unpin k-9 Mail Library from JitPack SHA to a Reproducible Coordinate (or Vendor as :k9mail-vendored); Record in verification-metadata.xml

## Story

As a developer maintaining the SMS Backup+ build,
I want the k-9 mail library resolved from a reproducible, semantically versioned coordinate recorded in `gradle/verification-metadata.xml` rather than from the bare JitPack git-SHA `eaf689025e`,
so that the build never fails because JitPack evicts an artifact, security fixes from the upstream k-9/Thunderbird lineage are reachable without manual SHA hunting, and every CI environment produces a byte-for-byte identical artifact graph.

## Acceptance Criteria

- [ ] **AC-1 — The bare JitPack git-SHA pin `eaf689025e` is removed from `app/build.gradle`**

  Given `app/build.gradle` before this story contains the line:
  `implementation 'com.github.jberkel.k-9:k9mail-library:eaf689025e'` (line 58),
  when this story is complete and its changes are committed,
  then running `grep "eaf689025e" app/build.gradle` from the repo root returns zero results;
  no other file in the `app/` or `gradle/` directories references the string `eaf689025e` (verified by `grep -r "eaf689025e" app/ gradle/` returning zero results).

- [ ] **AC-2 — A reproducible coordinate replaces the SHA pin, evaluated in preference order per ADR-009-A**

  Given the implementer has evaluated the availability of a published, semantically versioned k-9 coordinate (per the evaluation protocol in Technical Notes),
  when the preferred path is taken (Replace-coordinate),
  then `app/build.gradle` declares the k-9 library via one of the following forms — and exactly one must apply:

  **Path A — Published mavenCentral() coordinate (preferred):** `implementation 'app.k9mail:k9mail-library:{VERSION}'` or the equivalent Thunderbird/k-9 fork coordinate with a concrete semantic version string (e.g., `6.x.y`) that resolves from `mavenCentral()` or `google()` without a JitPack repository entry; the repository block in root `build.gradle` must not add a new JitPack URL beyond what is already present; running `./gradlew :app:dependencies --configuration debugRuntimeClasspath 2>&1 | grep "k9mail"` prints the resolved version, not a SHA.

  **Path B — JitPack tag coordinate (fallback for Replace-coordinate path):** `implementation 'com.github.{owner}:{repo}:{TAG}'` where `{TAG}` is a Git tag string (e.g., `v5.600`) — not a bare commit SHA; JitPack resolves tagged coordinates reproducibly without eviction risk (tag artifacts are cached permanently); the tag must be verifiable by running `git ls-remote https://github.com/{owner}/{repo} refs/tags/{TAG}` and receiving a non-empty response.

  **Path C — Vendored local module (fallback if no reproducible published coordinate exists):** A Gradle submodule `:k9mail-vendored` is created at `k9mail-vendored/` in the repo root; `app/build.gradle` replaces the external coordinate with `implementation project(':k9mail-vendored')`; the module's `build.gradle` declares `com.android.library`, includes the IMAP/MIME source from the k-9 commit `eaf689025e` verbatim (Asset Capture — no rewrite), and carries the Apache-2.0 license header and a `NOTICE` file recording provenance (source commit, original repo URL, capture date). See "Escalation gate" below for the case where none of these paths is viable.

- [ ] **AC-3 — The resolved coordinate is recorded in `gradle/verification-metadata.xml` (supply-chain integrity)**

  Given the dependency resolution is changed to Path A, B, or C per AC-2,
  when `./gradlew --write-verification-metadata sha256 dependencies` (or the equivalent Gradle verification command for the project's Gradle version) is run from the repo root,
  then `gradle/verification-metadata.xml` is created or updated and contains a `<component>` entry for the k-9 library (group, name, version) with at least one `<artifact>` child carrying a `sha256` checksum matching the resolved artifact;
  a subsequent `./gradlew :app:compileDebugJavaSources` with `--no-daemon` exits code 0 and does not print a "verification-metadata.xml is missing" warning;
  if Path C (vendor) is taken, the `:k9mail-vendored` module's declared dependency entries (if any transitive dependencies are declared) are also included in the verification metadata.

- [ ] **AC-4 — A clean build resolves the k-9 library reproducibly without JitPack network access to an evictable SHA artifact**

  Given the local Gradle cache is cleared (`./gradlew cleanBuildCache` and the Gradle wrapper cache at `~/.gradle/caches/` is pruned of any cached `eaf689025e` artifact),
  when `./gradlew :app:compileDebugJavaSources` is run from the repo root (with or without `--offline`, subject to the path taken: tagged JitPack coordinates are cacheable; mavenCentral coordinates are stable; vendored module requires no network),
  then the command exits code 0; the k-9 library classes are on the compile classpath; and `./gradlew :app:dependencies --configuration debugRuntimeClasspath 2>&1 | grep -i "k9mail"` prints a coordinate containing a version string, not a 40-character hex SHA.

- [ ] **AC-5 — If Path C (vendor) is taken, the vendored module source is the k-9 IMAP/MIME subset at commit `eaf689025e`, adopted verbatim (Asset Capture), with Apache-2.0 provenance recorded**

  Given Path C is selected because no reproducible published coordinate exists,
  when a developer reads `k9mail-vendored/`,
  then:
  (a) the directory contains an `src/` subtree that is the IMAP/MIME library source from the k-9 repository at commit `eaf689025e` with no functional modifications — no class renames, no package renames, no method changes, no logic rewrites;
  (b) a `LICENSE` file containing the Apache License, Version 2.0 full text is present at `k9mail-vendored/LICENSE`;
  (c) a `NOTICE` file is present at `k9mail-vendored/NOTICE` recording: upstream repository URL, the specific commit SHA `eaf689025e`, the capture date (this story's completion date), and the statement "This module is an asset capture for supply-chain reproducibility; no source modifications have been made";
  (d) `grep -r "class " k9mail-vendored/src/main/java/com/fsck/k9/` returns the same class names as the original k-9 library at that commit;
  (e) running `./gradlew :k9mail-vendored:assembleDebug` exits code 0;
  (f) a comment in `k9mail-vendored/build.gradle` states "Asset capture of com.github.jberkel.k-9:k9mail-library:eaf689025e for reproducible builds — see NOTICE".

- [ ] **AC-6 — If Path C (vendor) is taken, the `MailTransport` adapter (`K9MailTransport`, introduced in U-025/U-026) continues to import from `com.fsck.k9.*` inside the adapter only, now sourced from `:k9mail-vendored` rather than a JitPack coordinate**

  Given the vendored module is on the classpath,
  when `./gradlew :app:testDebugUnitTest` is run,
  then all tests that exercised `BackupImapStore` or the IMAP transport path pass unchanged; `grep -r "com.fsck.k9" app/src/main/java/com/zegoggles/smssync/service/` returns zero results (the engine ACL from U-025/U-026 is not disturbed); `grep "com.fsck.k9" app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java` returns at least one result (the adapter legitimately uses k-9 types), confirming the vendored source is the adapter's only k-9 dependency.

- [ ] **AC-7 — Escalation gate: if no viable path exists, the implementer stops and escalates without substituting an IMAP rewrite**

  Given the implementer has exhausted Path A and Path B (no reproducible published coordinate found) and Path C (vendoring is infeasible — for example, the k-9 source at `eaf689025e` cannot be legally or structurally vendored),
  when neither path can be completed,
  then the implementer does NOT write any IMAP/MIME protocol implementation from scratch;
  instead the implementer opens a blocking issue titled "U-027 escalation: k-9 unpin — no viable reproducible coordinate or vendor path found" with a summary of every path evaluated and the specific blocker for each, and halts this story in `blocked` status;
  this story is NOT considered complete until a path is identified by the team and the escalation issue is resolved;
  the existing `app/build.gradle:58` SHA pin (`eaf689025e`) is left in place if no viable replacement has been confirmed — partial replacement is not acceptable.

- [ ] **AC-8 — Full test suite passes after the dependency change**

  Given the dependency coordinate has been replaced per AC-2 and verification metadata recorded per AC-3,
  when `./gradlew :app:testDebugUnitTest` is run from the repo root,
  then the command exits code 0 with zero new test failures relative to the pre-story baseline; specifically `StateTest.shouldGetErrorMessagePrefix` passes (the characterization gate for the `State.java:32-34` edit from U-025/U-026); converter tests (`MessageConverterTest`, `MessageGeneratorTest`) pass unchanged (the Preserved-Core converter coupling to k-9 MIME types continues to resolve, whether from the published coordinate or the vendored module).

### Integration Criteria

- [ ] **IC-1 — The k-9 library coordinate (published or vendored) is resolvable from the project's declared repository list without adding unauthenticated or snapshot repository URLs**

  The repository block in the root `build.gradle` (or `settings.gradle`) must not introduce a repository entry whose URL resolves artifacts by bare commit SHA or whose artifacts are evictable; if JitPack is retained (Path B), it must only resolve tagged coordinates from this point forward.

- [ ] **IC-2 — `gradle/verification-metadata.xml` exists after this story and is committed to the repository**

  The file `gradle/verification-metadata.xml` must be present in the working tree after this story; `git status gradle/verification-metadata.xml` confirms it is tracked (either newly created or modified); any CI pipeline that runs `./gradlew` with `--dependency-verification=strict` will succeed against the committed metadata.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/build.gradle` (line 58) | `implementation 'com.github.jberkel.k-9:k9mail-library:eaf689025e'` — bare JitPack commit SHA; evictable; no upstream security stream | Replace with versioned coordinate (Path A/B) or local module reference `project(':k9mail-vendored')` (Path C) |
| `gradle/verification-metadata.xml` | Does not exist | Create: generated by `--write-verification-metadata sha256`; records SHA-256 checksum of the resolved k-9 artifact |
| `settings.gradle` | May need `:k9mail-vendored` module include (Path C only) | Add `include ':k9mail-vendored'` if Path C is taken |
| `k9mail-vendored/` (new directory, Path C only) | Does not exist | Create: Gradle `com.android.library` module containing the k-9 IMAP/MIME source at commit `eaf689025e` verbatim; `LICENSE` (Apache-2.0); `NOTICE` (provenance record); `build.gradle` with asset-capture comment |
| Root `build.gradle` repository block | Contains `maven { url 'https://jitpack.io' }` (used by current SHA pin) | Evaluate whether JitPack entry is still required; remove if all consumers are moved off JitPack (Path A), or retain for Path B |

## Existing Behavior to Preserve

- All IMAP and MIME operations that `BackupImapStore` / `K9MailTransport` (introduced in U-025/U-026) performs must continue to function identically — this story changes only the *source* from which the k-9 library classes are resolved, not the classes themselves.
- The Preserved-Core MIME converters (`MessageConverter`, `MessageGenerator`, `MmsSupport`, `Attachment`, `HeaderGenerator`, `Headers`, `ConversionResult`, `PersonRecord`) import `com.fsck.k9.mail.*` value types. These must continue to compile and resolve against the replacement coordinate. If Path C is taken, the vendored source must provide identical public API surface (same packages, same classes, same method signatures) as the original `eaf689025e` artifact.
- `App.java:38` bootstrap call to `com.fsck.k9.mail.K9MailLib` (the carved-out, legitimate init call per DES-MODERNIZATION-009 Context bucket 4) must continue to compile and execute unchanged.
- `preferences/AuthPreferences.java:11` import of `com.fsck.k9.mail.AuthType` (the carved-out settings enum per DES-MODERNIZATION-009 Context bucket 4) must continue to resolve.
- No test class behavioral change: `BackupImapStoreTest`, `StateTest`, `MessageConverterTest`, `MessageGeneratorTest` test results must be identical before and after the coordinate swap.

## Verification Steps

1. **AC-1 (SHA pin removed):** After committing the changes, run from the repo root:
   ```
   grep "eaf689025e" app/build.gradle
   grep -r "eaf689025e" app/ gradle/
   ```
   Both commands must return zero results. If either returns output, the SHA is still present — the story is not complete.

2. **AC-2 (reproducible coordinate present, correct path taken):**
   - Open `app/build.gradle`. Confirm the k-9 dependency line matches exactly one of: (A) a `mavenCentral()`-resolvable semantic-version coordinate without `jitpack.io` in the URL, (B) a `jitpack.io` coordinate with a tag string (not a 40-hex-char SHA) as the version component, or (C) `implementation project(':k9mail-vendored')`.
   - Run `./gradlew :app:dependencies --configuration debugRuntimeClasspath 2>&1 | grep -i "k9mail"`. Confirm the output contains a version string. Confirm the output does NOT contain `eaf689025e` or any other 40-character hex string as the version.
   - For Path B: run `git ls-remote https://github.com/{owner}/{repo} refs/tags/{TAG}` and confirm a SHA is returned, confirming the tag exists on the remote.

3. **AC-3 (verification metadata):** Run:
   ```
   ./gradlew --write-verification-metadata sha256 dependencies
   ```
   Confirm `gradle/verification-metadata.xml` exists after the command. Open it and confirm a `<component>` element exists whose `group` and `name` attributes correspond to the k-9 library and whose `version` attribute matches the coordinate in `app/build.gradle`. Run `./gradlew :app:compileDebugJavaSources --no-daemon` and confirm exit code 0 with no dependency-verification warning in the output.

4. **AC-4 (clean reproducible build):** Run:
   ```
   ./gradlew cleanBuildCache
   ./gradlew :app:compileDebugJavaSources --no-daemon
   ```
   Confirm the second command exits code 0. Confirm `./gradlew :app:dependencies --configuration debugRuntimeClasspath 2>&1 | grep -i "k9mail"` prints a versioned coordinate.

5. **AC-5 (Path C vendor content, if taken):**
   - Run `ls k9mail-vendored/` and confirm `LICENSE`, `NOTICE`, `build.gradle`, and `src/` are present.
   - Run `grep "Asset capture" k9mail-vendored/build.gradle` and confirm output.
   - Run `cat k9mail-vendored/NOTICE` and confirm it contains `eaf689025e`, a repository URL, and a capture date.
   - Run `./gradlew :k9mail-vendored:assembleDebug` and confirm exit code 0.
   - Run `grep -r "class " k9mail-vendored/src/main/java/com/fsck/k9/` and manually spot-check that class names match the k-9 library (e.g., `ImapStore`, `ImapFolder`, `MessagingException`, `Message`, `Address`).

6. **AC-6 (Path C: adapter still imports k-9 types; engine is clean):**
   - Run `grep -r "com.fsck.k9" app/src/main/java/com/zegoggles/smssync/service/` — confirm zero results.
   - Run `grep "com.fsck.k9" app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java` — confirm at least one result.
   - Run `./gradlew :app:testDebugUnitTest` — confirm exit code 0.

7. **AC-7 (escalation gate, if triggered):** If this path is reached: confirm no IMAP/MIME source has been added to the repository that was not present at commit `eaf689025e`. Confirm a GitHub issue titled "U-027 escalation: k-9 unpin — no viable reproducible coordinate or vendor path found" exists and is linked from this story. Confirm `app/build.gradle:58` still contains the original `eaf689025e` coordinate (unchanged, because no replacement was confirmed).

8. **AC-8 (full test suite green):** Run `./gradlew :app:testDebugUnitTest` from the repo root. Confirm exit code 0. Run `./gradlew :app:testDebugUnitTest --tests "*StateTest*"` and confirm `shouldGetErrorMessagePrefix` passes. Run `./gradlew :app:testDebugUnitTest --tests "*MessageConverterTest*"` and `./gradlew :app:testDebugUnitTest --tests "*MessageGeneratorTest*"` and confirm both pass.

9. **IC-2 (verification metadata committed):** Run `git status gradle/verification-metadata.xml` and confirm the file appears as either a new tracked file or a modified tracked file (not untracked). Confirm `git diff --cached gradle/verification-metadata.xml` shows the k-9 component's SHA-256 entry.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Gradle build system) | Evaluate coordinate availability (Path A/B); replace `app/build.gradle:58`; generate and commit `gradle/verification-metadata.xml`; if Path C: create `:k9mail-vendored` module, copy source, write `LICENSE`/`NOTICE`, update `settings.gradle` | Developer |

## Technical Notes

**Coordinate evaluation protocol — do this first, before writing any code.**
The implementer must evaluate paths in the ADR-009-A preference order before touching any file:

1. **Check mavenCentral() for the maintained Thunderbird/k-9 lineage.** The project forked from `jberkel/k-9` to `thunderbird/thunderbird-android` (formerly `k9mail/k-9`). Search `https://search.maven.org/search?q=g:app.k9mail` and `g:com.fsck.k9`. As of the source assessment, no `k9mail-library` artifact with this group/artifact ID appears on mavenCentral — **but verify this at implementation time** because the Thunderbird project's Maven publishing posture may have changed. If a coordinate exists, check its API surface against the imports in `mail/BackupImapStore.java` (specifically `com.fsck.k9.mail.store.imap.ImapStore`, `com.fsck.k9.mail.store.imap.ImapFolder`, `com.fsck.k9.mail.MessagingException`, `com.fsck.k9.mail.Message`, `com.fsck.k9.mail.AuthType`, `com.fsck.k9.mail.K9MailLib`, `com.fsck.k9.mail.internet.*`). If the API surface is compatible, take Path A.

2. **Check for a JitPack tag on `jberkel/k-9` matching commit `eaf689025e`.** Run `git log --oneline --decorate` or `git ls-remote --tags https://github.com/jberkel/k-9` and check whether commit `eaf689025e` is reachable from a named tag. If a tag exists (e.g., `v5.600` or `5.600`), substitute the tag string for the SHA — JitPack resolves tags reproducibly. If no tag points to that exact commit but a nearby tag has compatible API (same major version, no breaking changes to the consumed surface), that tag is acceptable; document the delta in a `build.gradle` comment. Take Path B.

3. **Vendor if and only if neither path above succeeds.** Clone `https://github.com/jberkel/k-9`, check out `eaf689025e`, copy the `k9mail-library/` subtree into `k9mail-vendored/` verbatim. Verify Apache-2.0 license applies (it does — k-9 is Apache-2.0 licensed). Write the `NOTICE` file. Take Path C.

4. **Escalate if none of the above is actionable.** See AC-7. A from-scratch IMAP/MIME rewrite is explicitly out of scope per ADR-009-A and `approach.md` Component B (scores 12/100 on risk; safety-critical field-edge cases in the IMAP state machine make any behavioral regression silent until production). Do not attempt it.

**Why this story depends on U-025 (not U-026).**
U-025 introduces the `MailTransport` port interface and app-owned value types (`MailException`, `BackupFolderHandle`, `MailMessageHandle`, etc.). U-027 (this story) must run after U-025 so that `K9MailTransport` (the adapter that owns all k-9 imports) exists as the receiving class for whatever coordinate the dependency resolves to. U-027 does not need U-026 (engine rewire) to be complete first — the engine can still reference `BackupImapStore` directly while the build dependency is being stabilized — but U-027 must not precede U-025 because the adapter's package and import structure determines whether the vendored or published module's API surface is correct. If U-025 and U-026 are complete before U-027 begins, the engine grep assertions in AC-6 can be verified in full; if only U-025 is complete, the engine grep is deferred to U-026's scope.

**`gradle/verification-metadata.xml` — Gradle version prerequisite.**
Gradle dependency verification (`--write-verification-metadata`) was introduced in Gradle 6.2. The project's current `gradle-wrapper.properties` pins Gradle 6.7 (part of the U-001 AGP/Gradle uplift sequence). Confirm the wrapper version before running the metadata generation command. If the project is still on a Gradle version older than 6.2, the metadata file cannot be generated; the AGP/Gradle uplift stories (U-001, U-002) must have landed first. The `depends_on: U-025` frontmatter does not encode the Gradle version prerequisite — the developer must confirm it at sprint-planning by checking `gradle/wrapper/gradle-wrapper.properties`.

**Path C implementation details — what "verbatim" means.**
"Asset Capture — no rewrite" means: copy the source tree, adjust only what the Gradle module system requires for it to compile as a standalone `com.android.library` module (module-level `build.gradle`, manifest, dependency declarations). Specifically permitted: adding a `build.gradle` and `AndroidManifest.xml` for the module; copying `src/main/` and `src/test/` from the original library. Explicitly forbidden: renaming packages, renaming classes, changing method signatures, adding or removing methods, changing logic, removing files. If a file in the original library fails to compile under the new Gradle/AGP version without a behavioral change (e.g., an annotation processor upgrade), document the change in `NOTICE` and in a `build.gradle` comment — it is not a disqualifying modification, but it must be recorded.

**Exception-translation compatibility (Path A/B version delta).**
If Path A or B uses a k-9 version newer than `eaf689025e`, the exception hierarchy may have changed. Before finalizing the coordinate, verify that the following types still exist with the same package and supertype structure: `com.fsck.k9.mail.MessagingException`, `com.fsck.k9.mail.AuthenticationFailedException`, `com.fsck.k9.mail.XOAuth2AuthenticationFailedException`, `com.fsck.k9.mail.store.imap.ImapStore`, `com.fsck.k9.mail.store.imap.ImapFolder`. If any type has been removed or its package changed, the adapter (`K9MailTransport`) will need a targeted update — which is acceptable as a consequence of replacing the coordinate, because it is still inside the adapter (one file) and does not touch the engine. Document the version delta in a `build.gradle` comment.

**No IMAP logic change in this story.**
This story exclusively changes how the k-9 library artifact is resolved. It does not change any Java source file in `app/src/main/java/` beyond `app/build.gradle` and potentially `settings.gradle`. The only exception is Path C, which adds files to `k9mail-vendored/`. Any Java source edit in `service/`, `mail/`, or `service/state/` belongs to U-025 or U-026, not this story. If the implementer finds that the version delta (Path A/B) requires a targeted adapter edit in `K9MailTransport.java`, that edit is in scope here only if it is limited to adjusting import paths or constructor calls that changed between the old SHA and the new coordinate — not protocol logic.

**Verification-metadata.xml and CI.**
Once `gradle/verification-metadata.xml` is committed, any CI run that adds `--dependency-verification=strict` to its Gradle invocation will fail if a dependency's resolved checksum does not match the recorded one. This is the desired behavior — it detects supply-chain tampering. The developer should verify that the existing CI workflow file (`.github/workflows/`) does not already pass `--dependency-verification=lenient` or `--dependency-verification=off`, and if so, update it to `strict` or `warn` as appropriate for the project's CI maturity level. This CI hardening is within the scope of IC-2.

## Supporting Documentation

- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-009-mail-anti-corruption-layer-k9-unpin.md` — governing requirement; AC-4 (k-9 on reproducible coordinate; SHA pin removed) is this story's primary driver
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-009-mail-acl-k9-unpin-design.md` — ADR-009-A (Replace-coordinate preferred, Vendor fallback, Escalation gate), §Reproducibility risk, §Design Validation AC-4 fitness function, §Risks (no coordinate AND vendoring infeasible)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-009-mail-acl-k9-unpin-design.md#adr-009-b` — scopes the AC-2 engine grep; explains why the converter residual (inside `mail.*`) is out of scope; relevant here because Path C must preserve the same API surface for those converters

## Integration Contract References

This story does not produce or consume a CNTR artifact. It operates entirely at the Gradle build-dependency layer. The `MailTransport` port contract (CNTR-MODERNIZATION-009-mailtransport, required before sprint-planning any MU-008 story) is produced by U-025; this story depends on U-025 being complete so the adapter class (`K9MailTransport`) exists and defines the k-9 API surface that the replacement coordinate must satisfy.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Not finalized. This story covers only the build-dependency unpin decision (REQ-MODERNIZATION-009 AC-4 / ADR-009-A): evaluate for a published reproducible coordinate, replace the SHA pin or vendor verbatim, record checksums in `gradle/verification-metadata.xml`. Java source changes are not in scope except for targeted adapter adjustments arising from a version API delta (Path A/B) or the vendored module scaffolding (Path C). The escalation gate (AC-7) is load-bearing: the story must halt and escalate rather than substituting an IMAP rewrite if no viable path is found.
