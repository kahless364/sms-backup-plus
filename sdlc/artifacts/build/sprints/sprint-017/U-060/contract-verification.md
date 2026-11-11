---
artifact_type: gate-result
story_id: "U-060"
gate_id: "contract-verification"
gate: "contract-verification"
checkpoint: "plan-gate"
verdict: "PASS"
agent: "Solution Architect (separate instance)"
timestamp: "2026-11-11"
iteration: 3
artifact_under_review: "sdlc/artifacts/build/sprints/sprint-017/U-060/plan.md"
blockers: []
recommended_checks: 5
---

# Gate Report: gate-result — U-060

## Gate: contract-verification @ plan-gate

## Verdict: PASS

## Summary

None of the eight approved CNTR-MODERNIZATION contracts is changed by this story. No port signature, action string, key string, scheduling invariant or exported flag is edited, and no production source or manifest file is in the plan's modification list. So the plan's `integration_contracts: []` and "Contract Gate does not apply" are correct as far as **interface definitions** go.

The plan's reasoning is incomplete on one point. It treats "no interface edited" as "no contract at risk". A toolchain upgrade does not edit contract *definitions*, but it does rebuild the *artifacts that realize them*:
- the merged manifest that publishes CNTR-005;
- the R8 output that must keep worker class names stable for CNTR-004 and Tink/protobuf for CNTR-003;
- the KSP2-generated Hilt code that wires all of them.

For the debug build, existing steps cover these well enough: B1, the full unit suite including `WorkManagerSchedulerTest` and `NetworkSecurityConfigTest`, and the AC-5 launch, which exercises the Hilt bytecode transform on `App` and `MainActivity`. For the **merged manifest** and the **release (R8) build**, the plan checks only that the build succeeds, not that its output is equivalent.

Neither gap is a blocker:
- Every input to those outputs is unchanged on the main path: same source manifest, same WorkManager 2.9.1, same security-crypto 1.1.0, same keep rules.
- The plausible failure modes are low-probability.
- Each gap can be closed with a cheap diff against the baseline tree that Step 0 already builds.

Five Developer checks are specified below (C-1 to C-5). They should be added before the story closes.

## Scope verification (grep-cited)

- **Contracts in scope:** `sdlc/artifacts/design/contracts/` holds 8 artifacts (CNTR-MODERNIZATION-001…008), all `status: approved`. All 8 were read. 003, 004 and 005 were read in full. For 001, 002 and 006–008, the frontmatter was read and the bodies were grepped for SDK, platform, R8, manifest and toolchain assumptions.
- **SDK-floor assumptions in contracts:** grep for `minSdk|API 2[12]|SDK_INT|Build.VERSION` across all contracts found **no contract clause that assumes minSdk 21 or API 21–22 behaviour**.
  - CNTR-004 INV-4 depends on `SDK_INT >= 24` (API ≤ 23 uses the broadcast fallback). That branch remains live at minSdk 23.
  - CNTR-003's only minSdk mention is a descriptive dependency on REQ-MODERNIZATION-001.
- **Hilt entry points:** grep `@AndroidEntryPoint|@HiltAndroidApp` in `app/src/main` finds only `App.java:89` and `MainActivity.java:105`. Both are exercised at runtime by the AC-5 launch (Step 10).
- **Workers:**
  - `@HiltWorker` is on `service/BackupWorker.kt:105` and `service/RestoreWorker.kt:118`.
  - `worker/BackupTriggerWorker.kt:48` is a plain `CoroutineWorker`, created by the default `WorkerFactory` fallback through reflection. That is the CNTR-004 INV-4 content-trigger path.
- **Vendored k9 reflection:** grep `Class.forName|getDeclaredMethod|getDeclaredField|newInstance(|ServiceLoader` in `k9mail-vendored/src/main` finds only static factory methods named `newInstance()` and `SAXParserFactory.newInstance()`. There is no reflective surface needing keep rules, and `k9mail-vendored/build.gradle` declares no consumer rules.
- **Existing manifest tests:** `NetworkSecurityConfigTest` asserts `BackupBroadcastReceiver` `exported="true"` with no `android:permission`, and `networkSecurityConfig` on `<application>` only. It parses **`src/main/AndroidManifest.xml`** (`MANIFEST_PATH`, line 27), the source manifest, **not the merged one**.
- **Release keep rules:** `app/proguard-rules.pro` (lines 37–53) uses only `{ *; }` keeps and `keepclassmembers` with member specs. This confirms the plan's R6 statement that AGP 9's `strictFullModeForKeepRules` default does not affect project rules.

