---
artifact_type: review-security
story_id: "U-060"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-11-12"
blockers: 0
warnings: 5
---

# Security Review: U-060 (quality-gate iteration 2)

## Review Summary

Iteration 2 reviews the rework made after QA failed AC-6. The rework diff (`git diff 8dc3e417`) is limited to a safer, OS-gated Windows `JAVA_TOOL_OPTIONS` block in `gradlew`, comment-only changes in `app/build.gradle` and `gradle.properties`, one documentation bullet in `.claude/CLAUDE.md`, a note in `sdlc/artifacts/engagement/code-location.md`, and new sections in `implementation-log.md`. It weakens no supply-chain or release-hardening control, and the iteration-1 conclusions still hold. The new evidence contains no email address, token, password, account or message data and no image. The `evidence/` folder is not yet suitable to commit as is: it is 23 MB, and a few files add no audit value and leak local details (see Evidence hygiene).

## Verdict: PASS

## Iteration-1 disposition (brief)

- Supply chain (wrapper URL and checksums, `verification-metadata.xml`, repositories, shipped dependency set, R8 posture, manifest posture, CI, `gradle.properties`, daemon JVM criteria): verified PASS in iteration 1 and re-confirmed unchanged below.
- M1 (personal Gmail address in `evidence/ac5-launch.png`): RESOLVED. The PNG was deleted and `implementation-log.md` no longer references it. The delta re-check confirmed no email-pattern match and no image file in `evidence/`. The same scan was re-run in iteration 2 across 87 files with the same result. This is no longer an open `fix-in-cycle` item.
- L1 to L5: carried forward as defer-justified. L3 (the Windows `SelectorProvider` flag on Linux) is partly addressed by this rework (see R2).

## Rework diff audit (iteration 2)

| # | Check | Result |
|---|-------|--------|
| R1 | `gradlew` is the stock Gradle 9.5.1 template plus only the local-addition block | `git diff 8dc3e417 -- gradlew` has two hunks. (a) The unconditional `export JAVA_TOOL_OPTIONS=...` and its comment block are removed from before `MAX_FD`. (b) A marked `>>> LOCAL ADDITION ... <<<` block is added after the stock `case "$( uname )"` OS detection. The block is `if "$cygwin" \|\| "$msys" ; then export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider -Djdk.net.unixdomain.tmpdir=/nonexistent"; fi`. Nothing else in the file differs from the version I reviewed in iteration 1, which was the stock template plus the same export. The wrapper invocation (`-jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar"`), the `xargs`/`sed`/`eval` argument-escaping and `exec "$JAVACMD" "$@"` are untouched. |
| R2 | Block cannot be influenced by untrusted input | The condition uses `$cygwin` / `$msys`, which are set only to the literals `true`/`false` by the stock `case "$( uname )"`. `uname` output selects among fixed branches and is never interpolated into the exported string. The exported text is two fixed `-D` flags, and the only expansion is the caller's own `${JAVA_TOOL_OPTIONS}`, which the caller already controls and could set anyway. No repository-controlled path, no `eval`, no command substitution. Gating to Windows shells also removes the earlier Linux/macOS crash exposure (L3): a JVM naming `WindowsSelectorProvider` on Linux throws `ServiceConfigurationError`, which `evidence/linux-jdk25-selector-probe.txt` shows. |
| R3 | File mode and line endings | `git ls-files -s` shows mode `100755`; `git diff --summary` reports no mode change; `file` reports "POSIX shell script ... executable"; 0 carriage returns (LF). The CRLF notice is git's `autocrlf` warning about the next git operation, not the file content. |
| R4 | `app/build.gradle` is comment-only | `git diff -U0` shows 14 changed lines: added comments and edits inside trailing `//` comments on the two `minimum = 0.20` / `minimum = 0.50` lines. The `minimum` values (0.20, 0.50) are unchanged. No dependency, repository, plugin, version, flag, `proguardFiles` or `minifyEnabled` line changed. |
| R5 | `gradle.properties` is comment-only | `git diff -U0` shows only `#` comment lines replaced or added. `org.gradle.jvmargs`, `org.gradle.daemon` and the `android.*` flags are untouched. No credential, proxy, TLS or verification setting added. |
| R6 | `.claude/CLAUDE.md` bullet | One bullet replaced: the JDK 17 `JAVA_HOME` instruction is replaced by a description of the JDK 25 daemon criteria, `auto-detect=false` troubleshooting and the `gradlew` local block. It does not instruct disabling verification, TLS or checks, and it removes a hard-coded personal path (`C:/Users/Michael.Horsley/.jdks/...`), a small privacy improvement. The "per-package LINE floors" wording replaces "LINE >= 70%", which is more accurate and not a security matter. |
| R7 | `code-location.md` | The `gradlew` note now states the block is a marked local addition, is Windows-only and must be re-applied after wrapper regeneration, and that `gradlew.bat` is stock without the workaround. Documentation only; nothing weakens a control. |
| R8 | `implementation-log.md` additions | New rework sections. I grepped the added lines for verification-disabling, `--offline`/`-x`, trust or TLS advice, tokens, passwords and account names; none instruct weakening a control. The log discloses what was NOT verified (full Linux app build, CI runner JDK 25 detection). The log retains one mention of the Windows username in the quoted "Before" text and in an absolute path (Low, see L1). |
| R9 | Files that must be unchanged since 8dc3e417 | `git diff 8dc3e417 --quiet` over `.github/workflows/ci.yml`, `gradle/verification-metadata.xml`, `gradle/wrapper/` (jar and properties), `app/proguard-rules.pro`, `build.gradle`, `settings.gradle`, `gradle/gradle-daemon-jvm.properties` and `gradlew.bat` exits 0. `sha256sum` of `gradle-wrapper.jar` is still `497c8c2a...a9c7`, the value Gradle publishes for 9.5.1. |
| R10 | Story file and sprint-plan | Lifecycle-engine status/phase/timestamp changes only, as previously verified. Not re-read for more. |
| R11 | Source and manifest | `git diff 8dc3e417 -- . ':!sdlc' ':!.claude'` touches only `app/build.gradle`, `gradle.properties` and `gradlew`. No app source, manifest, or proguard change, so the exported-component/permission/network/backup posture from iteration 1 is unchanged. |

