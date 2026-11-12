---
artifact_type: review-security
story_id: "U-061"
verdict: "PASS"                        # PASS / FAIL
agent: "Security Reviewer"
timestamp: "2026-11-12"
blockers: 0
warnings: 4
---

# Security Review: U-061

## Review Summary

U-061 removes the donation flow and the Play Billing dependency. I found no blocking security issue. The attack surface only shrinks.

- **Manifest:** every manifest diff (merged and packaged, debug and release) is deletions only. No component gained `exported="true"`, no permission was added, and no intent filter was widened.
- **Dependencies:** 12 modules were removed. None was added and no surviving module changed version.
- **R8 rule:** the one added R8 rule restores a `-dontwarn` that the release build already carried at baseline.
- **Secrets and personal data:** the product diff and the 96-file evidence folder contain no credentials, e-mail addresses, account names, device serials, host names or user-profile paths.

I verified these points against the files on disk, not the implementation log: the manifest, `app/proguard-rules.pro`, `app/build.gradle`, the evidence dependency trees and manifests, and `gradle/verification-metadata.xml`. The review is read-only. I ran no Gradle, no adb and no network calls, and I wrote only this file.

## Verdict: PASS

## Findings

### Blockers

None. No Critical or High finding.

### Warnings

**Medium**

None.

**Low**

- **W-1. Stale trusted checksums remain in `gradle/verification-metadata.xml`.** Entries for the removed billing, Play services, datatransport and Firebase-encoders modules stay in the file. The file is byte-unchanged by this story (`git diff --stat e0c51d42..7dfd651b -- gradle metadata` is empty), `verify-metadata` is still `true` (`gradle/verification-metadata.xml:4`), and `verify-signatures` is `false` (line 5, unchanged baseline).
  - These entries are untidy, not a risk. Gradle checks only the artifacts it resolves. An entry for a module that is no longer a dependency cannot admit anything, because no build path requests that module.
  - If the module were later re-added at the same version, the old checksum would be accepted. That is a trust-on-first-use pin to a known artifact, which is the intended behaviour of the file.
  - Pruning is optional hygiene. No relaxing flag (`dependency-verification=off` or `lenient`) appears in the product diff. — Classification: defer-justified — the entries are inert for any module no build resolves and pruning is optional hygiene, which AC-9 explicitly permits.
- **W-2. The new `-dontwarn com.google.errorprone.annotations.**` is a package-wide wildcard.** It sits in `app/proguard-rules.pro:62`. It cannot hide a real run-time missing class for the following reasons.
  - The `com.google.errorprone.annotations` package holds only source/CLASS-retention annotations (`@CanIgnoreReturnValue`, `@CheckReturnValue`, `@Immutable`, `@RestrictedApi` and similar). The JVM and ART never load an annotation class that is merely referenced from an annotation attribute, and none is used reflectively.
  - R8's "Missing class" error for these names comes only from Tink's `@Immutable`, `@CanIgnoreReturnValue` and similar annotations, which are compile-time markers.
  - The consumer is `com.google.crypto.tink:tink-android:1.8.0`, pulled in by `androidx.security:security-crypto:1.1.0`. The release dependency tree shows this at `evidence/after-deps-releaseRuntime.txt:272-276`, and the pre-change tree has the same edge at `before-deps-releaseRuntime.txt:314`. The code comment in `proguard-rules.pro` is therefore correct.
  - Restoration, not novelty: the identical rule text was already in the baseline R8 configuration at `evidence/before-configuration-rules.txt:86`. It came from the consumer rules of `play-services-basement`. It is now at `evidence/after-configuration-rules.txt:67`, supplied by the app's own file. Net R8 behaviour for this concern is unchanged.
  - Scope: the wildcard covers only that one package. It does not silence warnings for Tink, androidx or any code that executes. A real missing executable class from another package would still fail the build. The trade-off is that a future Tink or other dependency referencing a new Error Prone annotation would also be silent, and that is harmless.
  - The added lines are comment plus `-dontwarn` only. The diff touches no `-keep` or `-keepclassmembers` rule, and `-dontoptimize` is unchanged. Nothing was loosened. The evidence supports this in three places.
    - The kept-name comparison (`I9-ic8.txt`) shows that `comm -23` prints exactly one removed name (`DonationActivity`) and `comm -13` prints nothing.
    - The member-level comparison (`I9-kept-members.txt`) shows classes lost only from the donation and billing code, plus one R8-synthetic class.
    - The configuration-pairs comparison (`I9-r8-config-pairs.txt`) shows exactly one added (rule, section) pair, this rule, and 63 removed pairs, all attributed to removed libraries or to AAPT keeps for the removed components.
  - Obfuscation and shrinking are intact: `app/build.gradle:53` still has `minifyEnabled true` for release, and the release APK shrank (2398685 to 1995017 bytes) because of the removed library classes. — Classification: defer-justified — the wildcard is the pre-existing baseline rule restored and is the narrowest rule that fixes the R8 failure, so narrowing it would need a build run, which is outside static-provability.
