---
artifact_type: gate-result
story_id: "U-061"
gate_id: "contract-verification"
gate: contract-verification
checkpoint: plan-gate
verdict: PASS
agent: "Solution Architect (separate instance)"
timestamp: "2026-11-12"
iteration: 2
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-018/U-061/plan.md"
base_commit: "218387187371237b288ca60db390441b6e66da80"
contracts_checked:
  - CNTR-MODERNIZATION-001
  - CNTR-MODERNIZATION-002
  - CNTR-MODERNIZATION-003
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-005
  - CNTR-MODERNIZATION-006
  - CNTR-MODERNIZATION-007
  - CNTR-MODERNIZATION-008
blockers: 0
observations: 6
---

# Gate Report: gate-result — U-061

## Gate: contract-verification @ plan-gate

## Verdict: PASS

## Summary

None of the eight approved contracts is altered by the plan. Each contract surface that the removal touches
is guarded by a fail-closed step that runs before the story closes. These steps are the four pure-deletion
manifest diffs (I8.5), the invariants step (I8.6), the kept-name and R8 configuration attribution (I8.7),
and the dependency drift guard (I5.3, I8.8). App code has no compile-time or R8-time dependency on any of the
12 removed modules.

One runtime side effect is not analysed by the design or the plan, and no step would catch it. Removing the
billing `<queries>` element removes the app's only declared package visibility of `com.android.vending`.
`compat/GooglePlayServices.isAvailable()` depends on that visibility, so the "Old backup scheduler"
preference may become disabled with the summary "Google Play Services not available". No contract covers
this, and the preference has had no readers since U-017, so scheduling behaviour cannot change. It is
recorded as a non-blocking observation (O1), with a read-only check to add at I8.10.

## Method

- All eight contract files were read for frontmatter `status` and searched for manifest, export, keep/R8,
  preference-key, dependency and verification clauses. All eight are `status: approved`.
  `grep -nicE "billing|donat|gms|play.services|GooglePlayServices"` returns 0 in every contract.
  CNTR-005 §Versioning and §Validation Rules 1-5 were read in full. CNTR-003 §Backing key set and
  §Backup exclusion, and CNTR-004 rules 1-8, were read in full.
- Every app and vendored source was searched for the removed namespaces:
  `grep -rnE "com\.google\.android\.(gms|datatransport)|com\.google\.firebase|com\.android\.billingclient|com\.android\.vending|com\.firebase" app/src k9mail-vendored/src`.
  The same search over all tracked `*.java`, `*.kt`, `*.xml` and `*.pro` files used
  `git grep … ':!sdlc'`. Scope was verified via grep.
- `GooglePlayServices.java` and both of its consumers were read in full (`App.java:115,180,272`,
  `AutoBackupSettings.java:15-33`).
- The existing merged debug manifest was read (`<queries>`, lines 117-124), along with the source manifest
  (`allowBackup`, `fullBackupContent`, `networkSecurityConfig`, `SystemForegroundService`,
  `InitializationProvider`) and the IC-11 AVD config (`google_apis_playstore`, API 36).
- U-060's release `after-configuration.txt` was read to establish which R8 rules leave with the removed
  libraries and whether any global rule loses its only source. U-060's `kept-after.txt` was checked for the
  contract classes.

## Findings

### Blocking

None.

### Per-contract and per-boundary assessment