## Evidence hygiene (iteration 2)

Scanned all 87 files under `evidence/` (including `evidence/qa/`, 24 rework/QA/Linux files not present in iteration 1), `qa-results.md`, `review-code.md` and `implementation-log.md`.

| Scan | Result |
|------|--------|
| Email-address regex | No match anywhere. |
| Image or binary media (`png/jpg/jpeg/gif/webp/bmp/heic/mp4/apk/zip/jar/db`) | None. The only non-text-log files are three JaCoCo XML reports. |
| Tokens, passwords, bearer strings, API keys, private keys, `imap.gmail`, `smtp` in `qa/`, `rework-*`, `linux-*` | No credential value. Hits in the JaCoCo XML are only class and method names (`getToken`, `OAuth2Token.java`, `SecretModule_...`). |
| Phone numbers | No real number. Regex hits are single-line JaCoCo XML only. |
| Device logs (`qa-ac5-device.txt`) | Text-only and deliberately minimal: install/launch status, pid, crash counts, four ActivityManager lines. It states that no account or message data was copied and no broadcast or setting change was made. The only identifier is `emulator-5554` (an emulator serial, not personal). The captured package is the app's own `com.zegoggles.smssync`. No account name, IMAP host or message body. |
| Machine/user identifiers | The workstation hostname (`<hostname redacted>`) appears in the `<sessioninfo>` of the three JaCoCo XML files. The Windows username `Michael.Horsley` appears in absolute paths in 21 evidence files plus `implementation-log.md` (the `evidence/` list: `after-configuration.txt`, `baseline-configuration.txt`, `after-gates.log`, `final-gates.log`, `rework-gates.log`, `checkpointA.log`, `checkpointB.log`, `step4-updateDaemonJvm.log`, `step5-wrapper.log`, `step6-writer-pass.log`, `step9-M1..M4-help.log`, `after-gradle-version-step3.1.txt`, `after-gradle-version-step4.txt`, `after-jacocoreport.log`, `baseline-gradle-version.txt`, and under `qa/`: `qa-baseline-77de23da-run1.log`, `qa-gates-run1.log`, `qa-gates-run2.log`). Low sensitivity (see L1). |
| Linux probes | `linux-*.txt` contain no home-directory path, username or hostname. |