Scope verified via grep.

## Findings

### Per-contract / per-boundary assessment

| # | Contract / boundary | In scope? | What in the plan protects it | Sufficient? |
|---|---|---|---|---|
| 1 | **CNTR-MODERNIZATION-005** public `com.zegoggles.smssync.BACKUP` broadcast: action string, package, `exported="true"`, no permission, opt-in gate | **Indirectly.** Its observable surface is the **merged** manifest, which AGP 9's manifest merger regenerates. The source manifest and receiver code are untouched. | Step 7 unit suite: `NetworkSecurityConfigTest` checks exported/no-permission, but on the *source* manifest. Step 10 launch. Step 2 does not touch `AndroidManifest.xml`. | **Partial.** Nothing verifies the merged/packaged manifest, and nothing exercises the receiver at runtime. Low likelihood (source manifest unchanged, no new library components). Add **C-1** and **C-3**. |
| 2 | **BUG-016 override**: `SystemForegroundService` `foregroundServiceType="dataSync"` + `tools:replace` (manifest:141–144); `InitializationProvider` `tools:node="remove"` (manifest:247–250), which HiltWorkerFactory injection into WorkManager depends on (CNTR-004 / DES-008) | Indirectly (manifest merger output). WorkManager stays 2.9.1 on every path. Under Branch M, androidx.hilt 1.3.0 pulls `work-runtime 2.3.4`, which resolves upward to 2.9.1 (M.4). | `processDebugMainManifest` must succeed (an implicit part of `assembleDebug`). M.3 checks only the merged `minSdkVersion`, and only under Branch M. | **Partial.** A dropped `tools:replace` or `tools:node="remove"` would compile and launch fine. It would only fail when a backup goes foreground, or when WorkManager initialises before Hilt. AC-5 does neither. Add **C-1**. |
| 3 | **CNTR-MODERNIZATION-004** BackupScheduler port: INV-1..INV-4 and `scheduleManual` | Behaviour: no source change. Release realization: yes. HiltWorkerFactory resolves workers by **class-name string** (`@StringKey` literals). Persisted WorkSpecs store `workerClassName`. `BackupTriggerWorker` is created reflectively. All three rely on library consumer keep rules (hilt-work `-keepnames @HiltWorker …`, work-runtime `-keep class * extends ListenableWorker { <init>(Context, WorkerParameters); }`) being applied by the **new R8 (AGP 9)** under the **swapped default ProGuard file**. | Debug: Step 7 runs `WorkManagerSchedulerTest` (the INV harness), B1(b)/(d) check the generated `*_AssistedFactory`/`*_HiltModule` and runtime HiltWorkerFactory wiring, and Step 7.5 requires executed ≥ baseline with 0 failures. Release: only Step 8.2 "`assembleRelease` must succeed". R6 records "release not device-tested" as residual. | **Debug: sufficient. Release: not verified.** A renamed worker class in release would build cleanly and fail at runtime: backups would stop, and pending work would be orphaned across the upgrade. That is the highest-impact silent failure in scope. Low likelihood (the consumer rules have member/annotation specs that strict mode does not affect, and the project rules are unchanged), but cheap to rule out. Add **C-2**. |
| 4 | **CNTR-MODERNIZATION-003** SecretStore / EncryptedSharedPreferences data at rest: AES-256-GCM/SIV, Keystore master key, key strings, `credentials.xml` backup exclusion | Library and key strings unchanged (`security-crypto:1.1.0` stays; Tink comes in transitively and is unchanged unless resolution shifts). Release: Tink's protobuf-lite reflection depends on Tink's consumer rules under the new R8. Backup exclusion: `android:fullBackupContent="@xml/backup_descriptor"` in the merged manifest, plus the unchanged resource. Branch M: security-crypto 1.1.0 needs minSdk ≥ 21, which 23 satisfies. | Step 7 unit tests (SecretStore tests run on JDK 25 via Robolectric). Step 6 additions-only metadata proof (no silent Tink version change goes unreviewed). Step 2 leaves the dependency line untouched. | **Mostly.** On-disk compatibility is governed by Tink's format, not the toolchain. The residual gaps are release keep behaviour for `com.google.crypto.tink.**` (covered by **C-2**) and the `fullBackupContent` attribute in the merged manifest (covered by **C-1**). Also confirm in Step 6.4 that **no `com.google.crypto.tink` version change** appears among the added components; see **C-5**. |
| 5 | **CNTR-MODERNIZATION-007** MailTransport / vendored k9 boundary | `:k9mail-vendored` stays `VERSION_1_8` / minSdk 21, and the `implementation project(...)` edge is unchanged. JDK 25 javac compiles it (proven in planning; `-Werror` is scoped to `:app`, grep-verified at `app/build.gradle:202`). No reflection surface (grep above). | Step 2.3 (no functional change). Step 7.3 (`javap` major 52 on `Address.class`). Step 7 GreenMail integration tests (`greenmail-junit4:2.1.3`). | **Sufficient.** |
| 6 | **CNTR-MODERNIZATION-001 / -002** TLS socket factory / pinned-cert enrollment; `networkSecurityConfig` | Production code unchanged. Unit tests now run on the JDK 25 host JSSE (Robolectric uses host TLS), so they may behave differently. `networkSecurityConfig` on `<application>` is part of the merged manifest. | Step 7: all tests must pass with failures = errors = 0. `NetworkSecurityConfigTest` (source manifest + XML). | **Sufficient for behaviour.** A JDK-25 JSSE policy difference would fail loudly in Step 7; it could not pass silently. The merged-manifest attribute is covered by **C-1**. |
| 7 | **CNTR-MODERNIZATION-006 / -008** SyncStateRepository (Kotlin Flow) / ContactsPort | Kotlin 2.0.21 → 2.3.21 compiler; coroutines 1.7.3 unchanged. No interface change. | B4 (Kotlin sources under `src/*/java` still compiled). Step 7 full suite. Step 7.2 per-package class counts (AC-9). | **Sufficient.** |
| 8 | **De-facto identity / packaging interface**: `applicationId com.zegoggles.smssync`, `versionCode 1602`, `ndk.abiFilters`, `namespace`, signing config, resource IDs | Not edited. AGP 9 DSL compatibility of `abiFilters`, `compileSdkVersion(int)` and similar was proven by `javap` in planning. `nonFinalResIds`/`nonTransitiveRClass` are unchanged (Step 2.5), and resource IDs are not a persisted contract. | Step 11 grep (35/35, minSdk). Step 8.2 `assembleRelease` (it would fail if AGP 9 rejected the null-`storeFile` release `signingConfig`). | **Sufficient**, plus one line in **C-1** that asserts package and versionCode from the packaged APK. |
| 9 | **Conditional minSdk 21 → 23 (Branch M)** | No **contract** assumes minSdk 21 (grep above). CNTR-004's pre-24 fallback stays live at API 23, and M.5 forbids "fixing" obsolete SDK checks in production source, which protects INV-4's `SDK_INT >= 24` branch. **However**, two approved **requirements** state minSdk 21 literally: REQ-MODERNIZATION-001 AC-2 ("`app/build.gradle` declares `minSdkVersion 21`") and REQ-MODERNIZATION-006 AC-3 / AC-5 ("minSdkVersion is set to 21"; "No behavioral change to supported devices (API 21+)"). | M.10 routes a **story** amendment to the Requirements Analyst. The PO approval is recorded in SC-2. | **Contract-wise sufficient.** Requirement traceability is incomplete: M.10 amends the story but leaves two approved REQs contradicting the build. Add **C-4**. Non-blocking because the branch is conditional and PO-approved, and those ACs are historical delivery criteria, not contracts. |
| 10 | **Supply-chain interface**: `gradle/verification-metadata.xml`, wrapper `distributionSha256Sum`, wrapper-jar hash | Directly in scope (AC-3, AC-13, AC-14; REQ-MODERNIZATION-003 AC-8). | Step 1 sets the checksum **before** the first 9.5.1 download. Step 5 regenerates with `--gradle-distribution-sha256-sum` and checks the jar sha256 against the official `497c8c2a…a9c7`. Step 6 does a pristine single-file restore, one writer pass, and the additions-only script (configuration block unchanged, 0 removed or altered), plus manual review of added groups. Step 3.4 keeps a verified checksum for 9.3.1. The daemon criteria have no download URLs and no foojay. | **Sufficient.** This is the strongest-protected boundary in the plan. Pre-existing observation, not introduced by this story: CI has no Gradle wrapper-validation action. |
| 11 | **CI contract**: `.github/workflows/ci.yml` under REQ-MODERNIZATION-003 (AC-1 commands, AC-2 minify, AC-3 JaCoCo gate) | Directly in scope (AC-8). | Step 8.1 keeps the commands verbatim. JDK becomes temurin 25, plus a build-tools 36.0.0 step. Step 8.2 does a local CI-parity run. Step 8.2 records the AGP 9 `onlyEnableUnitTestForTheTestedBuildType` change (CI `test` no longer runs `testReleaseUnitTest`; grep finds no REQ requiring release unit tests). R14 containment is pre-written. | **Sufficient as planned.** The only CI risk that cannot be verified locally is R14 (daemon JVM detection on the runner). It is honestly recorded and contained, and it is low-likelihood because setup-java's JDK 25 is the launcher JVM, which satisfies the criteria directly. |