| # | Contract / boundary | In scope? | What could break | Plan protection (step) | Sufficient? |
|---|---------------------|-----------|------------------|------------------------|-------------|
| 1 | **CNTR-005** public `com.zegoggles.smssync.BACKUP` receiver | Yes, the manifest is regenerated | Rules 1-3: action string, `exported="true"`, no `android:permission` | I8.6 asserts `exported=true`, no `permission` inside the element, and the action string, on both debug and release APK manifests. I8.5 rejects any `>` line or non-deletion hunk in four diffs (raw merged XML and normalised aapt2 dumps), so any edit to the receiver element fails the gate. The I8.7(3) `comm -13`/`-23` keeps `receiver.BackupBroadcastReceiver` by name (it is in U-060 `kept-after.txt`). | Yes |
| 2 | CNTR-005 rule 4, the `third_party_integration` user gate | Yes, the settings XML and `Preferences.Keys` are edited | Losing or renaming the key | I2 deletes only `DONATE("donate")` (line 101) and the `donate` `<Preference>`. The key `third_party_integration` (`Preferences.java:179`, `preferences.xml:210-213`) is outside the expected-file diff scope enforced by the I2 end check. `PreferenceTitlesTest` parses the edited XML at every gate. The plan only reads this setting on the device (I0.5, I8.10) and never writes it. | Yes |
| 3 | **CNTR-004** scheduler / WorkManager adapter | Yes, the manifest and R8 output are regenerated | `SystemForegroundService` losing `foregroundServiceType="dataSync"` (BUG-016); `InitializationProvider` reappearing; worker classes renamed by R8 | I8.6 asserts `foregroundServiceType=0x00000001` and `InitializationProvider` count 0. I8.7(3) kept-name set (`androidx.work`, `com.zegoggles.smssync` prefixes) includes `service.BackupWorker` and `service.RestoreWorker`. I5.3 and I8.8 `deps_drift.py` fail if `work-runtime` or Hilt-work versions drift. The contract's `com.firebase.*` mention is historical: jobdispatcher was removed in U-017 (`app/build.gradle:133`). | Yes |
| 4 | **CNTR-003** secret store (keys `login_password`, `oauth2_token`, `oauth2_refresh_token`; backup exclusion) | Indirectly: release shrinking (tink) and manifest backup attributes | Tink classes shrunk; `fullBackupContent` changed | The contract enumerates only the three secret keys and the backup descriptor, not settings keys or screen structure. `backup_descriptor.xml` is not in the 38-path list, and the porcelain end check rejects any other path. I8.6 asserts `fullBackupContent` is present, and the raw merged-XML diff in I8.5 would show any change to `@xml/backup_descriptor` or `allowBackup="true"`. The kept-name filter includes `com.google.crypto.tink`. | Yes |
| 5 | **CNTR-001 / CNTR-002** TLS socket factory, pinned-cert enrollment (flag key `transport_security_notice_shown`; pinned-cert file in the backup exclusion) | Indirectly: `networkSecurityConfig`, backup exclusion | Security provider changes; manifest attribute loss | No `ProviderInstaller` or other gms security-provider call exists anywhere (git grep 0), so removing play-services-basement cannot change the TLS stack. I8.6 asserts `networkSecurityConfig` is present, and I8.5 catches any change to it. The flag key and backup descriptor are untouched. | Yes |
| 6 | **CNTR-006** sync-state repository, **CNTR-007** mail transport | No | None: no file in either contract's producer or consumer set is among the 38 paths | Full suite at every increment (gate G); kept-name `comm` covers `com.zegoggles.smssync.*` and `com.fsck.k9.*`. | Yes |
| 7 | **CNTR-008** contacts port / People API OAuth | Checked: the OAuth flow could in principle use Google Sign-In (gms) | gms removal breaking auth | No `GoogleSignIn`, `GoogleApiAvailability` or gms auth import exists (git grep 0). OAuth is HTTP via `auth/OAuth2Client.java`. The account-manager path (`AccountManagerAuthActivity`, `TokenRefresher`) uses the platform `android.accounts.AccountManager`, which is governed by account visibility, not by `<queries>` package visibility. The removed `<queries>` intents (billing `BIND` actions) never covered browser or account intents. | Yes |
| 8 | `applicationId` / `versionCode` | Yes | Identity change breaks the in-place upgrade | I8.6 asserts `package="com.zegoggles.smssync"` and `versionCode=1602`. I8.10 uses `install -r` and escalates on `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. | Yes |
| 9 | Release shrinking: class-level keeps for contract classes (App, MainActivity, workers, receivers) | Yes | A removed library's consumer rule was the only thing keeping an app class | I8.7(3): `comm -23` must print exactly `DonationActivity` and `comm -13` nothing. U-060 `kept-after.txt` contains `App`, `activity.MainActivity`, `receiver.BackupBroadcastReceiver`, `service.BackupWorker` and `service.RestoreWorker`. I8.7(4) `r8_config_diff.py` fails closed on any added or unattributed line. | Yes, at class level; member level was verified by this gate (see O3) |
| 10 | Release shrinking: global (member-level) rules contributed by play-services-basement | Yes | Basement contributes `-keepclassmembers class * implements android.os.Parcelable { public static final *** CREATOR; }` and `@androidx.annotation.Keep` class/field/method keeps, which apply to app classes | Verified against U-060 `after-configuration.txt`. The `CREATOR` rule survives in `proguard-android-optimize.txt-9.0.1` (line 153, `**` form). The `@Keep` rules survive in `androidx-annotations.pro` (lines 730-742, stronger `{*;}` form). The app's only `Parcelable` is `activity/donation/Sku.java`, which is deleted, and no app class uses `@Keep`. `r8_config_diff.py` would attribute the removed basement lines as "OK removed-library rule" without checking for a surviving equivalent, so this gate checked that itself. | Yes (verified here) |
| 11 | Supply-chain boundary: `gradle/verification-metadata.xml` | Yes | Weakened verification; hand-edited checksums | I8.8: `git diff --stat "$BASE" -- gradle/verification-metadata.xml` must be empty, the `<configuration>` block is shown unchanged, and the path-limited `lenient` / `dependency-verification=off` grep must return 0. I8.9 runs the writer only in a scratch copy outside the repo. The orchestrator's staging rule rejects any `gradle/` path. Stale entries for the 12 removed components (for example `verification-metadata.xml:1859-1915`) remain. AC-9 explicitly permits this, and stale exact-version checksums do not weaken verification. | Yes |
| 12 | Engagement docs `endpoints.md:62`, `systems.md:55` | Documentation accuracy, not a contract | Agents directed by `.claude/CLAUDE.md` to these files read a stale tech stack | AC-11 names only `code-location.md`. Plan deviation 7 records the drift, and I7.4 asserts that both files are unedited. `systems.md:54` ("Firebase JobDispatcher + AlarmManager fallback") is already stale from U-017, so this is pre-existing engagement-doc drift. It is a legitimate follow-up, not a defect of this story (O4). | Yes for this story |

### `GooglePlayServices.java` and other app use of the removed libraries

- **No compile-time, link-time or R8 dependency.** `GooglePlayServices.java` imports only `android.*` and
  `App.TAG`. It names `com.google.android.gms` and `com.android.vending` as package-name string constants and
  calls `PackageManager.getPackageInfo` on them. No class from any of the 12 removed modules is imported
  anywhere under `app/src`, `k9mail-vendored/src` or any tracked `*.java`/`*.kt`/`*.xml`/`*.pro` outside
  `sdlc/`, apart from the files the story deletes. The build cannot fail, and R8 cannot strip anything the
  app references by name. No code reads the `com.google.android.gms.version` meta-data. That meta-data
  exists only for gms client libraries, which leave with it.
- **There is a runtime dependency on package visibility. It is unanalysed and is not covered by any
  contract (O1).** The app targets SDK 35 (`app/build.gradle:25`). From API 30 on, `getPackageInfo` throws
  `NameNotFoundException` for packages that are not visible to the caller. The merged manifest's only
  `<queries>` element (merged debug manifest lines 117-124) comes from billing. Its
  `com.android.vending.billing.InAppBillingService.BIND` intent is what currently makes `com.android.vending`
  visible to the app. AC-3 removes the whole element.

  After the change, `isAvailable()` returns `false` at its first `getPackageInfo(COM_ANDROID_VENDING, …)`
  call, unless Play Store is visible by another route (installer of record, or system force-queryable).
  This is likely on the IC-11 AVD (`google_apis_playstore`, debug APK installed via adb), and possibly for
  sideload and F-Droid users. The consequences would be:
  - `App.gcmAvailable` becomes `false`.
  - `AutoBackupSettings.checkGCM()` disables the "Old backup scheduler" (`use_old_scheduler`) preference
    and replaces its summary with "Google Play Services not available".
  - Debug builds skip registering the `LoggingContentObserver` (`App.java:272`).

  Nothing reads `isUseOldScheduler()` since U-017, so scheduling, backup and restore cannot change, and no
  contract is affected. The story's "settings behavior (other than the donation entry point) stays exactly
  as it is" could still be visibly violated on the Auto backup settings sub-screen. IC-11 would not notice,
  because it inspects only the root settings titles.

  DES-013 ("Post-removal shape") and the plan's Quality Attribute Coverage ("no remaining code needs
  them") both make an inaccurate claim here: they treat `GooglePlayServices.java` as string constants only.

### Non-blocking observations and recommended Developer checks

**O1 — Package-visibility effect on `GooglePlayServices.isAvailable()` (add to I0.5 and I8.10;
read-only).**
- **At I0.5.3**, when the app is installed, also run
  `"$ADB" -s $SER shell dumpsys package queries > "$SCRATCH/queries-before.txt"`. Record two facts in
  `$E/I0-device-precondition.txt`: whether `com.zegoggles.smssync` can see `com.android.vending`, and
  whether it can see `com.google.android.gms` (the forceQueryable list and the app's
  "queries via intent/package" entries). Record package names only, then delete the scratch file.
- **At I8.10, after step 2 (`install -r`)**, repeat the check into `$E/I8-device.txt`. Then:
  - grep the step-5 logcat for `Google Play Services` lines and record the count;
  - optionally, tap "Auto backup settings" using the bounds from the existing uiautomator dump, take a
    third dump, and record the `use_old_scheduler` row's summary text and enabled state, with no account
    data.
- **If visibility of either package is lost**, do not edit to restore it: re-adding `<queries>` contradicts
  AC-3. Escalate to the product owner with two options:
  1. Accept the change and open a follow-up that deletes the vestigial `use_old_scheduler` preference,
     its strings, `App.gcmAvailable` and `compat/GooglePlayServices.java`.
  2. Raise a CR that adds `<queries><package android:name="com.android.vending"/><package android:name="com.google.android.gms"/></queries>`
     and amends AC-3 and IC-6 accordingly.
- **The orchestrator** should also record the inaccurate "no remaining code needs them" claim against
  DES-013 for correction.

**O2 — The plan's `contracts_verified` frontmatter lists only CNTR-001..005,** while its Contracts
Verification section greps 001..008. This gate assessed all eight (table rows 6-7). This is cosmetic, and
plan.md must not be edited at this gate. The orchestrator may note it in the story's Implementation Notes.

**O3 — Member-level R8 keeps (optional hardening of I8.7).** The class-level `comm` cannot see a lost member
keep. This gate checked analytically that no global member rule loses its only source (row 10). As a cheap
empirical confirmation, add I8.7(5):
- Extract the member mapping lines of every kept app-prefix class from `before-mapping.txt` and
  `after-mapping.txt`. That is the indented lines under each `com.zegoggles.smssync.*`, `com.fsck.k9.*`,
  `androidx.work.*` and `com.google.crypto.tink.*` class header, keeping only lines whose original and
  obfuscated names are equal.
- Then `LC_ALL=C comm -23` the sorted sets. The only lines it may print are members of
  `com.zegoggles.smssync.activity.donation.*`, `MainSettings` (the removed `checkUserDonationStatus`) and
  `Consts$Billing`.

**O4 — Engagement-doc drift (follow-up, not this story).**
- **Where:** `sdlc/artifacts/engagement/endpoints.md:62` (In-app donations / Play Billing row),
  `systems.md:55` (Billing bullet) and, from earlier stories, `systems.md:54` (Firebase JobDispatcher).
- **Why it matters:** `.claude/CLAUDE.md` directs agents to these files for the tech stack, so they are
  read as authoritative. No contract depends on them.
- **Action:** the orchestrator should file one engagement-docs accuracy follow-up covering all three lines
  rather than widen AC-11.

**O5 — Stale verification-metadata entries.** The 12 removed components' checksum entries remain (for
example `verification-metadata.xml:1859-1915` for datatransport/gms, and the billing entry near line 931).
This is permitted by AC-9 and does not weaken verification. A later cleanup through the writer, removal
only, is a reasonable backlog item. No action is needed in U-061 beyond the existing I8.8 and I8.9 evidence.

**O6 — CNTR-003 body note.** CNTR-003's closing note (line 265) says the contract is "still `draft`",
while its frontmatter says `status: approved`. The frontmatter governs, so this does not affect the
verdict. It should be passed to the Artifact Librarian as a cosmetic correction.

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-061 plan (iteration 2) | sdlc/artifacts/build/sprints/sprint-018/U-061/plan.md | Artifact under review (read in full) |
| U-061 story | sdlc/artifacts/build/sprints/sprint-018/U-061-remove-donation-flow-and-play-billing.md | AC-3, AC-9, AC-11, IC-4, IC-8, scope notes (targeted grep) |
| DES-MODERNIZATION-013 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-013-donation-flow-removal-design.md | Post-removal shape (lines 80-95); manifest removal set |
| REQ-MODERNIZATION-018 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-018-remove-donation-flow-and-play-billing.md | `<queries>` removal requirement (targeted grep) |
| CNTR-MODERNIZATION-001..008 | sdlc/artifacts/design/contracts/ | Status, constraint, keep, manifest and key clauses; CNTR-005 lines 150-200 in full |
| GooglePlayServices.java | app/src/main/java/com/zegoggles/smssync/compat/GooglePlayServices.java | Full read |
| App.java | app/src/main/java/com/zegoggles/smssync/App.java | `gcmAvailable` producer and consumer (lines 115, 170-195, 265-285) |
| AutoBackupSettings.java | app/src/main/java/com/zegoggles/smssync/activity/fragments/AutoBackupSettings.java | `gcmAvailable` consumer |
| Preferences.java | app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java | `USE_OLD_SCHEDULER` readers (grep) |
| AndroidManifest.xml | app/src/main/AndroidManifest.xml | Backup, network-security, WorkManager overrides, no `<queries>` |
| Merged debug manifest (existing build output) | app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml | `<queries>` source and contents |
| app/build.gradle, settings.gradle | app/build.gradle; settings.gradle | targetSdk 35, minify config, modules |
| U-060 R8 evidence | sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/after-configuration.txt; kept-after.txt | Removed-library rule content; surviving equivalents; contract classes kept |
| Engagement docs | sdlc/artifacts/engagement/{endpoints,systems,code-location}.md | Drift lines |
| AVD config | Android AVD home, Pixel_9_Pro.avd/config.ini | Image type `google_apis_playstore`, API 36 |
| Gate report template | plugin cache templates/build-artifacts/gate-report-template.md | Frontmatter and sections |

## Phase Completion Report
---
story_id: "U-061"
phase: "plan-gate/contract-verification"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-018/U-061/contract-verification.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