No file contains a secret, credential, account name or message content. Nothing is a blocker for committing; the items below are hygiene recommendations.

### Recommendation: which evidence to commit

Commit (small, auditable, directly support an acceptance criterion or this review): `step5-wrapper-jar-sha256.txt`, `step6-vm-diff-summary.txt`, `step6-vm-added-components.txt`, `baseline-/after-manifest-{debug,release}.txt`, `kept-baseline.txt`, `kept-after.txt`, `c2-configuration-diff.txt`, `baseline-/after-deps-*.txt`, `after-gradle-version*.txt` and `final-gradle-version.txt`, `*-junit-totals*.txt`, `step10-device.txt`, `step11-sweep.txt`, `step6-vm-added-full.txt`, `qa/qa-ac5-device.txt`, `qa/qa-ac9-class-counts.txt`, `qa/qa-*-lines-run1.txt`, `qa/qa-junit-xml.init.gradle`, `qa/qa-gradle-version.txt`, `qa/qa-ac7-*.txt`, `rework-gradle-version.txt`, `rework-gates.exit`, `linux-*.txt`, and the JSON jacoco inventories.

Exclude from the public commit (add to `.gitignore` or leave untracked; they are bulk or leak local detail and are reproducible):
1. `evidence/after-mapping.txt` and `evidence/baseline-mapping.txt` (about 9.6 MB each, 19 MB combined, full R8 maps, L2). The comparison is already captured in `kept-*.txt` and `c2-configuration-diff.txt`.
2. `evidence/qa/qa-baseline-jacoco-run1.xml`, `evidence/qa/qa-head-jacoco-run1.xml`, `evidence/qa/qa-head-jacoco-run2.xml` (about 0.8 MB each, contain the workstation hostname; the derived `qa-*-lines-run1.txt` and `qa-ac9-class-counts.txt` carry the evidence).
3. `evidence/step9-M1-help.log` to `step9-M4-help.log` (about 190 KB each, raw `help` output with the username and Gradle-cache paths; the `step9-M*-version.txt` files carry the result).
4. `evidence/baseline-configuration.txt` and `evidence/after-configuration.txt` (merged R8 configs full of `C:\Users\<username>\.gradle\caches\...` paths; `c2-configuration-diff.txt` and `kept-*.txt` are the audit-relevant extract).
5. The raw gate logs with absolute paths and username if the owner wants no local-path disclosure: `*-gates.log`, `checkpointA.log`, `checkpointB.log`, `step4-updateDaemonJvm.log`, `step5-wrapper.log`, `step6-writer-pass.log`, `after-jacocoreport.log`, `qa/qa-gates-run1.log`, `qa/qa-gates-run2.log`, `qa/qa-baseline-77de23da-run1.log`, `rework-gates.log`. If they are kept, the cost is one account name and directory layout per file (L1). Keeping the `.exit` files and `*-junit-totals*.txt` preserves the pass/fail record.

## Findings

### Blockers

None. No Critical or High findings. `## Runtime Claims` in `implementation-log.md` still declares no runtime-provable claim for the toolchain change. The rework claim that the `gradlew` gate works on Linux is bounded by the log itself (a stub-JDK Linux probe and minimal-project Linux Gradle/Kotlin daemon probes on a real JDK 25; the full app build on Linux is explicitly not verified). That is an availability and reliability claim, not a security-relevant runtime claim. The probes ran in WSL/this checkout and did not write into `../amp`.

### Warnings

**M1 (Medium): personal email address in evidence screenshot.** Status: RESOLVED in iteration 1's delta re-check and re-confirmed here (PNG deleted, no image files, no email match across 87 files). Original classification was fix-in-cycle (evidence-redaction). Not an open item.

**L1 (Low): workstation username and local paths in evidence logs and the implementation log.** About 21 evidence files plus `implementation-log.md` carry `C:\Users\<username>\...` paths; three JaCoCo XML files carry the workstation hostname. — Classification: defer-justified — low-sensitivity local-path disclosure in machine-generated logs, mitigated by the recommended exclusion list above, and the owner can accept it.

