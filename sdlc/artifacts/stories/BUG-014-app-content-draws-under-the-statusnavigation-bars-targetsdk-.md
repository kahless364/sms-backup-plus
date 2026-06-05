---
type: bug
status: ready
artifact_type: bug
severity: medium
priority: high
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items: []
platforms: []
tags: []
id: BUG-014
title: App content draws under the status/navigation bars (targetSdk 35 forced edge-to-edge, no window insets) — toolbar title overlaps status bar; nav bar area not usable
domain: build
origin: qa
---

# BUG-014: App content draws under the system bars (targetSdk 35 edge-to-edge, no insets)

## Description
On Android 15 / API 35 (the app's `targetSdkVersion 35`), the system enforces **edge-to-edge** display: the app window extends behind the status bar (top) and navigation bar (bottom). SMS Backup+ does not apply any window-insets handling, so its content is drawn underneath the system bars. On the main screen the green `Toolbar` extends to the very top edge and its title **"SMS Backup+" overlaps the status-bar clock**, making the status bar appear merged/missing; at the bottom, the preferences content extends under the gesture/navigation bar, so the bottom of the list and the nav area are awkward or unusable. Reported by the user as "the status bar is missing / it's not possible to interact with the nav bar."

## Steps to Reproduce
1. Run the app on an Android 15 / API 35 device or emulator (e.g. emulator-5554, API 37).
2. Open the main screen.
3. Observe: the toolbar title overlaps the status-bar clock at the top; content runs under the navigation bar at the bottom. The same applies to other activity screens (Advanced settings, etc.).

## Expected Behavior
Content respects the system bars: the toolbar sits **below** the status bar (status-bar clock/icons fully visible and not overlapped), and scrollable content ends **above** the navigation bar (or scrolls clear of it). Edge-to-edge is handled correctly via window insets, per the Android 15 requirement.

## Actual Behavior
The toolbar (and its title) is drawn behind the status bar — the title text collides with the status-bar clock — and the content container extends behind the navigation bar, because no `WindowInsets` / `fitsSystemWindows` handling is applied anywhere.

## Evidence
- `app/build.gradle` `targetSdkVersion 35` (Android 15 forces edge-to-edge for targetSdk ≥ 35).
- `grep -rE "enableEdgeToEdge|setDecorFitsSystemWindows|fitsSystemWindows|WindowInsets|systemBars"` over `app/src/main` → **no matches** (no insets handling at all).
- Theme `SMSBackupPlusTheme.Light` parent `Theme.AppCompat.Light.NoActionBar` (`app/src/main/res/values/styles.xml`) — no `android:statusBarColor` / edge-to-edge opt-out.
- Layout `app/src/main/res/layout/main.xml`: root vertical `LinearLayout` → `Toolbar` (height `?attr/actionBarSize`) + `FrameLayout` `@id/preferences_container`, with no inset padding and `fitsSystemWindows` not set.
- `MainActivity.java:140-142`: `setContentView(R.layout.main)` then `setSupportActionBar(toolbar)` — no `WindowCompat`/insets listener.
- On-device screenshot (emulator-5554, 2026-06-05): the toolbar title "SMS Backup+" is rendered overlapping the status-bar clock at the top-left.

## Acceptance Criteria
- [ ] AC-1: On API 35, the main screen's toolbar is laid out below the status bar — the status-bar clock and icons are fully visible and not overlapped by the toolbar title; the toolbar background still extends behind the status bar (proper edge-to-edge), but its content is inset.
- [ ] AC-2: Scrollable/preferences content is not occluded by the navigation bar — the bottom of the list can be reached and any bottom controls are tappable (content padded/scrolls clear of the nav bar).
- [ ] AC-3: The fix uses the supported AndroidX insets approach (`WindowCompat.setDecorFitsSystemWindows(window,false)` + `ViewCompat.setOnApplyWindowInsetsListener` applying `systemBars()` insets as padding, or `fitsSystemWindows`), handles left/right insets for landscape/display cutouts, and works in both light and dark theme. No regression on pre-35 devices (API 21+).
- [ ] AC-4: Other top-level activity screens that show a toolbar/content (e.g. Advanced settings and any preference/auth activities) are checked and given consistent insets handling (or share a common mechanism), so the issue is fixed app-wide, not only on the main screen. Build green (assembleDebug, testDebugUnitTest, jacoco ≥70%); on-device before/after screenshots confirm the fix.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java:140-142` | `setContentView` + `setSupportActionBar`; no insets handling | apply system-bar insets (top → toolbar, bottom → content) via `ViewCompat.setOnApplyWindowInsetsListener`; opt into edge-to-edge cleanly |
| `app/src/main/res/layout/main.xml` | root LinearLayout, no inset padding / `fitsSystemWindows` | add inset padding hooks (id on root/container) or `android:fitsSystemWindows` as appropriate |
| other activities hosting toolbars/preferences (e.g. `AdvancedSettings*`, auth activities) | same edge-to-edge issue | apply the same insets handling (prefer a shared helper) |
| `app/src/main/res/values/styles.xml` (optional) | theme has no edge-to-edge attrs | adjust only if needed for status/nav bar contrast (icon tint), without breaking AppCompat |

## Existing Behavior to Preserve
- The green toolbar color and title styling; the toolbar should still visually extend behind the status bar (edge-to-edge look) — only its *content* is inset.
- All existing navigation/preference behavior; light and dark themes; the BUG-002/U-034 themed-dialog fix (use themed contexts).
- No regression on pre-API-35 devices (minSdk 21).

## Verification Steps
1. AC-1/AC-2 (on-device, API 35+): open the main screen; confirm the status-bar clock/icons are fully visible (toolbar title no longer overlaps), and the bottom of the preferences list clears the navigation bar. Capture before/after screenshots.
2. AC-4: open Advanced settings and other activities; confirm consistent inset handling.
3. Rotate to landscape and (if available) test a display-cutout device profile to confirm left/right insets are handled.

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context
- Android 15 (API 35) removed the ability to opt out of edge-to-edge for `targetSdk ≥ 35`; apps must handle `WindowInsets`. This app was raised to targetSdk 35 in U-003 without adding insets handling, so the regression appeared on API 35 devices. Use AndroidX `androidx.core:core` (`WindowCompat`, `ViewCompat.setOnApplyWindowInsetsListener`, `WindowInsetsCompat.Type.systemBars()`), which is already on the classpath. Prefer a single shared helper applied by each top-level activity to avoid drift.

## Supporting Documentation
- Android 15 edge-to-edge enforcement (developer.android.com/about/versions/15/behavior-changes-15#edge-to-edge).
- Related: U-003 (targetSdk 35 uplift), U-021 (MainActivity decomposition), U-034 (themed dialog context).

## Notes
Reported by user during exploratory testing 2026-06-05; screenshot evidence captured. To be fixed in sprint-010 alongside BUG-012.