- **W-3. A runtime effect of the removed `<queries>` block exists but is fail-closed and not security-relevant.** The `<queries>` block was removed, so the app no longer declares package visibility of `com.android.vending` or `com.google.android.gms`. The product owner accepted this as a decision.
  - `GooglePlayServices.isAvailable()` (`app/src/main/java/com/zegoggles/smssync/compat/GooglePlayServices.java:19-52`) uses only `PackageManager`. It links no Play services library. On a device where the packages are not force-queryable, `NameNotFoundException` makes it return `false`.
  - The result feeds `App.gcmAvailable`. That flag has two uses, both read in full: `AutoBackupSettings.checkGCM()` (`AutoBackupSettings.java:27-32`) disables the already-dead "Old backup scheduler" preference, and `App.java:272` gates a **debug-only** `LoggingContentObserver`. That observer logs only `onChange(selfChange, uri)` (`App.java:405`), never message content.
  - A false value therefore reduces behaviour, which is the safe direction. No credential, SMS data or network path depends on it.
  - Leaving `GooglePlayServices.java` and `App.gcmAvailable` in place for the planned follow-up story carries no security risk. The class makes no network call and handles no tokens. The only plausible concern is that the code is dead. The on-device evidence showed 0 "Google Play Services" log lines and no fatal errors (`I8-device.txt`). — Classification: defer-justified — a dead-code removal needs call-graph proof, and the product owner has already scheduled it as a separate story.
- **W-4. Observation (pre-existing, not a finding against this diff): `BackupBroadcastReceiver` is exported without a permission.** It is declared at `app/src/main/AndroidManifest.xml:171-178` with `android:exported="true"` and an intent filter for `com.zegoggles.smssync.BACKUP`, with no `android:permission`.
  - This diff does not touch that element: the manifest hunk is one deleted `<activity>` line, and the `exported="true"` count in the merged manifest is identical before and after for both debug and release (checked from `before-merged-*.xml` and `after-merged-*.xml`; the intent-filter count is 42 before and after).
  - The story therefore did not change its exposure. The receiver is gated at run time by the "third-party integration" preference (`Preferences.java:179`, per the log). A third-party app can only trigger a backup, and only when that setting is ON.
  - IC-11 was run with the setting ON under the product owner's recorded waiver. No broadcast was sent, no backup or restore was triggered, and the device was not modified, so the waiver added no exposure. This is noted as context for a possible future hardening story (a signature-level permission or `exported="false"` once the third-party integration use case is revisited). It carries no tag against this story. — Classification: defer-justified — pre-existing behaviour outside this diff, unchanged by it, and not a defect of U-061.

### What was removed (manifest and permission attack surface)

Compared line by line against the merged manifests (`before-merged-*.xml` and `after-merged-*.xml`), with an independent `diff` I ran myself: 0 added lines in either variant. The normalised merged-manifest and packaged-manifest diffs in `I9-manifest-diffs.txt` agree with this independent run.