**L2 (Low): large raw R8 mapping files.** `after-mapping.txt` and `baseline-mapping.txt` are about 9.6 MB each; they expose nothing secret for open-source code but add 19 MB to history. — Classification: defer-justified — no security impact, whether bulky evidence enters git is a repository-hygiene decision (exclusion recommended above).

**L3 (Low): Windows `SelectorProvider` flag still in `gradle.properties` `org.gradle.jvmargs` on Linux.** The rework correctly stops `gradlew` exporting it outside Windows shells, and `gradle.properties` documents that a Linux daemon and a Kotlin daemon still started in a minimal-project probe because Gradle applies `-D` jvmargs after the daemon starts. The full Linux app build (AGP, KSP, lint, R8, Robolectric test JVMs, `sdkmanager` step) is unverified and will first be exercised by the CI run. This is an availability risk, not a confidentiality or integrity risk. — Classification: defer-justified — it needs a real CI run to adjudicate and is not statically provable.

**L4 (Low): build-time-only dependencies with published advisories** (netty 4.1.110, grpc, protobuf, commons-compress, bouncycastle in the AGP classpath), none in the shipped runtime set. Unchanged by the rework. — Classification: defer-justified — versions are dictated by AGP 9.0.1/Robolectric; forcing newer versions risks the toolchain.

**L5 (Low): `-allowaccessmodification` active in release (from `proguard-android-optimize.txt`).** Unchanged by the rework; no exported-component, keep-rule or obfuscation weakening. — Classification: defer-justified — no security impact and removal would need a custom rule or reverting AGP 9's `proguard-android.txt` disallowance.

### Informational (no action)

- `verify-signatures=false` (checksum-only dependency verification, trust on first use) and tag-pinned GitHub Actions without an explicit `permissions:` block match HEAD and are not changed by U-060.
- The jacoco coverage floors are unchanged in value (comments only); the zero-headroom `receiver` package (29/58 = 50.0% against a 0.50 floor) is a quality-gate follow-up the log records, not a security finding. Note that the receiver package contains `BackupBroadcastReceiver` (the opt-in gate) and its test is the recorded follow-up.

### Security Checklist

- [x] No hardcoded credentials or secrets: none in the rework diff or any of the 87 evidence files.
- [x] Input validation: not applicable (no app source changed).
- [x] Path traversal prevention: not applicable; the `gradlew` block is two fixed literals gated on the stock OS detection.
- [x] Authentication/authorization: unchanged (no manifest, source or proguard change since 8dc3e417).
- [x] Data exposure: no account, message or token data in evidence; local username/hostname only (L1).
- [x] Error handling (no information leakage): not applicable to a build-only change.
- [x] Dependencies: unchanged since iteration 1 (`verification-metadata.xml`, repositories and wrapper unchanged; build-time advisories noted in L4).

### Runtime Claims Verified

- [x] `## Runtime Claims` inventory read in `implementation-log.md`: explicitly declared empty (not omitted).
- [x] No declared claim and no security-relevant runtime-behaviour AC requires a probe; the `gradlew` OS-gating and Linux daemon behaviour are availability claims, evidenced by `evidence/linux-*.txt` and disclosed as partial.
- [x] Embedded probe designs: the Linux probes (real JDK 25 `Selector.open` with and without the flag; Gradle 9.5.1 daemon and Kotlin daemon on a minimal project) were assessed for fidelity. They are adequate for the narrow claim (the flag class fails on Linux; the daemons still start) and the log states what they do not cover.
- [x] No escape-hatch justifications present.
- [x] Inventory completeness audited against the story's security-relevant ACs: no under-declaration.
- [x] No probe wrote into `../amp`.
- [x] UNVERIFIED (escape-hatch) claims: None. Disclosed as not exercised, not claimed verified: full Linux app build and the first CI run on JDK 25; release APK on a device.

## Phase Completion Report
---
story_id: "U-060"
phase: "security-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-017/U-060/review-security.md"
story_status: "in-progress"
current_build_phase: "security-review"
blockers: 0
warnings: 5
errors: []
---