### Blocking findings

None.

### Non-blocking findings: Developer checks to add (with plan step references)

**C-1: Merged/packaged manifest equivalence (debug and release).** Protects CNTR-005, the BUG-016 override, the `InitializationProvider` removal (CNTR-004 / Hilt-WorkManager init), `networkSecurityConfig` (CNTR-001/002) and `fullBackupContent` (CNTR-003).

*Where:*
- At the end of **Step 0.5**, in the baseline tree, after `assembleRelease`.
- Again as a new **Step 7.6**, with release taken after Step 8.2.

*How:* dump the packaged manifests from both APKs with the same tool:
```bash
AAPT2="$LOCALAPPDATA/Android/Sdk/build-tools/36.0.0/aapt2"
"$AAPT2" dump xmltree --file AndroidManifest.xml app/build/outputs/apk/debug/app-debug.apk > <evidence>/{baseline|after}-manifest-debug.txt
"$AAPT2" dump xmltree --file AndroidManifest.xml app/build/outputs/apk/release/app-release-unsigned.apk > <evidence>/{baseline|after}-manifest-release.txt   # use the actual release APK name
diff <evidence>/baseline-manifest-debug.txt <evidence>/after-manifest-debug.txt
```

*Pass condition:*
- The diff is **empty**, except for `minSdkVersion` under the full Branch M (and any `platformBuildVersion*` / `compileSdkVersion*` attributes, which should also be identical because compileSdk stays 35). Explain every other line or treat it as a failure.
- Additionally assert each of the following in the *after* dumps:
  - `package="com.zegoggles.smssync"`, `versionCode=1602`.
  - Receiver `.receiver.BackupBroadcastReceiver` has `exported=true`, **no** `permission`, and action `com.zegoggles.smssync.BACKUP`.
  - `androidx.work.impl.foreground.SystemForegroundService` has `foregroundServiceType=0x1` (dataSync).
  - **No** `androidx.startup.InitializationProvider` element.
  - `<application>` carries `networkSecurityConfig` and `fullBackupContent`.

