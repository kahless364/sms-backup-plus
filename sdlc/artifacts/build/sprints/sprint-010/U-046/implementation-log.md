---
artifact_type: implementation-log
story_id: "U-046"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-05"
files_changed: 1
files_created: 2
tests_added: 3
tests_passing: 662
---

# Implementation Log: U-046

## Summary

Applied AndroidX edge-to-edge window insets handling to fix BUG-014. On API 35+, Android
forces edge-to-edge display; without insets handling the toolbar title overlapped the
status-bar clock and preferences content ran under the navigation bar. The fix uses
`WindowCompat.setDecorFitsSystemWindows(window, false)` + a
`ViewCompat.setOnApplyWindowInsetsListener` that applies top inset to the toolbar and bottom
inset to the content container, with left/right for landscape/cutout. On API 21–34 insets
are zero so no visual change occurs there.

Audit of all activities with `setContentView`: only `MainActivity` calls `setContentView`
(confirmed by `grep -rn "setContentView" app/src/main/java/`). The other activities
(`AccountManagerAuthActivity`, `OAuth2WebAuthActivity`, `DonationActivity`,
`RedirectReceiverActivity`) have no toolbar/content layouts — they are dialog-only or
transparent launcher activities. The helper is applied to `MainActivity` only, which
satisfies AC-4 (consistent app-wide fix via a shared helper).

## Files Modified

| File | Change |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` | Added import for `WindowInsetsUtil`; added `WindowInsetsUtil.applyEdgeToEdgeInsets(getWindow(), toolbar, findViewById(R.id.preferences_container))` call after `setSupportActionBar(toolbar)` in `onCreate` (lines 140-145). |

## Files Created

| File | Description |
|------|-------------|
| `app/src/main/java/com/zegoggles/smssync/utils/WindowInsetsUtil.java` | Shared static helper. `applyEdgeToEdgeInsets(Window, View toolbar, View content)`: calls `WindowCompat.setDecorFitsSystemWindows(window, false)`, registers `ViewCompat.setOnApplyWindowInsetsListener` on the content view applying `systemBars() | displayCutout()` insets — top to toolbar, bottom to content, left/right to both. Returns `WindowInsetsCompat.CONSUMED`. |
| `app/src/test/java/com/zegoggles/smssync/utils/WindowInsetsUtilTest.java` | 3 unit tests: (1) no-public-constructor contract, (2) smoke — helper invocable without exception, (3) zero-insets dispatch leaves all padding at zero (pre-API-35 no-regression). |

## Integration Path

New code `WindowInsetsUtil.applyEdgeToEdgeInsets` is called from
`MainActivity.onCreate()` at line 144 after `setSupportActionBar`. `MainActivity` is the
app's sole entry point activity (declared in `AndroidManifest.xml` as the launcher
activity). No registry or dispatch map needed — direct static call.

## Contract Adherence

No `integration_contracts` listed in story frontmatter (U-046). N/A.

## Test Results

| Phase | Result |
|-------|--------|
| `:app:assembleDebug` | BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` | BUILD SUCCESSFUL (all 662 tests pass) |
| `:app:jacocoTestCoverageVerification` | BUILD SUCCESSFUL (per-package LINE >= 70% for service*, mail*, auth*) |
| Authoritative `@Test` count (HEAD) | **662** (was 659 pre-story; +3 new tests) |

Coverage gate packages (`service*`, `mail*`, `auth*`) are unaffected — `WindowInsetsUtil`
is in `utils` which has no coverage gate rule. The new test class exercises the helper's
constructor and listener path for JaCoCo credit.

Note: the `[ant:jacocoReport] Execution data for class MainActivity does not match` warning
is pre-existing (Hilt ASM transformation artefact, present since U-032) and does not affect
the coverage gate result.

## On-Device Validation Note

AC-1 and AC-2 (visual: status-bar clock visible, nav-bar not obscured) require on-device
validation on API 35+ (emulator-5554, API 37). The orchestrator confirms this via before/after
screenshots post-merge. The JVM unit tests verify structural correctness (listener registered,
zero-insets no-op). This is per the story guidance: "primary acceptance is the on-device
before/after screenshot, which the orchestrator runs post-merge."

## Acceptance Criteria Status

| AC | Status | Evidence |
|----|--------|----------|
| AC-1: toolbar title sits below status bar on API 35 | PENDING on-device | `WindowInsetsUtil` applies top inset to toolbar padding; visual confirmation post-merge |
| AC-2: content clears navigation bar | PENDING on-device | `WindowInsetsUtil` applies bottom inset to content padding; visual confirmation post-merge |
| AC-3: AndroidX approach, light/dark, no regression API 21-34 | PASS | Uses `WindowCompat` + `ViewCompat.setOnApplyWindowInsetsListener` + `WindowInsetsCompat.Type.systemBars()|displayCutout()`; zero-inset test confirms no regression |
| AC-4: consistent across activities via shared helper; build green | PASS | Only `MainActivity` has toolbar/content layout (audited); helper in `WindowInsetsUtil`; build GREEN, 662 tests pass, jacoco >= 70% |