| Removed element | Source | Exported status before | Relied on by anything remaining? |
|---|---|---|---|
| `<uses-permission com.android.vending.BILLING>` | billing library | n/a | No. It was used only by the billing client, which has no remaining caller, and `grep -rniE "billing\|donat\|purchase\|sku" app/src/main --include=*.java --include=*.kt --include=*.xml` returns only an unrelated Slovak string at `values-sk/strings.xml:128`. The permission-list diff is exactly this single line, and all other permissions are unchanged (READ_SMS, WRITE_SMS, SEND_SMS, RECEIVE_SMS, call log, contacts, calendar, INTERNET, WAKE_LOCK, boot, foreground service, notifications, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` and so on). |
| `<queries>` with the Play billing BIND intents | billing library | n/a | `GooglePlayServices.isAvailable()` (see W-3). Package-visibility behaviour only. |
| `DonationActivity` | app, `AndroidManifest.xml:129` | not exported (no intent filter) | No. Nothing else referenced it (IC-10 grep returns 0). |
| `ProxyBillingActivity`, `ProxyBillingActivityV2` | billing | `exported="false"` | No. |
| `GoogleApiActivity` and the `gms.version` / `billingclient.version` meta-data | Play services | `exported="false"` | No. |
| `TransportBackendDiscovery` | datatransport | `exported="false"` | No. |
| `JobInfoSchedulerService` | datatransport | `exported="false"` with `BIND_JOB_SERVICE` | No. |
| `AlarmManagerSchedulerBroadcastReceiver` | datatransport | `exported="false"` | No. |

- No removed component was exported. Every removed component was `exported="false"` or had no intent filter, so removal does not remove any externally reachable boundary.
- Another component relying on one of the removed permissions or components: none found. The remaining permissions that overlap with the removed libraries (`WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`) are still declared by the app or by WorkManager. The after-manifest diff lists none of them as removed.
- The IC-7 packaged checks hold for both variants (`I8-ic7.txt` debug, `I9-ic7-release.txt` release): no `InitializationProvider`, `networkSecurityConfig` and `fullBackupContent` present, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` present twice, `SystemForegroundService` type unchanged.
- The packaged-release manifest diff exists only after I9. It is deletions only, and I confirmed it contains no added line.

### Dependency and supply-chain verification

- `com.android.billingclient:billing:7.1.1` and its transitive modules are gone, and the drift guard (`I5-drift-debug.txt`, `I5-drift-release.txt`) reports exit 0.
  - Before there were 95 modules and after there are 83, with 83 in common.
  - 12 modules were removed (the log counted the billing library plus 11 transitives): billing, 3 datatransport, 5 gms (base, basement, location, places-placereport, tasks) and 3 firebase-encoders.
  - Modules added: none.
  - Version drift on any surviving module: none.
  - No pin or `constraints {}` block was added.
- A grep of the after-trees finds 0 billingclient entries.
- The `app/build.gradle` diff has three hunks: a comment, deletion of the dependency line, and JaCoCo comment/rule changes. Nothing relaxes verification.
- The IC-4 check (a scratch-copy `--write-verification-metadata` run, empty diff once CRLF is stripped) shows that the committed metadata already covers every surviving artifact, so no new trust was needed and none was added. I accepted this as evidence because it is a tool diff, not a prose claim, and the committed metadata file is itself unchanged.

### Residual code

- No billing, purchase, SKU or token handling remains in `app/src/main` (grep above). Nothing logs a purchase token or SKU, because the donation classes were deleted whole.
- `Keys.DONATE` was removed (`Preferences.java:101`, diff above). The XML key `donate` is gone from `preferences.xml`, and `grep -rn "donate\|DONATE" app/src` finds no hits.
- Orphaned persisted state: none. The log establishes that no `put*`/`edit()`/`remove` call ever wrote this key, so no donation state exists in SharedPreferences to leak or orphan.
- `res/xml/backup_descriptor.xml` is unchanged. It still excludes `credentials.xml` and `pinned_certs.xml` from backup, and the story did not change any exclusion.
- `Consts.Billing` (SKU string constants `donation.1..3`) was deleted. These were public identifiers, not secrets.

### Secrets and personal-data scan

- **Product diff** (`git diff e0c51d42..7dfd651b -- . ':!sdlc'`): no credentials, tokens, e-mail addresses or absolute paths. The only added lines are the proguard rule and its comment, the `CHANGES` entry, the BuildConfig comment, the JaCoCo comment and the `EnrollmentDialogThemeTest` re-hosting.
- **Committed evidence** (96 files, including `tools/`):
  - Searched for e-mail address patterns, user-profile and home paths, the Windows user and host names, drive-letter paths, `AppData`, and secret/password/token/API-key/OAuth/bearer/authorization terms. No e-mail address and no user or host name was found.
  - Keyword hits are only class names in dex class lists (`after-dex-classes.txt`, for example Tink and OAuth2 class names) and a proguard keep line (`after-configuration-rules.txt:132`). These are identifiers, not values.
  - The device evidence (`I0-device-precondition.txt`, `I0-queries-visibility.txt`, `I8-device.txt`) records only counts, UI titles, a boolean preference value (`third_party_integration`) and the AVD name. There are no shared-preference dumps, no `dumpsys` output beyond the two package-visibility lines, no logcat lines (only counts) and no screenshots. The main-screen row summary is already redacted in the evidence (`<redacted: main-screen row, not recorded>`). No device serial or phone number was found. Device and emulator identity is limited to the stock AVD name `Pixel_9_Pro`.
  - The raw Gradle logs, JaCoCo XML, `mapping.txt` and `configuration.txt` were deliberately kept out of the repository (implementation log, I0 deviation 1). I verified the derived substitutes contain no machine paths.
- **Workspace artifacts** (`git diff e0c51d42..7dfd651b -- sdlc` outside evidence): `implementation-log.md` and one line deleted from `code-location.md`. A targeted scan found no hits.
- Result: no secrets, tokens, e-mail addresses, IMAP account names, phone numbers, SMS content, serials, hostnames or user-profile paths in what was committed.

### Security Checklist

- [x] Input validation: no input-handling code was added. The diff is deletions plus one test host class and a proguard rule.
- [x] Path traversal prevention: not applicable, no file or path handling changed.
- [x] Authentication/authorization: no auth component changed. `BackupBroadcastReceiver` exposure is unchanged (W-4, pre-existing). No removed permission is relied on by remaining code.
- [x] Data exposure: no hardcoded credentials or secrets. No sensitive data in the committed evidence. `backup_descriptor.xml` exclusions unchanged. No purchase or SKU logging remains.
- [x] Error handling (no information leakage): no error-handling code changed. `GooglePlayServices` logs only the GMS version number or a "missing" message.
- [x] Dependencies: 12 modules removed, none added, no version drift. Dependency verification stays strict (`verify-metadata` true, metadata file unchanged).
- [x] No hardcoded credentials or secrets.
- [x] Sensitive data encrypted at rest and in transit: no change to `androidx.security:security-crypto`, Tink, the network security config or pinned-certificate handling.

### Runtime Claims Verified

- [x] `## Runtime Claims` inventory read in `implementation-log.md` (`implementation-log.md:21-25`): it is declared as "None" on purpose, with a stated reason (no claim of the five runtime-provable classes). It was declared, not omitted.
- [x] Every declared claim and every security-relevant runtime-behavior AC has an embedded probe, or a stated escape-hatch justification. There are no declared claims. I audited the inventory for completeness against the story's security-relevant ACs.
  - The security-relevant ACs are AC-3 (manifest), AC-4 (dependencies), AC-9 (verification) and IC-6/IC-7/IC-8. All are static artifact comparisons backed by raw tool output (merged and packaged manifest diffs, dependency trees, R8 configuration and mapping comparison). None asserts a concurrency, timing, crash-resilience, filesystem or hook behaviour.
  - IC-11 is a UI check on the device (no Donate entry, no crash), not an exploit-path or runtime-behaviour claim of the kind the live-verification checklist covers.
  - The release APK was not run on a device (a stated limit of the plan). That is a disclosed limit of AC-4 evidence, not an unprobed security-relevant claim.
  - I found no under-declaration.
- [x] Every embedded probe's design is adequate: no probe was declared, so the inventory has nothing to assess.
- [x] Every escape-hatch justification names the specific missing infrastructure: none were used.
- [x] Inventory completeness audited against security-relevant runtime-behavior ACs: no gap. This audit is read-only and I initiated no probe.
- [x] No probe wrote into `../amp`: no probe was run, and the `evidence/tools/` scripts do not reference a sibling repo path.
- [x] UNVERIFIED (escape-hatch) claims: None.
- Disclosed (not a claim, not an escape hatch): the R8-obfuscated release APK was built but not executed. The kept-name, member and configuration comparisons are static artifact diffs.

## Phase Completion Report
---
story_id: "U-061"
phase: "security-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-018/U-061/review-security.md"
story_status: "review"
current_build_phase: "security-review"
blockers: 0
warnings: 4
errors: []
---