*Failure routing:* any unexplained difference is a Checkpoint C failure. Do not regenerate or suppress it.

**C-2: R8 release keep-behaviour equivalence.** Protects the CNTR-004 worker identity (HiltWorkerFactory string keys, persisted `workerClassName`, reflective `BackupTriggerWorker`) and CNTR-003 Tink/protobuf. It also substantiates the plan's "release semantics preserved" claim for the `proguard-android-optimize.txt` + `-dontoptimize` swap.

*Where:*
- Copy the baseline `app/build/outputs/mapping/release/{mapping.txt,configuration.txt}` to evidence in **Step 0.5**. The baseline tree is deleted in Step 13.
- Compare in **Step 8.2**, after `assembleRelease`.

*How:*
```bash
# 1) reflection-sensitive classes must keep their names (identity mapping) — before and after
grep -E '^com\.zegoggles\.smssync\.(service\.(BackupWorker|RestoreWorker)|worker\.BackupTriggerWorker|App|activity\.MainActivity|receiver\.[A-Za-z]+) -> ' mapping.txt
# 2) kept-name set must not shrink
grep -oE '^([A-Za-z0-9_.$]+) -> \1:' mapping.txt | sed 's/ -> .*//' | grep -E '^(com\.zegoggles\.smssync|com\.fsck\.k9|com\.google\.crypto\.tink|androidx\.work)\.' | sort > kept-{baseline|after}.txt
comm -23 kept-baseline.txt kept-after.txt      # must print nothing (no previously-kept class renamed/removed)
# 3) effective configuration delta, for the record
diff <(grep -vE '^#' configuration-baseline.txt | sort -u) <(grep -vE '^#' configuration-after.txt | sort -u)
```

