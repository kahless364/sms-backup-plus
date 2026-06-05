---
type: bug
status: done
artifact_type: user-story
priority: high
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-046
title: Handle window insets for edge-to-edge so content clears the system bars (fix BUG-014)
pipeline: ''
domain: modernization
resolution: done
requirement_source: bug:BUG-014
sprint: '000010'
---

# U-046: Handle window insets for edge-to-edge so content clears the system bars

## Story
As an SMS Backup+ user on Android 15 / API 35+,
I want the app's toolbar and content to sit clear of the status and navigation bars,
So that the status-bar clock isn't covered by the toolbar title and I can reach the bottom of the screen / nav bar.

## Source
Fixes **BUG-014** (medium) — `sdlc/artifacts/stories/BUG-014-app-content-draws-under-the-statusnavigation-bars-targetsdk-.md`.

## Acceptance Criteria
- [ ] AC-1: On API 35, the main screen toolbar is laid out below the status bar — the status-bar clock/icons are fully visible and not overlapped by the toolbar title. The toolbar background may still extend behind the status bar (edge-to-edge look) but its *content* is inset by the top system-bar inset.
- [ ] AC-2: Preferences/content is not occluded by the navigation bar — the bottom of the list is reachable and any bottom controls are tappable (bottom system-bar inset applied as padding so content scrolls clear of the nav bar).
- [ ] AC-3: Implemented with the supported AndroidX approach: `WindowCompat.setDecorFitsSystemWindows(getWindow(), false)` + `ViewCompat.setOnApplyWindowInsetsListener(...)` consuming `WindowInsetsCompat.Type.systemBars()` (and `displayCutout()`), applying top/bottom (and left/right for landscape/cutout) insets as padding. Works in light and dark themes; no regression on API 21–34.
- [ ] AC-4: Applied consistently across the app's top-level activities that host a toolbar/content (MainActivity and the other activities — e.g. Advanced settings / preference / auth activities), preferably via a shared helper (e.g. an `applySystemBarInsets(View)` util) to avoid drift. Build green (assembleDebug, testDebugUnitTest, jacoco per-package LINE ≥70%). On-device before/after screenshots confirm the fix on the main screen and at least one other activity.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java:140-142` (`onCreate`) | `setContentView(R.layout.main)` + `setSupportActionBar`; no insets handling | enable edge-to-edge + apply system-bar insets (top → toolbar padding, bottom → content padding) via a shared helper |
| `app/src/main/res/layout/main.xml` | root `LinearLayout` → `Toolbar` + `FrameLayout` `@id/preferences_container`; no inset hooks | ensure the toolbar and container have ids and receive inset padding (or set `fitsSystemWindows` appropriately) |
| new `app/src/main/java/com/zegoggles/smssync/utils/WindowInsetsUtil.java` (or similar) | n/a | shared helper applying `systemBars()`+`displayCutout()` insets as padding to a given root/toolbar/content |
| other top-level activities (Advanced settings / preference / OAuth/auth activities) | same edge-to-edge issue | call the shared helper after `setContentView` |

## Existing Behavior to Preserve
- Green toolbar color + title styling; the toolbar still visually reaches the top edge (edge-to-edge look), only its content is inset.
- All navigation/preference behavior; light and dark themes; BUG-002/U-034 themed-dialog fix (themed contexts) intact.
- No regression on pre-API-35 devices (minSdk 21): insets are zero/handled gracefully there.
- The status widget (BACKUP/RESTORE buttons, status row) remains fully visible and tappable.

## Verification Steps
1. AC-1/AC-2 (on-device, emulator-5554 API 37): launch the app; capture a screenshot; confirm the status-bar clock is fully visible (no toolbar-title overlap) and the bottom of the preferences list clears the gesture/nav bar. Compare against the pre-fix screenshot in BUG-014.
2. AC-4: open Advanced settings (and any other activity) and confirm consistent insets.
3. Rotate to landscape; confirm left/right insets handled (no content under a side cutout/nav).

## Technical Context
- Root cause: `targetSdkVersion 35` (raised in U-003) triggers Android 15 forced edge-to-edge, but the app applies no `WindowInsets` handling, so content draws under the system bars. Use `androidx.core` (`WindowCompat`, `ViewCompat.setOnApplyWindowInsetsListener`, `WindowInsetsCompat`), already on the classpath. Prefer a single shared helper invoked by each activity so the behavior can't drift between screens. Return `WindowInsetsCompat.CONSUMED` appropriately or pass-through per AndroidX guidance.

## Supporting Documentation
- `sdlc/artifacts/stories/BUG-014-app-content-draws-under-the-statusnavigation-bars-targetsdk-.md` (incl. pre-fix screenshot evidence).
- Android 15 edge-to-edge behavior change. Related: U-003 (targetSdk 35), U-021 (MainActivity), U-034 (themed dialog).

## Notes
Disjoint file set from U-045 (vendored DefaultTrustedSocketFactory) — parallel-eligible. Pure UI/insets change; if a Robolectric unit test for insets is impractical, the primary gate is the on-device before/after screenshot (post-merge) plus build green.