*Pass condition:*
- Step (1) shows every listed class mapped to itself in both builds.
- Step (2) prints nothing.
- Every difference in step (3) is attributable to one of the following, and is recorded in Implementation Notes:
  - the default-file swap (expected: `-allowaccessmodification` appears, alongside the project's `-dontoptimize`);
  - Hilt 2.59.2 / androidx.hilt consumer-rule changes;
  - AGP-generated rules.

*Failure routing:* a previously kept class that is now renamed or removed is a release-behaviour regression → stop and escalate. It must not be fixed by adding the `android.r8.*` opt-outs that AC-17 forbids. A project keep rule is acceptable only if it is justified and recorded.

*Also:* reword R6/Quality "APK size and runtime are unchanged" to "keep behaviour verified equivalent (C-2); R8 version changed". `-dontoptimize` preserves the optimisation posture, not the R8 implementation.

**C-3: Runtime probe of the CNTR-005 surface on the device (no settings change needed).** Protects the CNTR-005 exported/action/no-permission surface end-to-end on the installed APK.

*Where:* **Step 10**, after the launch check, in the same adb-only shell.

*How:* with `third_party_integration` at its default (`false`), run:
```bash
adb -s $SERIAL logcat -c
adb -s $SERIAL shell am broadcast -a com.zegoggles.smssync.BACKUP com.zegoggles.smssync   # contract's reference invocation
sleep 3; adb -s $SERIAL logcat -d | grep -E "backup requested via broadcast intent|FATAL EXCEPTION"
```

*Pass condition:*
- `Broadcast completed: result=0`.
- The `"…but ignored"` debug line from `BackupBroadcastReceiver.java:56` is present.
- No `FATAL EXCEPTION`.

This proves that the packaged receiver is reachable by an unprivileged external sender with the frozen action string and that the gate is intact. It does not enqueue a backup. Under Branch M with an API 23 image (M.9), repeat it there.

**C-4: Requirement supersession under the full Branch M only.**

*Where:* add to **M.10** as amendment item (e).

*What:* the Requirements Analyst records, alongside the story amendment, a change note or change record that U-060 Branch M (PO-approved) supersedes the literal minSdk-21 wording of:
- **REQ-MODERNIZATION-001 AC-2**;
- **REQ-MODERNIZATION-006 AC-3 and AC-5** ("supported devices (API 21+)" becomes "API 23+").

Without it, two approved requirements contradict the shipped build, and a later requirements-conformance audit would flag the build as non-conformant. No CNTR needs amending; CNTR-003's REQ-001 dependency line is descriptive only. Not required on the T3 path, because minSdk stays 21.

**C-5: Data-at-rest library pin check.**

*Where:* **Step 6.4**, during the review of added components.

*What:* confirm that **no `com.google.crypto.tink:*` and no `androidx.security:security-crypto` version** appears among the *added* components. Also confirm that `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -E "crypto.tink|security-crypto"` shows the same resolved versions as the baseline tree.

*Why:* AGP 9's `dependency.useConstraints=false` (R12) and the new Hilt/androidx.hilt POMs change resolution inputs. A silent Tink bump is unlikely to break the EncryptedSharedPreferences format, but CNTR-003's at-rest guarantee is tied to that library. A resolution change should be a reviewed decision, not a side effect.

### Observations (no action required for this gate)

- **O-1: wording of the plan's Contracts Verification section.** The conclusion is right: no `integration_contracts` are needed, because no boundary definition changes. The rationale should say that the toolchain *regenerates* the artifacts realizing CNTR-003/004/005 (merged manifest, R8 output, KSP2 Hilt code), and that C-1/C-2 plus the existing B1 and Step 7 checks verify them as unchanged. Optional editorial fix.
- **O-2: stale comments in `app/proguard-rules.pro`.** Lines 28–46 refer to "AGP 8 R8 full-mode", Otto and firebase-jobdispatcher keeps, but those libraries were removed (U-017/U-020). The stale `-keep` rules are harmless, since they match nothing. The plan already edits this file (appending `-dontoptimize`). Correcting the header comment is optional and keeps AC-18-style comment hygiene; removing the dead rules is out of scope.
- **O-3: R14 (CI daemon JVM selection)** is the only boundary that cannot be verified before story close. It is honestly recorded, low-likelihood (the setup-java JDK 25 launcher satisfies `toolchainVersion=25` directly), and has a pre-written containment.
- **O-4: CI wrapper validation.** CI has no `gradle/actions/wrapper-validation` step. This is pre-existing and outside U-060's ACs. A candidate for a follow-up hardening story, given that this story adds a newly tracked `gradlew.bat` and a regenerated wrapper jar (the latter is hash-checked locally in Step 5).

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| Plan under review | sdlc/artifacts/build/sprints/sprint-017/U-060/plan.md | Full read (1007 lines): Steps 0–13, Branch M, T3 path, Contracts Verification, Risks R1–R17 |
| U-060 story (frontmatter + preserve list) | sdlc/artifacts/build/sprints/sprint-017/U-060-jdk-25-build-toolchain-upgrade.md | `integration_contracts: []`, `requirements: []`; "app runtime behavior unchanged" |
| CNTR-MODERNIZATION-005 | sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-005-backup-broadcast.md | Full read: frozen action, exported=true, no permission, gate |
| CNTR-MODERNIZATION-004 | sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md | Full read: INV-1..4, pre-24 fallback, worker-tag mechanism |
| CNTR-MODERNIZATION-003 | sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-003-secret-store.md | Full read: crypto, key strings, backup exclusion, minSdk dependency note |
| CNTR-MODERNIZATION-001, -002, -006, -007, -008 | sdlc/artifacts/design/contracts/ | Frontmatter (all approved) + grep for SDK/platform/R8/manifest/toolchain assumptions (none binding) |
| REQ-MODERNIZATION-001, -003, -006 | sdlc/artifacts/requirements/modernization/ | Status (approved); minSdk-21 literal ACs (C-4); CI/minify constraints |
| Manifest | app/src/main/AndroidManifest.xml | Exported components, BUG-016 override (141–144), InitializationProvider removal (247–250), NSC, fullBackupContent |
| ProGuard rules | app/proguard-rules.pro | Keep-rule shapes; stale Otto/JobDispatcher rules |
| App build | app/build.gradle (lines 1–200) | applicationId, versionCode, abiFilters, release minify + default file, deps (security-crypto, work, hilt) |
| CI | .github/workflows/ci.yml | Commands, JDK step, caches |
| Vendored module | k9mail-vendored/build.gradle; k9mail-vendored/src/main (grep) | No consumer rules; no reflection surface |
| Manifest test | app/src/test/java/com/zegoggles/smssync/NetworkSecurityConfigTest.java | Asserts the CNTR-005 surface on the *source* manifest only (MANIFEST_PATH line 27) |
| Receiver | app/src/main/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiver.java (grep) | Log lines 52/56 used by C-3 |
| Entry points / workers (grep) | app/src/main/** | `@HiltAndroidApp` App, `@AndroidEntryPoint` MainActivity only; `@HiltWorker` Backup/RestoreWorker; plain BackupTriggerWorker |
| Gate report template | plugin cache templates/build-artifacts/gate-report-template.md | Frontmatter and sections |

## Phase Completion Report
---
story_id: "U-060"
phase: "plan-gate/contract-verification"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-017/U-060/contract-verification.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
